# SmartHire Deployment Notes

This folder contains the first deployment-oriented assets for SmartHire.

## Files

- `docker-compose.prod.yml`
  Uses prebuilt backend and frontend images instead of local build contexts.
- `.env.example`
  Example runtime variables for the production compose stack.

## What Exists Today

The repository already has two deployment-related workflows:

- `.github/workflows/publish-images.yml`
  Publishes backend and frontend images to `GHCR`
- `.github/workflows/deploy.yml`
  Connects to a remote host through SSH and deploys the selected image tag

## GitHub Side Setup

### 1. Recommended GitHub Environment

Create a GitHub environment such as:

- `production`

This matches the default `environment_name` in `deploy.yml` and lets you add environment-scoped secrets or approval rules later.

### 2. Required GitHub Secrets

Add these secrets either at repository level or, preferably, under the `production` environment.

| Secret | Used by | Purpose |
| --- | --- | --- |
| `DEPLOY_HOST` | `deploy.yml` | Remote server IP or domain |
| `DEPLOY_USER` | `deploy.yml` | SSH login user |
| `DEPLOY_SSH_KEY` | `deploy.yml` | Private key used by GitHub Actions to SSH into the server |
| `DEPLOY_PORT` | `deploy.yml` | SSH port, usually `22` |
| `DEPLOY_PATH` | `deploy.yml` | Remote directory where compose files and env files will live |
| `GHCR_USERNAME` | `deploy.yml` | Username used on the server to log in to GHCR |
| `GHCR_TOKEN` | `deploy.yml` | GHCR token used on the server to pull private images |
| `PROD_ENV_FILE` | `deploy.yml` | Multiline runtime env content appended into the remote `.env` file |

### 3. Notes About GHCR Credentials

- `publish-images.yml` pushes images using GitHub's built-in `GITHUB_TOKEN`
- `deploy.yml` still needs `GHCR_USERNAME` and `GHCR_TOKEN` because the remote server must pull the images
- if your GHCR packages are private, the token must have package read permission

## PROD_ENV_FILE

Store the runtime variables as a multiline secret.

Start from `.env.example`, then remove these three lines before saving the secret:

- `BACKEND_IMAGE`
- `FRONTEND_IMAGE`
- `IMAGE_TAG`

Those three values are injected by the deployment workflow based on the selected image tag.

## First-Time Server Preparation

Before you run deployment for the first time, make sure the remote host already has:

1. Docker installed
2. Docker Compose v2 available as `docker compose`
3. A writable deploy directory, for example `/opt/smarthire`
4. Network access to `ghcr.io`
5. Ports opened as needed:
   - `5173` for frontend
   - `8080` for backend if you want direct backend access
   - `3306`, `6379`, `5672`, `15672` only if you intentionally expose them

## Recommended Runbook

### A. Publish images

Use one of these:

1. Push to `main` and let `CI` complete successfully
2. Trigger `publish-images.yml` manually from GitHub Actions

Expected result:

- backend image pushed to `ghcr.io/<owner>/smarthire-backend`
- frontend image pushed to `ghcr.io/<owner>/smarthire-frontend`
- tags include `latest` and a short commit SHA

### B. Deploy a selected image tag

1. Open GitHub Actions
2. Run `Deploy`
3. Select:
   - `environment_name`: usually `production`
   - `image_tag`: `latest` or a specific short SHA tag

Expected remote steps:

1. copy `deploy/docker-compose.prod.yml` to the server
2. write a fresh `.env`
3. log in to GHCR
4. pull the selected backend/frontend images
5. run `docker compose up -d --remove-orphans`

## Quick Verification After Deploy

After deployment, verify these on the server:

1. `docker compose --env-file .env -f docker-compose.prod.yml ps`
2. `docker compose --env-file .env -f docker-compose.prod.yml logs --tail=100 backend`
3. open the frontend URL and log in once
4. check `/api/health`
5. check that Flyway ran successfully in backend logs

## Current Scope

This is a deployable CD skeleton, not a full production platform yet.

Still worth adding later:

- domain and HTTPS setup
- rollback strategy
- staging vs production environments
- health checks after deployment
- package retention and cleanup strategy for GHCR
