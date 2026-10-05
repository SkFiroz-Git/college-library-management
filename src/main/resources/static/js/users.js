/* Admin users page: search, create, edit, activate, delete. */

"use strict";

const userState = { page: 0, size: 10, search: "", role: "", active: "" };
let editingUserId = null;

document.addEventListener("shell:ready", function () {
    bindUserEvents();
    loadUsers();
});

/* ---------- events ---------- */

function bindUserEvents() {
    const searchInput = document.getElementById("searchInput");
    const roleFilter = document.getElementById("roleFilter");
    const statusFilter = document.getElementById("statusFilter");
    const dialog = document.getElementById("userDialog");
    const form = document.getElementById("userForm");

    let debounce;
    searchInput.addEventListener("input", function (event) {
        clearTimeout(debounce);
        debounce = setTimeout(() => {
            userState.search = event.target.value.trim();
            userState.page = 0;
            loadUsers();
        }, 300);
    });

    roleFilter.addEventListener("change", (event) => {
        userState.role = event.target.value;
        userState.page = 0;
        loadUsers();
    });

    statusFilter.addEventListener("change", (event) => {
        userState.active = event.target.value;
        userState.page = 0;
        loadUsers();
    });

    document.getElementById("prevPage").addEventListener("click", () => {
        if (userState.page > 0) { userState.page--; loadUsers(); }
    });

    document.getElementById("nextPage").addEventListener("click", () => {
        if (userState.page < userState.totalPages - 1) { userState.page++; loadUsers(); }
    });

    document.getElementById("addUserBtn").addEventListener("click", () => openUserDialog(null));
    document.getElementById("closeDialog").addEventListener("click", () => dialog.close());
    document.getElementById("cancelDialog").addEventListener("click", () => dialog.close());
    document.getElementById("userRows").addEventListener("click", onRowAction);

    dialog.addEventListener("click", (event) => {
        if (event.target === dialog) dialog.close();   // click outside = close
    });

    form.addEventListener("submit", saveUser);
    form.addEventListener("input", () => UI.clearFieldErrors(form));
}

/* ---------- load + render ---------- */

async function loadUsers() {
    const params = new URLSearchParams({ page: userState.page, size: userState.size });
    if (userState.search) params.set("search", userState.search);
    if (userState.role) params.set("role", userState.role);
    if (userState.active !== "") params.set("active", userState.active);

    try {
        const data = await API.get("/api/admin/users?" + params.toString());
        cacheUsers(data.content || []);
        renderUsers(data);
    } catch (error) {
        UI.showError(error);
    }
}

/** Remember the users currently on screen so editing is instant. */
function cacheUsers(users) {
    window.__usersCache = {};
    users.forEach((user) => { window.__usersCache[user.id] = user; });
}

function renderUsers(data) {
    const body = document.getElementById("userRows");
    const users = data.content || [];

    if (!users.length) {
        body.innerHTML = `
            <tr class="loading-row">
                <td colspan="6"><div class="empty"><strong>No users found</strong>Try a different search, or add a new account.</div></td>
            </tr>`;
    } else {
        body.innerHTML = users.map((user) => `
            <tr>
                <td>
                    <div class="cell-main">${UI.escape(user.name)}</div>
                    <div class="cell-sub">${UI.escape(user.email)}</div>
                </td>
                <td>${UI.escape(user.collegeId)}</td>
                <td>${roleBadge(user.role)}</td>
                <td>${UI.escape(user.course || "—")}${user.year ? " · Year " + UI.escape(user.year) : ""}</td>
                <td>${user.active ? UI.badge("Active", "active") : UI.badge("Inactive", "inactive")}</td>
                <td class="actions">
                    <span class="row-actions">
                        <button class="icon-btn" data-action="edit" data-id="${user.id}" title="Edit" aria-label="Edit ${UI.escape(user.name)}">
                            <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 20h9"/><path d="M16.5 3.5a2.1 2.1 0 0 1 3 3L7 19l-4 1 1-4z"/></svg>
                        </button>
                        <button class="icon-btn" data-action="toggle" data-id="${user.id}" data-active="${user.active}" title="${user.active ? "Deactivate" : "Activate"}">
                            <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M18.36 6.64a9 9 0 1 1-12.73 0"/><path d="M12 2v10"/></svg>
                        </button>
                        <button class="icon-btn icon-btn--danger" data-action="delete" data-id="${user.id}" data-name="${UI.escape(user.name)}" title="Delete">
                            <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M3 6h18"/><path d="M8 6V4h8v2"/><path d="M19 6l-1 14H6L5 6"/></svg>
                        </button>
                    </span>
                </td>
            </tr>`).join("");
    }

    document.getElementById("resultCount").textContent =
        data.totalElements + " account" + (data.totalElements === 1 ? "" : "s");
    document.getElementById("pageInfo").textContent =
        data.totalElements === 0
            ? "No results"
            : "Page " + (data.page + 1) + " of " + Math.max(data.totalPages, 1) + " · " + data.totalElements + " total";

    document.getElementById("prevPage").disabled = data.page <= 0;
    document.getElementById("nextPage").disabled = data.page >= data.totalPages - 1;

    userState.totalPages = data.totalPages;
}

