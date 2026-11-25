Task: Restructure and Improve RoLang Lessons

Inspect and reorganize all existing Tagalog lessons in the "rolang-server" repository. The current lesson content is messy: paragraphs are poorly structured, dialogues are mixed with explanations, and exercises are mixed with learning materials.

The goal is to make every lesson clean, readable, well-organized, and easy for Indonesian beginners to understand.

1. Review All Lessons

- Inspect every lesson JSON file in the "data" branch.
- Review the original "elementary tagalog 1.epub" whenever necessary to understand the correct content and context.
- Fix broken paragraphs, inconsistent formatting, and poorly separated sections.
- Preserve the original textbook's meaning and learning progression.
- Do not remove important explanations, examples, vocabulary, or exercises.

2. Separate Content Types

Organize each lesson into clearly separated sections:

1. Introduction – Briefly explain what the student will learn.
2. Learning Material – Grammar explanations and concepts, divided into short, readable paragraphs.
3. Vocabulary – Tagalog words, Indonesian meanings, and relevant examples.
4. Examples – Separate Tagalog sentences from their Indonesian translations.
5. Dialogues – Display conversations in dedicated dialogue blocks, with clear speaker names and separate messages. Never mix dialogue with grammar explanations.
6. Exercises – Put all questions and practice activities in a dedicated section, separate from learning materials.
7. Summary – Briefly recap the important concepts when appropriate.

Not every lesson needs every section. Only include sections supported by the original textbook.

3. Improve Readability

- Split long paragraphs into smaller, logically connected paragraphs.
- Use clear headings and subheadings.
- Avoid combining multiple unrelated concepts in one section.
- Keep Tagalog text and Indonesian explanations visually distinguishable.
- Preserve original Tagalog spelling, punctuation, and examples.
- Fix incorrect paragraph boundaries caused by EPUB extraction.
- Remove duplicated or meaningless fragments introduced during extraction.
- Use lists and tables only when they genuinely improve comprehension.

4. Exercises and Dialogues

- Exercises must never appear inside explanations or vocabulary sections.
- Preserve exercise instructions, questions, and answer choices.
- Keep answer keys separate from questions when available.
- Dialogues must retain their original speaker order and conversational context.
- Do not accidentally classify textbook examples as dialogues or exercises.

5. Adapt Lesson UI/UX

Inspect the Android lesson rendering components and adapt them to the improved JSON structure.

- Render each content type using an appropriate Material 3 component.
- Use dedicated layouts for vocabulary, examples, dialogues, and exercises.
- Make dialogue speakers easy to distinguish.
- Make exercises interactive where the existing architecture supports it.
- Display relevant textbook illustrations in their appropriate sections.
- Improve spacing, typography, hierarchy, and reading comfort.
- Ensure long lessons remain easy to navigate.

Do not simply display all JSON content as generic paragraphs.

6. Repository and Compatibility

- Work exclusively on the "data" branch of "rolang-server" for content changes.
- Preserve the existing repository structure and GitHub Raw URLs.
- Update JSON schemas or indexes if necessary.
- Keep the Android app compatible with the updated content format.
- Do not implement Stories yet.
- Do not modify unrelated application features.

7. Validation

After restructuring:

- Validate every lesson JSON file.
- Check that no content is accidentally missing or duplicated.
- Verify all image references.
- Check that dialogues, exercises, vocabulary, and explanations are properly separated.
- Test the Android lesson UI with short and long lessons.
- Run relevant tests, lint, and Android debug build.

Implement the changes directly. Do not merely provide recommendations.

At completion, report the number of lessons reviewed, files changed, structural improvements, and verification results.