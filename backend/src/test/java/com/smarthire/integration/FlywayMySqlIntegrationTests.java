package com.smarthire.integration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class FlywayMySqlIntegrationTests extends ContainersIntegrationTestSupport {

    @Test
    void flywayShouldCreateSchemaHistoryAndCoreObjects() {
        Integer migrationCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM flyway_schema_history WHERE success = TRUE",
            Integer.class
        );
        Integer roleCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM roles",
            Integer.class
        );
        Integer operationLogTableCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'operation_logs'",
            Integer.class
        );
        Integer eventKeyColumnCount = jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'notifications'
              AND column_name = 'event_key'
            """,
            Integer.class
        );

        assertThat(migrationCount).isEqualTo(3);
        assertThat(roleCount).isEqualTo(3);
        assertThat(operationLogTableCount).isEqualTo(1);
        assertThat(eventKeyColumnCount).isEqualTo(1);
    }
}
