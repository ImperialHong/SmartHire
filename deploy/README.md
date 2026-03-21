# SmartHire Deployment Notes

This folder contains the first deployment-oriented assets for SmartHire.

## Files

- `docker-compose.prod.yml`
  Uses prebuilt backend and frontend images instead of local build contexts.
- `.env.example`
  Example runtime variables for the production compose stack.

## Recommended flow

1. Let `CI` pass.
2. Run `publish-images.yml` or wait for it to publish images to GHCR.
3. Configure the required GitHub secrets.
4. Trigger `deploy.yml` manually with the target image tag.

## Required GitHub Secrets

- `DEPLOY_HOST`
- `DEPLOY_USER`
- `DEPLOY_SSH_KEY`
- `DEPLOY_PORT`
- `DEPLOY_PATH`
- `GHCR_USERNAME`
- `GHCR_TOKEN`
- `PROD_ENV_FILE`

## PROD_ENV_FILE

Store the runtime variables as a multiline secret. Start from `.env.example`, then remove:

- `BACKEND_IMAGE`
- `FRONTEND_IMAGE`
- `IMAGE_TAG`

Those three values are injected by the deployment workflow based on the selected image tag.

## Current scope

This is a deployable CD skeleton, not a full production platform yet.

Still worth adding later:

- domain and HTTPS setup
- rollback strategy
- staging vs production environments
- health checks after deployment
