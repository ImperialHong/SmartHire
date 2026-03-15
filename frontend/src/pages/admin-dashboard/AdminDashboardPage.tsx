import { useQuery } from "@tanstack/react-query";
import { useAuth } from "../../app/providers/AuthProvider";
import { getStatisticsOverview } from "../../features/statistics/api/getStatisticsOverview";
import { EmptyState } from "../../shared/components/EmptyState";
import { SectionCard } from "../../shared/components/SectionCard";
import { StatusPill } from "../../shared/components/StatusPill";

export function AdminDashboardPage() {
    const { session } = useAuth();
    const statsQuery = useQuery({
        queryKey: ["statistics-overview", "admin", session?.token],
        queryFn: () => getStatisticsOverview(session!.token),
        enabled: Boolean(session?.token)
    });

    return (
        <div className="page-grid">
            <SectionCard
                eyebrow="Admin Workspace"
                title="See the whole recruiting system breathe"
                description="This page is ready for admin job oversight, user management, and operation log views."
            >
                <div className="dashboard-grid dashboard-grid--three">
                    <article className="panel">
                        <strong>Identity</strong>
                        <p>{session?.user.fullName}</p>
                        <p className="muted-text">{session?.user.email}</p>
                        <div className="inline-pills">
                            {session?.user.roles.map(role => (
                                <StatusPill key={role} tone="good">
                                    {role}
                                </StatusPill>
                            ))}
                        </div>
                    </article>
                    <article className="panel">
                        <strong>Next component slice</strong>
                        <ul className="feature-list">
                            <li>Admin jobs table with status actions</li>
                            <li>User management filters and status toggles</li>
                            <li>Operation log timeline and filters</li>
                            <li>Charts for global recruiting overview</li>
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
                                    <span>Scope</span>
                                    <strong>{statsQuery.data.scope}</strong>
                                </div>
                                <div className="metric-card">
                                    <span>Jobs</span>
                                    <strong>{statsQuery.data.jobs.total}</strong>
                                </div>
                                <div className="metric-card">
                                    <span>Applications</span>
                                    <strong>{statsQuery.data.applications.total}</strong>
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
            </SectionCard>
        </div>
    );
}
