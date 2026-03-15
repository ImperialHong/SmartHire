const DEMO_CREDENTIALS = {
    candidate: { email: "candidate@example.com", password: "password123" },
    hr: { email: "hr@example.com", password: "password123" },
    admin: { email: "admin@example.com", password: "password123" }
};

const STORAGE_KEY = "smarthire.frontend.sessions";
const DEFAULT_RESUME_TEXT = "No resume uploaded yet";
const DEFAULT_JOB_FORM = {
    title: "Backend Engineer",
    description: "Build SmartHire backend services and workflow integrations.",
    city: "Shanghai",
    category: "Engineering",
    employmentType: "FULL_TIME",
    experienceLevel: "JUNIOR",
    status: "OPEN",
    salaryMin: "15000",
    salaryMax: "25000",
    applicationDeadline: ""
};

const state = {
    selectedJob: null,
    selectedApplicationId: null,
    selectedInterview: null,
    sessions: {
        candidate: { token: "", user: null },
        hr: { token: "", user: null },
        admin: { token: "", user: null }
    }
};

const dom = {
    roleButtons: {
        candidate: {
            login: document.querySelector('[data-role-login="candidate"]'),
            logout: document.querySelector('[data-role-logout="candidate"]')
        },
        hr: {
            login: document.querySelector('[data-role-login="hr"]'),
            logout: document.querySelector('[data-role-logout="hr"]')
        },
        admin: {
            login: document.querySelector('[data-role-login="admin"]'),
            logout: document.querySelector('[data-role-logout="admin"]')
        }
    },
    toast: document.querySelector("#toast"),
    jobFilterForm: document.querySelector("#job-filter-form"),
    jobList: document.querySelector("#job-list"),
    jobSummary: document.querySelector("#job-summary"),
    selectedJobOverview: document.querySelector("#selected-job-overview"),
    candidateProfile: document.querySelector("#candidate-profile"),
    resumeForm: document.querySelector("#resume-form"),
    resumePath: document.querySelector("#resume-path"),
    candidateApplyForm: document.querySelector("#candidate-apply-form"),
    selectedJobChip: document.querySelector("#selected-job-chip"),
    candidateApplications: document.querySelector("#candidate-applications"),
    candidateInterviews: document.querySelector("#candidate-interviews"),
    candidateNotifications: document.querySelector("#candidate-notifications"),
    candidateNotificationMeta: document.querySelector("#candidate-notification-meta"),
    hrProfile: document.querySelector("#hr-profile"),
    jobComposerForm: document.querySelector("#job-create-form"),
    hrSelectedJob: document.querySelector("#hr-selected-job"),
    jobFormMode: document.querySelector("#job-form-mode"),
    jobEditorNote: document.querySelector("#job-editor-note"),
    interviewForm: document.querySelector("#interview-form"),
    selectedApplicationChip: document.querySelector("#selected-application-chip"),
    selectedInterviewChip: document.querySelector("#selected-interview-chip"),
    interviewFormMode: document.querySelector("#interview-form-mode"),
    interviewEditorNote: document.querySelector("#interview-editor-note"),
    hrApplications: document.querySelector("#hr-applications"),
    hrInterviews: document.querySelector("#hr-interviews"),
    hrStats: document.querySelector("#hr-stats"),
    adminProfile: document.querySelector("#admin-profile"),
    adminStats: document.querySelector("#admin-stats"),
    adminUsers: document.querySelector("#admin-users"),
    adminLogs: document.querySelector("#admin-logs"),
    adminUserFilterForm: document.querySelector("#admin-user-filter-form"),
    adminLogFilterForm: document.querySelector("#admin-log-filter-form")
};

let toastTimer = null;

document.addEventListener("DOMContentLoaded", () => {
    hydrateSessions();
    bindEvents();
    renderAllProfiles();
    resetJobForm();
    setSelectedJob(null);
    setSelectedApplication(null);
    setSelectedInterview(null);
    initializeInterviewDefault();
    syncJobComposer();
    syncInterviewComposer();
    loadPublicJobs().catch(handleError);
    refreshSessions().catch(handleError);
});

