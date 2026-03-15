export interface ApiResponse<T> {
    success: boolean;
    code: string;
    message: string;
    data: T;
}

export interface PageResponse<T> {
    records: T[];
    page: number;
    size: number;
    total: number;
}
