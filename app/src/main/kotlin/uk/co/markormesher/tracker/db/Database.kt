package uk.co.markormesher.tracker.db

import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.jetbrains.anko.doAsync
import uk.co.markormesher.tracker.models.LogEntry
import uk.co.markormesher.tracker.models.LogEntryMeta
import uk.co.markormesher.tracker.widget.WidgetProvider

private const val DB_NAME = "db"
private const val DB_VERSION = 1

class Database private constructor(private val context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

	companion object {
		const val LOG_ENTRIES_UPDATED_ACTION = "uk.co.markormesher.tracker.LOG_ENTRIES_UPDATED"

		private var instance: Database? = null
		fun getInstance(context: Context): Database {
			instance = instance ?: Database(context)
			return instance!!
		}
	}

	override fun onCreate(db: SQLiteDatabase) {
		onUpgrade(db, 0, DB_VERSION)
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

		val sortedOutput = output.sortedByDescending { it.startTime.millis }
		sortedOutput.forEachIndexed { index, logEntry ->
			if (index > 0) {
				logEntry.endTime = sortedOutput[index - 1].startTime
			}
		}

		callback?.invoke(sortedOutput)
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

		callback?.invoke(output)
	}

	fun saveLogEntry(logEntry: LogEntry, callback: (() -> Unit)? = null) = doAsync {
		writableDatabase.insertWithOnConflict(
				LogEntryMeta.TABLE_NAME,
				null,
				logEntry.toContentValues(),
				SQLiteDatabase.CONFLICT_REPLACE
		)

		sendUpdateBroadcast()

		callback?.invoke()
	}

	fun saveLogEntries(logEntries: List<LogEntry>, callback: (() -> Unit)? = null) = doAsync {
		logEntries.forEach { entry ->
			writableDatabase.insertWithOnConflict(
					LogEntryMeta.TABLE_NAME,
					null,
					entry.toContentValues(),
					SQLiteDatabase.CONFLICT_REPLACE
			)
		}

		sendUpdateBroadcast()

		callback?.invoke()
	}

	fun deleteLogEntry(logEntry: LogEntry, callback: (() -> Unit)? = null) = doAsync {
		writableDatabase.delete(
				LogEntryMeta.TABLE_NAME,
				"${LogEntryMeta.ID} = ?",
				arrayOf(logEntry.id)
		)

		sendUpdateBroadcast()

		callback?.invoke()
	}

	fun deleteAllLogEntries(callback: (() -> Unit)? = null) = doAsync {
		writableDatabase.delete(LogEntryMeta.TABLE_NAME, null, null)
		sendUpdateBroadcast()
		callback?.invoke()
	}

	private fun sendUpdateBroadcast() {
		val updateBroadcastIntent = Intent(context, WidgetProvider::class.java)
		updateBroadcastIntent.action = LOG_ENTRIES_UPDATED_ACTION
		context.sendBroadcast(updateBroadcastIntent)
	}

	fun getAllEntriesAsJsonString(callback: ((data: String) -> Unit)? = null) = doAsync {
		getSortedLogEntries { entries ->
			val output = entries.joinToString(
					prefix = "[", separator = ",", postfix = "]",
					transform = { it.toJsonString() }
			)

			callback?.invoke(output)
		}
	}
}
