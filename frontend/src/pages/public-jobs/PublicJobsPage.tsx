import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../../app/providers/AuthProvider";
import { DEMO_CREDENTIALS } from "../../features/auth/constants";
import type { UserRole } from "../../features/auth/types";
import { usePublicJobs } from "../../features/jobs/hooks/usePublicJobs";
import { EmptyState } from "../../shared/components/EmptyState";
import { SectionCard } from "../../shared/components/SectionCard";
import { StatusPill } from "../../shared/components/StatusPill";
import { getBackendOrigin } from "../../shared/api/client";
import { formatDateTime, formatSalaryRange } from "../../shared/lib/formatters";

const destinationByRole: Record<UserRole, string> = {
    CANDIDATE: "/candidate",
    HR: "/hr",
    ADMIN: "/admin"
};

export function PublicJobsPage() {
    const navigate = useNavigate();
    const { loginWithDemoRole, session } = useAuth();
    const jobsQuery = usePublicJobs();
    const [activeRole, setActiveRole] = useState<UserRole | null>(null);

    async function handleDemoLogin(role: UserRole) {
        setActiveRole(role);
        try {
            await loginWithDemoRole(role);
            navigate(destinationByRole[role]);
        } finally {
            setActiveRole(null);
        }
    }

    return (
        <div className="page-grid">
            <section className="hero-card">
                <div>
                    <p className="eyebrow">Recruiting workflow demo</p>
                    <h2>Build the independent frontend on top of the backend story you already own.</h2>
                    <p className="lead">
                        The independent React app now has real public browsing, candidate workflow,
                        and HR workflow wired into the Spring Boot API, with admin next in line.
                    </p>
                    <div className="hero-actions">
                        <a className="button" href={`${getBackendOrigin()}/swagger-ui/index.html`} rel="noreferrer" target="_blank">
                            Open Swagger
                        </a>
                        <StatusPill tone="info">
                            {session ? `Active session: ${session.user.roles.join(", ")}` : "No active session"}
                        </StatusPill>
                    </div>
                </div>
                <div className="callout">
                    <strong>Seeded demo logins</strong>
                    <p>Use the same demo accounts that already exist in the backend seed data.</p>
                    <div className="demo-grid">
                        {(Object.entries(DEMO_CREDENTIALS) as Array<[UserRole, { email: string }]>).map(([role, credentials]) => (
                            <button
                                key={role}
                                className="button button--primary"
                                disabled={activeRole === role}
                                onClick={() => void handleDemoLogin(role)}
                                type="button"
                            >
                                {activeRole === role ? `Signing in ${role}...` : `${role} Demo Login`}
                                <span className="button__meta">{credentials.email}</span>
                            </button>
                        ))}
                    </div>
                </div>
            </section>

            <div className="dashboard-grid dashboard-grid--two">
                <SectionCard
                    eyebrow="Current Focus"
                    title="What the independent app already handles"
                    description="Shared auth, route guards, live API queries, and real candidate plus HR actions are now wired in."
                >
                    <ul className="feature-list">
                        <li>React Router split for public, candidate, HR, and admin pages</li>
                        <li>JWT session persistence backed by the existing `/api/auth` endpoints</li>
                        <li>Vite dev server proxy for local API development on port `5173`</li>
                        <li>Live public jobs, candidate apply flow, and HR management flow</li>
                    </ul>
                </SectionCard>

                <SectionCard
                    eyebrow="Next Build"
                    title="Most valuable screens to flesh out next"
                    description="The next highest-value slice is the admin workspace on top of the flows that are already live."
                >
                    <ul className="feature-list">
                        <li>Candidate: polish error states, filters, and richer timeline presentation</li>
                        <li>HR: refine form ergonomics and bulk review cues</li>
                        <li>Admin: job oversight, user management, operation logs, overview metrics</li>
                    </ul>
                </SectionCard>
            </div>

            <SectionCard
                eyebrow="Public Jobs"
                title="Live job list from the backend API"
                description="This already queries the current `/api/jobs` endpoint instead of using mock data."
                action={
                    <button
                        className="button button--ghost"
                        onClick={() => {
                            void jobsQuery.refetch();
                        }}
                        type="button"
                    >
                        Refresh
                    </button>
                }
            >
                {jobsQuery.isLoading ? (
                    <div className="panel">
                        <EmptyState
                            title="Loading jobs"
                            description="Fetching open positions from the backend."
                        />
                    </div>
                ) : jobsQuery.isError ? (
                    <div className="panel">
                        <EmptyState
                            title="Could not load public jobs"
                            description={jobsQuery.error instanceof Error ? jobsQuery.error.message : "Unexpected error"}
                        />
                    </div>
                ) : jobsQuery.data?.records.length ? (
                    <div className="job-grid">
                        {jobsQuery.data.records.map(job => (
                            <article className="job-card" key={job.id}>
                                <div className="job-card__header">
                                    <div>
                                        <h3>{job.title}</h3>
                                        <p>{job.city || "Unknown city"} · {job.category || "General"}</p>
                                    </div>
                                    <StatusPill>{job.status}</StatusPill>
                                </div>
                                <p>{job.description}</p>
                                <div className="job-card__meta">
                                    <StatusPill tone="info">{job.employmentType || "N/A"}</StatusPill>
                                    <StatusPill tone="info">{job.experienceLevel || "N/A"}</StatusPill>
                                    <StatusPill tone="good">{formatSalaryRange(job)}</StatusPill>
                                </div>
                                <p className="muted-text">Deadline: {formatDateTime(job.applicationDeadline)}</p>
                            </article>
                        ))}
                    </div>
                ) : (
                    <EmptyState
                        title="No open jobs"
                        description="The public list returned zero records."
                    />
                )}
            </SectionCard>
        </div>
    );
}
