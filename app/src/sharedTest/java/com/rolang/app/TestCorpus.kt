package com.rolang.app

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import com.rolang.app.data.*
import org.json.JSONObject

/** The same validated offline corpus is used by production, Robolectric, and device tests. */
object TestCorpus {
    fun json(context: Context): JSONObject {
        val stream =
            TestCorpus::class.java.classLoader?.getResourceAsStream("corpus.json")
                ?: InstrumentationRegistry.getInstrumentation()
                    .context
                    .assets
                    .open("story-fixture.json")
        return JSONObject(stream.bufferedReader().use { it.readText() })
    }

    fun load(context: Context) = Corpus.parse(json(context))

    suspend fun seed(dao: LearningDao, context: Context) {
        val (stories, words) = load(context)
        dao.seedStories(stories)
        dao.seedLexicon(words)
        dao.cacheCatalog(stories.map { StoryCatalogEntity(it.id, it.content) })
    }
}
