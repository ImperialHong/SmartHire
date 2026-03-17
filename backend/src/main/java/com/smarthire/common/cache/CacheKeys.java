package com.smarthire.common.cache;

import com.smarthire.modules.auth.security.AuthenticatedUser;
import com.smarthire.modules.job.dto.request.JobSearchRequest;
import org.springframework.util.StringUtils;

public final class CacheKeys {

    private CacheKeys() {
    }

    public static String jobSearch(JobSearchRequest request) {
        if (request == null) {
            return "jobs:page=1:size=10:keyword=-:city=-:category=-:status=-";
        }

        return "jobs:page=" + request.page()
            + ":size=" + request.size()
            + ":keyword=" + normalize(request.keyword())
            + ":city=" + normalize(request.city())
            + ":category=" + normalize(request.category())
            + ":status=" + normalize(request.status());
    }

    public static String statisticsOverview(AuthenticatedUser currentUser) {
        if (currentUser == null || currentUser.roles() == null) {
            return "statistics:anonymous";
        }

        if (currentUser.roles().contains("ADMIN")) {
            return "statistics:admin";
        }

        return "statistics:hr:user:" + (currentUser.userId() == null ? "unknown" : currentUser.userId());
    }

    private static String normalize(String value) {
        if (!StringUtils.hasText(value)) {
            return "-";
        }
        return value.trim().toLowerCase();
    }
}
