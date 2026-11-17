package com.echoplay.android

import android.os.Bundle
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.Image
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat

class MainActivity : ComponentActivity() {
    private lateinit var libraryStore: LibraryStore
    private val playbackService = mutableStateOf<PlaybackService?>(null)
    private var isPlaybackServiceBound = false
    private val playbackServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            PlaybackDiagnostics.record(this@MainActivity, "service.connected")
            val binder = service as? PlaybackService.LocalBinder ?: return
            playbackService.value = binder.service()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            PlaybackDiagnostics.record(this@MainActivity, "service.disconnected")
            playbackService.value = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PlaybackDiagnostics.record(this, "activity.onCreate", "intentAction=${intent?.action}")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                NOTIFICATION_PERMISSION_REQUEST,
            )
        }
        WindowCompat.setDecorFitsSystemWindows(window, true)
        window.statusBarColor = android.graphics.Color.WHITE
        window.navigationBarColor = android.graphics.Color.WHITE
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        libraryStore = LibraryStore(applicationContext)
        val repository = BilibiliRepository()
        setContent {
            val settingsPreferences = remember {
                getSharedPreferences("user-settings", MODE_PRIVATE)
            }
            var themeMode by remember {
                mutableStateOf(
                    runCatching {
                        AppThemeMode.valueOf(
                            settingsPreferences.getString("theme-mode", AppThemeMode.SYSTEM.name)!!,
                        )
                    }.getOrDefault(AppThemeMode.SYSTEM),
                )
            }
            val useDarkTheme = when (themeMode) {
                AppThemeMode.SYSTEM -> isSystemInDarkTheme()
                AppThemeMode.LIGHT -> false
                AppThemeMode.DARK -> true
            }
            SideEffect {
                window.statusBarColor = if (useDarkTheme) android.graphics.Color.BLACK else android.graphics.Color.WHITE
                window.navigationBarColor = if (useDarkTheme) android.graphics.Color.BLACK else android.graphics.Color.WHITE
                WindowInsetsControllerCompat(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !useDarkTheme
                    isAppearanceLightNavigationBars = !useDarkTheme
                }
            }
            MaterialTheme(
                colorScheme = if (useDarkTheme) {
                    darkColorScheme(
                        onPrimary = Color(0xFFD2CADF),
                        onSurface = Color(0xFFD8D1E5),
                        onSurfaceVariant = Color(0xFFBEB6CC),
                        onError = Color(0xFFE9D4DD),
                    )
                } else {
                    lightColorScheme()
                },
            ) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val service = playbackService.value
                    if (service == null) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = androidx.compose.ui.Alignment.Center,
                        ) {
                            Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                                Image(
                                    painter = painterResource(R.drawable.echoplay_logo),
                                    contentDescription = "EchoPlay",
                                    modifier = Modifier.size(148.dp),
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "EchoPlay",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Spacer(modifier = Modifier.height(20.dp))
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            }
                        }
                    } else {
                        EchoPlayApp(
                            repository = repository,
                            libraryStore = libraryStore,
                            player = service.player(),
                            setPlaybackHeaders = service::setRequestHeaders,
                            startPlaybackService = service::startPlaybackForegroundService,
                            setTrackNavigationActions = service::setTrackNavigationActions,
                            themeMode = themeMode,
                            isDarkTheme = useDarkTheme,
                            onThemeModeChanged = { mode ->
                                themeMode = mode
                                settingsPreferences.edit().putString("theme-mode", mode.name).apply()
                            },
                        )
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        PlaybackDiagnostics.record(this, "activity.onStart")
        if (!isPlaybackServiceBound) {
            val intent = Intent(this, PlaybackService::class.java).apply {
                action = PlaybackService.ACTION_BIND_LOCAL
            }
            isPlaybackServiceBound = bindService(
                intent,
                playbackServiceConnection,
                Context.BIND_AUTO_CREATE,
            )
        }
    }

    override fun onStop() {
        PlaybackDiagnostics.record(this, "activity.onStop", "changingConfigurations=$isChangingConfigurations")
        if (isPlaybackServiceBound) {
            unbindService(playbackServiceConnection)
            isPlaybackServiceBound = false
        }
        super.onStop()
    }

    override fun onDestroy() {
        PlaybackDiagnostics.record(this, "activity.onDestroy", "finishing=$isFinishing changingConfigurations=$isChangingConfigurations")
        if (::libraryStore.isInitialized) libraryStore.close()
        super.onDestroy()
    }

    private companion object {
        const val NOTIFICATION_PERMISSION_REQUEST = 1001
    }
}