function bindEvents() {
    document.querySelector('[data-action="load-public-jobs"]').addEventListener("click", () => {
        loadPublicJobs(new FormData(dom.jobFilterForm)).catch(handleError);
    });

    dom.jobFilterForm.addEventListener("submit", event => {
        event.preventDefault();
        loadPublicJobs(new FormData(event.currentTarget)).catch(handleError);
    });

    Object.entries(dom.roleButtons).forEach(([role, buttons]) => {
        buttons.login.addEventListener("click", () => loginRole(role, buttons.login).catch(handleError));
        buttons.logout.addEventListener("click", () => logoutRole(role));
    });

    dom.resumeForm.addEventListener("submit", async event => {
        event.preventDefault();
        const button = event.submitter || event.currentTarget.querySelector('button[type="submit"]');
        await runBusy(button, "Uploading...", async () => {
            const fileInput = event.currentTarget.querySelector("#resume-file");
            if (!fileInput.files || fileInput.files.length === 0) {
                throw new Error("Select a PDF resume first.");
            }

            const body = new FormData();
            body.append("file", fileInput.files[0]);
            const data = await api("/api/resumes/upload", { role: "candidate", method: "POST", body });
            dom.resumePath.textContent = data.filePath || "uploaded";
            showToast("Resume uploaded successfully.");
        });
    });

    dom.candidateApplyForm.addEventListener("submit", async event => {
        event.preventDefault();
        const button = event.submitter || event.currentTarget.querySelector('button[type="submit"]');
        await runBusy(button, "Submitting...", async () => {
            if (!state.selectedJob) {
                throw new Error("Select a job from the public list first.");
            }

            const formData = new FormData(event.currentTarget);
            const payload = {
                jobId: Number(state.selectedJob.id),
                resumeFilePath: dom.resumePath.textContent.startsWith("resumes/")
                    ? dom.resumePath.textContent
                    : null,
                coverLetter: normalizeText(formData.get("coverLetter"))
            };

            await api("/api/applications", {
                role: "candidate",
                method: "POST",
                body: JSON.stringify(payload)
            });
            showToast("Application submitted.");
            event.currentTarget.reset();
            await Promise.all([loadCandidateApplications(), loadCandidateNotifications()]);
        });
    });

    document.querySelector('[data-action="job-create"]').addEventListener("click", event => {
        createJobFromComposer(event.currentTarget).catch(handleError);
    });
    document.querySelector('[data-action="job-save"]').addEventListener("click", event => {
        updateSelectedJobFromComposer(event.currentTarget).catch(handleError);
    });
    document.querySelector('[data-action="job-reset"]').addEventListener("click", () => {
        resetJobComposerSelection();
    });
    document.querySelector('[data-action="job-delete"]').addEventListener("click", event => {
        deleteSelectedJob(event.currentTarget).catch(handleError);
    });

    dom.interviewForm.addEventListener("submit", async event => {
        event.preventDefault();
        const button = event.submitter || event.currentTarget.querySelector('button[type="submit"]');
        await submitInterviewForm(button);
    });
    document.querySelector('[data-action="interview-reset"]').addEventListener("click", () => {
        resetInterviewComposerSelection();
    });

    document.querySelector('[data-action="load-candidate-applications"]').addEventListener("click", () => {
        loadCandidateApplications().catch(handleError);
    });
    document.querySelector('[data-action="load-candidate-interviews"]').addEventListener("click", () => {
        loadCandidateInterviews().catch(handleError);
    });
    document.querySelector('[data-action="load-candidate-notifications"]').addEventListener("click", () => {
        loadCandidateNotifications().catch(handleError);
    });
    document.querySelector('[data-action="load-hr-applications"]').addEventListener("click", () => {
        loadHrApplications().catch(handleError);
    });
    document.querySelector('[data-action="load-hr-interviews"]').addEventListener("click", () => {
        loadHrInterviews().catch(handleError);
    });
    document.querySelector('[data-action="load-hr-stats"]').addEventListener("click", () => {
        loadHrStats().catch(handleError);
    });
    document.querySelector('[data-action="load-admin-stats"]').addEventListener("click", () => {
        loadAdminStats().catch(handleError);
    });
    document.querySelector('[data-action="load-admin-users"]').addEventListener("click", () => {
        loadAdminUsers(new FormData(dom.adminUserFilterForm)).catch(handleError);
    });
    document.querySelector('[data-action="load-admin-logs"]').addEventListener("click", () => {
        loadAdminLogs(new FormData(dom.adminLogFilterForm)).catch(handleError);
    });

    dom.adminUserFilterForm.addEventListener("submit", event => {
        event.preventDefault();
        loadAdminUsers(new FormData(event.currentTarget)).catch(handleError);
    });
    document.querySelector('[data-action="reset-admin-users"]').addEventListener("click", () => {
        dom.adminUserFilterForm.reset();
        loadAdminUsers(new FormData(dom.adminUserFilterForm)).catch(handleError);
    });

    dom.adminLogFilterForm.addEventListener("submit", event => {
        event.preventDefault();
        loadAdminLogs(new FormData(event.currentTarget)).catch(handleError);
    });
    document.querySelector('[data-action="reset-admin-logs"]').addEventListener("click", () => {
        dom.adminLogFilterForm.reset();
        loadAdminLogs(new FormData(dom.adminLogFilterForm)).catch(handleError);
    });
}

async function refreshSessions() {
    const roles = Object.keys(state.sessions);
    for (const role of roles) {
        if (!state.sessions[role].token) {
            continue;
        }
        try {
            const user = await api("/api/auth/me", { role });
            state.sessions[role].user = user;
        } catch (error) {
            clearRoleSession(role);
        }
    }

    renderAllProfiles();
    syncJobComposer();
    syncInterviewComposer();

    await Promise.all(
        roles
            .filter(role => state.sessions[role].user)
            .map(role => loadRoleData(role).catch(handleError))
    );
}

async function loginRole(role, button) {
    const credentials = DEMO_CREDENTIALS[role];
    if (!credentials) {
        return;
    }

    await runBusy(button, "Connecting...", async () => {
        const data = await api("/api/auth/login", {
            method: "POST",
            body: JSON.stringify(credentials)
        });

        state.sessions[role] = {
            token: data.accessToken,
            user: data.user
        };
        persistSessions();
        renderAllProfiles();
        syncJobComposer();
        syncInterviewComposer();
        showToast(`${labelForRole(role)} session connected.`);
        await loadRoleData(role);
    });
    renderAllProfiles();
    syncJobComposer();
    syncInterviewComposer();
}

function logoutRole(role) {
    clearRoleSession(role);
    if (role === "hr") {
        setSelectedApplication(null);
        setSelectedInterview(null);
    }
    renderAllProfiles();
    syncJobComposer();
    syncInterviewComposer();
    showToast(`${labelForRole(role)} session cleared.`);
}

async function loadRoleData(role) {
    if (!state.sessions[role].user) {
        return;
    }

    if (role === "candidate") {
        await Promise.all([
            loadCandidateApplications(),
            loadCandidateInterviews(),
            loadCandidateNotifications()
        ]);
        return;
    }

    if (role === "hr") {
        syncJobComposer();
        syncInterviewComposer();
        await Promise.all([
            loadHrApplications(),
            loadHrInterviews(),
            loadHrStats()
        ]);
        return;
    }

    if (role === "admin") {
        await Promise.all([
            loadAdminStats(),
            loadAdminUsers(new FormData(dom.adminUserFilterForm)),
            loadAdminLogs(new FormData(dom.adminLogFilterForm))
        ]);
    }
}

async function loadPublicJobs(filterData) {
    const query = new URLSearchParams({ page: "1", size: "12" });
    const activeFilters = filterData instanceof FormData ? filterData : new FormData(dom.jobFilterForm);
    let hasExplicitFilter = false;

    for (const [key, value] of activeFilters.entries()) {
        if (value) {
            query.set(key, value);
            hasExplicitFilter = true;
        }
    }

    if (!hasExplicitFilter) {
        query.set("status", "OPEN");
    }

    const page = await api(`/api/jobs?${query.toString()}`);
    const records = page.records || [];
    const nextSelected = records.find(job => String(job.id) === String(state.selectedJob?.id)) || records[0] || null;

    dom.jobSummary.textContent = records.length
        ? `${records.length} jobs loaded. Click one to use it across the workbench.`
        : "No jobs matched the current filters.";

    setSelectedJob(nextSelected);
    renderJobList(records);
}

async function loadCandidateApplications() {
    const page = await api("/api/applications/me?page=1&size=5", { role: "candidate" });
    renderApplicationList(dom.candidateApplications, page.records || [], false);
}

