interface ProgressStatCardProps {
    label: string;
    numerator: number;
    denominator: number;
    helper?: string;
}

function toPercent(numerator: number, denominator: number) {
    if (!denominator) {
        return 0;
    }

    return Math.round((numerator / denominator) * 100);
}

export function ProgressStatCard({
    label,
    numerator,
    denominator,
    helper
}: ProgressStatCardProps) {
    const percent = toPercent(numerator, denominator);

    return (
        <article className="progress-stat-card">
            <div className="progress-stat-card__top">
                <span>{label}</span>
                <strong>{percent}%</strong>
            </div>
            <div className="progress-stat-card__track" aria-hidden="true">
                <div
                    className="progress-stat-card__fill"
                    style={{ width: `${percent}%` }}
                />
            </div>
            <p className="progress-stat-card__value">
                {numerator} / {denominator || 0}
            </p>
            {helper ? <p className="progress-stat-card__helper">{helper}</p> : null}
        </article>
    );
}
