Upgrade the UI/UX and lesson/story content across both the Android app (`rolang-app`, `tools/build_corpus.py`) and the backend server (`rolang-server/`) since lesson and story content is fetched/synced from the server:

1. **Server & Corpus Content Updates (`rolang-server/` & `tools/build_corpus.py`)**
   - Update lesson and story data/schemas/seed/import scripts in `rolang-server/` (and `tools/build_corpus.py` / bundled corpus as applicable) so the server serves the upgraded, structured lessons and longer, more engaging stories.
   - Keep dictionary/word coverage intact and pass corpus validation, migrations, and import checks.

2. **Deeper, Properly Structured Lessons (Backend Content + Lesson UI)**
   - Make every lesson thorough, structured, and clear rather than brief summary notes:
     - **Goals**: Clear learning objectives and real-life usage context.
     - **Vocabulary**: Core words with pronunciation/reading guide, part of speech, meaning, and usage notes.
     - **Grammar**: Beginner-friendly sentence pattern explanations with structure formulas, rules, and common pitfalls.
     - **Examples**: At least 4–5 realistic example sentences per lesson, each with natural translation, word/structure breakdown, and audio/speaker support.
     - **Practice**: Varied exercises testing the lesson thoroughly.

3. **Longer, More Engaging Stories & Gated Comprehension Questions**
   - Make stories/chapters longer and more engaging with lively character dialogue, minor conflicts/stakes, or light plot twists instead of short/flat narratives.
   - **Question gating**: Hide or lock comprehension questions while the user is still reading the story. Only reveal or enable the comprehension questions/quiz after the user reaches/finishes the end of the story.

4. **Modern & Clean UI/UX Redesign (Jetpack Compose Material 3)**
   - Redesign the Lesson and Reading Room screens to look modern, clean, and breathable with strong visual hierarchy (avoid cramped/stacked buttons, dull flat tones, and clunky layouts).
   - Use well-spaced cards, clean typography, consistent icons, and smooth step/tab navigation.
   - Improve word highlighting in the Reading Room (subtle underlines or soft highlights instead of heavy solid blue blocks) and group audio/translation controls cleanly.

Preserve Room migrations, DataStore preferences, offline caching/download behavior, sync logic, and existing unit/instrumentation/integration tests.
