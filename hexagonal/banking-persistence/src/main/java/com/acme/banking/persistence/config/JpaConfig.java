package com.acme.banking.persistence.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(basePackages = "com.acme.banking.infrastructure.persistence")
@EntityScan(basePackages = "com.acme.banking.infrastructure.persistence")
public class JpaConfig {
    // Generated JPA entities and repositories routed from reactor
}
