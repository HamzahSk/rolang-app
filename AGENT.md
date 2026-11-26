# Working on Ro-Lang

## Project overview

Ro-Lang (`com.rolang.app`) is an Indonesian → Tagalog Android learning app built
with Kotlin, Jetpack Compose, and Material 3. It supports Android 8.0 (API 26+).
Room schema version 6 stores local learning data and validated content snapshots;
DataStore stores preferences. There is no backend, account, authentication, or
sync service. OkHttp caches static GitHub Raw content, and Android TextToSpeech
provides Filipino pronunciation using `fil-PH`.

`rolang-server/` is a separate static-content checkout, not an application server.
It contains the manifest, SHA-256 indexes, course/lesson JSON, source assets,
importers, and content validator. Inspect its tools and indexes before changing
content; update hashes from the leaves up when content changes.

## Key directories and responsibilities

- `app/src/main/java/com/rolang/app/data/`: Room entities, DAOs, migrations,
  learning repositories, DataStore preferences, corpus parsing, and offline content.
- `app/src/main/java/com/rolang/app/network/`: GitHub Raw HTTP/cache client,
  content validation, and application service wiring.
- `app/src/main/java/com/rolang/app/ui/`: Compose screens and Material 3 UI.
- `app/src/main/java/com/rolang/app/speech/`: lifecycle-owned Filipino TTS.
- `app/src/main/assets/corpus.json`: bundled stories, dictionary, and legacy
  fixtures; normalize and validate with `tools/build_corpus.py`.
- `app/src/test/`, `app/src/sharedTest/`: JVM/Robolectric tests and shared
  learning-flow tests; shared tests also compile into the Android test APK.
- `app/schemas/`: exported Room schemas; preserve upgrade compatibility.
- `rolang-server/`: static content and Python import/validation pipeline; manage
  changes independently from this Android repository.
- `.github/workflows/`: Android formatting, tests, lint, ABI APK builds, artifact
  uploads, and Codex automation.

## Build, formatting, and verification

Use JDK 17, Android SDK `platforms;android-35`, and Build Tools `35.0.0`.
Set `ANDROID_HOME` or an untracked `local.properties` with `sdk.dir`.
Use the checked-in Gradle wrapper. Plugin versions are pinned directly in the
root `build.gradle.kts`; there is currently no version catalog.

Run from the repository root with the static-content checkout available:

```sh
python3 tools/build_corpus.py
python3 rolang-server/tools/validate_content.py rolang-server
./gradlew spotlessApply
./gradlew spotlessCheck
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest
```

The corpus command rewrites normalized corpus files, so inspect its diff.
Spotless uses pinned ktfmt Kotlin style for all `src/**/*.kt` sources and Gradle
Kotlin scripts, with whitespace/newline checks for repository metadata. It skips
build/generated directories and the separate static-content checkout.
`assembleDebugAndroidTest` builds the test APK; it does not run device tests.
The Android workflow checks formatting and offers optional `spotlessApply` on
manual dispatch; applying there only changes that runner's workspace.

## Guardrails and conventions

- Keep Room at schema version 6 with explicit non-destructive migrations.
  Preserve the legacy `PendingChange` and `SyncState` tables for safe upgrades;
  do not introduce destructive fallback or restore a sync service.
- Render cached content first. Never replace cached/last-readable content with
  failed, malformed, or unverified updates. Preserve SHA-256 index/content
  validation, safe relative paths, and referenced-image offline persistence.
- Keep Indonesian explanations separate from source text and Tagalog examples.
  Preserve `id`/`fil` localization and the existing lifecycle-owned `fil-PH` TTS.
- Preserve ABI splits for `arm64-v8a`, `armeabi-v7a`, `x86`, and `x86_64`.
  There is no universal APK; retain wildcard artifact uploads for all splits.
- Never commit secrets, `keystore.properties`, or `keystore.jks`. Release signing
  is optional locally and uses CI secrets when available.
- Keep changes focused. Run content validation for content edits and the full
  commands above before handing off app changes; report any environment blockers.
