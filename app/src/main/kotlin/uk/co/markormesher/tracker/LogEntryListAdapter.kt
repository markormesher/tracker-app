package uk.co.markormesher.tracker

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.log_entry_list_item.view.*
import org.joda.time.DateTime
import org.joda.time.Period
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.PeriodFormat
import uk.co.markormesher.tracker.helpers.defaultActivityIcons
import uk.co.markormesher.tracker.models.LogEntry

class LogEntryListAdapter(private val context: Context, private val eventListener: EventListener? = null)
	: RecyclerView.Adapter<LogEntryListAdapter.ViewHolder>() {

	private val layoutInflater by lazy { LayoutInflater.from(context)!! }

	private val now = DateTime.now()
	private val durationFormat = PeriodFormat.wordBased()
	private val sameDayDateFormat by lazy { DateTimeFormat.forPattern(context.getString(R.string.same_day_joda_time_format)) }
	private val differentDayDateFormat by lazy { DateTimeFormat.forPattern(context.getString(R.string.different_day_joda_time_format)) }

	val logEntries = ArrayList<LogEntry>()

	override fun getItemCount() = logEntries.size

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogEntryListAdapter.ViewHolder {
		return ViewHolder(layoutInflater.inflate(R.layout.log_entry_list_item, parent, false))
	}

	override fun onBindViewHolder(holder: LogEntryListAdapter.ViewHolder, position: Int) {
		val logEntry = logEntries[position]

		holder.titleView.text = logEntry.title
		holder.iconView.setImageResource(defaultActivityIcons[logEntry.title] ?: R.drawable.ic_crop_free_white_24dp)
		if (logEntry.note.isNullOrEmpty()) {
			holder.noteView.visibility = View.GONE
		} else {
			holder.noteView.visibility = View.VISIBLE
			holder.noteView.text = logEntry.note
		}

		if (position == 0) {
			holder.currentActivityLabel.visibility = View.VISIBLE
			holder.switchBtn.visibility = View.VISIBLE
			holder.switchBtn.setOnClickListener { eventListener?.onSwitchBtnClick() }

			if (logEntry.startTime.year == now.year && logEntry.startTime.dayOfYear == now.dayOfYear) {
				holder.durationView.text = context.getString(R.string.log_entry_start_time, logEntry.startTime.toString(sameDayDateFormat))
			} else {
				holder.durationView.text = context.getString(R.string.log_entry_start_time, logEntry.startTime.toString(differentDayDateFormat))
			}
		} else {
			holder.currentActivityLabel.visibility = View.GONE
			holder.switchBtn.visibility = View.GONE
			holder.switchBtn.setOnClickListener(null)

			val duration = Period(logEntry.startTime, logEntry.endTime).withSeconds(0).withMillis(0)
			holder.durationView.text = duration.toString(durationFormat)
		}

		holder.rootView.setOnLongClickListener {
			if (eventListener != null) {
				eventListener.onLogEntryLongClick(logEntry)
				true
			} else {
				false
			}
		}
	}

	class ViewHolder(v: View): RecyclerView.ViewHolder(v) {
		val rootView = v
		val currentActivityLabel = v.currentActivityLabel!!
		val iconView = v.iconView!!
		val titleView = v.titleView!!
		val noteView = v.noteView!!
		val durationView = v.durationView!!
		val switchBtn = v.switchBtn!!
	}

	interface EventListener {
		fun onLogEntryLongClick(logEntry: LogEntry)
		fun onSwitchBtnClick()
	}

}
