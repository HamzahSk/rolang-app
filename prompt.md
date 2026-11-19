Task: Migrate Ro-Lang Server from PostgreSQL to MongoDB Atlas

Migrate the existing "rolang-server/" backend from PostgreSQL to MongoDB Atlas because the current PostgreSQL setup keeps producing HTTP 500 errors on Vercel despite multiple configuration attempts.

The goal is to have a fully functional MongoDB-backed backend that deploys reliably on Vercel.

1. Inspect Existing Architecture

Before making changes:

- Inspect the entire "rolang-server/" project.
- Identify the current framework, database client, ORM, API routes, models, and data relationships.
- Understand how stories, chapters, vocabulary, translations, exercises, and other learning content are currently structured.
- Inspect existing PostgreSQL migrations and schemas to understand the data that must be preserved.

Do not blindly replace the existing architecture.

2. Migrate Database to MongoDB

Replace PostgreSQL with MongoDB Atlas.

- Use the official MongoDB Node.js driver or an appropriate existing MongoDB-compatible solution.
- Remove PostgreSQL-specific database dependencies and configuration when no longer needed.
- Implement a centralized MongoDB connection manager.
- Reuse database connections across Vercel serverless function invocations whenever possible.
- Avoid creating unnecessary connections for every request.
- Use environment variables for MongoDB configuration.
- Support MongoDB Atlas connection strings.
- Implement appropriate connection error handling and timeouts.

Use a single consistent database configuration throughout the backend.

3. Redesign Data Models

Convert existing relational models into appropriate MongoDB document structures.

Preserve all existing data and relationships, including:

- Stories
- Chapters
- Story metadata
- Translations
- Vocabulary
- Exercises
- Learning content
- Other existing entities

Choose between embedded documents and references based on data size, access patterns, and scalability.

Avoid excessively large documents and respect MongoDB document size limitations.

Create appropriate indexes for frequently queried fields.

4. Preserve Existing API Compatibility

Keep the existing API endpoints and response formats compatible with "rolang-app".

Ensure:

- Story catalog retrieval works.
- Story details can be retrieved.
- Chapters can be retrieved in the correct order.
- Story content remains complete.
- Pagination works.
- Search and filtering continue working.
- Existing API consumers do not break unnecessarily.

Do not redesign unrelated application functionality.

5. Data Migration

Implement a migration/import script that transfers existing PostgreSQL data into MongoDB.

Requirements:

- Preserve existing story content.
- Preserve chapter ordering.
- Preserve translations, vocabulary, exercises, and relationships.
- Maintain stable identifiers wherever possible.
- Avoid duplicate records when migration is executed multiple times.
- Validate imported document counts and relationships.
- Provide migration logs without exposing credentials.

Do not delete PostgreSQL configuration or existing migration files until the MongoDB migration has been successfully verified.

6. Vercel Compatibility

Ensure the MongoDB implementation works correctly in Vercel serverless environments.

- Use supported Node.js runtime features.
- Configure database connection reuse.
- Avoid persistent filesystem dependencies.
- Handle concurrent requests safely.
- Ensure all API routes use the centralized MongoDB connection.
- Avoid database initialization failures during module imports where possible.

Add a "/health" endpoint that checks server availability and MongoDB connectivity without exposing sensitive information.

7. Environment Configuration

Inspect the actual environment variable names used throughout the project.

Use a centralized configuration, preferably:

"MONGODB_URI"
"MONGODB_DB_NAME"

Update ".env.example" with the required variable names and descriptions.

Do not hardcode credentials or expose connection strings in logs.

Do not assume environment variables are automatically available without verifying their usage.

8. Testing

Before considering the migration complete:

- Install and update dependencies.
- Run the production build.
- Test MongoDB connectivity.
- Test all API endpoints.
- Test story and chapter retrieval.
- Test pagination and search.
- Verify database indexes.
- Test error handling when MongoDB is unavailable.
- Verify Vercel deployment compatibility.

If MongoDB Atlas credentials are unavailable, complete the implementation and provide the exact configuration steps needed for testing.

9. Cleanup

After successful migration and verification:

- Remove unused PostgreSQL dependencies.
- Remove obsolete PostgreSQL connection code.
- Update documentation.
- Update deployment instructions.
- Keep ".env.example" synchronized with the actual implementation.

Do not remove existing PostgreSQL data or migrations before the migration has been verified.

Important Rules

- Work directly inside "rolang-server/".
- Do not modify "rolang-app" unless API compatibility requires it.
- Do not merely provide suggestions; implement the actual migration.
- Do not create mock database implementations.
- Do not silently discard existing story data.
- Do not unnecessarily rewrite unrelated backend functionality.
- Do not claim deployment success without actual verification.

Final Report

Provide:

1. Previous PostgreSQL architecture and identified problems.
2. MongoDB architecture implemented.
3. Files modified.
4. Data migration results.
5. Required MongoDB Atlas environment variables.
6. Build and test results.
7. Remaining manual configuration or deployment steps.

The final result must be a fully MongoDB-backed Ro-Lang server that works reliably on Vercel.