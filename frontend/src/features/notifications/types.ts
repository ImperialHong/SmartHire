export interface NotificationItem {
    id: number;
    type: string;
    title: string;
    content: string;
    relatedType: string | null;
    relatedId: number | null;
    isRead: boolean;
    readAt: string | null;
    createdAt: string;
    updatedAt: string;
}

export interface NotificationUnreadCount {
    unreadCount: number;
}
