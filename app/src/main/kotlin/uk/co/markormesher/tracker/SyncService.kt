package uk.co.markormesher.tracker

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import okhttp3.OkHttpClient
import org.jetbrains.anko.doAsync
import uk.co.markormesher.tracker.db.Database
import uk.co.markormesher.tracker.helpers.createUploadRequest
import uk.co.markormesher.tracker.helpers.getAccessKey

class SyncService: JobService() {

	companion object {
		fun schedule(context: Context) {
			val jobId = 1
			val scheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler

			if (scheduler.getPendingJob(jobId) == null) {
				val jobInfo = JobInfo.Builder(jobId, ComponentName(context, SyncService::class.java))
						.setPeriodic(1000 * 60 * 60 * 2, 1000 * 60 * 60) // 2h period, 1h flex
						.setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED)
						.build()
				scheduler.schedule(jobInfo)
			}
		}
	}

	private val httpClient by lazy { OkHttpClient() }

	override fun onStartJob(params: JobParameters?): Boolean {
		// if we don't have the access key yet, give up until the user syncs manually
		val accessKey = getAccessKey() ?: return false

		Database.getInstance(baseContext).prepareExportData { data ->
			doAsync {
				val request = createUploadRequest(data, accessKey)
				val response = httpClient.newCall(request).execute()
				if (response.isSuccessful) {
					jobFinished(params, false)
				} else {
					// reschedule only if this was a real failure, not just a bad access key
					if (response.code() == 403) {
						jobFinished(params, false)
					} else {
						jobFinished(params, true)
					}
				}
				response.close()
			}
		}
		return true
	}

	override fun onStopJob(params: JobParameters?): Boolean {
		return true
	}

}
