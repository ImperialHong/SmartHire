import { apiRequest } from "../../../shared/api/client";

export function deleteJob(token: string, jobId: number) {
    return apiRequest<null>(`/api/jobs/${jobId}`, {
        method: "DELETE",
        token
    });
}
