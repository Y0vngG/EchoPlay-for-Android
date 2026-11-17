package com.echoplay.android

import android.database.ContentObserver
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.webkit.CookieManager as WebCookieManager
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.fadeOut
import androidx.media3.common.MediaItem
import coil.compose.AsyncImage
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt
import kotlin.math.abs

private enum class AppTab(val label: String) {
    Search("搜索"),
    Library("曲库"),
    UserInfo("我的")
}

enum class AppThemeMode(val label: String) {
    SYSTEM("\u8ddf\u968f\u7cfb\u7edf"),
    LIGHT("\u4eae\u8272\u6a21\u5f0f"),
    DARK("\u6697\u8272\u6a21\u5f0f"),
}

private val NavigationAccentLight = Color(0xFF3978D4)
private val NavigationAccentDark = Color(0xFF586FE8)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EchoPlayApp(
    repository: BilibiliRepository,
    libraryStore: LibraryStore,
    player: ExoPlayer,
    setPlaybackHeaders: (Map<String, String>) -> Unit,
    startPlaybackService: () -> Unit,
    setTrackNavigationActions: (previous: () -> Unit, next: () -> Unit) -> Unit,
    themeMode: AppThemeMode,
    isDarkTheme: Boolean,
    onThemeModeChanged: (AppThemeMode) -> Unit,
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val audioManager = remember(context) { context.getSystemService(AudioManager::class.java) }
    fun readSystemMediaVolume(): Float {
        val maximum = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        return audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / maximum
    }
    val clipboard = LocalClipboardManager.current
    val playbackStateStore = remember(context) { PlaybackStateStore(context) }
    var selectedTab by remember { mutableStateOf(AppTab.Library) }
    var swipedTrackId by remember { mutableStateOf<String?>(null) }
    var searchText by remember { mutableStateOf("") }
    var libraryFilter by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var toastMessage by remember { mutableStateOf<Pair<String, Boolean>?>(null) }
    androidx.compose.runtime.LaunchedEffect(errorMessage, successMessage) {
        val message = errorMessage ?: successMessage ?: return@LaunchedEffect
        toastMessage = message to (errorMessage == message)
        delay(2_000)
        if (errorMessage == message) errorMessage = null
        if (successMessage == message) successMessage = null
        if (toastMessage?.first == message) toastMessage = null
    }
    var page by remember { mutableStateOf(1) }
    var showBilibiliLogin by remember { mutableStateOf(false) }
    var isBilibiliLoggedIn by remember { mutableStateOf(false) }
    var bilibiliUsername by remember { mutableStateOf("") }
    var bilibiliAvatar by remember { mutableStateOf("") }
    var bilibiliIsVip by remember { mutableStateOf(false) }
    var bilibiliVipBadge by remember { mutableStateOf("") }
    val settingsPreferences = remember(context) {
        context.getSharedPreferences("user-settings", android.content.Context.MODE_PRIVATE)
    }
    var playbackQuality by remember {
        mutableStateOf(runCatching {
            PlaybackQuality.valueOf(settingsPreferences.getString("playback-quality", PlaybackQuality.HI_RES.name)!!)
        }.getOrDefault(PlaybackQuality.HI_RES))
    }
    var playbackTrack by remember { mutableStateOf<Track?>(null) }
    var playbackQueue by remember { mutableStateOf<List<Track>>(emptyList()) }
    var playbackStatus by remember { mutableStateOf("就绪") }
    var playbackLoading by remember { mutableStateOf(false) }
    var playbackIsPlaying by remember { mutableStateOf(false) }
    var showPlayerDetails by remember { mutableStateOf(false) }
    var playbackPosition by remember { mutableStateOf(0L) }
    var playbackDuration by remember { mutableStateOf(0L) }
    var playbackVolume by remember(audioManager) { mutableStateOf(readSystemMediaVolume()) }
    var shufflePlayback by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<Track?>(null) }
    var renameVideoBvid by remember { mutableStateOf<String?>(null) }
    var renameTitle by remember { mutableStateOf("") }
    var addTarget by remember { mutableStateOf<Track?>(null) }
    var pendingAddTracks by remember { mutableStateOf<List<Track>>(emptyList()) }
    var pendingAddIsCollection by remember { mutableStateOf(false) }
    var addingTrack by remember { mutableStateOf(false) }
    val results = remember { mutableStateListOf<Track>() }
    val library = remember { mutableStateListOf<Track>() }
    val videoPages = remember { mutableStateMapOf<String, List<Track>>() }
    val loadingPageVideos = remember { mutableStateListOf<String>() }
    val expandedSearchVideos = remember { mutableStateListOf<String>() }
    val expandedLibraryVideos = remember { mutableStateListOf<String>() }
    val searchResultsListState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val navigationAccent = if (isDarkTheme) {
        NavigationAccentDark
    } else {
        NavigationAccentLight
    }
    val dismissSwipeOnTap = Modifier.pointerInput(swipedTrackId) {
        awaitEachGesture {
            val down = awaitFirstDown(
                requireUnconsumed = false,
                pass = PointerEventPass.Final,
            )
            var movedBeyondTouchSlop = false
            var released = false
            while (!released) {
                val event = awaitPointerEvent(PointerEventPass.Final)
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                if ((change.position - down.position).getDistance() > viewConfiguration.touchSlop) {
                    movedBeyondTouchSlop = true
                }
                released = !change.pressed
            }
            if (released && !movedBeyondTouchSlop && swipedTrackId != null) {
                swipedTrackId = null
            }
        }
    }
    var playbackRequestId by remember { mutableStateOf(0) }

    DisposableEffect(context, audioManager) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                playbackVolume = readSystemMediaVolume()
            }
        }
        context.contentResolver.registerContentObserver(Settings.System.CONTENT_URI, true, observer)
        onDispose { context.contentResolver.unregisterContentObserver(observer) }
    }

    fun savePlaybackState(positionMs: Long = player.currentPosition) {
        val current = playbackTrack ?: return
        playbackStateStore.save(
            SavedPlaybackState(
                currentTrack = current,
                queue = playbackQueue,
                positionMs = positionMs,
                volume = playbackVolume,
                shuffleEnabled = shufflePlayback,
                wasPlaying = playbackIsPlaying || player.playWhenReady,
            ),
        )
    }

    fun playTrack(
        track: Track,
        queue: List<Track> = library.toList(),
        startPositionMs: Long = 0L,
        resumePlaying: Boolean = true,
        quality: PlaybackQuality = if (playbackQuality == PlaybackQuality.HI_RES && !bilibiliIsVip) {
            PlaybackQuality.P1080
        } else playbackQuality,
    ) {
        focusManager.clearFocus()
        PlaybackDiagnostics.record(context, "ui.playRequested", "trackId=${track.libraryId}")
        // Promote the media session service while the Activity is still in
        // the foreground; stream resolution may take several seconds.
        startPlaybackService()
        playbackRequestId += 1
        val requestId = playbackRequestId
        repository.updateWebCookies(currentBilibiliCookieHeader())
        player.stop()
        playbackTrack = track
        playbackQueue = queue
        playbackStateStore.save(
            SavedPlaybackState(track, queue, startPositionMs, playbackVolume, shufflePlayback, resumePlaying),
        )
        playbackLoading = true
        playbackIsPlaying = false
        playbackStatus = "正在获取 B 站音轨…"
        scope.launch {
            try {
                val stream = withContext(Dispatchers.IO) {
                    repository.audioStream(track, quality)
                }
                PlaybackDiagnostics.record(context, "stream.resolved", "trackId=${track.libraryId} urlCount=${stream.urls.size} description=${stream.description}")
                if (requestId != playbackRequestId) return@launch
                setPlaybackHeaders(stream.headers)
                val mediaMetadata = androidx.media3.common.MediaMetadata.Builder()
                    .setTitle(track.title)
                    .setArtist(track.author)
                    .setArtworkUri(track.coverUrl.takeIf(String::isNotBlank)?.let(android.net.Uri::parse))
                    .build()
                player.setMediaItems(
                    stream.urls.map { url ->
                        MediaItem.Builder()
                            .setUri(url)
                            .setMediaMetadata(mediaMetadata)
                            .build()
                    },
                )
                player.prepare()
                if (startPositionMs > 0L) player.seekTo(startPositionMs)
                playbackLoading = false
                playbackStatus = stream.description
                if (resumePlaying) player.play() else player.pause()
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                PlaybackDiagnostics.record(context, "stream.error", "trackId=${track.libraryId} type=${exception.javaClass.simpleName} message=${exception.message}")
                if (requestId != playbackRequestId) return@launch
                playbackLoading = false
                playbackIsPlaying = false
                playbackStatus = exception.message ?: "获取 B 站音轨失败。"
            }
        }
    }

    fun selectPlaybackQuality(quality: PlaybackQuality) {
        if (quality == PlaybackQuality.HI_RES && !bilibiliIsVip) return
        if (playbackQuality == quality) return
        playbackQuality = quality
        settingsPreferences.edit().putString("playback-quality", quality.name).apply()
        val currentTrack = playbackTrack ?: return
        val resume = playbackIsPlaying || player.playWhenReady
        val position = player.currentPosition
        playTrack(
            track = currentTrack,
            queue = playbackQueue,
            startPositionMs = position,
            resumePlaying = resume,
            quality = quality,
        )
    }

    fun playSearchResult(track: Track) {
        scope.launch {
            playbackLoading = true
            playbackStatus = "正在读取合集分 P…"
            try {
                val pages = withContext(Dispatchers.IO) { repository.pages(track) }
                val orderedQueue = pages.ifEmpty { listOf(track) }
                playTrack(orderedQueue.first(), orderedQueue)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                playbackLoading = false
                playbackStatus = exception.message ?: "读取视频失败。"
                errorMessage = playbackStatus
            }
        }
    }

    fun playNextTrack() {
        val currentTrack = playbackTrack ?: return
        val currentIndex = playbackQueue.indexOfFirst { it.libraryId == currentTrack.libraryId }
        if (currentIndex < 0) return
        if (playbackQueue.isEmpty()) return
        val nextTrack = playbackQueue[(currentIndex + 1) % playbackQueue.size]
        playTrack(nextTrack, playbackQueue)
    }

    fun setShuffleMode(enabled: Boolean) {
        if (shufflePlayback == enabled) return
        val currentTrack = playbackTrack
        val currentId = currentTrack?.libraryId
        val baseQueue = library.toList().ifEmpty { playbackQueue }
        playbackQueue = if (enabled && currentTrack != null) {
            listOf(currentTrack) + baseQueue
                .filterNot { it.libraryId == currentId }
                .shuffled()
        } else if (!enabled) {
            val orderedQueue = baseQueue.toMutableList()
            if (currentTrack != null && orderedQueue.none { it.libraryId == currentId }) {
                orderedQueue.add(0, currentTrack)
            }
            orderedQueue
        } else {
            baseQueue.shuffled()
        }
        shufflePlayback = enabled
        savePlaybackState()
    }

    fun playLibrary(shuffle: Boolean) {
        val queue = library.toList().let { tracks ->
            if (shuffle) tracks.shuffled() else tracks
        }
        val firstTrack = queue.firstOrNull() ?: return
        shufflePlayback = shuffle
        playTrack(firstTrack, queue)
    }

    fun playPreviousTrack() {
        val currentTrack = playbackTrack ?: return
        if (player.currentPosition > 3_000L) {
            player.seekTo(0)
            return
        }
        val currentIndex = playbackQueue.indexOfFirst { it.libraryId == currentTrack.libraryId }
        if (currentIndex < 0 || playbackQueue.isEmpty()) return
        val previousIndex = (currentIndex - 1 + playbackQueue.size) % playbackQueue.size
        playTrack(playbackQueue[previousIndex], playbackQueue)
    }

    DisposableEffect(setTrackNavigationActions) {
        setTrackNavigationActions(::playPreviousTrack, ::playNextTrack)
        onDispose { setTrackNavigationActions({}, {}) }
    }

    androidx.compose.runtime.DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                playbackIsPlaying = isPlaying
                savePlaybackState()
                PlaybackDiagnostics.record(context, "ui.player.isPlaying", "isPlaying=$isPlaying ${PlaybackDiagnostics.playerState(player)}")
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                savePlaybackState()
                PlaybackDiagnostics.record(context, "ui.player.state", PlaybackDiagnostics.playerState(player))
                if (playbackState == Player.STATE_ENDED) {
                    playbackIsPlaying = false
                    if (playbackQueue.isNotEmpty()) {
                        playNextTrack()
                    } else {
                        playbackStatus = "播放完毕"
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                PlaybackDiagnostics.record(context, "ui.player.error", "code=${error.errorCodeName} message=${error.message}")
                playbackLoading = false
                playbackIsPlaying = false
                playbackStatus = error.message ?: "音频播放失败。"
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
        }
    }

    fun refreshLibrary() {
        scope.launch {
            try {
                val tracks = withContext(Dispatchers.IO) { libraryStore.tracks() }
                library.clear()
                library.addAll(tracks)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                errorMessage = exception.message ?: "读取曲库失败。"
            }
        }
    }

    fun search(nextPage: Int = 1) {
        if (searchText.isBlank() || loading) return
        scope.launch {
            loading = true
            errorMessage = null
            successMessage = null
            if (nextPage == 1) results.clear()
            try {
                val nextResults = withContext(Dispatchers.IO) {
                    repository.search(searchText.trim(), nextPage)
                }
                val knownVideoIds = results.mapTo(mutableSetOf()) { it.bvid }
                results.addAll(nextResults.filter { knownVideoIds.add(it.bvid) })
                page = nextPage
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                errorMessage = exception.message ?: "搜索失败，请稍后重试。"
            } finally {
                loading = false
            }
        }
    }

    fun requestAddTracks(tracks: List<Track>, isCollection: Boolean = false) {
        if (tracks.isEmpty()) return
        pendingAddTracks = tracks
        pendingAddIsCollection = isCollection
        addTarget = tracks.first()
        renameTitle = if (isCollection) {
            tracks.first().videoTitle.ifBlank { tracks.first().title }
        } else {
            val track = tracks.first()
            val collectionTitle = track.videoTitle.trim()
            val partTitle = track.title.trim()
            if (track.partTitle.isNotBlank() && collectionTitle.isNotBlank() &&
                !collectionTitle.equals(partTitle, ignoreCase = true)
            ) {
                "$collectionTitle - $partTitle"
            } else {
                partTitle
            }
        }
        errorMessage = null
        successMessage = null
    }

    fun submitSearch() {
        val query = searchText.trim()
        if (query.isBlank() || loading) return
        if (!query.startsWith("http://", ignoreCase = true) &&
            !query.startsWith("https://", ignoreCase = true) &&
            !query.startsWith("www.", ignoreCase = true)
        ) {
            search()
            return
        }

        scope.launch {
            loading = true
            errorMessage = null
            successMessage = null
            results.clear()
            try {
                val (tracks, alreadyExists) = withContext(Dispatchers.IO) {
                    val track = repository.videoTrackFromUrl(query)
                    repository.pages(track) to libraryStore.containsVideo(track.bvid)
                }
                if (alreadyExists) {
                    errorMessage = "曲库中已存在。"
                } else {
                    requestAddTracks(tracks, isCollection = true)
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                errorMessage = exception.message ?: "添加 B 站视频到曲库失败。"
            } finally {
                loading = false
            }
        }
    }

    fun addToLibrary(track: Track) {
        requestAddTracks(listOf(track))
    }

    fun addVideoToLibrary(track: Track) {
        scope.launch {
            try {
                val pages = withContext(Dispatchers.IO) { repository.pages(track) }
                requestAddTracks(pages, isCollection = true)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                errorMessage = exception.message ?: "添加视频到曲库失败。"
            }
        }
    }

    fun confirmAddToLibrary() {
        addTarget ?: return
        val title = renameTitle.trim()
        val tracksToAdd = pendingAddTracks
        val isCollection = pendingAddIsCollection && tracksToAdd.size > 1
        if (title.isBlank()) {
            errorMessage = "歌曲名称不能为空。"
            return
        }
        scope.launch {
            addingTrack = true
            try {
                val added = withContext(Dispatchers.IO) {
                    if (isCollection) {
                        libraryStore.addAll(
                            tracksToAdd.map { pageTrack ->
                                pageTrack.copy(
                                    title = if (tracksToAdd.size == 1) title else pageTrack.title,
                                    videoTitle = title,
                                )
                            },
                            collectionId = tracksToAdd.first().bvid,
                        )
                    } else {
                        libraryStore.add(tracksToAdd.first().copy(title = title))
                    }
                }
                if (!added) {
                    addTarget = null
                    pendingAddTracks = emptyList()
                    pendingAddIsCollection = false
                    errorMessage = "曲库中已存在。"
                    return@launch
                }
                addTarget = null
                pendingAddTracks = emptyList()
                pendingAddIsCollection = false
                refreshLibrary()
                successMessage = if (isCollection) {
                    "合集已添加到曲库。"
                } else {
                    "音频已添加到曲库。"
                }
                errorMessage = null
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                errorMessage = if (exception.message?.contains("UNIQUE constraint failed", ignoreCase = true) == true) {
                    "曲库中已存在。"
                } else {
                    exception.message ?: "添加到曲库失败。"
                }
            } finally {
                addingTrack = false
            }
        }
    }

    fun loadPages(track: Track) {
        if (videoPages.containsKey(track.bvid) || track.bvid in loadingPageVideos) return
        loadingPageVideos.add(track.bvid)
        scope.launch {
            try {
                val pages = withContext(Dispatchers.IO) { repository.pages(track) }
                videoPages[track.bvid] = pages
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                errorMessage = exception.message ?: "读取分 P 信息失败。"
            } finally {
                loadingPageVideos.remove(track.bvid)
            }
        }
    }

    fun togglePages(track: Track) {
        if (track.bvid in expandedSearchVideos) {
            val resultIndex = results.indexOfFirst { it.bvid == track.bvid }
            val collapsedHeaderIndex = if (resultIndex >= 0) {
                results.take(resultIndex).sumOf { previousTrack ->
                    if (previousTrack.bvid in expandedSearchVideos) {
                        1 + (videoPages[previousTrack.bvid]?.size ?: 1) +
                            if (videoPages[previousTrack.bvid] != null) 1 else 0
                    } else {
                        1
                    }
                }
            } else {
                0
            }
            val headerKey = "search-expanded-${track.bvid}-$resultIndex"
            val headerOffset = searchResultsListState.layoutInfo.visibleItemsInfo
                .firstOrNull { it.key == headerKey }
                ?.offset ?: 0
            searchResultsListState.requestScrollToItem(collapsedHeaderIndex, -headerOffset)
            expandedSearchVideos.remove(track.bvid)
            return
        }
        if ((videoPages[track.bvid]?.size ?: 0) > 1) {
            expandedSearchVideos.add(track.bvid)
        }
        loadPages(track)
    }

    fun removeFromLibrary(track: Track) {
        scope.launch {
            try {
                withContext(Dispatchers.IO) { libraryStore.remove(track) }
                refreshLibrary()
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                errorMessage = exception.message ?: "从曲库移除失败。"
            }
        }
    }

    fun removeVideoFromLibrary(bvid: String) {
        scope.launch {
            try {
                withContext(Dispatchers.IO) { libraryStore.removeVideo(bvid) }
                refreshLibrary()
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                errorMessage = exception.message ?: "从曲库移除合集失败。"
            }
        }
    }

    fun renameInLibrary(track: Track, newTitle: String) {
        val title = newTitle.trim()
        if (title.isBlank()) {
            errorMessage = "歌曲名称不能为空。"
            return
        }
        scope.launch {
            try {
                withContext(Dispatchers.IO) { libraryStore.rename(track, title) }
                val renamedTrack = track.copy(title = title)
                val index = library.indexOfFirst { it.libraryId == track.libraryId }
                if (index >= 0) library[index] = renamedTrack
                if (playbackTrack?.libraryId == track.libraryId) playbackTrack = renamedTrack
                renameTarget = null
                errorMessage = null
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                errorMessage = exception.message ?: "重命名失败。"
            }
        }
    }

    fun renameVideoInLibrary(bvid: String, newTitle: String) {
        val title = newTitle.trim()
        if (title.isBlank()) {
            errorMessage = "合集名称不能为空。"
            return
        }
        scope.launch {
            try {
                withContext(Dispatchers.IO) { libraryStore.renameVideo(bvid, title) }
                library.indices
                    .filter { library[it].bvid == bvid }
                    .forEach { index ->
                        library[index] = library[index].copy(videoTitle = title)
                    }
                renameVideoBvid = null
                errorMessage = null
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                errorMessage = exception.message ?: "重命名合集失败。"
            }
        }
    }

    androidx.compose.runtime.LaunchedEffect(selectedTab) {
        if (selectedTab == AppTab.Library || selectedTab == AppTab.UserInfo) refreshLibrary()
        val cookies = currentBilibiliCookieHeader()
        repository.updateWebCookies(cookies)
        isBilibiliLoggedIn = hasBilibiliSession(cookies)
        if (isBilibiliLoggedIn) {
            val profile = runCatching { withContext(Dispatchers.IO) { repository.userProfile() } }.getOrNull()
            bilibiliUsername = profile?.username.orEmpty()
            bilibiliAvatar = profile?.avatarUrl.orEmpty()
                bilibiliVipBadge = profile?.vipBadgeUrl.orEmpty()
            bilibiliIsVip = profile?.isVip == true
            if (!bilibiliIsVip && playbackQuality == PlaybackQuality.HI_RES) {
                playbackQuality = PlaybackQuality.P1080
                settingsPreferences.edit().putString("playback-quality", PlaybackQuality.P1080.name).apply()
            }
        } else {
            bilibiliUsername = ""
            bilibiliAvatar = ""
                bilibiliVipBadge = ""
            bilibiliIsVip = false
            if (playbackQuality == PlaybackQuality.HI_RES) {
                playbackQuality = PlaybackQuality.P1080
                settingsPreferences.edit().putString("playback-quality", PlaybackQuality.P1080.name).apply()
            }
        }
    }

    androidx.compose.runtime.LaunchedEffect(isBilibiliLoggedIn) {
        if (isBilibiliLoggedIn) {
            val profile = runCatching { withContext(Dispatchers.IO) { repository.userProfile() } }.getOrNull()
            bilibiliUsername = profile?.username.orEmpty()
            bilibiliAvatar = profile?.avatarUrl.orEmpty()
            bilibiliVipBadge = profile?.vipBadgeUrl.orEmpty()
            bilibiliIsVip = profile?.isVip == true
            if (!bilibiliIsVip && playbackQuality == PlaybackQuality.HI_RES) {
                playbackQuality = PlaybackQuality.P1080
                settingsPreferences.edit().putString("playback-quality", PlaybackQuality.P1080.name).apply()
            }
        }
    }

    androidx.compose.runtime.LaunchedEffect(player) {
        val saved = withContext(Dispatchers.IO) { playbackStateStore.load() } ?: return@LaunchedEffect
        playbackVolume = readSystemMediaVolume()
        player.volume = 1f
        shufflePlayback = saved.shuffleEnabled
        playbackTrack = saved.currentTrack
        playbackQueue = saved.queue
        if (player.mediaItemCount > 0) {
            // The service owns the live player. Reopening the Activity from
            // its notification must reconnect to that playback instead of
            // replacing it with a paused copy of the saved state.
            playbackIsPlaying = player.isPlaying
            return@LaunchedEffect
        }
        playTrack(
            track = saved.currentTrack,
            queue = saved.queue,
            startPositionMs = saved.positionMs,
            resumePlaying = false,
        )
    }

    androidx.compose.runtime.LaunchedEffect(playbackTrack, playbackIsPlaying) {
        while (playbackTrack != null && playbackIsPlaying) {
            delay(1_000)
            savePlaybackState()
        }
    }

    if (showBilibiliLogin) {
        BackHandler { showBilibiliLogin = false }
        androidx.compose.runtime.LaunchedEffect(Unit) {
            while (showBilibiliLogin) {
                val cookies = currentBilibiliCookieHeader()
                if (cookies != null) {
                    repository.updateWebCookies(cookies)
                    isBilibiliLoggedIn = true
                    showBilibiliLogin = false
                } else {
                    delay(LOGIN_COOKIE_POLL_INTERVAL_MILLIS)
                }
            }
        }
        BilibiliLoginWebView(
            modifier = Modifier.fillMaxSize(),
            onCookiesChanged = { cookies ->
                val currentCookies = currentBilibiliCookieHeader() ?: cookies
                repository.updateWebCookies(currentCookies)
                val loggedIn = hasBilibiliSession(currentCookies)
                isBilibiliLoggedIn = loggedIn
                if (loggedIn) showBilibiliLogin = false
            },
            onPageError = { errorMessage = it },
        )
        return
    }

    androidx.compose.runtime.LaunchedEffect(showPlayerDetails, playbackTrack) {
        while (showPlayerDetails && playbackTrack != null) {
            playbackPosition = player.currentPosition.coerceAtLeast(0L)
            playbackDuration = player.duration.takeIf { it > 0L } ?: 0L
            delay(500)
        }
    }

    addTarget?.let { track ->
        AlertDialog(
            onDismissRequest = {
                if (!addingTrack) {
                    addTarget = null
                    pendingAddTracks = emptyList()
                    pendingAddIsCollection = false
                }
            },
            title = {
                Text(if (pendingAddIsCollection && pendingAddTracks.size > 1) "添加合集到曲库" else "添加到曲库")
            },
            text = {
                OutlinedTextField(
                    value = renameTitle,
                    onValueChange = { renameTitle = it },
                    label = {
                        Text(if (pendingAddIsCollection && pendingAddTracks.size > 1) "合集名称" else "歌曲名称")
                    },
                    singleLine = true,
                    enabled = !addingTrack,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { confirmAddToLibrary() },
                    enabled = !addingTrack,
                ) {
                    Text(if (addingTrack) "添加中…" else "加入")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        addTarget = null
                        pendingAddTracks = emptyList()
                        pendingAddIsCollection = false
                    },
                    enabled = !addingTrack,
                ) {
                    Text("取消")
                }
            },
        )
    }

    renameTarget?.let { track ->
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text("重命名歌曲") },
            text = {
                OutlinedTextField(
                    value = renameTitle,
                    onValueChange = { renameTitle = it },
                    label = { Text("歌曲名称") },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(onClick = { renameInLibrary(track, renameTitle) }) {
                    Text("保存")
                }
            },
            dismissButton = {
                TextButton(onClick = { renameTarget = null }) {
                    Text("取消")
                }
            },
        )
    }

    renameVideoBvid?.let { bvid ->
        AlertDialog(
            onDismissRequest = { renameVideoBvid = null },
            title = { Text("重命名合集") },
            text = {
                OutlinedTextField(
                    value = renameTitle,
                    onValueChange = { renameTitle = it },
                    label = { Text("合集名称") },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(onClick = { renameVideoInLibrary(bvid, renameTitle) }) {
                    Text("保存")
                }
            },
            dismissButton = {
                TextButton(onClick = { renameVideoBvid = null }) {
                    Text("取消")
                }
            },
        )
    }

    if (showPlayerDetails) {
        val track = playbackTrack
        if (track != null) {
            PlayerDetailsScreen(
                track = track,
                accentColor = navigationAccent,
                isPlaying = playbackIsPlaying,
                isLoading = playbackLoading,
                status = playbackStatus,
                position = playbackPosition,
                duration = playbackDuration,
                volume = playbackVolume,
                canPlayPrevious = playbackQueue.isNotEmpty() && (
                    playbackQueue.indexOfFirst { it.libraryId == track.libraryId } > 0 ||
                        playbackQueue.size > 1 ||
                        playbackPosition > 3_000L
                    ),
                canPlayNext = playbackQueue.size > 1,
                onClose = { showPlayerDetails = false },
                onPlayPause = {
                    focusManager.clearFocus()
                    if (player.isPlaying) {
                        player.pause()
                    } else if (!playbackLoading) {
                        if (player.playbackState == Player.STATE_ENDED) player.seekTo(0)
                        player.play()
                    }
                },
                onPrevious = { playPreviousTrack() },
                onNext = { playNextTrack() },
                onSeek = {
                    player.seekTo(it)
                    savePlaybackState(it)
                },
                onVolumeChange = {
                    val maximum = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                    val requested = (it.coerceIn(0f, 1f) * maximum).roundToInt()
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, requested, 0)
                    playbackVolume = readSystemMediaVolume()
                    savePlaybackState()
                },
                queue = playbackQueue,
                shuffleEnabled = shufflePlayback,
                onToggleShuffle = { setShuffleMode(!shufflePlayback) },
                onListPlayback = { setShuffleMode(false) },
                onSelectTrack = { playTrack(it, playbackQueue) },
            )
            return
        }
    }

    Scaffold(
        topBar = {},
        bottomBar = {
            Column {
                playbackTrack?.let { track ->
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .clickable { showPlayerDetails = true }
                            .then(dismissSwipeOnTap),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (track.coverUrl.isNotBlank()) {
                            AsyncImage(
                                model = track.coverUrl,
                                contentDescription = null,
                                modifier = Modifier.size(44.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop,
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    track.title,
                                    modifier = Modifier.weight(1f, fill = false),
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                if (track.author.isNotBlank()) {
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        track.author,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                            Text(
                                if (playbackLoading) playbackStatus else
                                    playbackStatus,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        IconButton(
                            onClick = {
                                focusManager.clearFocus()
                                if (player.isPlaying) {
                                    player.pause()
                                } else if (!playbackLoading) {
                                    if (player.playbackState == Player.STATE_ENDED) player.seekTo(0)
                                    player.play()
                                }
                            },
                            enabled = !playbackLoading,
                        ) {
                            Icon(
                                imageVector = if (playbackIsPlaying) {
                                    Icons.Filled.Pause
                                } else {
                                    Icons.Filled.PlayArrow
                                },
                                contentDescription = if (playbackIsPlaying) "暂停" else "播放",
                            )
                        }
                        val currentIndex = playbackQueue.indexOfFirst {
                            it.libraryId == track.libraryId
                        }
                        IconButton(
                            onClick = { playNextTrack() },
                            enabled = !playbackLoading && currentIndex >= 0 &&
                                playbackQueue.size > 1,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.SkipNext,
                                contentDescription = "下一首",
                            )
                        }
                    }
                }
                NavigationBar(
                    content = {
                        AppTab.entries.forEach { tab ->
                            NavigationBarItem(
                                selected = selectedTab == tab,
                                onClick = { selectedTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = when (tab) {
                                            AppTab.Search -> Icons.Filled.Search
                                            AppTab.Library -> Icons.Filled.LibraryMusic
                                            AppTab.UserInfo -> Icons.Filled.Person
                                        },
                                        contentDescription = tab.label,
                                    )
                                },
                                label = { Text(tab.label) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = navigationAccent,
                                    selectedTextColor = navigationAccent,
                                    indicatorColor = navigationAccent.copy(alpha = 0.14f),
                                ),
                            )
                        }
                    },
                )
            }
        },
    ) { padding ->
        Box(
            Modifier.fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { focusManager.clearFocus() })
                }
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(
                            requireUnconsumed = false,
                            pass = PointerEventPass.Initial,
                        )
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Final)
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            val movement = change.position - down.position
                            if (abs(movement.y) > viewConfiguration.touchSlop &&
                                abs(movement.y) > abs(movement.x)
                            ) {
                                focusManager.clearFocus()
                                break
                            }
                            if (!change.pressed) break
                        }
                    }
                },
        ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)
                .then(dismissSwipeOnTap),
        ) {
            when (selectedTab) {
                AppTab.Search -> {
                    Text(
                        "搜索",
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(top = 12.dp, bottom = 8.dp),
                    )
                    val searchControls: @Composable () -> Unit = {
                        OutlinedTextField(
                            value = searchText,
                            onValueChange = {
                                searchText = it
                                successMessage = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("搜索视频或输入 B 站视频 URL") },
                            singleLine = true,
                            shape = RoundedCornerShape(28.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = navigationAccent,
                                focusedLabelColor = navigationAccent,
                                cursorColor = navigationAccent,
                            ),
                            trailingIcon = {
                                if (searchText.isNotEmpty()) {
                                    IconButton(
                                        onClick = {
                                            searchText = ""
                                            successMessage = null
                                            errorMessage = null
                                        },
                                    ) {
                                        Icon(
                                            Icons.Filled.Cancel,
                                            contentDescription = "清空输入",
                                        )
                                    }
                                }
                            },
                        )
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = { submitSearch() },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !loading && searchText.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = navigationAccent),
                        ) {
                            Text(
                                when {
                                    loading -> "处理中…"
                                    searchText.trim().startsWith("http", ignoreCase = true) ||
                                        searchText.trim().startsWith("www.", ignoreCase = true) ->
                                        "添加到曲库"
                                    else -> "搜索"
                                },
                            )
                        }
                    }
                    if (results.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                searchControls()
                                if (loading) {
                                    CircularProgressIndicator(modifier = Modifier.padding(top = 16.dp))
                                }
                            }
                        }
                    } else {
                        Column {
                            searchControls()
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                state = searchResultsListState,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                results.forEachIndexed { resultIndex, track ->
                                    val isExpanded = track.bvid in expandedSearchVideos
                                    val row: @Composable () -> Unit = {
                                        androidx.compose.runtime.LaunchedEffect(track.bvid) {
                                            loadPages(track)
                                        }
                                        TrackRow(
                                            track = track,
                                            actionLabel = "加入曲库",
                                            onAction = { addVideoToLibrary(track) },
                                            currentlyPlaying = playbackIsPlaying && playbackTrack?.libraryId == track.libraryId,
                                            onPlay = { playSearchResult(track) },
                                            expanded = isExpanded,
                                            pages = videoPages[track.bvid],
                                            onTogglePages = { togglePages(track) },
                                            showCover = true,
                                            onAddPage = { addToLibrary(it) },
                                            renderExpandedPages = false,
                                        )
                                    }
                                    if (isExpanded) {
                                        stickyHeader(key = "search-expanded-${track.bvid}-$resultIndex") {
                                            Box(
                                                modifier = Modifier.fillMaxWidth()
                                                    .background(MaterialTheme.colorScheme.surface),
                                            ) { row() }
                                        }
                                        val pages = videoPages[track.bvid]
                                        if (pages == null) {
                                            item(key = "search-pages-loading-${track.bvid}") {
                                                CircularProgressIndicator(modifier = Modifier.padding(12.dp))
                                            }
                                        } else {
                                            items(pages, key = { "search-page-${track.bvid}-${it.pageNumber}" }) { pageTrack ->
                                                Row(
                                                    modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                ) {
                                                    if (playbackTrack?.libraryId == pageTrack.libraryId) {
                                                        Icon(
                                                            imageVector = if (playbackIsPlaying) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                                                            contentDescription = "正在播放此分 P",
                                                            tint = MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.size(20.dp),
                                                        )
                                                    } else {
                                                        Spacer(Modifier.width(20.dp))
                                                    }
                                                    Text(
                                                        pageTrack.title,
                                                        modifier = Modifier.weight(1f).padding(top = 10.dp, bottom = 10.dp, end = 8.dp)
                                                            .clickable { playTrack(pageTrack, listOf(pageTrack)) },
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        maxLines = 2,
                                                        overflow = TextOverflow.Ellipsis,
                                                    )
                                                    IconButton(onClick = { addToLibrary(pageTrack) }) {
                                                        Icon(Icons.Filled.Add, contentDescription = "添加 ${pageTrack.title} 到曲库")
                                                    }
                                                }
                                                HorizontalDivider()
                                            }
                                            stickyHeader(key = "search-expanded-end-${track.bvid}") {
                                                Spacer(Modifier.fillMaxWidth().height(1.dp))
                                            }
                                        }
                                    } else {
                                        item(key = "search-${track.bvid}-$resultIndex") { row() }
                                    }
                                }
                                item {
                                    TextButton(
                                        onClick = { search(page + 1) },
                                        enabled = !loading,
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Text(if (loading) "正在加载…" else "加载更多")
                                    }
                                }
                            }
                        }
                    }
                }

                AppTab.Library -> {
                    Text(
                        "曲库",
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(top = 12.dp, bottom = 8.dp),
                    )
                    OutlinedTextField(
                        value = libraryFilter,
                        onValueChange = { libraryFilter = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("搜索收藏歌曲") },
                        leadingIcon = {
                            Icon(Icons.Filled.Search, contentDescription = null)
                        },
                        trailingIcon = {
                            if (libraryFilter.isNotEmpty()) {
                                IconButton(onClick = { libraryFilter = "" }) {
                                    Icon(Icons.Filled.Cancel, contentDescription = "清空筛选")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(28.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = navigationAccent,
                            focusedLabelColor = navigationAccent,
                            cursorColor = navigationAccent,
                        ),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Button(
                            onClick = { playLibrary(shuffle = false) },
                            modifier = Modifier.weight(1f),
                            enabled = library.isNotEmpty(),
                            shape = RoundedCornerShape(32.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = navigationAccent),
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null)
                            Text("播放")
                        }
                        Button(
                            onClick = { playLibrary(shuffle = true) },
                            modifier = Modifier.weight(1f),
                            enabled = library.isNotEmpty(),
                            shape = RoundedCornerShape(32.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = navigationAccent),
                        ) {
                            Icon(Icons.Filled.Shuffle, contentDescription = null)
                            Text("随机播放")
                        }
                    }
                    val filteredLibraryGroups = library.groupBy { it.collectionId ?: it.libraryId }.mapNotNull { (groupId, tracks) ->
                        val bvid = tracks.first().bvid
                        val collectionTitle = tracks.first().videoTitle.ifBlank {
                            tracks.first().title
                        }
                        val collectionMatches = libraryFilter.isBlank() ||
                            collectionTitle.contains(libraryFilter, ignoreCase = true) ||
                            tracks.first().author.contains(libraryFilter, ignoreCase = true)
                        val visibleTracks = if (collectionMatches) {
                            tracks
                        } else {
                            tracks.filter { track ->
                                track.title.contains(libraryFilter, ignoreCase = true) ||
                                    track.author.contains(libraryFilter, ignoreCase = true)
                            }
                        }
                        if (visibleTracks.isEmpty()) null else Triple(groupId, tracks, visibleTracks)
                    }
                    if (filteredLibraryGroups.isEmpty()) {
                        Text(
                            if (library.isEmpty()) "曲库还是空的。搜索视频后可将曲目添加到这里。"
                            else "没有匹配的歌曲。",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(vertical = 16.dp),
                        )
                    }
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        filteredLibraryGroups.forEach { (groupId, allTracks, visibleTracks) ->
                            val bvid = allTracks.first().bvid
                            if (allTracks.size > 1 && allTracks.first().collectionId != null) {
                                val firstTrack = allTracks.first()
                                val collectionTrack = firstTrack.copy(
                                    title = firstTrack.videoTitle.ifBlank { firstTrack.title },
                                    duration = "${allTracks.size} 个分P",
                                    pageNumber = 0,
                                    partTitle = "",
                                )
                                stickyHeader(key = "library-group-$bvid") {
                                    Box(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                                        SwipeableLibraryTrackRow(
                                            track = collectionTrack,
                                            currentlyPlaying = false,
                                            expanded = bvid in expandedLibraryVideos,
                                            pages = allTracks,
                                            onTogglePages = {
                                                if (bvid in expandedLibraryVideos) {
                                                    expandedLibraryVideos.remove(bvid)
                                                } else {
                                                    expandedLibraryVideos.add(bvid)
                                                }
                                            },
                                            showCover = true,
                                            showExpandedPages = false,
                                            revealed = swipedTrackId == collectionTrack.libraryId,
                                            onRevealChanged = { revealed ->
                                                swipedTrackId = if (revealed) {
                                                    collectionTrack.libraryId
                                                } else if (swipedTrackId == collectionTrack.libraryId) {
                                                    null
                                                } else {
                                                    swipedTrackId
                                                }
                                            },
                                            onPlay = {
                                                if (bvid in expandedLibraryVideos) {
                                                    expandedLibraryVideos.remove(bvid)
                                                } else {
                                                    expandedLibraryVideos.add(bvid)
                                                }
                                            },
                                            onRename = {
                                                renameTitle = collectionTrack.title
                                                renameVideoBvid = bvid
                                            },
                                            onDelete = { removeVideoFromLibrary(bvid) },
                                        )
                                    }
                                }
                                if (bvid in expandedLibraryVideos) {
                                    items(visibleTracks, key = { it.libraryId }) { track ->
                                        SwipeableLibraryTrackRow(
                                            track = track,
                                            currentlyPlaying = playbackIsPlaying &&
                                                playbackTrack?.libraryId == track.libraryId,
                                            expanded = false,
                                            pages = null,
                                            onTogglePages = {},
                                            showCover = false,
                                            showExpandedPages = false,
                                            revealed = swipedTrackId == track.libraryId,
                                            onRevealChanged = { revealed ->
                                                swipedTrackId = if (revealed) {
                                                    track.libraryId
                                                } else if (swipedTrackId == track.libraryId) {
                                                    null
                                                } else {
                                                    swipedTrackId
                                                }
                                            },
                                            onPlay = { playTrack(track, library.toList()) },
                                            onRename = {
                                                renameTitle = track.title
                                                renameTarget = track
                                            },
                                            onDelete = { removeFromLibrary(track) },
                                        )
                                    }
                                }
                            } else {
                                val track = visibleTracks.first()
                                item(key = track.libraryId) {
                                    SwipeableLibraryTrackRow(
                                        track = track,
                                        currentlyPlaying = playbackIsPlaying &&
                                            playbackTrack?.libraryId == track.libraryId,
                                        expanded = false,
                                        pages = null,
                                        onTogglePages = {},
                                        showCover = true,
                                        showExpandedPages = false,
                                        revealed = swipedTrackId == track.libraryId,
                                        onRevealChanged = { revealed ->
                                            swipedTrackId = if (revealed) {
                                                track.libraryId
                                            } else if (swipedTrackId == track.libraryId) {
                                                null
                                            } else {
                                                swipedTrackId
                                            }
                                        },
                                        onPlay = { playTrack(track, library.toList()) },
                                        onRename = {
                                            renameTitle = track.title
                                            renameTarget = track
                                        },
                                        onDelete = { removeFromLibrary(track) },
                                    )
                                }
                            }
                        }
                    }
                }

                AppTab.UserInfo -> {
                    Text("\u6211\u7684", style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(top = 12.dp, bottom = 8.dp))
                    LazyColumn(modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        item {
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                                    Text("\u767b\u5f55\u72b6\u6001", style = MaterialTheme.typography.titleMedium)
                                    Row(Modifier.fillMaxWidth().padding(top = 16.dp),
                                        verticalAlignment = Alignment.CenterVertically) {
                                        if (bilibiliAvatar.isNotBlank()) {
                                            AsyncImage(model = bilibiliAvatar, contentDescription = "B\u7ad9\u7528\u6237\u5934\u50cf",
                                                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(28.dp)),
                                                contentScale = ContentScale.Crop)
                                        } else {
                                            Icon(Icons.Filled.Person, contentDescription = null,
                                                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(28.dp))
                                                    .background(MaterialTheme.colorScheme.surface).padding(12.dp))
                                        }
                                        Column(Modifier.weight(1f).padding(start = 12.dp)) {
                                            Text(if (isBilibiliLoggedIn) bilibiliUsername.ifBlank { "B\u7ad9\u7528\u6237" } else "\u672a\u767b\u5f55",
                                                style = MaterialTheme.typography.titleMedium,
                                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            if (bilibiliIsVip) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    AsyncImage(
                                                        model = bilibiliVipBadge.ifBlank {
                                                            "https://i0.hdslb.com/bfs/vip/8d4f8bfc713826a5412a0a27eaaac4d6b9ede1d9.png"
                                                        },
                                                        contentDescription = "B站大会员标识",
                                                        modifier = Modifier.width(48.dp).height(18.dp),
                                                        contentScale = ContentScale.Fit,
                                                    )
                                                    Spacer(Modifier.width(4.dp))
                                                    Text("\u5927\u4f1a\u5458", style = MaterialTheme.typography.bodySmall,
                                                        color = Color(0xFFFB7299))
                                                }
                                            } else {
                                                Text("\u975e\u5927\u4f1a\u5458", style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    }
                                    Button(onClick = {
                                        errorMessage = null
                                        if (isBilibiliLoggedIn) {
                                            val cookieManager = WebCookieManager.getInstance()
                                            repository.updateWebCookies(null)
                                            isBilibiliLoggedIn = false
                                            bilibiliUsername = ""
                                            bilibiliAvatar = ""
                                            bilibiliVipBadge = ""
                                            bilibiliIsVip = false
                                            if (playbackQuality == PlaybackQuality.HI_RES) {
                                                playbackQuality = PlaybackQuality.P1080
                                                settingsPreferences.edit().putString("playback-quality", PlaybackQuality.P1080.name).apply()
                                            }
                                            showBilibiliLogin = false
                                            WebStorage.getInstance().deleteOrigin(BILIBILI_WEB_ORIGIN)
                                            WebStorage.getInstance().deleteOrigin(BILIBILI_PASSPORT_ORIGIN)
                                            cookieManager.removeAllCookies { removed ->
                                                cookieManager.flush()
                                                if (!removed && cookieManager.getCookie(BILIBILI_API_COOKIE_URL)
                                                        ?.split(';')?.any { it.trim().startsWith("SESSDATA=") } == true)
                                                    errorMessage = "\u9000\u51fa\u767b\u5f55\u5931\u8d25\uff0c\u8bf7\u7a0d\u540e\u91cd\u8bd5\u3002"
                                            }
                                        } else showBilibiliLogin = true
                                    }, modifier = Modifier.padding(top = 12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = navigationAccent)) {
                                        Text(if (isBilibiliLoggedIn) "\u9000\u51fa\u767b\u5f55" else "\u767b\u5f55 B\u7ad9")
                                    }
                                }
                            }
                        }
                        item {
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                                    Text("\u64ad\u653e\u9009\u9879", style = MaterialTheme.typography.titleMedium)
                                    Text("\u97f3\u89c6\u9891\u83b7\u53d6\u8d28\u91cf", style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
                                    PlaybackQuality.entries.forEach { quality ->
                                        val qualityEnabled = quality != PlaybackQuality.HI_RES || bilibiliIsVip
                                        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                                            .clickable(enabled = qualityEnabled) {
                                                selectPlaybackQuality(quality)
                                            }.padding(vertical = 10.dp, horizontal = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically) {
                                            Column(Modifier.weight(1f)) {
                                                Text(quality.label, style = MaterialTheme.typography.bodyLarge)
                                                Text(when (quality) {
                                                    PlaybackQuality.HI_RES -> if (bilibiliIsVip) "\u4f18\u5148\u9009\u62e9\u65e0\u635f\u97f3\u9891" else "\u4ec5\u9650 B \u7ad9\u5927\u4f1a\u5458\u4f7f\u7528"
                                                    PlaybackQuality.P1080 -> "\u4f18\u5148\u83b7\u53d6\u6700\u9ad8\u53ef\u7528\u97f3\u8d28"
                                                    PlaybackQuality.P360 -> "\u83b7\u53d6\u4f4e\u7801\u7387\u97f3\u9891\uff0c\u8282\u7701\u6d41\u91cf"
                                                }, style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                            androidx.compose.material3.RadioButton(
                                                selected = playbackQuality == quality,
                                                enabled = qualityEnabled,
                                                colors = RadioButtonDefaults.colors(selectedColor = navigationAccent),
                                                onClick = {
                                                    selectPlaybackQuality(quality)
                                                })
                                        }
                                    }
                                }
                            }
                        }
                        item {
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                                    Text("\u5916\u89c2\u6a21\u5f0f", style = MaterialTheme.typography.titleMedium)
                                    AppThemeMode.entries.forEach { mode ->
                                        Row(
                                            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                                                .clickable { onThemeModeChanged(mode) }
                                                .padding(vertical = 6.dp, horizontal = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Text(mode.label, modifier = Modifier.weight(1f),
                                                style = MaterialTheme.typography.bodyLarge)
                                            androidx.compose.material3.RadioButton(
                                                selected = themeMode == mode,
                                                onClick = { onThemeModeChanged(mode) },
                                                colors = RadioButtonDefaults.colors(selectedColor = navigationAccent),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        item {
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                                    Text("\u8bca\u65ad\u4e0e\u652f\u6301", style = MaterialTheme.typography.titleMedium)
                                    OutlinedButton(onClick = {
                                        scope.launch {
                                            val file = withContext(Dispatchers.IO) {
                                                PlaybackDiagnostics.record(context, "diagnostics.fileGenerated")
                                                PlaybackDiagnostics.generateFile(context)
                                            }
                                            successMessage = "\u65e5\u5fd7\u6587\u4ef6\u5df2\u4fdd\u5b58\uff1a${file.absolutePath}"
                                        }
                                    }, modifier = Modifier.padding(top = 8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = navigationAccent)) {
                                        Text("\u751f\u6210\u65e5\u5fd7\u6587\u4ef6")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        AnimatedVisibility(
            visible = toastMessage != null,
            enter = slideInVertically(animationSpec = tween(260)) { -it } + fadeIn(tween(220)),
            exit = slideOutVertically(animationSpec = tween(200)) { -it } + fadeOut(tween(180)),
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 12.dp, start = 20.dp, end = 20.dp),
        ) {
            toastMessage?.let { (message, isError) ->
                Text(
                    text = message,
                    color = if (isError) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                )
            }
        }
        }
    }
}

@Composable
private fun BilibiliLoginWebView(
    modifier: Modifier = Modifier,
    onCookiesChanged: (String?) -> Unit,
    onPageError: (String) -> Unit,
) {
    val context = LocalContext.current
    val latestOnCookiesChanged = rememberUpdatedState(onCookiesChanged)
    val latestOnPageError = rememberUpdatedState(onPageError)
    val webView = remember {
        WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true
            settings.textZoom = 100
            settings.setSupportZoom(false)
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            isVerticalScrollBarEnabled = true
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView, url: String) {
                    if (url.startsWith("https://passport.bilibili.com/")) {
                        view.evaluateJavascript(
                            """
                            (() => {
                              const repair = () => {
                                const visible = (element) => {
                                  const rect = element.getBoundingClientRect();
                                  return rect.width > 0 && rect.height > 0;
                                };
                                const consentPhrase = '\u6211\u5df2\u9605\u8bfb\u5e76\u540c\u610f';
                                const consent = Array.from(document.querySelectorAll('body *'))
                                  .filter(element => element.textContent.includes(consentPhrase) && visible(element))
                                  .sort((a, b) => {
                                    const ar = a.getBoundingClientRect();
                                    const br = b.getBoundingClientRect();
                                    return ar.width * ar.height - br.width * br.height;
                                  })[0];
                                const loginLabel = '\u767b\u5f55';
                                const loginNodes = Array.from(document.querySelectorAll('body *'))
                                  .filter(element => element.children.length === 0 &&
                                    element.textContent.trim() === loginLabel && visible(element));
                                const buttons = loginNodes.map(label => {
                                  for (let element = label; element && element !== document.body;
                                       element = element.parentElement) {
                                    const rect = element.getBoundingClientRect();
                                    if (rect.width >= window.innerWidth * 0.72 &&
                                        rect.height >= 32 && rect.height <= 150) return element;
                                  }
                                  return null;
                                }).filter(Boolean).sort((a, b) => {
                                  const ar = a.getBoundingClientRect();
                                  const br = b.getBoundingClientRect();
                                  return ar.width * ar.height - br.width * br.height;
                                });
                                if (!consent || !buttons.length) return;
                                const consentRect = consent.getBoundingClientRect();
                                const button = buttons[0];
                                const buttonRect = button.getBoundingClientRect();
                                const overlap = consentRect.bottom + 12 - buttonRect.top;
                                if (overlap > 0) {
                                  const shift = Math.min(overlap, 64);
                                  button.style.position = 'relative';
                                  button.style.top = shift + 'px';
                                  button.style.zIndex = '2';
                                  const accountPhrase = '\u8d26\u53f7\u5bc6\u7801\u767b\u5f55';
                                  const accountLogin = Array.from(document.querySelectorAll('body *'))
                                    .filter(element => element.children.length === 0 &&
                                      element.textContent.trim() === accountPhrase && visible(element))
                                    .sort((a, b) => {
                                      const ar = a.getBoundingClientRect();
                                      const br = b.getBoundingClientRect();
                                      return ar.width * ar.height - br.width * br.height;
                                    })[0];
                                  if (accountLogin) {
                                    accountLogin.style.position = 'relative';
                                    // Keep the alternate login link below the primary button
                                    // while leaving enough room for it to remain visible.
                                    accountLogin.style.top = '32px';
                                    accountLogin.style.zIndex = '1';
                                  }
                                }
                              };
                              repair();
                              setTimeout(repair, 300);
                              setTimeout(repair, 1000);
                            })();
                            """.trimIndent(),
                            null,
                        )
                    }
                    if (url.startsWith("https://www.bilibili.com/") ||
                        url.startsWith("https://passport.bilibili.com/")
                    ) {
                        val cookies = WebCookieManager.getInstance()
                            .getCookie("https://api.bilibili.com")
                        latestOnCookiesChanged.value(cookies)
                    }
                }

                override fun onReceivedError(
                    view: WebView,
                    request: WebResourceRequest,
                    error: WebResourceError,
                ) {
                    if (request.isForMainFrame) {
                        latestOnPageError.value("B 站登录页面加载失败：${error.description}")
                    }
                }
            }
            loadUrl(BILIBILI_LOGIN_URL)
        }
    }
    DisposableEffect(webView) {
        onDispose {
            webView.stopLoading()
            webView.destroy()
        }
    }
    AndroidView(
        factory = { webView },
        modifier = modifier,
    )
}

@Composable
private fun PlayerDetailsScreen(
    track: Track,
    accentColor: Color,
    isPlaying: Boolean,
    isLoading: Boolean,
    status: String,
    position: Long,
    duration: Long,
    volume: Float,
    canPlayPrevious: Boolean,
    canPlayNext: Boolean,
    onClose: () -> Unit,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onVolumeChange: (Float) -> Unit,
    queue: List<Track>,
    shuffleEnabled: Boolean,
    onToggleShuffle: () -> Unit,
    onListPlayback: () -> Unit,
    onSelectTrack: (Track) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    var showQueue by remember { mutableStateOf(false) }
    BackHandler(onBack = onClose)
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        accentColor.copy(alpha = 0.28f),
                        colors.surfaceVariant,
                        colors.surface,
                    ),
                ),
            ),
    ) {
        val coverSize = minOf(maxWidth * 0.78f, maxHeight * 0.36f, 360.dp)
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Filled.Close, contentDescription = "关闭播放详情", tint = accentColor)
                }
                Text(
                    "正在播放",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.size(48.dp))
            }

            Box(
                modifier = Modifier.size(coverSize)
                    .clip(RoundedCornerShape(20.dp))
                    .background(colors.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                if (track.coverUrl.isNotBlank()) {
                    AsyncImage(
                        model = track.coverUrl,
                        contentDescription = "${track.title} 封面",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Icon(
                        Icons.Filled.MusicNote,
                        contentDescription = null,
                        modifier = Modifier.size(72.dp),
                        tint = colors.onSurfaceVariant,
                    )
                }
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            track.title,
                            style = MaterialTheme.typography.headlineSmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            track.author.ifBlank { status },
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    IconButton(onClick = onToggleShuffle) {
                        Icon(
                            Icons.Filled.Shuffle,
                            contentDescription = if (shuffleEnabled) "随机播放已开启" else "开启随机播放",
                            tint = if (shuffleEnabled) accentColor else colors.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = onListPlayback) {
                        Icon(
                            Icons.Filled.Repeat,
                            contentDescription = if (shuffleEnabled) "切换列表播放" else "列表循环播放已开启",
                            tint = if (shuffleEnabled) colors.onSurfaceVariant else accentColor,
                        )
                    }
                    IconButton(onClick = { showQueue = true }) {
                        Icon(
                            Icons.AutoMirrored.Filled.QueueMusic,
                            contentDescription = "播放列表",
                            tint = accentColor,
                        )
                    }
                }
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                Slider(
                    value = if (duration > 0L) {
                        (position.toFloat() / duration).coerceIn(0f, 1f)
                    } else {
                        0f
                    },
                    onValueChange = { fraction ->
                        if (duration > 0L) onSeek((duration * fraction).toLong())
                    },
                    enabled = duration > 0L && !isLoading,
                    colors = SliderDefaults.colors(
                        thumbColor = accentColor,
                        activeTrackColor = accentColor,
                        inactiveTrackColor = accentColor.copy(alpha = 0.24f),
                    ),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(formatPlaybackTime(position), color = colors.onSurfaceVariant)
                    Text(
                        "-${formatPlaybackTime((duration - position).coerceAtLeast(0L))}",
                        color = colors.onSurfaceVariant,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onPrevious, enabled = canPlayPrevious && !isLoading) {
                    Icon(
                        Icons.Filled.SkipPrevious,
                        contentDescription = "上一首",
                        modifier = Modifier.size(36.dp),
                        tint = accentColor,
                    )
                }
                IconButton(onClick = onPlayPause, enabled = !isLoading) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) "暂停" else "播放",
                        modifier = Modifier.size(64.dp),
                        tint = accentColor,
                    )
                }
                IconButton(onClick = onNext, enabled = canPlayNext && !isLoading) {
                    Icon(
                        Icons.Filled.SkipNext,
                        contentDescription = "下一首",
                        modifier = Modifier.size(36.dp),
                        tint = accentColor,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.VolumeDown,
                    contentDescription = "音量低",
                    tint = accentColor,
                )
                Slider(
                    value = volume,
                    onValueChange = onVolumeChange,
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = accentColor,
                        activeTrackColor = accentColor,
                        inactiveTrackColor = accentColor.copy(alpha = 0.24f),
                    ),
                )
                Icon(
                    Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = "音量高",
                    tint = accentColor,
                )
            }
        }
    }
    if (showQueue) {
        AlertDialog(
            onDismissRequest = { showQueue = false },
            title = { Text("播放列表") },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                    items(queue, key = { it.libraryId }) { item ->
                        Text(
                            text = item.title,
                            modifier = Modifier.fillMaxWidth()
                                .clickable {
                                    showQueue = false
                                    onSelectTrack(item)
                                }
                                .padding(vertical = 12.dp),
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showQueue = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = accentColor),
                ) {
                    Text("关闭")
                }
            },
        )
    }
}

