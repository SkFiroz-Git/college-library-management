/* Admin books page: catalogue CRUD + search. */

"use strict";

/* Default view: only books that are actually in the catalogue. */
const bookState = { page: 0, size: 10, search: "", active: "true" };
let editingBookId = null;
let booksCache = {};

document.addEventListener("shell:ready", function () {
    bindBookEvents();
    loadBooks();
});

function bindBookEvents() {
    const dialog = document.getElementById("bookDialog");
    const form = document.getElementById("bookForm");

    let debounce;
    document.getElementById("searchInput").addEventListener("input", (event) => {
        clearTimeout(debounce);
        debounce = setTimeout(() => {
            bookState.search = event.target.value.trim();
            bookState.page = 0;
            loadBooks();
        }, 300);
    });

    document.getElementById("statusFilter").addEventListener("change", (event) => {
        bookState.active = event.target.value;
        bookState.page = 0;
        loadBooks();
    });

    document.getElementById("prevPage").addEventListener("click", () => {
        if (bookState.page > 0) { bookState.page--; loadBooks(); }
    });

    document.getElementById("nextPage").addEventListener("click", () => {
        if (bookState.page < bookState.totalPages - 1) { bookState.page++; loadBooks(); }
    });

    document.getElementById("addBookBtn").addEventListener("click", () => openBookDialog(null));
    document.getElementById("closeDialog").addEventListener("click", () => dialog.close());
    document.getElementById("cancelDialog").addEventListener("click", () => dialog.close());
    document.getElementById("bookRows").addEventListener("click", onBookRowAction);

    dialog.addEventListener("click", (event) => {
        if (event.target === dialog) dialog.close();
    });

    form.addEventListener("submit", saveBook);
    form.addEventListener("input", () => UI.clearFieldErrors(form));
}

/* ---------- load + render ---------- */

async function loadBooks() {
    const params = new URLSearchParams({ page: bookState.page, size: bookState.size });
    if (bookState.search) params.set("search", bookState.search);
    if (bookState.active !== "") params.set("active", bookState.active);

    try {
        const data = await API.get("/api/admin/books?" + params.toString());
        booksCache = {};
        (data.content || []).forEach((book) => { booksCache[book.id] = book; });
        renderBooks(data);
    } catch (error) {
        UI.showError(error);
    }
}

function renderBooks(data) {
    const body = document.getElementById("bookRows");
    const books = data.content || [];

    if (!books.length) {
        body.innerHTML = `
            <tr class="loading-row">
                <td colspan="9"><div class="empty"><strong>No books found</strong>Add the first book to your catalogue.</div></td>
            </tr>`;
    } else {
        body.innerHTML = books.map((book) => `
            <tr>
                <td>
                    <div class="cell-main">${UI.escape(book.title)}</div>
                    <div class="cell-sub">${UI.escape(book.author)}${book.publisher ? " · " + UI.escape(book.publisher) : ""}</div>
                </td>
                <td>${UI.escape(book.isbn)}</td>
                <td>${UI.escape(book.category || "—")}</td>
                <td>${book.publicationYear || "—"}</td>
                <td>${book.price != null ? UI.money(book.price) : "—"}</td>
                <td>${book.rating != null ? `<span class="badge badge--active">★ ${Number(book.rating).toFixed(1)}</span>` : "—"}</td>
                <td>
                    <span class="badge ${book.availableCopies > 0 ? "badge--active" : "badge--overdue"}">${book.availableCopies} / ${book.totalCopies}</span>
                </td>
                <td>${book.active ? UI.badge("In catalogue", "active") : UI.badge("Archived", "inactive")}</td>
                <td class="actions">
                    <span class="row-actions">
                        <button class="icon-btn" data-action="edit" data-id="${book.id}" title="Edit">
                            <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 20h9"/><path d="M16.5 3.5a2.1 2.1 0 0 1 3 3L7 19l-4 1 1-4z"/></svg>
                        </button>
                        <button class="icon-btn" data-action="toggle" data-id="${book.id}" data-active="${book.active}" title="${book.active ? "Archive" : "Restore"}">
                            <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 8v13H3V8"/><path d="M1 3h22v5H1z"/><path d="M10 12h4"/></svg>
                        </button>
                        <button class="icon-btn icon-btn--danger" data-action="delete" data-id="${book.id}" data-title="${UI.escape(book.title)}" title="Delete">
                            <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M3 6h18"/><path d="M8 6V4h8v2"/><path d="M19 6l-1 14H6L5 6"/></svg>
                        </button>
                    </span>
                </td>
            </tr>`).join("");
    }

    document.getElementById("resultCount").textContent =
        data.totalElements + " title" + (data.totalElements === 1 ? "" : "s");
    document.getElementById("pageInfo").textContent =
        data.totalElements === 0
            ? "No results"
            : "Page " + (data.page + 1) + " of " + Math.max(data.totalPages, 1) + " · " + data.totalElements + " total";

    document.getElementById("prevPage").disabled = data.page <= 0;
    document.getElementById("nextPage").disabled = data.page >= data.totalPages - 1;

    bookState.totalPages = data.totalPages;
}

