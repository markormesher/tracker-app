package uk.co.markormesher.tracker.helpers

import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody

fun createUploadRequest(data: String, accessKey: String) = Request.Builder()
		.url("https://tracker.markormesher.co.uk/data")
		.header("Authorization", "Bearer $accessKey")
		.post(RequestBody.create(MediaType.parse("application/octet-stream"), data))
		.build()!!
