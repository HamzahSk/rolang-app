Task: Simplify Ro-Lang Language System and Implement Tagalog Text-to-Speech

Inspect the entire existing Ro-Lang project before making changes.

1. Remove English Learning Content

Simplify the application to support only one language pair:

- Reference language: Indonesian ("id")
- Target language: Filipino / Tagalog ("fil")

Indonesian must be the primary interface and explanation language. Tagalog must be the only language being studied.

Requirements:

- Remove English from language selection and learning content.
- Remove English translations, explanations, and vocabulary entries.
- Replace English-based learning references with Indonesian.
- Ensure all lessons, stories, vocabulary, exercises, flashcards, and dictionary explanations use Indonesian as their reference language.
- Update onboarding, profile, settings, and language selection accordingly.
- Remove obsolete English content from the bundled learning corpus.

Do not remove unrelated application functionality or accidentally delete user learning progress.

Keep the architecture extensible for additional languages in the future, but expose only Indonesian → Tagalog for now.

2. Implement Native Android Text-to-Speech

Add pronunciation audio throughout the application using Android's built-in "TextToSpeech" API.

Before implementation, inspect the official documentation:

https://developer.android.com/reference/android/speech/tts/TextToSpeech

Google documents Filipino support with locale "fil-PH":

https://docs.cloud.google.com/text-to-speech/docs/list-voices-and-types

Use Android's native TTS engine first. Do not require API keys, external accounts, or a cloud service.

Language Detection

- Initialize Android TextToSpeech properly.
- Check Filipino availability using "isLanguageAvailable()".
- Inspect available languages and voices where supported.
- Use "Locale("fil", "PH")" as the preferred locale.
- Verify that the selected voice actually supports Filipino.
- Handle missing voice data and unsupported devices gracefully.
- Never silently fall back to English pronunciation.

If Filipino is unavailable, show a helpful message explaining how to install Filipino voice data through Android Text-to-Speech settings.

Pronunciation Features

Add speaker buttons to:

- Individual vocabulary words.
- Example sentences.
- Lesson vocabulary.
- Dialogue lines.
- Story sentences.
- Flashcards.
- Pronunciation exercises.

The audio should pronounce the original Tagalog text, not its Indonesian translation.

Audio Controls

Implement:

- Play pronunciation.
- Stop playback.
- Replay pronunciation.
- Adjustable speech speed.
- Optional pitch adjustment.
- Clear playback state and visual feedback.

Use sensible default speech speed for language learners.

3. Improve Pronunciation Learning

Add pronunciation-focused learning interactions.

Examples:

- Listen to a Tagalog word.
- Listen to a complete sentence.
- Repeat and compare vocabulary through listening practice.
- Listen to dialogue one line at a time.
- Play an entire story sentence by sentence.

Keep the interface simple and intuitive.

4. TTS Architecture

Create a reusable TTS manager/service instead of initializing separate TextToSpeech instances throughout the application.

Requirements:

- Proper initialization and shutdown.
- Lifecycle-aware resource management.
- Handle rapid repeated playback requests.
- Prevent overlapping speech.
- Support different speech queue modes.
- Handle initialization and synthesis errors.
- Avoid memory leaks.
- Reuse the existing ViewModel and Compose architecture.

All pronunciation playback should work offline when the required Filipino voice data is installed.

Do not bundle unnecessarily large audio files.

5. UI/UX Integration

Integrate pronunciation controls naturally into the existing UI.

- Use recognizable speaker icons.
- Add subtle playback animations.
- Make buttons accessible.
- Ensure speaker buttons do not interfere with vocabulary selection or word highlighting.
- Support light and dark themes.
- Keep the interface clean and consistent.

6. Data and Compatibility

- Update language-pair configuration and content models.
- Preserve existing Indonesian → Tagalog learning progress.
- Migrate or safely handle obsolete English-related records.
- Do not use destructive database migrations.
- Keep vocabulary, reading, and SRS functionality intact.
- Ensure the bundled corpus works entirely offline.

7. Validation

Test:

- Indonesian interface and explanations.
- Tagalog vocabulary and translations.
- Filipino TTS initialization.
- Filipino voice availability detection.
- Word and sentence playback.
- Playback stop and replay.
- Missing voice data handling.
- Offline functionality.
- Existing database migrations.
- Unit tests, lint, and debug APK build.

Fix any issues introduced by the changes.

Final Goal

Transform Ro-Lang into a focused Indonesian-to-Tagalog learning application with reliable native pronunciation support.

Indonesian is the teaching language. Tagalog is the learning language.

Remove English-related confusion completely while preserving the existing learning experience and user progress.

Implement everything directly in the existing project. Avoid unrelated architectural rewrites
.