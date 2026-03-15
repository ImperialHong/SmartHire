import { apiRequest } from "../../../shared/api/client";
import type { NotificationUnreadCount } from "../types";

export function getUnreadCount(token: string) {
    return apiRequest<NotificationUnreadCount>("/api/notifications/unread-count", { token });
}
