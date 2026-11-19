package com.rolang.app.data

import androidx.room.withTransaction
import org.json.JSONArray
import org.json.JSONObject

class StoryLocalSource(private val database: LearningDatabase) {
    val dao = database.dao()
    suspend fun story(id: String) = dao.cachedStory(id)
    suspend fun store(id: String, detail: JSONObject, chapterIds: List<String>) {
        // Validate every translation and dictionary link before replacing a readable snapshot.
        val (stories, words) = Corpus.parse(JSONObject().put("stories", JSONArray().put(detail))
            .put("lexicon", detail.getJSONArray("vocabulary")))
        require(stories.single().id == id)
        database.withTransaction {
            dao.removeStoryLexicon(id)
            dao.cacheStoryLexicon(words.map { StoryLexemeEntity(id, it.language, it.word, it.content) })
            // Reviews still use the shared dictionary for distractors and grammar practice.
            dao.seedLexicon(words)
            dao.seedStories(stories.map { it.copy(remote = true) })
            dao.pruneChapters(id, chapterIds)
        }
    }
    suspend fun remove(id: String) = database.withTransaction {
        dao.removeStoryCache(id)
        dao.removeStoryLexicon(id)
        dao.removeChapters(id)
        dao.removeDownload(id)
        // Keep dictionary entries needed by saved words; release unused content.
        database.openHelper.writableDatabase.execSQL("""DELETE FROM lexicon WHERE NOT EXISTS
            (SELECT 1 FROM story_lexicon s WHERE s.language=lexicon.language AND s.word=lexicon.word)
            AND NOT EXISTS (SELECT 1 FROM vocabulary v WHERE v.language=lexicon.language AND v.word=lexicon.word)""")
    }
}
