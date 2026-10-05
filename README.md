# College Library Management System

> A full-stack College Library Management System built with **Spring Boot 4 + Java 25 + MySQL 8**
> and a clean vanilla-JS frontend (no framework, no build step).

**New to this codebase?** Read **[ARCHITECTURE.md](ARCHITECTURE.md)** first — it explains
every layer, the reason behind each decision, and how to add new features.

**What it does**

- Session-based login with roles (Admin / Librarian / Student), CSRF protection and password hashing
- Admin dashboard with live statistics: books, students, copies, issued, overdue
- User management: create, edit, deactivate and delete students and librarians
- Book catalogue: add, edit, archive, search, pagination, genre / year / price / rating
- Issue & return books with automatic stock counts, due dates (+14 days) and overdue detection
- Server-side validation with friendly field-level error messages
- Responsive, modern UI served straight from Spring Boot

---

## Quick Start

**Prerequisites:** JDK 25 · Maven · MySQL 8

```bash
# 1. Create the database (run as MySQL root)
CREATE DATABASE college_library CHARACTER SET utf8mb4;
CREATE USER 'college_library_app'@'localhost' IDENTIFIED BY 'your_password';
GRANT ALL PRIVILEGES ON college_library.* TO 'college_library_app'@'localhost';
FLUSH PRIVILEGES;

# 2. Local credentials (this file is gitignored)
copy application-local.properties.example application-local.properties
#    ...then put your DB password inside it

# 3. Run the application
mvn spring-boot:run

# 4. Open http://localhost:8080
```

**Default logins** (seeded on the first start):

| Role | Login | Password |
|---|---|---|
| Admin | `admin@college.edu` | `Admin@123` |
| Student | `S1006` | `Student@123` |

The first run also seeds 5 sample books and 5 students. Set `SEED_DEMO_DATA=false`
in `application.properties` for an empty catalogue.

**Configuration:** real secrets live in `application-local.properties` (gitignored).
In production use environment variables instead — `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`.

---

## 1. Project Overview

This project is a **real-world style College Library Management System**
being built as a learning, portfolio, and future-extendable project.

The goal is NOT to make a simple tutorial/demo application. The goal is
to learn how a professional web application is planned, designed,
developed, connected to a database, secured, tested, maintained, and
eventually deployed.

The developer is a BCA 2nd-year student who wants to become capable of
building applications independently rather than becoming dependent on
AI-generated code.

Therefore, every major technical decision must be explained while
building the project.

------------------------------------------------------------------------

# 2. Main Goal

Build a usable College Library Management System where:

-   Students can log in and manage their library activity.
-   Librarians can manage books and issue/return operations.
-   Administrators can manage users and system-level operations.
-   Book data is stored in MySQL.
-   Authentication and authorization are handled securely by the
    backend.
-   The frontend communicates with the backend through APIs.
-   The application has a clean, modern, responsive UI.
-   The project follows maintainable software-development practices.
-   The project can eventually be deployed and used as a real
    application.

The system should be designed so that new features can be added later
without rewriting the entire project.

------------------------------------------------------------------------

# 3. Learning Goal

This project is also a complete learning path.

The developer should understand:

-   How websites work.
-   How frontend and backend communicate.
-   How HTTP requests and responses work.
-   What REST APIs are.
-   How Spring Boot applications are structured.
-   What controllers, services, repositories, entities, and DTOs are.
-   How Java applications communicate with MySQL.
-   How authentication works.
-   How authorization and roles work.
-   How passwords should be handled securely.
-   How database relationships work.
-   How validation works.
-   How errors are handled.
-   How frontend JavaScript communicates with APIs.
-   How Git and GitHub are used in real projects.
-   How applications are tested.
-   How applications are configured for different environments.
-   How a project can eventually be deployed.

AI assistance is allowed, but the AI must teach the reasoning behind the
code.

------------------------------------------------------------------------

# 4. Current Technology Stack

## Backend

-   Java
-   Spring Boot
-   Spring Web / REST API
-   Spring Security
-   Spring Data JPA
-   Hibernate
-   Maven
-   Bean Validation

## Database

