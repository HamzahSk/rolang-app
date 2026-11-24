# Ro-Lang for Android

Ro-Lang is an Indonesian → Tagalog learning app built with Kotlin, Jetpack
Compose, Material 3, Room, DataStore, OkHttp, and Android TextToSpeech. Android
8.0 (API 26) or newer is supported.

## Content architecture

The app has no backend, authentication, account, or synchronization service.
Learning progress, vocabulary, review schedules, reading positions, and exercise
results remain only in the existing local Room database. Room stays at schema
version 6 with explicit non-destructive migrations; legacy sync tables remain in
the schema solely so existing installations can open without losing data.

Lesson content is fetched directly from GitHub Raw:

```text
https://raw.githubusercontent.com/HamzahSk/rolang-server/data/
```

The app discovers `manifest.json`, `lesson/index.json`, a course index, and then
individual lesson JSON files. The indexes carry SHA-256 hashes. OkHttp supplies an
HTTP disk cache, Room keeps validated lesson snapshots, and failed/malformed
updates never replace the last readable copy. Cached content is rendered before a
refresh. Saving a lesson for offline use also persists all referenced images.

The adjacent `rolang-server/` checkout is now a static repository containing the
complete 24-lesson *Elementary Tagalog 1* curriculum, eight unit overviews, and
152 extracted EPUB images. Stories are intentionally not part of this migration;
the app's existing bundled stories continue to work locally.

## Learning experience

- Course overview with description, progress, eight units, and 24 ordered lessons.
- Typed, visually distinct overview, vocabulary, dialogue, activity, grammar,
  practice, table, and illustration sections.
- Indonesian explanations stored separately from the auditable English source and
  Tagalog examples/dialogues.
- Filipino pronunciation controls throughout textbook content using the existing
  lifecycle-owned `fil-PH` TextToSpeech implementation.
- Local vocabulary, spaced repetition, reviews, story reading, and diagnostics.

## Build

Use JDK 17, Android SDK platform 35, and Build Tools 35.0.0:

```sh
./gradlew :app:assembleDebug
./gradlew :app:installDebug
```

No API keys, server configuration, account credentials, or deployment are needed.

## Verification

```sh
python3 tools/build_corpus.py
python3 rolang-server/tools/validate_content.py rolang-server
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest
```

The static-content validator parses every JSON file, checks manifest/index hashes,
requires all 24 textbook lessons, and verifies every relative image path. Android
tests cover Room migrations/local learning plus GitHub catalog parsing, content
hash validation, malformed JSON, HTTP failure, and cached fallback.

The GitHub Actions Android workflow runs the same content checks, unit tests, lint,
and debug builds. The static repository has its own validation workflow.

## Project structure

- `app/src/main/java/com/rolang/app/data`: Room, local learning, static catalog,
  cached lesson parsing, and offline asset storage.
- `app/src/main/java/com/rolang/app/network`: GitHub Raw HTTP/cache client and app
  service wiring.
- `app/src/main/java/com/rolang/app/ui`: Material 3 course, lesson, story,
  vocabulary, review, and profile screens.
- `app/src/main/assets/corpus.json`: bundled stories/dictionary and legacy local
  fixtures; its former lessons are hidden in favor of the textbook course.
- `rolang-server/`: static manifest, indexes, lesson JSON, EPUB assets, importer,
  validator, and content CI.

Uninstalling or clearing app data removes local learning data. Ordinary upgrades
and this backend-removal migration do not.
