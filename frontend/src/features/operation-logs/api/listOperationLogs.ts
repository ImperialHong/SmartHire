import { apiRequest } from "../../../shared/api/client";
import type { PageResponse } from "../../../shared/types/api";
import type { OperationLogItem, OperationLogListParams } from "../types";

export function listOperationLogs(token: string, params: OperationLogListParams = {}) {
    const searchParams = new URLSearchParams();
    searchParams.set("page", String(params.page ?? 1));
    searchParams.set("size", String(params.size ?? 12));

    if (params.action) {
        searchParams.set("action", params.action);
    }
    if (params.targetType) {
        searchParams.set("targetType", params.targetType);
    }
    if (params.operatorUserId != null) {
        searchParams.set("operatorUserId", String(params.operatorUserId));
    }

    return apiRequest<PageResponse<OperationLogItem>>(`/api/admin/operation-logs?${searchParams.toString()}`, {
        token
    });
}
