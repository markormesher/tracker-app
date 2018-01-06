package uk.co.markormesher.tracker

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import uk.co.markormesher.tracker.db.Database
import uk.co.markormesher.tracker.helpers.createActivityChooser
import uk.co.markormesher.tracker.models.LogEntry
import java.io.File

class MainActivity: AppCompatActivity(), LogEntryListAdapter.EventListener {

	private val listAdapter by lazy { LogEntryListAdapter(this, this) }
	private var viewState = ViewState.EMPTY
		set(value) {
			field = value
			updateView()
		}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		initView()
	}

	override fun onResume() {
		super.onResume()
		reloadLogEntries()
	}

	private fun initView() {
		setContentView(R.layout.activity_main)

		val layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
		listView.layoutManager = layoutManager
		listView.addItemDecoration(DividerItemDecoration(listView.context, layoutManager.orientation))
		listView.adapter = listAdapter

		fab.setOnClickListener { editLogEntry(null) }

		viewState = ViewState.EMPTY
	}

	private fun updateView() {
		loadingSpinner.visibility = if (viewState == ViewState.LOADING) {
			View.VISIBLE
		} else {
			View.GONE
		}

		listView.visibility = if (viewState == ViewState.LOADED) {
			View.VISIBLE
		} else {
			View.GONE
		}

		emptyMessage.visibility = if (viewState == ViewState.EMPTY) {
			View.VISIBLE
		} else {
			View.GONE
		}
	}

	override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
		menuInflater.inflate(R.menu.main_activity, menu)
		return true
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		when (item.itemId) {
			R.id.exportData -> {
				startDataExport()
				return true
			}
		}
		return super.onOptionsItemSelected(item)
	}

	override fun onLogEntryLongClick(logEntry: LogEntry) {
		editLogEntry(logEntry)
	}

	override fun onSwitchBtnClick() {
		switchActivity()
	}

	private fun reloadLogEntries() {
		if (viewState != ViewState.LOADING) {
			viewState = ViewState.LOADING
			Database.getInstance(this).getSortedLogEntries { entries ->
				with(listAdapter) {
					entries.forEachIndexed { index, logEntry ->
						if (index > 0) {
							logEntry.endTime = entries[index - 1].startTime
						}
					}
					logEntries.clear()
					logEntries.addAll(entries)
					notifyDataSetChanged()
				}

				viewState = if (entries.isEmpty()) {
					ViewState.EMPTY
				} else {
					ViewState.LOADED
				}
			}
		}
	}

	private fun editLogEntry(logEntry: LogEntry?) {
		val intent = Intent(this, EditLogEntryActivity::class.java)
		if (logEntry != null) {
			intent.putExtra("log_entry", logEntry)
		}
		startActivity(intent)
	}

	private fun switchActivity() {
		createActivityChooser(this, { activity ->
			if (activity != null) {
				Database.getInstance(this).saveLogEntry(LogEntry(title = activity), { reloadLogEntries() })
			}
		})
	}

	private fun startDataExport() {
		Toast.makeText(this, R.string.export_data_in_progress, Toast.LENGTH_SHORT).show()
		Database.getInstance(this).prepareDataDownload({ path ->
			if (path != null) {
				val shareIntent = Intent()
				shareIntent.action = Intent.ACTION_SEND
				shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(File(path)))
				shareIntent.type = "text/csv"
				startActivity(Intent.createChooser(shareIntent, getString(R.string.export_data_share)))
			} else {
				Toast.makeText(this, getString(R.string.export_data_failed), Toast.LENGTH_SHORT).show()
			}
		})
	}

	private enum class ViewState {
		LOADING,
		LOADED,
		EMPTY
	}

}
