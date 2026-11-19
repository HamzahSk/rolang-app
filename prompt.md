Task: Build Complete Tagalog A1 Learning Content and Integrate with Server

Work directly in both repositories:

- "rolang-app/" — Android application.
- "rolang-server/" — Backend API and MongoDB database.

The goal is to populate Ro-Lang with high-quality beginner learning content and engaging stories, then ensure the Android application retrieves everything directly from the server.

1. Language Configuration

Set the learning configuration as follows:

- Native language: Indonesian ("id")
- Target language: Tagalog ("tl")
- Learning level: A1 (Beginner)

Indonesian must be the primary explanation language. Tagalog is the language being learned.

All lesson instructions, grammar explanations, translations, and learning guidance must be written in clear, natural Indonesian.

Do not mix English into the learning materials.

2. Build a Complete A1 Lesson Curriculum

Create a structured A1 curriculum covering essential beginner Tagalog skills.

Organize lessons into modules and progressive learning units.

Include topics such as:

- Greetings and introductions.
- Pronouns and personal information.
- Basic sentence structures.
- The particles "ang", "ng", and "sa".
- Common nouns and everyday objects.
- Numbers, dates, and time.
- Family and relationships.
- Food, drinks, and ordering.
- Daily routines and activities.
- Places and directions.
- Basic questions and answers.
- Common verbs and everyday actions.
- Describing people and objects.
- Possession and ownership.
- Negation and simple expressions.
- Everyday conversations.

Cover the essential A1 foundations comprehensively without introducing advanced grammar prematurely.

Lesson Structure

Every lesson must contain:

1. Title and description
2. Learning objectives.
3. New vocabulary.
4. Tagalog example sentences.
5. Indonesian translations.
6. Clear grammar explanations in Indonesian.
7. Natural everyday dialogues.
8. Pronunciation guidance using Tagalog spelling and syllable breakdown where useful.
9. Practical examples and contextual usage.
10. Interactive exercises.
11. Answer keys and explanations.
12. Short lesson review.

Exercises should include:

- Multiple choice.
- Vocabulary matching.
- Fill in the blanks.
- Sentence ordering.
- Translation exercises.
- Conversation completion.

Ensure answers are accurate and consistent with the lesson material.

Organize lessons progressively so learners understand basic concepts before encountering more complex ones.

3. Story Library — Tagalog A1

Create an engaging collection of original Tagalog A1 stories.

Stories must be genuinely interesting, coherent, and significantly longer than simple beginner reading passages.

Each story should:

- Have a compelling title and premise.
- Contain multiple chapters.
- Have a clear beginning, development, and conclusion.
- Include interesting characters and natural dialogue.
- Use beginner-friendly Tagalog vocabulary and grammar.
- Introduce useful everyday expressions.
- Maintain consistent characters and story continuity.
- Avoid unnecessarily advanced vocabulary and grammar.

Create engaging themes such as:

- Friendship and school life.
- Everyday adventures.
- Family and relationships.
- Mystery and exploration.
- Traveling and discovering new places.
- Unexpected encounters.
- Everyday challenges and problem-solving.

Do not make all stories repetitive or follow the same plot.

Story Chapter Structure

Every chapter should contain:

1. Chapter title.
2. Original Tagalog story text.
3. Complete Indonesian translation.
4. Important vocabulary with Indonesian meanings.
5. Useful expressions.
6. Grammar observations.
7. Reading comprehension questions.
8. Answer keys.

Keep Tagalog and Indonesian content aligned paragraph by paragraph or sentence by sentence.

Stories should teach vocabulary naturally through context rather than simply listing unrelated words.

Maintain consistent character names, relationships, and events between chapters.

4. Vocabulary System

Build a reusable vocabulary structure.

Each vocabulary entry should support:

- Tagalog word or expression.
- Indonesian meaning.
- Word category.
- Pronunciation or syllable breakdown.
- Example sentence in Tagalog.
- Indonesian example translation.
- Related lesson or story references.

Avoid duplicate vocabulary entries where possible.

Reuse vocabulary across lessons and stories to reinforce learning.

5. Populate MongoDB

