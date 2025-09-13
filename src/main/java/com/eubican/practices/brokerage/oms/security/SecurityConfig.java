package com.eubican.practices.brokerage.oms.security;

import com.eubican.practices.brokerage.oms.security.properties.ApplicationSecurityProperties;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Configuration
@RequiredArgsConstructor
@EnableMethodSecurity
public class SecurityConfig {

    private final ApplicationSecurityProperties props;

    private byte[] getSecretKeyBytes() {
        String secret = props.getJwtSecret();
        if (secret == null || secret.isBlank()) {
            // Fallback for tests/dev to avoid startup failure when secret is not provided
            secret = "local-dev-test-secret-change-me";
        }
        return secret.getBytes(StandardCharsets.UTF_8);
    }

    @Bean
    public JwtEncoder jwtEncoder() {
        byte[] secretKeyBytes = getSecretKeyBytes();
        return new NimbusJwtEncoder(new ImmutableSecret<>(secretKeyBytes));
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        byte[] secretKeyBytes = getSecretKeyBytes();
        return NimbusJwtDecoder.withSecretKey(new SecretKeySpec(secretKeyBytes, "HmacSHA256")).build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecurityFilterChain apiSecurityFilterChain(HttpSecurity httpSecurity) throws Exception {
        var config = httpSecurity
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> auth
                        // Carve out actuator health and auth endpoints to be publicly accessible
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        // Secure API endpoints
                        .requestMatchers("/api/**").authenticated()
                );

        // Enable JWT resource server only if a secret is configured
        if (props.getJwtSecret() != null && !props.getJwtSecret().isBlank()) {
            config.oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
        }

        return config.build();
    }

}
