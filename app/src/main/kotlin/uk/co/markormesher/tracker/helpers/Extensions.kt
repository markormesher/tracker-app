package uk.co.markormesher.tracker.helpers

inline fun consume(f: () -> Unit): Boolean {
	f()
	return true
}
