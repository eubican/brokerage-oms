package com.eubican.practices.brokerage.oms.security;

import com.eubican.practices.brokerage.oms.domain.exception.ApplicationException;
import com.eubican.practices.brokerage.oms.domain.model.Customer;
import com.eubican.practices.brokerage.oms.domain.service.CustomerService;
import com.eubican.practices.brokerage.oms.web.dto.LoginRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class TokenService {

    private final CustomerService customerService;

    private final ApplicationSecurityProps props;

    private final JwtEncoder jwtEncoder;

    private final PasswordEncoder passwordEncoder;

    public Jwt createToken(LoginRequest request) {
        Customer customer = customerService.findByEmail(request.email());

        if (!passwordEncoder.matches(request.password(), customer.getPasswordHash())) {
            throw new ApplicationException(HttpStatus.FORBIDDEN, "Invalid credentials");
        }

        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(props.getJwtTtlSeconds());

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(customer.getId().toString())
                .issuedAt(now)
                .expiresAt(expiresAt)
                .claim("email", customer.getEmail())
                .claim("role", customer.getRole())
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims));
    }

}
