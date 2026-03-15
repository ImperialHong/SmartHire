import { apiRequest } from "../../../shared/api/client";
import type { PageResponse } from "../../../shared/types/api";
import type { ApplicationItem } from "../types";

export function listMyApplications(token: string) {
    return apiRequest<PageResponse<ApplicationItem>>("/api/applications/me?page=1&size=6", { token });
}
