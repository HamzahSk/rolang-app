Upload and populate all learning lessons and stories to the existing online server.

Tasks:

1. Inspect the existing Android app and backend to understand the current lesson and story data structures.
2. Collect all existing lessons, stories, chapters, vocabulary, and exercises from the project.
3. Upload all content to MongoDB through the existing backend APIs or a proper seed script.
4. Ensure every lesson and story has complete content, stable IDs, correct ordering, and valid relationships.
5. Keep the learning configuration as Indonesian ("id") → Tagalog ("tl"), A1 level.
6. Preserve the existing content. Do not replace complete lessons or stories with short placeholders.
7. Make the seed process safe to run multiple times without creating duplicate records or deleting user data.
8. Verify that all content is actually stored in MongoDB and can be retrieved through the production API.
9. Ensure the Android app loads the uploaded content from the server correctly.

Important:

- Work in both "rolang-app/" and "rolang-server/".
- Use MongoDB only. Do not reintroduce PostgreSQL.
- Do not use mock data or hardcoded content as a substitute for server data.
- Preserve existing UI and unrelated features.
- Do not claim completion until the content has been uploaded and verified through actual API requests.

Prioritize completeness and data integrity over simply increasing the number of records.
