import type { JobSummary } from "../../features/jobs/types";

export function formatSalaryRange(job: Pick<JobSummary, "salaryMin" | "salaryMax">) {
    if (job.salaryMin == null && job.salaryMax == null) {
        return "Salary TBD";
    }
    if (job.salaryMin != null && job.salaryMax != null) {
        return `${job.salaryMin} - ${job.salaryMax}`;
    }
    return job.salaryMin != null ? `From ${job.salaryMin}` : `Up to ${job.salaryMax}`;
}

export function formatDateTime(value: string | null | undefined) {
    if (!value) {
        return "Not set";
    }
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) {
        return value;
    }
    return date.toLocaleString();
}