-   MySQL Server
-   MySQL Workbench or another SQL client

## Frontend

Initially:

-   HTML5
-   CSS3
-   JavaScript

The frontend should remain simple initially so the developer understands
the fundamentals.

A frontend framework such as React may be considered later if it
provides a real benefit, but it should NOT be introduced just because it
is popular.

## Development Tools

-   Visual Studio Code
-   Git
-   GitHub
-   Maven Wrapper
-   Java Development Kit (JDK)

## Current Environment

The project was created using:

-   Java 25
-   Spring Boot 4.x
-   Maven Wrapper
-   VS Code

The exact dependency versions should normally be controlled by `pom.xml`
rather than manually changing versions throughout the project.

------------------------------------------------------------------------

# 5. Important Architecture Decision

The project should use a layered architecture.

High-level architecture:

``` text
Browser
   |
   | HTTP
   v
Frontend
HTML + CSS + JavaScript
   |
   | REST API / JSON
   v
Spring Boot Backend
   |
   +--> Controller
   |
   +--> Service
   |
   +--> Repository
   |
   +--> Entity / DTO
   |
   v
MySQL Database
```

Authentication/security will eventually be handled through Spring
Security.

------------------------------------------------------------------------

# 6. User Roles

The application should support role-based access control.

Initial roles:

``` text
ADMIN
LIBRARIAN
STUDENT
```

## ADMIN

Possible responsibilities:

-   Manage users
-   Manage librarians
-   Manage system settings
-   View reports
-   Manage roles/permissions
-   Monitor library activity

## LIBRARIAN

Possible responsibilities:

-   Add books
-   Edit books
-   Remove books
-   Search books
-   Issue books
-   Return books
-   Manage members
-   View overdue books
-   View library activity

## STUDENT

Possible responsibilities:

-   Log in
-   View available books
-   Search books
-   View book details
-   View currently issued books
-   View due dates
-   View borrowing history
-   Request/renew books if the system supports it
-   Update permitted profile information

IMPORTANT:

The user must NOT select a role on the login page and gain permissions
based on that selection.

The backend must determine the authenticated user's role.

Example:

``` text
User logs in
     |
     v
Spring Security authenticates user
     |
     v
Backend reads user's role
     |
     +---- STUDENT ----> Student permissions
     |
     +---- LIBRARIAN -> Librarian permissions
     |
     +---- ADMIN -----> Admin permissions
```

Frontend buttons must NOT be considered a security mechanism.

------------------------------------------------------------------------

# 7. Account Creation Strategy

This is a college system, not a public social-media website.

Do NOT implement an unrestricted:

``` text
Create Admin Account
```

publicly.

Students should not automatically receive unlimited access simply by
entering an email address.

A realistic future account flow can be:

``` text
Admin/Librarian
       |
       v
Creates or approves student account
       |
       v
Student activates account
       |
       v
Student creates password
       |
       v
Student logs in
```

A registration-request system may also be added later:

``` text
Student
   |
   v
Request account
   |
   v
Admin reviews request
   |
   +---- Reject
   |
   +---- Approve
             |
             v
        Account activated
```

The exact implementation can be decided when authentication is built.

------------------------------------------------------------------------

# 8. Current Frontend Design

The first page is a modern College Library login/welcome page.

Visual direction:

-   Split-screen layout
-   Large library image on the left
-   Login panel on the right
-   White/light background
-   Modern typography
-   Rounded corners
-   Subtle shadows
-   Professional blue/teal accents
-   Responsive design
-   Clean spacing
-   Library branding
-   Password visibility control
-   Remember-me option
-   Forgot-password option
-   Login button
-   Account-access/help message

Reference design concept:

``` text
+-------------------------------------------------------------+
|                                                             |
|                 |                                           |
|  LIBRARY       |        College Library                     |
|  IMAGE         |        Welcome back                        |
|                |        Sign in to your account             |
|                |                                           |
|                |        College ID / Email                  |
|                |        [________________________]          |
|                |                                           |
|                |        Password                            |
|                |        [________________________]          |
|                |                                           |
|                |        [ ] Remember me      Forgot?        |
|                |                                           |
|                |        [          SIGN IN          ]       |
|                |                                           |
|                |        Need access?                        |
|                |        Contact library administrator       |
|                                                             |
+-------------------------------------------------------------+
```

