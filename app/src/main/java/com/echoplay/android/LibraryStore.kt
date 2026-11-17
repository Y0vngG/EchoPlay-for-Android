package com.echoplay.android

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.util.UUID

class LibraryStore(context: Context) :
    SQLiteOpenHelper(context, "echoplay-library.db", null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        createLibraryTable(db, "library")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS library")
        createLibraryTable(db, "library")
    }

    private fun createLibraryTable(db: SQLiteDatabase, tableName: String) {
        db.execSQL(
            """CREATE TABLE $tableName (
                bvid TEXT NOT NULL,
                page_number INTEGER NOT NULL,
                title TEXT NOT NULL,
                author TEXT NOT NULL,
                duration TEXT NOT NULL,
                cover_url TEXT NOT NULL,
                page_url TEXT NOT NULL,
                part_title TEXT NOT NULL,
                video_title TEXT NOT NULL,
                library_entry_id TEXT NOT NULL PRIMARY KEY,
                collection_id TEXT
            )""".trimIndent(),
        )
        db.execSQL(
            "CREATE UNIQUE INDEX ${tableName}_standalone_identity " +
                "ON $tableName (bvid, page_number) WHERE collection_id IS NULL",
        )
        db.execSQL(
            "CREATE UNIQUE INDEX ${tableName}_collection_page_identity " +
                "ON $tableName (collection_id, page_number) WHERE collection_id IS NOT NULL",
        )
    }

    fun tracks(): List<Track> = readableDatabase.rawQuery(
        "SELECT * FROM library ORDER BY rowid ASC",
        null,
    ).use { cursor ->
        buildList {
            while (cursor.moveToNext()) {
                add(
                    Track(
                        bvid = cursor.getString(cursor.getColumnIndexOrThrow("bvid")),
                        title = cursor.getString(cursor.getColumnIndexOrThrow("title")),
                        author = cursor.getString(cursor.getColumnIndexOrThrow("author")),
                        duration = cursor.getString(cursor.getColumnIndexOrThrow("duration")),
                        coverUrl = cursor.getString(cursor.getColumnIndexOrThrow("cover_url")),
                        pageUrl = cursor.getString(cursor.getColumnIndexOrThrow("page_url")),
                        pageNumber = cursor.getInt(cursor.getColumnIndexOrThrow("page_number")),
                        partTitle = cursor.getString(cursor.getColumnIndexOrThrow("part_title")),
                        videoTitle = cursor.getString(cursor.getColumnIndexOrThrow("video_title")),
                        libraryEntryId = cursor.getString(cursor.getColumnIndexOrThrow("library_entry_id")),
                        collectionId = cursor.getString(cursor.getColumnIndexOrThrow("collection_id")),
                    ),
                )
            }
        }
    }

    fun containsVideo(bvid: String): Boolean = readableDatabase.rawQuery(
        "SELECT 1 FROM library WHERE bvid = ? LIMIT 1",
        arrayOf(bvid),
    ).use { it.moveToFirst() }

    fun add(track: Track): Boolean {
        return addAll(listOf(track))
    }

    fun addAll(tracks: List<Track>, collectionId: String? = null): Boolean {
        val database = writableDatabase
        var alreadyExists = false
        database.beginTransaction()
        try {
            alreadyExists = if (collectionId != null) {
                database.rawQuery(
                    "SELECT 1 FROM library WHERE collection_id = ? LIMIT 1",
                    arrayOf(collectionId),
                ).use { it.moveToFirst() }
            } else {
                tracks.any { track ->
                    database.rawQuery(
                        "SELECT 1 FROM library WHERE bvid = ? AND page_number = ? AND collection_id IS NULL LIMIT 1",
                        arrayOf(track.bvid, track.pageNumber.toString()),
                    ).use { it.moveToFirst() }
                }
            }
            if (!alreadyExists) {
                tracks.forEach { original ->
                    val track = original.copy(
                        libraryEntryId = UUID.randomUUID().toString(),
                        collectionId = collectionId,
                    )
                    val values = ContentValues().apply {
                        put("bvid", track.bvid)
                        put("page_number", track.pageNumber)
                        put("title", track.title)
                        put("author", track.author)
                        put("duration", track.duration)
                        put("cover_url", track.coverUrl)
                        put("page_url", track.pageUrl)
                        put("part_title", track.partTitle)
                        put("video_title", track.videoTitle)
                        put("library_entry_id", track.libraryEntryId)
                        put("collection_id", track.collectionId)
                    }
                    database.insertOrThrow("library", null, values)
                }
                database.setTransactionSuccessful()
            }
        } finally {
            database.endTransaction()
        }
        return !alreadyExists
    }

    fun remove(track: Track) {
        writableDatabase.delete(
            "library",
            "library_entry_id = ?",
            arrayOf(track.libraryId),
        )
    }

    fun removeVideo(bvid: String) {
        writableDatabase.delete("library", "collection_id = ?", arrayOf(bvid))
    }

    fun rename(track: Track, title: String) {
        val updated = writableDatabase.update(
            "library",
            ContentValues().apply { put("title", title) },
            "library_entry_id = ?",
            arrayOf(track.libraryId),
        )
        check(updated > 0) { "无法重命名曲库中的曲目。" }
    }

    fun renameVideo(bvid: String, title: String) {
        val updated = writableDatabase.update(
            "library",
            ContentValues().apply { put("video_title", title) },
            "collection_id = ?",
            arrayOf(bvid),
        )
        check(updated > 0) { "无法重命名曲库中的合集。" }
    }

    companion object {
        private const val DATABASE_VERSION = 5
    }
}
