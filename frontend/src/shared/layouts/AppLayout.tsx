import { NavLink, Outlet } from "react-router-dom";
import { useAuth } from "../../app/providers/AuthProvider";
import { StatusPill } from "../components/StatusPill";

const navigationItems = [
    { to: "/", label: "Public Jobs" },
    { to: "/candidate", label: "Candidate" },
    { to: "/hr", label: "HR" },
    { to: "/admin", label: "Admin" }
];

export function AppLayout() {
    const { session, logout, isHydrating } = useAuth();

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
                        <StatusPill>Use demo login on the home page</StatusPill>
                    )}
                </div>
            </header>

            <main className="page-shell">
                <Outlet />
            </main>
        </div>
    );
}
