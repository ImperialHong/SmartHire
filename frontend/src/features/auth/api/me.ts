import { apiRequest } from "../../../shared/api/client";
import type { AuthUser } from "../types";

export function getCurrentUser(token: string) {
    return apiRequest<AuthUser>("/api/auth/me", { token });
}
