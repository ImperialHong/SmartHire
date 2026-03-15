import { apiRequest } from "../../../shared/api/client";
import type { ApplicationItem, UpdateApplicationStatusRequest } from "../types";

export function updateApplicationStatus(
    token: string,
    applicationId: number,
    request: UpdateApplicationStatusRequest
) {
    return apiRequest<ApplicationItem>(`/api/applications/${applicationId}/status`, {
        method: "PATCH",
        token,
        body: JSON.stringify(request)
    });
}
