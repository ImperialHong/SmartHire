import { apiRequest } from "../../../shared/api/client";
import type { PageResponse } from "../../../shared/types/api";
import type { ApplicationItem } from "../types";

export function listJobApplications(token: string, jobId: number) {
    return apiRequest<PageResponse<ApplicationItem>>(`/api/jobs/${jobId}/applications?page=1&size=12`, {
        token
    });
}
