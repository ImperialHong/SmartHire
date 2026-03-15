import type { PropsWithChildren } from "react";
import { createContext, useContext, useEffect, useMemo, useState } from "react";
import { DEMO_CREDENTIALS } from "../../features/auth/constants";
import { login } from "../../features/auth/api/login";
import { getCurrentUser } from "../../features/auth/api/me";
import type { AuthSession, AuthUser, UserRole } from "../../features/auth/types";

const STORAGE_KEY = "smarthire.frontend.session";

interface AuthContextValue {
    session: AuthSession | null;
    isHydrating: boolean;
    loginWithDemoRole: (role: UserRole) => Promise<AuthSession>;
    logout: () => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

function loadStoredSession(): AuthSession | null {
    try {
        const raw = localStorage.getItem(STORAGE_KEY);
        if (!raw) {
            return null;
        }
        const parsed = JSON.parse(raw) as AuthSession;
        if (!parsed?.token || !parsed?.user) {
            return null;
        }
        return parsed;
    } catch {
        return null;
    }
}

function persistSession(session: AuthSession | null) {
    if (!session) {
        localStorage.removeItem(STORAGE_KEY);
        return;
    }
    localStorage.setItem(STORAGE_KEY, JSON.stringify(session));
}

export function AuthProvider({ children }: PropsWithChildren) {
    const [session, setSession] = useState<AuthSession | null>(() => loadStoredSession());
    const [isHydrating, setIsHydrating] = useState(Boolean(loadStoredSession()?.token));

    useEffect(() => {
        let isMounted = true;
        const existingSession = loadStoredSession();

        if (!existingSession?.token) {
            setIsHydrating(false);
            return () => {
                isMounted = false;
            };
        }

        getCurrentUser(existingSession.token)
            .then((user: AuthUser) => {
                if (!isMounted) {
                    return;
                }
                const nextSession = {
                    token: existingSession.token,
                    user
                };
                setSession(nextSession);
                persistSession(nextSession);
            })
            .catch(() => {
                if (!isMounted) {
                    return;
                }
                setSession(null);
                persistSession(null);
            })
            .finally(() => {
                if (isMounted) {
                    setIsHydrating(false);
                }
            });

        return () => {
            isMounted = false;
        };
    }, []);

    const value = useMemo<AuthContextValue>(() => ({
        session,
        isHydrating,
        async loginWithDemoRole(role: UserRole) {
            const response = await login(DEMO_CREDENTIALS[role]);
            const nextSession = {
                token: response.accessToken,
                user: response.user
            };
            setSession(nextSession);
            persistSession(nextSession);
            return nextSession;
        },
        logout() {
            setSession(null);
            persistSession(null);
        }
    }), [isHydrating, session]);

    return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
    const context = useContext(AuthContext);
    if (!context) {
        throw new Error("useAuth must be used within AuthProvider");
    }
    return context;
}
