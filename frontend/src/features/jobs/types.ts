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

export interface JobUpsertRequest {
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
}

export interface JobListParams {
    page?: number;
    size?: number;
    keyword?: string;
    city?: string;
    category?: string;
    status?: string;
}
