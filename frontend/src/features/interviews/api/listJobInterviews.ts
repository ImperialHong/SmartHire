import { apiRequest } from "../../../shared/api/client";
import type { PageResponse } from "../../../shared/types/api";
import type { InterviewItem } from "../types";

export function listJobInterviews(token: string, jobId: number) {
    return apiRequest<PageResponse<InterviewItem>>(`/api/jobs/${jobId}/interviews?page=1&size=12`, {
        token
    });
}
