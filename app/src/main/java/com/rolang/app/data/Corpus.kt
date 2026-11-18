package com.rolang.app.data

import android.content.Context
import com.rolang.app.domain.Language
import com.rolang.app.domain.Level
import com.rolang.app.domain.Words
import org.json.JSONObject

enum class UnitKind(val label: String) { LESSON("Lesson"), DIALOGUE("Dialogue"), STORY("Reading") }

data class LearningNote(val title: Map<String, String>, val text: Map<String, String>) {
    fun title(language: Language) = title.getValue(language.code)
    fun text(language: Language) = text.getValue(language.code)
}

data class Exercise(val question: Map<String, String>, val answer: Map<String, String>) {
    fun question(language: Language) = question.getValue(language.code)
    fun answer(language: Language) = answer.getValue(language.code)
}

data class Story(
    val id: String, val family: String, val category: String, val level: Level,
    val titles: Map<String, String>, val summaries: Map<String, String>,
    val sentences: List<Map<String, String>>,
    val order: Int = Int.MAX_VALUE, val kind: UnitKind = UnitKind.STORY, val image: String = "",
    val speakers: List<String?> = emptyList(), val objectives: List<Map<String, String>> = emptyList(),
    val notes: List<LearningNote> = emptyList(), val exercises: List<Exercise> = emptyList(),
    val focus: Map<String, List<String>> = emptyMap(), val pronunciation: Map<String, String> = emptyMap(),
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
    private fun translations(obj: JSONObject) = Language.entries.associate { it.code to obj.getString(it.code) }
    fun story(entity: StoryEntity): Story {
        val json = JSONObject(entity.content)
        val sentences = json.getJSONArray("sentences")
        fun localizedList(field: String): List<Map<String, String>> = json.optJSONArray(field)?.let { array ->
            (0 until array.length()).map { translations(array.getJSONObject(it)) }
        }.orEmpty()
        fun notes() = json.optJSONArray("notes")?.let { array -> (0 until array.length()).map {
            val item = array.getJSONObject(it)
            LearningNote(translations(item.getJSONObject("title")), translations(item.getJSONObject("text")))
        } }.orEmpty()
        fun exercises() = json.optJSONArray("exercises")?.let { array -> (0 until array.length()).map {
            val item = array.getJSONObject(it)
            Exercise(translations(item.getJSONObject("question")), translations(item.getJSONObject("answer")))
        } }.orEmpty()
        val focus = json.optJSONObject("focus")?.let { obj -> Language.entries.associate { language ->
            language.code to obj.optJSONArray(language.code)?.let { array ->
                (0 until array.length()).map(array::getString)
            }.orEmpty()
        } }.orEmpty()
        return Story(entity.id, entity.family, entity.category, Level.valueOf(entity.level),
            translations(json.getJSONObject("title")), translations(json.getJSONObject("summary")),
            (0 until sentences.length()).map { translations(sentences.getJSONObject(it)) },
            json.optInt("order", Int.MAX_VALUE),
            runCatching { UnitKind.valueOf(json.optString("kind", "STORY")) }.getOrDefault(UnitKind.STORY),
            json.optString("image"),
            json.optJSONArray("speakers")?.let { array -> (0 until array.length()).map {
                if (array.isNull(it)) null else array.getString(it)
            } }.orEmpty(), localizedList("objectives"), notes(), exercises(), focus,
            json.optJSONObject("pronunciation")?.let(::translations).orEmpty())
    }

    fun load(context: Context): Pair<List<StoryEntity>, List<LexemeEntity>> {
        val root = JSONObject(context.assets.open("corpus.json").bufferedReader().use { it.readText() })
        return parse(root)
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
        val wordRows = (0 until lexicon.length()).map { index ->
            val word = lexicon.getJSONObject(index)
            Language.entries.forEach { language ->
                listOf("translations", "meanings", "notes").forEach { field ->
                    require(word.getJSONObject(field).getString(language.code).isNotBlank())
                }
            }
            require(word.getString("pos").isNotBlank())
            LexemeEntity(word.getString("language"), Words.key(word.getString("word")), word.toString())
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
                    Words.tokens(sentence.getValue(language.code)).forEach { token ->
                        require("${language.code}:${Words.key(token.text)}" in keys) {
                            "Missing ${language.code} translation for ${token.text} in ${row.id}"
                        }
                    }
                }
                story.focus(language).forEach { expression ->
                    require("${language.code}:${Words.key(expression)}" in keys) {
                        "Missing ${language.code} focus expression $expression in ${row.id}"
                    }
                }
            }
        }
        return storyRows to wordRows
    }

    fun detail(lexeme: LexemeEntity, word: String, native: Language, example: String, translated: String): WordDetail {
        val json = JSONObject(lexeme.content)
        return WordDetail(word, json.getJSONObject("translations").getString(native.code),
            json.getJSONObject("meanings").getString(native.code), json.getString("pos"),
            buildString {
                val pronunciation = json.optString("pronunciation")
                if (pronunciation.isNotBlank()) append(when (native) {
                    Language.INDONESIAN -> "Pelafalan: $pronunciation\n\n"
                    Language.TAGALOG -> "Bigkas: $pronunciation\n\n"
                    Language.ENGLISH -> "Pronunciation: $pronunciation\n\n"
                })
                append(json.getJSONObject("notes").getString(native.code))
            }, example, translated)
    }
}
