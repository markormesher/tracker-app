package uk.co.markormesher.tracker

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import uk.co.markormesher.tracker.helpers.pushData

class SyncService : JobService() {

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

	override fun onStartJob(params: JobParameters?): Boolean {
		pushData(false) { successful -> jobFinished(params, successful) }
		return true
	}

	override fun onStopJob(params: JobParameters?): Boolean {
		return true
	}

}
