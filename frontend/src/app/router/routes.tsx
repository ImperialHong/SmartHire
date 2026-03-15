import { createBrowserRouter } from "react-router-dom";
import { AppLayout } from "../../shared/layouts/AppLayout";
import { AdminDashboardPage } from "../../pages/admin-dashboard/AdminDashboardPage";
import { CandidateDashboardPage } from "../../pages/candidate-dashboard/CandidateDashboardPage";
import { HrDashboardPage } from "../../pages/hr-dashboard/HrDashboardPage";
import { PublicJobsPage } from "../../pages/public-jobs/PublicJobsPage";
import { RequireRole } from "./RequireRole";

export const router = createBrowserRouter([
    {
        path: "/",
        element: <AppLayout />,
        children: [
            {
                index: true,
                element: <PublicJobsPage />
            },
            {
                path: "candidate",
                element: (
                    <RequireRole roles={["CANDIDATE"]}>
                        <CandidateDashboardPage />
                    </RequireRole>
                )
            },
            {
                path: "hr",
                element: (
                    <RequireRole roles={["HR", "ADMIN"]}>
                        <HrDashboardPage />
                    </RequireRole>
                )
            },
            {
                path: "admin",
                element: (
                    <RequireRole roles={["ADMIN"]}>
                        <AdminDashboardPage />
                    </RequireRole>
                )
            }
        ]
    }
]);
