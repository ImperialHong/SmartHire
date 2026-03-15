export type UserRole = "CANDIDATE" | "HR" | "ADMIN";

export interface AuthUser {
    id: number;
    email: string;
    fullName: string;
    phone: string | null;
    roles: UserRole[];
}

export interface LoginRequest {
    email: string;
    password: string;
}

export interface AuthResponse {
    accessToken: string;
    tokenType: string;
    expiresIn: number;
    user: AuthUser;
}

export interface AuthSession {
    token: string;
    user: AuthUser;
}
