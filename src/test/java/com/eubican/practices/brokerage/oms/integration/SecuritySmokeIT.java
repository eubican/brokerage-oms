package com.eubican.practices.brokerage.oms.integration;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SecuritySmokeIT {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private String baseUrl(String path) {
        return "http://localhost:" + port + path;
    }

    @Test
    void apiWithoutAuthIsUnauthorized() {
        String url = baseUrl("/api/v1/orders?customerId=" + UUID.randomUUID() +
                "&from=" + Instant.now().minusSeconds(3600).toString() +
                "&to=" + Instant.now().toString());

        ResponseEntity<String> response = restTemplate.getForEntity(URI.create(url), String.class);
        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void apiWithWrongAutIsUnauthorized() {
        TestRestTemplate wrongUser = restTemplate.withBasicAuth("wrong", "creds");
        String url = baseUrl("/api/v1/orders?customerId=" + UUID.randomUUID() +
                "&from=" + Instant.now().minusSeconds(3600).toString() +
                "&to=" + Instant.now().toString());
        ResponseEntity<String> response = wrongUser.getForEntity(URI.create(url), String.class);
        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void apiWithCorrectAuthIsAuthorized() {
        TestRestTemplate admin = restTemplate.withBasicAuth("test", "test");
        String url = baseUrl("/api/v1/orders?customerId=" + UUID.randomUUID() +
                "&from=" + Instant.now().minusSeconds(3600).toString() +
                "&to=" + Instant.now().toString());

        ResponseEntity<String> response = admin.exchange(url, HttpMethod.GET, new HttpEntity<>(new HttpHeaders()), String.class);
        Assertions.assertThat(response.getStatusCode()).isNotIn(HttpStatus.UNAUTHORIZED);
        Assertions.assertThat(response.getStatusCode()).isIn(HttpStatus.OK);
    }
}
