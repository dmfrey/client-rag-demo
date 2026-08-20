package com.example.clientragdemo.configuration;

import com.example.clientragdemo.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.micrometer.metrics.test.autoconfigure.AutoConfigureMetrics;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * @AutoConfigureMetrics is required here - @SpringBootTest disables metrics export by default
 * (DisableMetricsExportContextCustomizer sets management.defaults.metrics.export.enabled=false),
 * so without it PrometheusMetricsExportAutoConfiguration's @ConditionalOnEnabledMetricsExport
 * never matches and /actuator/prometheus 401s regardless of the real
 * management.endpoints.web.exposure.include config. Production is unaffected either way.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureTestRestTemplate
@AutoConfigureMetrics
class ActuatorSecurityIT {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void healthIsReachableWithoutAuthentication() {
        ResponseEntity<String> response = restTemplate.getForEntity("/actuator/health", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void prometheusIsReachableWithoutAuthentication() {
        ResponseEntity<String> response = restTemplate.getForEntity("/actuator/prometheus", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void unexposedActuatorEndpointsStillRequireAuthentication() {
        ResponseEntity<String> response = restTemplate.getForEntity("/actuator/env", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
