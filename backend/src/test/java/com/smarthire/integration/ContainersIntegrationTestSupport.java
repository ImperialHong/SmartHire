package com.smarthire.integration;

import com.smarthire.modules.auth.security.AuthenticatedUser;
import com.smarthire.modules.notification.config.NotificationMessagingProperties;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers(disabledWithoutDocker = true)
public abstract class ContainersIntegrationTestSupport {

    @Container
    @SuppressWarnings("resource")
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>(DockerImageName.parse("mysql:8.1"));

    @Container
    @SuppressWarnings("resource")
    private static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7.4-alpine"))
        .withExposedPorts(6379);

    @Container
    @SuppressWarnings("resource")
    private static final RabbitMQContainer RABBITMQ =
        new RabbitMQContainer(DockerImageName.parse("rabbitmq:3.13-management-alpine"));

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Autowired
    protected StringRedisTemplate stringRedisTemplate;

    @Autowired
    protected RabbitTemplate rabbitTemplate;

    @Autowired
    protected NotificationMessagingProperties notificationMessagingProperties;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
        registry.add("spring.flyway.enabled", () -> true);
        registry.add("spring.flyway.baseline-on-migrate", () -> false);
        registry.add("spring.cache.type", () -> "redis");
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", REDIS::getFirstMappedPort);
        registry.add("spring.rabbitmq.dynamic", () -> true);
        registry.add("spring.rabbitmq.host", RABBITMQ::getHost);
        registry.add("spring.rabbitmq.port", RABBITMQ::getAmqpPort);
        registry.add("spring.rabbitmq.username", RABBITMQ::getAdminUsername);
        registry.add("spring.rabbitmq.password", RABBITMQ::getAdminPassword);
        registry.add("spring.rabbitmq.listener.simple.auto-startup", () -> true);
        registry.add("app.jobs.expiration.enabled", () -> false);
    }

    @BeforeEach
    void resetInfrastructureState() {
        purgeNotificationQueues();
        clearRedis();
        deleteBusinessData();
    }

    protected Long insertUser(String email, String fullName, String status) {
        jdbcTemplate.update(
            """
            INSERT INTO users (email, password_hash, full_name, phone, status, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, NOW(), NOW())
            """,
            email,
            "$2a$10$testHashValue123456789012345678901234567890123456",
            fullName,
            "13800138000",
            status
        );
        return jdbcTemplate.queryForObject("SELECT id FROM users WHERE email = ?", Long.class, email);
    }

    protected void assignRole(Long userId, String roleCode) {
        jdbcTemplate.update(
            """
            INSERT INTO user_roles (user_id, role_id, created_at)
            SELECT ?, id, NOW()
            FROM roles
            WHERE code = ?
            """,
            userId,
            roleCode
        );
    }

    protected Long insertJob(
        Long createdBy,
        String title,
        String status,
        LocalDateTime applicationDeadline
    ) {
        jdbcTemplate.update(
            """
            INSERT INTO jobs (
              created_by,
              title,
              description,
              city,
              category,
              employment_type,
              experience_level,
              salary_min,
              salary_max,
              status,
              application_deadline,
              created_at,
              updated_at
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())
            """,
            createdBy,
            title,
            title + " description",
            "Shanghai",
            "Engineering",
            "FULL_TIME",
            "JUNIOR",
            BigDecimal.valueOf(15000),
            BigDecimal.valueOf(25000),
            status,
            applicationDeadline == null ? null : Timestamp.valueOf(applicationDeadline)
        );

        return jdbcTemplate.queryForObject(
            "SELECT id FROM jobs WHERE created_by = ? AND title = ?",
            Long.class,
            createdBy,
            title
        );
    }

    protected AuthenticatedUser authenticatedUser(Long userId, String email, String fullName, String... roles) {
        return new AuthenticatedUser(userId, email, fullName, List.of(roles));
    }

    protected void waitForAssertion(Runnable assertion) {
        waitForAssertion(Duration.ofSeconds(10), assertion);
    }

    protected void waitForAssertion(Duration timeout, Runnable assertion) {
        AssertionError lastAssertionError = null;
        long deadline = System.nanoTime() + timeout.toNanos();

        while (System.nanoTime() < deadline) {
            try {
                assertion.run();
                return;
            } catch (AssertionError error) {
                lastAssertionError = error;
                sleepSilently(150);
            }
        }

        if (lastAssertionError != null) {
            throw lastAssertionError;
        }
    }

    protected <T> void waitForValue(Duration timeout, Supplier<T> supplier, java.util.function.Predicate<T> predicate) {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            T value = supplier.get();
            if (predicate.test(value)) {
                return;
            }
            sleepSilently(150);
        }
        throw new AssertionError("Condition was not met within " + timeout);
    }

    private void purgeNotificationQueues() {
        rabbitTemplate.execute(channel -> {
            channel.queuePurge(notificationMessagingProperties.getQueue());
            channel.queuePurge(notificationMessagingProperties.getDeadLetterQueue());
            return null;
        });
    }

    private void clearRedis() {
        RedisConnectionFactory connectionFactory = stringRedisTemplate.getConnectionFactory();
        if (connectionFactory == null) {
            return;
        }
        try (var connection = connectionFactory.getConnection()) {
            connection.serverCommands().flushDb();
        }
    }

    private void deleteBusinessData() {
        jdbcTemplate.update("DELETE FROM notifications");
        jdbcTemplate.update("DELETE FROM interviews");
        jdbcTemplate.update("DELETE FROM applications");
        jdbcTemplate.update("DELETE FROM operation_logs");
        jdbcTemplate.update("DELETE FROM user_roles");
        jdbcTemplate.update("DELETE FROM jobs");
        jdbcTemplate.update("DELETE FROM users");
    }

    private void sleepSilently(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Interrupted while waiting for async assertion", exception);
        }
    }
}
