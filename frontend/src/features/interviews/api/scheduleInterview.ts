import { apiRequest } from "../../../shared/api/client";
import type { InterviewItem, ScheduleInterviewRequest } from "../types";

export function scheduleInterview(token: string, request: ScheduleInterviewRequest) {
    return apiRequest<InterviewItem>("/api/interviews", {
        method: "POST",
        token,
        body: JSON.stringify(request)
    });
}
