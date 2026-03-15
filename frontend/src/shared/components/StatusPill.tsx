import type { PropsWithChildren } from "react";

interface StatusPillProps extends PropsWithChildren {
    tone?: "default" | "good" | "warn" | "info";
}

export function StatusPill({ tone = "default", children }: StatusPillProps) {
    return <span className={`status-pill status-pill--${tone}`}>{children}</span>;
}
