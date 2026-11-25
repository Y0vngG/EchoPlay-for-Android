package com.echoplay.android

import android.content.Context
import android.os.Process
import android.util.Log
import androidx.media3.common.Player
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

/** Small, bounded diagnostic log for background playback reports. */
object PlaybackDiagnostics {
    private const val FILE_NAME = "playback-diagnostics.log"
    private const val MAX_BYTES = 512 * 1024

    private val sessionPattern = Regex("(?i)(cookie|authorization|sessdata|bili_jct)\\s*[:=]\\s*[^\\s,;]+")
    private val urlPattern = Regex("https?://\\S+")
    private data class Entry(val context: Context, val time: Long, val event: String, val details: String)
    private val entries = Channel<Entry>(capacity = 64, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    init {
        CoroutineScope(Dispatchers.IO).launch {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
            for (entry in entries) {
                write(entry, dateFormat.format(Date(entry.time)))
            }
        }
    }

    fun record(context: Context, event: String, details: String = "") {
        entries.trySend(Entry(context.applicationContext, System.currentTimeMillis(), event, details))
    }

    private fun write(entry: Entry, timestamp: String) {
        val safeDetails = entry.details
            .replace(sessionPattern, "$1=[REDACTED]")
            .replace(urlPattern, "[URL]")
            .replace('\n', ' ')
            .take(700)
        runCatching {
            val file = File(entry.context.filesDir, FILE_NAME)
            if (file.exists() && file.length() > MAX_BYTES) {
                val tail = file.readLines().takeLast(1200).joinToString("\n")
                file.writeText(tail + "\n")
            }
            file.appendText("$timestamp pid=${Process.myPid()} ${entry.event} $safeDetails\n")
        }
        Log.i("EchoPlayPlayback", "${entry.event} $safeDetails")
    }

    fun playerState(player: Player): String =
        "state=${stateName(player.playbackState)} isPlaying=${player.isPlaying} " +
            "playWhenReady=${player.playWhenReady} suppression=${player.playbackSuppressionReason} " +
            "positionMs=${player.currentPosition} bufferedMs=${player.bufferedPosition} " +
            "mediaItemCount=${player.mediaItemCount}"

    private fun stateName(state: Int): String = when (state) {
        Player.STATE_IDLE -> "IDLE"
        Player.STATE_BUFFERING -> "BUFFERING"
        Player.STATE_READY -> "READY"
        Player.STATE_ENDED -> "ENDED"
        else -> "UNKNOWN($state)"
    }
}
