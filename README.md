# Ro-Lang for Android

An online-first language-learning app with persistent offline reading built with Kotlin, Jetpack Compose,
Material 3, Room, DataStore, and Navigation Compose. Android 8.0 (API 26) or newer.

## Build and run

Open this directory in Android Studio. Use JDK 17 and install Android SDK platform
35 and Build Tools 35.0.0. The included Gradle 8.9 wrapper verifies its distribution
checksum. No API keys or service accounts are needed.

```sh
./gradlew :app:assembleDebug
./gradlew :app:installDebug
```

APKs are generated per ABI under `app/build/outputs/apk/debug/`, for example
`app-arm64-v8a-debug.apk`.
The first build needs internet to download build dependencies; the installed app
fetches its library from the configured Ro-Lang server. Opening a story saves it
for offline reading; **Download story** saves every chapter through a durable
background job. A fresh installation needs a connection to obtain content.
An account is optional. Online content is enabled in the default configuration.

## Build with GitHub Actions

The [Ro-Lang Android Build workflow](../.github/workflows/android-build.yml)
runs on pushes and pull requests to `main` that change `rolang-app/**` or the
workflow file. It also supports manual runs:

1. Commit the app and workflow file, then push them to GitHub.
2. Open **Actions → Ro-Lang Android Build → Run workflow**.
3. After a successful run, download **rolang-android-debug** from **Artifacts**.
   Extract the archive to get the installable `app-debug.apk`.

CI sets up JDK 17, Python 3.12, Android SDK 35, Build Tools 35.0.0, and Gradle
caching. It validates the corpus, runs Robolectric unit tests and Android lint,
and builds the debug app and Android test APKs. The test APK is available in
**rolang-android-tests**; device tests can be run separately on an emulator or
device. Test and lint reports are uploaded as **rolang-test-reports**, including
when checks fail. This debug build requires no GitHub Secrets or signing keystore.

## Learning flow

1. A new installation starts with Indonesian as the reference language, Tagalog
   as the learning language, and A1 as the level. These settings remain editable
   in **Profile** and survive restart.
2. Follow the ordered **Tagalog A1 Lessons** curriculum by unit and topic, with
   goals, vocabulary, grammar, examples, prerequisites, and graded final practice.
   Browse **Stories** independently by level, format, and topic.
3. Scroll through an entire story as continuous paragraphs in the Reading room.
   Tap any word to see its offline translation,
   contextual meaning, part of speech, explanation, and example in both languages.
   **Translate story** shows reference-language text beneath each paragraph. Your
   paragraph position is saved. Completing the comprehension practice records
   the story and score; every practice set gives immediate feedback and can be retried.
4. Save words and find them in **Vocabulary**, grouped by story or category.
   New words are blue, Learning words green, Almost Mastered words yellow, and
   Known words have no highlight. Every word remains tappable. Change any of the
   four statuses in word details, or filter the collection by status.
   Removing a word asks for confirmation and removes its schedule.
5. In **Review**, answer multiple-choice questions or type translations. Incorrect
   answers return in ten minutes. Correct answers can be rated Again, Hard, Good,
   or Easy; Good starts at one day, then six days, then increasing intervals.
   Three successful repetitions reach Almost Mastered. Five successful repetitions
   and an interval of at least 21 days confer Known status. Missed answers return
   words to Learning. Manual Known status pauses reviews; New/Learning resets the
   consecutive streak and makes the word due. Manual Almost Mastered is preserved
   through Good/Easy reviews. An open quiz cannot overwrite a later manual edit.
6. **Home** and **Profile** show separate lesson/story completion, the next lesson,
   reading progress, collected words, daily activity, upcoming reviews, mastery,
   review accuracy, and a calendar-day review streak.

Vocabulary and review statistics are separated by target/reference language pair.
Reading progress is separated by story variant and target language. Database writes
and review logs use transactions; duplicate saves and stale quiz submissions are
rejected. Quizzes left without a rating do not change the schedule. Learning data is
stored on the device. An optional account synchronizes it with the backend in
`rolang-server/`; offline reading uses automatically saved or manually downloaded content.

