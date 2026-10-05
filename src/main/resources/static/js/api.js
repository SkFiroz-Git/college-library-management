/* =========================================================
   College Library — API helper (shared by every page)

   Why this file exists:
   - one place that knows how to call the backend
   - one place that attaches the CSRF token
   - one place that handles "session expired" (401)
   Frontend never decides permissions — it only calls the API
   and displays what the server answers.
   ========================================================= */

"use strict";

function readCookie(name) {
    const match = document.cookie.match(new RegExp("(?:^|; )" + name.replace(/[.*+?^${}()|[\]\\]/g, "\\$&") + "=([^;]*)"));
    return match ? decodeURIComponent(match[1]) : null;
}

const API = {

    async request(url, options = {}) {
        const method = (options.method || "GET").toUpperCase();
        const headers = new Headers(options.headers || {});

        if (options.body && !headers.has("Content-Type")) {
            headers.set("Content-Type", "application/json");
        }

        // CSRF: the server sets the XSRF-TOKEN cookie, we echo it back
        // in this header for every state-changing request.
        if (method !== "GET" && method !== "HEAD") {
            const token = readCookie("XSRF-TOKEN");
            if (token) headers.set("X-XSRF-TOKEN", token);
        }

        const response = await fetch(url, {
            ...options,
            method,
            headers,
            credentials: "same-origin"
        });

        if (response.status === 401) {
            window.location.href = "/index.html";
            throw new ApiError(401, "Your session has expired");
        }

        const text = await response.text();
        let data = null;
        if (text) {
            try { data = JSON.parse(text); } catch (err) { data = { message: text }; }
        }

        if (!response.ok) {
            throw new ApiError(
                response.status,
                (data && data.message) || "Request failed",
                data && data.fields
            );
        }
        return data;
    },

    get(url) { return this.request(url); },

    post(url, body) {
        return this.request(url, { method: "POST", body: body ? JSON.stringify(body) : null });
    },

    put(url, body) {
        return this.request(url, { method: "PUT", body: JSON.stringify(body) });
    },

    patch(url) { return this.request(url, { method: "PATCH" }); },

    del(url) { return this.request(url, { method: "DELETE" }); },

    /** Ask the server for the CSRF cookie — call this once on the login page. */
    ensureCsrf() { return this.get("/api/auth/csrf").catch(() => null); }
};

class ApiError extends Error {
    constructor(status, message, fields) {
        super(message);
        this.name = "ApiError";
        this.status = status;
        this.fields = fields || null;
    }
}

/* ---------- small UI utilities shared by all pages ---------- */

const UI = {

    escape(value) {
        if (value === null || value === undefined) return "";
        return String(value)
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#39;");
    },

    /** Short, readable date: 05 Oct 2026 */
    formatDate(value) {
        if (!value) return "—";
        const date = typeof value === "string" ? new Date(value.length === 10 ? value + "T00:00:00" : value) : value;
        if (Number.isNaN(date.getTime())) return "—";
        return date.toLocaleDateString("en-GB", { day: "2-digit", month: "short", year: "numeric" });
    },

    /** 05 Oct 2026, 14:32 */
    formatDateTime(value) {
        if (!value) return "—";
        const date = new Date(value);
        if (Number.isNaN(date.getTime())) return "—";
        return date.toLocaleDateString("en-GB", { day: "2-digit", month: "short", year: "numeric" })
            + ", " + date.toLocaleTimeString("en-GB", { hour: "2-digit", minute: "2-digit" });
    },

    /** 29.999 -> "30.00" (used for book prices) */
    money(value) {
        if (value === null || value === undefined || value === "") return "—";
        const number = Number(value);
        return Number.isNaN(number) ? "—" : number.toFixed(2);
    },

    badge(text, type) {
        return `<span class="badge badge--${UI.escape(type || "neutral")}">${UI.escape(text)}</span>`;
    },

    /** Show API validation errors under the matching form fields. */
    showFieldErrors(form, error) {
        UI.clearFieldErrors(form);
        if (!error.fields) return false;
        let shown = false;
        Object.entries(error.fields).forEach(([name, message]) => {
            const input = form.querySelector("[name='" + name + "']");
            if (!input) return;
            const field = input.closest(".field");
            if (!field) return;
            field.classList.add("is-invalid");
            const box = field.querySelector(".field__error");
            if (box) { box.textContent = message; box.hidden = false; }
            shown = true;
        });
        return shown;
    },

    clearFieldErrors(form) {
        form.querySelectorAll(".field.is-invalid").forEach((field) => {
            field.classList.remove("is-invalid");
            const box = field.querySelector(".field__error");
            if (box) { box.textContent = ""; box.hidden = true; }
        });
    },

    toast(message, type) {
        const old = document.querySelector(".toast");
        if (old) old.remove();

        const toast = document.createElement("div");
        toast.className = "toast toast--" + (type || "success");
        toast.setAttribute("role", "status");
        toast.textContent = message;
        document.body.appendChild(toast);

        requestAnimationFrame(() => toast.classList.add("toast--show"));
        setTimeout(() => {
            toast.classList.remove("toast--show");
            setTimeout(() => toast.remove(), 250);
        }, 3500);
    },

    /** Convert an ApiError into a user-friendly toast. */
    showError(error) {
        if (error instanceof ApiError && error.fields) {
            UI.toast(Object.values(error.fields)[0] || "Please correct the form", "error");
        } else {
            UI.toast(error.message || "Something went wrong", "error");
        }
    },

    setLoading(button, loading) {
        button.classList.toggle("is-loading", loading);
        button.disabled = loading;
    }
};
