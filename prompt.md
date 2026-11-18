Task: Redesign Ro-Lang Learning Experience and UI/UX

Inspect the existing Ro-Lang Android project and understand its current architecture before implementing changes.

1. Restructure the Learning Sections

Separate learning content into two independent sections:

A. Lessons

Create a structured, progressive learning curriculum.

- Organize lessons into units and topics.
- Each unit should contain logically ordered lessons.
- Display lesson numbers, titles, descriptions, and learning objectives.
- Show completion status and learning progress.
- Make prerequisites and lesson progression clear.
- Each lesson should have a dedicated learning interface.
- Present vocabulary, grammar explanations, examples, and exercises in well-organized sections.

Lessons should feel like a guided language course rather than a collection of unrelated content.

B. Stories

Create a completely separate section dedicated to reading practice.

- Organize stories by level, genre, and topic.
- Provide story cards with attractive illustrations, titles, descriptions, and difficulty indicators.
- Keep stories independent from the lesson curriculum.
- Preserve the existing reading experience, including word highlighting, dictionary lookup, translations, and reading progress.
- Add story completion tracking.

Lessons and Stories must have separate navigation, content organization, and progress tracking.

2. Interactive Exercises and Questions

Add interactive questions at the end of EVERY lesson and story.

For lessons, include:

- Multiple-choice questions.
- Vocabulary translation.
- Fill-in-the-blank exercises.
- Grammar questions.
- Sentence construction.

For stories, include:

- Reading comprehension questions.
- Multiple-choice questions.
- Vocabulary understanding.
- Questions about story events and details.

Requirements:

- Questions must be relevant to the content.
- Provide immediate feedback for answers.
- Explain why an answer is correct or incorrect.
- Display a final score and performance summary.
- Allow learners to retry exercises.
- Track completed exercises and results.

Do not make questions purely decorative. They must be functional and interactive.

3. Learning Interface Improvements

Redesign the lesson experience to make it engaging and easy to follow.

Consider using:

- Clear lesson sections and visual hierarchy.
- Progress indicators.
- Vocabulary cards.
- Grammar explanation cards.
- Example sentence blocks.
- Illustrations supporting each topic.
- Interactive exercises between learning sections where appropriate.
- A clear next/previous lesson navigation.

Avoid displaying all lesson content as one long, unstructured page.

4. UI/UX Redesign

Improve the overall application interface with a modern educational design.

Focus on:

- More attractive Home screen.
- Better lesson and story browsing interfaces.
- Improved content cards and layouts.
- Consistent spacing, typography, and visual hierarchy.
- Better use of illustrations and visual elements.
- Smooth and meaningful animations.
- Clear navigation and interaction feedback.
- Proper light and dark theme support.

The design should feel polished, modern, friendly, and comparable to a high-quality language-learning application.

Avoid excessive decoration, clutter, or unnecessary gradients.

5. Learning Dashboard

Improve the Home screen to show useful learning information:

- Current learning course and level.
- Continue learning shortcut.
- Current unit and next lesson.
- Lesson completion progress.
- Story reading progress.
- Vocabulary collected.
- Upcoming reviews.
- Daily learning activity.

Make it easy for users to resume where they left off.

6. Progress and Data Persistence

Integrate all changes with the existing persistence system.

- Track lesson completion independently from story completion.
- Save exercise answers and results.
- Preserve learning progress after restarting the application.
- Prevent duplicate completion records.
- Maintain compatibility with existing vocabulary and SRS systems.
- Preserve existing user data and database migrations.

7. Technical Requirements

- Reuse existing Kotlin, Jetpack Compose, Material 3, Room, and ViewModel architecture.
- Avoid unnecessary architectural rewrites.
- Keep all existing language-learning functionality working.
- Ensure lessons, stories, and exercises work offline.
- Use reusable Compose components.
- Keep content models structured and extensible for future levels.
- Do not hardcode UI content when it should come from the learning corpus.

8. Validation

After implementation:

- Verify lesson and story navigation.
- Test all question types.
- Verify answer evaluation and scoring.
- Test completion and progress persistence.
- Verify light and dark themes.
- Check different screen sizes.
- Run unit tests and Android lint.
- Build the debug APK.

Fix any issues introduced by the changes.

Final Goal

Transform Ro-Lang into a structured, interactive language-learning application.

Lessons should function as a guided educational curriculum, while Stories should function as an independent reading practice experience.

Both must include meaningful interactive questions at the end, with progress tracking and persistent results.

Prioritize usability, visual quality, learning effectiveness, and consistency with the existing application architecture.

Implement the changes directly in the project. Do not merely provide mockups, recommendations, or placeholder screens.