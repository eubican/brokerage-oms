package com.eubican.practices.brokerage.oms.unit;

import com.eubican.practices.brokerage.oms.security.AuthorizationGuard;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AuthorizationGuardTest {

    private final AuthorizationGuard guard = new AuthorizationGuard();

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void canAccessCustomerReturnsFalseWhenUnauthenticated() {
        SecurityContextHolder.clearContext();
        Assertions.assertThat(guard.canAccessCustomer(UUID.randomUUID())).isFalse();
    }

    @Test
    void checkCustomerAccessThrowsWhenUnauthenticated() {
        SecurityContextHolder.clearContext();
        Assertions.assertThatThrownBy(() -> guard.checkCustomerAccess(UUID.randomUUID()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void adminAuthorityGrantsAccess() {
        Authentication auth = new TestingAuthenticationToken("user", "n/a", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        auth.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(auth);

        UUID requested = UUID.randomUUID();
        Assertions.assertThat(guard.canAccessCustomer(requested)).isTrue();
        Assertions.assertThatCode(() -> guard.checkCustomerAccess(requested)).doesNotThrowAnyException();
    }

    @Test
    void customerIdMustMatchForNonAdmin() {
        UUID sub = UUID.randomUUID();
        Jwt jwt = new Jwt(
                "token",
                Instant.now(),
                Instant.now().plusSeconds(60),
                Map.of("alg", "HS256"),
                Map.of("sub", sub.toString())
        );
        Authentication auth = new TestingAuthenticationToken(jwt, "n/a", List.of(new SimpleGrantedAuthority("ROLE_USER")));
        auth.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(auth);

        Assertions.assertThat(guard.canAccessCustomer(sub)).isTrue();
        Assertions.assertThatCode(() -> guard.checkCustomerAccess(sub)).doesNotThrowAnyException();

        UUID other = UUID.randomUUID();
        Assertions.assertThat(guard.canAccessCustomer(other)).isFalse();
        Assertions.assertThatThrownBy(() -> guard.checkCustomerAccess(other))
                .isInstanceOf(AccessDeniedException.class);
    }
}
