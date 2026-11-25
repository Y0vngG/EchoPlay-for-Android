package com.echoplay.android

import android.app.UiModeManager
import android.os.Bundle
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import android.os.Build
import android.os.SystemClock
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat

private val EchoPlayLightColors = lightColorScheme(
    primary = Color(0xFF2F65B5),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE8FC),
    onPrimaryContainer = Color(0xFF193C70),
    secondary = Color(0xFF4C6084),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDFE9FB),
    onSecondaryContainer = Color(0xFF233B64),
    surface = Color(0xFFF8FAFF),
    background = Color(0xFFF8FAFF),
    onSurface = Color(0xFF17243B),
    onBackground = Color(0xFF17243B),
    surfaceVariant = Color(0xFFE5EBF8),
    onSurfaceVariant = Color(0xFF435370),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF2F5FE),
    surfaceContainer = Color(0xFFEEF2FC),
    surfaceContainerHigh = Color(0xFFE9EDFC),
    surfaceContainerHighest = Color(0xFFE0E7F8),
    outline = Color(0xFF657693),
    outlineVariant = Color(0xFFC5D1E7),
)

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
        val settingsPreferences = getSharedPreferences("user-settings", MODE_PRIVATE)
        val initialThemeMode = settingsPreferences.getString("theme-mode", null)
            ?.let { savedMode -> AppThemeMode.entries.find { it.name == savedMode } }
            ?: AppThemeMode.SYSTEM
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            when (initialThemeMode) {
                AppThemeMode.LIGHT -> setTheme(R.style.EchoPlayStartingTheme_Light)
                AppThemeMode.DARK -> setTheme(R.style.EchoPlayStartingTheme_Dark)
                AppThemeMode.SYSTEM -> Unit
            }
        }
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        syncStartingTheme(initialThemeMode)
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
        val systemBars = WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        libraryStore = LibraryStore(applicationContext)
        val repository = BilibiliRepository()
        val launchTransition = if (savedInstanceState == null) {
            LaunchLogoTransition(window.decorView as ViewGroup, systemBars).also { transition ->
                splashScreen.setOnExitAnimationListener { transition.play(it) }
            }
        } else {
            null
        }
        var contentReady = false
        val startupDeadline = SystemClock.uptimeMillis() + STARTUP_WAIT_LIMIT_MS
        splashScreen.setKeepOnScreenCondition {
            !contentReady && SystemClock.uptimeMillis() < startupDeadline
        }
        setContent {
            var themeMode by remember { mutableStateOf(initialThemeMode) }
            val useDarkTheme = when (themeMode) {
                AppThemeMode.SYSTEM -> isSystemInDarkTheme()
                AppThemeMode.LIGHT -> false
                AppThemeMode.DARK -> true
            }
            SideEffect {
                val systemBarColor = if (useDarkTheme) android.graphics.Color.BLACK else EchoPlayLightColors.surface.toArgb()
                window.statusBarColor = systemBarColor
                window.navigationBarColor = systemBarColor
                if (launchTransition == null) {
                    systemBars.isAppearanceLightStatusBars = !useDarkTheme
                    systemBars.isAppearanceLightNavigationBars = !useDarkTheme
                } else {
                    launchTransition.updateTheme(useDarkTheme)
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
                    EchoPlayLightColors
                },
            ) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val service = playbackService.value
                    if (service == null) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = androidx.compose.ui.Alignment.Center,
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        }
                    } else {
                        SideEffect { contentReady = true }
                        PhoneContent {
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
                                    syncStartingTheme(mode)
                                },
                                headerLogoModifier = launchTransition?.badgeModifier ?: Modifier,
                                awaitLaunchAnimationEnd = { launchTransition?.awaitEnd() },
                            )
                        }
                    }
                }
            }
        }
        window.decorView.postInvalidateDelayed(STARTUP_WAIT_LIMIT_MS)
    }

    private fun syncStartingTheme(themeMode: AppThemeMode) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        val nightMode = when (themeMode) {
            // AUTO clears the per-app override, so resources follow the system configuration again.
            AppThemeMode.SYSTEM -> UiModeManager.MODE_NIGHT_AUTO
            AppThemeMode.LIGHT -> UiModeManager.MODE_NIGHT_NO
            AppThemeMode.DARK -> UiModeManager.MODE_NIGHT_YES
        }
        getSystemService(UiModeManager::class.java).setApplicationNightMode(nightMode)
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
        const val STARTUP_WAIT_LIMIT_MS = 1_200L
    }
}

@Composable
private fun PhoneContent(content: @Composable () -> Unit) {
    val windowSize = LocalWindowInfo.current.containerDpSize
    val systemDensity = LocalDensity.current
    val referenceWidth = 412.dp
    val isTallPhoneWindow = windowSize.width in referenceWidth..680.dp &&
        windowSize.height >= windowSize.width * 1.7f
    val scale = if (isTallPhoneWindow) {
        (windowSize.width / referenceWidth).coerceAtMost(1.5f)
    } else {
        1f
    }

    // Scale only the Compose pages and retain the system's font-size preference.
    CompositionLocalProvider(
        LocalDensity provides Density(
            density = systemDensity.density * scale,
            fontScale = systemDensity.fontScale,
        ),
        content = content,
    )
}
