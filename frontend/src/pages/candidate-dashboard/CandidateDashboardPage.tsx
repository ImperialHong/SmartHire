import { useAuth } from "../../app/providers/AuthProvider";
import { SectionCard } from "../../shared/components/SectionCard";
import { StatusPill } from "../../shared/components/StatusPill";

export function CandidateDashboardPage() {
    const { session } = useAuth();

    return (
        <div className="page-grid">
            <SectionCard
                eyebrow="Candidate Workspace"
                title="Apply, track, and stay updated"
                description="This page is the home for resume upload, applications, interviews, and notifications."
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
                        <strong>Recommended next implementation</strong>
                        <ul className="feature-list">
                            <li>Resume upload card backed by `/api/resumes/upload`</li>
                            <li>Selected job apply form backed by `/api/applications`</li>
                            <li>My applications, interviews, and notifications lists</li>
                        </ul>
                    </article>
                </div>
            </SectionCard>
        </div>
    );
}
