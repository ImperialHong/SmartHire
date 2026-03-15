import { apiRequest } from "../../../shared/api/client";
import type { PageResponse } from "../../../shared/types/api";
import type { JobSummary } from "../types";

export function listPublicJobs() {
    return apiRequest<PageResponse<JobSummary>>("/api/jobs?page=1&size=9&status=OPEN");
}
