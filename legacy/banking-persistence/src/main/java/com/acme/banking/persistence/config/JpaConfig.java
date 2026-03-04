package com.acme.banking.persistence.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(basePackages = "com.acme.banking.persistence.repository")
public class JpaConfig {
    // JPA repositories auto-configured by Spring Boot
}
