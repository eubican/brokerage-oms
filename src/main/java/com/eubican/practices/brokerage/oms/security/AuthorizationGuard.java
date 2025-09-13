package com.eubican.practices.brokerage.oms.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class AuthorizationGuard {

    public void checkCustomerAccess(UUID requestedCustomerId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException("Unauthenticated");
        }

        if (isAdmin(auth)) {
            return;
        }

        UUID currentCustomerId = resolveCurrentCustomerId(auth);
        if (requestedCustomerId != null && requestedCustomerId.equals(currentCustomerId)) {
            return;
        }

        throw new AccessDeniedException("Access denied");
    }

    public boolean canAccessCustomer(UUID requestedCustomerId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        if (isAdmin(auth)) {
            return true;
        }
        UUID currentCustomerId = resolveCurrentCustomerId(auth);
        return requestedCustomerId != null && requestedCustomerId.equals(currentCustomerId);
    }

    public boolean isAdmin(Authentication auth) {
        for (GrantedAuthority authority : auth.getAuthorities()) {
            if ("ROLE_ADMIN".equals(authority.getAuthority())) {
                return true;
            }
        }
        // Fallback for JWT without mapped authorities: look at claim 'role'
        Object principal = auth.getPrincipal();
        if (principal instanceof Jwt jwt) {
            String role = jwt.getClaim("role");
            return "ROLE_ADMIN".equals(role);
        }
        return false;
    }

    private UUID resolveCurrentCustomerId(Authentication auth) {
        Object principal = auth.getPrincipal();
        if (principal instanceof Jwt jwt) {
            String sub = jwt.getSubject();
            try {
                return UUID.fromString(sub);
            } catch (Exception ignored) {
                return null;
            }
        }
        return null;
    }
}
