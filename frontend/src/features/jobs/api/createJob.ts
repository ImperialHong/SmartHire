import { apiRequest } from "../../../shared/api/client";
import type { JobSummary, JobUpsertRequest } from "../types";

export function createJob(token: string, request: JobUpsertRequest) {
    return apiRequest<JobSummary>("/api/jobs", {
        method: "POST",
        token,
        body: JSON.stringify(request)
    });
}
