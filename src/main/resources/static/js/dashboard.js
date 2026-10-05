/* Admin dashboard: one API call, several cards. */

"use strict";

document.addEventListener("shell:ready", async function (event) {
    const me = event.detail;

    try {
        const stats = await API.get("/api/admin/dashboard/stats");
        renderStats(stats);
        renderRecent(stats.recentIssues || []);
        renderQuickActions(me);
    } catch (error) {
        UI.showError(error);
    }
});

function renderStats(stats) {
    const cards = [
        { label: "Members", value: stats.totalUsers, hint: stats.students + " students · " + stats.librarians + " librarians", tone: "" },
        { label: "Active accounts", value: stats.activeUsers, hint: stats.admins + " admin" + (stats.admins === 1 ? "" : "s"), tone: "teal" },
        { label: "Books", value: stats.totalBooks, hint: stats.activeBooks + " active in catalogue", tone: "" },
        { label: "Copies available", value: stats.availableCopies + " / " + stats.totalCopies, hint: "on the shelf right now", tone: "teal" },
        { label: "Books out", value: stats.issuedNow, hint: "currently borrowed", tone: "amber" },
        { label: "Overdue", value: stats.overdue, hint: stats.returnedAllTime + " returns all time", tone: "red" }
    ];

    document.getElementById("stats").innerHTML = cards.map((card) => `
        <article class="stat ${card.tone ? "stat--" + card.tone : ""}">
            <div class="stat__label">${UI.escape(card.label)}</div>
            <div class="stat__value">${UI.escape(card.value)}</div>
            <div class="stat__hint">${UI.escape(card.hint)}</div>
        </article>`).join("");
}

function renderRecent(issues) {
    const body = document.getElementById("recentBody");

    if (!issues.length) {
        body.innerHTML = `<tr class="loading-row"><td colspan="5">No books have been issued yet.</td></tr>`;
        return;
    }

    body.innerHTML = issues.map((issue) => `
        <tr>
            <td>
                <div class="cell-main">${UI.escape(issue.bookTitle)}</div>
                <div class="cell-sub">${UI.escape(issue.bookIsbn)}</div>
            </td>
            <td>
                <div class="cell-main">${UI.escape(issue.userName)}</div>
                <div class="cell-sub">${UI.escape(issue.userCollegeId)}</div>
            </td>
            <td>${UI.formatDateTime(issue.issuedAt)}</td>
            <td>${UI.formatDate(issue.dueDate)}</td>
            <td>${issueBadge(issue)}</td>
        </tr>`).join("");
}

function issueBadge(issue) {
    if (issue.overdue) return UI.badge("Overdue", "overdue");
    if (issue.status === "RETURNED") return UI.badge("Returned", "returned");
    return UI.badge("Issued", "issued");
}

function renderQuickActions(me) {
    const actions = [
        { label: "Issue or return a book", href: "/admin/issues.html", roles: ["ADMIN", "LIBRARIAN"] },
        { label: "Add a book to the catalogue", href: "/admin/books.html", roles: ["ADMIN", "LIBRARIAN"] },
        { label: "Manage user accounts", href: "/admin/users.html", roles: ["ADMIN"] }
    ].filter((action) => action.roles.includes(me.role));

    document.getElementById("quickActions").innerHTML = actions.map((action) => `
        <a class="btn btn--ghost btn--block" style="margin-bottom:10px; justify-content:flex-start" href="${action.href}">
            <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M9 18l6-6-6-6"/></svg>
            ${UI.escape(action.label)}
        </a>`).join("");
}
