package com.eubican.practices.brokerage.oms.web;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.UUID;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AssetControllerIT {

    @LocalServerPort
    private int port;

    private static final UUID CUSTOMER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Autowired
    private TestRestTemplate restTemplate;

    private String baseUrl(String path) {
        return "http://localhost:" + port + path;
    }

    @Test
    void fetchCustomerAllAssets() {
        String from = Instant.now().minusSeconds(3600).toString();
        String to = Instant.now().plusSeconds(3600).toString();
        String url = baseUrl("/api/v1/assets?customerId=" + CUSTOMER_ID + "&from=" + from + "&to=" + to);

        TestRestTemplate authed = restTemplate.withBasicAuth("test", "test");

        ResponseEntity<String> stringResponseEntity = authed.getForEntity(url, String.class);
        Assertions.assertThat(stringResponseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(stringResponseEntity.getBody()).contains("XYZ");
    }
}
