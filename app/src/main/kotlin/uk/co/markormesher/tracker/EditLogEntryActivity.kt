package uk.co.markormesher.tracker

import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.edit_log_entry_activity.*
import org.joda.time.format.DateTimeFormat
import uk.co.markormesher.tracker.db.Database
import uk.co.markormesher.tracker.helpers.consume
import uk.co.markormesher.tracker.models.LogEntry
import uk.co.markormesher.tracker.models.LogEntryMeta

class EditLogEntryActivity : AppCompatActivity() {

	private lateinit var currentEntry: LogEntry
	private var newEntry = false

	private val dateFormat by lazy { DateTimeFormat.forPattern(getString(R.string.date_entry_joda_format)) }
	private val timeFormat by lazy { DateTimeFormat.forPattern(getString(R.string.time_entry_joda_format)) }

	private val titleChooserResultCode = 4957

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		currentEntry = intent?.extras?.getParcelable(LogEntryMeta.ENTITY_NAME) ?: LogEntry()
		newEntry = intent?.extras?.containsKey(LogEntryMeta.ENTITY_NAME) != true // false or null
		initView()
	}

	override fun onCreateOptionsMenu(menu: Menu): Boolean {
		if (newEntry) {
			menuInflater.inflate(R.menu.edit_log_entry_no_delete, menu)
		} else {
			menuInflater.inflate(R.menu.edit_log_entry, menu)
		}
		return true
	}

	override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
		R.id.save -> consume { saveEntry() }
		R.id.delete -> consume { deleteEntry() }
		else -> super.onOptionsItemSelected(item)
	}

	override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
		if (requestCode == titleChooserResultCode && resultCode == Activity.RESULT_OK) {
			if (data?.getStringExtra(LogEntryMeta.TITLE)?.isBlank() == false) {
				titleEdit.setText(data.getStringExtra(LogEntryMeta.TITLE))
			}
		} else {
			super.onActivityResult(requestCode, resultCode, data)
		}
	}

	private fun initView() {
		setContentView(R.layout.edit_log_entry_activity)

		titleEdit.setText(currentEntry.title)
		notesEdit.setText(currentEntry.note)
		updateStartDateText()
		updateStartTimeText()

		quickInputBtn.setOnClickListener { startTitleQuickEdit() }
		startDateEdit.setOnClickListener { editStartDate() }
		startTimeEdit.setOnClickListener { editStartTime() }
	}

	private fun updateStartDateText() {
		startDateEdit.setText(currentEntry.startTime.toString(dateFormat))
	}

	private fun updateStartTimeText() {
		startTimeEdit.setText(currentEntry.startTime.toString(timeFormat))
	}

	private fun startTitleQuickEdit() {
		val intent = Intent(this@EditLogEntryActivity, LogEntryQuickChooserDialog::class.java)
		intent.putExtra(LogEntryQuickChooserDialog.FOR_RESULT, true)
		startActivityForResult(intent, titleChooserResultCode)
	}

	private fun editStartDate() {
		val date = currentEntry.startTime
		DatePickerDialog(this, { _, year, month, day ->
			currentEntry.startTime = currentEntry.startTime
					.withYear(year)
					.withMonthOfYear(month + 1)
					.withDayOfMonth(day)
			updateStartDateText()
		}, date.year, date.monthOfYear - 1, date.dayOfMonth).show()
	}

	private fun editStartTime() {
		val time = currentEntry.startTime
		TimePickerDialog(this, { _, hour, minute ->
			currentEntry.startTime = currentEntry.startTime
					.withHourOfDay(hour)
					.withMinuteOfHour(minute)
			updateStartTimeText()
		}, time.hourOfDay, time.minuteOfHour, true).show()
	}

	private fun saveEntry() {
		currentEntry.title = titleEdit.text.toString().trim()
		currentEntry.note = notesEdit.text.toString().trim()
		Database.getInstance(this).saveLogEntry(currentEntry) { runOnUiThread { finish() } }
	}

	private fun deleteEntry() {
		AlertDialog.Builder(this)
				.setTitle(R.string.delete_confirm_title)
				.setMessage(R.string.delete_confirm_body)
				.setNegativeButton(R.string.no, null)
				.setPositiveButton(R.string.yes) { _, _ ->
					Database.getInstance(this).deleteLogEntry(currentEntry) { runOnUiThread { onBackPressed() } }
				}
				.create()
				.show()
	}
}
