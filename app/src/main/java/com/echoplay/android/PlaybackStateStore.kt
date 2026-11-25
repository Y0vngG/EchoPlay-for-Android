package com.echoplay.android

import android.content.Context
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject

enum class PlaybackRepeatMode {
    Off,
    List,
    Single,
}

data class SavedPlaybackState(
    val currentTrack: Track,
    val queue: List<Track>,
    val positionMs: Long,
    val shuffleEnabled: Boolean,
    val wasPlaying: Boolean,
    val repeatMode: PlaybackRepeatMode = PlaybackRepeatMode.Off,
    val playlistId: String? = null,
)

class PlaybackStateStore(context: Context) {
    private val preferences = context.applicationContext
        .getSharedPreferences("playback_state", Context.MODE_PRIVATE)

    private var lastSavedState: SavedPlaybackState? = null

    fun loadVolume(): Float = preferences.getFloat(KEY_APP_VOLUME, 1f)
        .takeIf(Float::isFinite)
        ?.coerceIn(0f, 1f) ?: 1f

    fun saveVolume(volume: Float) {
        preferences.edit { putFloat(KEY_APP_VOLUME, volume) }
    }

    fun load(): SavedPlaybackState? = runCatching {
        val values = preferences.all
        val root = (values[KEY_STATE] as? String)?.let(::JSONObject) ?: return null
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
            positionMs = ((values[KEY_POSITION] as? Long) ?: root.optLong("positionMs", 0L)).coerceAtLeast(0L),
            shuffleEnabled = root.optBoolean("shuffleEnabled", false),
            wasPlaying = root.optBoolean("wasPlaying", false),
            repeatMode = PlaybackRepeatMode.entries.firstOrNull { it.name == root.optString("repeatMode") }
                ?: PlaybackRepeatMode.Off,
            playlistId = root.optString("playlistId").takeIf(String::isNotBlank),
        )
    }.getOrNull()

    fun save(state: SavedPlaybackState) {
        val position = state.positionMs.coerceAtLeast(0L)
        val previous = lastSavedState
        val metadataChanged = previous == null ||
            previous.queue != state.queue ||
            previous.currentTrack != state.currentTrack ||
            previous.shuffleEnabled != state.shuffleEnabled ||
            previous.wasPlaying != state.wasPlaying ||
            previous.repeatMode != state.repeatMode ||
            previous.playlistId != state.playlistId
        if (!metadataChanged && previous?.positionMs == position) return

        preferences.edit {
            if (metadataChanged) {
                val root = JSONObject().apply {
                    put("current", state.currentTrack.toJson())
                    put("queue", JSONArray().apply { state.queue.forEach { put(it.toJson()) } })
                    put("positionMs", position)
                    put("shuffleEnabled", state.shuffleEnabled)
                    put("wasPlaying", state.wasPlaying)
                    put("repeatMode", state.repeatMode.name)
                    put("playlistId", state.playlistId)
                }
                putString(KEY_STATE, root.toString())
            }
            putLong(KEY_POSITION, position)
        }
        lastSavedState = state
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
        put("artist", artist)
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
        artist = if (isNull("artist")) null else optString("artist"),
    )

    private companion object {
        const val KEY_STATE = "last_playback"
        const val KEY_POSITION = "last_playback_position_ms"
        const val KEY_APP_VOLUME = "app_volume"
    }
}
