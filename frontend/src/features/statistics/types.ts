export interface StatisticsOverview {
    scope: string;
    ownerUserId: number | null;
    jobs: {
        total: number;
        byStatus: Record<string, number>;
    };
    applications: {
        total: number;
        withResume: number;
        byStatus: Record<string, number>;
    };
    interviews: {
        total: number;
        upcoming: number;
        byStatus: Record<string, number>;
        byResult: Record<string, number>;
    };
    generatedAt: string;
}