async function loadCandidateInterviews() {
    const page = await api("/api/interviews/me?page=1&size=5", { role: "candidate" });
    renderInterviewList(dom.candidateInterviews, page.records || [], false);
}

async function loadCandidateNotifications() {
    const [page, unread] = await Promise.all([
        api("/api/notifications?page=1&size=5", { role: "candidate" }),
        api("/api/notifications/unread-count", { role: "candidate" })
    ]);
    dom.candidateNotificationMeta.textContent = `${unread.unreadCount} unread notifications`;
    renderNotificationList(page.records || []);
}

async function loadHrApplications() {
    if (!state.sessions.hr.user) {
        resetHrWorkspace();
        return;
    }
    if (!state.selectedJob) {
        dom.hrApplications.innerHTML = emptyState("Choose or create a job to view applications.");
        return;
    }

    const page = await api(`/api/jobs/${state.selectedJob.id}/applications?page=1&size=6`, { role: "hr" });
    const records = page.records || [];
    if (state.selectedApplicationId && !records.some(record => String(record.id) === String(state.selectedApplicationId))) {
        setSelectedApplication(null);
    }
    renderApplicationList(dom.hrApplications, records, true);
}

async function loadHrInterviews() {
    if (!state.sessions.hr.user) {
        resetHrWorkspace();
        return;
    }
    if (!state.selectedJob) {
        dom.hrInterviews.innerHTML = emptyState("Choose or create a job to view interviews.");
        return;
    }

    const page = await api(`/api/jobs/${state.selectedJob.id}/interviews?page=1&size=6`, { role: "hr" });
    const records = page.records || [];
    if (state.selectedInterview && !records.some(record => String(record.id) === String(state.selectedInterview.id))) {
        setSelectedInterview(null);
    } else if (state.selectedInterview) {
        const refreshed = records.find(record => String(record.id) === String(state.selectedInterview.id));
        if (refreshed) {
            state.selectedInterview = refreshed;
            syncInterviewComposer();
        }
    }
    renderInterviewList(dom.hrInterviews, records, true);
}

async function loadHrStats() {
    if (!state.sessions.hr.user) {
        resetHrWorkspace();
        return;
    }
    const stats = await api("/api/statistics/overview", { role: "hr" });
    renderStats(dom.hrStats, stats);
}

async function loadAdminStats() {
    if (!state.sessions.admin.user) {
        resetAdminWorkspace();
        return;
    }
    const stats = await api("/api/statistics/overview", { role: "admin" });
    renderStats(dom.adminStats, stats);
}

async function loadAdminUsers(filterData) {
    if (!state.sessions.admin.user) {
        resetAdminWorkspace();
        return;
    }
    const query = buildQuery(filterData || new FormData(dom.adminUserFilterForm), { page: "1", size: "8" });
    const page = await api(`/api/admin/users?${query.toString()}`, { role: "admin" });
    renderUserList(page.records || []);
}

async function loadAdminLogs(filterData) {
    if (!state.sessions.admin.user) {
        resetAdminWorkspace();
        return;
    }
    const query = buildQuery(filterData || new FormData(dom.adminLogFilterForm), { page: "1", size: "8" });
    const page = await api(`/api/admin/operation-logs?${query.toString()}`, { role: "admin" });
    renderLogList(page.records || []);
}

async function createJobFromComposer(button) {
    await runBusy(button, "Creating...", async () => {
        validateHrSession();
        const payload = collectJobPayload();
        const created = await api("/api/jobs", {
            role: "hr",
            method: "POST",
            body: JSON.stringify(payload)
        });
        setSelectedJob(created);
        showToast(`Created job: ${created.title}`);
        await Promise.all([
            loadPublicJobs(new FormData(dom.jobFilterForm)),
            loadHrStats(),
            maybeLoadRoleData("admin", () => loadAdminLogs(new FormData(dom.adminLogFilterForm)))
        ]);
    });
    syncJobComposer();
}

async function updateSelectedJobFromComposer(button) {
    await runBusy(button, "Saving...", async () => {
        validateHrSession();
        if (!canCurrentHrManageSelectedJob()) {
            throw new Error("Select one of your own jobs before saving changes.");
        }

        const payload = collectJobPayload();
        const updated = await api(`/api/jobs/${state.selectedJob.id}`, {
            role: "hr",
            method: "PUT",
            body: JSON.stringify(payload)
        });
        setSelectedJob(updated);
        showToast(`Updated job: ${updated.title}`);
        await Promise.all([
            loadPublicJobs(new FormData(dom.jobFilterForm)),
            loadHrStats(),
            maybeLoadRoleData("admin", () => loadAdminLogs(new FormData(dom.adminLogFilterForm)))
        ]);
    });
    syncJobComposer();
}

async function deleteSelectedJob(button) {
    await runBusy(button, "Deleting...", async () => {
        validateHrSession();
        if (!canCurrentHrManageSelectedJob()) {
            throw new Error("Select one of your own jobs before deleting it.");
        }
        if (!window.confirm(`Delete job "${state.selectedJob.title}"? This cannot be undone.`)) {
            return;
        }

        await api(`/api/jobs/${state.selectedJob.id}`, {
            role: "hr",
            method: "DELETE"
        });
        showToast(`Deleted job: ${state.selectedJob.title}`);
        setSelectedJob(null);
        await Promise.all([
            loadPublicJobs(new FormData(dom.jobFilterForm)),
            loadHrStats(),
            maybeLoadRoleData("admin", () => loadAdminLogs(new FormData(dom.adminLogFilterForm)))
        ]);
    });
    syncJobComposer();
    syncInterviewComposer();
}

