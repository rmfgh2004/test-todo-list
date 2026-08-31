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
