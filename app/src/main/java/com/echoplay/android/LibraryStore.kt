package com.echoplay.android

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.core.database.sqlite.transaction
import java.util.UUID

internal data class LibraryPlaylist(
    val id: String,
    val name: String,
    val trackIds: List<String>,
)

class LibraryStore(context: Context) :
    SQLiteOpenHelper(context, "echoplay-library.db", null, DATABASE_VERSION) {

    override fun onConfigure(db: SQLiteDatabase) {
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        createLibraryTable(db)
        createPlaylistTables(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 5) {
            db.execSQL("DROP TABLE IF EXISTS library")
            createLibraryTable(db)
        } else if (oldVersion < 6) {
            db.execSQL("ALTER TABLE library ADD COLUMN artist TEXT")
        }
        if (oldVersion < 7) createPlaylistTables(db)
    }

    private fun createPlaylistTables(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE playlists (id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL)")
        db.execSQL(
            """CREATE TABLE playlist_tracks (
                playlist_id TEXT NOT NULL REFERENCES playlists(id) ON DELETE CASCADE,
                library_entry_id TEXT NOT NULL REFERENCES library(library_entry_id) ON DELETE CASCADE,
                position INTEGER NOT NULL,
                PRIMARY KEY (playlist_id, library_entry_id)
            )""".trimIndent(),
        )
        db.execSQL("CREATE INDEX playlist_tracks_library_entry ON playlist_tracks(library_entry_id)")
    }

    internal fun playlists(): List<LibraryPlaylist> = readableDatabase.rawQuery(
        """SELECT playlists.id, playlists.name, playlist_tracks.library_entry_id
            FROM playlists LEFT JOIN playlist_tracks ON playlist_tracks.playlist_id = playlists.id
            ORDER BY playlists.rowid, playlist_tracks.position""".trimIndent(),
        null,
    ).use { cursor ->
        buildList {
            if (!cursor.moveToFirst()) return@buildList
            do {
                val id = cursor.getString(0)
                val name = cursor.getString(1)
                val trackIds = buildList {
                    do {
                        if (!cursor.isNull(2)) add(cursor.getString(2))
                    } while (cursor.moveToNext() && cursor.getString(0) == id)
                }
                add(LibraryPlaylist(id, name, trackIds))
            } while (!cursor.isAfterLast)
        }
    }

    internal fun savePlaylist(id: String?, name: String, trackIds: List<String>): String {
        val playlistName = name.trim()
        require(playlistName.isNotEmpty()) { "播放列表名称不能为空。" }
        val playlistId = id ?: UUID.randomUUID().toString()
        writableDatabase.transaction {
            val values = ContentValues().apply { put("name", playlistName) }
            if (id == null) {
                values.put("id", playlistId)
                insertOrThrow("playlists", null, values)
            } else {
                val updated = update("playlists", values, "id = ?", arrayOf(playlistId))
                check(updated > 0) { "这个播放列表已不存在。" }
                delete("playlist_tracks", "playlist_id = ?", arrayOf(playlistId))
            }
            trackIds.distinct().forEachIndexed { position, trackId ->
                execSQL(
                    """INSERT INTO playlist_tracks (playlist_id, library_entry_id, position)
                        SELECT ?, library_entry_id, ? FROM library WHERE library_entry_id = ?""".trimIndent(),
                    arrayOf<Any>(playlistId, position, trackId),
                )
            }
        }
        return playlistId
    }

    internal fun deletePlaylist(id: String) {
        writableDatabase.delete("playlists", "id = ?", arrayOf(id))
    }

    private fun createLibraryTable(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE library (
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
                collection_id TEXT,
                artist TEXT
            )""".trimIndent(),
        )
        db.execSQL(
            "CREATE UNIQUE INDEX library_standalone_identity " +
                "ON library (bvid, page_number) WHERE collection_id IS NULL",
        )
        db.execSQL(
            "CREATE UNIQUE INDEX library_collection_page_identity " +
                "ON library (collection_id, page_number) WHERE collection_id IS NOT NULL",
        )
    }

    fun tracks(): List<Track> = readableDatabase.rawQuery(
        """SELECT bvid, title, author, duration, cover_url, page_url, page_number, part_title,
                video_title, library_entry_id, collection_id, artist
            FROM library ORDER BY rowid ASC""".trimIndent(),
        null,
    ).use { cursor ->
        buildList {
            while (cursor.moveToNext()) {
                add(
                    Track(
                        bvid = cursor.getString(0),
                        title = cursor.getString(1),
                        author = cursor.getString(2),
                        duration = cursor.getString(3),
                        coverUrl = cursor.getString(4),
                        pageUrl = cursor.getString(5),
                        pageNumber = cursor.getInt(6),
                        partTitle = cursor.getString(7),
                        videoTitle = cursor.getString(8),
                        libraryEntryId = cursor.getString(9),
                        collectionId = cursor.getString(10),
                        artist = cursor.getString(11),
                    ),
                )
            }
        }
    }

    fun containsVideo(bvid: String): Boolean = readableDatabase.rawQuery(
        "SELECT 1 FROM library WHERE bvid = ? LIMIT 1",
        arrayOf(bvid),
    ).use { it.moveToFirst() }

    fun add(track: Track): Boolean = addAll(listOf(track))

    fun addAll(tracks: List<Track>, collectionId: String? = null): Boolean = writableDatabase.transaction {
        val alreadyExists = if (collectionId != null) {
            rawQuery(
                "SELECT 1 FROM library WHERE collection_id = ? LIMIT 1",
                arrayOf(collectionId),
            ).use { it.moveToFirst() }
        } else {
            tracks.any { track ->
                rawQuery(
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
                    put("artist", track.artist)
                }
                insertOrThrow("library", null, values)
            }
        }
        !alreadyExists
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

    fun rename(track: Track, title: String, artist: String?) {
        val updated = writableDatabase.update(
            "library",
            ContentValues().apply {
                put("title", title)
                put("artist", artist)
            },
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
        private const val DATABASE_VERSION = 7
    }
}
