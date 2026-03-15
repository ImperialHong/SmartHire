import { apiRequest } from "../../../shared/api/client";
import type { AdminUserItem } from "../types";

export function updateAdminUserStatus(token: string, userId: number, status: string) {
    return apiRequest<AdminUserItem>(`/api/admin/users/${userId}/status`, {
        method: "PATCH",
        token,
        body: JSON.stringify({ status })
    });
}
