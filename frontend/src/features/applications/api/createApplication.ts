import { apiRequest } from "../../../shared/api/client";
import type { ApplicationItem, CreateApplicationRequest } from "../types";

export function createApplication(token: string, request: CreateApplicationRequest) {
    return apiRequest<ApplicationItem>("/api/applications", {
        method: "POST",
        token,
        body: JSON.stringify(request)
    });
}
