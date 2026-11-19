Task: Implement Tagalog A1 Content and Server Integration

Work directly in both repositories:

- "rolang-app/" — Android application.
- "rolang-server/" — Backend and MongoDB.

1. Learning Configuration

Set the default learning language:

- Native language: Indonesian ("id").
- Target language: Tagalog ("tl").
- Level: A1 (Beginner).

Use Indonesian for all explanations, translations, and learning instructions. Do not mix English into the learning materials.

2. Create Complete A1 Lessons

Build a structured A1 curriculum with progressive modules covering:

- Greetings and introductions.
- Pronouns and sentence structures.
- Basic grammar and particles ("ang", "ng", "sa").
- Numbers, dates, and time.
- Family and relationships.
- Food, drinks, and daily activities.
- Places and directions.
- Common verbs and expressions.
- Questions, negation, and everyday conversations.

Each lesson should include:

- Learning objectives.
- Tagalog vocabulary and Indonesian meanings.
- Example sentences and translations.
- Grammar explanations in Indonesian.
- Natural dialogues.
- Pronunciation guidance.
- Interactive exercises and answer explanations.
- Short review.

Ensure the lessons are progressive, accurate, and suitable for absolute beginners.

3. Create Engaging A1 Stories

Create original, interesting, and reasonably long Tagalog stories.

Use themes such as friendship, school, adventure, mystery, family, and everyday life.

Each story should have multiple chapters, coherent plots, consistent characters, and natural dialogue.

Every chapter must include:

- Original Tagalog text.
- Complete Indonesian translation.
- Important vocabulary and meanings.
- Useful expressions.
- Grammar observations.
- Reading comprehension questions and answers.

Keep vocabulary and grammar appropriate for A1 learners. Stories should teach useful words naturally through context.

4. MongoDB Integration

Inspect the existing MongoDB architecture and store all learning content in the database.

- Save lessons, modules, stories, chapters, vocabulary, and exercises.
- Maintain proper relationships and ordering.
- Use stable identifiers.
- Prevent duplicate records when seeding repeatedly.
- Create a reusable seed script.
- Preserve existing user data and content.

Do not use mock data or hardcoded story content as the primary data source.

5. Backend API

Ensure the server provides working APIs for:

- Learning curriculum and modules.
- Lesson details.
- Story catalogs and details.
- Chapter content.
- Vocabulary.
- Exercises and answers.

Preserve existing API compatibility where possible.

6. Android Integration

Ensure "rolang-app" actually retrieves content from "rolang-server".

- Replace hardcoded learning content with real API responses.
- Display modules, lessons, stories, chapters, and vocabulary.
- Implement loading, empty, and error states.
- Add retry handling.
- Preserve existing UI/UX and navigation.
- Set Indonesian → Tagalog A1 as the default configuration.

Verify that the app displays actual MongoDB content, not placeholders.

7. Offline Support

Preserve existing offline functionality:

- Automatically cache lessons and stories when opened.
- Allow downloading complete stories.
- Show download status and progress.
- Support offline reading.
- Synchronize outdated content when online.
- Never delete cached content because of network failures.

8. Testing

Test both repositories:

- MongoDB connectivity and seed execution.
- Duplicate prevention.
- Lesson and story API responses.
- Chapter ordering and translations.
- Android server connectivity.
- Automatic caching and offline reading.
- Relevant backend tests, Android tests, lint, and build.

If deployment access is available, verify the production API on Vercel.

Important Rules

- Implement actual code and content in both repositories.
- Do not merely provide suggestions or sample data.
- Do not modify unrelated features.
- Do not replace MongoDB.
- Do not delete existing user data.
- Prioritize content quality over quantity.
- Do not claim successful uploads or integration without verification.
- Work incrementally and fix errors before proceeding.

Final Report

Summarize the lessons, stories, chapters, vocabulary, MongoDB seed results, API verification, Android integration, and test results.