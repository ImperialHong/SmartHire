import { apiRequest } from "../../../shared/api/client";
import type { PageResponse } from "../../../shared/types/api";
import type { InterviewItem } from "../types";

export function listMyInterviews(token: string) {
    return apiRequest<PageResponse<InterviewItem>>("/api/interviews/me?page=1&size=6", { token });
}
