package uk.co.markormesher.tracker.db

import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.Environment
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import uk.co.markormesher.tracker.models.LogEntry
import uk.co.markormesher.tracker.models.LogEntryMeta
import uk.co.markormesher.tracker.widget.WidgetProvider
import java.io.File

class Database private constructor(private val context: Context):
		SQLiteOpenHelper(context, Database.NAME, null, Database.VERSION) {

	companion object {
		private val NAME = "db"
		private val VERSION = 1

		private var instance: Database? = null
		fun getInstance(context: Context): Database {
			if (instance == null) {
				instance = Database(context)
			}
			return instance!!
		}

		val LOG_ENTRIES_UPDATED_ACTION = "uk.co.markormesher.tracker.LOG_ENTRIES_UPDATED"
	}

	override fun onCreate(db: SQLiteDatabase) {
		onUpgrade(db, 0, Database.VERSION)
	}

	override fun onUpgrade(db: SQLiteDatabase, from: Int, to: Int) {
		for (v in (from + 1)..to) {
			upgradeToVersion(v, db)
		}
	}

	private fun upgradeToVersion(version: Int, db: SQLiteDatabase) {
		when (version) {
			1 -> {
				db.execSQL("CREATE TABLE ${LogEntryMeta.TABLE_NAME} (" +
						"${LogEntryMeta.ID} TEXT PRIMARY KEY," +
						"${LogEntryMeta.TITLE} TEXT NOT NULL," +
						"${LogEntryMeta.NOTE} TEXT," +
						"${LogEntryMeta.START_TIME} NUMBER" +
						")")
			}
		}
	}

	fun getSortedLogEntries(callback: ((entries: List<LogEntry>) -> Unit)? = null) = doAsync {
		val output = ArrayList<LogEntry>()

		val cursor = readableDatabase.query(
				LogEntryMeta.TABLE_NAME,
				null, null, null, null, null, null
		)
		if (cursor.moveToFirst()) {
			do {
				output.add(LogEntry(cursor))
			} while (cursor.moveToNext())
		}
		cursor.close()

		uiThread { callback?.invoke(output.sortedByDescending { it.startTime.millis }) }
	}

	fun getCurrentLogEntry(callback: ((entry: LogEntry?) -> Unit)? = null) = doAsync {
		var output: LogEntry? = null
		val cursor = readableDatabase.query(
				LogEntryMeta.TABLE_NAME,
				null, null, null, null, null,
				"${LogEntryMeta.START_TIME} DESC", "1"
		)
		if (cursor.moveToFirst()) {
			output = LogEntry(cursor)
		}
		cursor.close()

		uiThread { callback?.invoke(output) }
	}

	fun saveLogEntry(logEntry: LogEntry, callback: ((successful: Boolean) -> Unit)? = null) = doAsync {
		writableDatabase.insertWithOnConflict(
				LogEntryMeta.TABLE_NAME,
				null,
				logEntry.toContentValues(),
				SQLiteDatabase.CONFLICT_REPLACE
		)

		val bIntent = Intent(context, WidgetProvider::class.java)
		bIntent.action = LOG_ENTRIES_UPDATED_ACTION
		context.sendBroadcast(bIntent)

		uiThread { callback?.invoke(true) }
	}

	fun deleteLogEntry(logEntry: LogEntry, callback: ((successful: Boolean) -> Unit)? = null) = doAsync {
		writableDatabase.delete(
				LogEntryMeta.TABLE_NAME,
				"${LogEntryMeta.ID} = ?",
				arrayOf(logEntry.id)
		)

		val bIntent = Intent(context, WidgetProvider::class.java)
		bIntent.action = LOG_ENTRIES_UPDATED_ACTION
		context.sendBroadcast(bIntent)

		uiThread { callback?.invoke(true) }
	}

	fun prepareExportData(callback: ((data: String) -> Unit)? = null) = doAsync {
		getSortedLogEntries({ entries ->
			val output = StringBuilder()

			entries.forEachIndexed { index, logEntry ->
				if (index > 0) {
					logEntry.endTime = entries[index - 1].startTime
				}
			}

			output.append("[")

			entries.forEach { logEntry ->
				with(logEntry) {
					title = title.replace("\"", "\\\"")
					note = note?.replace("\"", "\\\"")
					if (endTime == null) {
						endTime = DateTime.now()
					}
					output.append("{" +
							"\"id\": \"$id\"," +
							"\"title\": \"$title\"," +
							"\"note\": ${if (note.isNullOrBlank()) "null" else "\"$note\""}," +
							"\"startTime\": \"$startTime\"," +
							"\"endTime\": \"$endTime\"" +
							"},")
				}
			}

			output.setLength(output.length - 1)
			output.append("]")

			callback?.invoke(output.toString())
		})
	}

	fun prepareExportDataAsFile(callback: ((path: String?) -> Unit)? = null) = doAsync {
		prepareExportData({ data ->
			try {
				val timestamp = DateTime.now().toString(ISODateTimeFormat.dateTimeNoMillis())
				val folder = File(Environment.getExternalStorageDirectory().absolutePath, "/Tracker/")
				if (folder.exists() || folder.mkdirs()) {
					val file = File(folder, "tracker-output-$timestamp.json")
					file.writeText(data)
					uiThread { callback?.invoke(file.absolutePath) }
				} else {
					uiThread { callback?.invoke(null) }
				}
			} catch (e: Exception) {
				e.printStackTrace()
				uiThread { callback?.invoke(null) }
			}

		})
	}
}
