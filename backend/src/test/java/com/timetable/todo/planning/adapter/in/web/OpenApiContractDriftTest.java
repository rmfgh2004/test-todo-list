package com.timetable.todo.planning.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.yaml.snakeyaml.Yaml;

/**
 * NFR-007: Fails when the checked-in OpenAPI contract and the real routing table drift apart.
 *
 * <p>The contract is the frontend's source of truth for U2, so an endpoint may never be added,
 * removed or renamed in code without the same change in {@code backend/openapi/planning-api.yaml}.
 *
 * <p>Path and method equality alone missed a real defect: {@link
 * com.timetable.todo.platform.RateLimitFilter} answered 429 with {@code RATE_LIMITED} and a {@code
 * Retry-After} header while the contract documented neither. The response-level assertions below
 * close that gap for the cross-cutting statuses a filter can produce ahead of routing.
 */
@SpringBootTest
class OpenApiContractDriftTest {

  private static final Path CONTRACT = Path.of("openapi", "planning-api.yaml");
  private static final Set<String> DOCUMENTED_METHODS =
      Set.of("get", "post", "put", "patch", "delete");

  @Autowired
  @Qualifier("requestMappingHandlerMapping") private RequestMappingHandlerMapping handlerMapping;

  @Test
  @SuppressWarnings("unchecked")
  void NFR_007_documented_operations_match_the_planning_routes() throws Exception {
    Map<String, Object> document;
    try (InputStream contract = Files.newInputStream(CONTRACT)) {
      document = new Yaml().load(contract);
    }

    Set<String> documented = new TreeSet<>();
    ((Map<String, Map<String, Object>>) document.get("paths"))
        .forEach(
            (path, operations) ->
                operations.keySet().stream()
                    .filter(DOCUMENTED_METHODS::contains)
                    .forEach(method -> documented.add(method.toUpperCase() + " " + path)));

    Set<String> routed = new TreeSet<>();
    handlerMapping.getHandlerMethods().keySet().forEach(info -> routed.addAll(operationsOf(info)));

    assertThat(routed).as("every planning route is documented").isEqualTo(documented);
  }

  @Test
  @SuppressWarnings("unchecked")
  void NFR_007_every_operation_documents_the_globally_reachable_rate_limit_response()
      throws Exception {
    Map<String, Object> document = contract();

    Set<String> missing = new TreeSet<>();
    ((Map<String, Map<String, Object>>) document.get("paths"))
        .forEach(
            (path, operations) ->
                operations.forEach(
                    (method, operation) -> {
                      if (!DOCUMENTED_METHODS.contains(method)) {
                        return;
                      }
                      Map<String, Object> responses =
                          (Map<String, Object>) ((Map<String, Object>) operation).get("responses");
                      if (responses == null || !responses.containsKey("429")) {
                        missing.add(method.toUpperCase() + " " + path);
                      }
                    }));

    assertThat(missing)
        .as("the rate limiter runs ahead of routing, so 429 is reachable on every operation")
        .isEmpty();
  }

  @Test
  @SuppressWarnings("unchecked")
  void NFR_007_the_documented_error_codes_include_every_platform_emitted_code() throws Exception {
    Map<String, Object> document = contract();

    Map<String, Object> schemas =
        (Map<String, Object>) ((Map<String, Object>) document.get("components")).get("schemas");
    Map<String, Object> code =
        (Map<String, Object>)
            ((Map<String, Object>)
                    ((Map<String, Object>) schemas.get("ApiError")).get("properties"))
                .get("code");
    List<String> documentedCodes = (List<String>) code.get("enum");

    assertThat(documentedCodes)
        .as("codes emitted by platform filters, not only by the web adapter, must be documented")
        .contains("RATE_LIMITED", "INTERNAL_ERROR", "METHOD_NOT_ALLOWED", "UNSUPPORTED_MEDIA_TYPE");
  }

  @Test
  @SuppressWarnings("unchecked")
  void NFR_007_the_rate_limit_response_documents_the_retry_after_header() throws Exception {
    Map<String, Object> document = contract();

    Map<String, Object> responses =
        (Map<String, Object>) ((Map<String, Object>) document.get("components")).get("responses");
    Map<String, Object> tooManyRequests = (Map<String, Object>) responses.get("TooManyRequests");

    assertThat(tooManyRequests).as("the 429 response component exists").isNotNull();
    assertThat((Map<String, Object>) tooManyRequests.get("headers"))
        .as("the client cannot honour a retry delay it was never told about")
        .containsKey("Retry-After");
  }

  private Map<String, Object> contract() throws Exception {
    try (InputStream contract = Files.newInputStream(CONTRACT)) {
      return new Yaml().load(contract);
    }
  }

  private List<String> operationsOf(RequestMappingInfo info) {
    Set<String> patterns =
        info.getPathPatternsCondition() == null
            ? Set.of()
            : info.getPathPatternsCondition().getPatternValues();
    return patterns.stream()
        .filter(pattern -> pattern.startsWith("/api/v1/"))
        .flatMap(
            pattern ->
                info.getMethodsCondition().getMethods().stream()
                    .map(method -> method.name() + " " + pattern))
        .toList();
  }
}
