Task: Migrate Ro-Lang Stories to Online Server with Offline Support

Read and inspect both repositories before making changes:

- Android app: current workspace ("rolang-app").
- Backend server: "rolang-server/".

Main Goal

Transform Ro-Lang into an online-first language-learning application. All story content should be managed and stored on the backend server and its database instead of being bundled directly inside the Android application.

The app must still support offline reading through downloaded content.

1. Backend & Database

Inspect the existing backend architecture, database, and API implementation before modifying anything.

- Move existing story data into the server database.
- Create or update database models and migrations as needed.
- Implement APIs to retrieve story lists, story details, chapters, and story content.
- Support pagination and efficient data retrieval.
- Provide story metadata such as title, description, language, difficulty, cover, chapters, and other existing fields.
- Avoid duplicate records when importing existing stories.
- Preserve existing story content and structure.
- Implement appropriate validation and error handling.

Do not unnecessarily replace the existing backend stack or database.

2. Android App Integration

Connect the Android application to "rolang-server".

- Replace local story loading with server API integration.
- Fetch story catalogs and content from the backend.
- Implement loading, empty, and error states.
- Add retry handling and appropriate network timeouts.
- Keep the existing UI/UX and navigation wherever possible.
- Ensure API configuration is centralized and easy to change.

3. Automatic Offline Saving

Implement automatic local caching:

- Whenever a user opens a story or chapter, automatically save its content locally.
- Save downloaded content in a persistent local database or storage.
- Avoid unnecessary downloads when content is already cached and up to date.
- Use versioning or updated timestamps to detect changed content.
- Preserve downloaded content even after the app is closed or restarted.
- If the device is offline, automatically load previously cached content.

4. Manual Download Feature

Add a download option to the story interface.

Users should be able to:

- Download an entire story, including all its chapters.
- View download progress.
- Cancel an active download.
- See which stories are already downloaded.
- Remove downloaded content to free up storage.
- Access downloaded stories without an internet connection.

The download process must handle interruptions and resume or retry safely when possible.

5. Online and Offline Synchronization

Implement a reliable content synchronization strategy:

- Online: retrieve the latest available story data from the server.
- Offline: use locally cached or downloaded data.
- Automatically update outdated cached content when appropriate.
- Never delete downloaded content just because a network request fails.
- Avoid blocking the reading experience while background synchronization occurs.

6. Architecture & Data Integrity

- Separate remote data sources from local data sources.
- Implement a repository layer to coordinate server and local storage.
- Use the existing Android architecture and libraries where possible.
- Handle concurrent requests and duplicate downloads safely.
- Ensure database migrations preserve existing user data.
- Do not hardcode story content or server responses in the application.

7. Migration

Inspect all existing story assets and data in "rolang-app".

Create a reliable migration/import process to transfer the existing stories into "rolang-server" and its database.

Verify that:

- All existing stories are imported.
- Chapter ordering is preserved.
- Text, translations, vocabulary, exercises, and other related content remain intact.
- Existing story identifiers and relationships are handled consistently.

Do not remove the original local story assets until the server migration has been verified and the application can retrieve the migrated content successfully.

8. Testing

Test both repositories.

Verify:

- Story catalog retrieval.
- Story detail and chapter retrieval.
- Automatic saving when opening a story.
- Manual story downloads.
- Offline reading.
- Cache updates after server content changes.
- App restart and persistent storage.
- Network failures and interrupted downloads.

Run relevant backend tests and Android unit tests, lint, and build checks.

Important Instructions

- Work directly in both repositories.
- Do not merely provide implementation suggestions; implement the actual changes.
- Inspect existing code and documentation before deciding the architecture.
- Do not rewrite unrelated features or unnecessarily redesign the UI.
- Keep the Android application and backend API compatible.
- Preserve all existing story content and learning features.
- Update API documentation and project documentation where necessary.
- Commit-ready implementation, without placeholders or unfinished mock APIs.

The final result should be an online-first Ro-Lang application where stories are served by the backend, automatically cached when opened, and available offline through manual downloads.