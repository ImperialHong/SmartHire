const DEMO_CREDENTIALS = {
    candidate: { email: "candidate@example.com", password: "password123" },
    hr: { email: "hr@example.com", password: "password123" },
    admin: { email: "admin@example.com", password: "password123" }
};

const STORAGE_KEY = "smarthire.frontend.sessions";
const state = {
    selectedJobId: null,
    selectedJobTitle: "",
    selectedApplicationId: null,
    sessions: {
        candidate: { token: "", user: null },
        hr: { token: "", user: null },
        admin: { token: "", user: null }
    }
};

const dom = {
    toast: document.querySelector("#toast"),
    jobList: document.querySelector("#job-list"),
    jobSummary: document.querySelector("#job-summary"),
    candidateProfile: document.querySelector("#candidate-profile"),
    resumePath: document.querySelector("#resume-path"),
    selectedJobChip: document.querySelector("#selected-job-chip"),
    candidateApplications: document.querySelector("#candidate-applications"),
    candidateInterviews: document.querySelector("#candidate-interviews"),
    candidateNotifications: document.querySelector("#candidate-notifications"),
    candidateNotificationMeta: document.querySelector("#candidate-notification-meta"),
    hrProfile: document.querySelector("#hr-profile"),
    hrSelectedJob: document.querySelector("#hr-selected-job"),
    selectedApplicationChip: document.querySelector("#selected-application-chip"),
    hrApplications: document.querySelector("#hr-applications"),
    hrInterviews: document.querySelector("#hr-interviews"),
    hrStats: document.querySelector("#hr-stats"),
    adminProfile: document.querySelector("#admin-profile"),
    adminStats: document.querySelector("#admin-stats"),
    adminUsers: document.querySelector("#admin-users"),
    adminLogs: document.querySelector("#admin-logs")
};

let toastTimer = null;

document.addEventListener("DOMContentLoaded", () => {
    hydrateSessions();
    bindEvents();
    renderAllProfiles();
    setSelectedJob(null, "");
    setSelectedApplication(null);
    initializeInterviewDefault();
    loadPublicJobs().catch(handleError);
    refreshSessions().catch(handleError);
});

