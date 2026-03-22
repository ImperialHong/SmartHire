import type { FormEvent } from "react";
import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../../../app/providers/AuthProvider";
import { resolveDefaultWorkspace } from "../utils/resolveWorkspace";

interface LoginDialogProps {
    isOpen: boolean;
    onClose: () => void;
}

export function LoginDialog({ isOpen, onClose }: LoginDialogProps) {
    const navigate = useNavigate();
    const { loginWithCredentials } = useAuth();
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        if (!isOpen) {
            return;
        }

        function handleKeyDown(event: KeyboardEvent) {
            if (event.key === "Escape") {
                onClose();
            }
        }

        window.addEventListener("keydown", handleKeyDown);
        return () => {
            window.removeEventListener("keydown", handleKeyDown);
        };
    }, [isOpen, onClose]);

    useEffect(() => {
        if (isOpen) {
            return;
        }

        setPassword("");
        setError(null);
    }, [isOpen]);

    if (!isOpen) {
        return null;
    }

    async function handleSubmit(event: FormEvent<HTMLFormElement>) {
        event.preventDefault();
        setIsSubmitting(true);
        setError(null);

        try {
            const nextSession = await loginWithCredentials({
                email: email.trim(),
                password
            });
            onClose();
            navigate(resolveDefaultWorkspace(nextSession.user.roles));
        } catch (submitError) {
            setError(submitError instanceof Error ? submitError.message : "Login failed.");
        } finally {
            setIsSubmitting(false);
        }
    }

    return (
        <div
            aria-modal="true"
            className="modal-backdrop"
            onClick={onClose}
            role="dialog"
        >
            <div className="modal-card" onClick={event => event.stopPropagation()}>
                <div className="modal-card__header">
                    <div>
                        <p className="eyebrow">Sign In</p>
                        <h2>Login to your workspace</h2>
                        <p className="section-card__description">
                            Sign in with your own email and password. We will open the right workspace based on your account role.
                        </p>
                    </div>
                    <button
                        aria-label="Close login dialog"
                        className="button button--ghost"
                        onClick={onClose}
                        type="button"
                    >
                        Close
                    </button>
                </div>

                <form className="auth-form" onSubmit={handleSubmit}>
                    <div className="form-field">
                        <label htmlFor="login-email">Email</label>
                        <input
                            id="login-email"
                            autoComplete="username"
                            onChange={event => setEmail(event.target.value)}
                            placeholder="you@example.com"
                            type="email"
                            value={email}
                        />
                    </div>

                    <div className="form-field">
                        <label htmlFor="login-password">Password</label>
                        <input
                            id="login-password"
                            autoComplete="current-password"
                            onChange={event => setPassword(event.target.value)}
                            placeholder="Enter your password"
                            type="password"
                            value={password}
                        />
                    </div>

                    <p className="field-note field-note--info">
                        Admin accounts always open the admin workspace after a successful sign-in.
                    </p>

                    {error ? <div className="notice notice--warn">{error}</div> : null}

                    <div className="button-row">
                        <button
                            className="button button--primary"
                            disabled={!email.trim() || !password || isSubmitting}
                            type="submit"
                        >
                            {isSubmitting ? "Signing in..." : "Login"}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
}