async function submitInterviewForm(button) {
    await runBusy(button, state.selectedInterview ? "Updating..." : "Scheduling...", async () => {
        validateHrSession();
        const formData = new FormData(dom.interviewForm);
        const basePayload = {
            interviewAt: toDateTimePayload(formData.get("interviewAt")),
            location: normalizeText(formData.get("location")),
            meetingLink: normalizeText(formData.get("meetingLink")),
            remark: normalizeText(formData.get("remark"))
        };

        if (state.selectedInterview) {
            const payload = {
                ...basePayload,
                status: formData.get("status"),
                result: formData.get("result")
            };
            const updated = await api(`/api/interviews/${state.selectedInterview.id}`, {
                role: "hr",
                method: "PATCH",
                body: JSON.stringify(payload)
            });
            setSelectedInterview(updated);
            showToast(`Interview #${updated.id} updated.`);
        } else {
            if (!state.selectedApplicationId) {
                throw new Error("Choose an application from the HR list first.");
            }
            const created = await api("/api/interviews", {
                role: "hr",
                method: "POST",
                body: JSON.stringify({
                    applicationId: Number(state.selectedApplicationId),
                    ...basePayload
                })
            });
            setSelectedInterview(created);
            showToast(`Interview #${created.id} scheduled.`);
        }

        await Promise.all([
            loadHrApplications(),
            loadHrInterviews(),
            maybeLoadRoleData("candidate", loadCandidateInterviews),
            maybeLoadRoleData("candidate", loadCandidateNotifications),
            maybeLoadRoleData("admin", () => loadAdminLogs(new FormData(dom.adminLogFilterForm)))
        ]);
    });
    syncInterviewComposer();
}

function renderJobList(records) {
    if (!records.length) {
        dom.jobList.innerHTML = emptyState("No jobs matched the current filters.");
        return;
    }

    dom.jobList.innerHTML = records.map(job => `
        <article class="job-card ${String(job.id) === String(state.selectedJob?.id) ? "is-selected" : ""}" data-job-select="${job.id}">
            <div class="job-card-top">
                <div>
                    <h3>${escapeHtml(job.title)}</h3>
                    <div class="meta-row">
                        <span class="pill">${escapeHtml(job.status || "OPEN")}</span>
                        <span class="pill">${escapeHtml(job.city || "Unknown city")}</span>
                        <span class="pill">${escapeHtml(job.category || "General")}</span>
                    </div>
                </div>
                <button class="button button-inline" type="button">Use This Job</button>
            </div>
            <p>${escapeHtml(truncateText(job.description || "No description provided.", 180))}</p>
            <div class="meta-row">
                <span class="pill">${escapeHtml(job.employmentType || "N/A")}</span>
                <span class="pill">${escapeHtml(job.experienceLevel || "N/A")}</span>
                <span class="pill">${escapeHtml(formatSalaryRange(job))}</span>
            </div>
        </article>
    `).join("");

    dom.jobList.querySelectorAll("[data-job-select]").forEach(card => {
        card.addEventListener("click", async () => {
            const selected = records.find(item => String(item.id) === card.dataset.jobSelect);
            setSelectedJob(selected);
            renderJobList(records);
            await Promise.all([
                maybeLoadRoleData("hr", loadHrApplications),
                maybeLoadRoleData("hr", loadHrInterviews)
            ]);
            showToast(`Selected job: ${selected.title}`);
        });
    });
}

function renderSelectedJobOverview() {
    if (!state.selectedJob) {
        dom.selectedJobOverview.innerHTML = emptyState("Select a job to inspect the current working context.");
        return;
    }

    const job = state.selectedJob;
    const hrNote = !state.sessions.hr.user
        ? "Log in as HR to create, update, or delete postings."
        : canCurrentHrManageSelectedJob()
            ? "This posting belongs to the current HR account, so you can update it directly below."
            : "This posting is read-only in the HR panel because it belongs to another recruiter.";

    dom.selectedJobOverview.innerHTML = `
        <div class="record-top">
            <div>
                <p class="eyebrow">Selected Context</p>
                <h3>${escapeHtml(job.title)}</h3>
                <div class="tag-row">
                    <span class="pill">${escapeHtml(job.status || "OPEN")}</span>
                    <span class="pill">${escapeHtml(job.city || "Unknown city")}</span>
                    <span class="pill">${escapeHtml(job.category || "General")}</span>
                </div>
            </div>
            <span class="pill pill-highlight">${escapeHtml(formatSalaryRange(job))}</span>
        </div>
        <p>${escapeHtml(job.description || "No description provided.")}</p>
        <div class="detail-list">
            <div class="detail-line">
                <span>Employment</span>
                <strong>${escapeHtml(job.employmentType || "N/A")}</strong>
            </div>
            <div class="detail-line">
                <span>Experience</span>
                <strong>${escapeHtml(job.experienceLevel || "N/A")}</strong>
            </div>
            <div class="detail-line">
                <span>Application Deadline</span>
                <strong>${escapeHtml(formatDateTime(job.applicationDeadline) || "Not set")}</strong>
            </div>
            <div class="detail-line">
                <span>Last Updated</span>
                <strong>${escapeHtml(formatDateTime(job.updatedAt) || "N/A")}</strong>
            </div>
        </div>
        <div class="field-help">${escapeHtml(hrNote)}</div>
    `;
}

function renderApplicationList(container, records, hrMode) {
    if (!records.length) {
        container.innerHTML = emptyState(hrMode ? "No applications under the selected job yet." : "Nothing here yet.");
        return;
    }

    container.innerHTML = records.map(record => `
        <article class="record-item ${hrMode ? "is-interactive" : ""} ${String(record.id) === String(state.selectedApplicationId) ? "is-selected" : ""}" data-application-select="${record.id}">
            <div class="record-top">
                <div>
                    <strong>${escapeHtml(record.jobTitle || "Untitled job")}</strong>
                    <div class="tag-row">
                        <span class="pill">${escapeHtml(record.status)}</span>
                        ${record.candidateName ? `<span class="pill">${escapeHtml(record.candidateName)}</span>` : ""}
                        ${record.jobCity ? `<span class="pill">${escapeHtml(record.jobCity)}</span>` : ""}
                    </div>
                </div>
                ${hrMode ? `<button class="button button-inline" type="button">Use in Interview</button>` : ""}
            </div>
            <p>${escapeHtml(record.coverLetter || "No cover letter provided.")}</p>
            ${record.resumeFilePath ? `<p class="field-help">Resume: ${escapeHtml(record.resumeFilePath)}</p>` : ""}
            ${record.hrNote ? `<p class="field-help">HR note: ${escapeHtml(record.hrNote)}</p>` : ""}
            <p class="field-help">Applied ${escapeHtml(formatDateTime(record.appliedAt) || "N/A")}</p>
            ${hrMode ? `
                <div class="record-actions">
                    ${["REVIEWING", "INTERVIEW", "OFFERED", "REJECTED"].map(status => `
                        <button class="button-tag" type="button" data-application-status="${record.id}:${status}">${status}</button>
                    `).join("")}
                </div>
            ` : ""}
        </article>
    `).join("");

    if (!hrMode) {
        return;
    }

    container.querySelectorAll("[data-application-select]").forEach(item => {
        item.addEventListener("click", () => {
            const selected = records.find(record => String(record.id) === item.dataset.applicationSelect);
            setSelectedApplication(selected.id);
            renderApplicationList(container, records, true);
            showToast(`Selected application #${selected.id}`);
        });
    });

    container.querySelectorAll("[data-application-status]").forEach(button => {
        button.addEventListener("click", async event => {
            const [applicationId, status] = event.currentTarget.dataset.applicationStatus.split(":");
            await runBusy(event.currentTarget, "Saving...", async () => {
                await api(`/api/applications/${applicationId}/status`, {
                    role: "hr",
                    method: "PATCH",
                    body: JSON.stringify({
                        status,
                        hrNote: `Updated from frontend workbench to ${status}.`
                    })
                });
                showToast(`Application moved to ${status}.`);
                await Promise.all([
                    loadHrApplications(),
                    maybeLoadRoleData("candidate", loadCandidateApplications),
                    maybeLoadRoleData("candidate", loadCandidateNotifications),
                    maybeLoadRoleData("admin", () => loadAdminLogs(new FormData(dom.adminLogFilterForm)))
                ]);
            });
        });
    });
}

