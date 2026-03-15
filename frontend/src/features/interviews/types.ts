export interface InterviewItem {
    id: number;
    applicationId: number;
    jobId: number | null;
    jobTitle: string | null;
    candidateId: number | null;
    candidateName: string | null;
    candidateEmail: string | null;
    applicationStatus: string | null;
    scheduledBy: number;
    interviewAt: string;
    location: string | null;
    meetingLink: string | null;
    status: string;
    result: string;
    remark: string | null;
    createdAt: string;
    updatedAt: string;
}

export interface ScheduleInterviewRequest {
    applicationId: number;
    interviewAt: string;
    location: string | null;
    meetingLink: string | null;
    remark: string | null;
}

export interface UpdateInterviewRequest {
    interviewAt: string | null;
    location: string | null;
    meetingLink: string | null;
    status: string | null;
    result: string | null;
    remark: string | null;
}
