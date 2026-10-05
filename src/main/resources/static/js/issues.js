/* Issue & return page: lending records + return action. */

"use strict";

const issueState = { page: 0, size: 10, search: "", status: "" };

document.addEventListener("shell:ready", async function () {
    bindIssueEvents();
    loadIssues();
    loadStats();
});

function bindIssueEvents() {
    const dialog = document.getElementById("issueDialog");

    let debounce;
    document.getElementById("searchInput").addEventListener("input", (event) => {
        clearTimeout(debounce);
        debounce = setTimeout(() => {
            issueState.search = event.target.value.trim();
            issueState.page = 0;
            loadIssues();
        }, 300);
    });

    document.getElementById("statusFilter").addEventListener("change", (event) => {
        issueState.status = event.target.value;
        issueState.page = 0;
        loadIssues();
    });

    document.getElementById("prevPage").addEventListener("click", () => {
        if (issueState.page > 0) { issueState.page--; loadIssues(); }
    });

    document.getElementById("nextPage").addEventListener("click", () => {
        if (issueState.page < issueState.totalPages - 1) { issueState.page++; loadIssues(); }
    });

    document.getElementById("issueBtn").addEventListener("click", openIssueDialog);
    document.getElementById("closeDialog").addEventListener("click", () => dialog.close());
    document.getElementById("cancelDialog").addEventListener("click", () => dialog.close());
    document.getElementById("issueRows").addEventListener("click", onIssueRowAction);
    document.getElementById("issueForm").addEventListener("submit", confirmIssue);

    dialog.addEventListener("click", (event) => {
        if (event.target === dialog) dialog.close();
    });
}

/* ---------- stats strip ---------- */

async function loadStats() {
    try {
        const stats = await API.get("/api/admin/dashboard/stats");
        document.getElementById("issueStats").innerHTML = `
            <article class="stat">
                <div class="stat__label">Currently out</div>
                <div class="stat__value">${stats.issuedNow}</div>
                <div class="stat__hint">active loans</div>
            </article>
            <article class="stat stat--red">
                <div class="stat__label">Overdue</div>
                <div class="stat__value">${stats.overdue}</div>
                <div class="stat__hint">past the due date</div>
            </article>
            <article class="stat stat--teal">
                <div class="stat__label">Returned</div>
                <div class="stat__value">${stats.returnedAllTime}</div>
                <div class="stat__hint">all time</div>
            </article>
            <article class="stat stat--amber">
                <div class="stat__label">Copies free</div>
                <div class="stat__value">${stats.availableCopies}</div>
                <div class="stat__hint">of ${stats.totalCopies} copies</div>
            </article>`;
    } catch (error) {
        UI.showError(error);
    }
}

/* ---------- list ---------- */

async function loadIssues() {
    const params = new URLSearchParams({ page: issueState.page, size: issueState.size });
    if (issueState.search) params.set("search", issueState.search);
    if (issueState.status && issueState.status !== "OVERDUE") params.set("status", issueState.status);

    const url = issueState.status === "OVERDUE"
        ? "/api/admin/issues/overdue?page=" + issueState.page + "&size=" + issueState.size
        : "/api/admin/issues?" + params.toString();

    try {
        const data = await API.get(url);
        renderIssues(data);
    } catch (error) {
        UI.showError(error);
    }
}

