import { apiRequest } from "../../../shared/api/client";
import type { InterviewItem, UpdateInterviewRequest } from "../types";

export function updateInterview(token: string, interviewId: number, request: UpdateInterviewRequest) {
    return apiRequest<InterviewItem>(`/api/interviews/${interviewId}`, {
        method: "PATCH",
        token,
        body: JSON.stringify(request)
    });
}
