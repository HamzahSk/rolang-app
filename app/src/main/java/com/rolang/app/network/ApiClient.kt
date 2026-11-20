package com.rolang.app.network

import com.rolang.app.diagnostics.DiagnosticLogger
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class ApiException(
    val status: Int,
    val code: String,
    val requestUrl: String = "",
    val method: String = "GET",
) : IOException("Server request failed ($status, $code).")
class ApiResponseException(message: String) : IOException(message)

class ApiClient(private val config: ServerConfig) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(config.connectionTimeoutMs, TimeUnit.MILLISECONDS)
        .readTimeout(config.requestTimeoutMs, TimeUnit.MILLISECONDS)
        .callTimeout(config.requestTimeoutMs, TimeUnit.MILLISECONDS)
        // Authentication must not follow a redirect to a different API host.
        .followRedirects(false).followSslRedirects(false).build()

    suspend fun request(path: String, method: String = "GET", body: JSONObject? = null,
        token: String? = null, query: Map<String, String> = emptyMap(), headers: Map<String, String> = emptyMap()): JSONObject {
        val url = config.apiUrl().newBuilder()
        path.split('/').filter { it.isNotEmpty() }.forEach(url::addPathSegment)
        query.forEach { (key, value) -> url.addQueryParameter(key, value) }
        val requestUrl = url.build()
        val diagnosticUrl = diagnosticUrl(requestUrl)
        val builder = Request.Builder().url(requestUrl).header("Accept", "application/json")
        headers.forEach { (name, value) -> builder.header(name, value) }
        if (token != null) builder.header("Authorization", "Bearer $token")
        val payload = body?.toString()?.toRequestBody("application/json; charset=utf-8".toMediaType())
        builder.method(method, if (method in setOf("POST", "PUT")) payload ?: "{}".toRequestBody("application/json".toMediaType()) else null)
        val call = client.newCall(builder.build())
        DiagnosticLogger.info("HTTP", "$method $diagnosticUrl")
        return suspendCancellableCoroutine { continuation ->
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (!continuation.isCancelled) {
                        DiagnosticLogger.error("HTTP", "$method $diagnosticUrl failed before receiving a response.", e)
                        continuation.resumeWithException(e)
                    }
                }
                override fun onResponse(call: Call, response: Response) {
                    response.use {
                        try {
                            DiagnosticLogger.info("HTTP", "$method $diagnosticUrl -> HTTP ${response.code}")
                            val source = response.body?.source()
                            if (source?.request(8_388_609) == true) throw ApiResponseException("Server response is too large.")
                            val raw = source?.readUtf8().orEmpty()
                            if (response.code == 304) {
                                if (!continuation.isCancelled) continuation.resume(JSONObject().put("notModified", true))
                                return
                            }
                            if (!response.isSuccessful) {
                                val code = runCatching { JSONObject(raw).optJSONObject("error")?.optString("code") }
                                    .getOrNull()?.takeIf { it.isNotBlank() } ?: "SERVER_ERROR"
                                throw ApiException(response.code, code, diagnosticUrl, method)
                            }
                            if (response.code != 204 && raw.isBlank()) throw ApiResponseException("Server returned an empty JSON response.")
                            val json = if (response.code == 204) JSONObject() else JSONObject(raw)
                            if (!continuation.isCancelled) continuation.resume(json)
                        } catch (e: Exception) {
                            if (!continuation.isCancelled) {
                                DiagnosticLogger.error("HTTP", "$method $diagnosticUrl could not be completed; HTTP ${response.code}.", e)
                                continuation.resumeWithException(e)
                            }
                        }
                    }
                }
            })
        }
    }

    private fun diagnosticUrl(url: HttpUrl): String {
        val safe = url.newBuilder().query(null)
        for (index in 0 until url.querySize) {
            val name = url.queryParameterName(index)
            val value = if (name.lowercase() in setOf("token", "password", "authorization", "secret", "key"))
                "<redacted>" else url.queryParameterValue(index)
            safe.addQueryParameter(name, value)
        }
        return safe.build().toString()
    }
}
