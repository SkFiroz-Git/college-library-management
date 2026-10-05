/* =========================================================
   College Library — Login page behaviour
   Now talks to the real Spring Boot API:
     GET  /api/auth/csrf   (get the CSRF cookie)
     POST /api/auth/login  (create the session)
   ========================================================= */

"use strict";

document.addEventListener("DOMContentLoaded", function () {

    const form = document.getElementById("loginForm");
    const usernameInput = document.getElementById("username");
    const passwordInput = document.getElementById("password");
    const toggleBtn = document.getElementById("togglePassword");
    const rememberMe = document.getElementById("rememberMe");
    const signInBtn = document.getElementById("signInBtn");
    const formMessage = document.getElementById("formMessage");
    const forgotLink = document.getElementById("forgotPassword");
    const yearSlot = document.getElementById("currentYear");
    const capsHint = document.getElementById("capsHint");

    /* Ask the server for the CSRF cookie before any POST can happen. */
    API.ensureCsrf();

    /* ---------- helpers ---------- */

    function setError(fieldId, message) {
        const input = document.getElementById(fieldId);
        const field = input.closest(".field");
        const error = document.getElementById(fieldId + "Error");

        if (message) {
            field.classList.add("is-invalid");
            error.textContent = message;
            error.hidden = false;
            input.setAttribute("aria-invalid", "true");
        } else {
            field.classList.remove("is-invalid");
            error.textContent = "";
            error.hidden = true;
            input.removeAttribute("aria-invalid");
        }
    }

    function showMessage(text, type) {
        if (!text) {
            formMessage.hidden = true;
            formMessage.textContent = "";
            formMessage.className = "message";
            return;
        }
        formMessage.textContent = text;
        formMessage.className = "message message--" + type;
        formMessage.hidden = false;
    }

    function setLoading(isLoading) {
        signInBtn.classList.toggle("is-loading", isLoading);
        signInBtn.disabled = isLoading;
        signInBtn.querySelector(".btn__label").textContent = isLoading ? "Signing in..." : "Sign in";
    }

    /* ---------- 1. Show / hide password ---------- */

    toggleBtn.addEventListener("click", function () {
        const isRevealed = passwordInput.type === "text";

        if (isRevealed) {
            passwordInput.type = "password";
            toggleBtn.classList.remove("is-revealed");
            toggleBtn.setAttribute("aria-label", "Show password");
            toggleBtn.setAttribute("aria-pressed", "false");
        } else {
            passwordInput.type = "text";
            toggleBtn.classList.add("is-revealed");
            toggleBtn.setAttribute("aria-label", "Hide password");
            toggleBtn.setAttribute("aria-pressed", "true");
        }

        passwordInput.focus();
    });

    /* ---------- 1b. Caps Lock warning ---------- */

    function checkCapsLock(event) {
        if (typeof event.getModifierState !== "function") return;
        capsHint.hidden = !event.getModifierState("CapsLock");
    }

    passwordInput.addEventListener("keyup", checkCapsLock);
    passwordInput.addEventListener("keydown", checkCapsLock);
    passwordInput.addEventListener("blur", function () { capsHint.hidden = true; });

    /* ---------- 2. Remember me (stores only the ID, never the password) ---------- */

    const REMEMBER_KEY = "collegeLibrary.username";
    let restoredUsername = false;

    try {
        const saved = localStorage.getItem(REMEMBER_KEY);
        if (saved) {
            usernameInput.value = saved;
            rememberMe.checked = true;
            passwordInput.focus();
            restoredUsername = true;
        }
    } catch (err) { /* storage blocked by browser settings */ }

    if (!restoredUsername && window.matchMedia("(min-width: 901px)").matches) {
        usernameInput.focus();
    }

    /* ---------- 3. Validation ---------- */

    function validateUsername() {
        const value = usernameInput.value.trim();
        if (value === "") { setError("username", "Please enter your College ID or email."); return false; }
        if (value.length < 4) { setError("username", "This ID looks too short."); return false; }
        setError("username", null);
        return true;
    }

    function validatePassword() {
        const value = passwordInput.value;
        if (value === "") { setError("password", "Please enter your password."); return false; }
        if (value.length < 6) { setError("password", "Password must be at least 6 characters."); return false; }
        setError("password", null);
        return true;
    }

    usernameInput.addEventListener("blur", validateUsername);
    passwordInput.addEventListener("blur", validatePassword);

    usernameInput.addEventListener("input", function () {
        if (usernameInput.closest(".field").classList.contains("is-invalid")) validateUsername();
    });
    passwordInput.addEventListener("input", function () {
        if (passwordInput.closest(".field").classList.contains("is-invalid")) validatePassword();
    });

    /* ---------- 4. Real login ---------- */

    form.addEventListener("submit", async function (event) {
        event.preventDefault();
        showMessage("");

        const usernameOk = validateUsername();
        const passwordOk = validatePassword();
        if (!usernameOk || !passwordOk) {
            showMessage("Please fix the highlighted fields.", "error");
            (usernameOk ? passwordInput : usernameInput).focus();
            return;
        }

        /* save / clear the remembered ID */
        try {
            if (rememberMe.checked) {
                localStorage.setItem(REMEMBER_KEY, usernameInput.value.trim());
            } else {
                localStorage.removeItem(REMEMBER_KEY);
            }
        } catch (err) { /* ignore */ }

        setLoading(true);
        try {
            const user = await API.post("/api/auth/login", {
                identifier: usernameInput.value.trim(),
                password: passwordInput.value
            });

            if (user.role === "STUDENT") {
                /* Students get their own portal in a later phase. */
                showMessage("Signed in. Your student portal is being prepared — please check back soon.", "info");
                setLoading(false);
                return;
            }

            showMessage("Welcome back, " + user.name + ". Redirecting…", "success");
            window.setTimeout(function () {
                window.location.href = "/admin/dashboard.html";
            }, 400);

        } catch (error) {
            setLoading(false);
            if (error.fields) UI.showFieldErrors(form, error);
            showMessage(error.message || "Unable to sign in right now.", "error");
        }
    });

    /* ---------- 5. Forgot password (handled by the library staff) ---------- */

    forgotLink.addEventListener("click", function (event) {
        event.preventDefault();
        showMessage(
            "Password resets are handled by the library administrator. Please contact the helpdesk.",
            "info"
        );
    });

    /* ---------- 6. Current year in the footer ---------- */

    if (yearSlot) yearSlot.textContent = String(new Date().getFullYear());
});
