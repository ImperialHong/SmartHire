import { apiRequest } from "../../../shared/api/client";
import type { AuthResponse, RegisterRequest } from "../types";

export function register(request: RegisterRequest) {
    return apiRequest<AuthResponse>("/api/auth/register", {
        method: "POST",
        body: JSON.stringify(request)
    });
}