private fun formatPlaybackTime(milliseconds: Long): String {
    val totalSeconds = milliseconds.coerceAtLeast(0L) / 1_000L
    return "%d:%02d".format(totalSeconds / 60L, totalSeconds % 60L)
}

private const val BILIBILI_LOGIN_URL =
    "https://passport.bilibili.com/login?gourl=https%3A%2F%2Fwww.bilibili.com%2F"
private const val BILIBILI_API_COOKIE_URL = "https://api.bilibili.com"
private const val BILIBILI_WEB_ORIGIN = "https://www.bilibili.com"
private const val BILIBILI_PASSPORT_ORIGIN = "https://passport.bilibili.com"
private const val LOGIN_COOKIE_POLL_INTERVAL_MILLIS = 500L

private fun hasBilibiliSession(cookies: String?): Boolean = cookies
    ?.split(';')
    ?.any { cookie ->
        val parts = cookie.trim().split('=', limit = 2)
        parts.size == 2 && parts[0] == "SESSDATA" && parts[1].isNotBlank()
    } == true

private fun currentBilibiliCookieHeader(): String? {
    val cookieManager = WebCookieManager.getInstance()
    val cookies = linkedMapOf<String, String>()
    listOf(
        BILIBILI_API_COOKIE_URL,
        BILIBILI_WEB_ORIGIN,
        BILIBILI_PASSPORT_ORIGIN,
    ).forEach { origin ->
        cookieManager.getCookie(origin)
            ?.split(';')
            ?.forEach { cookie ->
                val parts = cookie.trim().split('=', limit = 2)
                if (parts.size == 2 && parts[0].isNotBlank()) {
                    cookies[parts[0]] = parts[1]
                }
            }
    }
    return cookies.entries
        .joinToString("; ") { (name, value) -> "$name=$value" }
        .takeIf(::hasBilibiliSession)
}

