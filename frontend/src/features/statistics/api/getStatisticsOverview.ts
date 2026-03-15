import { apiRequest } from "../../../shared/api/client";
import type { StatisticsOverview } from "../types";

export function getStatisticsOverview(token: string) {
    return apiRequest<StatisticsOverview>("/api/statistics/overview", { token });
}
