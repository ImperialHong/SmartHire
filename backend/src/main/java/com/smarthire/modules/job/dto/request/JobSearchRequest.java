package com.smarthire.modules.job.dto.request;

public record JobSearchRequest(
    long page,
    long size,
    String keyword,
    String city,
    String category,
    String status
) {
}
