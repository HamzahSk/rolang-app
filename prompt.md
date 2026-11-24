Task: Replace RoLang Backend with GitHub Static Content

Refactor the RoLang Android app to remove its server/API dependency. I want "rolang-server" to become a static content repository hosted on GitHub, containing JSON files and assets that the Android app fetches directly through GitHub Raw URLs.

1. Remove Server Dependency

- Remove backend API, authentication, account, and server synchronization features.
- Remove related WorkManager jobs, network configuration, and unused server-specific code.
- Do not introduce another backend or require server deployment.
- Keep the existing local Room database for learning progress, vocabulary, reviews, and other user data.
- Preserve existing offline functionality and cached content.
- Avoid destructive database migrations or losing existing user data.
- Remove obsolete server settings from the app configuration and update related UI.

2. New GitHub Content Repository

Restructure "rolang-server" into a static content repository:

rolang-server/
├── manifest.json
├── lesson/
│   ├── index.json
│   └── elementary-tagalog-1/
│       ├── index.json
│       ├── 01-introduction.json
│       ├── 02-pronouns.json
│       ├── 03-basic-sentences.json
│       └── assets/
│           ├── ...
│
└── storie/
    └── (implement later)

Use nested folders to organize content into courses, units, and topics where appropriate.

Requirements:

- Every lesson must be stored in its own JSON file.
- Use a manifest/index system to discover available courses and lessons.
- All content and image references must resolve through GitHub Raw URLs.
- Use relative asset paths in JSON and resolve them against the repository's raw content base URL.
- Keep the JSON structure consistent, versioned, and easy to extend.
- Do not implement stories yet.

3. Import Elementary Tagalog 1 EPUB

Locate "elementary tagalog 1.epub" in the project/repository or available source files.

Extract and analyze its contents, including:

- Chapters and lesson sections.
- Explanations and grammar.
- Vocabulary and translations.
- Examples and dialogues.
- Exercises and relevant learning materials.
- Images and illustrations embedded in the EPUB.

Convert the book into structured application lessons rather than simply displaying EPUB pages.

Requirements:

- Organize the material into logical lessons and topics.
- Preserve the original educational content and its learning progression.
- Rewrite the structure into the app's Indonesian → Tagalog learning format.
- Keep Tagalog examples, Indonesian explanations, and translations clearly separated.
- Extract and preserve relevant images in the lesson assets folders.
- Reference extracted images from the corresponding JSON lessons.
- Do not invent missing textbook content or silently omit important material.

4. Adapt Android UI/UX

Redesign the lesson experience around the new textbook-based curriculum.

- Create a course overview showing the course name, description, progress, and lesson list.
- Organize lessons into units and topics.
- Give each lesson a clear learning objective.
- Present explanations, vocabulary, examples, dialogues, and exercises in visually distinct sections.
- Display EPUB illustrations in relevant lesson sections.
- Add pronunciation controls using the existing Filipino TextToSpeech implementation.
- Make the layout comfortable to read on mobile.
- Maintain the existing Material 3 design system.
- Ensure navigation and learning progress remain intuitive.

Do not just change the data source while leaving an unsuitable lesson UI.

5. Content Loading

- Fetch the manifest and lesson JSON files directly from GitHub Raw.
- Load cached content immediately when available.
- Refresh remote content when internet is available.
- Handle loading, empty, invalid JSON, HTTP errors, and missing assets gracefully.
- Never crash when GitHub is unavailable.
- Preserve previously downloaded lessons when updates fail.
- Avoid unnecessary downloads using HTTP caching or content hashes.
- Allow lessons to be saved for offline access.

6. Important Constraints

- Implement lessons only for now. Stories will be handled in a separate task later.
- Do not generate placeholder lessons or fake textbook content.
- Do not unnecessarily rewrite unrelated app features.
- Keep the project architecture clean and maintainable.
- Update tests, documentation, and GitHub Actions validation for the new static content structure.

7. Verification

Before finishing:

- Verify every generated JSON file.
- Verify all image paths and references.
- Verify the complete lesson catalog can be loaded from GitHub.
- Test offline access and cache fallback.
- Test malformed JSON and unavailable GitHub scenarios.
- Run unit tests, lint, and Android debug build.

Implement the complete migration, not just a proposal. Inspect the existing codebase and EPUB first, then make the necessary changes.