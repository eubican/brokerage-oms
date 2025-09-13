package com.eubican.practices.brokerage.oms.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "application.security")
public class ApplicationSecurityProps {

    private String jwtSecret;

    private long jwtTtlSeconds;

    private String adminUser;

    private String adminPassword;

}
