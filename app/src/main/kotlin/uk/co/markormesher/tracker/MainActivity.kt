package uk.co.markormesher.tracker

import android.Manifest
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
import kotlinx.android.synthetic.main.main_activity.*
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import uk.co.markormesher.tracker.db.Database
import uk.co.markormesher.tracker.helpers.checkPermissionList
import uk.co.markormesher.tracker.helpers.checkPermissionRequestResult
import uk.co.markormesher.tracker.helpers.consume
import uk.co.markormesher.tracker.helpers.requestPermissionList
import uk.co.markormesher.tracker.models.LogEntry
import uk.co.markormesher.tracker.models.LogEntryMeta
import java.io.File

class MainActivity: AppCompatActivity(), LogEntryListAdapter.EventListener {

	private val requiredPermissions = arrayOf(
			Manifest.permission.WRITE_EXTERNAL_STORAGE,
			Manifest.permission.INTERNET
	)

	private val httpClient by lazy { OkHttpClient() }
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

		logEntryRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
		logEntryRecyclerView.addItemDecoration(DividerItemDecoration(this, LinearLayoutManager.VERTICAL))
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
		menuInflater.inflate(R.menu.main_activity, menu)
		return true
	}

	override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
		R.id.exportData -> consume { dataExport() }
		R.id.syncData -> consume { dataSync() }
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

	private fun editLogEntry(logEntry: LogEntry?) {
		val intent = Intent(this, EditLogEntryActivity::class.java)
		if (logEntry != null) {
			intent.putExtra(LogEntryMeta.ENTITY_NAME, logEntry)
		}
		startActivity(intent)
	}

	private fun dataExport() {
		Toast.makeText(this, R.string.export_data_in_progress, Toast.LENGTH_SHORT).show()
		Database.getInstance(this).prepareExportDataAsFile({ path ->
			if (path != null) {
				val shareIntent = Intent()
				shareIntent.action = Intent.ACTION_SEND
				shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(File(path)))
				shareIntent.type = "application/json"
				startActivity(Intent.createChooser(shareIntent, getString(R.string.export_data_share)))
			} else {
				Toast.makeText(this, getString(R.string.export_data_failed), Toast.LENGTH_SHORT).show()
			}
		})
	}

	private fun dataSync() {
		Toast.makeText(this, R.string.sync_data_in_progress, Toast.LENGTH_SHORT).show()
		Database.getInstance(this).prepareExportData({ data ->
			doAsync {
				val request = Request.Builder()
						.url("https://tracker.markormesher.co.uk/data")
						.post(RequestBody.create(MediaType.parse("application/octet-stream"), data))
						.build()
				val response = httpClient.newCall(request).execute()
				if (response.isSuccessful) {
					uiThread { Toast.makeText(this@MainActivity, R.string.sync_data_succeeded, Toast.LENGTH_SHORT).show() }
				} else {
					uiThread { Toast.makeText(this@MainActivity, R.string.sync_data_failed, Toast.LENGTH_SHORT).show() }
				}
				response.close()
			}
		})
	}

	private enum class ViewState {
		LOADING,
		LOADED,
		EMPTY
	}

}
