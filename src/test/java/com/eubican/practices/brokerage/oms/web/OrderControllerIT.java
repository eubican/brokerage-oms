package com.eubican.practices.brokerage.oms.web;

import com.eubican.practices.brokerage.oms.domain.model.OrderSide;
import com.eubican.practices.brokerage.oms.web.dto.CreateOrderRequest;
import com.eubican.practices.brokerage.oms.web.dto.OrderResponse;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class OrderControllerIT {

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
    void createOrderHappyPathAndFetchInListThenCancel() {
        String createUrl = baseUrl("/api/v1/orders");
        CreateOrderRequest request = new CreateOrderRequest(
                CUSTOMER_ID,
                "XYZ",
                OrderSide.BUY,
                new BigDecimal("2"),
                new BigDecimal("10.00")
        );

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", bearerTokenAdmin());

        // ---- CREATE ORDER
        ResponseEntity<OrderResponse> createResponse = restTemplate.exchange(createUrl, HttpMethod.POST, new HttpEntity<>(request, headers), OrderResponse.class);
        Assertions.assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        OrderResponse body = createResponse.getBody();
        Assertions.assertThat(body).isNotNull();
        Assertions.assertThat(body.orderId()).isNotNull();
        Assertions.assertThat(body.customerId()).isEqualTo(CUSTOMER_ID);
        Assertions.assertThat(body.assetName()).isEqualTo("XYZ");
        Assertions.assertThat(body.size()).isEqualByComparingTo(new BigDecimal("2"));
        Assertions.assertThat(body.price()).isEqualByComparingTo(new BigDecimal("10.00"));

        // ---- FETCH ORDERS (check created is included)
        String from = Instant.now().minusSeconds(3600).toString();
        String to = Instant.now().plusSeconds(3600).toString();
        String listUrl = baseUrl("/api/v1/orders?customerId=" + CUSTOMER_ID + "&from=" + from + "&to=" + to);
        ResponseEntity<String> listResponse =
                restTemplate.exchange(listUrl, HttpMethod.GET, new HttpEntity<>(headers), String.class);
        Assertions.assertThat(listResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(listResponse.getBody()).contains(body.orderId().toString());

        // ---- CANCEL ORDER (check created is canceled)
        String cancelUrl = baseUrl("/api/v1/orders/" + body.orderId() + "/cancel");
        ResponseEntity<Void> cancelResponse =
                restTemplate.exchange(cancelUrl, HttpMethod.POST, new HttpEntity<>(headers), Void.class);
        Assertions.assertThat(cancelResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }
}
