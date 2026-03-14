package com.smarthire.common.api;

import java.util.List;

public record PageResponse<T>(
    List<T> records,
    long page,
    long size,
    long total,
    long totalPages
) {

    public static <T> PageResponse<T> of(List<T> records, long page, long size, long total) {
        long totalPages = size <= 0 ? 0 : (long) Math.ceil((double) total / size);
        return new PageResponse<>(records, page, size, total, totalPages);
    }
}
