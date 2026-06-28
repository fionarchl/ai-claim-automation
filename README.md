# ClaimOps

Spring Boot REST API plus React dashboard for claim operations: claim intake, benefit/detail review, notes, workflow status, documents, and user administration.

The dashboard includes two claim registration paths:

- **New**: manual claim registration.
- **Register via AI**: upload claim documents, let AI extract a claim draft, run stored rules, create the claim in `UNDER_REVIEW`, and store an explainable AI recommendation for analyst review.

The application uses Spring Data JPA so the business code is database-neutral. Customer and policy data are treated as existing reference data; the claim workflow reads that data and writes claims against it. Switch databases by changing the active Spring profile:

- `sqlite` uses a local SQLite file at `./claimops.sqlite`.
- `sqlserver` uses Microsoft SQL Server through the JDBC driver.

## Requirements

- JDK 21
- Maven 3.8+ or the included project-local Maven helper scripts
- Node.js and npm for the React dashboard

## Run With SQLite

```powershell
.\scripts\run-sqlite.ps1
```

The default profile is `sqlite`.

In another PowerShell window, start the React dashboard:

```powershell
.\scripts\run-frontend.ps1
```

Open:

```text
http://127.0.0.1:5173
```

Sign in with the seeded administrator:

- Username: `jdoe`
- Password: `admin123`

Stop background servers:

```powershell
.\scripts\stop-dev.ps1
```

## AI Intake Configuration

AI intake starts with database-backed settings that can be managed from **AI Configuration** in the sidebar. The application only keeps secrets outside the database. Copy `.env.example` to `.env`, then add the provider key when using provider mode:

```text
CLAIMOPS_AI_PROVIDER_API_KEY=your-provider-key
```

`test` mode is deterministic and needs no API key. `provider` mode calls the OpenAI-compatible chat completions endpoint configured in the database.

The helper scripts `.\scripts\run-sqlite.ps1`, `.\scripts\run-sqlserver.ps1`, and `node .\scripts\start-dev.mjs` load `.env` automatically.

After the app starts, system administrators can use **AI Configuration** in the sidebar. The menu stores settings in the database and separates:

- **Document Extraction**: OCR/vision/text extraction from uploaded claim documents.
- **Claim Reasoning**: analyst-facing summary and evidence after backend rules run.

`.env` and process environment variables remain the place for API keys. The database-backed AI Configuration screen is the normal place to adjust non-secret provider settings such as mode, endpoint, model, temperature, text preview limit, confidence threshold, and rule behavior.

API keys are not stored from the UI. Keep provider keys in `.env` or process environment variables:

```text
CLAIMOPS_AI_PROVIDER_API_KEY=your-shared-provider-key
CLAIMOPS_AI_EXTRACTION_API_KEY=optional-extraction-only-key
CLAIMOPS_AI_REASONING_API_KEY=optional-reasoning-only-key
```

The AI Configuration screen only shows whether a usable key is configured.

For Google Gemini's OpenAI-compatible endpoint, set the API key in `.env`, then configure provider mode, endpoint, and model from **AI Configuration**. The endpoint can be the base URL `https://generativelanguage.googleapis.com/v1beta/openai/` or the full chat-completions URL.

The AI provider returns a structured claim draft. The backend still owns policy lookup, validation, rule evaluation, database writes, status history, and final workflow status. AI-created claims are registered as `UNDER_REVIEW`; claim analysts approve or reject them through the normal workflow.

Current deterministic rules include:

- Policy must be active.
- Policy must not have an active blacklist entry.
- Claim dates must be present, ordered correctly, and inside coverage dates.
- Estimated amount must be present and within coverage.
- Claim documents must be uploaded.
- Billing evidence should be present.
- Duplicate same-policy claims are flagged for manual review.
- Low AI confidence is flagged for manual review.

## Run With SQL Server

Create a SQL Server database named `ClaimOps`, then copy the example environment file:

```powershell
Copy-Item .env.example .env
```

Edit `.env` with your local credentials:

```text
DB_URL=jdbc:sqlserver://localhost:1433;databaseName=ClaimOps;encrypt=true;trustServerCertificate=true
DB_USERNAME=sa
DB_PASSWORD=your-local-password
```

Then run:

```powershell
.\scripts\run-sqlserver.ps1
```

You can also start the SQL Server backend and React dashboard together in the background:

```powershell
.\scripts\mvnw-local.ps1 package
node .\scripts\start-dev.mjs
```

Logs are written to `logs\backend.log` and `logs\frontend.log`.

The real `.env` file is ignored by Git. You can also pass temporary values without creating `.env`:

