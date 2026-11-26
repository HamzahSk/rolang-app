Task: Build a Global Vocabulary System for RoLang

Currently, RoLang extracts and displays word translations directly from individual stories. This approach causes duplicated vocabulary entries and makes vocabulary difficult to manage across multiple stories.

Replace this system with a centralized, reusable Global Vocabulary Database hosted in "rolang-server".

1. Create a Global Vocabulary Repository

Create a new directory:

"rolang-server/vocabulary/"

All vocabulary data must be stored here as JSON files.

Requirements:

- Organize vocabulary into JSON files containing approximately 100 unique words each.
- Use consistent IDs for every vocabulary entry.
- Create an "index.json" to register all vocabulary files and their metadata.
- Support adding new vocabulary files without replacing existing ones.
- Avoid duplicate entries across different JSON files.
- Keep the structure scalable for thousands of words.

Suggested structure:

vocabulary/
├── index.json
├── vocabulary-001.json
├── vocabulary-002.json
├── vocabulary-003.json
└── ...

2. Vocabulary Data Structure

Each vocabulary entry should support:

- Unique ID.
- Tagalog word or phrase.
- Indonesian meaning.
- Alternative meanings or additional senses when applicable.
- Part of speech (noun, verb, adjective, etc.).
- Pronunciation.
- Relevant example sentences in Tagalog.
- Indonesian translations of examples.
- References to stories where the word appears.

Example:

{
  "id": "usa",
  "word": "usa",
  "language": "tl",
  "meanings": [
    {
      "language": "id",
      "value": "rusa"
    }
  ],
  "partOfSpeech": "noun",
  "pronunciation": "usa",
  "examples": [
    {
      "tagalog": "Araw-araw na hinahangaan ng usa ang malapad niyang dibdib.",
      "indonesian": "Setiap hari rusa mengagumi dadanya yang lebar."
    }
  ],
  "storyRefs": [
    "story-book-1-001"
  ]
}

Adapt the schema to the existing project conventions if necessary.

Important:

- One word must have one canonical global entry.
- If a word appears in multiple stories, add those story references to the same entry.
- Do not duplicate the vocabulary entry just because its context changes.
- Support multiple meanings when a word genuinely has different meanings.
- Preserve context-specific example sentences and translations.

3. Migrate Existing Story Vocabulary

Inspect all existing vocabulary embedded in stories from Book 1, Book 2, and other available collections.

- Extract all existing vocabulary entries.
- Normalize and deduplicate them.
- Merge entries referring to the same Tagalog word.
- Preserve useful Indonesian meanings, pronunciation, examples, and story references.
- Move the vocabulary into the new global JSON database.
- Replace embedded vocabulary data with references to global vocabulary IDs.
- Do not lose existing vocabulary information during migration.

Do not arbitrarily merge different words that happen to have similar spellings or meanings.

4. Update Story Integration

Stories should no longer own separate copies of vocabulary.

Instead, each story should contain references such as:

{
  "vocabularyIds": [
    "usa",
    "araw",
    "bahay"
  ]
}

When opening a story, the Android app should resolve these IDs against the global vocabulary database.

This allows:

- Multiple stories to share vocabulary.
- Vocabulary updates to apply globally.
- New stories to reuse existing words.
- Vocabulary learning progress to remain centralized.

5. Redesign the Word Translation UI

Inspect the existing word-detail interface shown when users tap a Tagalog word.

Update it to retrieve data from the global vocabulary database.

Preserve and improve these features:

- Tagalog word display.
- Indonesian meaning.
- Part of speech.
- Pronunciation and TTS.
- Multiple meanings when available.
- Example sentences and translations.
- Story contexts where the word appears.
- Global learning status (New, Learning, Almost Mastered, Mastered).
- Save vocabulary and review functionality.

Important:

- Learning status must belong to the global vocabulary entry, not an individual story.
- If a word appears in another story, its existing learning status must remain the same.
- Keep user progress locally stored and preserve existing progress during migration.
- Handle vocabulary that is not yet available in the global database gracefully.

6. Global Vocabulary Management and Loading

Implement:

- Vocabulary index loading from GitHub Raw.
- Lazy loading of JSON files.
- Local caching and offline access.
- Search by Tagalog word and Indonesian meaning.
- Fast lookup by vocabulary ID.
- Duplicate detection when adding new entries.
- Safe index updates when new vocabulary files are published.

Avoid downloading every vocabulary JSON file whenever a story is opened.

Use the existing networking, caching, and repository architecture where possible.

7. GitHub Repository Rules

All server vocabulary changes MUST be made in:

"rolang-server" → branch "data"

- Create the "vocabulary/" directory on "data".
- Commit and push all vocabulary files and updated story indexes to "origin/data".
- Ensure GitHub Raw URLs point to the "data" branch.
- Never push content changes to "main".
- Never force-push or reset "main".

Keep Android app changes separate from server content changes.

8. Validation

Before finishing:

- Verify that all existing story vocabulary has been migrated.
- Check for duplicate vocabulary IDs and words.
- Verify all story vocabulary references.
- Ensure words shared by multiple stories resolve to the same global entry.
- Test word lookup, search, TTS, learning status, and offline access.
- Confirm that existing bookmarks and learning progress remain intact.
- Test the actual GitHub Raw loading flow.

Do not merely create the JSON files. Integrate the global vocabulary system into the Android app.

Final Report

Report:

- Total unique vocabulary entries migrated.
- Number of vocabulary JSON files created.
- Number of duplicate entries merged.
- Number of stories connected to global vocabulary.
- Any missing or unresolved vocabulary references.
- Android files changed.
- Test and build results.
- Server commit hash and confirmation of push to "origin/data".
- Android commit and push status.

Implement everything directly without modifying unrelated features.