package com.rolang.app.network

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rolang.app.diagnostics.DiagnosticLogger
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class DiagnosticLoggingTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test fun httpUrlsStatusesAndStackTracesArePersistedWithoutSecrets() = runBlocking {
        DiagnosticLogger.initialize(context)
        val server = MockWebServer().apply {
            enqueue(MockResponse().setResponseCode(503)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"error\":{\"code\":\"TEMPORARY\"}}"))
            start()
        }
        val querySecret = "query-secret-${UUID.randomUUID()}"
        val bearerSecret = "bearer-secret-${UUID.randomUUID()}"
        try {
            val error = runCatching {
                ApiClient(ServerConfig(serverBaseUrl = server.url("/").toString(), onlineEnabled = true))
                    .request("diagnostic", token = bearerSecret, query = mapOf("token" to querySecret))
            }.exceptionOrNull()
            assertTrue(error is ApiException)
            assertEquals(503, (error as ApiException).status)
            assertEquals("TEMPORARY", error.code)

            val marker = "stack-${UUID.randomUUID()}"
            DiagnosticLogger.error("TEST", marker, IllegalStateException("safe failure"))
            val directory = DiagnosticLogger.logDirectory(context)
            assertNotNull(directory)
            val text = directory!!.listFiles().orEmpty().filter { it.extension == "log" }
                .joinToString("\n") { it.readText() }
            assertTrue(text.contains("/api/v1/diagnostic"))
            assertTrue(text.contains("HTTP 503"))
            assertTrue(text.contains(marker))
            assertTrue(text.contains("IllegalStateException"))
            assertFalse(text.contains(querySecret))
            assertFalse(text.contains(bearerSecret))
            val export = DiagnosticLogger.createExport(context)
            assertNotNull(export)
            assertTrue(export!!.readText().contains(marker))
        } finally { server.shutdown() }
    }

    @Test fun corruptSessionStorageIsDiscardedWithoutThrowing() {
        val preferences = context.getSharedPreferences("online_account", Context.MODE_PRIVATE)
        assertTrue(preferences.edit().putInt("session", 42).commit())
        assertNull(SessionStore(context).read())
        assertFalse(preferences.contains("session"))
    }
}
