export interface ApplicationItem {
    id: number;
    jobId: number;
    jobTitle: string | null;
    jobCity: string | null;
    jobCategory: string | null;
    candidateId: number;
    candidateName: string | null;
    candidateEmail: string | null;
    status: string;
    resumeFilePath: string | null;
    coverLetter: string | null;
    hrNote: string | null;
    appliedAt: string;
    statusUpdatedAt: string | null;
    lastUpdatedBy: number | null;
    createdAt: string;
    updatedAt: string;
}

export interface CreateApplicationRequest {
    jobId: number;
    resumeFilePath: string | null;
    coverLetter: string | null;
}

export interface UpdateApplicationStatusRequest {
    status: string;
    hrNote: string | null;
}
