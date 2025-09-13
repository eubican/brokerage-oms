package com.eubican.practices.brokerage.oms.web;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import java.time.Instant;
import java.util.UUID;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AssetControllerIT {

    @LocalServerPort
    private int port;

    private static final UUID CUSTOMER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

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
                .subject(CUSTOMER_ID.toString())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(3600))
                .claim("role", "ROLE_ADMIN")
                .build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        return "Bearer " + token;
    }

    @Test
    void fetchCustomerAllAssets() {
        String from = Instant.now().minusSeconds(3600).toString();
        String to = Instant.now().plusSeconds(3600).toString();
        String url = baseUrl("/api/v1/assets?customerId=" + CUSTOMER_ID + "&from=" + from + "&to=" + to);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", bearerTokenAdmin());

        ResponseEntity<String> stringResponseEntity =
                restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
        Assertions.assertThat(stringResponseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(stringResponseEntity.getBody()).contains("XYZ");
    }
}
