package uk.co.markormesher.tracker

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.log_entry_quick_chooser_dialog.*
import kotlinx.android.synthetic.main.log_entry_quick_chooser_list_item.view.*
import uk.co.markormesher.tracker.db.Database
import uk.co.markormesher.tracker.helpers.defaultActivities
import uk.co.markormesher.tracker.helpers.defaultActivityIcons
import uk.co.markormesher.tracker.models.LogEntry
import uk.co.markormesher.tracker.models.LogEntryMeta

class LogEntryQuickChooserDialog: AppCompatActivity() {

	companion object {
		val FOR_RESULT = "for_result"
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setFinishOnTouchOutside(true)
		setContentView(R.layout.log_entry_quick_chooser_dialog)

		quickChooserRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
		quickChooserRecyclerView.addItemDecoration(DividerItemDecoration(this, LinearLayoutManager.VERTICAL))
		quickChooserRecyclerView.adapter = ListAdapter()
	}

	private fun onTitleSelected(title: String) {
		if (intent.extras?.getBoolean(FOR_RESULT) == true) {
			val data = Intent()
			data.putExtra(LogEntryMeta.TITLE, title)
			setResult(Activity.RESULT_OK, data)
			finish()
		} else {
			Database.getInstance(this).saveLogEntry(LogEntry(title = title), { finish() })
		}
	}

	private inner class ListAdapter: RecyclerView.Adapter<ListAdapter.ViewHolder>() {

		private val layoutInflater by lazy { LayoutInflater.from(this@LogEntryQuickChooserDialog)!! }

		override fun getItemCount() = defaultActivities.size

		override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
			return ViewHolder(layoutInflater.inflate(R.layout.log_entry_quick_chooser_list_item, parent, false))
		}

		override fun onBindViewHolder(holder: ViewHolder, position: Int) {
			val title = defaultActivities[position]
			val icon = defaultActivityIcons[title] ?: R.drawable.ic_crop_free_white_24dp

			holder.titleView.text = title
			holder.iconView.setImageResource(icon)
			holder.rootView.setOnClickListener { onTitleSelected(title) }
		}

		private inner class ViewHolder(val rootView: View): RecyclerView.ViewHolder(rootView) {
			val iconView = rootView.iconView!!
			val titleView = rootView.titleView!!
		}
	}
}
