export interface JobSummary {
    id: number;
    createdBy: number;
    title: string;
    description: string;
    city: string | null;
    category: string | null;
    employmentType: string | null;
    experienceLevel: string | null;
    salaryMin: number | null;
    salaryMax: number | null;
    status: string;
    applicationDeadline: string | null;
    createdAt: string;
    updatedAt: string;
}
