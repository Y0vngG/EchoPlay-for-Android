package com.echoplay.android

import android.app.PendingIntent
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import android.widget.RemoteViews
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

@OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService() {
    private lateinit var player: ExoPlayer
    private lateinit var mediaSession: MediaSession
    private val localBinder = LocalBinder()
    private val httpDataSourceFactory = DefaultHttpDataSource.Factory()
        .setAllowCrossProtocolRedirects(true)
    private var foregroundStarted = false
    private var lastNotificationProgress = -1
    private val notificationManager by lazy { getSystemService(NotificationManager::class.java) }
    private val launchIntent by lazy {
        PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).addFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP,
            ),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
    private val previousIntent by lazy { notificationAction(ACTION_PREVIOUS, 1) }
    private val toggleIntent by lazy { notificationAction(ACTION_TOGGLE, 2) }
    private val nextIntent by lazy { notificationAction(ACTION_NEXT, 3) }
    private val notificationHandler = Handler(Looper.getMainLooper())
    private val progressUpdater = object : Runnable {
        override fun run() {
            if (player.isPlaying) {
                val progress = playbackProgress()
                if (progress != lastNotificationProgress) updatePlaybackNotification(progress)
                notificationHandler.postDelayed(this, NOTIFICATION_REFRESH_MS)
            }
        }
    }
    @Volatile private var previousTrackAction: (() -> Unit)? = null
    @Volatile private var nextTrackAction: (() -> Unit)? = null

    inner class LocalBinder : Binder() {
        fun service(): PlaybackService = this@PlaybackService
    }

    override fun onCreate() {
        super.onCreate()
        PlaybackDiagnostics.record(this, "service.onCreate")
        notificationManager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "音频播放", NotificationManager.IMPORTANCE_LOW),
        )
        player = ExoPlayer.Builder(this)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .setMediaSourceFactory(
                DefaultMediaSourceFactory(this)
                    .setDataSourceFactory(httpDataSourceFactory),
            )
            .build()
        player.volume = PlaybackStateStore(this).loadVolume()
        player.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build(),
            false,
        )
        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(launchIntent)
            .build()
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                PlaybackDiagnostics.record(this@PlaybackService, "player.isPlaying", "isPlaying=$isPlaying ${PlaybackDiagnostics.playerState(player)}")
                notificationHandler.removeCallbacks(progressUpdater)
                if (isPlaying) notificationHandler.postDelayed(progressUpdater, NOTIFICATION_REFRESH_MS)
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                PlaybackDiagnostics.record(this@PlaybackService, "player.state", PlaybackDiagnostics.playerState(player))
            }

            override fun onEvents(player: Player, events: Player.Events) {
                if (player.isPlaying && !foregroundStarted) {
                    enterForeground()
                } else if (events.containsAny(
                        Player.EVENT_IS_PLAYING_CHANGED,
                        Player.EVENT_PLAYBACK_STATE_CHANGED,
                        Player.EVENT_MEDIA_METADATA_CHANGED,
                        Player.EVENT_MEDIA_ITEM_TRANSITION,
                        Player.EVENT_POSITION_DISCONTINUITY,
                    )
                ) {
                    updatePlaybackNotification()
                }
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                PlaybackDiagnostics.record(this@PlaybackService, "player.error", "code=${error.errorCodeName} message=${error.message}")
            }
        })
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession =
        mediaSession

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        PlaybackDiagnostics.record(
            this,
            "service.onStartCommand",
            "action=${intent?.action} flags=$flags startId=$startId ${PlaybackDiagnostics.playerState(player)}",
        )
        when (intent?.action) {
            ACTION_PREVIOUS -> previousTrackAction?.invoke()
            ACTION_TOGGLE -> if (player.isPlaying) player.pause() else player.play()
            ACTION_NEXT -> nextTrackAction?.invoke()
        }
        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }

    private fun notificationAction(action: String, requestCode: Int): PendingIntent = PendingIntent.getService(
        this,
        requestCode,
        Intent(this, PlaybackService::class.java).setAction(action),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun buildNotification(progress: Int): Notification {
        lastNotificationProgress = progress
        val metadata = player.mediaMetadata
        val contentView = RemoteViews(packageName, R.layout.notification_playback).apply {
            setTextViewText(R.id.notification_title, metadata.title ?: getString(R.string.app_name))
            setTextViewText(R.id.notification_artist, metadata.artist ?: "")
            setProgressBar(R.id.notification_progress, 1_000, progress, false)
            setImageViewResource(R.id.notification_previous, android.R.drawable.ic_media_previous)
            setImageViewResource(
                R.id.notification_play_pause,
                if (player.isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
            )
            setImageViewResource(R.id.notification_next, android.R.drawable.ic_media_next)
            setOnClickPendingIntent(R.id.notification_previous, previousIntent)
            setOnClickPendingIntent(R.id.notification_play_pause, toggleIntent)
            setOnClickPendingIntent(R.id.notification_next, nextIntent)
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(metadata.title ?: getString(R.string.app_name))
            .setContentText(metadata.artist ?: "姝ｅ湪鎾斁")
            .setCustomContentView(contentView)
            .setCustomBigContentView(contentView)
            .setContentIntent(launchIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setOngoing(foregroundStarted || player.isPlaying)
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun playbackProgress(): Int {
        val duration = player.duration
        if (duration <= 0L) return 0
        return ((player.currentPosition.coerceIn(0L, duration) * 1_000L) / duration)
            .toInt()
            .coerceIn(0, 1_000)
    }

    private fun enterForeground() {
        val notification = buildNotification(playbackProgress())
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(NOTIFICATION_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        foregroundStarted = true
        PlaybackDiagnostics.record(this, "service.enteredForeground")
    }

    private fun updatePlaybackNotification(progress: Int = playbackProgress()) {
        // Keep the controls visible while the next track resolves.
        if (foregroundStarted) notificationManager.notify(NOTIFICATION_ID, buildNotification(progress))
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        PlaybackDiagnostics.record(
            this,
            "service.onTaskRemoved",
            "playbackOngoing=$isPlaybackOngoing ${PlaybackDiagnostics.playerState(player)}",
        )
        // MediaSessionService keeps an ongoing foreground playback session
        // alive when its task is dismissed. Preserve that default behavior.
        super.onTaskRemoved(rootIntent)
    }

    override fun onBind(intent: Intent): IBinder? {
        return if (intent.action == ACTION_BIND_LOCAL) localBinder else super.onBind(intent)
    }

    fun player(): ExoPlayer = player

    fun setTrackNavigationActions(previous: () -> Unit, next: () -> Unit) {
        previousTrackAction = previous
        nextTrackAction = next
    }

    fun setRequestHeaders(headers: Map<String, String>) {
        httpDataSourceFactory.setDefaultRequestProperties(headers)
    }

    fun startPlaybackForegroundService() {
        // Start while the activity is visible. Foreground promotion happens when playback starts.
        applicationContext.startService(Intent(applicationContext, PlaybackService::class.java))
        PlaybackDiagnostics.record(this, "service.startForegroundRequested", PlaybackDiagnostics.playerState(player))
    }

    override fun onDestroy() {
        PlaybackDiagnostics.record(this, "service.onDestroy", PlaybackDiagnostics.playerState(player))
        notificationHandler.removeCallbacks(progressUpdater)
        mediaSession.release()
        player.release()
        super.onDestroy()
    }

    companion object {
        const val ACTION_BIND_LOCAL = "com.echoplay.android.BIND_PLAYBACK_LOCAL"
        private const val CHANNEL_ID = "echoplay_playback"
        private const val NOTIFICATION_ID = 4201
        private const val NOTIFICATION_REFRESH_MS = 1_000L
        private const val ACTION_PREVIOUS = "com.echoplay.android.PLAYBACK_PREVIOUS"
        private const val ACTION_TOGGLE = "com.echoplay.android.PLAYBACK_TOGGLE"
        private const val ACTION_NEXT = "com.echoplay.android.PLAYBACK_NEXT"
    }
}
