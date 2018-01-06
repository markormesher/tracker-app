package uk.co.markormesher.tracker.models

import android.content.ContentValues
import android.database.Cursor
import android.os.Parcel
import android.os.Parcelable
import org.joda.time.DateTime
import java.util.*

object LogEntryMeta {
	val ENTITY_NAME = "log_entry"
	val TABLE_NAME = "log_entries"
	val ID = "id"
	val TITLE = "title"
	val NOTE = "note"
	val START_TIME = "start_time"
}

data class LogEntry(
		val id: String = UUID.randomUUID().toString(),
		var title: String = "",
		var note: String? = null,
		var startTime: DateTime = DateTime.now()
): Parcelable {

	constructor(cursor: Cursor): this(
			cursor.getString(cursor.getColumnIndex(LogEntryMeta.ID)),
			cursor.getString(cursor.getColumnIndex(LogEntryMeta.TITLE)),
			cursor.getString(cursor.getColumnIndex(LogEntryMeta.NOTE)),
			DateTime(cursor.getLong(cursor.getColumnIndex(LogEntryMeta.START_TIME)))
	)

	constructor(parcel: Parcel): this(
			parcel.readString(),
			parcel.readString(),
			parcel.readString(),
			DateTime(parcel.readLong())
	)

	var endTime: DateTime? = null

	fun toContentValues(): ContentValues {
		val cv = ContentValues()
		cv.put(LogEntryMeta.ID, id)
		cv.put(LogEntryMeta.TITLE, title)
		cv.put(LogEntryMeta.NOTE, note)
		cv.put(LogEntryMeta.START_TIME, startTime.millis)
		return cv
	}

	override fun writeToParcel(parcel: Parcel, flags: Int) {
		parcel.writeString(id)
		parcel.writeString(title)
		parcel.writeString(note)
		parcel.writeLong(startTime.millis)
	}

	override fun describeContents(): Int {
		return 0
	}

	companion object CREATOR: Parcelable.Creator<LogEntry> {
		override fun createFromParcel(parcel: Parcel): LogEntry = LogEntry(parcel)
		override fun newArray(size: Int): Array<LogEntry?> = arrayOfNulls(size)
	}

}
