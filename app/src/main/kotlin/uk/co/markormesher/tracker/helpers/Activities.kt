package uk.co.markormesher.tracker.helpers

import uk.co.markormesher.tracker.R

val defaultActivities = arrayOf(
		"Commuting",
		"Downtime",
		"Exercise",
		"Personal development",
		"Personal projects",
		"Sleeping",
		"Work",
		"Work break",
		"Work travel"
)

val defaultActivityIcons = mapOf(
		Pair("Commuting", R.drawable.ic_directions_bus_white_24dp),
		Pair("Downtime", R.drawable.ic_sentiment_satisfied_white_24dp),
		Pair("Exercise", R.drawable.ic_fitness_center_white_24dp),
		Pair("Personal development", R.drawable.ic_trending_up_white_24dp),
		Pair("Personal projects", R.drawable.ic_build_white_24dp),
		Pair("Sleeping", R.drawable.ic_hotel_white_24dp),
		Pair("Work", R.drawable.ic_work_white_24dp),
		Pair("Work break", R.drawable.ic_pause_circle_outline_white_24dp),
		Pair("Work travel", R.drawable.ic_flight_takeoff_white_24dp)
)