```powershell
.\scripts\run-sqlserver.ps1 -DbUrl "jdbc:sqlserver://localhost:1433;databaseName=ClaimOps;encrypt=true;trustServerCertificate=true" -DbUsername "sa" -DbPassword "your-local-password"
```

SQLite and SQL Server both run the same development seed data on startup. For a clean portfolio database with audit columns displayed last in SQL Server table design, recreate the local `ClaimOps` database before starting the SQL Server profile. SQL Server does not support reordering existing table columns in place.

Only configuration changes are needed. The controllers, services, repositories, and entities stay the same.

## Local Tooling

This project has helper scripts that use `JAVA_HOME` or `java` from `PATH`, project-local Maven at `.tools\apache-maven-3.9.6` when present, installed `mvn` as a fallback, and a project-local dependency cache at `.m2\repository`.

Run Maven through:

```powershell
.\scripts\mvnw-local.ps1 test
.\scripts\mvnw-local.ps1 package
.\scripts\mvnw-local.ps1 spring-boot:run
```

Run the frontend through:

```powershell
cd frontend
npm run dev
npm run build
```

## Verify

```powershell
.\scripts\mvnw-local.ps1 test
```

## Main Endpoints

Auth:

- `POST /api/auth/login`

Users:

- `GET /api/users`
- `GET /api/users/{id}`
- `POST /api/users`
- `PATCH /api/users/{id}/permissions`
- `PATCH /api/users/{id}`
- `DELETE /api/users/{id}`

Customers:

- `GET /api/customers`
- `GET /api/customers/{id}`

Policies:

- `GET /api/policies`
- `GET /api/policies/{id}`
- `GET /api/policies/customer/{customerId}`
- `GET /api/policies/{id}/blacklist`
- `POST /api/policies/{id}/blacklist`
- `PATCH /api/policies/{id}/blacklist/{entryId}/resolve`

Claims:

- `POST /api/claims`
- `POST /api/claims/ai-intake`
- `GET /api/claims`
- `GET /api/claims/{id}`
- `GET /api/claims/{id}/ai-assessment`
- `GET /api/claims?status=UNDER_REVIEW`
- `PUT /api/claims/{id}`
- `PATCH /api/claims/{id}/status`
- `GET /api/claims/{id}/details`
- `POST /api/claims/{id}/details`
- `PUT /api/claims/{id}/details/{detailId}`
- `DELETE /api/claims/{id}/details/{detailId}`
- `POST /api/claims/{id}/notes`
- `GET /api/claims/{id}/notes`
- `GET /api/claims/{id}/history`
- `GET /api/claims/{id}/documents/{documentId}`
- `DELETE /api/claims/{id}/documents/{documentId}`

AI Configuration:

- `GET /api/ai-configuration`
- `PUT /api/ai-configuration/provider`
- `PUT /api/ai-configuration/extraction`
- `PUT /api/ai-configuration/reasoning`
- `PUT /api/ai-configuration/rules`

Claim detail rejected amounts are generated from submitted minus approved amount. Claim approved amount is derived from the total approved amount across claim details and cannot exceed coverage for the claim admission/discharge period.

Protected endpoints are enforced by a Spring Security bearer-token filter and require the `Authorization: Bearer <token>` request header after login. The React dashboard stores the login token locally and adds it automatically for the signed-in user.

## Seeded Development Admin User

The SQLite and SQL Server development profiles seed one administrator account:

- Name: `John Doe`
- Username: `jdoe`
- Role: `System Administrator`
- Password: `admin123`

These credentials are for local development only. Change the seeded account or reset the password before using a shared or hosted database.

The frontend uses a User Management menu under Administration for listing users. Add user and view/edit user each open on their own page. Passwords are stored as BCrypt hashes, are not shown in the UI, and are not returned by the API.

## Roles

- `Claim Admin`: can create claims and view/edit claims in `FILED` or `UNDER_REVIEW`; cannot approve or reject claims.
- `Claim Analyst`: can create and view claims, edit claim details while claims are filed or under review, and approve or reject claims.
- `System Administrator`: can do all claim actions and manage users.

Use the Logout button in the top bar to end the local dashboard session.

## Example Requests

Create claim:

```json
{
  "policyId": 1,
  "admissionDate": "2026-06-10",
  "dischargeDate": "2026-06-12",
  "description": "Rear bumper damage after minor accident.",
  "estimatedAmount": 25000000,
  "documentNames": ["invoice.pdf", "medical-summary.pdf"]
}
```

Claim numbers are generated automatically by the API.

Update claim status:

```json
{
  "status": "APPROVED",
  "changedBy": "adjuster@example.com",
  "comment": "Approved after document review."
}
```
