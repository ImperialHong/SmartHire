import type { PropsWithChildren, ReactNode } from "react";

interface SectionCardProps extends PropsWithChildren {
    eyebrow?: string;
    title: string;
    description?: string;
    action?: ReactNode;
}

export function SectionCard({ eyebrow, title, description, action, children }: SectionCardProps) {
    return (
        <section className="section-card">
            <header className="section-card__header">
                <div>
                    {eyebrow ? <p className="eyebrow">{eyebrow}</p> : null}
                    <h2>{title}</h2>
                    {description ? <p className="section-card__description">{description}</p> : null}
                </div>
                {action ? <div>{action}</div> : null}
            </header>
            {children}
        </section>
    );
}