function bindEvents() {
    document.querySelector('[data-action="load-public-jobs"]').addEventListener("click", () => {
        loadPublicJobs().catch(handleError);
    });

    document.querySelector("#job-filter-form").addEventListener("submit", event => {
        event.preventDefault();
        loadPublicJobs(new FormData(event.currentTarget)).catch(handleError);
    });

    document.querySelectorAll("[data-role-login]").forEach(button => {
        button.addEventListener("click", async event => {
            const role = event.currentTarget.dataset.roleLogin;
            await loginRole(role);
        });
    });

    document.querySelector("#resume-form").addEventListener("submit", async event => {
        event.preventDefault();
        const form = event.currentTarget;
        const fileInput = form.querySelector("#resume-file");
        if (!fileInput.files || fileInput.files.length === 0) {
            showToast("Select a PDF resume first.", true);
            return;
        }

        const body = new FormData();
        body.append("file", fileInput.files[0]);
        const data = await api("/api/resumes/upload", { role: "candidate", method: "POST", body });
        const filePath = data.filePath || "uploaded";
        dom.resumePath.textContent = filePath;
        showToast("Resume uploaded successfully.");
    });

    document.querySelector("#candidate-apply-form").addEventListener("submit", async event => {
        event.preventDefault();
        if (!state.selectedJobId) {
            showToast("Select a job from the public list first.", true);
            return;
        }

        const formData = new FormData(event.currentTarget);
        const payload = {
            jobId: Number(state.selectedJobId),
            resumeFilePath: dom.resumePath.textContent.startsWith("resumes/")
                ? dom.resumePath.textContent
                : null,
            coverLetter: formData.get("coverLetter")
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

    document.querySelector("#job-create-form").addEventListener("submit", async event => {
        event.preventDefault();
        const formData = new FormData(event.currentTarget);
        const payload = {
            title: formData.get("title"),
            description: formData.get("description"),
            city: formData.get("city"),
            category: formData.get("category"),
            employmentType: "FULL_TIME",
            experienceLevel: formData.get("experienceLevel"),
            salaryMin: 15000,
            salaryMax: 25000,
            status: "OPEN"
        };

        const data = await api("/api/jobs", {
            role: "hr",
            method: "POST",
            body: JSON.stringify(payload)
        });
        setSelectedJob(data.id, data.title);
        showToast("Job created and selected.");
        await Promise.all([loadPublicJobs(), loadHrStats()]);
    });

    document.querySelector("#interview-form").addEventListener("submit", async event => {
        event.preventDefault();
        if (!state.selectedApplicationId) {
            showToast("Choose an application from the HR list first.", true);
            return;
        }

        const formData = new FormData(event.currentTarget);
        const payload = {
            applicationId: Number(state.selectedApplicationId),
            interviewAt: formatDateTimeInput(formData.get("interviewAt")),
            location: formData.get("location"),
            meetingLink: formData.get("meetingLink"),
            remark: formData.get("remark")
        };

        await api("/api/interviews", {
            role: "hr",
            method: "POST",
            body: JSON.stringify(payload)
        });
        showToast("Interview scheduled.");
        event.currentTarget.reset();
        await Promise.all([
            loadHrApplications(),
            loadHrInterviews(),
            maybeLoadRoleData("candidate", loadCandidateNotifications),
            maybeLoadRoleData("candidate", loadCandidateInterviews),
            maybeLoadRoleData("admin", loadAdminLogs)
        ]);
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
        loadAdminUsers().catch(handleError);
    });
    document.querySelector('[data-action="load-admin-logs"]').addEventListener("click", () => {
        loadAdminLogs().catch(handleError);
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
    await Promise.all(
        roles
            .filter(role => state.sessions[role].user)
            .map(role => loadRoleData(role).catch(handleError))
    );
}

async function loginRole(role) {
    const credentials = DEMO_CREDENTIALS[role];
    if (!credentials) {
        return;
    }
    const data = await api("/api/auth/login", {
        method: "POST",
        body: JSON.stringify(credentials)
    });
    state.sessions[role] = {
        token: data.accessToken,
        user: data.user
    };
    persistSessions();
    renderProfile(role);
    showToast(`${labelForRole(role)} session connected.`);
    await loadRoleData(role);
}

async function loadRoleData(role) {
    if (role === "candidate") {
        await Promise.all([
            loadCandidateApplications(),
            loadCandidateInterviews(),
            loadCandidateNotifications()
        ]);
    }
    if (role === "hr") {
        await Promise.all([
            loadHrApplications(),
            loadHrInterviews(),
            loadHrStats()
        ]);
    }
    if (role === "admin") {
        await Promise.all([
            loadAdminStats(),
            loadAdminUsers(),
            loadAdminLogs()
        ]);
    }
}

async function loadPublicJobs(filterData) {
    const query = new URLSearchParams({ page: "1", size: "12" });
    if (filterData instanceof FormData) {
        for (const [key, value] of filterData.entries()) {
            if (value) {
                query.set(key, value);
            }
        }
    } else {
        query.set("status", "OPEN");
    }

    const page = await api(`/api/jobs?${query.toString()}`);
    const records = page.records || [];
    dom.jobSummary.textContent = `${records.length} jobs loaded. Click one to use it in the role panels.`;
    if (!state.selectedJobId && records.length > 0) {
        setSelectedJob(records[0].id, records[0].title);
    }
    renderJobList(records);
}

async function loadCandidateApplications() {
    const page = await api("/api/applications/me?page=1&size=5", { role: "candidate" });
    renderApplicationList(dom.candidateApplications, page.records || [], false);
}

async function loadCandidateInterviews() {
    const page = await api("/api/interviews/me?page=1&size=5", { role: "candidate" });
    renderInterviewList(dom.candidateInterviews, page.records || []);
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
    if (!state.selectedJobId) {
        dom.hrApplications.innerHTML = emptyState("Choose or create a job to view applications.");
        return;
    }
    const page = await api(`/api/jobs/${state.selectedJobId}/applications?page=1&size=6`, { role: "hr" });
    renderApplicationList(dom.hrApplications, page.records || [], true);
}

async function loadHrInterviews() {
    if (!state.selectedJobId) {
        dom.hrInterviews.innerHTML = emptyState("Choose or create a job to view interviews.");
        return;
    }
    const page = await api(`/api/jobs/${state.selectedJobId}/interviews?page=1&size=6`, { role: "hr" });
    renderInterviewList(dom.hrInterviews, page.records || []);
}

async function loadHrStats() {
    const stats = await api("/api/statistics/overview", { role: "hr" });
    renderStats(dom.hrStats, stats);
}

async function loadAdminStats() {
    const stats = await api("/api/statistics/overview", { role: "admin" });
    renderStats(dom.adminStats, stats);
}

async function loadAdminUsers() {
    const page = await api("/api/admin/users?page=1&size=8", { role: "admin" });
    renderUserList(page.records || []);
}

async function loadAdminLogs() {
    const page = await api("/api/admin/operation-logs?page=1&size=8", { role: "admin" });
    renderLogList(page.records || []);
}

function renderJobList(records) {
    if (!records.length) {
        dom.jobList.innerHTML = emptyState("No jobs matched the current filters.");
        return;
    }

    dom.jobList.innerHTML = records.map(job => `
        <article class="job-card ${String(job.id) === String(state.selectedJobId) ? "is-selected" : ""}" data-job-select="${job.id}">
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
            <p>${escapeHtml(job.description || "No description provided.")}</p>
        </article>
    `).join("");

    dom.jobList.querySelectorAll("[data-job-select]").forEach(card => {
        card.addEventListener("click", () => {
            const selected = records.find(item => String(item.id) === card.dataset.jobSelect);
            setSelectedJob(selected.id, selected.title);
            renderJobList(records);
            maybeLoadRoleData("hr", loadHrApplications);
            maybeLoadRoleData("hr", loadHrInterviews);
            showToast(`Selected job: ${selected.title}`);
        });
    });
}

function renderApplicationList(container, records, hrMode) {
    if (!records.length) {
        container.innerHTML = emptyState("Nothing here yet.");
        return;
    }

    container.innerHTML = records.map(record => `
        <article class="record-item" data-application-select="${record.id}">
            <div class="record-top">
                <div>
                    <strong>${escapeHtml(record.jobTitle || "Untitled job")}</strong>
                    <div class="tag-row">
                        <span class="pill">${escapeHtml(record.status)}</span>
                        ${record.candidateName ? `<span class="pill">${escapeHtml(record.candidateName)}</span>` : ""}
                    </div>
                </div>
                ${hrMode ? `<button class="button button-inline" type="button">Use in Interview</button>` : ""}
            </div>
            <p>${escapeHtml(record.coverLetter || "No cover letter provided.")}</p>
            ${hrMode ? `
                <div class="record-actions">
                    ${["REVIEWING", "INTERVIEW", "OFFERED", "REJECTED"].map(status => `
                        <button class="button-tag" type="button" data-application-status="${record.id}:${status}">${status}</button>
                    `).join("")}
                </div>
            ` : ""}
        </article>
    `).join("");

    if (hrMode) {
        container.querySelectorAll("[data-application-select]").forEach(item => {
            item.addEventListener("click", () => {
                setSelectedApplication(item.dataset.applicationSelect);
            });
        });
        container.querySelectorAll("[data-application-status]").forEach(button => {
            button.addEventListener("click", async event => {
                const [applicationId, status] = event.currentTarget.dataset.applicationStatus.split(":");
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
                    maybeLoadRoleData("admin", loadAdminLogs)
                ]);
            });
        });
    }
}

function renderInterviewList(container, records) {
    if (!records.length) {
        container.innerHTML = emptyState("No interviews available.");
        return;
    }

    container.innerHTML = records.map(record => `
        <article class="record-item">
            <div class="record-top">
                <div>
                    <strong>${escapeHtml(record.jobTitle || "Interview")}</strong>
                    <div class="tag-row">
                        <span class="pill">${escapeHtml(record.status)}</span>
                        <span class="pill">${escapeHtml(record.result)}</span>
                    </div>
                </div>
            </div>
            <p>${escapeHtml(formatDateTime(record.interviewAt))} · ${escapeHtml(record.location || "Online / TBD")}</p>
            <p>${escapeHtml(record.remark || "No remarks yet.")}</p>
        </article>
    `).join("");
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
        </article>
    `).join("");

    dom.candidateNotifications.querySelectorAll("[data-notification-read]").forEach(button => {
        button.addEventListener("click", async event => {
            const id = event.currentTarget.dataset.notificationRead;
            await api(`/api/notifications/${id}/read`, { role: "candidate", method: "PATCH" });
            showToast("Notification marked as read.");
            await loadCandidateNotifications();
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
            ${entries.map(([key, value]) => `
                <div class="stat-line">
                    <span>${escapeHtml(key)}</span>
                    <strong>${escapeHtml(String(value))}</strong>
                </div>
            `).join("")}
        </article>
    `;
}

function renderUserList(records) {
    if (!records.length) {
        dom.adminUsers.innerHTML = emptyState("No users available.");
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
            const [userId, status] = event.currentTarget.dataset.userStatus.split(":");
            await api(`/api/admin/users/${userId}/status`, {
                role: "admin",
                method: "PATCH",
                body: JSON.stringify({ status })
            });
            showToast(`User status changed to ${status}.`);
            await Promise.all([loadAdminUsers(), loadAdminLogs(), maybeLoadRoleData("candidate", loadCandidateNotifications)]);
        });
    });
}

