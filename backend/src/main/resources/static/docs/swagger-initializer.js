// NFR-007: Renders the checked-in OpenAPI contract that the drift test keeps aligned with the code.
window.onload = function () {
  window.ui = SwaggerUIBundle({
    url: '/openapi/planning-api.yaml',
    dom_id: '#swagger-ui',
    deepLinking: true,
    presets: [SwaggerUIBundle.presets.apis, SwaggerUIStandalonePreset],
    layout: 'BaseLayout'
  });
};
