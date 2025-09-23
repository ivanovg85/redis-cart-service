package org.example.rediscartservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "cart")
public class CartIdleProperties {
    /** e.g. 5m, 300s, PT5M */
    private Duration idleTtl = Duration.ofMinutes(5);
    /** Use Redis 7.4+ H(PEXPIRE) field TTL; otherwise fallback to key PEXPIRE. */
    private boolean useFieldTtl = true;
}