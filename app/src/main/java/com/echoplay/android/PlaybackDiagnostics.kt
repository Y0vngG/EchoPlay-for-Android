package com.echoplay.android

import android.content.Context
import android.os.Build
import android.os.Process
import android.util.Log
import androidx.media3.common.Player
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Small, bounded diagnostic log for background playback reports. */
object PlaybackDiagnostics {
    private const val FILE_NAME = "playback-diagnostics.log"
    private const val MAX_BYTES = 512 * 1024

    @Synchronized
    fun record(context: Context, event: String, details: String = "") {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())
        val safeDetails = details
            .replace(Regex("(?i)(cookie|authorization|sessdata|bili_jct)\\s*[:=]\\s*[^\\s,;]+"), "$1=[REDACTED]")
            .replace(Regex("https?://\\S+"), "[URL]")
            .replace('\n', ' ')
            .take(700)
        val file = File(context.applicationContext.filesDir, FILE_NAME)
        runCatching {
            if (file.exists() && file.length() > MAX_BYTES) {
                val tail = file.readLines().takeLast(1200).joinToString("\n")
                file.writeText(tail + "\n")
            }
            file.appendText("$timestamp pid=${Process.myPid()} $event $safeDetails\n")
        }
        Log.i("EchoPlayPlayback", "$event $safeDetails")
    }

    fun export(context: Context): String {
        val file = File(context.applicationContext.filesDir, FILE_NAME)
        val header = "EchoPlay playback diagnostics\nAndroid ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})\n"
        return header + if (file.exists()) file.readText() else "尚无诊断事件。\n"
    }

    fun generateFile(context: Context): File {
        val directory = File(context.applicationContext.filesDir, "logs").apply { mkdirs() }
        val name = "EchoPlay-${SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())}.log"
        return File(directory, name).apply { writeText(export(context)) }
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
