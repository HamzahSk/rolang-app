package com.rolang.app.diagnostics

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.os.Process
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.PrintWriter
import java.io.StringWriter
import java.time.Instant

/**
 * Small, dependency-free diagnostic logger that remains usable during process failure.
 * It never records request bodies or authentication headers and all writes are best effort.
 */
@SuppressLint("StaticFieldLeak") // Only applicationContext is retained for process-wide crash logging.
object DiagnosticLogger {
    private const val tag = "RoLang"
    private const val currentLog = "rolang.log"
    private const val maxLogBytes = 1_000_000L
    private const val retainedLogs = 4
    private val lock = Any()
    @Volatile private var context: Context? = null

    fun initialize(value: Context) {
        context = value.applicationContext
        installCrashHandler()
        info("APP", "Application started; pid=${Process.myPid()}")
    }

    fun info(area: String, message: String) {
        runCatching { write("INFO", area, sanitize(message)) }
            .onFailure { runCatching { Log.e(tag, "Diagnostic logging failed", it) } }
    }

    fun error(area: String, message: String, error: Throwable? = null) {
        runCatching {
            val details = if (error == null) sanitize(message) else buildString {
                append(sanitize(message))
                append('\n')
                append(safeStackTrace(error))
            }
            write("ERROR", area, details)
        }.onFailure { runCatching { Log.e(tag, "Diagnostic logging failed", it) } }
    }

    fun logDirectory(context: Context): File? = runCatching {
        context.getExternalFilesDir(null)?.resolve("logs")
    }.getOrNull()

    fun displayPath(context: Context): String = "Android/data/${context.packageName}/files/logs/"

    /** Creates one text file suitable for Android's share sheet. */
    fun createExport(context: Context): File? = runCatching {
        val logs = logDirectory(context)?.listFiles { file -> file.isFile && file.extension == "log" }
            ?.sortedBy { it.lastModified() }.orEmpty()
        val exportRoot = (context.externalCacheDir ?: context.cacheDir).resolve("diagnostics").apply { mkdirs() }
        val export = exportRoot.resolve("rolang-diagnostics-${System.currentTimeMillis()}.txt")
        FileOutputStream(export, false).bufferedWriter(Charsets.UTF_8).use { writer ->
            writer.appendLine("Ro-Lang diagnostic logs")
            writer.appendLine("Generated: ${Instant.now()}")
            writer.appendLine("Package: ${context.packageName}")
            writer.appendLine()
            if (logs.isEmpty()) writer.appendLine("No diagnostic entries are available.")
            logs.forEach { file ->
                writer.appendLine("===== ${file.name} =====")
                file.bufferedReader(Charsets.UTF_8).useLines { lines -> lines.forEach(writer::appendLine) }
                writer.appendLine()
            }
        }
        export
    }.onFailure { error("EXPORT", "Could not create a diagnostic export.", it) }.getOrNull()

    fun share(context: Context): Boolean = runCatching {
        val export = createExport(context) ?: return false
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", export)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newRawUri("Ro-Lang diagnostics", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(send, "Ekspor log diagnostik")
        if (context !is android.app.Activity) chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
        info("EXPORT", "Diagnostic logs were prepared for export.")
        true
    }.onFailure { error("EXPORT", "Could not open the diagnostic share sheet.", it) }.getOrDefault(false)

    private fun installCrashHandler() {
        runCatching {
            val current = Thread.getDefaultUncaughtExceptionHandler()
            if (current !is CrashHandler) Thread.setDefaultUncaughtExceptionHandler(CrashHandler(current))
        }.onFailure { error("CRASH_HANDLER", "Could not install the uncaught-exception logger.", it) }
    }

    private class CrashHandler(private val delegate: Thread.UncaughtExceptionHandler?) : Thread.UncaughtExceptionHandler {
        override fun uncaughtException(thread: Thread, throwable: Throwable) {
            runCatching { error("UNCAUGHT", "Uncaught exception on thread ${thread.name} (${thread.id}).", throwable) }
            val next = delegate?.takeUnless { it === this }
            if (next != null && runCatching { next.uncaughtException(thread, throwable) }.isSuccess) return
            Process.killProcess(Process.myPid())
            kotlin.system.exitProcess(10)
        }
    }

    private fun write(level: String, area: String, message: String) {
        val line = "${Instant.now()} [$level] [${sanitize(area)}] [${Thread.currentThread().name}] $message\n"
        runCatching {
            if (level == "ERROR") Log.e(tag, line) else Log.i(tag, line)
        }
        val app = context ?: return
        runCatching {
            synchronized(lock) {
                val directory = logDirectory(app) ?: app.filesDir.resolve("logs")
                if (!directory.exists() && !directory.mkdirs()) return@synchronized
                val file = directory.resolve(currentLog)
                if (file.exists() && file.length() + line.toByteArray(Charsets.UTF_8).size > maxLogBytes) rotate(directory)
                FileOutputStream(file, true).bufferedWriter(Charsets.UTF_8).use { it.append(line) }
            }
        }.onFailure {
            runCatching { Log.e(tag, "Diagnostic file write failed", it) }
        }
    }

    private fun rotate(directory: File) {
        directory.resolve("rolang-$retainedLogs.log").delete()
        for (index in retainedLogs - 1 downTo 1) {
            val source = directory.resolve("rolang-$index.log")
            if (source.exists()) source.renameTo(directory.resolve("rolang-${index + 1}.log"))
        }
        val active = directory.resolve(currentLog)
        if (active.exists()) active.renameTo(directory.resolve("rolang-1.log"))
    }

    private fun safeStackTrace(error: Throwable): String = runCatching {
        val text = StringWriter().also { error.printStackTrace(PrintWriter(it)) }.toString()
        sanitize(text)
    }.getOrElse { "${error.javaClass.name}: ${sanitize(error.message.orEmpty())}" }

    private fun sanitize(value: String): String {
        var result = value
        result = result.replace(Regex("(?i)(bearer\\s+)[A-Za-z0-9._~+/=-]+"), "\$1<redacted>")
        result = result.replace(
            Regex("(?i)([\\\"']?(?:password|token|authorization|secret|cookie)[\\\"']?\\s*[:=]\\s*[\\\"']?)([^\\\"',}\\s]+)"),
            "\$1<redacted>",
        )
        return result
    }
}
