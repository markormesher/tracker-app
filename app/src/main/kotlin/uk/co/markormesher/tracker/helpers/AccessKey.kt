package uk.co.markormesher.tracker.helpers

import android.content.Context
import android.text.InputType
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import org.jetbrains.anko.defaultSharedPreferences
import uk.co.markormesher.tracker.R

fun Context.getAccessKey(): String? {
	val rawKey = defaultSharedPreferences.getString("access_key", null)
	return if (rawKey.isNullOrBlank()) {
		null
	} else {
		rawKey
	}
}

fun Context.setAccessKey(key: String?) {
	defaultSharedPreferences.edit().putString("access_key", key).apply()
}

fun Context.promptForAccessKey(callback: (() -> Unit)? = null) {
	val input = EditText(this)
	input.hint = getString(R.string.access_key_hint)
	input.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
	val builder = AlertDialog.Builder(this)
	builder
			.setTitle(R.string.access_key_title)
			.setView(input)
			.setPositiveButton(R.string.ok) { _, _ ->
				val key = input.text.toString().trim()
				if (key.isBlank()) {
					setAccessKey(null)
				} else {
					setAccessKey(key)
					callback?.invoke()
				}
			}
			.setNegativeButton(R.string.cancel, null)
			.create().show()
}