function renderInterviewList(container, records, hrMode) {
    if (!records.length) {
        container.innerHTML = emptyState(hrMode ? "No interviews scheduled for the selected job." : "No interviews available.");
        return;
    }

    container.innerHTML = records.map(record => `
        <article class="record-item ${hrMode ? "is-interactive" : ""} ${String(record.id) === String(state.selectedInterview?.id) ? "is-selected" : ""}" data-interview-select="${record.id}">
            <div class="record-top">
                <div>
                    <strong>${escapeHtml(record.jobTitle || "Interview")}</strong>
                    <div class="tag-row">
                        <span class="pill">${escapeHtml(record.status)}</span>
                        <span class="pill">${escapeHtml(record.result)}</span>
                        ${record.candidateName ? `<span class="pill">${escapeHtml(record.candidateName)}</span>` : ""}
                    </div>
                </div>
                ${hrMode ? `<button class="button button-inline" type="button">Edit Interview</button>` : ""}
            </div>
            <p>${escapeHtml(formatDateTime(record.interviewAt))} · ${escapeHtml(record.location || "Online / TBD")}</p>
            ${record.meetingLink ? `<p class="field-help">${escapeHtml(record.meetingLink)}</p>` : ""}
            <p>${escapeHtml(record.remark || "No remarks yet.")}</p>
        </article>
    `).join("");

    if (!hrMode) {
        return;
    }

    container.querySelectorAll("[data-interview-select]").forEach(item => {
        item.addEventListener("click", () => {
            const selected = records.find(record => String(record.id) === item.dataset.interviewSelect);
            setSelectedInterview(selected);
            renderInterviewList(container, records, true);
            showToast(`Loaded interview #${selected.id} into the editor.`);
        });
    });
}

function renderNotificationList(records) {
    if (!records.length) {
        dom.candidateNotifications.innerHTML = emptyState("No notifications yet.");
        return;
    }

    dom.candidateNotifications.innerHTML = records.map(record => `
        <article class="record-item">
            <div class="record-top">
                <div>
                    <strong>${escapeHtml(record.title)}</strong>
                    <div class="tag-row">
                        <span class="pill">${escapeHtml(record.type)}</span>
                        <span class="pill ${record.isRead ? "" : "pill-highlight"}">${record.isRead ? "READ" : "UNREAD"}</span>
                    </div>
                </div>
                ${record.isRead ? "" : `<button class="button button-inline" type="button" data-notification-read="${record.id}">Mark Read</button>`}
            </div>
            <p>${escapeHtml(record.content)}</p>
            <p class="field-help">${escapeHtml(formatDateTime(record.createdAt) || "N/A")}</p>
        </article>
    `).join("");

    dom.candidateNotifications.querySelectorAll("[data-notification-read]").forEach(button => {
        button.addEventListener("click", async event => {
            await runBusy(event.currentTarget, "Saving...", async () => {
                const id = event.currentTarget.dataset.notificationRead;
                await api(`/api/notifications/${id}/read`, { role: "candidate", method: "PATCH" });
                showToast("Notification marked as read.");
                await loadCandidateNotifications();
            });
        });
    });
}

function renderStats(container, stats) {
    if (!stats) {
        container.innerHTML = emptyState("No statistics loaded.");
        return;
    }

    container.innerHTML = `
        <div class="stats-row">
            <div class="stat-block">
                <span>Scope</span>
                <strong>${escapeHtml(stats.scope)}</strong>
            </div>
            <div class="stat-block">
                <span>Jobs</span>
                <strong>${escapeHtml(String(stats.jobs.total))}</strong>
            </div>
            <div class="stat-block">
                <span>Applications</span>
                <strong>${escapeHtml(String(stats.applications.total))}</strong>
            </div>
        </div>
        ${renderMapCard("Job Status", stats.jobs.byStatus)}
        ${renderMapCard("Application Status", stats.applications.byStatus)}
        ${renderMapCard("Interview Status", stats.interviews.byStatus)}
        ${renderMapCard("Interview Result", stats.interviews.byResult)}
    `;
}

function renderMapCard(title, mapData) {
    const entries = Object.entries(mapData || {});
    return `
        <article class="record-item">
            <div class="record-top">
                <strong>${escapeHtml(title)}</strong>
            </div>
            ${entries.length ? entries.map(([key, value]) => `
                <div class="stat-line">
                    <span>${escapeHtml(key)}</span>
                    <strong>${escapeHtml(String(value))}</strong>
                </div>
            `).join("") : `<div class="empty-state">No data yet.</div>`}
        </article>
    `;
}

