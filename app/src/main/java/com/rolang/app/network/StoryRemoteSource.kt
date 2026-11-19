package com.rolang.app.network

import org.json.JSONObject

/** Public content never requires an account. HTTP cancellation propagates to OkHttp. */
class StoryRemoteSource(private val configuration: () -> ServerConfig) {
    @Volatile private var cachedClient: Pair<ServerConfig, ApiClient>? = null
    private fun api(): ApiClient {
        val config = configuration()
        check(config.onlineEnabled) { "The online library is disabled." }
        cachedClient?.takeIf { it.first == config }?.let { return it.second }
        return synchronized(this) {
            cachedClient?.takeIf { it.first == config }?.second
                ?: ApiClient(config).also { cachedClient = config to it }
        }
    }
    suspend fun catalog(after: String? = null): JSONObject {
        val query = mutableMapOf("limit" to configuration().storyPageSize.toString(),
            "language" to "tl", "difficulty" to "A1")
        after?.let { query["after"] = it }
        return api().request("stories", query = query)
    }
    suspend fun detail(id: String, version: String? = null) = api().request("stories/$id", query = mapOf("view" to "manifest"),
        headers = version?.let { mapOf("If-None-Match" to "\"$it\"") }.orEmpty())
    suspend fun chapter(id: String, chapterId: String, version: String? = null) =
        api().request("stories/$id/chapters/$chapterId",
            headers = version?.let { mapOf("If-None-Match" to "\"$it\"") }.orEmpty())
}
