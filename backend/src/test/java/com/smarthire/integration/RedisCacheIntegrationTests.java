package com.smarthire.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.smarthire.modules.auth.security.AuthenticatedUser;
import com.smarthire.modules.job.dto.request.JobUpdateRequest;
import com.smarthire.modules.job.service.JobService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class RedisCacheIntegrationTests extends ContainersIntegrationTestSupport {

    @Autowired
    private JobService jobService;

    @Test
    void jobDetailsShouldBeCachedInRedisAndEvictedAfterUpdate() {
        Long hrUserId = insertUser("cache-hr@example.com", "Cache HR", "ACTIVE");
        assignRole(hrUserId, "HR");
        Long jobId = insertJob(
            hrUserId,
            "Cached Backend Engineer",
            "OPEN",
            LocalDateTime.now().plusDays(10)
        );
        AuthenticatedUser currentUser = authenticatedUser(hrUserId, "cache-hr@example.com", "Cache HR", "HR");

        assertThat(cacheKeys("jobDetails*")).isEmpty();

        jobService.getJob(jobId);

        assertThat(cacheKeys("jobDetails*")).isNotEmpty();

        jobService.updateJob(
            currentUser,
            jobId,
            new JobUpdateRequest(
                "Updated Cached Backend Engineer",
                "Updated description",
                "Shanghai",
                "Engineering",
                "FULL_TIME",
                "MID",
                BigDecimal.valueOf(20000),
                BigDecimal.valueOf(30000),
                "OPEN",
                LocalDateTime.now().plusDays(20)
            )
        );

        assertThat(cacheKeys("jobDetails*")).isEmpty();
    }

    private Set<String> cacheKeys(String pattern) {
        Set<String> keys = stringRedisTemplate.keys(pattern);
        return keys == null ? Set.of() : keys;
    }
}