function renderUserList(records) {
    if (!records.length) {
        dom.adminUsers.innerHTML = emptyState("No users available for the current filters.");
        return;
    }

    dom.adminUsers.innerHTML = records.map(record => `
        <article class="record-item">
            <div class="record-top">
                <div>
                    <strong>${escapeHtml(record.fullName || record.email)}</strong>
                    <div class="tag-row">
                        <span class="pill">${escapeHtml(record.status)}</span>
                        ${record.roles.map(role => `<span class="pill">${escapeHtml(role)}</span>`).join("")}
                    </div>
                </div>
            </div>
            <p>${escapeHtml(record.email)} · Last login ${escapeHtml(formatDateTime(record.lastLoginAt) || "N/A")}</p>
            <p class="field-help">Created ${escapeHtml(formatDateTime(record.createdAt) || "N/A")}</p>
            ${record.roles.includes("ADMIN") ? "" : `
                <div class="record-actions">
                    <button class="button-tag" type="button" data-user-status="${record.id}:ACTIVE" data-tone="good">Activate</button>
                    <button class="button-tag" type="button" data-user-status="${record.id}:DISABLED" data-tone="warn">Disable</button>
                </div>
            `}
        </article>
    `).join("");

    dom.adminUsers.querySelectorAll("[data-user-status]").forEach(button => {
        button.addEventListener("click", async event => {
            await runBusy(event.currentTarget, "Saving...", async () => {
                const [userId, status] = event.currentTarget.dataset.userStatus.split(":");
                await api(`/api/admin/users/${userId}/status`, {
                    role: "admin",
                    method: "PATCH",
                    body: JSON.stringify({ status })
                });
                showToast(`User status changed to ${status}.`);
                await Promise.all([
                    loadAdminUsers(new FormData(dom.adminUserFilterForm)),
                    loadAdminLogs(new FormData(dom.adminLogFilterForm))
                ]);
            });
        });
    });
}

function renderLogList(records) {
    if (!records.length) {
        dom.adminLogs.innerHTML = emptyState("No operation logs matched the current filters.");
        return;
    }

    dom.adminLogs.innerHTML = records.map(record => `
        <article class="record-item">
            <div class="record-top">
                <div>
                    <strong>${escapeHtml(record.action)}</strong>
                    <div class="tag-row">
                        <span class="pill">${escapeHtml(record.targetType)}</span>
                        <span class="pill">${escapeHtml(record.operatorName)}</span>
                        ${record.operatorRoles.map(role => `<span class="pill">${escapeHtml(role)}</span>`).join("")}
                    </div>
                </div>
                <span class="pill">#${escapeHtml(String(record.targetId))}</span>
            </div>
            <p>${escapeHtml(record.details)}</p>
            <p>${escapeHtml(formatDateTime(record.createdAt))} · ${escapeHtml(record.operatorEmail)}</p>
        </article>
    `).join("");
}

function renderAllProfiles() {
    renderProfile("candidate");
    renderProfile("hr");
    renderProfile("admin");

    if (!state.sessions.candidate.user) {
        resetCandidateWorkspace();
    }
    if (!state.sessions.hr.user) {
        resetHrWorkspace();
    }
    if (!state.sessions.admin.user) {
        resetAdminWorkspace();
    }
}

function renderProfile(role) {
    const target = role === "candidate"
        ? dom.candidateProfile
        : role === "hr"
            ? dom.hrProfile
            : dom.adminProfile;
    const session = state.sessions[role];
    const buttons = dom.roleButtons[role];

    buttons.login.disabled = Boolean(session.user);
    buttons.login.textContent = session.user ? `${labelForRole(role)} Connected` : `${labelForRole(role)} Login`;
    buttons.logout.disabled = !session.user;

    if (!session.user) {
        target.classList.add("empty-state");
        target.textContent = `${labelForRole(role)} session not connected yet.`;
        return;
    }

    target.classList.remove("empty-state");
    target.innerHTML = `
        <strong>${escapeHtml(session.user.fullName)}</strong><br>
        ${escapeHtml(session.user.email)}<br>
        Roles: ${escapeHtml(session.user.roles.join(", "))}
    `;
}

function setSelectedJob(job) {
    const nextId = job?.id ?? null;
    const previousId = state.selectedJob?.id ?? null;
    const changed = String(nextId) !== String(previousId);

    state.selectedJob = job || null;
    if (changed) {
        setSelectedApplication(null);
        setSelectedInterview(null);
    }

    const label = state.selectedJob ? `${state.selectedJob.title} (#${state.selectedJob.id})` : "No job selected";
    dom.selectedJobChip.textContent = label;
    dom.hrSelectedJob.textContent = `Current job: ${label}`;
    renderSelectedJobOverview();
    syncJobComposer();
}

function setSelectedApplication(applicationId) {
    state.selectedApplicationId = applicationId || null;
    dom.selectedApplicationChip.textContent = applicationId ? `Application #${applicationId}` : "Application: none";
    if (!applicationId) {
        state.selectedInterview = null;
        dom.selectedInterviewChip.textContent = "Interview: none";
    }
    syncInterviewComposer();
}

function setSelectedInterview(interview) {
    state.selectedInterview = interview || null;
    if (interview) {
        state.selectedApplicationId = interview.applicationId || state.selectedApplicationId;
    }
    dom.selectedInterviewChip.textContent = interview ? `Interview #${interview.id}` : "Interview: none";
    dom.selectedApplicationChip.textContent = state.selectedApplicationId
        ? `Application #${state.selectedApplicationId}`
        : "Application: none";
    syncInterviewComposer();
}

function syncJobComposer() {
    const session = state.sessions.hr.user;
    const saveButton = document.querySelector('[data-action="job-save"]');
    const createButton = document.querySelector('[data-action="job-create"]');
    const deleteButton = document.querySelector('[data-action="job-delete"]');

    if (!session) {
        dom.jobFormMode.textContent = "Create mode";
        dom.jobEditorNote.textContent = "Login as HR to create a posting or select one of your own jobs to edit it.";
        resetJobForm();
        createButton.disabled = true;
        saveButton.disabled = true;
        deleteButton.disabled = true;
        return;
    }

    createButton.disabled = false;

    if (canCurrentHrManageSelectedJob()) {
        populateJobForm(state.selectedJob);
        dom.jobFormMode.textContent = "Edit selected job";
        dom.jobEditorNote.textContent = "The selected public job belongs to the current HR account, so edits are enabled.";
        saveButton.disabled = false;
        deleteButton.disabled = false;
        return;
    }

    dom.jobFormMode.textContent = "Create mode";
    dom.jobEditorNote.textContent = state.selectedJob
        ? "The selected public job is read-only in this HR session. Use the form to create a new posting."
        : "Create a new job, then it will become the selected context automatically.";
    resetJobForm();
    saveButton.disabled = true;
    deleteButton.disabled = true;
}

