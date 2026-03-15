import { apiRequest } from "../../../shared/api/client";
import type { PageResponse } from "../../../shared/types/api";
import type { AdminUserItem, AdminUserListParams } from "../types";

export function listAdminUsers(token: string, params: AdminUserListParams = {}) {
    const searchParams = new URLSearchParams();
    searchParams.set("page", String(params.page ?? 1));
    searchParams.set("size", String(params.size ?? 12));

    if (params.keyword) {
        searchParams.set("keyword", params.keyword);
    }
    if (params.status) {
        searchParams.set("status", params.status);
    }
    if (params.roleCode) {
        searchParams.set("roleCode", params.roleCode);
    }

    return apiRequest<PageResponse<AdminUserItem>>(`/api/admin/users?${searchParams.toString()}`, {
        token
    });
}