function renderIssues(data) {
    const body = document.getElementById("issueRows");
    const records = data.content || [];

    if (!records.length) {
        body.innerHTML = `
            <tr class="loading-row">
                <td colspan="6"><div class="empty"><strong>No records found</strong>Issue a book to create the first record.</div></td>
            </tr>`;
    } else {
        body.innerHTML = records.map((record) => {
            const overdue = record.overdue;
            const status = overdue
                ? UI.badge("Overdue", "overdue")
                : record.status === "RETURNED"
                    ? UI.badge("Returned", "returned")
                    : UI.badge("Issued", "issued");

            const action = record.status === "ISSUED"
                ? `<button class="btn btn--ghost btn--sm" data-action="return" data-id="${record.id}" data-title="${UI.escape(record.bookTitle)}">
                        <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M9 14L4 9l5-5"/><path d="M4 9h10a6 6 0 0 1 0 12h-3"/></svg>
                        Mark returned
                    </button>`
                : `<span class="cell-sub">${UI.formatDateTime(record.returnedAt)}</span>`;

            return `
                <tr>
                    <td>
                        <div class="cell-main">${UI.escape(record.bookTitle)}</div>
                        <div class="cell-sub">${UI.escape(record.bookIsbn)}</div>
                    </td>
                    <td>
                        <div class="cell-main">${UI.escape(record.userName)}</div>
                        <div class="cell-sub">${UI.escape(record.userCollegeId)}</div>
                    </td>
                    <td>${UI.formatDateTime(record.issuedAt)}</td>
                    <td${overdue ? ' style="color:#dc2626;font-weight:600"' : ""}>${UI.formatDate(record.dueDate)}</td>
                    <td>${status}</td>
                    <td class="actions">${action}</td>
                </tr>`;
        }).join("");
    }

    document.getElementById("resultCount").textContent =
        data.totalElements + " record" + (data.totalElements === 1 ? "" : "s");
    document.getElementById("pageInfo").textContent =
        data.totalElements === 0
            ? "No results"
            : "Page " + (data.page + 1) + " of " + Math.max(data.totalPages, 1) + " · " + data.totalElements + " total";

    document.getElementById("prevPage").disabled = data.page <= 0;
    document.getElementById("nextPage").disabled = data.page >= data.totalPages - 1;

    issueState.totalPages = data.totalPages;
}

/* ---------- return action ---------- */

async function onIssueRowAction(event) {
    const button = event.target.closest("button[data-action='return']");
    if (!button) return;

    const id = Number(button.dataset.id);
    const title = button.dataset.title;

    if (!window.confirm('Mark "' + title + '" as returned?')) return;

    try {
        await API.post("/api/admin/issues/" + id + "/return");
        UI.toast("Book returned — copy added back to the shelf", "success");
        loadIssues();
        loadStats();
    } catch (error) {
        UI.showError(error);
    }
}

/* ---------- issue dialog ---------- */

async function openIssueDialog() {
    const dialog = document.getElementById("issueDialog");
    const form = document.getElementById("issueForm");
    UI.clearFieldErrors(form);
    form.reset();
    dialog.showModal();

    try {
        const [books, users] = await Promise.all([
            API.get("/api/admin/books?size=100&active=true"),
            API.get("/api/admin/users?size=100&active=true")
        ]);

        const bookSelect = document.getElementById("bookSelect");
        bookSelect.innerHTML = `<option value="">Select a book…</option>` +
            books.content
                .filter((book) => book.availableCopies > 0)
                .map((book) => `<option value="${book.id}">${UI.escape(book.title)} — ${UI.escape(book.isbn)} (${book.availableCopies} of ${book.totalCopies} free)</option>`)
                .join("") || `<option value="">No book has a free copy</option>`;

        const userSelect = document.getElementById("userSelect");
        userSelect.innerHTML = `<option value="">Select a member…</option>` +
            users.content
                .map((user) => `<option value="${user.id}">${UI.escape(user.name)} — ${UI.escape(user.collegeId)} (${UI.escape(user.role)})</option>`)
                .join("");

    } catch (error) {
        UI.showError(error);
    }
}

async function confirmIssue(event) {
    event.preventDefault();
    const form = event.currentTarget;
    UI.clearFieldErrors(form);

    const bookId = document.getElementById("bookSelect").value;
    const userId = document.getElementById("userSelect").value;

    if (!bookId) { showFieldError("bookSelect", "Please select a book"); return; }
    if (!userId) { showFieldError("userSelect", "Please select a member"); return; }

    const button = document.getElementById("confirmIssueBtn");
    UI.setLoading(button, true);

    try {
        const record = await API.post("/api/admin/issues", {
            bookId: Number(bookId),
            userId: Number(userId)
        });
        document.getElementById("issueDialog").close();
        UI.toast("Issued until " + UI.formatDate(record.dueDate), "success");
        loadIssues();
        loadStats();
    } catch (error) {
        if (!UI.showFieldErrors(form, error)) UI.showError(error);
    } finally {
        UI.setLoading(button, false);
    }
}

function showFieldError(fieldId, message) {
    const field = document.getElementById(fieldId).closest(".field");
    field.classList.add("is-invalid");
    const box = field.querySelector(".field__error");
    box.textContent = message;
    box.hidden = false;
}