Do NOT add Facebook or Google login unless a real requirement is
introduced.

Do NOT add unnecessary animations or libraries.

------------------------------------------------------------------------

# 9. Frontend Project Structure

Initial structure:

``` text
src/
└── main/
    └── resources/
        └── static/
            ├── index.html
            │
            ├── css/
            │   └── style.css
            │
            ├── js/
            │   └── app.js
            │
            └── assets/
                ├── images/
                │   ├── library.jpg
                │   └── ...
                │
                └── icons/
```

As the application grows, the structure may become:

``` text
static/
├── index.html
│
├── pages/
│   ├── login.html
│   ├── dashboard.html
│   ├── books.html
│   ├── students.html
│   └── ...
│
├── css/
│   ├── style.css
│   ├── auth.css
│   ├── dashboard.css
│   └── ...
│
├── js/
│   ├── app.js
│   ├── auth.js
│   ├── books.js
│   ├── students.js
│   └── ...
│
└── assets/
    ├── images/
    └── icons/
```

The structure should only be expanded when the project actually needs
it.

------------------------------------------------------------------------

# 10. Image and Asset Rules

Images belong inside:

``` text
src/main/resources/static/assets/images/
```

Example:

``` text
static/
└── assets/
    └── images/
        └── library.jpg
```

HTML example:

``` html
<img src="assets/images/library.jpg" alt="College Library">
```

CSS example:

``` css
background-image: url("../assets/images/library.jpg");
```

Do not assume filenames.

If the developer provides an image and filename, use that exact
filename.

Do not download random external images without asking.

Prefer local project assets for important UI images.

------------------------------------------------------------------------

# 11. Backend Structure

The backend should eventually follow a structure similar to:

``` text
src/main/java/com/collegelibrary/

├── CollegeLibraryApplication.java
│
├── controller/
│   ├── AuthController.java
│   ├── BookController.java
│   ├── UserController.java
│   └── IssueController.java
│
├── service/
│   ├── AuthService.java
│   ├── BookService.java
│   ├── UserService.java
│   └── IssueService.java
│
├── repository/
│   ├── UserRepository.java
│   ├── BookRepository.java
│   └── IssueRepository.java
│
├── entity/
│   ├── User.java
│   ├── Book.java
│   ├── Issue.java
│   └── ...
│
├── dto/
│   ├── LoginRequest.java
│   ├── LoginResponse.java
│   └── ...
│
├── security/
│   ├── SecurityConfig.java
│   └── ...
│
├── exception/
│   ├── GlobalExceptionHandler.java
│   └── ...
│
└── config/
    └── ...
```

Do not create every package immediately.

Create packages when their responsibility becomes necessary.

------------------------------------------------------------------------

# 12. Database Design

The database will eventually contain entities such as:

## users

Possible fields:

``` text
id
college_id
name
email
password_hash
role
course
year
active
created_at
updated_at
```

## books

Possible fields:

``` text
id
isbn
title
author
publisher
category
total_copies
available_copies
created_at
updated_at
```

## book_issues

Possible fields:

``` text
id
book_id
user_id
issued_at
due_date
returned_at
status
```

Other tables may be added later depending on requirements.

The final database design should be normalized and based on actual
relationships rather than creating random tables.

------------------------------------------------------------------------

# 13. Important Database Relationships

Example:

``` text
User
  |
  | can have many
  v
BookIssue
  ^
  |
  | belongs to
Book
```

Meaning:

-   One student can have multiple borrowing records.
-   One book can appear in multiple historical borrowing records.
-   Each issue record belongs to one user and one book.

The exact JPA relationships should be taught and explained before
implementation.

------------------------------------------------------------------------

# 14. API Design

The backend should eventually expose REST APIs.

Example:

