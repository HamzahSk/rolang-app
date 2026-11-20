package com.rolang.app.network

import android.content.Context
import com.rolang.app.diagnostics.DiagnosticLogger
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.json.JSONObject
import java.io.File
import java.io.IOException

class ServerConfigurationException(message: String, cause: Throwable) : IOException(message, cause)

data class ServerConfig(
    val serverBaseUrl: String = "", val apiVersion: String = "v1",
    val connectionTimeoutMs: Long = 10_000, val requestTimeoutMs: Long = 30_000,
    val onlineEnabled: Boolean = false, val automaticSyncEnabled: Boolean = true,
    val syncIntervalMinutes: Long = 15, val storyPageSize: Int = 30,
) {
    fun apiUrl(): HttpUrl {
        val url = requireNotNull(serverBaseUrl.toHttpUrlOrNull()) { "Configure a server URL." }
        require(url.isHttps || url.host in setOf("localhost", "127.0.0.1", "10.0.2.2")) { "The server must use HTTPS." }
        require(url.username.isEmpty() && url.password.isEmpty() && url.query == null && url.fragment == null)
        require(url.encodedPath == "/") { "The base URL must be an origin without an API path." }
        return url.newBuilder().addPathSegment("api").addPathSegment(apiVersion).addPathSegment("").build()
    }
    companion object {
        fun parse(json: String): ServerConfig {
            val root = JSONObject(json)
            val config = ServerConfig(root.getString("serverBaseUrl"), root.getString("apiVersion"),
                root.getLong("connectionTimeoutMs"), root.getLong("requestTimeoutMs"),
                root.getBoolean("onlineEnabled"), root.getBoolean("automaticSyncEnabled"),
                root.optLong("syncIntervalMinutes", 15), root.optInt("storyPageSize", 30))
            require(config.apiVersion.matches(Regex("v[1-9][0-9]*")))
            require(config.connectionTimeoutMs in 1_000..120_000 && config.requestTimeoutMs in 1_000..120_000)
            require(config.syncIntervalMinutes in 15..1440 && config.storyPageSize in 1..100)
            if (config.onlineEnabled) config.apiUrl()
            return config
        }
        fun load(context: Context): ServerConfig {
            // A private app-files override can be provisioned without rebuilding.
            // Read it on every sync; configuration never contains authentication.
            val override = File(context.filesDir, "config.json")
            if (override.isFile) {
                runCatching { parse(override.readText()) }
                    .onSuccess {
                        DiagnosticLogger.info("CONFIG", "Loaded the private server configuration for ${it.serverBaseUrl}.")
                        return it
                    }
                    .onFailure {
                        DiagnosticLogger.error("CONFIG", "The private server configuration is invalid; using the packaged configuration.", it)
                    }
            }
            return try {
                context.assets.open("config.json").bufferedReader().use { parse(it.readText()) }.also {
                    DiagnosticLogger.info("CONFIG", "Loaded the packaged server configuration for ${it.serverBaseUrl}.")
                }
            } catch (error: Exception) {
                DiagnosticLogger.error("CONFIG", "The packaged server configuration is invalid.", error)
                throw ServerConfigurationException("The packaged server configuration is invalid.", error)
            }
        }
    }
}
