# How This System Works

> **A complete explanation of this project: what was built, why it was built that way,
> and how a request travels from the browser to MySQL and back.**
>
> Read this file top to bottom once — you will understand the whole website.
> Every class, file and rule mentioned here really exists in this repository.

---

## 0. How to read this file

| If you want to… | Go to |
|---|---|
| Understand the whole system in 2 minutes | [1. The big picture](#1-the-big-picture) |
| Know why each technology was chosen | [2. Tech stack](#2-tech-stack-and-why) |
| Find where a piece of code lives | [3. Folder tour](#3-folder-tour) |
| Follow a real request end-to-end | [4. Anatomy of a request](#4-anatomy-of-a-request) |
| Understand login, roles and CSRF | [5. Security](#5-security-explained) |
| Understand the tables | [6. Database design](#6-database-design) |
| Understand the frontend | [7. Frontend architecture](#7-frontend-architecture) |
| Know what every HTTP status means | [8. Error handling](#8-error-handling-contract) |
| Understand startup and demo data | [9. Startup sequence](#9-startup-sequence--seeding) |
| See every "why" in one table | [12. Decision log](#12-decision-log-every-why-in-one-place) |
| Fix a problem | [13. Known bugs](#13-bugs-we-hit-and-how-they-were-solved) |
| Add something new | [14. Recipe](#14-recipe-how-to-add-a-new-feature) |

---

## 1. The big picture

The website is **one Java program** (Spring Boot). It serves the HTML/CSS/JS files
*and* the API *and* talks to MySQL. The browser never touches the database.

```
┌────────────────────────────── BROWSER ──────────────────────────────┐
│  index.html  (login)          admin/*.html  (dashboard, users, …)   │
│  js/app.js    (login logic)   js/admin.js   (sidebar + role guard)  │
│                              js/api.js    (fetch + CSRF + errors)   │
└───────────────┬──────────────────────────────────────┬──────────────┘
                │  HTTPS requests (JSON)               │  static files
                ▼                                      │
┌──────────────────────────── SPRING BOOT ─────────────┴──────────────┐
│  1. FILTER CHAIN        session check · CSRF check · role check     │
│  2. CONTROLLER          parses HTTP, validates input, returns JSON  │
│  3. SERVICE             business rules (the real logic)             │
│  4. REPOSITORY          talks to the database (JPA / Hibernate)     │
└───────────────────────────────┬─────────────────────────────────────┘
                                ▼
                    ┌──────────────────────┐
                    │  MySQL  college_library
                    │  users · books · book_issues
                    └──────────────────────┘
```

**Rule of the architecture:** each layer only talks to the layer directly below it.
The browser never knows SQL, and SQL never knows about HTML.

---

## 2. Tech stack and why

| Layer | Technology | Why this choice |
|---|---|---|
| Language | **Java 25** | Your college language; LTS version, modern syntax (`record`, `var`, text blocks) |
| Framework | **Spring Boot 4** | Industry standard in Java; gives security, validation, database and HTTP for free |
| Security | **Spring Security 7** | Battle-tested login, roles, CSRF protection — never hand-roll these |
| Database | **MySQL 8** | Required by the assignment; reliable and well documented |
| ORM | **Spring Data JPA / Hibernate** | Write Java classes, get tables and queries automatically — no SQL boilerplate |
| Validation | **Jakarta Bean Validation** | `@NotBlank`, `@Min`… validate in one place (the DTO), not scattered in code |
| Frontend | **HTML + CSS + vanilla JavaScript** | No build step (npm/webpack) → open the file and it works; you learn real HTTP |
| Build | **Maven** | One command builds, tests and runs the project anywhere |
| Passwords | **BCrypt** | Salted and one-way hashed; even you cannot read a user's password back |

**Why no React/Vue?** A library app has simple screens. A framework would add
complexity without benefit, and this way you understand exactly what happens
on every request.

**Why not JWT?** JWT is for *different servers/apps* trusting each other.
A session cookie is simpler, revocable instantly on logout, and is the standard
for server-rendered admin panels.

---

## 3. Folder tour

```
college-library/
└── src/main/
    ├── java/com/collegelibrary/
    │   ├── config/DataSeeder.java        ← runs at startup: creates admin + demo data
    │   ├── controller/                   ← LAYER 1: HTTP endpoints (thin!)
    │   │   AuthController                /api/auth/**
    │   │   AdminUserController           /api/admin/users/**
    │   │   AdminBookController           /api/admin/books/**
    │   │   AdminIssueController          /api/admin/issues/**
    │   │   AdminDashboardController      /api/admin/dashboard/stats
    │   ├── service/                      ← LAYER 2: business rules live here
    │   │   AuthService  UserService  BookService  IssueService  DashboardService
    │   ├── repository/                   ← LAYER 3: database access (SQL)
    │   ├── entity/                       ← the tables: User, Book, BookIssue
    │   ├── dto/                          ← request/response shapes (JSON contracts)
    │   ├── enums/                        ← Role (ADMIN/LIBRARIAN/STUDENT), IssueStatus
    │   ├── security/                     ← login, roles, CSRF configuration
    │   │   SecurityConfig                the "rule book" of the whole app
    │   │   AppUserDetailsService         loads a user for Spring Security
    │   │   AppPrincipal                  who is logged in (id, name, role)
    │   └── exception/                    ← converts exceptions to JSON errors
    └── resources/
        ├── application.properties        ← configuration (secrets are external)
        └── static/                       ← the entire frontend
            index.html  css/style.css  js/app.js        ← public login page
            admin/*.html  css/admin.css  js/admin.js    ← admin shell
            js/api.js                                  ← shared API helper
            js/{dashboard,users,books,issues}.js        ← one script per page
```

**The one sentence to remember:** *Controller receives → Service decides → Repository stores.*

---

## 4. Anatomy of a request

### 4.1 Logging in

```
User types admin@college.edu / Admin@123 in index.html
   │
   │  js/app.js  →  API.post("/api/auth/login", {identifier, password})
   │                + header X-XSRF-TOKEN (read from the XSRF-TOKEN cookie)
   ▼
POST /api/auth/login
   │
   ├─ FILTER CHAIN
   │    • CSRF filter: is the X-XSRF-TOKEN header equal to the cookie?  → else 403
   │    • Authorization: /api/auth/login is permitAll → continue
   ▼
AuthController.login()
   │   @Valid LoginRequest → bean validation (empty fields → 400 with field errors)
   ▼
AuthService.login()
   │
   ├─ AuthenticationManager.authenticate(identifier, password)
   │      ├─ AppUserDetailsService.loadUserByUsername()  → SELECT … FROM users
   │      │      (works with email OR college ID, case-insensitive)
   │      └─ BCryptPasswordEncoder.matches(raw, hash) → true/false
   │
   ├─ success → SecurityContext saved into the HTTP session
   │            → browser receives Set-Cookie: JSESSIONID=…
   │            → returns {"id":1,"name":…,"role":"ADMIN"}
   └─ failure → UnauthorizedException → 401
                {"message":"Invalid college ID/email or password."}
```

Then `js/app.js` redirects:

* role `ADMIN` / `LIBRARIAN` → `/admin/dashboard.html`
* role `STUDENT` → shown *"student portal is being prepared"* (next phase)

### 4.2 Opening the Books page

```
GET /admin/books.html
   ├─ filter: requires role ADMIN or LIBRARIAN  → otherwise 302 → /index.html
   ├─ Spring returns the static file
   ▼
books.js loads → waits for "shell:ready"
   │
   │  admin.js first ran:  GET /api/auth/me  → who am I?
   │  · 401 → API redirects to login
   │  · role not allowed → "Access blocked" screen
   │  · ok → draws sidebar + topbar → dispatches the event
   ▼
loadBooks()  →  GET /api/admin/books?page=0&size=10&active=true
   ▼
AdminBookController.list() → BookService.list()
   ├─ builds a Specification (dynamic WHERE: search + category + active)
   ├─ bookRepository.findAll(spec, page, sort)  → SQL with LIMIT
   └─ maps entities → BookResponse JSON
   ▼
books.js renders rows, badges and pagination. 300 ms debounce on the search box.
```

### 4.3 Issuing a book (the core business logic)

`IssueService.issue()` — all inside **one transaction**, so either everything
happens or nothing does:

1. Book must exist, else **404**
2. Book must not be archived → else **400** *"This book is archived…"*
3. Student account must be active → else **400**
4. `availableCopies > 0` → else **400** *"No copies available"*
5. Create `BookIssue` with `issuedAt = now`, `dueDate = today + 14 days`
6. `book.availableCopies - 1`
7. Save → commit

Returning a book does the reverse: mark `RETURNED`, set `returnedAt`, and
`availableCopies + 1` (never above `totalCopies`).

Because step 6 and the insert share a transaction, two librarians clicking at the
same millisecond can never make stock go negative.

---

## 5. Security explained

### 5.1 Three roles

| Capability | ADMIN | LIBRARIAN | STUDENT |
|---|:--:|:--:|:--:|
| Dashboard, Books, Issue/Return | ✅ | ✅ | ❌ |
| Manage users (create/delete) | ✅ | ❌ | ❌ |
| Own portal | ❌ | ❌ | 🔜 (next phase) |

Enforced **twice**:
* `SecurityConfig` (server side, authoritative) — `/api/admin/users/**` → `hasRole("ADMIN")`
* `admin.js` (client side, cosmetic) — hides links the role cannot use

If someone skips the UI and calls the API directly, the server still answers **401/403**.

### 5.2 Session (who are you)

* Successful login creates an HTTP session → cookie **`JSESSIONID`**
* The server stores the logged-in user in that session
* Logout clears the context and invalidates the session → next call = 401
* Any API call with 401 makes `js/api.js` redirect to `/index.html`

### 5.3 CSRF (double-submit cookie)

Attack being prevented: you are logged in and visit a malicious site that makes
*your* browser POST "delete book" to our server.

```
GET /api/auth/csrf  →  Set-Cookie: XSRF-TOKEN=3c30b728-…   (readable by JS)
every POST/PUT/PATCH/DELETE  →  header X-XSRF-TOKEN: <same value>
server: cookie value must equal header value  →  otherwise 403
```

A malicious site can make your browser send cookies, but it **cannot read** our
cookie (different origin), so it cannot copy the token into the header.

> Note: Spring Security 7 defaults to an *XOR-encoded* token handler. This app
> deliberately uses the plain `CsrfTokenRequestAttributeHandler` because our
> JavaScript echoes the raw cookie value. See [13](#13-bugs-we-hit-and-how-they-were-solved).

### 5.4 Passwords

* Stored as BCrypt hashes (`$2a$…`) — never plain text
* Login error is always the same for "unknown user" and "wrong password",
  so an attacker cannot guess valid email addresses
* Admin password comes from configuration (`app.seed.admin-password`), demo
  students use `Student@123` (dev only — change in production)

---

## 6. Database design

### users
| Column | Meaning |
|---|---|
| `id` | internal key |
| `college_id` | **login username** for students (`S1006`), unique |
| `name`, `email` | unique email, used for login too |
| `password_hash` | BCrypt |
| `role` | `ADMIN` / `LIBRARIAN` / `STUDENT` |
| `course`, `academic_year` | e.g. `Data Science`, `3` (`academic_year` because `year` is a MySQL keyword) |
| `active` | `false` = deactivated but history preserved |
| `created_at` / `updated_at` | audit timestamps |

### books
| Column | Meaning |
|---|---|
| `isbn` | unique book code (`B501`) |
| `title`, `author`, `publisher`, `category` | catalogue data |
| `publication_year`, `price`, `rating` | optional extras (nullable) |
| `total_copies` | how many the library owns |
| `available_copies` | how many are on the shelf **right now** |
| `active` | `false` = archived (hidden, not deletable-if-borrowed) |

### book_issues (the bridge table)
| Column | Meaning |
|---|---|
| `book_id` → books.id | which book (FK) |
| `user_id` → users.id | who borrowed (FK) |
| `issued_at`, `due_date`, `returned_at` | loan timeline |
| `status` | `ISSUED` or `RETURNED` |
| `overdue` | computed at read time: `status=ISSUED and due_date < today` |

```
users 1 ──── ∞ book_issues ∞ ──── 1 books
```

**Why store `available_copies` instead of counting?** Counting on every page load
is slow; updating a single number in the same transaction as the loan is fast and
safe. The number can never drift because issue/return always run atomically.

**Why `active` flags instead of deleting?** A student who borrowed books in March
must still appear in April's history. Delete is allowed only when there is no
history (the API returns **409** otherwise).

---

## 7. Frontend architecture

```
index.html + js/app.js          → public login page
admin/*.html + js/admin.js      → shared shell: auth guard, sidebar, topbar
js/api.js                       → fetch wrapper: CSRF, JSON parsing, 401 handling,
                                  UI helpers (escape, badges, field errors, toast)
js/{page}.js                    → one script per page, only page logic
```

**The load sequence of every admin page:**

```js
// 1. page loads, page script only registers a listener (top level)
document.addEventListener("shell:ready", async () => { loadBooks(); });

// 2. admin.js runs first: checks identity and builds the shell
const me = await API.get("/api/auth/me");   // 401 → redirect to login
if (!allowedRoles.includes(me.role)) return renderBlocked();
renderShell(me);
document.dispatchEvent(new CustomEvent("shell:ready", { detail: me }));

// 3. now the page script fetches its data
```

**Why an event?** All pages share the same guard, sidebar and "who am I" check.
Page code never has to repeat it, and no page can render before the role check.

**Validation errors are shown automatically**: the server answers
`400 {"fields":{"title":"Title is required"}}` and `UI.showFieldErrors(form, error)`
prints the message under the matching input (`name="title"`).

---

## 8. Error handling contract

Every error from every endpoint has the same JSON shape:

```json
{ "status": 400, "message": "Please correct the highlighted fields",
  "fields": { "email": "Please enter a valid email address" },
  "timestamp": "2026-10-05T16:14:25Z" }
```

| Status | Meaning | Example |
|---|---|---|
| `200` | OK | read/update |
| `201` | Created | POST a book or user |
| `204` | Deleted | DELETE |
| `400` | Bad input / business rule refused | empty title, no copies, negative price |
| `401` | Not signed in / wrong password | session expired → frontend redirects |
| `403` | Signed in but not allowed **or** CSRF failed | student calling `/api/admin/users` |
| `404` | Row does not exist | `/api/admin/books/999` |
| `409` | Conflict with existing data | duplicate email, delete a book with history |
| `500` | Unexpected server error (logged, generic message) | bug |

`GlobalExceptionHandler` converts Java exceptions into that shape — services never
return HTTP codes themselves, they just `throw`.

---

## 9. Startup sequence & seeding

```
mvn spring-boot:run
   ├─ 1. read application.properties (+ optional application-local.properties)
   ├─ 2. connect to MySQL, Hibernate syncs tables (ddl-auto=update adds new columns)
   ├─ 3. build the security filter chain
   └─ 4. DataSeeder.run():
         a. no ADMIN exists?  → create admin@college.edu
         b. app.seed.demo-data = true?
            · each sample book whose ISBN is missing → insert
            · each sample student whose college_id is missing → insert
```

Seeding is **idempotent** — restart the app ten times and you still have exactly
5 books and 5 students, because every insert is preceded by an existence check.

---

## 10. Configuration & secrets

| Key | Purpose |
|---|---|
| `spring.datasource.*` | DB URL/user/password — overridable with `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` |
| `spring.config.import` | loads `application-local.properties` (gitignored — real password lives there) |
| `spring.jpa.hibernate.ddl-auto=update` | create/alter tables from entities *(dev)* |
| `spring.jpa.open-in-view=false` | no lazy loading during rendering (catches performance bugs early) |
| `app.seed.*` | first admin account + `app.seed.demo-data` switch |
| `app.loan.days` | loan length, default **14** if not set |

**Golden rule:** nothing secret is ever committed. `application-local.properties`
is in `.gitignore`; the committed file only contains empty defaults plus comments.

---

## 11. Business rules (the "laws" of the system)

1. A book with **zero available copies** cannot be issued.
2. An **archived book** or **deactivated member** cannot be issued.
3. Issue date = now; **due date = today + 14 days**.
4. Return increments stock but never beyond `totalCopies`.
5. A user/book with borrowing history **cannot be deleted** → deactivate/archive (409).
6. ISBN and email must be unique (409 on duplicates).
7. Passwords must be 8–72 characters; rating 0–5; price ≥ 0; copies ≥ 1.
8. Only ADMIN can manage users; ADMIN + LIBRARIAN can manage books and issues.
9. All state-changing calls need a valid CSRF token (403 without it).

---

## 12. Decision log (every "why" in one place)

| Decision | Why | Alternative rejected |
|---|---|---|
| Session login (JSESSIONID) | instant logout, standard for admin panels | JWT (revoking is hard) |
| CSRF cookie + header | works with plain JS, no server-side forms | token in hidden form field |
| Plain CSRF handler | our JS echoes the raw cookie | Spring 7 XOR default (broke every POST) |
| Validation on DTOs | the API boundary is the only entry point | validating inside services |
| Business rules in Services | reusable by any controller; testable | logic inside controllers |
| Repository layer (JPA) | SQL isolated in one package | raw JDBC everywhere |
| `Specification` for search | dynamic `WHERE` built safely (no string concat) | hand-written SQL per filter |
| `active` flags, no hard delete | history stays intact | cascade delete (destroys audit trail) |
| Store `available_copies` | fast reads, safe writes in one transaction | count issued rows on every page |
| Vanilla JS, no framework | zero build step; teaches real HTTP | React (adds npm complexity) |
| `admin.js` shell + `shell:ready` | one auth guard for all pages | duplicating guards per page |
| Generic login error | prevents email enumeration | "user not found" (leaks info) |
| JSON 401/403 for APIs, 302 for pages | frontend can act on status codes | Spring's default HTML error pages |
| Explicit `AuthenticationManager` bean | Spring Boot 4 no longer creates it automatically | relying on auto-config (startup crash) |
| Secrets in `application-local.properties` | password never reaches GitHub | password as a default property |
| Seeding only when empty | safe to restart anytime | always insert (duplicates) |

---

## 13. Bugs we hit and how they were solved

| Symptom | Root cause | Fix |
|---|---|---|
| Every POST returned **403** with a valid token | Spring Security 7's default XOR handler base64-decodes the header; our raw UUID token failed the length check | `.csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())` in `SecurityConfig` |
| Startup crash: *required a bean of AuthenticationManager* | Boot 4 doesn't auto-create it once you define your own `SecurityFilterChain` | added `AppUserDetailsService` + an explicit `authenticationManager` bean |
| *Unresolved compilation problem* at startup | a stale `.class` file from the IDE was newer than the sources, so Maven skipped recompiling | `mvn clean compile` |
| Caps Lock hint stayed visible | author rule `display:flex` beat the browser's `[hidden]` | added `[hidden] { display:none !important }` in CSS |
| Eye icon flashed instead of toggling | JS toggled `hidden` on two separate SVGs | one grid cell, CSS class `.is-revealed` |
| Lazy-load `LazyInitializationException` risk | entities touched outside a transaction | `@Transactional` on services + `@EntityGraph` on issue queries |

---

## 14. Recipe: how to add a new feature

Example: **"fines"** (a student pays money for overdue books).

1. **Entity** → `entity/Fine.java` + columns → restart, Hibernate adds the table
2. **Repository** → `repository/FineRepository.java` (`JpaRepository<Fine, Long>`)
3. **DTO** → `dto/FineRequest.java` (with validation) + `dto/FineResponse.java`
4. **Service** → `service/FineService.java` — *all rules live here*
5. **Controller** → `controller/AdminFineController.java` under `/api/admin/fines`
6. **Security** → add the path to `SecurityConfig` (or it falls under `/api/admin/**`)
7. **Frontend** → `admin/fines.html` + `js/fines.js`, register in `admin.js` links
8. **Verify** → `mvn test`, then try it with the browser + check `git status`

Never skip step 4: *controller receives, service decides, repository stores.*

---

## 15. Day-to-day commands

```powershell
# run (PowerShell, from the project folder)
$env:JAVA_HOME="C:\Program Files\Eclipse Adoptium\jdk-25.0.4.101-hotspot"
& "C:\Program Files\apache-maven-3.10.0-bin\apache-maven-3.10.0\bin\mvn.cmd" spring-boot:run

# compile only / tests / clean build
mvn -q -DskipTests compile
mvn test
mvn clean

# peek at the data
mysql -u college_library_app -p college_library
SELECT isbn, title, available_copies FROM books;
SELECT college_id, name, active FROM users;
```

Open **http://localhost:8080** — admin `admin@college.edu` / `Admin@123`.

---

## 16. Roadmap

| Phase | Feature | Status |
|---|---|---|
| 1–2 | Login page, styling | ✅ done |
| 3 | Admin foundation: auth, dashboard, users, books, issue/return, stats | ✅ done |
| 4 | Student portal: my loans, due dates, overdue notice | ⬜ next |
| 5 | Librarian reports: most borrowed, overdue list, CSV export | ⬜ |
| 6 | Password change / profile, audit log | ⬜ |
| 7 | Deployment (Docker + cloud) and CI with GitHub Actions | ⬜ |

**When you finish a phase:** update this file, update the README, commit, push.
Documentation is part of the feature, not an afterthought.