Inspect the existing MongoDB schema and backend architecture before creating content.

Then:

- Insert all A1 lessons into MongoDB.
- Insert all A1 stories and chapters.
- Store vocabulary and exercises.
- Maintain relationships between lessons, stories, chapters, and vocabulary.
- Use stable identifiers.
- Create appropriate indexes.
- Avoid duplicate records when seeding is executed repeatedly.

Create a reliable seed script that can safely be executed multiple times.

Do not create mock data or leave the actual database empty.

Do not expose MongoDB credentials in source code or logs.

6. Verify Backend APIs

Inspect the existing API implementation and ensure endpoints support:

- Retrieving available learning languages.
- Retrieving the A1 curriculum.
- Retrieving modules and lessons.
- Retrieving lesson details.
- Retrieving story catalogs.
- Retrieving story details and chapters.
- Retrieving vocabulary.
- Retrieving exercises and answers.

Preserve existing API contracts where possible.

If additional endpoints are necessary, implement them properly.

Ensure API responses include all information required by the Android application.

7. Android Server Integration

Inspect "rolang-app" and verify that the application actually retrieves learning content from "rolang-server".

Do not assume the existing integration works.

Implement or fix:

- Centralized server API configuration.
- HTTP client and serialization.
- Lesson catalog retrieval.
- Lesson detail retrieval.
- Story catalog retrieval.
- Story and chapter retrieval.
- Vocabulary retrieval.
- Exercise retrieval.

Ensure the application uses real API responses instead of hardcoded or placeholder content.

Required UI Behavior

- Display actual lessons retrieved from MongoDB.
- Display actual stories retrieved from MongoDB.
- Show lesson modules and progress.
- Display chapter content and Indonesian translations.
- Display vocabulary and exercises.
- Handle loading, empty, and error states.
- Implement retry behavior.
- Preserve existing UI/UX wherever possible.

The default learning configuration should be Indonesian → Tagalog, A1.

8. Offline Compatibility

Preserve the existing offline architecture.

- Automatically cache lessons and stories when opened.
- Allow users to download complete stories.
- Allow downloaded content to be accessed offline.
- Synchronize updated content when online.
- Never erase cached content because of a failed network request.

9. Content Quality Requirements

Content quality is a priority.

- Use natural, grammatically correct Tagalog.
- Use natural Indonesian translations.
- Avoid literal translations that sound unnatural.
- Ensure all explanations are understandable to Indonesian beginners.
- Avoid mixing English into the curriculum.
- Keep content appropriate for A1 learners.
- Avoid repetitive or artificially generated filler.
- Ensure exercises have correct answer keys.

Prioritize complete, high-quality lessons and coherent stories over generating a large amount of shallow content.

10. Testing and Verification

Test both repositories.

Backend:

- MongoDB connectivity.
- Seed script execution.
- Duplicate prevention.
- Lesson and story API responses.
- Chapter ordering.
- Vocabulary and exercise retrieval.

Android:

- Actual server connectivity.
- Lesson catalog loading.
- Story catalog loading.
- Lesson and chapter details.
- Indonesian translations.
- Error and retry handling.
- Offline caching.

Run relevant backend tests, Android unit tests, lint, and build checks.

If deployment access is available, verify the deployed Vercel API and confirm that seeded data can be retrieved from production.

Do not claim successful server integration without actual verification.

Important Rules

- Work directly in both repositories.
- Implement actual content and code changes.
- Do not merely provide suggestions or sample JSON.
- Do not modify unrelated features.
- Do not replace MongoDB with another database.
- Do not remove existing user data.
- Preserve existing API compatibility where possible.
- Do not use placeholder lessons or stories.
- Do not claim that content was uploaded unless MongoDB insertion was verified.

Final Report

Provide:

1. A1 modules and lessons created.
2. Stories and chapters created.
3. Vocabulary and exercises created.
4. MongoDB seed results.
5. API endpoints verified.
6. Android integration results.
7. Offline functionality test results.
8. Build and deployment status.
9. Any remaining manual steps.

The final result should be a functional Indonesian-to-Tagalog A1 learning platform with complete lessons, engaging stories, real MongoDB-backed content, and working online/offline support.