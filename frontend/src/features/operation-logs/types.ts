export interface OperationLogItem {
    id: number;
    operatorUserId: number | null;
    operatorEmail: string | null;
    operatorName: string | null;
    operatorRoles: string[];
    action: string;
    targetType: string;
    targetId: number | null;
    details: string | null;
    createdAt: string;
}

export interface OperationLogListParams {
    page?: number;
    size?: number;
    action?: string;
    targetType?: string;
    operatorUserId?: number | null;
}
