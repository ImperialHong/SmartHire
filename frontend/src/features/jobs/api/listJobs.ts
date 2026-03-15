import { apiRequest } from "../../../shared/api/client";
import type { PageResponse } from "../../../shared/types/api";
import type { JobListParams, JobSummary } from "../types";

export function listJobs(params: JobListParams = {}) {
    const searchParams = new URLSearchParams();
    searchParams.set("page", String(params.page ?? 1));
    searchParams.set("size", String(params.size ?? 24));

    if (params.keyword) {
        searchParams.set("keyword", params.keyword);
    }
    if (params.city) {
        searchParams.set("city", params.city);
    }
    if (params.category) {
        searchParams.set("category", params.category);
    }
    if (params.status) {
        searchParams.set("status", params.status);
    }

    return apiRequest<PageResponse<JobSummary>>(`/api/jobs?${searchParams.toString()}`);
}
