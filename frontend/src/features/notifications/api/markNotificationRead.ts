import { apiRequest } from "../../../shared/api/client";
import type { NotificationItem } from "../types";

export function markNotificationRead(token: string, id: number) {
    return apiRequest<NotificationItem>(`/api/notifications/${id}/read`, {
        method: "PATCH",
        token
    });
}
