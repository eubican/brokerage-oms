package com.eubican.practices.brokerage.oms.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "application.order-service")
public class OrderServiceProperties {
    private int optimisticLockMaxRetries = 3; // default value
}