@Composable
private fun TrackRow(
    track: Track,
    actionLabel: String,
    onAction: (() -> Unit)?,
    currentlyPlaying: Boolean = false,
    onPlay: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    expanded: Boolean = false,
    pages: List<Track>? = null,
    onTogglePages: (() -> Unit)? = null,
    onAddPage: ((Track) -> Unit)? = null,
    showCover: Boolean = false,
    renderExpandedPages: Boolean = true,
    showBottomDivider: Boolean = true,
) {
    val playingIndicatorColor = MaterialTheme.colorScheme.primary
    Column(
        modifier = modifier.fillMaxWidth()
            .drawBehind {
                if (currentlyPlaying) {
                    drawLine(
                        color = playingIndicatorColor,
                        start = androidx.compose.ui.geometry.Offset.Zero,
                        end = androidx.compose.ui.geometry.Offset(0f, size.height),
                        strokeWidth = 4.dp.toPx(),
                    )
                }
            }
            .padding(start = 12.dp, top = 4.dp, bottom = 4.dp)
            .then(if (onPlay != null) Modifier.clickable(onClick = onPlay) else Modifier),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (showCover) {
                Box(
                    modifier = Modifier.size(64.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    if (track.coverUrl.isNotBlank()) {
                        AsyncImage(
                            model = track.coverUrl,
                            contentDescription = "${track.title} 封面",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                    } else {
                        Icon(Icons.Filled.MusicNote, contentDescription = null)
                    }
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    track.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    listOf(track.author, track.duration).filter { it.isNotBlank() }
                        .joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (onTogglePages != null && (pages?.size ?: 0) > 1) {
                IconButton(onClick = onTogglePages) {
                    Icon(
                        imageVector = if (expanded) {
                            Icons.Filled.ExpandLess
                        } else {
                            Icons.Filled.ExpandMore
                        },
                        contentDescription = if (expanded) "收起分 P" else "展开分 P",
                    )
                }
            }
            if (onAction != null) {
                IconButton(onClick = onAction) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = actionLabel,
                    )
                }
            }
        }
        if (expanded && renderExpandedPages) {
            if (pages == null) {
                CircularProgressIndicator()
            } else {
                pages.forEachIndexed { index, pageTrack ->
                    if (index > 0) HorizontalDivider()
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            pageTrack.title,
                            modifier = Modifier.weight(1f).padding(end = 8.dp)
                                .then(
                                    if (onAddPage != null) {
                                        Modifier.clickable { onAddPage(pageTrack) }
                                    } else {
                                        Modifier
                                    },
                                ),
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (onAddPage != null) {
                            IconButton(onClick = { onAddPage(pageTrack) }) {
                                Icon(
                                    imageVector = Icons.Filled.Add,
                                    contentDescription = "添加 ${pageTrack.title} 到曲库",
                                )
                            }
                        }
                    }
                }
            }
        }
        if (showBottomDivider) HorizontalDivider()
    }
}

