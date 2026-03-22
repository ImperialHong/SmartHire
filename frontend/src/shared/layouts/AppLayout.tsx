import { useState } from "react";
import { NavLink, Outlet, useLocation } from "react-router-dom";
import { useAuth } from "../../app/providers/AuthProvider";
import { LoginDialog } from "../../features/auth/components/LoginDialog";
import { RegisterDialog } from "../../features/auth/components/RegisterDialog";
import { resolveDefaultWorkspace } from "../../features/auth/utils/resolveWorkspace";
import { StatusPill } from "../components/StatusPill";

const workspaceItems = [
    { role: "CANDIDATE", to: "/candidate", label: "Candidate" },
    { role: "HR", to: "/hr", label: "HR" },
    { role: "ADMIN", to: "/admin", label: "Admin" }
] as const;

export function AppLayout() {
    const { session, logout, isHydrating } = useAuth();
    const location = useLocation();
    const [activeAuthDialog, setActiveAuthDialog] = useState<"login" | "register" | null>(null);
    const navigationItems = [
        { to: "/", label: "Public Jobs" },
        ...(session
            ? workspaceItems.filter(item => session.user.roles.includes(item.role))
            : [])
    ];
    const preferredWorkspace = session
        ? resolveDefaultWorkspace(session.user.roles)
        : null;
    const isOnPreferredWorkspace = preferredWorkspace
        ? location.pathname === preferredWorkspace || location.pathname.startsWith(`${preferredWorkspace}/`)
        : false;

    return (
        <div className="app-frame">
            <header className="topbar">
                <div className="brand-block">
                    <p className="eyebrow">Independent Frontend</p>
                    <h1>SmartHire</h1>
                </div>

                <nav className="topbar__nav" aria-label="Primary navigation">
                    {navigationItems.map(item => (
                        <NavLink
                            key={item.to}
                            className={({ isActive }) =>
                                isActive ? "topbar__link topbar__link--active" : "topbar__link"
                            }
                            to={item.to}
                        >
                            {item.label}
                        </NavLink>
                    ))}
                </nav>

                <div className="session-box">
                    {isHydrating ? (
                        <StatusPill tone="info">Restoring session...</StatusPill>
                    ) : session ? (
                        <>
                            <div className="session-box__content">
                                <div>
                                    <strong>{session.user.fullName}</strong>
                                    <p>{session.user.email}</p>
                                </div>
                                <div className="session-box__meta">
                                    <span className="muted-text">Access</span>
                                    <div className="inline-pills">
                                        {session.user.roles.map(role => (
                                            <StatusPill key={role} tone="info">
                                                {role}
                                            </StatusPill>
                                        ))}
                                    </div>
                                </div>
                            </div>
                            <div className="button-row">
                                {preferredWorkspace && preferredWorkspace !== "/" && !isOnPreferredWorkspace ? (
                                    <NavLink className="button button--primary" to={preferredWorkspace}>
                                        Open Workspace
                                    </NavLink>
                                ) : null}
                                <button className="button button--ghost" onClick={logout} type="button">
                                    Logout
                                </button>
                            </div>
                        </>
                    ) : (
                        <>
                            <div className="session-box__content">
                                <div>
                                    <strong>Guest mode</strong>
                                    <p>Browse public jobs now, then sign in to unlock your role workspace.</p>
                                </div>
                                <div className="session-box__meta">
                                    <span className="muted-text">Available access</span>
                                    <div className="inline-pills">
                                        <StatusPill tone="info">Candidate</StatusPill>
                                        <StatusPill tone="info">HR</StatusPill>
                                        <StatusPill tone="info">Admin existing accounts</StatusPill>
                                    </div>
                                </div>
                            </div>
                            <div className="button-row">
                                <button
                                    className="button button--primary"
                                    onClick={() => setActiveAuthDialog("login")}
                                    type="button"
                                >
                                    Login
                                </button>
                                <button
                                    className="button button--ghost"
                                    onClick={() => setActiveAuthDialog("register")}
                                    type="button"
                                >
                                    Register
                                </button>
                            </div>
                        </>
                    )}
                </div>
            </header>

            <main className="page-shell">
                <Outlet />
            </main>

            <LoginDialog isOpen={activeAuthDialog === "login"} onClose={() => setActiveAuthDialog(null)} />
            <RegisterDialog isOpen={activeAuthDialog === "register"} onClose={() => setActiveAuthDialog(null)} />
        </div>
    );
}
