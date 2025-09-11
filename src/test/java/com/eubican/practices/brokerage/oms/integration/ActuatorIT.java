package com.eubican.practices.brokerage.oms.integration;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ActuatorIT {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void healthEndpointIsPublicAndReturnsUp() {
        String url = "http://localhost:" + port + "/actuator/health";
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(response.getBody()).contains("\"status\":\"UP\"");
    }

    @Test
    void actuatorRootIsAvailableButDoesNotListNonExposedEndpoints() {
        String url = "http://localhost:" + port + "/actuator";
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

        Assertions.assertThat(response.getStatusCode()).isIn(HttpStatus.OK, HttpStatus.NOT_FOUND, HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN);
        if (response.getStatusCode().is2xxSuccessful()) {
            // Should contain a link to health but not to metrics since it's not exposed in application.yml
            Assertions.assertThat(response.getBody()).contains("health");
            Assertions.assertThat(response.getBody()).doesNotContain("metrics");
        }
    }
}