function syncInterviewComposer() {
    const submitButton = dom.interviewForm.querySelector('button[type="submit"]');
    const resetButton = document.querySelector('[data-action="interview-reset"]');
    const session = state.sessions.hr.user;

    if (!session) {
        dom.interviewFormMode.textContent = "Schedule mode";
        dom.interviewEditorNote.textContent = "Login as HR to schedule or update interviews.";
        resetInterviewForm();
        submitButton.textContent = "Schedule Interview";
        submitButton.disabled = true;
        resetButton.disabled = true;
        return;
    }

    resetButton.disabled = false;

    if (state.selectedInterview) {
        populateInterviewForm(state.selectedInterview);
        dom.interviewFormMode.textContent = "Update mode";
        dom.interviewEditorNote.textContent = `Editing interview #${state.selectedInterview.id} for application #${state.selectedInterview.applicationId}.`;
        submitButton.textContent = "Update Interview";
        submitButton.disabled = false;
        return;
    }

    resetInterviewForm();
    initializeInterviewDefault();
    dom.interviewForm.querySelector('[name="status"]').value = "SCHEDULED";
    dom.interviewForm.querySelector('[name="result"]').value = "PENDING";
    dom.interviewFormMode.textContent = "Schedule mode";
    dom.interviewEditorNote.textContent = state.selectedApplicationId
        ? `Scheduling a new interview for application #${state.selectedApplicationId}.`
        : "Pick an application from the HR list to schedule a new interview.";
    submitButton.textContent = "Schedule Interview";
    submitButton.disabled = !state.selectedApplicationId;
}

function validateHrSession() {
    if (!state.sessions.hr.user) {
        throw new Error("HR login is required first.");
    }
}

function canCurrentHrManageSelectedJob() {
    if (!state.sessions.hr.user || !state.selectedJob) {
        return false;
    }
    return state.sessions.hr.user.roles.includes("ADMIN")
        || String(state.selectedJob.createdBy) === String(state.sessions.hr.user.id);
}

function collectJobPayload() {
    const formData = new FormData(dom.jobComposerForm);
    const salaryMin = toNullableNumber(formData.get("salaryMin"));
    const salaryMax = toNullableNumber(formData.get("salaryMax"));
    if (salaryMin != null && salaryMax != null && salaryMin > salaryMax) {
        throw new Error("Salary min cannot exceed salary max.");
    }

    return {
        title: normalizeRequiredText(formData.get("title"), "Title is required."),
        description: normalizeRequiredText(formData.get("description"), "Description is required."),
        city: normalizeText(formData.get("city")),
        category: normalizeText(formData.get("category")),
        employmentType: formData.get("employmentType"),
        experienceLevel: formData.get("experienceLevel"),
        salaryMin,
        salaryMax,
        status: formData.get("status"),
        applicationDeadline: toDateTimePayload(formData.get("applicationDeadline"))
    };
}

function populateJobForm(job) {
    dom.jobComposerForm.querySelector('[name="title"]').value = job.title || "";
    dom.jobComposerForm.querySelector('[name="description"]').value = job.description || "";
    dom.jobComposerForm.querySelector('[name="city"]').value = job.city || "";
    dom.jobComposerForm.querySelector('[name="category"]').value = job.category || "";
    dom.jobComposerForm.querySelector('[name="employmentType"]').value = job.employmentType || "FULL_TIME";
    dom.jobComposerForm.querySelector('[name="experienceLevel"]').value = job.experienceLevel || "JUNIOR";
    dom.jobComposerForm.querySelector('[name="status"]').value = job.status || "OPEN";
    dom.jobComposerForm.querySelector('[name="salaryMin"]').value = job.salaryMin ?? "";
    dom.jobComposerForm.querySelector('[name="salaryMax"]').value = job.salaryMax ?? "";
    dom.jobComposerForm.querySelector('[name="applicationDeadline"]').value = toDateTimeLocalValue(job.applicationDeadline);
}

function resetJobForm() {
    dom.jobComposerForm.querySelector('[name="title"]').value = DEFAULT_JOB_FORM.title;
    dom.jobComposerForm.querySelector('[name="description"]').value = DEFAULT_JOB_FORM.description;
    dom.jobComposerForm.querySelector('[name="city"]').value = DEFAULT_JOB_FORM.city;
    dom.jobComposerForm.querySelector('[name="category"]').value = DEFAULT_JOB_FORM.category;
    dom.jobComposerForm.querySelector('[name="employmentType"]').value = DEFAULT_JOB_FORM.employmentType;
    dom.jobComposerForm.querySelector('[name="experienceLevel"]').value = DEFAULT_JOB_FORM.experienceLevel;
    dom.jobComposerForm.querySelector('[name="status"]').value = DEFAULT_JOB_FORM.status;
    dom.jobComposerForm.querySelector('[name="salaryMin"]').value = DEFAULT_JOB_FORM.salaryMin;
    dom.jobComposerForm.querySelector('[name="salaryMax"]').value = DEFAULT_JOB_FORM.salaryMax;
    dom.jobComposerForm.querySelector('[name="applicationDeadline"]').value = DEFAULT_JOB_FORM.applicationDeadline;
}

function resetJobComposerSelection() {
    if (canCurrentHrManageSelectedJob()) {
        populateJobForm(state.selectedJob);
    } else {
        resetJobForm();
    }
    syncJobComposer();
}

function populateInterviewForm(interview) {
    dom.interviewForm.querySelector('[name="interviewAt"]').value = toDateTimeLocalValue(interview.interviewAt);
    dom.interviewForm.querySelector('[name="location"]').value = interview.location || "";
    dom.interviewForm.querySelector('[name="meetingLink"]').value = interview.meetingLink || "";
    dom.interviewForm.querySelector('[name="remark"]').value = interview.remark || "";
    dom.interviewForm.querySelector('[name="status"]').value = interview.status || "SCHEDULED";
    dom.interviewForm.querySelector('[name="result"]').value = interview.result || "PENDING";
}

function resetInterviewForm() {
    dom.interviewForm.querySelector('[name="location"]').value = "Meeting Room A";
    dom.interviewForm.querySelector('[name="meetingLink"]').value = "";
    dom.interviewForm.querySelector('[name="remark"]').value = "";
}

