Task: Add New Tagalog Lessons from Course Book 1

Expand the RoLang learning content using the PDF:

"Learning Tagalog - Fluency Made Fast and Easy - Course Book 1.pdf"

1. Inspect the PDF

- Locate and open the PDF from the available project files.
- Read the entire book and inspect its structure, chapters, lessons, and learning progression.
- Extract relevant images, illustrations, diagrams, tables, and other visual materials.
- Preserve the original examples, dialogues, vocabulary, explanations, and exercises.
- Use the PDF as the primary source. Do not invent or replace its content with unrelated material.

2. Create New Lessons

- Convert the book's content into structured RoLang lessons.
- Organize lessons by chapter and topic, following the book's learning progression.
- Use Indonesian as the explanation language and Tagalog as the target language.
- Separate grammar, vocabulary, examples, dialogues, exercises, and summaries into appropriate content sections.
- Break long explanations into readable paragraphs.
- Include Indonesian translations for Tagalog examples where supported by the source.
- Preserve important context and avoid omitting useful material.

3. Extract and Use Images

- Extract images and illustrations from the PDF when available.
- Save them in the appropriate content repository directories.
- Reference them correctly in lesson JSON files.
- Associate each image with its relevant lesson or section.
- Optimize image sizes without making them unreadable.
- Do not use unrelated or randomly generated images as replacements.

4. Repository Integration

- Add the new content to "rolang-server" on the "data" branch.
- Follow the existing JSON schema, directory structure, and naming conventions.
- Update lesson indexes or manifests so the Android app can discover the new lessons.
- Keep existing lessons intact. Avoid duplicate content.
- Ensure all image paths work with the existing GitHub Raw content loader.

5. Android Compatibility and Validation

- Inspect the existing Android lesson renderer and make only necessary changes to support the new content.
- Ensure images, tables, dialogues, vocabulary, and exercises render correctly.
- Validate every generated JSON file and image reference.
- Check for missing pages, duplicated content, and extraction errors.
- Run relevant tests, lint, and Android debug build if app code changes are required.

Implement everything directly. Do not stop at recommendations or a content plan.

At completion, report the chapters processed, number of new lessons created, images extracted, files changed, and validation results.