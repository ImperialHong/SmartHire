import type { ReactNode } from "react";
import { Navigate, useLocation } from "react-router-dom";
import { EmptyState } from "../../shared/components/EmptyState";
import { useAuth } from "../providers/AuthProvider";
import type { UserRole } from "../../features/auth/types";

interface RequireRoleProps {
    roles: UserRole[];
    children: ReactNode;
}

export function RequireRole({ roles, children }: RequireRoleProps) {
    const { session, isHydrating } = useAuth();
    const location = useLocation();

    if (isHydrating) {
        return (
            <div className="panel">
                <EmptyState
                    title="Restoring session"
                    description="Checking the saved login state before loading this workspace."
                />
            </div>
        );
    }

    if (!session) {
        return <Navigate replace state={{ from: location.pathname }} to="/" />;
    }

    const allowed = roles.some(role => session.user.roles.includes(role));
    if (!allowed) {
        return (
            <div className="panel">
                <EmptyState
                    title="Role access required"
                    description={`This workspace requires one of: ${roles.join(", ")}.`}
                />
            </div>
        );
    }

    return <>{children}</>;
}