``` text
POST   /api/auth/login
POST   /api/auth/logout

GET    /api/books
GET    /api/books/{id}
POST   /api/books
PUT    /api/books/{id}
DELETE /api/books/{id}

GET    /api/users
GET    /api/users/{id}
POST   /api/users

POST   /api/issues
POST   /api/issues/{id}/return
GET    /api/issues
```

These are examples, not final requirements.

API design should be discussed before implementation.

------------------------------------------------------------------------

# 15. Login Architecture

The login page is initially only a frontend.

Eventually:

``` text
User
 |
 | enters email/password
 v
HTML form
 |
 v
JavaScript
 |
 | POST JSON
 v
/api/auth/login
 |
 v
Spring Boot Controller
 |
 v
Authentication Service / Spring Security
 |
 v
MySQL
 |
 v
Authentication result
 |
 +---- Success
 |       |
 |       v
 |   Session/token/cookie
 |       |
 |       v
 |   Dashboard
 |
 +---- Failure
         |
         v
     Error message
```

Never trust frontend JavaScript to decide whether a user is
authenticated.

Never store plaintext passwords.

Passwords must be securely hashed using an appropriate password encoder
through Spring Security.

------------------------------------------------------------------------

# 16. Frontend Responsibilities

JavaScript can handle:

-   Form validation
-   Show/hide password
-   Loading indicators
-   Displaying API errors
-   Sending API requests
-   Processing API responses
-   Updating UI
-   Navigation

JavaScript should NOT be responsible for:

-   Deciding user permissions
-   Storing plaintext passwords
-   Performing privileged database operations
-   Bypassing backend security

------------------------------------------------------------------------

# 17. Backend Responsibilities

Spring Boot should handle:

-   Business logic
-   Authentication
-   Authorization
-   Validation
-   Database access
-   API endpoints
-   Error handling
-   Transactions
-   Security rules

------------------------------------------------------------------------

# 18. MySQL Responsibilities

MySQL stores persistent data:

-   Users
-   Books
-   Borrowing records
-   Categories
-   Library activity
-   Other application data

Do not hardcode important application data into JavaScript.

------------------------------------------------------------------------

# 19. Security Requirements

Security is part of the project, not an optional extra.

Eventually implement:

-   Spring Security
-   Password hashing
-   Authentication
-   Role-based authorization
-   Input validation
-   Access control
-   Proper error handling
-   Secure session/token strategy
-   Protection against unauthorized API access
-   Environment-based database credentials
-   No passwords/API keys committed to Git
-   Appropriate CORS configuration if needed

Do not implement security by simply hiding buttons.

Example:

BAD:

``` text
Hide Delete Book button for students
```

GOOD:

``` text
Student calls DELETE /api/books/10
        |
        v
Spring Security checks permission
        |
        v
403 Forbidden
```

------------------------------------------------------------------------

# 20. Configuration

Database credentials and secrets should not be hardcoded into Java
source files.

Development configuration may use:

``` text
application.properties
```

Later, environment variables or a proper secrets/configuration system
should be used.

Never commit real passwords, API keys, or production secrets to GitHub.

------------------------------------------------------------------------

# 21. Git and GitHub

Git should be used throughout the project.

Recommended workflow:

``` text
Create feature
     |
     v
Write code
     |
     v
Test
     |
     v
Git status
     |
     v
Git add
     |
     v
Git commit
     |
     v
Git push
```

Use meaningful commits.

Examples:

``` text
feat: create library login page
style: add login page styling
feat: add book entity
feat: add book API
fix: correct book issue validation
```

Do not make one giant commit after the entire project is finished.

------------------------------------------------------------------------

# 22. Testing

Testing should be introduced gradually.

Eventually use:

-   JUnit
-   Spring Boot testing
-   MockMvc
-   Repository tests where useful
-   Service tests
-   API testing
-   Manual frontend testing

Possible tools:

-   Browser DevTools
-   Postman or another API client
-   MySQL Workbench

------------------------------------------------------------------------

# 23. Development Workflow

For each feature:

``` text
1. Understand the requirement
2. Design the feature
3. Decide data needed
4. Design database changes
5. Design API
6. Build backend
7. Test backend
8. Build frontend
9. Connect frontend to API
10. Test complete feature
11. Refactor if needed
12. Commit to Git
```

