package com.timetable.todo.platform;

import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * NFR-003, SECURITY-04, SECURITY-07, SECURITY-08: Declares every public route explicitly.
 *
 * <p>The planning API, the local documentation assets and the health endpoint are intentionally
 * public because this application is a single-user local tool with no accounts. Every other route
 * is denied, so a future endpoint is closed until it is declared here on purpose.
 */
@Configuration(proxyBeanMethods = false)
@EnableWebSecurity
public class PlatformSecurityConfiguration {

  private static final String CONTENT_SECURITY_POLICY =
      "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; "
          + "img-src 'self' data:; connect-src 'self'; font-src 'self' data:; "
          + "object-src 'none'; base-uri 'self'; form-action 'self'; frame-ancestors 'none'";

  @Bean
  SecurityFilterChain planningFilterChain(
      HttpSecurity http, @Qualifier("corsConfigurationSource") CorsConfigurationSource cors)
      throws Exception {
    return http.csrf(csrf -> csrf.disable())
        .cors(configurer -> configurer.configurationSource(cors))
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .formLogin(login -> login.disable())
        .httpBasic(basic -> basic.disable())
        .logout(logout -> logout.disable())
        .anonymous(anonymous -> anonymous.disable())
        .headers(
            headers ->
                headers
                    .frameOptions(HeadersConfigurer.FrameOptionsConfig::deny)
                    .httpStrictTransportSecurity(HeadersConfigurer.HstsConfig::disable)
                    .referrerPolicy(
                        referrer ->
                            referrer.policy(
                                org.springframework.security.web.header.writers
                                    .ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
                    .contentSecurityPolicy(csp -> csp.policyDirectives(CONTENT_SECURITY_POLICY)))
        .authorizeHttpRequests(
            authorize ->
                authorize
                    .requestMatchers("/api/v1/**")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/docs/**", "/openapi/**", "/webjars/**")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/**")
                    .permitAll()
                    .anyRequest()
                    .denyAll())
        .exceptionHandling(
            handling ->
                handling
                    .authenticationEntryPoint(
                        (request, response, failure) ->
                            deny(response, request.getRequestURI() != null))
                    .accessDeniedHandler((request, response, failure) -> deny(response, true)))
        .build();
  }

  /** SECURITY-08: Exact loopback development origins only; a wildcard fails property validation. */
  @Bean
  CorsConfigurationSource corsConfigurationSource(PlatformProperties properties) {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(properties.allowedOrigins());
    configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(List.of("Content-Type", "X-Request-Id"));
    configuration.setExposedHeaders(List.of("X-Request-Id"));
    configuration.setAllowCredentials(false);
    configuration.setMaxAge(600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/api/v1/**", configuration);
    return source;
  }

  private void deny(jakarta.servlet.http.HttpServletResponse response, boolean ignored)
      throws java.io.IOException {
    response.setStatus(HttpStatus.FORBIDDEN.value());
    response.setContentType("application/json");
    response
        .getWriter()
        .write(
            "{\"code\":\"ACCESS_DENIED\",\"message\":\"This route is not available\","
                + "\"requestId\":\""
                + RequestCorrelation.current()
                + "\"}");
  }
}
