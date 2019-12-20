package uk.co.markormesher.tracker.helpers

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import uk.co.markormesher.tracker.R

private const val permissionRequestCode = 1415

private fun shouldUseRuntimePermissions(): Boolean {
	return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
}

private fun hasPermission(context: Context, permission: String): Boolean {
	return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}

fun Activity.checkPermissionList(permissions: Array<String>): Boolean {
	if (!shouldUseRuntimePermissions()) return true
	return permissions.all { p -> hasPermission(this, p) }
}

fun Activity.requestPermissionList(permissions: Array<String>, reRequest: Boolean = false) {
	val alertBuilder = AlertDialog.Builder(this)
	with(alertBuilder) {
		setTitle(R.string.permissions_request_title)
		if (reRequest) {
			setMessage(R.string.permissions_request_failure)
			setPositiveButton(R.string.yes) { _, _ -> doRequestPermissionList(permissions) }
			setNegativeButton(R.string.no) { _, _ -> finish() }
		} else {
			setMessage(R.string.permissions_request_primer)
			setPositiveButton(R.string.ok) { _, _ -> doRequestPermissionList(permissions) }
			setOnCancelListener { finish() }
		}
		create().show()
	}
}

private fun Activity.doRequestPermissionList(permissions: Array<String>) {
	ActivityCompat.requestPermissions(this, permissions, permissionRequestCode)
}

fun checkPermissionRequestResult(requestCode: Int, grantResults: IntArray): Boolean {
	if (requestCode != permissionRequestCode) return false
	if (grantResults.isEmpty()) return false
	return grantResults.none { it != PackageManager.PERMISSION_GRANTED }
}
