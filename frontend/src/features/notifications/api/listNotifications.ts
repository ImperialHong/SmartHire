import { apiRequest } from "../../../shared/api/client";
import type { PageResponse } from "../../../shared/types/api";
import type { NotificationItem } from "../types";

export function listNotifications(token: string) {
    return apiRequest<PageResponse<NotificationItem>>("/api/notifications?page=1&size=6", { token });
}
