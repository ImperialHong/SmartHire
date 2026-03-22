import { useEffect, useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useAuth } from "../../app/providers/AuthProvider";
import { listJobApplications } from "../../features/applications/api/listJobApplications";
import { updateApplicationStatus } from "../../features/applications/api/updateApplicationStatus";
import type { ApplicationItem } from "../../features/applications/types";
import { createJob } from "../../features/jobs/api/createJob";
import { deleteJob } from "../../features/jobs/api/deleteJob";
import { listJobs } from "../../features/jobs/api/listJobs";
import { updateJob } from "../../features/jobs/api/updateJob";
import type { JobSummary, JobUpsertRequest } from "../../features/jobs/types";
import { listJobInterviews } from "../../features/interviews/api/listJobInterviews";
import { scheduleInterview } from "../../features/interviews/api/scheduleInterview";
import { updateInterview } from "../../features/interviews/api/updateInterview";
import type { InterviewItem, ScheduleInterviewRequest, UpdateInterviewRequest } from "../../features/interviews/types";
import { getStatisticsOverview } from "../../features/statistics/api/getStatisticsOverview";
import { buildDistributionItems, readCount } from "../../features/statistics/utils";
import { DistributionChart } from "../../shared/components/DistributionChart";
import { EmptyState } from "../../shared/components/EmptyState";
import { ProgressStatCard } from "../../shared/components/ProgressStatCard";
import { SectionCard } from "../../shared/components/SectionCard";
import { StatusPill } from "../../shared/components/StatusPill";
import { formatDateTime, formatSalaryRange } from "../../shared/lib/formatters";

const JOB_STATUS_OPTIONS = ["OPEN", "CLOSED", "EXPIRED"] as const;
const EMPLOYMENT_TYPE_OPTIONS = ["FULL_TIME", "PART_TIME", "CONTRACT", "INTERNSHIP"] as const;
const EXPERIENCE_LEVEL_OPTIONS = ["ENTRY", "JUNIOR", "MID", "SENIOR"] as const;
const APPLICATION_STATUS_OPTIONS = ["APPLIED", "REVIEWING", "INTERVIEW", "OFFERED", "REJECTED"] as const;
const INTERVIEW_STATUS_OPTIONS = ["SCHEDULED", "COMPLETED", "CANCELLED"] as const;
const INTERVIEW_RESULT_OPTIONS = ["PENDING", "PASSED", "FAILED"] as const;
const MAX_HR_NOTE_LENGTH = 2000;
const MAX_INTERVIEW_REMARK_LENGTH = 500;
const MAX_INTERVIEW_FIELD_LENGTH = 255;

interface JobFormState {
    title: string;
    description: string;
    city: string;
    category: string;
    employmentType: string;
    experienceLevel: string;
    salaryMin: string;
    salaryMax: string;
    status: string;
    applicationDeadline: string;
}

interface ReviewFormState {
    status: string;
    hrNote: string;
}

interface InterviewFormState {
    interviewAt: string;
    location: string;
    meetingLink: string;
    remark: string;
    status: string;
    result: string;
}

function emptyJobForm(): JobFormState {
    return {
        title: "",
        description: "",
        city: "",
        category: "",
        employmentType: "FULL_TIME",
        experienceLevel: "JUNIOR",
        salaryMin: "",
        salaryMax: "",
        status: "OPEN",
        applicationDeadline: ""
    };
}

function emptyReviewForm(): ReviewFormState {
    return {
        status: "REVIEWING",
        hrNote: ""
    };
}

function emptyInterviewForm(): InterviewFormState {
    return {
        interviewAt: "",
        location: "",
        meetingLink: "",
        remark: "",
        status: "SCHEDULED",
        result: "PENDING"
    };
}

function normalizeText(value: string) {
    const trimmed = value.trim();
    return trimmed ? trimmed : null;
}

function normalizeNumber(value: string) {
    const trimmed = value.trim();
    if (!trimmed) {
        return null;
    }
    const numericValue = Number(trimmed);
    return Number.isFinite(numericValue) ? numericValue : null;
}

function toDateTimeLocal(value: string | null | undefined) {
    if (!value) {
        return "";
    }
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) {
        return value.slice(0, 16);
    }

    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, "0");
    const day = String(date.getDate()).padStart(2, "0");
    const hours = String(date.getHours()).padStart(2, "0");
    const minutes = String(date.getMinutes()).padStart(2, "0");
    return `${year}-${month}-${day}T${hours}:${minutes}`;
}

function buildJobForm(job: JobSummary): JobFormState {
    return {
        title: job.title,
        description: job.description,
        city: job.city || "",
        category: job.category || "",
        employmentType: job.employmentType || "FULL_TIME",
        experienceLevel: job.experienceLevel || "JUNIOR",
        salaryMin: job.salaryMin == null ? "" : String(job.salaryMin),
        salaryMax: job.salaryMax == null ? "" : String(job.salaryMax),
        status: job.status,
        applicationDeadline: toDateTimeLocal(job.applicationDeadline)
    };
}

function buildJobRequest(form: JobFormState): JobUpsertRequest {
    return {
        title: form.title.trim(),
        description: form.description.trim(),
        city: normalizeText(form.city),
        category: normalizeText(form.category),
        employmentType: normalizeText(form.employmentType),
        experienceLevel: normalizeText(form.experienceLevel),
        salaryMin: normalizeNumber(form.salaryMin),
        salaryMax: normalizeNumber(form.salaryMax),
        status: form.status,
        applicationDeadline: form.applicationDeadline || null
    };
}

function buildReviewForm(application: ApplicationItem): ReviewFormState {
    return {
        status: application.status,
        hrNote: application.hrNote || ""
    };
}

