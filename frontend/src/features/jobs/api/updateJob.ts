import { apiRequest } from "../../../shared/api/client";
import type { JobSummary, JobUpsertRequest } from "../types";

export function updateJob(token: string, jobId: number, request: JobUpsertRequest) {
    return apiRequest<JobSummary>(`/api/jobs/${jobId}`, {
        method: "PUT",
        token,
        body: JSON.stringify(request)
    });
}