function renderLogList(records) {
    if (!records.length) {
        dom.adminLogs.innerHTML = emptyState("No operation logs yet.");
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
                    </div>
                </div>
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
}

function renderProfile(role) {
    const target = role === "candidate"
        ? dom.candidateProfile
        : role === "hr"
            ? dom.hrProfile
            : dom.adminProfile;
    const session = state.sessions[role];
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

function setSelectedJob(jobId, title) {
    state.selectedJobId = jobId;
    state.selectedJobTitle = title || "";
    const label = jobId ? `${title} (#${jobId})` : "No job selected";
    dom.selectedJobChip.textContent = label;
    dom.hrSelectedJob.textContent = `Current job: ${label}`;
}

function setSelectedApplication(applicationId) {
    state.selectedApplicationId = applicationId;
    dom.selectedApplicationChip.textContent = applicationId ? `Application #${applicationId}` : "Application: none";
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
    renderProfile(role);
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
    if (!response.ok || !payload?.success) {
        throw new Error(payload?.message || "Request failed");
    }
    return payload.data;
}

function showToast(message, isError = false) {
    window.clearTimeout(toastTimer);
    dom.toast.textContent = message;
    dom.toast.style.background = isError ? "rgba(123, 47, 32, 0.95)" : "rgba(16, 34, 42, 0.92)";
    dom.toast.classList.add("is-visible");
    toastTimer = window.setTimeout(() => {
        dom.toast.classList.remove("is-visible");
    }, 2600);
}

function handleError(error) {
    showToast(error.message || "Something went wrong.", true);
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

function formatDateTimeInput(value) {
    if (!value) {
        return value;
    }
    return value.length === 16 ? `${value}:00` : value;
}

function labelForRole(role) {
    return role === "candidate" ? "Candidate" : role === "hr" ? "HR" : "Admin";
}

function emptyState(message) {
    return `<div class="empty-state">${escapeHtml(message)}</div>`;
}

function initializeInterviewDefault() {
    const input = document.querySelector('#interview-form input[name="interviewAt"]');
    if (!input) {
        return;
    }
    const tomorrow = new Date(Date.now() + 24 * 60 * 60 * 1000);
    tomorrow.setMinutes(0, 0, 0);
    input.value = `${tomorrow.getFullYear()}-${String(tomorrow.getMonth() + 1).padStart(2, "0")}-${String(tomorrow.getDate()).padStart(2, "0")}T${String(tomorrow.getHours()).padStart(2, "0")}:${String(tomorrow.getMinutes()).padStart(2, "0")}`;
}

async function maybeLoadRoleData(role, loader) {
    if (!state.sessions[role].token) {
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
