import { useEffect, useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useAuth } from "../../app/providers/AuthProvider";
import { createApplication } from "../../features/applications/api/createApplication";
import { listMyApplications } from "../../features/applications/api/listMyApplications";
import { usePublicJobs } from "../../features/jobs/hooks/usePublicJobs";
import { listMyInterviews } from "../../features/interviews/api/listMyInterviews";
import { getUnreadCount } from "../../features/notifications/api/getUnreadCount";
import { listNotifications } from "../../features/notifications/api/listNotifications";
import { markNotificationRead } from "../../features/notifications/api/markNotificationRead";
import { uploadResume } from "../../features/resumes/api/uploadResume";
import { EmptyState } from "../../shared/components/EmptyState";
import { SectionCard } from "../../shared/components/SectionCard";
import { StatusPill } from "../../shared/components/StatusPill";
import { formatDateTime, formatSalaryRange } from "../../shared/lib/formatters";

const MAX_RESUME_SIZE_BYTES = 5 * 1024 * 1024;
const MAX_COVER_LETTER_LENGTH = 2000;

export function CandidateDashboardPage() {
    const { session } = useAuth();
    const queryClient = useQueryClient();
    const jobsQuery = usePublicJobs();
    const applicationsQuery = useQuery({
        queryKey: ["candidate-applications", session?.token],
        queryFn: () => listMyApplications(session!.token),
        enabled: Boolean(session?.token)
    });
    const interviewsQuery = useQuery({
        queryKey: ["candidate-interviews", session?.token],
        queryFn: () => listMyInterviews(session!.token),
        enabled: Boolean(session?.token)
    });
    const notificationsQuery = useQuery({
        queryKey: ["candidate-notifications", session?.token],
        queryFn: () => listNotifications(session!.token),
        enabled: Boolean(session?.token)
    });
    const unreadCountQuery = useQuery({
        queryKey: ["candidate-notifications-unread", session?.token],
        queryFn: () => getUnreadCount(session!.token),
        enabled: Boolean(session?.token)
    });

    const [selectedJobId, setSelectedJobId] = useState<number | null>(null);
    const [coverLetter, setCoverLetter] = useState("");
    const [resumeFile, setResumeFile] = useState<File | null>(null);
    const [resumePath, setResumePath] = useState("");
    const [feedback, setFeedback] = useState<{ tone: "good" | "warn" | "info"; text: string } | null>(null);

    const selectedJob = useMemo(
        () => jobsQuery.data?.records.find(job => job.id === selectedJobId) || null,
        [jobsQuery.data?.records, selectedJobId]
    );

    useEffect(() => {
        if (!jobsQuery.data?.records.length) {
            return;
        }
        setSelectedJobId(current => current ?? jobsQuery.data!.records[0].id);
    }, [jobsQuery.data]);

    const uploadMutation = useMutation({
        mutationFn: (file: File) => uploadResume(session!.token, file),
        onSuccess(data) {
            setResumePath(data.filePath);
            setResumeFile(null);
            setFeedback({
                tone: "good",
                text: `Resume uploaded: ${data.originalFilename}`
            });
        },
        onError(error) {
            setFeedback({
                tone: "warn",
                text: error instanceof Error ? error.message : "Resume upload failed."
            });
        }
    });

    const applyMutation = useMutation({
        mutationFn: () => createApplication(session!.token, {
            jobId: selectedJobId!,
            resumeFilePath: resumePath || null,
            coverLetter: coverLetter.trim() || null
        }),
        onSuccess() {
            setCoverLetter("");
            setFeedback({
                tone: "good",
                text: "Application submitted successfully."
            });
            void Promise.all([
                queryClient.invalidateQueries({ queryKey: ["candidate-applications"] }),
                queryClient.invalidateQueries({ queryKey: ["candidate-notifications"] }),
                queryClient.invalidateQueries({ queryKey: ["candidate-notifications-unread"] })
            ]);
        },
        onError(error) {
            setFeedback({
                tone: "warn",
                text: error instanceof Error ? error.message : "Application submission failed."
            });
        }
    });

    const markReadMutation = useMutation({
        mutationFn: (notificationId: number) => markNotificationRead(session!.token, notificationId),
        onSuccess() {
            void Promise.all([
                queryClient.invalidateQueries({ queryKey: ["candidate-notifications"] }),
                queryClient.invalidateQueries({ queryKey: ["candidate-notifications-unread"] })
            ]);
        },
        onError(error) {
            setFeedback({
                tone: "warn",
                text: error instanceof Error ? error.message : "Could not mark the notification as read."
            });
        }
    });

    function handleResumeSelection(file: File | null) {
        setResumeFile(file);
        if (file) {
            setResumePath("");
        }
        setFeedback(null);
    }

    function handleResumeUpload() {
        if (!resumeFile) {
            setFeedback({
                tone: "warn",
                text: "Choose a PDF resume before uploading."
            });
            return;
        }

        const isPdf = resumeFile.type === "application/pdf" || resumeFile.name.toLowerCase().endsWith(".pdf");
        if (!isPdf) {
            setFeedback({
                tone: "warn",
                text: "Resume upload only supports PDF files."
            });
            return;
        }

        if (resumeFile.size > MAX_RESUME_SIZE_BYTES) {
            setFeedback({
                tone: "warn",
                text: "Resume file is too large. Please keep it within 5 MB."
            });
            return;
        }

        uploadMutation.mutate(resumeFile);
    }

    function handleApply() {
        if (!selectedJob || !selectedJobId) {
            setFeedback({
                tone: "warn",
                text: "Select a job before submitting an application."
            });
            return;
        }

        if (selectedJob.status !== "OPEN") {
            setFeedback({
                tone: "warn",
                text: "This job is no longer accepting applications."
            });
            return;
        }

        if (selectedJob.applicationDeadline) {
            const deadline = new Date(selectedJob.applicationDeadline);
            if (!Number.isNaN(deadline.getTime()) && deadline.getTime() < Date.now()) {
                setFeedback({
                    tone: "warn",
                    text: "The application deadline has already passed for this job."
                });
                return;
            }
        }

        if (!resumePath) {
            setFeedback({
                tone: "warn",
                text: "Upload your PDF resume before submitting the application."
            });
            return;
        }

        if (resumeFile && !uploadMutation.isPending) {
            setFeedback({
                tone: "warn",
                text: "You selected a new resume file. Upload it first before applying."
            });
            return;
        }

        if (coverLetter.trim().length > MAX_COVER_LETTER_LENGTH) {
            setFeedback({
                tone: "warn",
                text: `Cover letter must stay within ${MAX_COVER_LETTER_LENGTH} characters.`
            });
            return;
        }

        applyMutation.mutate();
    }

    return (
        <div className="page-grid">
            <SectionCard
                eyebrow="Candidate Workspace"
                title="Apply, track, and stay updated"
                description="The first real candidate flow is now wired to the live backend APIs."
            >
                <div className="dashboard-grid dashboard-grid--two">
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
                        <strong>Current live modules</strong>
                        <ul className="feature-list">
                            <li>Public jobs query and selection</li>
                            <li>Resume upload to `/api/resumes/upload`</li>
                            <li>Application submit to `/api/applications`</li>
                            <li>Live lists for applications, interviews, and notifications</li>
                        </ul>
                    </article>
                </div>
            </SectionCard>

            {feedback ? <div className={`notice notice--${feedback.tone}`}>{feedback.text}</div> : null}

            <div className="dashboard-grid dashboard-grid--two">
                <SectionCard
                    eyebrow="Selected Job"
                    title="Pick a public job to apply for"
                    description="The candidate flow keeps its own selected job context inside the dashboard."
                >
                    {jobsQuery.isLoading ? (
                        <EmptyState
                            title="Loading public jobs"
                            description="Fetching available positions for candidate applications."
                        />
                    ) : jobsQuery.isError ? (
                        <EmptyState
                            title="Could not load jobs"
                            description={jobsQuery.error instanceof Error ? jobsQuery.error.message : "Unexpected error"}
                        />
                    ) : jobsQuery.data?.records.length ? (
                        <div className="stack-list">
                            {jobsQuery.data.records.map(job => (
                                <button
                                    key={job.id}
                                    className={job.id === selectedJobId ? "list-card list-card--selected" : "list-card"}
                                    onClick={() => setSelectedJobId(job.id)}
                                    type="button"
                                >
                                    <div className="list-card__top">
                                        <strong>{job.title}</strong>
                                        <StatusPill>{job.status}</StatusPill>
                                    </div>
                                    <p>{job.city || "Unknown city"} · {job.category || "General"}</p>
                                    <div className="inline-pills">
                                        <StatusPill tone="info">{job.employmentType || "N/A"}</StatusPill>
                                        <StatusPill tone="info">{job.experienceLevel || "N/A"}</StatusPill>
                                        <StatusPill tone="good">{formatSalaryRange(job)}</StatusPill>
                                    </div>
                                </button>
                            ))}
                        </div>
                    ) : (
                        <EmptyState
                            title="No public jobs"
                            description="No open jobs are available to apply for right now."
                        />
                    )}
                </SectionCard>

                <SectionCard
                    eyebrow="Apply Flow"
                    title={selectedJob ? selectedJob.title : "Select a job first"}
                    description={selectedJob ? selectedJob.description : "Choose a public job from the list to activate the form."}
                >
                    {selectedJob ? (
                        <div className="form-stack">
                            <div className="panel panel--soft">
                                <p className="muted-text">Deadline</p>
                                <strong>{formatDateTime(selectedJob.applicationDeadline)}</strong>
                            </div>
                            <div className="form-field">
                                <label htmlFor="candidate-resume">Upload resume (PDF)</label>
                                <input
                                    id="candidate-resume"
                                    accept="application/pdf"
                                    onChange={event => handleResumeSelection(event.target.files?.[0] || null)}
                                    type="file"
                                />
                                <p className="field-note">PDF only, up to 5 MB. Upload is required before applying.</p>
                                {resumeFile ? <p className="field-note field-note--info">Selected file: {resumeFile.name}</p> : null}
                                {resumePath ? <p className="field-note field-note--good">Uploaded resume path: {resumePath}</p> : null}
                            </div>
                            <div className="button-row">
                                <button
                                    className="button button--ghost"
                                    disabled={!resumeFile || uploadMutation.isPending}
                                    onClick={handleResumeUpload}
                                    type="button"
                                >
                                    {uploadMutation.isPending ? "Uploading..." : "Upload Resume"}
                                </button>
                            </div>
                            <div className="form-field">
                                <label htmlFor="candidate-cover-letter">Cover letter</label>
                                <textarea
                                    id="candidate-cover-letter"
                                    onChange={event => {
                                        setCoverLetter(event.target.value);
                                        setFeedback(null);
                                    }}
                                    placeholder="Write a short message to the recruiter"
                                    rows={5}
                                    value={coverLetter}
                                />
                                <p className="field-note field-note--info">
                                    {coverLetter.trim().length}/{MAX_COVER_LETTER_LENGTH} characters
                                </p>
                            </div>
                            <div className="button-row">
                                <button
                                    className="button button--primary"
                                    disabled={!selectedJobId || applyMutation.isPending}
                                    onClick={handleApply}
                                    type="button"
                                >
                                    {applyMutation.isPending ? "Submitting..." : "Submit Application"}
                                </button>
                            </div>
                        </div>
                    ) : (
                        <EmptyState
                            title="No selected job"
                            description="Choose a job from the left-hand list before uploading a resume or applying."
                        />
                    )}
                </SectionCard>
            </div>

            <div className="dashboard-grid dashboard-grid--three">
                <SectionCard
                    eyebrow="My Applications"
                    title="Live application history"
                    description="This list reads from the current candidate session."
                >
                    {applicationsQuery.isLoading ? (
                        <p className="muted-text">Loading applications...</p>
                    ) : applicationsQuery.data?.records.length ? (
                        <div className="stack-list">
                            {applicationsQuery.data.records.map(application => (
                                <article className="list-card list-card--static" key={application.id}>
                                    <div className="list-card__top">
                                        <strong>{application.jobTitle || "Untitled job"}</strong>
                                        <StatusPill>{application.status}</StatusPill>
                                    </div>
                                    <p>{application.jobCity || "Unknown city"} · Applied {formatDateTime(application.appliedAt)}</p>
                                    {application.resumeFilePath ? <p className="muted-text">Resume: {application.resumeFilePath}</p> : null}
                                    {application.coverLetter ? <p className="muted-text">{application.coverLetter}</p> : null}
                                </article>
                            ))}
                        </div>
                    ) : (
                        <EmptyState
                            title="No applications yet"
                            description="Submit the first application from the selected job panel."
                        />
                    )}
                </SectionCard>

                <SectionCard
                    eyebrow="My Interviews"
                    title="Upcoming interview visibility"
                    description="This reads directly from `/api/interviews/me`."
                >
                    {interviewsQuery.isLoading ? (
                        <p className="muted-text">Loading interviews...</p>
                    ) : interviewsQuery.data?.records.length ? (
                        <div className="stack-list">
                            {interviewsQuery.data.records.map(interview => (
                                <article className="list-card list-card--static" key={interview.id}>
                                    <div className="list-card__top">
                                        <strong>{interview.jobTitle || "Interview"}</strong>
                                        <StatusPill tone="info">{interview.status}</StatusPill>
                                    </div>
                                    <p>{formatDateTime(interview.interviewAt)}</p>
                                    <p className="muted-text">{interview.location || interview.meetingLink || "Location TBD"}</p>
                                    <p className="muted-text">Result: {interview.result}</p>
                                </article>
                            ))}
                        </div>
                    ) : (
                        <EmptyState
                            title="No interviews yet"
                            description="Interviews will appear here after HR schedules one for an application."
                        />
                    )}
                </SectionCard>

                <SectionCard
                    eyebrow="My Notifications"
                    title="Unread updates and history"
                    description={unreadCountQuery.data ? `${unreadCountQuery.data.unreadCount} unread notifications` : "Loading unread count..."}
                >
                    {notificationsQuery.isLoading ? (
                        <p className="muted-text">Loading notifications...</p>
                    ) : notificationsQuery.data?.records.length ? (
                        <div className="stack-list">
                            {notificationsQuery.data.records.map(notification => (
                                <article className="list-card list-card--static" key={notification.id}>
                                    <div className="list-card__top">
                                        <strong>{notification.title}</strong>
                                        <StatusPill tone={notification.isRead ? "default" : "warn"}>
                                            {notification.isRead ? "READ" : "UNREAD"}
                                        </StatusPill>
                                    </div>
                                    <p>{notification.content}</p>
                                    <div className="button-row button-row--compact">
                                        <span className="muted-text">{formatDateTime(notification.createdAt)}</span>
                                        {!notification.isRead ? (
                                            <button
                                                className="button button--ghost"
                                                disabled={markReadMutation.isPending}
                                                onClick={() => markReadMutation.mutate(notification.id)}
                                                type="button"
                                            >
                                                Mark Read
                                            </button>
                                        ) : null}
                                    </div>
                                </article>
                            ))}
                        </div>
                    ) : (
                        <EmptyState
                            title="No notifications yet"
                            description="Application and interview updates will land here."
                        />
                    )}
                </SectionCard>
            </div>
        </div>
    );
}
