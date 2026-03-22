import { StatusPill } from "./StatusPill";

type ChartTone = "default" | "good" | "info" | "warn";

export interface DistributionChartDatum {
    label: string;
    value: number;
    tone?: ChartTone;
}

interface DistributionChartProps {
    title: string;
    subtitle?: string;
    items: DistributionChartDatum[];
    emptyMessage?: string;
}

function formatPercent(value: number, total: number) {
    if (!total) {
        return "0%";
    }

    const percentage = (value / total) * 100;
    return `${Math.round(percentage)}%`;
}

export function DistributionChart({
    title,
    subtitle,
    items,
    emptyMessage = "No data available yet."
}: DistributionChartProps) {
    const total = items.reduce((sum, item) => sum + item.value, 0);

    return (
        <article className="distribution-chart">
            <header className="distribution-chart__header">
                <div>
                    <h3>{title}</h3>
                    {subtitle ? <p>{subtitle}</p> : null}
                </div>
                <strong>{total}</strong>
            </header>

            {items.length && total ? (
                <div className="distribution-chart__rows">
                    {items.map(item => {
                        const percent = total ? (item.value / total) * 100 : 0;

                        return (
                            <div className="distribution-chart__row" key={item.label}>
                                <div className="distribution-chart__meta">
                                    <StatusPill tone={item.tone || "default"}>{item.label}</StatusPill>
                                    <span>
                                        {item.value} · {formatPercent(item.value, total)}
                                    </span>
                                </div>
                                <div className="distribution-chart__track" aria-hidden="true">
                                    <div
                                        className="distribution-chart__fill"
                                        style={{ width: `${percent}%` }}
                                    />
                                </div>
                            </div>
                        );
                    })}
                </div>
            ) : (
                <p className="distribution-chart__empty">{emptyMessage}</p>
            )}
        </article>
    );
}
