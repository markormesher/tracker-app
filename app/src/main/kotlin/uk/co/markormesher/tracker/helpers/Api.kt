package uk.co.markormesher.tracker.helpers

import android.content.Context
import android.widget.Toast
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.runOnUiThread
import org.json.JSONArray
import uk.co.markormesher.tracker.R
import uk.co.markormesher.tracker.db.Database
import uk.co.markormesher.tracker.models.LogEntry

private val httpClient by lazy { OkHttpClient() }

fun createPullRequest(): Request = Request.Builder()
		.url("https://tracker.markormesher.co.uk/data")
		.build()

fun createPushRequest(data: String, accessKey: String): Request = Request.Builder()
		.url("https://tracker.markormesher.co.uk/data")
		.header("Authorization", "Bearer $accessKey")
		.post(data.toRequestBody("application/octet-stream".toMediaType()))
		.build()

fun Context.pullData() {
	Toast.makeText(this, R.string.pull_data_in_progress, Toast.LENGTH_SHORT).show()

	val ctx = this
	doAsync {
		val response = httpClient.newCall(createPullRequest()).execute()
		if (response.isSuccessful) {
			val rawResponseBody = response.body!!.string()
			val jsonObjects = JSONArray(rawResponseBody)
			val logEntries = IntRange(0, jsonObjects.length() - 1).map { i -> LogEntry(jsonObjects.getJSONObject(i)) }
			Database.getInstance(ctx).deleteAllLogEntries {
				Database.getInstance(ctx).saveLogEntries(logEntries) {
					runOnUiThread {
						Toast.makeText(ctx, R.string.pull_data_succeeded, Toast.LENGTH_SHORT).show()
					}
				}
			}
		} else {
			runOnUiThread {
				Toast.makeText(ctx, R.string.pull_data_failed, Toast.LENGTH_SHORT).show()
			}
		}
		response.close()
	}
}

fun Context.pushData(interactive: Boolean = false, onComplete: ((successful: Boolean) -> Unit)? = null) {
	val accessKey = getAccessKey()
	if (accessKey == null) {
		if (interactive) {
			promptForAccessKey { pushData(interactive) }
		}
		return
	}

	if (interactive) {
		Toast.makeText(this, R.string.push_data_in_progress, Toast.LENGTH_SHORT).show()
	}

	val ctx = this
	Database.getInstance(ctx).getAllEntriesAsJsonString { data ->
		doAsync {
			val response = httpClient.newCall(createPushRequest(data, accessKey)).execute()
			if (response.isSuccessful) {
				runOnUiThread {
					onComplete?.invoke(true)
					if (interactive) {
						Toast.makeText(ctx, R.string.push_data_succeeded, Toast.LENGTH_SHORT).show()
					}
				}
			} else {
				if (response.code == 403) {
					setAccessKey(null)
				}
				runOnUiThread {
					onComplete?.invoke(false)
					if (interactive) {
						Toast.makeText(ctx, R.string.push_data_failed, Toast.LENGTH_SHORT).show()
					}
				}
			}
			response.close()
		}
	}
}