function buildInterviewForm(interview: InterviewItem): InterviewFormState {
    return {
        interviewAt: toDateTimeLocal(interview.interviewAt),
        location: interview.location || "",
        meetingLink: interview.meetingLink || "",
        remark: interview.remark || "",
        status: interview.status,
        result: interview.result
    };
}

function validateJobForm(form: JobFormState) {
    if (!form.title.trim()) {
        return "Job title is required.";
    }
    if (!form.description.trim()) {
        return "Job description is required.";
    }

    const salaryMin = form.salaryMin.trim();
    const salaryMax = form.salaryMax.trim();
    if (salaryMin && Number.isNaN(Number(salaryMin))) {
        return "Salary min must be a valid number.";
    }
    if (salaryMax && Number.isNaN(Number(salaryMax))) {
        return "Salary max must be a valid number.";
    }
    if (salaryMin && salaryMax && Number(salaryMin) > Number(salaryMax)) {
        return "Salary max must be greater than or equal to salary min.";
    }

    return null;
}

function validateReviewForm(form: ReviewFormState) {
    if (form.hrNote.trim().length > MAX_HR_NOTE_LENGTH) {
        return `Recruiter note must stay within ${MAX_HR_NOTE_LENGTH} characters.`;
    }

    return null;
}

function validateInterviewForm(form: InterviewFormState, isUpdate: boolean) {
    if (!form.interviewAt.trim()) {
        return isUpdate ? "Interview time is still required when updating the schedule." : "Choose an interview time before scheduling.";
    }

    const interviewAt = new Date(form.interviewAt);
    if (Number.isNaN(interviewAt.getTime())) {
        return "Interview time is invalid.";
    }
    if (interviewAt.getTime() <= Date.now()) {
        return "Interview time must be in the future.";
    }
    if (form.location.trim().length > MAX_INTERVIEW_FIELD_LENGTH) {
        return "Interview location must stay within 255 characters.";
    }
    if (form.meetingLink.trim().length > MAX_INTERVIEW_FIELD_LENGTH) {
        return "Meeting link must stay within 255 characters.";
    }
    if (form.remark.trim().length > MAX_INTERVIEW_REMARK_LENGTH) {
        return `Interview remark must stay within ${MAX_INTERVIEW_REMARK_LENGTH} characters.`;
    }

    return null;
}

function getStatusTone(status: string) {
    if (["OPEN", "OFFERED", "PASSED", "COMPLETED"].includes(status)) {
        return "good" as const;
    }
    if (["REVIEWING", "INTERVIEW", "SCHEDULED", "PENDING"].includes(status)) {
        return "info" as const;
    }
    if (["CLOSED", "EXPIRED", "REJECTED", "FAILED", "CANCELLED"].includes(status)) {
        return "warn" as const;
    }
    return "default" as const;
}

