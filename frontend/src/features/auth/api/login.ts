import { apiRequest } from "../../../shared/api/client";
import type { AuthResponse, LoginRequest } from "../types";

export function login(request: LoginRequest) {
    return apiRequest<AuthResponse>("/api/auth/login", {
        method: "POST",
        body: JSON.stringify(request)
    });
}
