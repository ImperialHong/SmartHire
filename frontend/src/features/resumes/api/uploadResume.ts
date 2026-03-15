import { apiRequest } from "../../../shared/api/client";
import type { ResumeUploadResult } from "../types";

export function uploadResume(token: string, file: File) {
    const formData = new FormData();
    formData.append("file", file);

    return apiRequest<ResumeUploadResult>("/api/resumes/upload", {
        method: "POST",
        token,
        body: formData
    });
}
