export interface AdminUserItem {
    id: number;
    email: string;
    fullName: string;
    phone: string | null;
    status: string;
    lastLoginAt: string | null;
    createdAt: string;
    updatedAt: string;
    roles: string[];
}

export interface AdminJobItem {
    id: number;
    createdBy: number;
    ownerName: string | null;
    ownerEmail: string | null;
    ownerStatus: string | null;
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

export interface AdminUserListParams {
    page?: number;
    size?: number;
    keyword?: string;
    status?: string;
    roleCode?: string;
}

export interface AdminJobListParams {
    page?: number;
    size?: number;
    keyword?: string;
    status?: string;
    ownerKeyword?: string;
}
