/* =========================================================
   College Library — Admin shell
   Every /admin/*.html page loads this file. It:
     1. checks who is logged in (GET /api/auth/me)
     2. blocks the page if the role is not allowed
     3. draws the sidebar + topbar
     4. fires "shell:ready" so the page can load its data
   ========================================================= */

"use strict";

document.addEventListener("DOMContentLoaded", async function () {

    const allowedRoles = (document.body.dataset.roles || "ADMIN,LIBRARIAN")
        .split(",")
        .map((role) => role.trim());

    let me = null;
    try {
        me = await API.get("/api/auth/me");   // 401 -> API redirects to login
    } catch (error) {
        return;
    }

    if (!allowedRoles.includes(me.role)) {
        renderBlocked();
        return;
    }

    renderShell(me);
    document.dispatchEvent(new CustomEvent("shell:ready", { detail: me }));
});

function renderShell(me) {
    const currentPage = document.body.dataset.page || "";
    const isAdmin = me.role === "ADMIN";

    const initials = (me.name || me.email || "?")
        .split(/\s+/)
        .slice(0, 2)
        .map((part) => part.charAt(0).toUpperCase())
        .join("");

    const links = [
        { id: "dashboard", label: "Dashboard", href: "/admin/dashboard.html", icon: "M3 13h8V3H3v10zm0 8h8v-6H3v6zm10 0h8V11h-8v10zm0-18v6h8V3h-8z", roles: ["ADMIN", "LIBRARIAN"] },
        { id: "users", label: "Users", href: "/admin/users.html", icon: "M16 11c1.66 0 3-1.34 3-3s-1.34-3-3-3-3 1.34-3 3 1.34 3 3 3zm-8 0c1.66 0 3-1.34 3-3S9.66 5 8 5 5 6.34 5 8s1.34 3 3 3zm0 2c-2.33 0-7 1.17-7 3.5V19h14v-2.5c0-2.33-4.67-3.5-7-3.5zm8 0c-.29 0-.62.02-.97.05 1.16.84 1.97 1.97 1.97 3.45V19h6v-2.5c0-2.33-4.67-3.5-7-3.5z", roles: ["ADMIN"] },
        { id: "books", label: "Books", href: "/admin/books.html", icon: "M18 2H6c-1.1 0-2 .9-2 2v16c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zM6 4h5v8l-2.5-1.5L6 12V4z", roles: ["ADMIN", "LIBRARIAN"] },
        { id: "issues", label: "Issue & Return", href: "/admin/issues.html", icon: "M19 3h-4.18C14.4 1.84 13.3 1 12 1c-1.3 0-2.4.84-2.82 2H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm-7 0c.55 0 1 .45 1 1s-.45 1-1 1-1-.45-1-1 .45-1 1-1zm2 14H7v-2h7v2zm3-4H7v-2h10v2zm0-4H7V7h10v2z", roles: ["ADMIN", "LIBRARIAN"] }
    ];

    const navHtml = links
        .filter((link) => link.roles.includes(me.role))
        .map((link) => `
            <a class="nav__link${link.id === currentPage ? " is-active" : ""}" href="${link.href}">
                <svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor" aria-hidden="true"><path d="${link.icon}"/></svg>
                <span>${link.label}</span>
            </a>`)
        .join("");

    const sidebar = `
        <div class="nav-overlay" id="navOverlay"></div>
        <aside class="sidebar" id="sidebar">
            <a class="sidebar__brand" href="/admin/dashboard.html">
                <span class="sidebar__logo" aria-hidden="true">
                    <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round">
                        <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"/>
                        <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"/>
                    </svg>
                </span>
                <span class="sidebar__name">College Library</span>
            </a>

            <nav class="sidebar__nav" aria-label="Main navigation">
                <p class="nav__caption">Management</p>
                ${navHtml}
            </nav>

            <div class="sidebar__user">
                <span class="avatar">${UI.escape(initials)}</span>
                <span class="sidebar__user-info">
                    <strong>${UI.escape(me.name)}</strong>
                    <small>${UI.escape(me.role)}</small>
                </span>
            </div>
        </aside>`;

    const topbar = `
        <header class="topbar">
            <button class="icon-btn topbar__menu" id="navToggle" aria-label="Open menu">
                <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M3 6h18M3 12h18M3 18h18"/></svg>
            </button>
            <div class="topbar__titles">
                <h1>${UI.escape(document.title.split("|")[0].trim())}</h1>
                <p>${UI.escape(document.body.dataset.subtitle || "College Library Management System")}</p>
            </div>
            <div class="topbar__actions">
                <span class="chip">${UI.escape(me.role)}</span>
                <button class="btn btn--ghost" id="logoutBtn">
                    <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><path d="M16 17l5-5-5-5"/><path d="M21 12H9"/></svg>
                    Sign out
                </button>
            </div>
        </header>`;

    document.body.insertAdjacentHTML("afterbegin", sidebar + topbar);

    /* mobile menu */
    const sidebarEl = document.getElementById("sidebar");
    const overlay = document.getElementById("navOverlay");
    document.getElementById("navToggle").addEventListener("click", () => {
        sidebarEl.classList.add("sidebar--open");
        overlay.classList.add("nav-overlay--show");
    });
    overlay.addEventListener("click", () => {
        sidebarEl.classList.remove("sidebar--open");
        overlay.classList.remove("nav-overlay--show");
    });

    /* logout */
    document.getElementById("logoutBtn").addEventListener("click", async (event) => {
        const button = event.currentTarget;
        button.disabled = true;
        try {
            await API.post("/api/auth/logout");
        } catch (error) { /* session may already be gone */ }
        window.location.href = "/index.html";
    });
}

function renderBlocked() {
    document.body.innerHTML = `
        <main class="blocked">
            <div class="blocked__card">
                <span class="blocked__icon">!</span>
                <h1>Access denied</h1>
                <p>Your account does not have permission to open the administration area.</p>
                <a class="btn btn--primary" href="/index.html">Back to sign in</a>
            </div>
        </main>`;
}
