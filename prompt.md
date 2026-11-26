Task: Resynchronize and Reorganize RoLang Stories

The Stories content from Book 1 and Book 2 has already been added to "rolang-server", but the Android app's Stories section is not properly synchronized with the server.

Previously, adding Book 2 accidentally replaced Book 1. This MUST NOT happen again.

1. Server as the Source of Truth

Inspect the current Stories content in "rolang-server" on the "data" branch.

- Audit all existing story files, indexes, and manifests.
- Identify stories belonging to Book 1 and Book 2.
- Treat the current server content as the source of truth.
- Synchronize the Android app with the complete server collection.
- Preserve all existing stories from both books.
- Never replace the entire collection when adding or updating a book.
- Do not regenerate, rewrite, or delete existing story content unnecessarily.
- Detect duplicate IDs and filename conflicts without accidentally deleting valid stories.

2. Fix Story Synchronization

Inspect the complete content-loading flow:

- GitHub Raw URLs and branch configuration.
- Story index and manifest loading.
- Remote story discovery.
- JSON parsing and data mapping.
- Local caching and offline storage.
- Update detection and cache invalidation.
- Course/book grouping and story ordering.

Make sure the app can discover and load every story from both books.

Important requirements:

- Remote updates must merge or reconcile with existing local data instead of replacing unrelated stories.
- Adding Book 2 must never remove Book 1.
- Updating Book 1 must never remove Book 2.
- Preserve bookmarks, reading progress, and other user data.
- Handle missing, deleted, and updated remote entries safely.
- Avoid stale indexes and outdated cached content.
- Ensure offline stories remain accessible.

3. Reorganize Story Categories

The current category structure should be redesigned.

Categories must represent broad themes, NOT individual story titles.

For example:

- Adventure
- Animals
- Daily Life
- Family and Friendship
- Nature and Environment
- Folktales and Fables
- History and Culture
- Mystery and Discovery

These are examples only. Determine the final categories based on the actual stories from both books.

Requirements:

- Each category should contain multiple relevant stories whenever possible.
- Stories from Book 1 and Book 2 can belong to the same category.
- A story may have multiple relevant tags if the existing architecture supports it.
- Keep book/source information separate from categories.
- Preserve each story's original title and numbering.
- Do not create a separate category for every story.
- Do not use Book 1 and Book 2 as story categories; display them as source/book filters instead.

4. Improve Stories UI

Update the Stories library to support:

- Broad thematic categories.
- Book/source filters (Book 1 and Book 2).
- Difficulty filters.
- Search by story title.
- Story counts for each category.
- Clear story grouping and navigation.
- A clean Material 3 layout.

Avoid excessive category cards or unnecessary nested navigation.

5. Data Integrity and Validation

Before finishing:

- Compare server and Android story inventories.
- Verify that every server story is discoverable in the app.
- Verify that both books are present.
- Check for missing, duplicated, or incorrectly grouped stories.
- Validate all JSON files and image references.
- Test remote loading, caching, offline access, and update synchronization.
- Confirm that existing bookmarks and reading progress remain intact.

Do not simply report the number of stories in the local database. Test actual remote discovery and loading.

6. Git Rules — CRITICAL

For "rolang-server":

- Work exclusively on the "data" branch.
- Commit and push all intended server changes to "origin/data".
- Never push story content to "main".
- Never force-push or reset "main".
- Verify the remote branch after pushing.

Keep Android app changes separate and report their commit status.

Final Report

Provide:

- Total stories found on the server.
- Number of stories from Book 1 and Book 2.
- Final category list and story counts per category.
- Any synchronization bugs found and fixed.
- Confirmation that no existing stories were lost.
- Test and build results.
- Server commit hash and confirmation of push to "origin/data".
- Android commit and push status.

Implement the fixes directly. Do not just provide recommendations.