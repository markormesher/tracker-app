package uk.co.markormesher.tracker

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.main_activity.*
import uk.co.markormesher.tracker.db.Database
import uk.co.markormesher.tracker.helpers.*
import uk.co.markormesher.tracker.models.LogEntry
import uk.co.markormesher.tracker.models.LogEntryMeta

class MainActivity : AppCompatActivity(), LogEntryListAdapter.EventListener {

	private val requiredPermissions = arrayOf(
			Manifest.permission.WRITE_EXTERNAL_STORAGE,
			Manifest.permission.INTERNET
	)

	private val listAdapter by lazy { LogEntryListAdapter(this, this) }
	private var viewState = ViewState.EMPTY
		set(value) {
			field = value
			updateView()
		}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		initView()

		if (!checkPermissionList(requiredPermissions)) {
			requestPermissionList(requiredPermissions)
		}

		SyncService.schedule(this)
	}

	override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
		if (!checkPermissionRequestResult(requestCode, grantResults)) {
			requestPermissionList(requiredPermissions, true)
		}
	}

	override fun onResume() {
		super.onResume()
		reloadLogEntries()
	}

	private fun initView() {
		setContentView(R.layout.main_activity)

		logEntryRecyclerView.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
		logEntryRecyclerView.addItemDecoration(DividerItemDecoration(this, RecyclerView.VERTICAL))
		logEntryRecyclerView.adapter = listAdapter

		fabView.setOnClickListener { editLogEntry(null) }

		viewState = ViewState.EMPTY
	}

	private fun updateView() {
		loadingSpinnerView.visibility = if (viewState == ViewState.LOADING) {
			View.VISIBLE
		} else {
			View.GONE
		}

		logEntryRecyclerView.visibility = if (viewState == ViewState.LOADED) {
			View.VISIBLE
		} else {
			View.GONE
		}

		emptyMessageView.visibility = if (viewState == ViewState.EMPTY) {
			View.VISIBLE
		} else {
			View.GONE
		}
	}

	override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
		menu?.clear()
		menuInflater.inflate(R.menu.main_activity, menu)
		return true
	}

	override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
		R.id.pushData -> consume {
			pushData(true)
		}
		R.id.pullData -> consume {
			pullData()
		}
		else -> super.onOptionsItemSelected(item)
	}

	override fun onLogEntryLongClick(logEntry: LogEntry) {
		editLogEntry(logEntry)
	}

	override fun onSwitchBtnClick() {
		startActivity(Intent(this, LogEntryQuickChooserDialog::class.java))
	}

	private fun reloadLogEntries() {
		if (viewState == ViewState.LOADING) {
			return
		}

		viewState = ViewState.LOADING
		Database.getInstance(this).getSortedLogEntries { entries ->
			runOnUiThread {
				with(listAdapter) {
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
			intent.putExtra(LogEntryMeta.ENTITY_NAME, logEntry)
		}
		startActivity(intent)
	}

	private enum class ViewState {
		LOADING,
		LOADED,
		EMPTY
	}

}