However, for learning, small vertical slices are preferred.

Example:

``` text
LOGIN FEATURE

HTML
 ↓
CSS
 ↓
JavaScript
 ↓
Spring Boot API
 ↓
MySQL
 ↓
Spring Security
 ↓
Dashboard
```

Then move to:

``` text
BOOK FEATURE
```

Then:

``` text
ISSUE/RETURN FEATURE
```

------------------------------------------------------------------------

# 24. Planned Feature Roadmap

## Phase 1 --- Project Foundation

-   Spring Boot project
-   Maven
-   VS Code
-   Git/GitHub
-   Basic project structure
-   Understand Spring Boot startup
-   Understand static resources

## Phase 2 --- Login UI

-   Welcome/login page
-   Responsive layout
-   Library image
-   CSS
-   Password visibility
-   Form validation
-   Error states
-   Loading state

## Phase 3 --- Backend Fundamentals

-   REST API
-   Controller
-   Service
-   Repository
-   DTO
-   HTTP methods
-   JSON
-   Request/response

## Phase 4 --- Database

-   MySQL connection
-   Database creation
-   JPA
-   Entities
-   Repositories
-   Relationships
-   CRUD

## Phase 5 --- Authentication

-   Spring Security
-   Password hashing
-   Login API
-   Authentication
-   Authorization
-   Roles

## Phase 6 --- Dashboard

Student dashboard:

-   Available books
-   Issued books
-   Due dates
-   Borrowing history
-   Profile

Librarian dashboard:

-   Books
-   Students
-   Issue
-   Return
-   Overdue books

Admin dashboard:

-   Users
-   Librarians
-   Reports
-   System management

## Phase 7 --- Book Management

-   Add book
-   Edit book
-   Delete book
-   Search book
-   Filter
-   Categories
-   Stock/copies

## Phase 8 --- Issue and Return

-   Issue book
-   Return book
-   Due date
-   Overdue status
-   Borrowing history
-   Availability calculation

## Phase 9 --- Validation and Error Handling

-   Backend validation
-   Frontend validation
-   Global exception handling
-   User-friendly errors
-   HTTP status codes

## Phase 10 --- Professional Improvements

-   Pagination
-   Search
-   Sorting
-   Filtering
-   Audit information
-   Better UI
-   Accessibility
-   Responsive mobile design

## Phase 11 --- Testing

-   Unit tests
-   Service tests
-   Repository tests
-   Controller/API tests
-   Integration tests

## Phase 12 --- Deployment

Eventually learn:

-   Build JAR
-   Environment variables
-   Production database
-   Server deployment
-   HTTPS
-   Domain
-   Logging
-   Monitoring

Deployment should happen only after the application is stable.

------------------------------------------------------------------------

# 25. What NOT to Do

Do NOT:

-   Copy an old YouTube tutorial blindly.
-   Use Java 1.8 just because an old tutorial uses it.
-   Use Apache Tomcat separately for the Spring Boot application unless
    there is a specific reason.
-   Create the entire application in one huge file.
-   Put database code directly inside controllers.
-   Put passwords in JavaScript.
-   Trust frontend role selection for security.
-   Create unnecessary libraries.
-   Add technologies just because they are trendy.
-   Generate hundreds of lines of AI code without understanding them.
-   Change the architecture without explaining why.
-   Hardcode database credentials.
-   Commit secrets to GitHub.

Spring Boot already provides an embedded web server, so a separate
manual Tomcat installation is not required for normal development.

------------------------------------------------------------------------

# 26. AI Assistance Rules

Any AI helping with this project MUST follow these rules.

## Rule 1 --- Teach, don't just generate

For every important code change, explain:

-   What it does
-   Why it exists
-   How it works
-   Where it fits in the architecture
-   When the developer will use the concept again

## Rule 2 --- Small steps

Do not provide the entire project at once.

Work feature-by-feature and step-by-step.

## Rule 3 --- Preserve architecture

Do not randomly replace:

-   Spring Boot
-   Java
-   MySQL
-   HTML/CSS/JavaScript

with another stack.

If a change is genuinely useful, explain the reason first.

