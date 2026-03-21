import type { ApiResponse } from "../types/api";

const apiBaseUrl = (import.meta.env.VITE_API_BASE_URL || "").replace(/\/$/, "");

export class ApiError extends Error {
    code?: string;
    status?: number;

    constructor(message: string, options?: { code?: string; status?: number }) {
        super(message);
        this.name = "ApiError";
        this.code = options?.code;
        this.status = options?.status;
    }
}

export function getBackendOrigin() {
    if (import.meta.env.VITE_BACKEND_ORIGIN) {
        return import.meta.env.VITE_BACKEND_ORIGIN.replace(/\/$/, "");
    }

    if (typeof window !== "undefined" && window.location?.origin) {
        return window.location.origin.replace(/\/$/, "");
    }

    return "http://localhost:8080";
}

export async function apiRequest<T>(
    path: string,
    options: RequestInit & { token?: string } = {}
): Promise<T> {
    const { token, headers, ...rest } = options;
    const normalizedPath = path.startsWith("/") ? path : `/${path}`;
    const requestHeaders = new Headers(headers || {});

    if (token) {
        requestHeaders.set("Authorization", `Bearer ${token}`);
    }

    if (rest.body && !(rest.body instanceof FormData) && !requestHeaders.has("Content-Type")) {
        requestHeaders.set("Content-Type", "application/json");
    }

    const response = await fetch(`${apiBaseUrl}${normalizedPath}`, {
        ...rest,
        headers: requestHeaders
    });
    const payload = (await response.json().catch(() => null)) as ApiResponse<T> | null;

    if (!response.ok || !payload?.success) {
        throw new ApiError(payload?.message || `Request failed with status ${response.status}`, {
            code: payload?.code,
            status: response.status
        });
    }

    return payload.data;
}