export function HrDashboardPage() {
    const { session } = useAuth();
    const queryClient = useQueryClient();

    const [selectedJobId, setSelectedJobId] = useState<number | null>(null);
    const [selectedApplicationId, setSelectedApplicationId] = useState<number | null>(null);
    const [jobForm, setJobForm] = useState<JobFormState>(emptyJobForm);
    const [reviewForm, setReviewForm] = useState<ReviewFormState>(emptyReviewForm);
    const [interviewForm, setInterviewForm] = useState<InterviewFormState>(emptyInterviewForm);
    const [feedback, setFeedback] = useState<{ tone: "good" | "warn" | "info"; text: string } | null>(null);
    const [jobFormMessage, setJobFormMessage] = useState<string | null>(null);
    const [reviewFormMessage, setReviewFormMessage] = useState<string | null>(null);
    const [interviewFormMessage, setInterviewFormMessage] = useState<string | null>(null);

    const statsQuery = useQuery({
        queryKey: ["statistics-overview", "hr", session?.token],
        queryFn: () => getStatisticsOverview(session!.token),
        enabled: Boolean(session?.token)
    });

    const jobsQuery = useQuery({
        queryKey: ["hr-owned-jobs", session?.user.id],
        queryFn: () => listJobs({ page: 1, size: 50 }),
        enabled: Boolean(session?.user.id)
    });

    const ownedJobs = useMemo(
        () => jobsQuery.data?.records.filter(job => job.createdBy === session?.user.id) || [],
        [jobsQuery.data?.records, session?.user.id]
    );

    const selectedJob = useMemo(
        () => ownedJobs.find(job => job.id === selectedJobId) || null,
        [ownedJobs, selectedJobId]
    );

    const applicationsQuery = useQuery({
        queryKey: ["hr-job-applications", session?.token, selectedJobId],
        queryFn: () => listJobApplications(session!.token, selectedJobId!),
        enabled: Boolean(session?.token && selectedJobId)
    });

    const interviewsQuery = useQuery({
        queryKey: ["hr-job-interviews", session?.token, selectedJobId],
        queryFn: () => listJobInterviews(session!.token, selectedJobId!),
        enabled: Boolean(session?.token && selectedJobId)
    });

    const selectedApplication = useMemo(
        () => applicationsQuery.data?.records.find(application => application.id === selectedApplicationId) || null,
        [applicationsQuery.data?.records, selectedApplicationId]
    );

    const interviewByApplicationId = useMemo(
        () => new Map((interviewsQuery.data?.records || []).map(interview => [interview.applicationId, interview])),
        [interviewsQuery.data?.records]
    );

    const selectedInterview = selectedApplication ? interviewByApplicationId.get(selectedApplication.id) || null : null;
    const applicationCount = applicationsQuery.data?.records.length || 0;
    const interviewCount = interviewsQuery.data?.records.length || 0;
    const hrMetrics = [
        { label: "Owned jobs", value: ownedJobs.length },
        { label: "Open jobs", value: ownedJobs.filter(job => job.status === "OPEN").length },
        { label: "Applicants on selected job", value: selectedJob ? applicationCount : "--" },
        { label: "Interviews on selected job", value: selectedJob ? interviewCount : "--" }
    ];
    const hrAnalytics = statsQuery.data ? {
        progressCards: [
            {
                label: "Open jobs share",
                numerator: readCount(statsQuery.data.jobs.byStatus, "OPEN"),
                denominator: statsQuery.data.jobs.total,
                helper: "How much of your portfolio is actively accepting applications."
            },
            {
                label: "Resume coverage",
                numerator: statsQuery.data.applications.withResume,
                denominator: statsQuery.data.applications.total,
                helper: "Applicants who already uploaded a resume."
            },
            {
                label: "Upcoming interviews",
                numerator: statsQuery.data.interviews.upcoming,
                denominator: statsQuery.data.interviews.total,
                helper: "Scheduled conversations still ahead of you."
            }
        ],
        charts: [
            {
                title: "Job status mix",
                subtitle: "How your current postings are distributed.",
                items: buildDistributionItems(statsQuery.data.jobs.byStatus, [...JOB_STATUS_OPTIONS], getStatusTone)
            },
            {
                title: "Application pipeline",
                subtitle: "Where active applicants currently sit.",
                items: buildDistributionItems(statsQuery.data.applications.byStatus, [...APPLICATION_STATUS_OPTIONS], getStatusTone)
            },
            {
                title: "Interview status",
                subtitle: "Scheduled, completed, and cancelled interviews.",
                items: buildDistributionItems(statsQuery.data.interviews.byStatus, [...INTERVIEW_STATUS_OPTIONS], getStatusTone)
            },
            {
                title: "Interview outcomes",
                subtitle: "Result mix across finished interviews.",
                items: buildDistributionItems(statsQuery.data.interviews.byResult, [...INTERVIEW_RESULT_OPTIONS], getStatusTone)
            }
        ]
    } : null;

    useEffect(() => {
        if (!ownedJobs.length) {
            setSelectedJobId(null);
            return;
        }

        setSelectedJobId(current => (
            current && ownedJobs.some(job => job.id === current) ? current : ownedJobs[0].id
        ));
    }, [ownedJobs]);

    useEffect(() => {
        setJobForm(selectedJob ? buildJobForm(selectedJob) : emptyJobForm());
        setJobFormMessage(null);
    }, [selectedJob]);

    useEffect(() => {
        const applications = applicationsQuery.data?.records || [];
        if (!applications.length) {
            setSelectedApplicationId(null);
            return;
        }

        setSelectedApplicationId(current => (
            current && applications.some(application => application.id === current) ? current : applications[0].id
        ));
    }, [applicationsQuery.data?.records]);

    useEffect(() => {
        setReviewForm(selectedApplication ? buildReviewForm(selectedApplication) : emptyReviewForm());
        setReviewFormMessage(null);
    }, [selectedApplication]);

    useEffect(() => {
        setInterviewForm(selectedInterview ? buildInterviewForm(selectedInterview) : emptyInterviewForm());
        setInterviewFormMessage(null);
    }, [selectedInterview]);

    function patchJobForm(patch: Partial<JobFormState>) {
        setJobForm(current => ({ ...current, ...patch }));
        setJobFormMessage(null);
        setFeedback(null);
    }

    function patchReviewForm(patch: Partial<ReviewFormState>) {
        setReviewForm(current => ({ ...current, ...patch }));
        setReviewFormMessage(null);
        setFeedback(null);
    }

    function patchInterviewForm(patch: Partial<InterviewFormState>) {
        setInterviewForm(current => ({ ...current, ...patch }));
        setInterviewFormMessage(null);
        setFeedback(null);
    }

    const createJobMutation = useMutation({
        mutationFn: (request: JobUpsertRequest) => createJob(session!.token, request),
        onSuccess(job) {
            setFeedback({
                tone: "good",
                text: `Created job: ${job.title}`
            });
            setSelectedJobId(job.id);
            void Promise.all([
                queryClient.invalidateQueries({ queryKey: ["hr-owned-jobs"] }),
                queryClient.invalidateQueries({ queryKey: ["public-jobs"] }),
                queryClient.invalidateQueries({ queryKey: ["statistics-overview"] })
            ]);
        },
        onError(error) {
            setFeedback({
                tone: "warn",
                text: error instanceof Error ? error.message : "Could not create the job."
            });
        }
    });

    const updateJobMutation = useMutation({
        mutationFn: ({ jobId, request }: { jobId: number; request: JobUpsertRequest }) => updateJob(session!.token, jobId, request),
        onSuccess(job) {
            setFeedback({
                tone: "good",
                text: `Updated job: ${job.title}`
            });
            void Promise.all([
                queryClient.invalidateQueries({ queryKey: ["hr-owned-jobs"] }),
                queryClient.invalidateQueries({ queryKey: ["public-jobs"] }),
                queryClient.invalidateQueries({ queryKey: ["statistics-overview"] })
            ]);
        },
        onError(error) {
            setFeedback({
                tone: "warn",
                text: error instanceof Error ? error.message : "Could not update the job."
            });
        }
    });

    const deleteJobMutation = useMutation({
        mutationFn: (jobId: number) => deleteJob(session!.token, jobId),
        onSuccess() {
            setFeedback({
                tone: "good",
                text: "Job deleted successfully."
            });
            setSelectedJobId(null);
            setJobForm(emptyJobForm());
            void Promise.all([
                queryClient.invalidateQueries({ queryKey: ["hr-owned-jobs"] }),
                queryClient.invalidateQueries({ queryKey: ["public-jobs"] }),
                queryClient.invalidateQueries({ queryKey: ["statistics-overview"] }),
                queryClient.invalidateQueries({ queryKey: ["hr-job-applications"] }),
                queryClient.invalidateQueries({ queryKey: ["hr-job-interviews"] })
            ]);
        },
        onError(error) {
            setFeedback({
                tone: "warn",
                text: error instanceof Error ? error.message : "Could not delete the job."
            });
        }
    });

    const reviewMutation = useMutation({
        mutationFn: ({ applicationId, status, hrNote }: { applicationId: number; status: string; hrNote: string | null }) =>
            updateApplicationStatus(session!.token, applicationId, { status, hrNote }),
        onSuccess(application) {
            setFeedback({
                tone: "good",
                text: `Application moved to ${application.status}.`
            });
            void Promise.all([
                queryClient.invalidateQueries({ queryKey: ["hr-job-applications"] }),
                queryClient.invalidateQueries({ queryKey: ["statistics-overview"] })
            ]);
        },
        onError(error) {
            setFeedback({
                tone: "warn",
                text: error instanceof Error ? error.message : "Could not update the application."
            });
        }
    });

    const scheduleMutation = useMutation({
        mutationFn: (request: ScheduleInterviewRequest) => scheduleInterview(session!.token, request),
        onSuccess(interview) {
            setFeedback({
                tone: "good",
                text: `Interview scheduled for ${formatDateTime(interview.interviewAt)}.`
            });
            void Promise.all([
                queryClient.invalidateQueries({ queryKey: ["hr-job-interviews"] }),
                queryClient.invalidateQueries({ queryKey: ["hr-job-applications"] }),
                queryClient.invalidateQueries({ queryKey: ["statistics-overview"] })
            ]);
        },
        onError(error) {
            setFeedback({
                tone: "warn",
                text: error instanceof Error ? error.message : "Could not schedule the interview."
            });
        }
    });

    const updateInterviewMutation = useMutation({
        mutationFn: ({ interviewId, request }: { interviewId: number; request: UpdateInterviewRequest }) =>
            updateInterview(session!.token, interviewId, request),
        onSuccess(interview) {
            setFeedback({
                tone: "good",
                text: `Interview updated to ${interview.status}.`
            });
            void Promise.all([
                queryClient.invalidateQueries({ queryKey: ["hr-job-interviews"] }),
                queryClient.invalidateQueries({ queryKey: ["statistics-overview"] })
            ]);
        },
        onError(error) {
            setFeedback({
                tone: "warn",
                text: error instanceof Error ? error.message : "Could not update the interview."
            });
        }
    });

    function handleJobSubmit() {
        const validationMessage = validateJobForm(jobForm);
        if (validationMessage) {
            setJobFormMessage(validationMessage);
            return;
        }

        const request = buildJobRequest(jobForm);
        if (selectedJob) {
            updateJobMutation.mutate({ jobId: selectedJob.id, request });
            return;
        }
        createJobMutation.mutate(request);
    }

    function handleJobDelete() {
        if (!selectedJob) {
            return;
        }
        if (!window.confirm(`Delete job "${selectedJob.title}"?`)) {
            return;
        }
        deleteJobMutation.mutate(selectedJob.id);
    }

    function handleReviewSubmit() {
        if (!selectedApplication) {
            return;
        }

        const validationMessage = validateReviewForm(reviewForm);
        if (validationMessage) {
            setReviewFormMessage(validationMessage);
            return;
        }

        reviewMutation.mutate({
            applicationId: selectedApplication.id,
            status: reviewForm.status,
            hrNote: normalizeText(reviewForm.hrNote)
        });
    }

    function handleInterviewSubmit() {
        if (!selectedApplication) {
            return;
        }

        const validationMessage = validateInterviewForm(interviewForm, Boolean(selectedInterview));
        if (validationMessage) {
            setInterviewFormMessage(validationMessage);
            return;
        }

        if (selectedInterview) {
            updateInterviewMutation.mutate({
                interviewId: selectedInterview.id,
                request: {
                    interviewAt: interviewForm.interviewAt || null,
                    location: normalizeText(interviewForm.location),
                    meetingLink: normalizeText(interviewForm.meetingLink),
                    status: interviewForm.status,
                    result: interviewForm.result,
                    remark: normalizeText(interviewForm.remark)
                }
            });
            return;
        }

        scheduleMutation.mutate({
            applicationId: selectedApplication.id,
            interviewAt: interviewForm.interviewAt,
            location: normalizeText(interviewForm.location),
            meetingLink: normalizeText(interviewForm.meetingLink),
            remark: normalizeText(interviewForm.remark)
        });
    }

    return (
        <div className="page-grid">
            <SectionCard
                eyebrow="HR Workspace"
                title="Manage postings and move applicants forward"
                description="The React workbench now runs the same HR flow as the backend demo: create jobs, review applications, and schedule interviews."
                action={
                    <button
                        className="button button--ghost"
                        onClick={() => {
                            void Promise.all([
                                statsQuery.refetch(),
                                jobsQuery.refetch(),
                                applicationsQuery.refetch(),
                                interviewsQuery.refetch()
                            ]);
                        }}
                        type="button"
                    >
                        Refresh All
                    </button>
                }
            >
                <div className="dashboard-grid dashboard-grid--three">
                    <article className="panel">
                        <strong>{session?.user.fullName}</strong>
                        <p>{session?.user.email}</p>
                        <div className="inline-pills">
                            {session?.user.roles.map(role => (
                                <StatusPill key={role} tone="good">
                                    {role}
                                </StatusPill>
                            ))}
                        </div>
                    </article>
                    <article className="panel">
                        <strong>What is live now</strong>
                        <ul className="feature-list">
                            <li>Owned jobs list with create, edit, and delete</li>
                            <li>Per-job application review and recruiter notes</li>
                            <li>Interview scheduling and updates for selected applicants</li>
                            <li>Statistics overview synced with the current HR scope</li>
                        </ul>
                    </article>
                    <article className="panel">
                        <strong>Live stats preview</strong>
                        {statsQuery.isLoading ? (
                            <p className="muted-text">Loading HR overview...</p>
                        ) : statsQuery.isError ? (
                            <p className="muted-text">
                                {statsQuery.error instanceof Error ? statsQuery.error.message : "Unexpected error"}
                            </p>
                        ) : statsQuery.data ? (
                            <div className="metric-grid">
                                <div className="metric-card">
                                    <span>Jobs</span>
                                    <strong>{statsQuery.data.jobs.total}</strong>
                                </div>
                                <div className="metric-card">
                                    <span>Applications</span>
                                    <strong>{statsQuery.data.applications.total}</strong>
                                </div>
                                <div className="metric-card">
                                    <span>Interviews</span>
                                    <strong>{statsQuery.data.interviews.total}</strong>
                                </div>
                                <div className="metric-card">
                                    <span>Upcoming</span>
                                    <strong>{statsQuery.data.interviews.upcoming}</strong>
                                </div>
                            </div>
                        ) : (
                            <EmptyState
                                title="No statistics loaded"
                                description="HR overview data will appear here after the query runs."
                            />
                        )}
                    </article>
                </div>
                <div className="metric-grid">
                    {hrMetrics.map(metric => (
                        <article className="metric-card" key={metric.label}>
                            <span>{metric.label}</span>
                            <strong>{metric.value}</strong>
                        </article>
                    ))}
                </div>
            </SectionCard>

            {feedback ? <div className={`notice notice--${feedback.tone}`}>{feedback.text}</div> : null}

            <SectionCard
                eyebrow="Analytics"
                title="See the recruiting pipeline at a glance"
                description={statsQuery.data
                    ? `Snapshot generated at ${formatDateTime(statsQuery.data.generatedAt)} for your recruiter scope.`
                    : "This view turns the existing statistics API into quick visual checks for pipeline health."}
            >
                {statsQuery.isLoading ? (
                    <EmptyState
                        title="Loading HR analytics"
                        description="Pulling the latest recruiting snapshot for your workspace."
                    />
                ) : statsQuery.isError ? (
                    <EmptyState
                        title="Could not load analytics"
                        description={statsQuery.error instanceof Error ? statsQuery.error.message : "Unexpected error"}
                    />
                ) : hrAnalytics ? (
                    <div className="page-grid">
                        <div className="chart-grid chart-grid--three">
                            {hrAnalytics.progressCards.map(card => (
                                <ProgressStatCard
                                    key={card.label}
                                    denominator={card.denominator}
                                    helper={card.helper}
                                    label={card.label}
                                    numerator={card.numerator}
                                />
                            ))}
                        </div>
                        <div className="chart-grid chart-grid--two">
                            {hrAnalytics.charts.map(chart => (
                                <DistributionChart
                                    key={chart.title}
                                    items={chart.items}
                                    subtitle={chart.subtitle}
                                    title={chart.title}
                                />
                            ))}
                        </div>
                    </div>
                ) : (
                    <EmptyState
                        title="No analytics available"
                        description="Statistics charts will appear here once recruiting data is available."
                    />
                )}
            </SectionCard>

            {selectedJob ? (
                <div className="dashboard-grid dashboard-grid--three">
                    <article className="panel panel--soft">
                        <strong>Selected job</strong>
                        <p>{selectedJob.title}</p>
                        <p className="muted-text">{selectedJob.city || "Unknown city"} · {selectedJob.category || "General"}</p>
                    </article>
                    <article className="panel panel--soft">
                        <strong>Status + deadline</strong>
                        <p>{selectedJob.status}</p>
                        <p className="muted-text">{formatDateTime(selectedJob.applicationDeadline)}</p>
                    </article>
                    <article className="panel panel--soft">
                        <strong>Pipeline snapshot</strong>
                        <p>{applicationCount} applicants · {interviewCount} interviews</p>
                        <p className="muted-text">Keep applications and interview updates moving from this job context.</p>
                    </article>
                </div>
            ) : null}

            <div className="dashboard-grid dashboard-grid--two">
                <SectionCard
                    eyebrow="Owned Jobs"
                    title="Select a posting to manage"
                    description="This list is filtered client-side to the signed-in recruiter's jobs using the live backend response."
                    action={
                        <button
                            className="button button--ghost"
                            onClick={() => {
                                void jobsQuery.refetch();
                            }}
                            type="button"
                        >
                            Refresh Jobs
                        </button>
                    }
                >
                    {jobsQuery.isLoading ? (
                        <EmptyState
                            title="Loading owned jobs"
                            description="Fetching the current HR job inventory."
                        />
                    ) : ownedJobs.length ? (
                        <div className="stack-list">
                            {ownedJobs.map(job => (
                                <button
                                    key={job.id}
                                    className={job.id === selectedJobId ? "list-card list-card--selected" : "list-card"}
                                    onClick={() => setSelectedJobId(job.id)}
                                    type="button"
                                >
                                    <div className="list-card__top">
                                        <strong>{job.title}</strong>
                                        <StatusPill tone={getStatusTone(job.status)}>{job.status}</StatusPill>
                                    </div>
                                    <p>{job.city || "Unknown city"} · {job.category || "General"}</p>
                                    <div className="inline-pills">
                                        <StatusPill tone="info">{job.employmentType || "N/A"}</StatusPill>
                                        <StatusPill tone="info">{job.experienceLevel || "N/A"}</StatusPill>
                                        <StatusPill tone="good">{formatSalaryRange(job)}</StatusPill>
                                    </div>
                                    <p className="muted-text">Deadline: {formatDateTime(job.applicationDeadline)}</p>
                                </button>
                            ))}
                        </div>
                    ) : (
                        <EmptyState
                            title="No owned jobs yet"
                            description="Create the first job in the composer panel to start the recruiter flow."
                        />
                    )}
                </SectionCard>

                <SectionCard
                    eyebrow="Job Composer"
                    title={selectedJob ? `Edit ${selectedJob.title}` : "Create a new job posting"}
                    description="The same form handles both create and update actions."
                    action={
                        <button
                            className="button button--ghost"
                            onClick={() => {
                                setSelectedJobId(null);
                                setJobForm(emptyJobForm());
                            }}
                            type="button"
                        >
                            New Draft
                        </button>
                    }
                >
                    <div className="form-stack">
                        <div className="form-field">
                            <label htmlFor="hr-job-title">Job title</label>
                                <input
                                    id="hr-job-title"
                                    onChange={event => patchJobForm({ title: event.target.value })}
                                    placeholder="Backend Engineer"
                                    type="text"
                                    value={jobForm.title}
                            />
                        </div>
                        <div className="form-field">
                            <label htmlFor="hr-job-description">Description</label>
                                <textarea
                                    id="hr-job-description"
                                    onChange={event => patchJobForm({ description: event.target.value })}
                                    placeholder="Describe the role, stack, and responsibilities"
                                    rows={5}
                                    value={jobForm.description}
                            />
                        </div>
                        <div className="dashboard-grid dashboard-grid--two">
                            <div className="form-field">
                                <label htmlFor="hr-job-city">City</label>
                                <input
                                    id="hr-job-city"
                                    onChange={event => patchJobForm({ city: event.target.value })}
                                    type="text"
                                    value={jobForm.city}
                                />
                            </div>
                            <div className="form-field">
                                <label htmlFor="hr-job-category">Category</label>
                                <input
                                    id="hr-job-category"
                                    onChange={event => patchJobForm({ category: event.target.value })}
                                    type="text"
                                    value={jobForm.category}
                                />
                            </div>
                            <div className="form-field">
                                <label htmlFor="hr-job-employment">Employment type</label>
                                <select
                                    id="hr-job-employment"
                                    onChange={event => patchJobForm({ employmentType: event.target.value })}
                                    value={jobForm.employmentType}
                                >
                                    {EMPLOYMENT_TYPE_OPTIONS.map(option => (
                                        <option key={option} value={option}>
                                            {option}
                                        </option>
                                    ))}
                                </select>
                            </div>
                            <div className="form-field">
                                <label htmlFor="hr-job-level">Experience level</label>
                                <select
                                    id="hr-job-level"
                                    onChange={event => patchJobForm({ experienceLevel: event.target.value })}
                                    value={jobForm.experienceLevel}
                                >
                                    {EXPERIENCE_LEVEL_OPTIONS.map(option => (
                                        <option key={option} value={option}>
                                            {option}
                                        </option>
                                    ))}
                                </select>
                            </div>
                            <div className="form-field">
                                <label htmlFor="hr-job-salary-min">Salary min</label>
                                <input
                                    id="hr-job-salary-min"
                                    inputMode="decimal"
                                    onChange={event => patchJobForm({ salaryMin: event.target.value })}
                                    placeholder="18000"
                                    type="text"
                                    value={jobForm.salaryMin}
                                />
                            </div>
                            <div className="form-field">
                                <label htmlFor="hr-job-salary-max">Salary max</label>
                                <input
                                    id="hr-job-salary-max"
                                    inputMode="decimal"
                                    onChange={event => patchJobForm({ salaryMax: event.target.value })}
                                    placeholder="28000"
                                    type="text"
                                    value={jobForm.salaryMax}
                                />
                            </div>
                            <div className="form-field">
                                <label htmlFor="hr-job-status">Status</label>
                                <select
                                    id="hr-job-status"
                                    onChange={event => patchJobForm({ status: event.target.value })}
                                    value={jobForm.status}
                                >
                                    {JOB_STATUS_OPTIONS.map(option => (
                                        <option key={option} value={option}>
                                            {option}
                                        </option>
                                    ))}
                                </select>
                            </div>
                            <div className="form-field">
                                <label htmlFor="hr-job-deadline">Application deadline</label>
                                <input
                                    id="hr-job-deadline"
                                    onChange={event => patchJobForm({ applicationDeadline: event.target.value })}
                                    type="datetime-local"
                                    value={jobForm.applicationDeadline}
                                />
                            </div>
                        </div>
                        <p className="field-note field-note--info">
                            Title and description are required. Salary range must be numeric and ordered.
                        </p>
                        {jobFormMessage ? <p className="field-note field-note--warn">{jobFormMessage}</p> : null}
                        <div className="button-row">
                            <button
                                className="button button--primary"
                                disabled={createJobMutation.isPending || updateJobMutation.isPending}
                                onClick={handleJobSubmit}
                                type="button"
                            >
                                {selectedJob
                                    ? (updateJobMutation.isPending ? "Saving..." : "Save Job Changes")
                                    : (createJobMutation.isPending ? "Creating..." : "Create Job")}
                            </button>
                            {selectedJob ? (
                                <button
                                    className="button button--danger"
                                    disabled={deleteJobMutation.isPending}
                                    onClick={handleJobDelete}
                                    type="button"
                                >
                                    {deleteJobMutation.isPending ? "Deleting..." : "Delete Job"}
                                </button>
                            ) : null}
                        </div>
                    </div>
                </SectionCard>
            </div>

            <div className="dashboard-grid dashboard-grid--two">
                <SectionCard
                    eyebrow="Applications"
                    title={selectedJob ? `Review applicants for ${selectedJob.title}` : "Select a job first"}
                    description="Choose an applicant to update status notes or move them toward interview."
                >
                    {!selectedJob ? (
                        <EmptyState
                            title="No selected job"
                            description="Pick a job from the left column to load its applicants."
                        />
                    ) : applicationsQuery.isLoading ? (
                        <p className="muted-text">Loading job applications...</p>
                    ) : applicationsQuery.data?.records.length ? (
                        <div className="stack-list">
                            {applicationsQuery.data.records.map(application => {
                                const relatedInterview = interviewByApplicationId.get(application.id);
                                return (
                                    <button
                                        key={application.id}
                                        className={application.id === selectedApplicationId ? "list-card list-card--selected" : "list-card"}
                                        onClick={() => setSelectedApplicationId(application.id)}
                                        type="button"
                                    >
                                        <div className="list-card__top">
                                            <strong>{application.candidateName || application.candidateEmail || "Candidate"}</strong>
                                            <StatusPill tone={getStatusTone(application.status)}>{application.status}</StatusPill>
                                        </div>
                                        <p>{application.candidateEmail || "No email"} · Applied {formatDateTime(application.appliedAt)}</p>
                                        {application.hrNote ? <p className="muted-text">Note: {application.hrNote}</p> : null}
                                        <div className="inline-pills">
                                            <StatusPill tone="info">{application.jobCategory || "General"}</StatusPill>
                                            <StatusPill tone={relatedInterview ? "good" : "default"}>
                                                {relatedInterview ? "Interview linked" : "No interview yet"}
                                            </StatusPill>
                                        </div>
                                    </button>
                                );
                            })}
                        </div>
                    ) : (
                        <EmptyState
                            title="No applications yet"
                            description="Candidates who apply to this job will appear here."
                        />
                    )}
                </SectionCard>

                <SectionCard
                    eyebrow="Review Console"
                    title={selectedApplication ? `Advance ${selectedApplication.candidateName || "candidate"}` : "Pick an application to review"}
                    description="This panel controls recruiter notes, application state, and interview scheduling."
                >
                    {!selectedApplication ? (
                        <EmptyState
                            title="No selected application"
                            description="Choose an applicant from the applications list to unlock review actions."
                        />
                    ) : (
                        <div className="form-stack">
                            <div className="panel panel--soft">
                                <div className="list-card__top">
                                    <strong>{selectedApplication.jobTitle || "Untitled job"}</strong>
                                    <StatusPill tone={getStatusTone(selectedApplication.status)}>
                                        {selectedApplication.status}
                                    </StatusPill>
                                </div>
                                <p>{selectedApplication.candidateEmail || "No email on record"}</p>
                                {selectedApplication.coverLetter ? (
                                    <p className="muted-text">{selectedApplication.coverLetter}</p>
                                ) : (
                                    <p className="muted-text">No cover letter submitted.</p>
                                )}
                            </div>

                            <div className="dashboard-grid dashboard-grid--two">
                                <div className="form-field">
                                    <label htmlFor="hr-application-status">Application status</label>
                                    <select
                                        id="hr-application-status"
                                        onChange={event => patchReviewForm({ status: event.target.value })}
                                        value={reviewForm.status}
                                    >
                                        {APPLICATION_STATUS_OPTIONS.map(option => (
                                            <option key={option} value={option}>
                                                {option}
                                            </option>
                                        ))}
                                    </select>
                                </div>
                                <div className="form-field">
                                    <label htmlFor="hr-application-resume">Resume path</label>
                                    <input
                                        id="hr-application-resume"
                                        readOnly
                                        type="text"
                                        value={selectedApplication.resumeFilePath || "No resume uploaded"}
                                    />
                                </div>
                            </div>

                            <div className="form-field">
                                <label htmlFor="hr-application-note">Recruiter note</label>
                                <textarea
                                    id="hr-application-note"
                                    onChange={event => patchReviewForm({ hrNote: event.target.value })}
                                    placeholder="Add guidance for later review steps"
                                    rows={4}
                                    value={reviewForm.hrNote}
                                />
                                <p className="field-note field-note--info">
                                    {reviewForm.hrNote.trim().length}/{MAX_HR_NOTE_LENGTH} characters
                                </p>
                            </div>

                            {reviewFormMessage ? <p className="field-note field-note--warn">{reviewFormMessage}</p> : null}

                            <div className="button-row">
                                <button
                                    className="button button--primary"
                                    disabled={reviewMutation.isPending}
                                    onClick={handleReviewSubmit}
                                    type="button"
                                >
                                    {reviewMutation.isPending ? "Updating..." : "Save Review Decision"}
                                </button>
                            </div>

                            <div className="panel panel--soft">
                                <div className="list-card__top">
                                    <strong>{selectedInterview ? "Update interview plan" : "Schedule interview"}</strong>
                                    {selectedInterview ? (
                                        <StatusPill tone={getStatusTone(selectedInterview.status)}>
                                            {selectedInterview.status}
                                        </StatusPill>
                                    ) : (
                                        <StatusPill tone="info">No interview yet</StatusPill>
                                    )}
                                </div>
                                {selectedInterview ? (
                                    <p className="muted-text">
                                        Current slot: {formatDateTime(selectedInterview.interviewAt)}
                                    </p>
                                ) : (
                                    <p className="muted-text">
                                        Scheduling here will move the application into the interview stage if the backend accepts it.
                                    </p>
                                )}
                            </div>

                            <div className="dashboard-grid dashboard-grid--two">
                                <div className="form-field">
                                    <label htmlFor="hr-interview-at">Interview time</label>
                                    <input
                                        id="hr-interview-at"
                                        onChange={event => patchInterviewForm({ interviewAt: event.target.value })}
                                        type="datetime-local"
                                        value={interviewForm.interviewAt}
                                    />
                                </div>
                                <div className="form-field">
                                    <label htmlFor="hr-interview-location">Location</label>
                                    <input
                                        id="hr-interview-location"
                                        onChange={event => patchInterviewForm({ location: event.target.value })}
                                        placeholder="Conference Room A / Remote"
                                        type="text"
                                        value={interviewForm.location}
                                    />
                                </div>
                                <div className="form-field">
                                    <label htmlFor="hr-interview-link">Meeting link</label>
                                    <input
                                        id="hr-interview-link"
                                        onChange={event => patchInterviewForm({ meetingLink: event.target.value })}
                                        placeholder="https://meet.example.com/..."
                                        type="text"
                                        value={interviewForm.meetingLink}
                                    />
                                </div>
                                {selectedInterview ? (
                                    <div className="form-field">
                                        <label htmlFor="hr-interview-status">Interview status</label>
                                        <select
                                            id="hr-interview-status"
                                            onChange={event => patchInterviewForm({ status: event.target.value })}
                                            value={interviewForm.status}
                                        >
                                            {INTERVIEW_STATUS_OPTIONS.map(option => (
                                                <option key={option} value={option}>
                                                    {option}
                                                </option>
                                            ))}
                                        </select>
                                    </div>
                                ) : null}
                                {selectedInterview ? (
                                    <div className="form-field">
                                        <label htmlFor="hr-interview-result">Interview result</label>
                                        <select
                                            id="hr-interview-result"
                                            onChange={event => patchInterviewForm({ result: event.target.value })}
                                            value={interviewForm.result}
                                        >
                                            {INTERVIEW_RESULT_OPTIONS.map(option => (
                                                <option key={option} value={option}>
                                                    {option}
                                                </option>
                                            ))}
                                        </select>
                                    </div>
                                ) : null}
                            </div>

                            <div className="form-field">
                                <label htmlFor="hr-interview-remark">Interview remark</label>
                                <textarea
                                    id="hr-interview-remark"
                                    onChange={event => patchInterviewForm({ remark: event.target.value })}
                                    placeholder="Add context for interviewer handoff"
                                    rows={3}
                                    value={interviewForm.remark}
                                />
                                <p className="field-note field-note--info">
                                    {interviewForm.remark.trim().length}/{MAX_INTERVIEW_REMARK_LENGTH} characters
                                </p>
                            </div>

                            {interviewFormMessage ? <p className="field-note field-note--warn">{interviewFormMessage}</p> : null}

                            <div className="button-row">
                                <button
                                    className="button button--primary"
                                    disabled={
                                        !interviewForm.interviewAt ||
                                        scheduleMutation.isPending ||
                                        updateInterviewMutation.isPending
                                    }
                                    onClick={handleInterviewSubmit}
                                    type="button"
                                >
                                    {selectedInterview
                                        ? (updateInterviewMutation.isPending ? "Updating..." : "Update Interview")
                                        : (scheduleMutation.isPending ? "Scheduling..." : "Schedule Interview")}
                                </button>
                            </div>
                        </div>
                    )}
                </SectionCard>
            </div>

            <SectionCard
                eyebrow="Interviews"
                title={selectedJob ? `Interview queue for ${selectedJob.title}` : "Select a job to see interviews"}
                description="Use this as a live readout of all scheduled interviews under the selected posting."
            >
                {!selectedJob ? (
                    <EmptyState
                        title="No selected job"
                        description="Select a job above to load its interview queue."
                    />
                ) : interviewsQuery.isLoading ? (
                    <p className="muted-text">Loading job interviews...</p>
                ) : interviewsQuery.data?.records.length ? (
                    <div className="job-grid">
                        {interviewsQuery.data.records.map(interview => (
                            <article className="job-card" key={interview.id}>
                                <div className="job-card__header">
                                    <div>
                                        <h3>{interview.candidateName || interview.candidateEmail || "Candidate"}</h3>
                                        <p>{formatDateTime(interview.interviewAt)}</p>
                                    </div>
                                    <StatusPill tone={getStatusTone(interview.status)}>
                                        {interview.status}
                                    </StatusPill>
                                </div>
                                <div className="job-card__meta">
                                    <StatusPill tone="info">{interview.result}</StatusPill>
                                    <StatusPill tone="info">{interview.location || "Location TBD"}</StatusPill>
                                </div>
                                <p>{interview.meetingLink || "No meeting link provided"}</p>
                                {interview.remark ? <p className="muted-text">{interview.remark}</p> : null}
                                <div className="button-row button-row--compact">
                                    <span className="muted-text">Application #{interview.applicationId}</span>
                                    <button
                                        className="button button--ghost"
                                        onClick={() => setSelectedApplicationId(interview.applicationId)}
                                        type="button"
                                    >
                                        Open Review
                                    </button>
                                </div>
                            </article>
                        ))}
                    </div>
                ) : (
                    <EmptyState
                        title="No interviews scheduled"
                        description="Use the review console to schedule the first interview for this job."
                    />
                )}
            </SectionCard>
        </div>
    );
}
