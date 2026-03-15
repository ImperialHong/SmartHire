import type { LoginRequest, UserRole } from "./types";

export const DEMO_CREDENTIALS: Record<UserRole, LoginRequest> = {
    CANDIDATE: {
        email: "candidate@example.com",
        password: "password123"
    },
    HR: {
        email: "hr@example.com",
        password: "password123"
    },
    ADMIN: {
        email: "admin@example.com",
        password: "password123"
    }
};