/* ---------- row actions ---------- */

async function onBookRowAction(event) {
    const button = event.target.closest("button[data-action]");
    if (!button) return;

    const id = Number(button.dataset.id);
    const action = button.dataset.action;

    if (action === "edit") {
        const cached = booksCache[id];
        if (cached) openBookDialog(cached);
        else API.get("/api/admin/books/" + id).then(openBookDialog).catch((error) => UI.showError(error));
        return;
    }

    if (action === "toggle") {
        const activate = button.dataset.active !== "true";
        try {
            await API.patch("/api/admin/books/" + id + "/active?active=" + activate);
            UI.toast(activate ? "Book restored to the catalogue" : "Book archived", "success");
            loadBooks();
        } catch (error) {
            UI.showError(error);
        }
        return;
    }

    if (action === "delete") {
        const title = button.dataset.title;
        if (!window.confirm('Delete "' + title + '"?\n\nBooks with borrowing history must be archived instead.')) return;
        try {
            await API.del("/api/admin/books/" + id);
            UI.toast("Book deleted", "success");
            loadBooks();
        } catch (error) {
            UI.showError(error);
        }
    }
}

/* ---------- create / edit dialog ---------- */

function openBookDialog(book) {
    const dialog = document.getElementById("bookDialog");
    const form = document.getElementById("bookForm");

    editingBookId = book ? book.id : null;
    UI.clearFieldErrors(form);
    form.reset();

    if (!book) {
        document.getElementById("dialogTitle").textContent = "Add book";
        document.getElementById("totalCopies").value = "1";
        document.getElementById("active").value = "true";
    } else {
        document.getElementById("dialogTitle").textContent = "Edit book";
        document.getElementById("title").value = book.title;
        document.getElementById("isbn").value = book.isbn;
        document.getElementById("author").value = book.author;
        document.getElementById("publisher").value = book.publisher || "";
        document.getElementById("category").value = book.category || "";
        document.getElementById("publicationYear").value = book.publicationYear ?? "";
        document.getElementById("price").value = book.price ?? "";
        document.getElementById("rating").value = book.rating ?? "";
        document.getElementById("totalCopies").value = book.totalCopies;
        document.getElementById("active").value = String(book.active);
    }

    dialog.showModal();
}

async function saveBook(event) {
    event.preventDefault();
    const form = event.currentTarget;
    UI.clearFieldErrors(form);

    const payload = {
        title: document.getElementById("title").value.trim(),
        isbn: document.getElementById("isbn").value.trim(),
        author: document.getElementById("author").value.trim(),
        publisher: document.getElementById("publisher").value.trim() || null,
        category: document.getElementById("category").value.trim() || null,
        publicationYear: document.getElementById("publicationYear").value
            ? Number(document.getElementById("publicationYear").value) : null,
        price: document.getElementById("price").value
            ? Number(document.getElementById("price").value) : null,
        rating: document.getElementById("rating").value
            ? Number(document.getElementById("rating").value) : null,
        totalCopies: Number(document.getElementById("totalCopies").value),
        active: document.getElementById("active").value === "true"
    };

    const button = document.getElementById("saveBookBtn");
    UI.setLoading(button, true);

    try {
        if (editingBookId) {
            await API.put("/api/admin/books/" + editingBookId, payload);
            UI.toast("Book updated", "success");
        } else {
            await API.post("/api/admin/books", payload);
            UI.toast("Book added to the catalogue", "success");
        }
        document.getElementById("bookDialog").close();
        loadBooks();
    } catch (error) {
        if (!UI.showFieldErrors(form, error)) UI.showError(error);
    } finally {
        UI.setLoading(button, false);
    }
}
