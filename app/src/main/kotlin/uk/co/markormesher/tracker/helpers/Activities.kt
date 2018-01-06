package uk.co.markormesher.tracker.helpers

import android.content.Context
import android.support.v7.app.AlertDialog
import uk.co.markormesher.tracker.R

val defaultActivities = arrayOf(
		"Downtime",
		"Exercise",
		"Personal development",
		"Personal projects",
		"Sleeping",
		"Travelling",
		"Work"
)

val defaultActivityIcons = mapOf(
		Pair("Downtime", R.drawable.ic_sentiment_satisfied_white_24dp),
		Pair("Exercise", R.drawable.ic_fitness_center_white_24dp),
		Pair("Personal development", R.drawable.ic_trending_up_white_24dp),
		Pair("Personal projects", R.drawable.ic_build_white_24dp),
		Pair("Sleeping", R.drawable.ic_hotel_white_24dp),
		Pair("Travelling", R.drawable.ic_flight_takeoff_white_24dp),
		Pair("Work", R.drawable.ic_work_white_24dp)
)

fun createActivityChooser(context: Context, onSelect: ((selectedValue: String?) -> Unit)?) {
	AlertDialog.Builder(context)
			.setItems(defaultActivities, { _, position ->
				onSelect?.invoke(defaultActivities[position])
			})
			.setOnCancelListener { onSelect?.invoke(null) }
			.create()
			.show()
}
