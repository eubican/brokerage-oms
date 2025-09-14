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
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SecuritySmokeIT {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JwtEncoder jwtEncoder;

    private String baseUrl(String path) {
        return "http://localhost:" + port + path;
    }

    private String bearerTokenAdmin() {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(UUID.randomUUID().toString())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(3600))
                .claim("role", "ROLE_ADMIN")
                .build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        return "Bearer " + token;
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
    void apiWithWrongAuthIsUnauthorized() {
        TestRestTemplate wrongUser = restTemplate.withBasicAuth("wrong", "creds");
        String url = baseUrl("/api/v1/orders?customerId=" + UUID.randomUUID() +
                "&from=" + Instant.now().minusSeconds(3600).toString() +
                "&to=" + Instant.now().toString());
        ResponseEntity<String> response = wrongUser.getForEntity(URI.create(url), String.class);
        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void apiWithCorrectAuthIsAuthorized() {
        String url = baseUrl("/api/v1/orders?customerId=" + UUID.randomUUID() +
                "&from=" + Instant.now().minusSeconds(3600).toString() +
                "&to=" + Instant.now().toString());

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", bearerTokenAdmin());
        ResponseEntity<String> response =
                restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
        Assertions.assertThat(response.getStatusCode()).isNotIn(HttpStatus.UNAUTHORIZED);
        Assertions.assertThat(response.getStatusCode()).isIn(HttpStatus.OK);
    }
}