function roleBadge(role) {
    if (role === "ADMIN") return UI.badge("Admin", "admin");
    if (role === "LIBRARIAN") return UI.badge("Librarian", "librarian");
    return UI.badge("Student", "student");
}

/* ---------- row actions ---------- */

async function onRowAction(event) {
    const button = event.target.closest("button[data-action]");
    if (!button) return;

    const id = Number(button.dataset.id);
    const action = button.dataset.action;

    if (action === "edit") {
        openUserDialog(id);
        return;
    }

    if (action === "toggle") {
        const activate = button.dataset.active !== "true";
        try {
            await API.patch("/api/admin/users/" + id + "/active?active=" + activate);
            UI.toast(activate ? "Account activated" : "Account deactivated", "success");
            loadUsers();
        } catch (error) {
            UI.showError(error);
        }
        return;
    }

    if (action === "delete") {
        const name = button.dataset.name;
        if (!window.confirm("Delete the account of " + name + "?\n\nAccounts with borrowing history cannot be deleted — deactivate them instead.")) return;
        try {
            await API.del("/api/admin/users/" + id);
            UI.toast("Account deleted", "success");
            loadUsers();
        } catch (error) {
            UI.showError(error);
        }
    }
}

/* ---------- create / edit dialog ---------- */

function openUserDialog(id) {
    const dialog = document.getElementById("userDialog");
    const form = document.getElementById("userForm");

    editingUserId = id;
    UI.clearFieldErrors(form);
    form.reset();
    document.getElementById("userId").value = id || "";
    document.getElementById("activeField").hidden = id === null;

    if (id === null) {
        document.getElementById("dialogTitle").textContent = "Add user";
        document.getElementById("passwordLabel").innerHTML = 'Password <span class="req">*</span>';
        document.getElementById("passwordHint").textContent = "At least 8 characters. Stored as a BCrypt hash.";
        document.getElementById("password").placeholder = "At least 8 characters";
        document.getElementById("password").required = true;
        dialog.showModal();
        return;
    }

    const cached = window.__usersCache && window.__usersCache[id];
    if (cached) {
        fillForm(cached);
        dialog.showModal();
    } else {
        // Not on this page (deep link) — ask the server.
        API.get("/api/admin/users/" + id)
            .then((user) => { fillForm(user); dialog.showModal(); })
            .catch((error) => UI.showError(error));
    }
}

function fillForm(user) {
    document.getElementById("dialogTitle").textContent = "Edit user";
    document.getElementById("userId").value = user.id;
    document.getElementById("collegeId").value = user.collegeId;
    document.getElementById("name").value = user.name;
    document.getElementById("email").value = user.email;
    document.getElementById("role").value = user.role;
    document.getElementById("course").value = user.course || "";
    document.getElementById("year").value = user.year || "";
    document.getElementById("active").value = String(user.active);

    document.getElementById("passwordLabel").innerHTML = 'New password <span class="hint">(leave empty to keep the current one)</span>';
    document.getElementById("passwordHint").textContent = "Only typed passwords are changed.";
    document.getElementById("password").placeholder = "Leave empty to keep current password";
    document.getElementById("password").required = false;
    document.getElementById("activeField").hidden = false;
}

async function saveUser(event) {
    event.preventDefault();
    const form = event.currentTarget;
    UI.clearFieldErrors(form);

    const yearValue = document.getElementById("year").value;
    const passwordValue = document.getElementById("password").value;
    const isEdit = editingUserId !== null;

    const payload = {
        collegeId: document.getElementById("collegeId").value.trim(),
        name: document.getElementById("name").value.trim(),
        email: document.getElementById("email").value.trim(),
        password: passwordValue ? passwordValue : null,
        role: document.getElementById("role").value,
        course: document.getElementById("course").value.trim() || null,
        year: yearValue ? Number(yearValue) : null
    };

    if (isEdit) {
        payload.active = document.getElementById("active").value === "true";
    }

    const button = document.getElementById("saveUserBtn");
    UI.setLoading(button, true);

    try {
        if (isEdit) {
            await API.put("/api/admin/users/" + editingUserId, payload);
            UI.toast("User updated", "success");
        } else {
            await API.post("/api/admin/users", payload);
            UI.toast("User created", "success");
        }
        document.getElementById("userDialog").close();
        loadUsers();
    } catch (error) {
        if (!UI.showFieldErrors(form, error)) UI.showError(error);
    } finally {
        UI.setLoading(button, false);
    }
}