function resetInterviewComposerSelection() {
    setSelectedInterview(null);
    syncInterviewComposer();
}

function resetCandidateWorkspace() {
    dom.resumePath.textContent = DEFAULT_RESUME_TEXT;
    dom.candidateApplications.innerHTML = emptyState("Login as candidate to view your applications.");
    dom.candidateInterviews.innerHTML = emptyState("Login as candidate to view your interviews.");
    dom.candidateNotifications.innerHTML = emptyState("Login as candidate to view notifications.");
    dom.candidateNotificationMeta.textContent = "";
}

function resetHrWorkspace() {
    dom.hrApplications.innerHTML = emptyState("Login as HR to load applications.");
    dom.hrInterviews.innerHTML = emptyState("Login as HR to load interviews.");
    dom.hrStats.innerHTML = emptyState("Login as HR to load statistics.");
}

function resetAdminWorkspace() {
    dom.adminStats.innerHTML = emptyState("Login as admin to view global statistics.");
    dom.adminUsers.innerHTML = emptyState("Login as admin to manage users.");
    dom.adminLogs.innerHTML = emptyState("Login as admin to inspect operation logs.");
}

function persistSessions() {
    const payload = Object.fromEntries(
        Object.entries(state.sessions).map(([role, session]) => [role, { token: session.token }])
    );
    localStorage.setItem(STORAGE_KEY, JSON.stringify(payload));
}

function hydrateSessions() {
    try {
        const payload = JSON.parse(localStorage.getItem(STORAGE_KEY) || "{}");
        Object.keys(state.sessions).forEach(role => {
            if (payload[role]?.token) {
                state.sessions[role].token = payload[role].token;
            }
        });
    } catch (error) {
        localStorage.removeItem(STORAGE_KEY);
    }
}

function clearRoleSession(role) {
    state.sessions[role] = { token: "", user: null };
    persistSessions();
}

async function api(path, options = {}) {
    const { role, method = "GET", body } = options;
    const headers = {};
    if (body && !(body instanceof FormData)) {
        headers["Content-Type"] = "application/json";
    }
    if (role) {
        const token = state.sessions[role].token;
        if (!token) {
            throw new Error(`${labelForRole(role)} login is required first.`);
        }
        headers.Authorization = `Bearer ${token}`;
    }

    const response = await fetch(path, {
        method,
        headers,
        body
    });

    const payload = await response.json().catch(() => null);
    if (response.status === 401 && role) {
        clearRoleSession(role);
        renderAllProfiles();
        syncJobComposer();
        syncInterviewComposer();
        throw new Error(`${labelForRole(role)} session expired. Please login again.`);
    }

    if (!response.ok || !payload?.success) {
        throw new Error(payload?.message || `Request failed with status ${response.status}`);
    }
    return payload.data;
}

async function runBusy(button, busyText, work) {
    if (!button) {
        return work();
    }

    const original = button.textContent;
    button.disabled = true;
    button.textContent = busyText;
    try {
        return await work();
    } finally {
        button.disabled = false;
        button.textContent = original;
    }
}

function showToast(message, isError = false) {
    window.clearTimeout(toastTimer);
    dom.toast.textContent = message;
    dom.toast.style.background = isError ? "rgba(123, 47, 32, 0.95)" : "rgba(16, 34, 42, 0.92)";
    dom.toast.classList.add("is-visible");
    toastTimer = window.setTimeout(() => {
        dom.toast.classList.remove("is-visible");
    }, 2800);
}

function handleError(error) {
    showToast(error.message || "Something went wrong.", true);
}

function buildQuery(formData, initialValues) {
    const query = new URLSearchParams(initialValues);
    for (const [key, value] of formData.entries()) {
        if (value !== null && value !== undefined && String(value).trim() !== "") {
            query.set(key, String(value).trim());
        }
    }
    return query;
}

function formatDateTime(value) {
    if (!value) {
        return "";
    }
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) {
        return String(value);
    }
    return date.toLocaleString();
}

function formatSalaryRange(job) {
    if (job.salaryMin == null && job.salaryMax == null) {
        return "Salary TBD";
    }
    if (job.salaryMin != null && job.salaryMax != null) {
        return `${job.salaryMin} - ${job.salaryMax}`;
    }
    return job.salaryMin != null ? `From ${job.salaryMin}` : `Up to ${job.salaryMax}`;
}

function toDateTimePayload(value) {
    if (!value) {
        return null;
    }
    return value.length === 16 ? `${value}:00` : value;
}

function toDateTimeLocalValue(value) {
    if (!value) {
        return "";
    }
    return String(value).slice(0, 16);
}

function toNullableNumber(value) {
    if (value === null || value === undefined || String(value).trim() === "") {
        return null;
    }
    const parsed = Number(value);
    if (Number.isNaN(parsed)) {
        throw new Error("Please enter a valid number.");
    }
    return parsed;
}

function normalizeText(value) {
    const normalized = String(value ?? "").trim();
    return normalized || null;
}

function normalizeRequiredText(value, message) {
    const normalized = normalizeText(value);
    if (!normalized) {
        throw new Error(message);
    }
    return normalized;
}

function truncateText(value, length) {
    const raw = String(value ?? "");
    return raw.length > length ? `${raw.slice(0, length - 1)}…` : raw;
}

function labelForRole(role) {
    return role === "candidate" ? "Candidate" : role === "hr" ? "HR" : "Admin";
}

function emptyState(message) {
    return `<div class="empty-state">${escapeHtml(message)}</div>`;
}

function initializeInterviewDefault() {
    const input = dom.interviewForm.querySelector('[name="interviewAt"]');
    const tomorrow = new Date(Date.now() + 24 * 60 * 60 * 1000);
    tomorrow.setMinutes(0, 0, 0);
    input.value = `${tomorrow.getFullYear()}-${String(tomorrow.getMonth() + 1).padStart(2, "0")}-${String(tomorrow.getDate()).padStart(2, "0")}T${String(tomorrow.getHours()).padStart(2, "0")}:${String(tomorrow.getMinutes()).padStart(2, "0")}`;
}

async function maybeLoadRoleData(role, loader) {
    if (!state.sessions[role].user) {
        return null;
    }
    return loader().catch(handleError);
}

function escapeHtml(value) {
    return String(value ?? "")
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#39;");
}
