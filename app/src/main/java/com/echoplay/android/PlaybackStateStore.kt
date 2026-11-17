package com.echoplay.android

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class SavedPlaybackState(
    val currentTrack: Track,
    val queue: List<Track>,
    val positionMs: Long,
    val volume: Float,
    val shuffleEnabled: Boolean,
    val wasPlaying: Boolean,
)

class PlaybackStateStore(context: Context) {
    private val preferences = context.applicationContext
        .getSharedPreferences("playback_state", Context.MODE_PRIVATE)

    fun load(): SavedPlaybackState? = runCatching {
        val root = preferences.getString(KEY_STATE, null)?.let(::JSONObject) ?: return null
        val current = root.optJSONObject("current")?.toTrack() ?: return null
        val queueJson = root.optJSONArray("queue") ?: JSONArray()
        val queue = buildList {
            for (index in 0 until queueJson.length()) {
                queueJson.optJSONObject(index)?.toTrack()?.let(::add)
            }
        }
        SavedPlaybackState(
            currentTrack = current,
            queue = queue.ifEmpty { listOf(current) },
            positionMs = root.optLong("positionMs", 0L).coerceAtLeast(0L),
            volume = root.optDouble("volume", 1.0).toFloat().coerceIn(0f, 1f),
            shuffleEnabled = root.optBoolean("shuffleEnabled", false),
            wasPlaying = root.optBoolean("wasPlaying", false),
        )
    }.getOrNull()

    fun save(state: SavedPlaybackState) {
        val root = JSONObject().apply {
            put("current", state.currentTrack.toJson())
            put("queue", JSONArray().apply { state.queue.forEach { put(it.toJson()) } })
            put("positionMs", state.positionMs.coerceAtLeast(0L))
            put("volume", state.volume.coerceIn(0f, 1f).toDouble())
            put("shuffleEnabled", state.shuffleEnabled)
            put("wasPlaying", state.wasPlaying)
        }
        preferences.edit().putString(KEY_STATE, root.toString()).apply()
    }

    private fun Track.toJson() = JSONObject().apply {
        put("bvid", bvid)
        put("title", title)
        put("author", author)
        put("duration", duration)
        put("coverUrl", coverUrl)
        put("pageUrl", pageUrl)
        put("pageNumber", pageNumber)
        put("partTitle", partTitle)
        put("videoTitle", videoTitle)
        put("libraryEntryId", libraryEntryId)
        put("collectionId", collectionId)
    }

    private fun JSONObject.toTrack() = Track(
        bvid = optString("bvid"),
        title = optString("title"),
        author = optString("author"),
        duration = optString("duration"),
        coverUrl = optString("coverUrl"),
        pageUrl = optString("pageUrl"),
        pageNumber = optInt("pageNumber", 1),
        partTitle = optString("partTitle"),
        videoTitle = optString("videoTitle", optString("title")),
        libraryEntryId = optString("libraryEntryId"),
        collectionId = optString("collectionId").takeIf { it.isNotBlank() },
    )

    private companion object {
        const val KEY_STATE = "last_playback"
    }
}
