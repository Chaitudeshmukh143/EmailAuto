const authBox = document.getElementById("authBox");
const loginPanel = document.getElementById("loginPanel");
const dashboardPanel = document.getElementById("dashboardPanel");
const emailForm = document.getElementById("emailForm");
const formStatus = document.getElementById("formStatus");
const fileInput = emailForm?.querySelector('input[name="file"]');
const attachmentsInput = emailForm?.querySelector('input[name="attachments"]');
const templateInput = emailForm?.querySelector('textarea[name="template"]');
const subjectInput = emailForm?.querySelector('input[name="subject"]');
const placeholderChips = document.getElementById("placeholderChips");
const sheetSummary = document.getElementById("sheetSummary");
const attachmentList = document.getElementById("attachmentList");
let csrf = null;

async function loadCsrf() {
    const response = await fetch("/api/csrf");
    csrf = await response.json();
}

async function loadMe() {
    const response = await fetch("/api/me");
    const me = await response.json();
    if (!me.authenticated) {
        authBox.innerHTML = '<a class="primary-button" href="/auth/google">Login with Google</a>';
        loginPanel.classList.remove("hidden");
        dashboardPanel.classList.add("hidden");
        return;
    }
    authBox.innerHTML = `<span>${escapeHtml(me.email)}</span><a href="/auth/logout">Logout</a>`;
    loginPanel.classList.add("hidden");
    dashboardPanel.classList.remove("hidden");
    await loadDashboard();
}

async function loadDashboard() {
    const response = await fetch("/api/dashboard");
    if (!response.ok) return;
    const dashboard = await response.json();
    document.getElementById("sentCount").textContent = dashboard.totalSent;
    document.getElementById("failedCount").textContent = dashboard.totalFailed;
    document.getElementById("campaigns").innerHTML = dashboard.recentCampaigns.length
        ? dashboard.recentCampaigns.map(renderCampaign).join("")
        : '<div class="item"><small>No campaigns yet.</small></div>';
    document.getElementById("failures").innerHTML = dashboard.recentFailures.length
        ? dashboard.recentFailures.map(renderFailure).join("")
        : '<div class="item"><small>No failures yet.</small></div>';
}

fileInput?.addEventListener("change", async () => {
    if (!fileInput.files?.length) {
        resetSheetHints();
        return;
    }

    const body = new FormData();
    body.append("file", fileInput.files[0]);
    try {
        const response = await fetch("/api/emails/inspect", {
            method: "POST",
            headers: { [csrf.headerName]: csrf.token },
            body
        });
        const payload = await response.json().catch(() => ({}));
        if (!response.ok) {
            throw new Error(payload.message || "Could not read the Excel file");
        }
        renderSheetHints(payload);
    } catch (error) {
        formStatus.classList.add("error");
        formStatus.textContent = error.message;
        resetSheetHints();
    }
});

attachmentsInput?.addEventListener("change", () => {
    if (!attachmentsInput.files?.length) {
        attachmentList.innerHTML = "";
        return;
    }
    attachmentList.innerHTML = Array.from(attachmentsInput.files)
        .map((file) => `<span class="attachment-pill">${escapeHtml(file.name)}</span>`)
        .join("");
});

emailForm?.addEventListener("submit", async (event) => {
    event.preventDefault();
    const submitButton = emailForm.querySelector("button[type='submit']");
    submitButton.disabled = true;
    formStatus.classList.remove("error");
    formStatus.textContent = "Sending emails. Keep this tab open until the campaign completes.";

    try {
        const response = await fetch("/api/emails/bulk", {
            method: "POST",
            headers: { [csrf.headerName]: csrf.token },
            body: new FormData(emailForm)
        });
        const payload = await response.json().catch(() => ({}));
        if (!response.ok) {
            throw new Error(payload.message || "Campaign failed");
        }
        formStatus.textContent = `Done. Sent ${payload.sent}, failed ${payload.failed}.`;
        emailForm.reset();
        emailForm.delayMs.value = "1500";
        resetSheetHints();
        attachmentList.innerHTML = "";
        await loadDashboard();
    } catch (error) {
        formStatus.classList.add("error");
        formStatus.textContent = error.message;
    } finally {
        submitButton.disabled = false;
    }
});

function renderSheetHints(metadata) {
    const placeholders = metadata.placeholders?.length ? metadata.placeholders : ["{{name}}", "{{email}}", "{{company}}"];
    placeholderChips.innerHTML = placeholders
        .map((token) => `<button type="button" class="chip" data-token="${escapeHtml(token)}">${escapeHtml(token)}</button>`)
        .join("");
    placeholderChips.querySelectorAll("button").forEach((button) => {
        button.addEventListener("click", () => insertToken(button.dataset.token));
    });
    sheetSummary.textContent = `${metadata.rowCount} recipients detected. Click a field to insert it into the subject or message.`;
}

function resetSheetHints() {
    placeholderChips.innerHTML = '<button type="button" class="chip muted">Upload a sheet first</button>';
    sheetSummary.textContent = "Upload a recipient sheet to unlock field suggestions.";
}

function insertToken(token) {
    const target = document.activeElement === subjectInput ? subjectInput : templateInput;
    const start = target.selectionStart ?? target.value.length;
    const end = target.selectionEnd ?? target.value.length;
    target.setRangeText(token, start, end, "end");
    target.focus();
}

function renderCampaign(campaign) {
    return `<div class="item"><strong>${escapeHtml(campaign.subject)}</strong><small>${campaign.sentCount} sent, ${campaign.failedCount} failed - ${formatDate(campaign.createdAt)}</small></div>`;
}

function renderFailure(failure) {
    return `<div class="item"><strong>${escapeHtml(failure.recipientEmail)}</strong><small>${escapeHtml(failure.errorMessage || "Unknown error")} - ${formatDate(failure.createdAt)}</small></div>`;
}

function formatDate(value) {
    return new Date(value).toLocaleString();
}

function escapeHtml(value) {
    return String(value ?? "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}

loadCsrf().then(loadMe);
