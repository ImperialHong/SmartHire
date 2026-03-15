import { useQuery } from "@tanstack/react-query";
import { listPublicJobs } from "../api/listPublicJobs";

export function usePublicJobs() {
    return useQuery({
        queryKey: ["public-jobs"],
        queryFn: listPublicJobs
    });
}
