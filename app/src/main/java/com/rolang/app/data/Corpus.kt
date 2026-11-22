package com.rolang.app.data

import com.rolang.app.domain.Language
import com.rolang.app.domain.Courses
import com.rolang.app.domain.Level
import com.rolang.app.domain.Words
import org.json.JSONObject

enum class UnitKind(val label: String) { LESSON("Pelajaran"), DIALOGUE("Dialog"), STORY("Bacaan") }

enum class ExerciseType {
    MULTIPLE_CHOICE, VOCABULARY_TRANSLATION, FILL_BLANK, GRAMMAR,
    SENTENCE_CONSTRUCTION, COMPREHENSION,
}

data class LearningNote(val title: Map<String, String>, val text: Map<String, String>) {
    fun title(language: Language) = title.getValue(language.code)
    fun text(language: Language) = text.getValue(language.code)
}

data class Exercise(
    val id: String,
    val type: ExerciseType,
    val prompt: Map<String, String>,
    val answer: Map<String, String>,
    val explanation: Map<String, String>,
    val choices: Map<String, List<String>> = emptyMap(),
    val tokens: Map<String, List<String>> = emptyMap(),
    val speech: Map<String, String> = emptyMap(),
) {
    fun question(language: Language) = prompt.getValue(language.code)
    fun answer(language: Language) = answer.getValue(language.code)
    fun explanation(language: Language) = explanation.getValue(language.code)
    fun choices(language: Language) = choices[language.code].orEmpty()
    fun tokens(language: Language) = tokens[language.code].orEmpty()
    fun spoken(language: Language) = speech[language.code].orEmpty()
}

data class Chapter(val id: String, val title: Map<String, String>, val start: Int, val count: Int)

data class Story(
    val id: String, val family: String, val category: String, val level: Level,
    val titles: Map<String, String>, val summaries: Map<String, String>,
    val sentences: List<Map<String, String>>,
    val order: Int = Int.MAX_VALUE, val kind: UnitKind = UnitKind.STORY, val image: String = "",
    val speakers: List<String?> = emptyList(), val objectives: List<Map<String, String>> = emptyList(),
    val notes: List<LearningNote> = emptyList(), val exercises: List<Exercise> = emptyList(),
    val focus: Map<String, List<String>> = emptyMap(), val pronunciation: Map<String, String> = emptyMap(),
    val chapters: List<Chapter> = emptyList(), val sentenceCount: Int = sentences.size,
) {
    fun title(language: Language) = titles.getValue(language.code)
    fun summary(language: Language) = summaries.getValue(language.code)
    fun sentence(index: Int, language: Language) = sentences[index].getValue(language.code)
    fun focus(language: Language) = focus[language.code].orEmpty()
}

data class WordDetail(
    val word: String, val translation: String, val meaning: String, val partOfSpeech: String,
    val explanation: String, val example: String, val exampleTranslation: String,
)

