import { useState } from "react";
import { NavLink, Outlet } from "react-router-dom";
import { useAuth } from "../../app/providers/AuthProvider";
import { LoginDialog } from "../../features/auth/components/LoginDialog";
import { StatusPill } from "../components/StatusPill";

const workspaceItems = [
    { role: "CANDIDATE", to: "/candidate", label: "Candidate" },
    { role: "HR", to: "/hr", label: "HR" },
    { role: "ADMIN", to: "/admin", label: "Admin" }
] as const;

export function AppLayout() {
    const { session, logout, isHydrating } = useAuth();
    const [isLoginOpen, setIsLoginOpen] = useState(false);
    const navigationItems = [
        { to: "/", label: "Public Jobs" },
        ...(session
            ? workspaceItems.filter(item => session.user.roles.includes(item.role))
            : [])
    ];

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
                            <div>
                                <strong>{session.user.fullName}</strong>
                                <p>{session.user.email}</p>
                            </div>
                            <button className="button button--ghost" onClick={logout} type="button">
                                Logout
                            </button>
                        </>
                    ) : (
                        <>
                            <div>
                                <strong>Guest mode</strong>
                                <p>Sign in to open the candidate or HR workspace.</p>
                            </div>
                            <button
                                className="button button--primary"
                                onClick={() => setIsLoginOpen(true)}
                                type="button"
                            >
                                Login
                            </button>
                        </>
                    )}
                </div>
            </header>

            <main className="page-shell">
                <Outlet />
            </main>

            <LoginDialog isOpen={isLoginOpen} onClose={() => setIsLoginOpen(false)} />
        </div>
    );
}
