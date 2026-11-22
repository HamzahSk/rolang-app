Upgrade the UI/UX and content quality of the Ro-Lang app (`rolang-app` and `tools/build_corpus.py`) with the following requirements:

1. **Modern & Clean UI/UX Redesign (Jetpack Compose Material 3)**
   - Redesign the Lesson and Reading Room screens so they look modern, clean, breathable, and have strong visual hierarchy (avoid cramped/stacked filter buttons, dull flat tones, and clunky layouts).
   - Use well-spaced cards, clean typography, consistent icons, and smoother navigation (such as a clean horizontal scrollable pill bar or step indicator).
   - Improve word highlighting in the Reading Room (use subtle underlines or clean soft highlights instead of heavy solid blue blocks), and group audio/translation controls cleanly into a compact action bar.

2. **Deeper, Properly Structured Lesson Content (`tools/build_corpus.py` & Lesson UI)**
   - Overhaul Lesson content so every unit feels thorough, structured, and genuinely educational rather than brief bullet notes.
   - Make each section rich and clear:
     - **Goals**: Clear learning objectives and real-life context for when/why the topic matters.
     - **Vocabulary**: Core words with pronunciation/reading guide, part of speech, meaning, and usage notes.
     - **Grammar**: Beginner-friendly sentence pattern explanations with clear structure formulas, rules, and common pitfalls to avoid.
     - **Examples**: At least 4–5 realistic example sentences per lesson (not just 1–2 short lines), each with natural translation, word/structure breakdown, and audio playback.
     - **Practice**: Varied exercises testing the lesson concepts thoroughly.

3. **Longer, More Engaging Stories & Gated Comprehension Questions**
   - Expand story/chapter lengths and add more engaging elements such as lively character dialogue, minor conflicts/stakes, or light plot twists so stories feel fun and immersive rather than flat.
   - **Question gating**: Hide or lock comprehension questions until the user finishes reading the story. Only reveal or enable the comprehension questions/quiz once the user reaches/completes the end of the story paragraphs.

Keep `python3 tools/build_corpus.py` validation passing, preserve dictionary/word coverage, and maintain compatibility with Room migrations, DataStore preferences, and existing unit/instrumentation tests.

Also update `rolang-server/` corpus/seed data or schemas if needed so server-delivered content matches the updated lesson and story structure.
