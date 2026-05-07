const authBox = document.getElementById("authBox");
const loginPanel = document.getElementById("loginPanel");
const dashboardPanel = document.getElementById("dashboardPanel");
const emailForm = document.getElementById("emailForm");
const formStatus = document.getElementById("formStatus");
const fileInput = emailForm?.querySelector('input[name="file"]');
const attachmentsInput = emailForm?.querySelector('input[name="attachments"]');
const templateInput = emailForm?.querySelector('textarea[name="template"]');
const subjectInput = emailForm?.querySelector('input[name="subject"]');
const scheduleInput = emailForm?.querySelector('input[name="scheduledAtLocal"]');
const placeholderChips = document.getElementById("placeholderChips");
const sheetSummary = document.getElementById("sheetSummary");
const attachmentList = document.getElementById("attachmentList");
const messageEditor = document.getElementById("messageEditor");
const fontColorPicker = document.getElementById("fontColorPicker");
const fontFamilySelect = document.getElementById("fontFamilySelect");
const sendButton = document.getElementById("sendButton");
let csrf = null;

function syncTemplateInput() {
    if (!templateInput || !messageEditor) {
        return;
    }
    templateInput.value = messageEditor.innerHTML.trim();
}

document.querySelectorAll(".format-button").forEach((button) => {
    button.addEventListener("click", () => {
        messageEditor.focus();
        const action = button.dataset.action;
        if (action === "link") {
            const url = window.prompt("Enter link URL");
            if (url) {
                document.execCommand("createLink", false, url);
            }
            return;
        }
        document.execCommand(button.dataset.command, false);
        syncTemplateInput();
    });
});

fontColorPicker?.addEventListener("input", () => {
    messageEditor.focus();
    document.execCommand("foreColor", false, fontColorPicker.value);
    syncTemplateInput();
});

fontFamilySelect?.addEventListener("change", () => {
    messageEditor.focus();
    document.execCommand("fontName", false, fontFamilySelect.value);
    syncTemplateInput();
});

messageEditor?.addEventListener("input", syncTemplateInput);
messageEditor?.addEventListener("blur", syncTemplateInput);

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

async function submitCampaign(event) {
    event.preventDefault();
    const submitButton = emailForm.querySelector("button[type='submit']");
    const activeButton = sendButton || submitButton;
    if (activeButton) {
        activeButton.disabled = true;
    }
    formStatus.classList.remove("error");
    formStatus.textContent = "Sending emails. Keep this tab open until the campaign completes.";

    try {
        syncTemplateInput();
        const formData = new FormData(emailForm);
        const scheduledValue = scheduleInput?.value;
        if (scheduledValue) {
            formData.append("scheduledAt", new Date(scheduledValue).toISOString());
        }
        const response = await fetch("/api/emails/bulk", {
            method: "POST",
            headers: { [csrf.headerName]: csrf.token },
            body: formData
        });
        const payload = await response.json().catch(() => ({}));
        if (!response.ok) {
            throw new Error(payload.message || "Campaign failed");
        }
        formStatus.textContent = scheduledValue
            ? "Scheduled successfully. The server will send this campaign at the selected time."
            : `Done. Sent ${payload.sent}, failed ${payload.failed}.`;
        emailForm.reset();
        emailForm.delayMs.value = "1500";
        messageEditor.innerHTML = "<p>Hi {{name}},</p><p>We loved what {{company}} is building.</p><p>I wanted to share a quick note with you.</p><p>Best regards,<br>Chaitu</p>";
        syncTemplateInput();
        resetSheetHints();
        attachmentList.innerHTML = "";
        await loadDashboard();
    } catch (error) {
        formStatus.classList.add("error");
        formStatus.textContent = error.message;
    } finally {
        if (activeButton) {
            activeButton.disabled = false;
        }
    }
}

emailForm?.addEventListener("submit", submitCampaign);
sendButton?.addEventListener("click", (event) => {
    submitCampaign(event);
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
    sheetSummary.textContent = "Upload a recipient sheet with at least an email column to unlock field suggestions.";
}

function insertToken(token) {
    if (document.activeElement === subjectInput) {
        const start = subjectInput.selectionStart ?? subjectInput.value.length;
        const end = subjectInput.selectionEnd ?? subjectInput.value.length;
        subjectInput.setRangeText(token, start, end, "end");
        subjectInput.focus();
        return;
    }
    messageEditor.focus();
    document.execCommand("insertText", false, token);
    syncTemplateInput();
}

function renderCampaign(campaign) {
    const status = (campaign.status || "COMPLETED").toLowerCase();
    const scheduleText = campaign.scheduledAt ? `Scheduled for ${formatDate(campaign.scheduledAt)}` : `Created ${formatDate(campaign.createdAt)}`;
    return `<div class="item">
        <div class="item-head">
            <strong>${escapeHtml(campaign.subject)}</strong>
            <span class="status-badge ${escapeHtml(status)}">${escapeHtml(status)}</span>
        </div>
        <small>${scheduleText}</small>
        <small>${campaign.sentCount} sent, ${campaign.failedCount} failed</small>
    </div>`;
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
syncTemplateInput();