object Corpus {
    private fun value(obj: JSONObject, language: Language): String = when (language) {
        Language.INDONESIAN -> obj.getString(Language.INDONESIAN.code)
        Language.FILIPINO -> if (obj.has(Language.FILIPINO.code)) obj.getString(Language.FILIPINO.code)
            else obj.getString("tl")
    }
    private fun translations(obj: JSONObject) = Language.entries.associate { it.code to value(obj, it) }
    private fun availableTranslations(obj: JSONObject) = buildMap {
        if (obj.has(Language.INDONESIAN.code)) put(Language.INDONESIAN.code, obj.getString(Language.INDONESIAN.code))
        when {
            obj.has(Language.FILIPINO.code) -> put(Language.FILIPINO.code, obj.getString(Language.FILIPINO.code))
            obj.has("tl") -> put(Language.FILIPINO.code, obj.getString("tl"))
        }
    }
    fun story(entity: StoryEntity): Story {
        val json = JSONObject(entity.content)
        val sentences = json.optJSONArray("sentences") ?: org.json.JSONArray()
        fun localizedList(field: String): List<Map<String, String>> = json.optJSONArray(field)?.let { array ->
            (0 until array.length()).map { translations(array.getJSONObject(it)) }
        }.orEmpty()
        fun notes() = json.optJSONArray("notes")?.let { array -> (0 until array.length()).map {
            val item = array.getJSONObject(it)
            LearningNote(translations(item.getJSONObject("title")), translations(item.getJSONObject("text")))
        } }.orEmpty()
        fun localizedStrings(obj: JSONObject?, field: String): Map<String, List<String>> = obj?.optJSONObject(field)?.let { values ->
            Language.entries.associate { language ->
                val array = values.optJSONArray(language.code)
                    ?: if (language == Language.FILIPINO) values.optJSONArray("tl") else null
                language.code to array?.let {
                    (0 until it.length()).map(it::getString)
                }.orEmpty()
            }
        }.orEmpty()
        fun exercises() = json.optJSONArray("exercises")?.let { array -> (0 until array.length()).map { index ->
            val item = array.getJSONObject(index)
            val prompt = item.optJSONObject("prompt") ?: item.getJSONObject("question")
            Exercise(
                id = item.optString("id", "${entity.id}_exercise_${index + 1}"),
                type = runCatching { ExerciseType.valueOf(item.optString("type", "COMPREHENSION")) }
                    .getOrDefault(ExerciseType.COMPREHENSION),
                prompt = translations(prompt),
                answer = translations(item.getJSONObject("answer")),
                explanation = item.optJSONObject("explanation")?.let(::translations)
                    ?: Language.entries.associate { language -> language.code to "Tinjau kembali teks pembelajaran untuk jawaban ini." },
                choices = localizedStrings(item, "choices"),
                tokens = localizedStrings(item, "tokens"),
                speech = item.optJSONObject("speech")?.let(::availableTranslations)
                    ?: availableTranslations(item.getJSONObject("answer")),
            )
        } }.orEmpty()
        val focus = json.optJSONObject("focus")?.let { obj -> Language.entries.associate { language ->
            val array = obj.optJSONArray(language.code)
                ?: if (language == Language.FILIPINO) obj.optJSONArray("tl") else null
            language.code to array?.let {
                (0 until it.length()).map(it::getString)
            }.orEmpty()
        } }.orEmpty()
        var chapterSentenceCount = 0
        val chapters = json.optJSONArray("chapters")?.let { array ->
            var start = 0
            (0 until array.length()).map { index ->
                val item = array.getJSONObject(index)
                val count = item.optInt("sentenceCount", item.optJSONArray("sentences")?.length() ?: 0)
                require(count in 1..300)
                Chapter(item.getString("id"), translations(item.getJSONObject("title")), start, count).also { start += count }
            }.also { chapterSentenceCount = start }
        }.orEmpty()
        val declaredSentenceCount = when {
            json.has("sentenceCount") -> json.getInt("sentenceCount")
            sentences.length() > 0 -> sentences.length()
            else -> chapterSentenceCount
        }
        require(declaredSentenceCount in 0..300)
        require(sentences.length() == 0 || declaredSentenceCount == sentences.length())
        if (chapters.isNotEmpty()) require(chapterSentenceCount == declaredSentenceCount)
        return Story(entity.id, entity.family, entity.category, Level.valueOf(entity.level),
            translations(json.getJSONObject("title")), translations(json.getJSONObject("summary")),
            (0 until sentences.length()).map { translations(sentences.getJSONObject(it)) },
            json.optInt("order", Int.MAX_VALUE),
            runCatching { UnitKind.valueOf(json.optString("kind", "STORY")) }.getOrDefault(UnitKind.STORY),
            json.optString("image"),
            json.optJSONArray("speakers")?.let { array -> (0 until array.length()).map {
                if (array.isNull(it)) null else array.getString(it)
            } }.orEmpty(), localizedList("objectives"), notes(), exercises(), focus,
            json.optJSONObject("pronunciation")?.let(::translations).orEmpty(), chapters, declaredSentenceCount)
    }

