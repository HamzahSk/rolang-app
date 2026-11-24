package com.rolang.app.network

import android.content.Context
import com.rolang.app.diagnostics.DiagnosticLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Cache
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

class ContentHttpException(val status: Int, message: String) : IOException(message)
class ContentValidationException(message: String, cause: Throwable? = null) : IOException(message, cause)

data class RemoteDocument(val bytes: ByteArray, val json: JSONObject)

class GitHubContentSource(
    context: Context,
    private val baseUrl: HttpUrl = DEFAULT_BASE_URL.toHttpUrl(),
    client: OkHttpClient? = null,
) {
    private val client = client ?: OkHttpClient.Builder()
        .cache(Cache(context.cacheDir.resolve("github-content"), 50L * 1024 * 1024))
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .callTimeout(35, TimeUnit.SECONDS)
        .build()

    fun resolve(relative: String, from: String = ""): HttpUrl {
        val clean = relative.replace('\\', '/')
        require(clean.isNotBlank() && !clean.startsWith('/') && clean.split('/').none { it == ".." }) {
            "Unsafe static-content path"
        }
        val parent = if (from.isBlank()) baseUrl else checkNotNull(baseUrl.resolve(from))
        return checkNotNull(parent.resolve(clean)) { "Invalid static-content path" }
    }

    suspend fun json(relative: String, expectedHash: String? = null, from: String = ""): RemoteDocument {
        val bytes = bytes(resolve(relative, from))
        if (!expectedHash.isNullOrBlank() && sha256(bytes) != expectedHash.lowercase()) {
            throw ContentValidationException("Content hash mismatch for $relative")
        }
        val json = try { JSONObject(bytes.toString(Charsets.UTF_8)) }
        catch (error: Exception) { throw ContentValidationException("Invalid JSON at $relative", error) }
        return RemoteDocument(bytes, json)
    }

    suspend fun bytes(url: HttpUrl): ByteArray = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url).header("Accept", "application/json,image/*;q=0.9,*/*;q=0.1").build()
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw ContentHttpException(response.code, "GitHub Raw returned HTTP ${response.code}")
                response.body?.bytes() ?: throw ContentValidationException("GitHub Raw returned an empty response")
            }
        } catch (error: IOException) {
            DiagnosticLogger.error("GITHUB_CONTENT", "Static content request failed for ${url.encodedPath}.", error)
            throw error
        }
    }

    companion object {
        const val DEFAULT_BASE_URL = "https://raw.githubusercontent.com/HamzahSk/rolang-server/data/"
        fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
            .digest(bytes).joinToString("") { "%02x".format(it) }
    }
}
