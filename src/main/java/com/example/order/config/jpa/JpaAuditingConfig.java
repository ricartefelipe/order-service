package com.example.order.config.jpa;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

/**
 * Audit timestamps are truncated to microseconds, the precision of PostgreSQL {@code timestamptz},
 * so the response of the creating request equals every later read of the same order.
 */
@Configuration
@EnableJpaAuditing(dateTimeProviderRef = "microsecondDateTimeProvider")
public class JpaAuditingConfig {

    @Bean
    DateTimeProvider microsecondDateTimeProvider() {
        return () -> Optional.of(Instant.now().truncatedTo(ChronoUnit.MICROS));
    }
}