    fun parse(root: JSONObject): Pair<List<StoryEntity>, List<LexemeEntity>> {
        val stories = root.getJSONArray("stories")
        val lexicon = root.getJSONArray("lexicon")
        val storyRows = (0 until stories.length()).map { index ->
            val story = stories.getJSONObject(index)
            require(story.getString("id").matches(Regex("[a-zA-Z0-9_-]{1,80}")))
            require(story.getString("family").matches(Regex("[a-zA-Z0-9_-]{1,80}")))
            require(story.getString("category").isNotBlank())
            require(story.optInt("order", index + 1) > 0)
            StoryEntity(story.getString("id"), story.getString("family"), story.getString("category"),
                story.getString("level"), story.toString())
        }
        val wordRows = (0 until lexicon.length()).mapNotNull { index ->
            val word = lexicon.getJSONObject(index)
            val language = Language.from(word.getString("language"))
            if (language != Courses.INDONESIAN_TO_FILIPINO.target) return@mapNotNull null
            listOf("translations", "meanings", "notes").forEach { field ->
                Language.entries.forEach { localized ->
                    require(value(word.getJSONObject(field), localized).isNotBlank())
                }
            }
            require(word.getString("pos").isNotBlank())
            LexemeEntity(language.code, Words.key(word.getString("word")), word.toString())
        }
        val keys = wordRows.map { "${it.language}:${it.word}" }.toSet()
        require(wordRows.size == keys.size) { "Duplicate dictionary entry" }
        storyRows.forEach { row ->
            val story = story(row)
            require(story.sentences.isNotEmpty() && story.sentences.size <= 300)
            Language.entries.forEach { language ->
                require(story.title(language).isNotBlank() && story.summary(language).isNotBlank())
                story.sentences.forEach { sentence ->
                    require(sentence.getValue(language.code).isNotBlank())
                }
                story.exercises.forEach { exercise ->
                    require(exercise.question(language).isNotBlank())
                    require(exercise.answer(language).isNotBlank())
                    require(exercise.explanation(language).isNotBlank())
                    if (exercise.type == ExerciseType.MULTIPLE_CHOICE || exercise.type == ExerciseType.GRAMMAR) {
                        require(exercise.choices(language).size >= 2)
                        require(exercise.answer(language) in exercise.choices(language))
                    }
                    if (exercise.type == ExerciseType.SENTENCE_CONSTRUCTION) require(exercise.tokens(language).isNotEmpty())
                }
            }
            val target = Courses.INDONESIAN_TO_FILIPINO.target
            story.sentences.forEach { sentence ->
                Words.tokens(sentence.getValue(target.code)).forEach { token ->
                    require("${target.code}:${Words.key(token.text)}" in keys) {
                        "Missing ${target.code} translation for ${token.text} in ${row.id}"
                    }
                }
            }
            story.focus(target).forEach { expression ->
                require("${target.code}:${Words.key(expression)}" in keys) {
                    "Missing ${target.code} focus expression $expression in ${row.id}"
                }
            }
        }
        return storyRows to wordRows
    }

    fun detail(lexeme: LexemeEntity, word: String, native: Language, example: String, translated: String): WordDetail {
        val json = JSONObject(lexeme.content)
        return WordDetail(word, value(json.getJSONObject("translations"), native),
            value(json.getJSONObject("meanings"), native), json.getString("pos"),
            buildString {
                val pronunciation = json.optString("pronunciation")
                if (pronunciation.isNotBlank()) append("Pelafalan: $pronunciation\n\n")
                append(value(json.getJSONObject("notes"), native))
            }, example, translated)
    }
}
