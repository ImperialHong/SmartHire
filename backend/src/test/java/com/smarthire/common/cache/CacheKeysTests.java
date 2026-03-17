package com.smarthire.common.cache;

import static org.assertj.core.api.Assertions.assertThat;

import com.smarthire.modules.auth.security.AuthenticatedUser;
import com.smarthire.modules.job.dto.request.JobSearchRequest;
import java.util.List;
import org.junit.jupiter.api.Test;

class CacheKeysTests {

    @Test
    void shouldBuildStableJobSearchKey() {
        JobSearchRequest request = new JobSearchRequest(2, 20, " Java ", " Sydney ", " Backend ", " OPEN ");

        String key = CacheKeys.jobSearch(request);

        assertThat(key).isEqualTo(
            "jobs:page=2:size=20:keyword=java:city=sydney:category=backend:status=open"
        );
    }

    @Test
    void shouldBuildAdminStatisticsScopeKey() {
        AuthenticatedUser currentUser = new AuthenticatedUser(9L, "admin@example.com", "Admin", List.of("ADMIN"));

        assertThat(CacheKeys.statisticsOverview(currentUser)).isEqualTo("statistics:admin");
    }

    @Test
    void shouldBuildHrStatisticsScopeKey() {
        AuthenticatedUser currentUser = new AuthenticatedUser(7L, "hr@example.com", "HR", List.of("HR"));

        assertThat(CacheKeys.statisticsOverview(currentUser)).isEqualTo("statistics:hr:user:7");
    }
}
