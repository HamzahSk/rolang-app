package com.rolang.app.network

import android.content.Context
import androidx.work.*
import com.rolang.app.diagnostics.DiagnosticLogger
import kotlinx.coroutines.CancellationException
import java.util.concurrent.TimeUnit

class StoryDownloadWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        val id = inputData.getString("storyId") ?: return Result.failure()
        val requestId = inputData.getString("requestId") ?: return Result.failure()
        val stories = AppServices.get(applicationContext).stories
        val download = stories.dao.download(id) ?: return Result.success()
        if (download.requestId != requestId || download.status == "CANCELLED") return Result.success()
        return try {
            stories.refresh(id, requestId)
            Result.success()
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (error: Exception) {
            DiagnosticLogger.error("DOWNLOAD", "Story download $id failed on attempt ${runAttemptCount + 1}.", error)
            val retry = runAttemptCount < 8 && (error !is ApiException || error.status in setOf(408, 409, 429) || error.status >= 500)
                && error !is IllegalArgumentException
            stories.failDownload(id, requestId, retry)
            if (retry) Result.retry() else Result.failure()
        }
    }
}

object StoryDownloadScheduler {
    private fun name(id: String) = "rolang-story-$id"
    fun enqueue(context: Context, id: String, requestId: String) {
        val request = OneTimeWorkRequestBuilder<StoryDownloadWorker>()
            .addTag(name(id))
            .setInputData(workDataOf("storyId" to id, "requestId" to requestId))
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS).build()
        // A new request supersedes a cancelled/failed generation; repeating a tap uses the same ID.
        WorkManager.getInstance(context).enqueueUniqueWork(name(id) + "-$requestId", ExistingWorkPolicy.KEEP, request)
    }
    fun cancel(context: Context, id: String) { WorkManager.getInstance(context).cancelAllWorkByTag(name(id)) }
}
