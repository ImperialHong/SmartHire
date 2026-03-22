import { useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { useAuth } from "../../app/providers/AuthProvider";
import { resolveDefaultWorkspace } from "../../features/auth/utils/resolveWorkspace";
import { usePublicJobs } from "../../features/jobs/hooks/usePublicJobs";
import { EmptyState } from "../../shared/components/EmptyState";
import { SectionCard } from "../../shared/components/SectionCard";
import { StatusPill } from "../../shared/components/StatusPill";
import { getBackendOrigin } from "../../shared/api/client";
import { formatDateTime, formatSalaryRange } from "../../shared/lib/formatters";

export function PublicJobsPage() {
    const { session } = useAuth();
    const jobsQuery = usePublicJobs();
    const [keyword, setKeyword] = useState("");
    const [cityFilter, setCityFilter] = useState("");
    const [categoryFilter, setCategoryFilter] = useState("");
    const allJobs = jobsQuery.data?.records || [];

    const cityOptions = useMemo(
        () => Array.from(new Set(allJobs.flatMap(job => (job.city ? [job.city] : [])))).sort(),
        [allJobs]
    );
    const categoryOptions = useMemo(
        () => Array.from(new Set(allJobs.flatMap(job => (job.category ? [job.category] : [])))).sort(),
        [allJobs]
    );
    const filteredJobs = useMemo(() => {
        const normalizedKeyword = keyword.trim().toLowerCase();
        return allJobs.filter(job => {
            const matchesKeyword = normalizedKeyword
                ? [job.title, job.description, job.city, job.category]
                    .some(value => typeof value === "string" && value.toLowerCase().includes(normalizedKeyword))
                : true;
            const matchesCity = cityFilter ? job.city === cityFilter : true;
            const matchesCategory = categoryFilter ? job.category === categoryFilter : true;
            return matchesKeyword && matchesCity && matchesCategory;
        });
    }, [allJobs, keyword, cityFilter, categoryFilter]);
    const preferredWorkspace = session ? resolveDefaultWorkspace(session.user.roles) : null;

    function clearFilters() {
        setKeyword("");
        setCityFilter("");
        setCategoryFilter("");
    }

    return (
        <div className="page-grid">
            <section className="hero-card">
                <div>
                    <p className="eyebrow">Recruiting workflow demo</p>
                    <h2>Build the independent frontend on top of the backend story you already own.</h2>
                    <p className="lead">
                        The independent React app now has real public browsing plus candidate, HR,
                        and admin workflows wired into the Spring Boot API.
                    </p>
                    <div className="hero-actions">
                        <a className="button" href={`${getBackendOrigin()}/swagger-ui/index.html`} rel="noreferrer" target="_blank">
                            Open Swagger
                        </a>
                        <StatusPill tone="info">
                            {session ? `Active session: ${session.user.roles.join(", ")}` : "No active session"}
                        </StatusPill>
                        {preferredWorkspace && preferredWorkspace !== "/" ? (
                            <Link className="button button--primary" to={preferredWorkspace}>
                                Open My Workspace
                            </Link>
                        ) : null}
                    </div>
                    <div className="hero-summary">
                        <article className="hero-summary__item">
                            <span className="hero-summary__label">Open jobs</span>
                            <strong className="hero-summary__value">{allJobs.length}</strong>
                        </article>
                        <article className="hero-summary__item">
                            <span className="hero-summary__label">Hiring cities</span>
                            <strong className="hero-summary__value">{cityOptions.length}</strong>
                        </article>
                        <article className="hero-summary__item">
                            <span className="hero-summary__label">Categories</span>
                            <strong className="hero-summary__value">{categoryOptions.length}</strong>
                        </article>
                    </div>
                </div>
                <div className="callout">
                    <strong>Role-based access</strong>
                    <p>Register as a candidate or HR user, or sign in with any existing account and we will route you automatically.</p>
                    <div className="inline-pills inline-pills--wrap">
                        <StatusPill tone="info">Candidate apply flow</StatusPill>
                        <StatusPill tone="info">HR review flow</StatusPill>
                        <StatusPill tone="info">Admin accounts default to the admin workspace</StatusPill>
                    </div>
                </div>
            </section>

            <div className="dashboard-grid dashboard-grid--two">
                <SectionCard
                    eyebrow="Current Focus"
                    title="What the independent app already handles"
                    description="Shared auth, route guards, live API queries, and all three role workspaces are now wired in."
                >
                    <ul className="feature-list">
                        <li>React Router split for public, candidate, HR, and admin pages</li>
                        <li>JWT session persistence backed by the existing `/api/auth` endpoints</li>
                        <li>Vite dev server proxy for local API development on port `5173`</li>
                        <li>Live public jobs plus candidate, HR, and admin workflows</li>
                    </ul>
                </SectionCard>

                <SectionCard
                    eyebrow="Next Build"
                    title="Most valuable screens to flesh out next"
                    description="The next highest-value slice is polish: richer feedback, stronger visuals, and production-grade validation."
                >
                    <ul className="feature-list">
                        <li>Candidate: polish error states, filters, and richer timeline presentation</li>
                        <li>HR: refine form ergonomics and bulk review cues</li>
                        <li>Admin: denser oversight views, charts, and better audit storytelling</li>
                    </ul>
                </SectionCard>
            </div>

            <SectionCard
                eyebrow="Public Jobs"
                title="Live job list from the backend API"
                description="This already queries the current `/api/jobs` endpoint instead of using mock data."
                action={
                    <div className="section-toolbar">
                        <button className="button button--ghost" onClick={clearFilters} type="button">
                            Clear Filters
                        </button>
                        <button
                            className="button button--ghost"
                            onClick={() => {
                                void jobsQuery.refetch();
                            }}
                            type="button"
                        >
                            Refresh
                        </button>
                    </div>
                }
            >
                <div className="filter-grid">
                    <div className="form-field">
                        <label htmlFor="public-job-keyword">Keyword</label>
                        <input
                            id="public-job-keyword"
                            onChange={event => setKeyword(event.target.value)}
                            placeholder="Search title, city, category, or description"
                            type="text"
                            value={keyword}
                        />
                    </div>
                    <div className="form-field">
                        <label htmlFor="public-job-city">City</label>
                        <select
                            id="public-job-city"
                            onChange={event => setCityFilter(event.target.value)}
                            value={cityFilter}
                        >
                            <option value="">All cities</option>
                            {cityOptions.map(city => (
                                <option key={city} value={city}>
                                    {city}
                                </option>
                            ))}
                        </select>
                    </div>
                    <div className="form-field">
                        <label htmlFor="public-job-category">Category</label>
                        <select
                            id="public-job-category"
                            onChange={event => setCategoryFilter(event.target.value)}
                            value={categoryFilter}
                        >
                            <option value="">All categories</option>
                            {categoryOptions.map(category => (
                                <option key={category} value={category}>
                                    {category}
                                </option>
                            ))}
                        </select>
                    </div>
                </div>
                <p className="field-note field-note--info">
                    Showing {filteredJobs.length} of {allJobs.length} live jobs
                </p>
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
                ) : filteredJobs.length ? (
                    <div className="job-grid">
                        {filteredJobs.map(job => (
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
                        title={allJobs.length ? "No jobs match the current filters" : "No open jobs"}
                        description={allJobs.length
                            ? "Try clearing one or more filters to widen the search."
                            : "The public list returned zero records."}
                    />
                )}
            </SectionCard>
        </div>
    );
}
