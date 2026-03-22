import type { UserRole } from "../types";

export function resolveDefaultWorkspace(roles: UserRole[]) {
    if (roles.includes("ADMIN")) {
        return "/admin";
    }

    if (roles.includes("HR")) {
        return "/hr";
    }

    if (roles.includes("CANDIDATE")) {
        return "/candidate";
    }

    return "/";
}
