Task: Build RoLang Stories from 4000 Essential English Words - Book 2

Use this EPUB as the correct source:

"4000 Essential English Words - Book 2.epub"

IMPORTANT: This replaces the previously used English Short Stories EPUB. Do not use the previous book or its 100-story inventory.

1. Inspect the EPUB

- Open and inspect the entire EPUB.
- Identify every actual story, fable, or narrative passage in the book.
- There should be approximately 30 stories, but determine the exact number from the EPUB itself.
- Extract each story's original title, order, characters, setting, theme, and main events.
- Distinguish stories from vocabulary lists, exercises, introductions, and other textbook sections.
- Inspect available illustrations and extract relevant images.

2. Create Stories Content

Create one RoLang story entry for every identified story.

For each entry:

- Preserve the original title and story order.
- Write a concise but informative summary based on the actual source.
- Create a readable Tagalog retelling that preserves the original story's main events, characters, and meaning.
- Provide a complete Indonesian version of that retelling.
- Keep both language versions equivalent in meaning and completeness.
- Use natural, grammatically correct Tagalog suitable for learners.
- Do not invent unrelated plots or replace the source stories with generic content.
- Do not reproduce or translate the complete copyrighted story text.

3. Learning Materials

Each story should include:

- Tagalog reading content.
- Indonesian translation.
- Important Tagalog vocabulary with Indonesian meanings.
- Useful expressions from the story.
- Relevant grammar notes.
- Reading comprehension questions based on the actual story.
- Answers stored separately from the questions.

Avoid generic or duplicated learning materials.

4. Images and EPUB Assets

- Extract relevant illustrations from the EPUB when available.
- Save images in the existing "storie/" content directory.
- Associate each image with its corresponding story.
- Preserve image quality while optimizing file sizes.
- Do not reuse unrelated illustrations as if they came from the source.

5. Android Stories Integration

- Inspect the existing Stories implementation.
- Replace the incorrect previous collection with the correct Book 1 collection, while preserving unrelated stories if they exist.
- Display all identified stories in their original order.
- Support Tagalog and Indonesian reading modes.
- Include vocabulary, grammar notes, exercises, bookmarks, reading progress, TTS, and offline caching.
- Follow the existing Material 3 design system.
- Do not break existing lesson functionality or user progress.

6. Validation

- Compare every generated entry against the EPUB inventory.
- Ensure no source story is missing or duplicated.
- Verify titles, story order, characters, themes, and summaries.
- Validate all JSON files and image references.
- Test actual GitHub Raw loading for every story.
- Test the Android Stories library and reader.

7. Git Deployment — CRITICAL

All "rolang-server" content changes MUST be pushed to "origin/data".

- Explicitly switch to the "data" branch before modifying content.
- Never commit or push story content to "main".
- Do not force-push or reset "main".
- Verify the remote "data" branch after pushing.
- Keep the Android app changes separate from the server content changes.

Implement everything directly, not just a plan.

Final Report

Report:

- Exact number of stories found in the EPUB.
- Complete numbered list of extracted story titles.
- Number of story entries created.
- Number of images extracted.
- Files changed.
- Validation, tests, and build results.
- Server commit hash and confirmation of push to "origin/data".
- Any Android changes that remain unpushed.