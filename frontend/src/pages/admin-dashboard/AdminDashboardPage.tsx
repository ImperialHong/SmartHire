import { useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useAuth } from "../../app/providers/AuthProvider";
import { listAdminJobs } from "../../features/admin/api/listAdminJobs";
import { listAdminUsers } from "../../features/admin/api/listAdminUsers";
import { updateAdminUserStatus } from "../../features/admin/api/updateAdminUserStatus";
import type { AdminJobItem, AdminUserItem } from "../../features/admin/types";
import { listOperationLogs } from "../../features/operation-logs/api/listOperationLogs";
import type { OperationLogItem } from "../../features/operation-logs/types";
import { getStatisticsOverview } from "../../features/statistics/api/getStatisticsOverview";
import { buildDistributionItems, readCount } from "../../features/statistics/utils";
import { DistributionChart } from "../../shared/components/DistributionChart";
import { EmptyState } from "../../shared/components/EmptyState";
import { ProgressStatCard } from "../../shared/components/ProgressStatCard";
import { SectionCard } from "../../shared/components/SectionCard";
import { StatusPill } from "../../shared/components/StatusPill";
import { formatDateTime, formatSalaryRange } from "../../shared/lib/formatters";

const USER_STATUS_OPTIONS = ["", "ACTIVE", "DISABLED"] as const;
const USER_ROLE_OPTIONS = ["", "CANDIDATE", "HR", "ADMIN"] as const;
const JOB_STATUS_OPTIONS = ["", "OPEN", "CLOSED", "EXPIRED"] as const;
const APPLICATION_STATUS_OPTIONS = ["APPLIED", "REVIEWING", "INTERVIEW", "OFFERED", "REJECTED"] as const;
const INTERVIEW_STATUS_OPTIONS = ["SCHEDULED", "COMPLETED", "CANCELLED"] as const;
const INTERVIEW_RESULT_OPTIONS = ["PENDING", "PASSED", "FAILED"] as const;
const LOG_ACTION_OPTIONS = [
    "",
    "JOB_CREATED",
    "JOB_UPDATED",
    "JOB_DELETED",
    "APPLICATION_SUBMITTED",
    "APPLICATION_STATUS_UPDATED",
    "INTERVIEW_SCHEDULED",
    "INTERVIEW_UPDATED",
    "USER_STATUS_UPDATED"
] as const;
const LOG_TARGET_OPTIONS = ["", "JOB", "APPLICATION", "INTERVIEW", "USER"] as const;

function getStatusTone(status: string | null | undefined) {
    if (!status) {
        return "default" as const;
    }
    if (["ACTIVE", "OPEN", "OFFERED", "PASSED", "COMPLETED"].includes(status)) {
        return "good" as const;
    }
    if (["REVIEWING", "INTERVIEW", "SCHEDULED", "PENDING"].includes(status)) {
        return "info" as const;
    }
    if (["DISABLED", "CLOSED", "EXPIRED", "REJECTED", "FAILED", "CANCELLED"].includes(status)) {
        return "warn" as const;
    }
    return "default" as const;
}

function hasAdminRole(user: AdminUserItem) {
    return user.roles.includes("ADMIN");
}

