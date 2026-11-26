Inspect the repository first (`settings.gradle.kts`, root `build.gradle.kts`, `gradle/libs.versions.toml` if present, `app/build.gradle.kts`, `README.md`, `tools/build_corpus.py`, `rolang-server/tools/validate_content.py`, and `.github/workflows/`), then complete the following two tasks cleanly and verify everything locally:

### 1. Add Spotless (`spotlessCheck` and `spotlessApply`)
- Configure `com.diffplug.spotless` so `./gradlew spotlessCheck` and `./gradlew spotlessApply` work across the project (matching the existing Gradle / version-catalog style in the repo).
- Format Kotlin sources (`src/**/*.kt`), Kotlin Gradle scripts (`*.gradle.kts`), and keep whitespace/newline rules clean while excluding generated/build directories (`**/build/**`).
- Run `./gradlew spotlessApply` so existing code conforms to the configured formatting rules, and make sure `./gradlew spotlessCheck` passes without errors.
- If `.github/workflows/android-build.yml` exists, ensure it runs `spotlessCheck` cleanly (and supports `spotlessApply` on `workflow_dispatch` if needed) without breaking ABI-split APK builds or artifact uploads.

### 2. Create `AGENT.md` at the Repository Root
Write a clear, concise, practical `AGENT.md` tailored specifically to the **Ro-Lang** codebase based on the actual files and architecture in the repository:
- **Project Overview**: Indonesian → Tagalog learning app (`com.rolang.app`), Kotlin, Jetpack Compose, Material 3, Room (schema version 6 with explicit non-destructive migrations, no backend/account/sync), DataStore, OkHttp cache, Android `fil-PH` TextToSpeech, and `rolang-server/` static content pipeline.
- **Key Directories & Responsibilities**: `app/src/main/java/com/rolang/app/{data,network,ui}`, `app/src/main/assets/corpus.json`, `tools/build_corpus.py`, `rolang-server/`, and `.github/workflows/`.
- **Build, Formatting & Verification Commands**:
  - JDK 17, Android SDK `platforms;android-35`, Build Tools `35.0.0` (API 26+ `minSdk`).
  - Corpus & content validation commands (`python3 tools/build_corpus.py`, `python3 rolang-server/tools/validate_content.py rolang-server`).
  - Formatting (`./gradlew spotlessApply`, `./gradlew spotlessCheck`) and Gradle verification (`./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest`).
- **Important Guardrails & Conventions for Agents**:
  - Keep Room schema at version 6 with explicit non-destructive migrations and preserve legacy sync tables for safe upgrades.
  - Never replace cached/last-readable content with failed, malformed, or unverified updates; keep SHA-256 index/content validation intact.
  - Respect ABI splits (`arm64-v8a`, `armeabi-v7a`, `x86`, `x86_64`) and avoid committing secrets or `keystore.properties` / `keystore.jks`.

### Final Check
- Run the relevant validation, Spotless, unit test, lint, and build commands and report exact results along with any remaining caveats.
