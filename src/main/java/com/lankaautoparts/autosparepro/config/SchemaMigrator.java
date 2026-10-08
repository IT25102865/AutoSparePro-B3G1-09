package com.lankaautoparts.autosparepro.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Small startup fix-ups for databases created by an earlier version of the app.
 *
 * <p>{@code spring.jpa.hibernate.ddl-auto=update} adds missing tables and
 * columns but never widens an existing one. {@code part.image_url} was first
 * created as VARCHAR(255), which can't hold long image links (Google Images,
 * CDN URLs, ...), so it is widened here to match {@code Part.imageUrl}.
 *
 * <p>(Enum columns used to need the same treatment: Hibernate created them as
 * native MySQL ENUM columns frozen to the values that existed at the time, so
 * adding {@code PaymentStatus.REFUNDED} later made every refund fail with
 * "Data truncated". The entities now map enums with
 * {@code @JdbcTypeCode(SqlTypes.VARCHAR)}, and Hibernate converts the old ENUM
 * columns to plain VARCHAR by itself on startup.)
 *
 * Idempotent, and a failure never stops the application from starting.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SchemaMigrator implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SchemaMigrator.class);

    private final JdbcTemplate jdbcTemplate;

    public SchemaMigrator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        widenImageUrlColumn();
    }

    private void widenImageUrlColumn() {
        try {
            jdbcTemplate.execute("ALTER TABLE part MODIFY COLUMN image_url VARCHAR(2048)");
        } catch (Exception e) {
            log.debug("Skipping image_url widening: {}", e.getMessage());
        }
    }
}