export function AdminDashboardPage() {
    const { session } = useAuth();
    const queryClient = useQueryClient();

    const [jobKeyword, setJobKeyword] = useState("");
    const [jobStatus, setJobStatus] = useState("");
    const [jobOwnerKeyword, setJobOwnerKeyword] = useState("");

    const [userKeyword, setUserKeyword] = useState("");
    const [userStatus, setUserStatus] = useState("");
    const [userRoleCode, setUserRoleCode] = useState("");

    const [logAction, setLogAction] = useState("");
    const [logTargetType, setLogTargetType] = useState("");
    const [logOperatorUserId, setLogOperatorUserId] = useState("");

    const [feedback, setFeedback] = useState<{ tone: "good" | "warn" | "info"; text: string } | null>(null);

    const statsQuery = useQuery({
        queryKey: ["statistics-overview", "admin", session?.token],
        queryFn: () => getStatisticsOverview(session!.token),
        enabled: Boolean(session?.token)
    });

    const jobsQuery = useQuery({
        queryKey: ["admin-jobs", session?.token, jobKeyword, jobStatus, jobOwnerKeyword],
        queryFn: () => listAdminJobs(session!.token, {
            page: 1,
            size: 12,
            keyword: jobKeyword || undefined,
            status: jobStatus || undefined,
            ownerKeyword: jobOwnerKeyword || undefined
        }),
        enabled: Boolean(session?.token)
    });

    const usersQuery = useQuery({
        queryKey: ["admin-users", session?.token, userKeyword, userStatus, userRoleCode],
        queryFn: () => listAdminUsers(session!.token, {
            page: 1,
            size: 12,
            keyword: userKeyword || undefined,
            status: userStatus || undefined,
            roleCode: userRoleCode || undefined
        }),
        enabled: Boolean(session?.token)
    });

    const logsQuery = useQuery({
        queryKey: ["admin-operation-logs", session?.token, logAction, logTargetType, logOperatorUserId],
        queryFn: () => listOperationLogs(session!.token, {
            page: 1,
            size: 12,
            action: logAction || undefined,
            targetType: logTargetType || undefined,
            operatorUserId: logOperatorUserId ? Number(logOperatorUserId) : null
        }),
        enabled: Boolean(session?.token)
    });

    const updateUserMutation = useMutation({
        mutationFn: ({ userId, status }: { userId: number; status: string }) =>
            updateAdminUserStatus(session!.token, userId, status),
        onSuccess(user) {
            setFeedback({
                tone: "good",
                text: `${user.fullName} is now ${user.status}.`
            });
            void Promise.all([
                queryClient.invalidateQueries({ queryKey: ["admin-users"] }),
                queryClient.invalidateQueries({ queryKey: ["admin-operation-logs"] })
            ]);
        },
        onError(error) {
            setFeedback({
                tone: "warn",
                text: error instanceof Error ? error.message : "Could not update the user status."
            });
        }
    });

    const usersForOperatorFilter = useMemo(
        () => usersQuery.data?.records || [],
        [usersQuery.data?.records]
    );

    const jobCards = jobsQuery.data?.records || [];
    const userCards = usersQuery.data?.records || [];
    const logCards = logsQuery.data?.records || [];
    const activeFilterCount = [jobKeyword, jobStatus, jobOwnerKeyword, userKeyword, userStatus, userRoleCode, logAction, logTargetType, logOperatorUserId]
        .filter(Boolean)
        .length;
    const adminMetrics = [
        { label: "Visible jobs", value: jobCards.length },
        { label: "Visible users", value: userCards.length },
        { label: "Visible logs", value: logCards.length },
        { label: "Active filters", value: activeFilterCount }
    ];
    const adminAnalytics = statsQuery.data ? {
        progressCards: [
            {
                label: "Open jobs share",
                numerator: readCount(statsQuery.data.jobs.byStatus, "OPEN"),
                denominator: statsQuery.data.jobs.total,
                helper: "How much of the platform inventory is still open."
            },
            {
                label: "Resume coverage",
                numerator: statsQuery.data.applications.withResume,
                denominator: statsQuery.data.applications.total,
                helper: "Applications that already include an uploaded resume."
            },
            {
                label: "Upcoming interviews",
                numerator: statsQuery.data.interviews.upcoming,
                denominator: statsQuery.data.interviews.total,
                helper: "Scheduled interviews that have not happened yet."
            }
        ],
        charts: [
            {
                title: "Job status mix",
                subtitle: "Open, closed, and expired jobs across the platform.",
                items: buildDistributionItems(statsQuery.data.jobs.byStatus, JOB_STATUS_OPTIONS.filter(Boolean) as string[], getStatusTone)
            },
            {
                title: "Application status mix",
                subtitle: "Distribution of candidates through the recruiting funnel.",
                items: buildDistributionItems(statsQuery.data.applications.byStatus, [...APPLICATION_STATUS_OPTIONS], getStatusTone)
            },
            {
                title: "Interview status",
                subtitle: "How interview activity is progressing.",
                items: buildDistributionItems(statsQuery.data.interviews.byStatus, [...INTERVIEW_STATUS_OPTIONS], getStatusTone)
            },
            {
                title: "Interview outcomes",
                subtitle: "Result breakdown for tracked interviews.",
                items: buildDistributionItems(statsQuery.data.interviews.byResult, [...INTERVIEW_RESULT_OPTIONS], getStatusTone)
            }
        ]
    } : null;

    function handleToggleUserStatus(user: AdminUserItem) {
        const nextStatus = user.status === "ACTIVE" ? "DISABLED" : "ACTIVE";
        updateUserMutation.mutate({ userId: user.id, status: nextStatus });
    }

    function clearJobFilters() {
        setJobKeyword("");
        setJobStatus("");
        setJobOwnerKeyword("");
    }

    function clearUserFilters() {
        setUserKeyword("");
        setUserStatus("");
        setUserRoleCode("");
    }

    function clearLogFilters() {
        setLogAction("");
        setLogTargetType("");
        setLogOperatorUserId("");
    }

    return (
        <div className="page-grid">
            <SectionCard
                eyebrow="Admin Workspace"
                title="See the whole recruiting system breathe"
                description="The React admin console now reads live statistics, cross-team jobs, user controls, and operation logs from the backend."
                action={
                    <button
                        className="button button--ghost"
                        onClick={() => {
                            void Promise.all([
                                statsQuery.refetch(),
                                jobsQuery.refetch(),
                                usersQuery.refetch(),
                                logsQuery.refetch()
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
                            <li>Global recruiting statistics with live backend data</li>
                            <li>Cross-HR job oversight filters and inventory view</li>
                            <li>User management with status toggle controls</li>
                            <li>Operation log filtering across action, target, and operator</li>
                        </ul>
                    </article>
                    <article className="panel">
                        <strong>Live global stats</strong>
                        {statsQuery.isLoading ? (
                            <p className="muted-text">Loading admin overview...</p>
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
                                description="Admin overview data will appear here after the query runs."
                            />
                        )}
                    </article>
                </div>
                <div className="metric-grid">
                    {adminMetrics.map(metric => (
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
                title="Track platform health visually"
                description={statsQuery.data
                    ? `Snapshot generated at ${formatDateTime(statsQuery.data.generatedAt)} for the global admin scope.`
                    : "Use the live statistics overview to spot recruiting bottlenecks and platform health quickly."}
            >
                {statsQuery.isLoading ? (
                    <EmptyState
                        title="Loading admin analytics"
                        description="Pulling the latest platform-wide recruiting snapshot."
                    />
                ) : statsQuery.isError ? (
                    <EmptyState
                        title="Could not load analytics"
                        description={statsQuery.error instanceof Error ? statsQuery.error.message : "Unexpected error"}
                    />
                ) : adminAnalytics ? (
                    <div className="page-grid">
                        <div className="chart-grid chart-grid--three">
                            {adminAnalytics.progressCards.map(card => (
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
                            {adminAnalytics.charts.map(chart => (
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
                        description="Charts will appear here once the statistics overview returns data."
                    />
                )}
            </SectionCard>

            <SectionCard
                eyebrow="Jobs Oversight"
                title="Monitor all recruiting postings"
                description="This is the admin view across every recruiter-owned job in the system."
                action={
                    <div className="section-toolbar">
                        <button className="button button--ghost" onClick={clearJobFilters} type="button">
                            Clear Filters
                        </button>
                        <button
                            className="button button--ghost"
                            onClick={() => {
                                void jobsQuery.refetch();
                            }}
                            type="button"
                        >
                            Refresh Jobs
                        </button>
                    </div>
                }
            >
                <div className="filter-grid">
                    <div className="form-field">
                        <label htmlFor="admin-job-keyword">Keyword</label>
                        <input
                            id="admin-job-keyword"
                            onChange={event => setJobKeyword(event.target.value)}
                            placeholder="Search title or description"
                            type="text"
                            value={jobKeyword}
                        />
                    </div>
                    <div className="form-field">
                        <label htmlFor="admin-job-status">Status</label>
                        <select
                            id="admin-job-status"
                            onChange={event => setJobStatus(event.target.value)}
                            value={jobStatus}
                        >
                            <option value="">All statuses</option>
                            {JOB_STATUS_OPTIONS.filter(Boolean).map(option => (
                                <option key={option} value={option}>
                                    {option}
                                </option>
                            ))}
                        </select>
                    </div>
                    <div className="form-field">
                        <label htmlFor="admin-job-owner">Owner keyword</label>
                        <input
                            id="admin-job-owner"
                            onChange={event => setJobOwnerKeyword(event.target.value)}
                            placeholder="Owner name or email"
                            type="text"
                            value={jobOwnerKeyword}
                        />
                    </div>
                </div>
                <p className="field-note field-note--info">
                    Showing {jobCards.length} jobs{activeFilterCount ? ` with ${activeFilterCount} active filters across the workspace` : ""}.
                </p>

                {jobsQuery.isLoading ? (
                    <p className="muted-text">Loading admin jobs...</p>
                ) : jobCards.length ? (
                    <div className="job-grid">
                        {jobCards.map((job: AdminJobItem) => (
                            <article className="job-card" key={job.id}>
                                <div className="job-card__header">
                                    <div>
                                        <h3>{job.title}</h3>
                                        <p>{job.city || "Unknown city"} · {job.category || "General"}</p>
                                    </div>
                                    <StatusPill tone={getStatusTone(job.status)}>{job.status}</StatusPill>
                                </div>
                                <p>{job.description}</p>
                                <div className="job-card__meta">
                                    <StatusPill tone="info">{job.employmentType || "N/A"}</StatusPill>
                                    <StatusPill tone="info">{job.experienceLevel || "N/A"}</StatusPill>
                                    <StatusPill tone="good">{formatSalaryRange(job)}</StatusPill>
                                </div>
                                <div className="meta-stack">
                                    <p className="muted-text">
                                        Owner: {job.ownerName || "Unknown"} · {job.ownerEmail || "No email"}
                                    </p>
                                    <p className="muted-text">
                                        Owner status: {job.ownerStatus || "Unknown"} · Deadline {formatDateTime(job.applicationDeadline)}
                                    </p>
                                </div>
                            </article>
                        ))}
                    </div>
                ) : (
                    <EmptyState
                        title="No jobs match the current filters"
                        description="Try clearing one of the filters to widen the admin jobs query."
                    />
                )}
            </SectionCard>

            <div className="dashboard-grid dashboard-grid--two">
                <SectionCard
                    eyebrow="User Management"
                    title="Enable or disable platform accounts"
                    description="This page uses the lightweight admin user management API already built in the backend."
                    action={
                        <div className="section-toolbar">
                            <button className="button button--ghost" onClick={clearUserFilters} type="button">
                                Clear Filters
                            </button>
                            <button
                                className="button button--ghost"
                                onClick={() => {
                                    void usersQuery.refetch();
                                }}
                                type="button"
                            >
                                Refresh Users
                            </button>
                        </div>
                    }
                >
                    <div className="filter-grid">
                        <div className="form-field">
                            <label htmlFor="admin-user-keyword">Keyword</label>
                            <input
                                id="admin-user-keyword"
                                onChange={event => setUserKeyword(event.target.value)}
                                placeholder="Search name or email"
                                type="text"
                                value={userKeyword}
                            />
                        </div>
                        <div className="form-field">
                            <label htmlFor="admin-user-status">Status</label>
                            <select
                                id="admin-user-status"
                                onChange={event => setUserStatus(event.target.value)}
                                value={userStatus}
                            >
                                <option value="">All statuses</option>
                                {USER_STATUS_OPTIONS.filter(Boolean).map(option => (
                                    <option key={option} value={option}>
                                        {option}
                                    </option>
                                ))}
                            </select>
                        </div>
                        <div className="form-field">
                            <label htmlFor="admin-user-role">Role</label>
                            <select
                                id="admin-user-role"
                                onChange={event => setUserRoleCode(event.target.value)}
                                value={userRoleCode}
                            >
                                <option value="">All roles</option>
                                {USER_ROLE_OPTIONS.filter(Boolean).map(option => (
                                    <option key={option} value={option}>
                                        {option}
                                    </option>
                                ))}
                            </select>
                        </div>
                    </div>
                    <p className="field-note field-note--info">
                        Showing {userCards.length} users in the current admin view.
                    </p>

                    {usersQuery.isLoading ? (
                        <p className="muted-text">Loading users...</p>
                    ) : userCards.length ? (
                        <div className="stack-list">
                            {userCards.map((user: AdminUserItem) => {
                                const isSelf = user.id === session?.user.id;
                                const nextStatus = user.status === "ACTIVE" ? "DISABLED" : "ACTIVE";
                                return (
                                    <article className="list-card list-card--static" key={user.id}>
                                        <div className="list-card__top">
                                            <div>
                                                <strong>{user.fullName}</strong>
                                                <p>{user.email}</p>
                                            </div>
                                            <StatusPill tone={getStatusTone(user.status)}>{user.status}</StatusPill>
                                        </div>
                                        <div className="inline-pills inline-pills--wrap">
                                            {user.roles.map(role => (
                                                <StatusPill key={role} tone={hasAdminRole(user) ? "good" : "info"}>
                                                    {role}
                                                </StatusPill>
                                            ))}
                                        </div>
                                        <div className="meta-stack">
                                            <p className="muted-text">Phone: {user.phone || "Not provided"}</p>
                                            <p className="muted-text">Last login: {formatDateTime(user.lastLoginAt)}</p>
                                        </div>
                                        <div className="button-row button-row--compact">
                                            <span className="muted-text">
                                                {isSelf ? "Current admin session" : `Ready to switch to ${nextStatus}`}
                                            </span>
                                            <button
                                                className={user.status === "ACTIVE" ? "button button--danger" : "button button--primary"}
                                                disabled={isSelf || updateUserMutation.isPending}
                                                onClick={() => handleToggleUserStatus(user)}
                                                type="button"
                                            >
                                                {updateUserMutation.isPending ? "Saving..." : nextStatus}
                                            </button>
                                        </div>
                                    </article>
                                );
                            })}
                        </div>
                    ) : (
                        <EmptyState
                            title="No users match the current filters"
                            description="Adjust the keyword, status, or role filters to widen the query."
                        />
                    )}
                </SectionCard>

                <SectionCard
                    eyebrow="Operation Logs"
                    title="Audit the key write actions"
                    description="The admin log stream captures job, application, interview, and user status changes."
                    action={
                        <div className="section-toolbar">
                            <button className="button button--ghost" onClick={clearLogFilters} type="button">
                                Clear Filters
                            </button>
                            <button
                                className="button button--ghost"
                                onClick={() => {
                                    void logsQuery.refetch();
                                }}
                                type="button"
                            >
                                Refresh Logs
                            </button>
                        </div>
                    }
                >
                    <div className="filter-grid">
                        <div className="form-field">
                            <label htmlFor="admin-log-action">Action</label>
                            <select
                                id="admin-log-action"
                                onChange={event => setLogAction(event.target.value)}
                                value={logAction}
                            >
                                <option value="">All actions</option>
                                {LOG_ACTION_OPTIONS.filter(Boolean).map(option => (
                                    <option key={option} value={option}>
                                        {option}
                                    </option>
                                ))}
                            </select>
                        </div>
                        <div className="form-field">
                            <label htmlFor="admin-log-target">Target</label>
                            <select
                                id="admin-log-target"
                                onChange={event => setLogTargetType(event.target.value)}
                                value={logTargetType}
                            >
                                <option value="">All targets</option>
                                {LOG_TARGET_OPTIONS.filter(Boolean).map(option => (
                                    <option key={option} value={option}>
                                        {option}
                                    </option>
                                ))}
                            </select>
                        </div>
                        <div className="form-field">
                            <label htmlFor="admin-log-operator">Operator</label>
                            <select
                                id="admin-log-operator"
                                onChange={event => setLogOperatorUserId(event.target.value)}
                                value={logOperatorUserId}
                            >
                                <option value="">All operators</option>
                                {usersForOperatorFilter.map(user => (
                                    <option key={user.id} value={user.id}>
                                        {user.fullName} ({user.email})
                                    </option>
                                ))}
                            </select>
                        </div>
                    </div>
                    <p className="field-note field-note--info">
                        Showing {logCards.length} log entries in the current audit stream.
                    </p>

                    {logsQuery.isLoading ? (
                        <p className="muted-text">Loading operation logs...</p>
                    ) : logCards.length ? (
                        <div className="stack-list">
                            {logCards.map((log: OperationLogItem) => (
                                <article className="list-card list-card--static" key={log.id}>
                                    <div className="list-card__top">
                                        <div>
                                            <strong>{log.action}</strong>
                                            <p>{log.operatorName || log.operatorEmail || "Unknown operator"}</p>
                                        </div>
                                        <StatusPill tone="info">{log.targetType}</StatusPill>
                                    </div>
                                    <div className="inline-pills inline-pills--wrap">
                                        {log.operatorRoles.map(role => (
                                            <StatusPill key={`${log.id}-${role}`} tone="good">
                                                {role}
                                            </StatusPill>
                                        ))}
                                    </div>
                                    <p>{log.details || "No details recorded."}</p>
                                    <div className="button-row button-row--compact">
                                        <span className="muted-text">Target #{log.targetId ?? "N/A"}</span>
                                        <span className="muted-text">{formatDateTime(log.createdAt)}</span>
                                    </div>
                                </article>
                            ))}
                        </div>
                    ) : (
                        <EmptyState
                            title="No operation logs match the current filters"
                            description="Clear one of the filters to inspect a broader audit stream."
                        />
                    )}
                </SectionCard>
            </div>
        </div>
    );
}
