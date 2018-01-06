package uk.co.markormesher.tracker

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import kotlinx.android.synthetic.main.activity_edit.*
import org.joda.time.DateTime
import uk.co.markormesher.tracker.db.Database
import uk.co.markormesher.tracker.helpers.createActivityChooser
import uk.co.markormesher.tracker.models.LogEntry

class EditLogEntryActivity: AppCompatActivity() {

	private lateinit var currentEntry: LogEntry
	private var newEntry = false

	private var startEditYear = 0
	private var startEditMonth = 0
	private var startEditDate = 0
	private var startEditHour = 0
	private var startEditMinute = 0

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		currentEntry = intent?.extras?.getParcelable("log_entry") ?: LogEntry()
		newEntry = !(intent?.extras?.containsKey("log_entry") ?: false)
		initView()
	}

	override fun onCreateOptionsMenu(menu: Menu): Boolean {
		if (newEntry) {
			menuInflater.inflate(R.menu.create_log_entry, menu)
		} else {
			menuInflater.inflate(R.menu.edit_log_entry, menu)
		}
		return true
	}

	override fun onOptionsItemSelected(item: MenuItem?): Boolean {
		when (item?.itemId) {
			R.id.save -> save()
			R.id.delete -> delete()
		}

		return super.onOptionsItemSelected(item)
	}

	private fun initView() {
		setContentView(R.layout.activity_edit)

		titleEdit.setText(currentEntry.title)
		startEditYear = currentEntry.startTime.year
		startEditMonth = currentEntry.startTime.monthOfYear
		startEditDate = currentEntry.startTime.dayOfMonth
		startEditHour = currentEntry.startTime.hourOfDay
		startEditMinute = currentEntry.startTime.minuteOfHour

		updateStartDateText()
		updateStartTimeText()

		startDate.setOnClickListener {
			DatePickerDialog(this, DatePickerDialog.OnDateSetListener { _, year, month, day ->
				startEditYear = year
				startEditMonth = month + 1
				startEditDate = day
				updateStartDateText()
			}, startEditYear, startEditMonth - 1, startEditDate).show()
		}

		startTime.setOnClickListener {
			TimePickerDialog(this, TimePickerDialog.OnTimeSetListener { _, hour, minute ->
				startEditHour = hour
				startEditMinute = minute
				updateStartTimeText()
			}, startEditHour, startEditMinute, true).show()
		}

		quickInputBtn.setOnClickListener {
			createActivityChooser(this, { activity ->
				if (activity != null) {
					titleEdit.setText(activity)
				}
			})
		}
	}

	private fun updateStartDateText() {
		startDate.setText(getString(R.string.date_entry_format, startEditYear, startEditMonth, startEditDate))
	}

	private fun updateStartTimeText() {
		startTime.setText(getString(R.string.time_entry_format, startEditHour, startEditMinute))
	}

	private fun save() {
		currentEntry.title = titleEdit.text.toString().trim()
		currentEntry.note = notesEdit.text.toString().trim()
		currentEntry.startTime = DateTime(startEditYear, startEditMonth, startEditDate, startEditHour, startEditMinute)
		Database.getInstance(this).saveLogEntry(currentEntry, { onBackPressed() })
	}

	private fun delete() {
		AlertDialog.Builder(this)
				.setTitle(R.string.delete_confirm_title)
				.setMessage(R.string.delete_confirm_body)
				.setNegativeButton(R.string.no, null)
				.setPositiveButton(R.string.yes, { _, _ ->
					Database.getInstance(this).deleteLogEntry(currentEntry, { onBackPressed() })
				})
				.create()
				.show()
	}
}
