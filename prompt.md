Task: Completely Remove PostgreSQL and Use MongoDB Only

Work directly inside "rolang-server/".

The backend is already connected to MongoDB, but the application currently displays:

"The database is not ready for this release."

This may be caused by the existing PostgreSQL migration or database readiness checks.

I no longer want PostgreSQL support or any PostgreSQL migration process. Simplify the backend and use MongoDB as the only database.

1. Remove PostgreSQL Completely

- Remove PostgreSQL dependencies, drivers, ORM integrations, and connection logic.
- Remove PostgreSQL-specific environment variables and configuration.
- Remove obsolete PostgreSQL migration scripts and database readiness checks.
- Remove Prisma or other PostgreSQL-specific tooling if it is no longer needed.
- Remove any release checks that require PostgreSQL migrations or PostgreSQL schema initialization.
- Clean up obsolete scripts and configuration files.

Do not leave unused PostgreSQL code or dependencies behind.

2. Make MongoDB the Only Database

Use the existing MongoDB connection that is already configured.

- Use "MONGODB_URI" and "MONGODB_DB_NAME".
- Centralize MongoDB connection management.
- Reuse connections safely in Vercel serverless environments.
- Ensure every database-dependent API route uses MongoDB.
- Initialize collections and indexes automatically when necessary.
- Do not require PostgreSQL setup, migrations, or database readiness checks.

3. Fix Database Readiness Error

Investigate why the application displays:

"The database is not ready for this release."

Identify and remove the actual PostgreSQL-related dependency causing this error.

Implement proper MongoDB readiness checks instead:

- Verify MongoDB connectivity.
- Ensure required collections and indexes are available.
- Handle database initialization correctly.
- Do not block the application because of obsolete PostgreSQL migration states.
- Return meaningful errors if MongoDB is genuinely unavailable.

Do not simply bypass the error or force the application to report that the database is ready.

4. Story Data

Keep the existing story APIs and learning features working.

- Store stories, chapters, translations, vocabulary, exercises, and related content in MongoDB.
- Preserve existing data structures and API response compatibility.
- Ensure story retrieval, search, pagination, and chapter ordering work correctly.
- If existing story data is stored in local files or other sources, import it into MongoDB where necessary.
- Avoid duplicate records during initialization.
- Do not silently discard existing story content.

If no data migration is required, do not create an unnecessary PostgreSQL-to-MongoDB migration system.

5. Vercel Compatibility

Ensure the backend works correctly on Vercel:

- Remove PostgreSQL-specific build and deployment requirements.
- Use a compatible Node.js runtime.
- Handle serverless database connections correctly.
- Ensure all API routes initialize MongoDB properly.
- Avoid persistent local filesystem dependencies.
- Add or update a "/health" endpoint to report server and MongoDB connectivity without exposing secrets.

6. Environment Configuration

Update ".env.example" and deployment documentation.

Only retain the required MongoDB configuration:

- "MONGODB_URI"
- "MONGODB_DB_NAME"

Inspect actual environment variable usage before removing anything.

Never print credentials or connection strings in logs.

7. Testing and Cleanup

- Install updated dependencies.
- Remove unused PostgreSQL packages.
- Run the production build.
- Test MongoDB connectivity.
- Test database readiness.
- Test all existing story and chapter APIs.
- Verify that no PostgreSQL references remain in active application code or deployment scripts.
- Verify Vercel compatibility.

Do not claim successful deployment unless it has actually been tested.

Important Rules

- MongoDB must be the only database.
- Do not retain PostgreSQL as a fallback.
- Do not create a new PostgreSQL migration system.
- Do not replace the existing backend framework unnecessarily.
- Do not modify unrelated features.
- Do not delete existing story data.
- Implement actual code changes, not just recommendations.

Final Report

Summarize:

1. PostgreSQL components removed.
2. MongoDB implementation retained or updated.
3. Root cause of the database readiness error.
4. Files modified.
5. Build and API test results.
6. Any remaining manual Vercel configuration.

The final backend must start and serve requests using MongoDB directly, without requiring PostgreSQL, PostgreSQL migrations, or PostgreSQL release readiness checks.