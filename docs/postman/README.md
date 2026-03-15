# Postman Files

Import these two files together:

- `docs/postman/SmartHire.postman_collection.json`
- `docs/postman/SmartHire.local.postman_environment.json`

Recommended usage:

1. Import both files into Postman.
2. Select the `SmartHire Local` environment.
3. Run `Candidate Login`, `HR Login`, and `Admin Login` first.
4. Continue with the `Candidate Flow`, `HR Flow`, and `Admin Flow` folders.

Notes:

- `Upload Resume` requires manually selecting a local PDF file in Postman.
- Login requests automatically save `candidateToken`, `hrToken`, and `adminToken`.
- Create/list requests automatically save helpful IDs such as `jobId`, `applicationId`, `interviewId`, `notificationId`, and `userId`.
- The environment is prefilled with the seeded demo accounts from `sql/002_seed_demo_data.sql`.
