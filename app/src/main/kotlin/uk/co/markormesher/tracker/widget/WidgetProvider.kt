package uk.co.markormesher.tracker.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import uk.co.markormesher.tracker.MainActivity
import uk.co.markormesher.tracker.R
import uk.co.markormesher.tracker.NewLogEntryThinActivity
import uk.co.markormesher.tracker.db.Database

class WidgetProvider: AppWidgetProvider() {

	override fun onReceive(context: Context, intent: Intent?) {
		super.onReceive(context, intent)
		if (intent?.action == Database.LOG_ENTRIES_UPDATED_ACTION) {
			val widgetManager = context.getSystemService(Context.APPWIDGET_SERVICE) as AppWidgetManager
			val name = ComponentName(context.applicationContext.packageName, WidgetProvider::class.java.name)
			val widgetIds = widgetManager.getAppWidgetIds(name)
			onUpdate(context, widgetManager, widgetIds)
		}
	}

	override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
		val sameDayDateFormat = DateTimeFormat.forPattern(context.getString(R.string.same_day_joda_time_format))
		val differentDayDateFormat = DateTimeFormat.forPattern(context.getString(R.string.different_day_joda_time_format))

		Database.getInstance(context).getCurrentLogEntry { entry ->
			val now = DateTime.now()
			val entryTitle = entry?.title ?: context.getString(R.string.no_log_entries)
			val entryStart = if (entry != null) {
				if (entry.startTime.year == now.year && entry.startTime.dayOfYear == now.dayOfYear) {
					entry.startTime.toString(sameDayDateFormat)
				} else {
					entry.startTime.toString(differentDayDateFormat)
				}
			} else {
				""
			}

			appWidgetIds.forEach { appWidgetId ->
				val switchIntent = Intent(context, NewLogEntryThinActivity::class.java)
				val switchPendingIntent = PendingIntent.getActivity(context, 0, switchIntent, 0)
				val openIntent = Intent(context, MainActivity::class.java)
				val openPendingIntent = PendingIntent.getActivity(context, 0, openIntent, 0)

				val views = RemoteViews(context.packageName, R.layout.widget)
				views.setTextViewText(R.id.widgetActivityTitle, entryTitle)
				if (entryStart.isNullOrEmpty()) {
					views.setViewVisibility(R.id.widgetActivityStartTime, View.GONE)
					views.setTextViewText(R.id.widgetActivityStartTime, entryStart)
				} else {
					views.setViewVisibility(R.id.widgetActivityStartTime, View.VISIBLE)
					views.setTextViewText(R.id.widgetActivityStartTime, context.getString(R.string.log_entry_start_time, entryStart))
				}

				views.setOnClickPendingIntent(R.id.widgetSwitchBtnWrapper, switchPendingIntent)
				views.setOnClickPendingIntent(R.id.widgetTextWrapper, openPendingIntent)

				appWidgetManager.updateAppWidget(appWidgetId, views)
			}
		}
	}
}