## Server configuration and synchronization

Edit the root `config.json` in this Android project. Gradle copies this one source
into generated app assets at build time. Set `serverBaseUrl` to the deployment
origin (for example `https://YOUR-DEPLOYMENT.vercel.app`, without `/api/v1`) and keep
`onlineEnabled` set to true. The app reads configuration at runtime, including on each
worker run. A complete private `files/config.json` override takes precedence over
the packaged configuration; managed deployments can provision it without changing
URLs in Kotlin. Invalid configuration disables networking and keeps local data.

| Setting | Default | Purpose |
| --- | --- | --- |
| `serverBaseUrl` | `https://rolang-server.vercel.app` | Server origin; HTTPS required outside local development |
| `apiVersion` | `v1` | API path version |
| `connectionTimeoutMs` | 10000 | Connection timeout (1000–120000 ms) |
| `requestTimeoutMs` | 30000 | Whole-request and read timeout (1000–120000 ms) |
| `onlineEnabled` | true | Enable public story downloads and account actions |
| `automaticSyncEnabled` | true | Synchronize learning changes automatically |
| `syncIntervalMinutes` | 15 | Background sync interval (15–1440 minutes) |
| `storyPageSize` | 30 | Number of catalog entries per request (1–100) |

When online features are enabled, launch refreshes published stories. In
**Profile → Online library**, register or sign in to synchronize saved vocabulary,
SRS schedules, reviews, reading positions, completion, and learning preferences.
**Sync now** manually retries; signed-out learners can use **Refresh stories**.
Disabling automatic sync still allows launch-time story refresh and manual sync.
Accounts require a valid email, a 12–128 character password, and a display name
when registering. Session tokens are encrypted with an Android Keystore key;
passwords are never persisted. Sign-out keeps local learning data.

Room writes learning changes and their pending operations in the same transaction.
WorkManager persists connected-network jobs and retries unavailable servers with
exponential backoff. A periodic job catches new changes while the app is closed;
Android battery policies can delay execution. Every upload has an operation UUID;
retries do not duplicate review events. Acknowledgements only remove the exact
uploaded operation, preserving edits made during a request. Downloaded records
and the revision cursor commit together. Deletions propagate as tombstones.

Catalog metadata persists separately from content. Opening a story retrieves its
latest manifest and chapters, validates dictionary coverage, and saves the complete
reading snapshot and dictionaries in Room. Cached content renders immediately while
the reader checks for updates. Versioned conditional requests avoid downloading
unchanged stories; chapter hashes let downloads reuse unchanged chapters. Background
sync refreshes metadata and updates previously cached stories, without downloading
unopened stories.

**Stories → Saved offline** filters automatically cached and manually downloaded
stories. Each story/lesson offers **Download story**, chapter progress, **Cancel
download**, **Retry download**, and **Remove saved content**. WorkManager persists
jobs across restart, waits for connectivity, and retries transient failures with
exponential backoff (up to nine attempts). Cancellation retains saved chapters for
retry. Removing content deletes its reading cache and unused dictionary data while
keeping vocabulary, review schedules, exercise scores, and reading progress.
Downloaded stories remain accessible even if the server removes or unpublishes them.
A failed request never deletes a reading snapshot; an incomplete or mixed revision
never replaces the previous complete snapshot.

Room version 5 upgrades versions 1–4 through explicit migrations. Existing local
stories remain readable after upgrade, and existing learning data and pending
operations are preserved. New installations do not seed the legacy corpus.
Deploy the backend migration and import before distributing this app build.

Each local library remains bound to the first account that signs in. Reconnecting
the same account preserves its pending edits; signing into another account is
rejected to prevent cross-account uploads. To use another account, use a separate
Android profile or clear app data after synchronizing. See the server README for
record conflict rules and deployment steps.

