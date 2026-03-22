import type { FormEvent } from "react";
import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../../../app/providers/AuthProvider";
import { resolveDefaultWorkspace } from "../utils/resolveWorkspace";
import type { RegisterRole } from "../types";

interface RegisterDialogProps {
    isOpen: boolean;
    onClose: () => void;
}

export function RegisterDialog({ isOpen, onClose }: RegisterDialogProps) {
    const navigate = useNavigate();
    const { registerWithDetails } = useAuth();
    const [roleCode, setRoleCode] = useState<RegisterRole>("CANDIDATE");
    const [fullName, setFullName] = useState("");
    const [email, setEmail] = useState("");
    const [phone, setPhone] = useState("");
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

        setRoleCode("CANDIDATE");
        setFullName("");
        setEmail("");
        setPhone("");
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
            const nextSession = await registerWithDetails({
                fullName: fullName.trim(),
                email: email.trim(),
                phone: phone.trim() || undefined,
                password,
                roleCode
            });
            onClose();
            navigate(resolveDefaultWorkspace(nextSession.user.roles));
        } catch (submitError) {
            setError(submitError instanceof Error ? submitError.message : "Registration failed.");
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
                        <p className="eyebrow">Create Account</p>
                        <h2>Register your workspace</h2>
                        <p className="section-card__description">
                            Choose whether you are joining as a candidate or an HR user. Admin accounts are provisioned separately.
                        </p>
                    </div>
                    <button
                        aria-label="Close register dialog"
                        className="button button--ghost"
                        onClick={onClose}
                        type="button"
                    >
                        Close
                    </button>
                </div>

                <form className="auth-form" onSubmit={handleSubmit}>
                    <div className="toggle-group" role="tablist" aria-label="Role selection">
                        <button
                            className={roleCode === "CANDIDATE" ? "toggle-chip toggle-chip--active" : "toggle-chip"}
                            onClick={() => setRoleCode("CANDIDATE")}
                            role="tab"
                            type="button"
                        >
                            Register as Candidate
                        </button>
                        <button
                            className={roleCode === "HR" ? "toggle-chip toggle-chip--active" : "toggle-chip"}
                            onClick={() => setRoleCode("HR")}
                            role="tab"
                            type="button"
                        >
                            Register as HR
                        </button>
                    </div>

                    <div className="form-field">
                        <label htmlFor="register-full-name">Full name</label>
                        <input
                            id="register-full-name"
                            autoComplete="name"
                            onChange={event => setFullName(event.target.value)}
                            placeholder={roleCode === "CANDIDATE" ? "Jane Candidate" : "Alex Recruiter"}
                            type="text"
                            value={fullName}
                        />
                    </div>

                    <div className="form-field">
                        <label htmlFor="register-email">Email</label>
                        <input
                            id="register-email"
                            autoComplete="username"
                            onChange={event => setEmail(event.target.value)}
                            placeholder={roleCode === "CANDIDATE" ? "candidate@example.com" : "hr@example.com"}
                            type="email"
                            value={email}
                        />
                    </div>

                    <div className="form-field">
                        <label htmlFor="register-phone">Phone</label>
                        <input
                            id="register-phone"
                            autoComplete="tel"
                            onChange={event => setPhone(event.target.value)}
                            placeholder="Optional contact number"
                            type="tel"
                            value={phone}
                        />
                    </div>

                    <div className="form-field">
                        <label htmlFor="register-password">Password</label>
                        <input
                            id="register-password"
                            autoComplete="new-password"
                            minLength={8}
                            onChange={event => setPassword(event.target.value)}
                            placeholder="Use at least 8 characters"
                            type="password"
                            value={password}
                        />
                    </div>

                    <p className="field-note field-note--info">
                        New accounts are activated immediately and will open the matching workspace after sign-up.
                    </p>

                    {error ? <div className="notice notice--warn">{error}</div> : null}

                    <div className="button-row">
                        <button
                            className="button button--primary"
                            disabled={!fullName.trim() || !email.trim() || password.length < 8 || isSubmitting}
                            type="submit"
                        >
                            {isSubmitting ? "Creating account..." : "Create account"}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
}
