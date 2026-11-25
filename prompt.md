Task: Build the RoLang Stories Section

Use the EPUB file:

"English Short Stories for Intermediate Learners 100 English Short Stories.epub"

Create a complete Stories section in RoLang, using the book to understand its learning level, themes, vocabulary, and story structure.

1. Inspect and Extract the EPUB

- Open and inspect the entire EPUB.
- Analyze all 100 stories, their themes, difficulty, vocabulary, and structure.
- Extract relevant illustrations and images if available.
- Organize the story metadata and learning topics.
- Do not reproduce or translate the book's complete copyrighted stories. Use it only as a reference to create original learning content.

2. Create Original Stories

- Create original short stories inspired by broad themes and learning objectives found in the book.
- Do not copy the original wording, characters, distinctive plots, or dialogue.
- Write each story in two versions:
  - Tagalog: Natural and grammatically correct Tagalog.
  - Indonesian: A complete, natural Indonesian translation of the original Tagalog story.
- Keep both versions equivalent in meaning, events, and dialogue.
- Use clear language suitable for Indonesian beginners learning Tagalog.
- Give every story a title, description, difficulty level, and category.
- Ensure stories are complete, not shortened summaries or unfinished fragments.

3. Learning Features

Each story should support:

- Tagalog and Indonesian reading modes.
- Paragraph-by-paragraph navigation.
- Vocabulary lists with Indonesian meanings.
- Important expressions and grammar notes.
- Reading comprehension questions.
- Interactive exercises where supported by the existing architecture.
- Audio/TTS support using the existing Android implementation.
- Relevant illustrations when available or appropriate.

4. Android Stories UI

Build a dedicated Stories section in the Android app.

- Add a story library with categories and difficulty levels.
- Provide a clean story-reading interface.
- Allow switching between Tagalog and Indonesian.
- Add vocabulary and learning notes.
- Support bookmarking and reading progress.
- Follow the existing Material 3 design system.
- Preserve existing lesson functionality and user progress.

5. Repository and Deployment

IMPORTANT: All content changes must go to the "data" branch of "rolang-server".

- Inspect the existing "storie/" directory and follow its naming conventions and JSON schema.
- Put all new story files in the appropriate "storie/" directories.
- Update story indexes/manifests so the Android app can discover every story.
- Ensure GitHub Raw URLs use the "data" branch.
- Do not push content changes to "main".
- Before committing, explicitly verify the current branch is "data".
- Commit and push all intended changes to "origin/data".
- Never force-push or reset "main".

6. Validation

- Validate all story JSON files.
- Check Tagalog and Indonesian versions for completeness and consistency.
- Verify vocabulary, exercises, and image references.
- Test story discovery and loading through actual GitHub Raw URLs.
- Test the Android Stories interface.
- Run relevant unit tests, lint, and debug build.

Implement everything directly.

At completion, report:

- Number of original stories created.
- Categories and difficulty levels.
- Number of images extracted or added.
- Android files changed.
- Test and build results.
- Git commit hash and confirmation that changes were pushed to "origin/data".