For an Android emulator talking to a local server, use `http://10.0.2.2:3000`.
Cleartext HTTP is disabled in the shipped app; local development requires a
debug-only manifest override enabling it, or a trusted local HTTPS endpoint.
Robolectric's contract tests can use a loopback HTTP server without modifying the
shipping manifest.

## Structure

- `app/src/main/java/com/rolang/app/domain`: Unicode tokenization, answer matching,
  language/level models, and the deterministic SRS scheduler.
- `app/src/main/java/com/rolang/app/data`: Room entities, DAO, repository, corpus
  loading/coverage validation, and DataStore preferences.
- `LearningViewModel.kt`: Lifecycle-managed state and learning operations.
- `network`: Runtime configuration, bounded HTTP requests, Keystore sessions,
  synchronization, shared services, and connected-network background jobs.
- `app/src/main/java/com/rolang/app/ui`: Navigation, theme, and functional screens.
- `app/src/main/assets/corpus.json`: Preserved migration/test fixture, excluded from app assets.
- `app/schemas`: Exported Room schema for future explicit migrations.
- `tools/build_corpus.py`: Authoring source and complete word-coverage validation.
- `app/src/sharedTest`: The same persistence and UI scenarios for Robolectric and
  device instrumentation; `app/src/test` contains scheduler and tokenizer tests.

The catalog and content are saved transactionally as they are retrieved. Room
persists independently of the ViewModel. There is no destructive migration fallback. Clearing app data or
uninstalling removes learning data; backups are disabled.

## Checks

```sh
python3 tools/build_corpus.py
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest
# With an emulator or USB-connected device:
./gradlew :app:connectedDebugAndroidTest
```

Local tests run the scheduler and answer matching plus real Room persistence,
full corpus coverage in every language pair, transactional review guards, DataStore
restart persistence, and the Compose read/save/translate/finish/review flow using
Robolectric. Device tests exercise those same shared scenarios. Run device tests
on a clean installation so the UI scenario starts with its default language pair.

## Content scope

The server-backed library contains ten structured lessons, six realistic dialogues,
and five coherent stories. Four stories contain 13 substantial chapters; the full
curriculum has 30 ordered chapter records and 150 interactive
questions cover multiple choice, vocabulary translation, fill-in-the-blank,
grammar, sentence construction, and reading comprehension. Topics cover pronunciation,
introductions, pronouns, questions, numbers and time, family, school and everyday
objects, verb aspect, negation, descriptions, food and shopping, directions,
travel, daily conversation, friendship, and simple romance. Each unit includes
complete Indonesian translations, focus vocabulary, grammar or usage
notes, and comprehension checks where appropriate. Eight optimized Android vector
illustrations provide a consistent offline visual identity.

Built-in translations are curated contextual glosses; grammatical markers and
multiword expressions do not always have exact one-word equivalents, so sentence
translations supply the complete meaning. There is no arbitrary-text translator.
New content is authored and published through the backend API. The corpus
generator and original assets are retained as migration/test fixtures. Content
must pass multilingual dictionary coverage checks.

The existing JavaScript Ro-Lang tutor under `../roccky_bot/ro-lang` is independent
of this app.

Content tests cover lightweight catalogs, opening/automatic caching, complete
manual downloads, cancellation, duplicate requests, interrupted chapter reuse,
server changes, invalid revisions, offline fallback, removal, and database restart.
Learning tests still cover word lookup, exercises, review guards, account sync,
preferences, and migrations. A live Android HTTP-client contract test also reads
all 21 curriculum units from real Express/MongoDB and reopens them offline:

```sh
# In rolang-server with a disposable DATABASE_URL configured:
npm ci
npm run migrate
npm run import:corpus
npm start
# In the app repository:
JAVA_HOME=/path/to/jdk17 bash gradlew :app:testDebugUnitTest \
  -ProlangIntegrationUrl=http://127.0.0.1:3000
```

Physical-device/emulator checks remain useful for OS connectivity, Keystore,
battery policies, and background job scheduling. See [migration instructions](rolang-server/docs/MIGRATION.md)
for import conflicts, verification, deployment order, and content ownership.
