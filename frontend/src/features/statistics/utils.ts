import type { DistributionChartDatum } from "../../shared/components/DistributionChart";

type Tone = "default" | "good" | "info" | "warn";

export function readCount(source: Record<string, number>, key: string) {
    return source[key] || 0;
}

export function buildDistributionItems(
    source: Record<string, number>,
    preferredOrder: string[],
    resolveTone: (label: string) => Tone
): DistributionChartDatum[] {
    const orderedKeys = [
        ...preferredOrder.filter(key => Object.prototype.hasOwnProperty.call(source, key)),
        ...Object.keys(source).filter(key => !preferredOrder.includes(key))
    ];

    return orderedKeys
        .map(key => ({
            label: key,
            value: source[key] || 0,
            tone: resolveTone(key)
        }))
        .filter(item => item.value > 0);
}
