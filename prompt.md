Upgrade the UI and UX of the **Ruang baca** (story reader) screen and its vocabulary bottom sheet in the Ro-Lang Android app (Kotlin, Jetpack Compose, Material 3) so the reading flow is cleaner, calmer, better structured, and much easier to scan and read without visual clutter.

### Context & Current Pain Points
In the current `Ruang baca` screen and vocabulary sheet:
1. **Top & Control Clutter**: The progress indicator ("Paragraf X dari 7"), helper hint ("Ketuk kata Tagalog untuk melihat artinya"), full-story audio/reset controls, offline save action ("Simpan untuk luring"), mastery legend pills ("Baru", "Dipelajari", "Hampir dikuasai", "Dikuasai"), and "Tampilkan terjemahan" button stack vertically with inconsistent hierarchy and spacing, pushing the actual story paragraphs down too far.
2. **Paragraph & Highlight Readability**: Multi-line paragraphs use dense light-blue word/phrase highlight boxes that look blocky/fragmented across wrapped lines, paired with a detached speaker icon on the right that feels loosely aligned to the paragraph block.
3. **Vocabulary Bottom Sheet Density**: In the word sheet (e.g., "maraming"), sections ("Makna", "Pelafalan dan konteks", "Contoh", "Status belajar") and status chips ("Dikuasai" wrapping onto a second line awkwardly) feel cramped and flat, with long explanatory text competing with the primary word, meaning, and example sentence.

### Design & UX Goals
- **Reading-First Hierarchy**: Prioritize comfortable reading typography, generous line height, and clear visual grouping so the story content takes center stage while secondary controls stay neat and unobtrusive.
- **Cohesive Material 3 Styling**: Use refined Material 3 surfaces, subtle tonal containers, clean borders, consistent horizontal/vertical spacing, and clear typographic hierarchy (title, metadata, body copy, caption/helper text). Keep existing Indonesian labels/copy intact unless a more compact layout is needed, without losing any functionality.
- **Clean Interactive Controls**:
  - Group reader controls logically (e.g., a compact top control bar/card for reading mode `Tagalog` / `Indonesia`, translation toggle, and story audio/reset; and a clean offline/status row without awkward wrapping).
  - Fix the mastery status legend chips so they align cleanly (e.g., single scrollable row or balanced 2x2 grid/flow with consistent chip sizing—no orphaned single chip wrapping awkwardly).
  - Refine paragraph blocks so text wraps naturally with clean line spacing, clear paragraph separation, subtle/harmonious highlight styling, and a well-aligned per-paragraph TTS button.
  - Upgrade the vocabulary bottom sheet with clear section rhythm, a clean horizontal/wrap chip group for mastery statuses, distinct styling for the Tagalog example and Indonesian translation + inline audio button, and a prominent bottom primary action ("Simpan kata").

### Technical Constraints & Guardrails
- Preserve all existing state, Room persistence, reading progress tracking, bookmarks, offline saving, `fil-PH` TextToSpeech playback/reset, reading mode switching, translation toggle, and word tap/save/status update behaviors.
- Do not change Room schema, network/cache logic, or unrelated screens/features.
- Ensure layouts adapt cleanly on small/standard phone screens (such as 360dp width) without text clipping, overlapping, or awkward single-item wrapping.
- Keep existing unit tests, lint checks, and debug build passing (`./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug`).

### Expected Output
1. Inspect the `Ruang baca` screen, story paragraph/control components, and vocabulary bottom sheet under `app/src/main/java/com/rolang/app/ui`.
2. Refactor the layout, spacing, typography, color/surface hierarchy, and chip/control alignment to make the screen clean and effortless to read.
3. Summarize the UI/UX improvements made and list the modified files.
