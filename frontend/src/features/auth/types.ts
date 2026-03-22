export type UserRole = "CANDIDATE" | "HR" | "ADMIN";
export type RegisterRole = "CANDIDATE" | "HR";

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

export interface RegisterRequest {
    email: string;
    password: string;
    fullName: string;
    phone?: string;
    roleCode: RegisterRole;
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
