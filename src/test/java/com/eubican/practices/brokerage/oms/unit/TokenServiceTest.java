package com.eubican.practices.brokerage.oms.unit;

import com.eubican.practices.brokerage.oms.domain.exception.ApplicationException;
import com.eubican.practices.brokerage.oms.domain.model.Customer;
import com.eubican.practices.brokerage.oms.domain.service.impl.CustomerServiceImpl;
import com.eubican.practices.brokerage.oms.security.properties.ApplicationSecurityProperties;
import com.eubican.practices.brokerage.oms.security.TokenService;
import com.eubican.practices.brokerage.oms.web.dto.LoginRequest;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.*;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
public class TokenServiceTest {

    @Mock
    private CustomerServiceImpl customerService;

    @Mock
    private ApplicationSecurityProperties props;

    @Mock
    private JwtEncoder jwtEncoder;

    @Mock
    private PasswordEncoder passwordEncoder;

    private TokenService tokenService;

    @BeforeEach
    void setup() {
        tokenService = new TokenService(customerService, props, jwtEncoder, passwordEncoder);
    }

    @Test
    void createTokenSuccessfulWhenPasswordMatches() {
        UUID id = UUID.randomUUID();
        Customer customer = Mockito.mock(Customer.class);
        Mockito.when(customer.getId()).thenReturn(id);
        Mockito.when(customer.getEmail()).thenReturn("user@example.com");
        Mockito.when(customer.getPasswordHash()).thenReturn("hashed");
        Mockito.when(customer.getRole()).thenReturn("ROLE_USER");
        Mockito.when(customerService.findByEmail("user@example.com")).thenReturn(customer);
        Mockito.when(passwordEncoder.matches("secret", "hashed")).thenReturn(true);
        Mockito.when(props.getJwtTtlSeconds()).thenReturn(3600L);
        Mockito.when(jwtEncoder.encode(Mockito.any())).thenAnswer(invocation -> new Jwt(
                "token",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("alg", "HS256"),
                Map.of(
                        "sub", id.toString(),
                        "email", "user@example.com",
                        "role", "ROLE_USER"
                )
        ));

        LoginRequest request = new LoginRequest("user@example.com", "secret");
        Jwt jwt = tokenService.createToken(request);

        Assertions.assertThat(jwt).isNotNull();
        Assertions.assertThat(jwt.getSubject()).isEqualTo(id.toString());

        String email = jwt.getClaim("email");
        Assertions.assertThat(email).isEqualTo("user@example.com");

        String role = jwt.getClaim("role");
        Assertions.assertThat(role).isEqualTo("ROLE_USER");

        Assertions.assertThat(jwt.getIssuedAt()).isNotNull();
        Assertions.assertThat(jwt.getExpiresAt()).isNotNull();
        Assertions.assertThat(jwt.getExpiresAt().isAfter(Instant.now())).isTrue();
    }

    @Test
    void createTokenThrowsWhenPasswordMismatch() {
        Customer customer = Mockito.mock(Customer.class);
        Mockito.when(customer.getPasswordHash()).thenReturn("hashed");
        Mockito.when(customerService.findByEmail("user@example.com")).thenReturn(customer);
        Mockito.when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        LoginRequest request = new LoginRequest("user@example.com", "wrong");

        Assertions.assertThatThrownBy(() -> tokenService.createToken(request)).isInstanceOf(ApplicationException.class);
        Assertions.assertThatThrownBy(() -> tokenService.createToken(request)).hasMessage("Invalid credentials");
        Assertions.assertThatThrownBy(() -> tokenService.createToken(request)).hasFieldOrPropertyWithValue("status", HttpStatus.FORBIDDEN);
    }
}
