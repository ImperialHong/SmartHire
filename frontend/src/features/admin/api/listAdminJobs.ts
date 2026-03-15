import { apiRequest } from "../../../shared/api/client";
import type { PageResponse } from "../../../shared/types/api";
import type { AdminJobItem, AdminJobListParams } from "../types";

export function listAdminJobs(token: string, params: AdminJobListParams = {}) {
    const searchParams = new URLSearchParams();
    searchParams.set("page", String(params.page ?? 1));
    searchParams.set("size", String(params.size ?? 12));

    if (params.keyword) {
        searchParams.set("keyword", params.keyword);
    }
    if (params.status) {
        searchParams.set("status", params.status);
    }
    if (params.ownerKeyword) {
        searchParams.set("ownerKeyword", params.ownerKeyword);
    }

    return apiRequest<PageResponse<AdminJobItem>>(`/api/admin/jobs?${searchParams.toString()}`, {
        token
    });
}