## Rule 4 --- No blind copying

The developer should understand the code before moving forward.

## Rule 5 --- Debug properly

When there is an error:

1.  Read the exact error.
2.  Identify the cause.
3.  Explain why it happened.
4.  Make the smallest appropriate fix.
5.  Explain how to prevent similar errors.

Do not immediately rewrite the whole project.

## Rule 6 --- Ask before major architectural changes

Major changes include:

-   Adding React
-   Changing database technology
-   Changing authentication architecture
-   Adding Docker
-   Changing API architecture
-   Introducing microservices

These should only happen when there is a clear reason.

------------------------------------------------------------------------

# 27. Current Project Status

Completed:

-   JDK installed
-   VS Code configured
-   Spring Boot project created
-   Maven Wrapper generated
-   Spring Boot application successfully starts
-   Embedded Tomcat starts on port 8080
-   `static/index.html` created
-   Initial HTML login structure created
-   CSS folder created
-   `style.css` created

Current task:

**Build the professional login/welcome page.**

Current files:

``` text
src/main/resources/static/
├── index.html
└── css/
    └── style.css
```

Next frontend structure:

``` text
src/main/resources/static/
├── index.html
├── css/
│   └── style.css
├── js/
│   └── app.js
└── assets/
    └── images/
```

Do not jump to database/authentication until the developer understands
the frontend foundation.

------------------------------------------------------------------------

# 28. Current Immediate Task

The immediate task is to transform the basic HTML login page into a
professional College Library login page based on the agreed design.

The page should have:

-   Library image
-   College Library branding
-   Login form
-   College email/ID field
-   Password field
-   Show/hide password
-   Remember me
-   Forgot password
-   Login button
-   Account-access/help message
-   Responsive layout

Do not implement real authentication yet.

First finish and understand:

``` text
HTML
 ↓
CSS
 ↓
JavaScript UI behavior
```

Then connect it to:

``` text
Spring Boot REST API
```

Then:

``` text
MySQL
```

Then:

``` text
Spring Security
```

------------------------------------------------------------------------

# 29. Definition of Done

The final project should eventually be capable of:

-   Secure user login
-   Role-based access
-   Student management
-   Librarian management
-   Book management
-   Book searching
-   Book issuing
-   Book returning
-   Due-date tracking
-   Overdue tracking
-   Borrowing history
-   Dashboard statistics
-   Database persistence
-   Input validation
-   Error handling
-   API-based frontend/backend communication
-   Automated tests
-   Git version control
-   Production-ready configuration
-   Deployment

The application should be maintainable and understandable by another
developer.

------------------------------------------------------------------------

# 30. Core Principle

This is not just a project to "make something that works."

The main goal is:

``` text
Understand
    ↓
Design
    ↓
Build
    ↓
Test
    ↓
Debug
    ↓
Refactor
    ↓
Deploy
```

The developer should finish the project able to start another
application without needing AI to write every line.

AI is a **teacher, reviewer, debugger, and assistant** --- not the
developer.

------------------------------------------------------------------------

# 31. Project Structure

```text
college-library/
├── src/main/java/com/collegelibrary/
│   ├── config/        # startup seeding
│   ├── controller/    # REST endpoints
│   ├── dto/           # request/response shapes
│   ├── entity/        # JPA tables (users, books, book_issues)
│   ├── enums/         # Role, IssueStatus
│   ├── exception/     # error handling
│   ├── repository/    # database access
│   ├── security/      # login, roles, CSRF
│   └── service/       # business rules
├── src/main/resources/
│   ├── application.properties
│   └── static/        # HTML + CSS + JS (no framework)
├── application-local.properties.example
├── ARCHITECTURE.md          # how the whole system works, and why
├── pom.xml
└── LICENSE
```

# 32. Version Control Rules

- Never commit secrets — `application-local.properties` is gitignored.
- Never commit `target/`, IDE folders or logs.
- Small, descriptive commits: `feat: add book issue flow`, `fix: cap lock hint`.
- Use a feature branch for anything experimental, then merge it.

# 33. License

Released under the [MIT License](LICENSE).
