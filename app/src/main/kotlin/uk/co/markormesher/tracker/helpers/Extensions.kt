package uk.co.markormesher.tracker.helpers

import android.util.Log

inline fun consume(f: () -> Unit): Boolean {
	f()
	return true
}

fun Any.log(msg: String) = Log.d("TRACKER-APP", "${this.javaClass.name}: $msg")
