package com.eubican.practices.brokerage.oms.security.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "application.security")
public class ApplicationSecurityProperties {

    private String jwtSecret;

    private long jwtTtlSeconds;

    private String adminUser;

    private String adminPassword;

}