@Composable
private fun SwipeableLibraryTrackRow(
    track: Track,
    currentlyPlaying: Boolean,
    expanded: Boolean,
    pages: List<Track>?,
    onTogglePages: () -> Unit,
    showCover: Boolean,
    revealed: Boolean,
    onRevealChanged: (Boolean) -> Unit,
    onPlay: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    showExpandedPages: Boolean = true,
) {
    val actionWidth = 160.dp
    val density = androidx.compose.ui.platform.LocalDensity.current
    val actionWidthPx = with(density) { actionWidth.toPx() }
    var dragOffset by remember(track.libraryId) { mutableStateOf(0f) }
    var isDragging by remember(track.libraryId) { mutableStateOf(false) }
    val animatedOffset by animateFloatAsState(
        targetValue = if (revealed) -actionWidthPx else 0f,
        label = "libraryRowSwipe",
    )
    val offset = if (isDragging) dragOffset else animatedOffset
    val revealProgress = (-offset / actionWidthPx).coerceIn(0f, 1f)
    val closeActions = {
        onRevealChanged(false)
        dragOffset = 0f
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.width(actionWidth).align(Alignment.CenterEnd)
                .graphicsLayer { alpha = revealProgress },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f)
                    .clickable(enabled = revealProgress > 0f) {
                        closeActions()
                        onRename()
                    }
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(Icons.Filled.Edit, contentDescription = "重命名")
                Text("重命名", style = MaterialTheme.typography.labelSmall)
            }
            Column(
                modifier = Modifier.weight(1f)
                    .clickable(enabled = revealProgress > 0f) {
                        closeActions()
                        onDelete()
                    }
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "\u5220\u9664",
                    tint = MaterialTheme.colorScheme.error,
                )
                Text(
                    "\u5220\u9664",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
        TrackRow(
            track = track,
            actionLabel = "",
            onAction = null,
            currentlyPlaying = currentlyPlaying,
            onPlay = {
                if (revealed) closeActions() else onPlay()
            },
            expanded = expanded,
            pages = pages,
            onTogglePages = onTogglePages,
            showCover = showCover,
            renderExpandedPages = showExpandedPages,
            showBottomDivider = false,
            modifier = Modifier.offset { IntOffset(offset.roundToInt(), 0) }
                .background(MaterialTheme.colorScheme.surface)
                .pointerInput(track.libraryId, actionWidthPx) {
                    detectHorizontalDragGestures(
                        onDragStart = {
                            dragOffset = animatedOffset
                            isDragging = true
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            dragOffset = (dragOffset + dragAmount)
                                .coerceIn(-actionWidthPx, 0f)
                        },
                        onDragEnd = {
                            val shouldReveal = dragOffset <= -actionWidthPx / 2
                            onRevealChanged(shouldReveal)
                            dragOffset = if (revealed) -actionWidthPx else 0f
                            isDragging = false
                        },
                        onDragCancel = {
                            dragOffset = if (revealed) -actionWidthPx else 0f
                            isDragging = false
                        },
                    )
                },
        )
    }
    HorizontalDivider()
}
