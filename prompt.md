Task: Rebuild RoLang Static Content & Migrate Android App

Start from scratch by inspecting the current repositories and workspace. This is a new session, so do not assume previous session context or existing uncommitted changes.

1. Analyze and Extract EPUB

Locate "elementary tagalog 1.epub" and fully inspect its contents.

Extract:

- Chapters and learning sections.
- Grammar explanations.
- Vocabulary and Indonesian translations.
- Examples and dialogues.
- Exercises and practice materials.
- All relevant embedded images and illustrations.

Convert the textbook into structured Indonesian → Tagalog lessons. Preserve its original educational content, learning progression, and relevant illustrations. Do not invent or silently omit content.

2. RoLang Server: Static GitHub Repository

Use the existing "rolang-server" GitHub repository, but publish all static content exclusively on the "data" branch.

Target structure:

rolang-server/
├── manifest.json
├── lesson/
│   ├── index.json
│   └── elementary-tagalog-1/
│       ├── index.json
│       ├── 01-introduction.json
│       ├── 02-pronouns.json
│       └── assets/
└── storie/
    └── (reserved for future content)

Requirements:

- Separate each lesson into its own JSON file.
- Organize content using nested folders.
- Extract EPUB images into the appropriate asset folders.
- Use relative asset paths.
- Create a manifest and indexes for content discovery.
- Validate all JSON files and image references.
- Keep static content independent of the old backend.

Important: All content must be committed and pushed to the "data" branch. Do not publish content to "main".

Use this Raw URL as the content base:

"https://raw.githubusercontent.com/HamzahSk/rolang-server/data/"

3. Android App Migration

Inspect the current RoLang Android app and remove its backend dependency.

- Replace API content loading with direct GitHub Raw JSON loading.
- Remove authentication, account synchronization, and backend-related WorkManager tasks.
- Keep Room for local learning progress, vocabulary, reviews, and preferences.
- Preserve existing user data and avoid destructive migrations.
- Implement caching, content hashes, offline access, and graceful network failure handling.
- Update all content URLs to use the "data" branch.

4. Lesson UI/UX

Redesign the learning experience around the extracted textbook.

Include:

- Course overview and progress.
- Units and ordered lessons.
- Learning objectives.
- Grammar explanations.
- Vocabulary with translations.
- Examples and dialogues.
- Relevant EPUB illustrations.
- Exercises and practice.
- Filipino TTS pronunciation controls.

Use the existing Material 3 design system and ensure the interface is comfortable on mobile.

5. Stories Deferred

Do not implement Stories yet. Only prepare the "storie/" directory for future content.

6. Verification & Delivery

- Verify all 24 lessons or adjust the count according to the actual EPUB structure.
- Verify every extracted image and JSON reference.
- Verify the manifest and all GitHub Raw URLs on the "data" branch.
- Test online loading, offline caching, and fallback behavior.
- Run unit tests, Android lint, and debug APK build.
- Update documentation and CI validation.

Do not delete legacy backend files unless they are confirmed unnecessary and unrelated to existing functionality.

Do not overwrite unrelated changes.

Actually implement, commit, and push the static content to the "data" branch if repository permissions allow. Never claim a successful push without verifying it.

At completion, report:

- Extracted lesson and image counts.
- Repository structure.
- Branch and commit hash.
- Android files modified.
- Test and build results.
- Any remaining manual steps.