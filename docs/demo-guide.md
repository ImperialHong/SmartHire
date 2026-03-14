# SmartHire Demo Guide

## Goal

Use one clear story to demonstrate the full SmartHire P0 workflow:

`candidate login -> browse jobs -> apply -> HR review -> schedule interview -> candidate checks notifications`

## Prerequisites

1. Initialize the database:

```bash
mysql -u root -p smarthire < sql/001_init_schema.sql
mysql -u root -p smarthire < sql/002_seed_demo_data.sql
```

2. Start the backend:

```bash
cd backend
mvn spring-boot:run
```

3. Open Swagger UI:

```text
http://localhost:8080/swagger-ui.html
```

## Demo Accounts

| Role | Email | Password |
| --- | --- | --- |
| Candidate | `candidate@example.com` | `password123` |
| Candidate | `candidate2@example.com` | `password123` |
| HR | `hr@example.com` | `password123` |
| Admin | `admin@example.com` | `password123` |

## Recommended Demo Flow

### 1. Candidate login

- Call `POST /api/auth/login` with `candidate@example.com`
- Copy the returned JWT token
- Use `GET /api/auth/me` to show the authenticated profile

Expected talking points:

- JWT based authentication
- fixed role model
- candidate default registration role

### 2. Browse jobs

- Call `GET /api/jobs`
- Highlight open jobs and filter support
- Open `GET /api/jobs/{id}` for `Backend Engineer`

Expected talking points:

- public job browsing
- pagination and filtering
- job status management

### 3. Candidate reviews existing application state

- Call `GET /api/applications/me`
- Show one application in `INTERVIEW`
- Show another application still in `APPLIED`

Expected talking points:

- one candidate can apply to one job only once
- application status flow

### 4. HR login and review applications

- Login with `hr@example.com`
- Call `GET /api/jobs/{jobId}/applications`
- Highlight `Candidate User` and `Second Candidate`

Expected talking points:

- HR can only manage their own jobs
- recruiter note and state transitions

### 5. Show interview arrangement

- Call `GET /api/jobs/{jobId}/interviews`
- Highlight the scheduled interview for `Candidate User`
- Optionally call `PATCH /api/interviews/{id}` to update remark or status

Expected talking points:

- single interview per application in V1
- interview scheduling pushes application to `INTERVIEW`

### 6. Candidate checks notifications

- Switch back to the candidate token
- Call `GET /api/notifications`
- Call `GET /api/notifications/unread-count`
- Call `PATCH /api/notifications/{id}/read`

Expected talking points:

- synchronous notification generation
- notification types map to business events
- unread count and read status support

### 7. Admin quick view

- Login with `admin@example.com`
- Call `GET /api/notifications`

Expected talking points:

- admin role already provisioned for later expansion
- current V1 keeps admin lightweight on purpose

## Recommended Screen Recording Order

1. Swagger home page
2. Candidate login and `/api/auth/me`
3. Job list and job detail
4. Candidate application list
5. HR application list
6. Interview list
7. Candidate notifications

## Notes

- If you use Docker Compose for a fresh environment, both `001` and `002` scripts load automatically.
- If a MySQL data volume already exists, re-run the SQL scripts manually to reset the demo state.
