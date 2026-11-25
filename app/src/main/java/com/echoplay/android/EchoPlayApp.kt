package com.echoplay.android

import android.content.Intent
import android.icu.text.AlphabeticIndex
import android.icu.text.Collator
import android.icu.util.ULocale
import android.webkit.CookieManager as WebCookieManager
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.gestures.awaitHorizontalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.gestures.snapTo
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.fadeOut
import androidx.core.content.edit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import coil.compose.AsyncImage
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt
import kotlin.math.abs
import kotlin.math.sign

private enum class AppTab(val label: String) {
    Search("搜索"),
    Library("曲库"),
    UserInfo("我的")
}

private enum class LibrarySection(val label: String) {
    Playlists("播放列表"),
    Artists("歌手"),
    Songs("歌曲"),
}

private enum class SearchRequest { Results, VideoUrl }

private object LibraryTitleOrder : Comparator<String> {
    private val locale = ULocale("zh@collation=pinyin")
    private val index = AlphabeticIndex<Unit>(locale)
        .addLabels(ULocale.ENGLISH)
        .buildImmutableIndex()
    private val collator = Collator.getInstance(locale).apply {
        strength = Collator.SECONDARY
    }.freeze()

    fun initial(title: String): String = index.getBucket(index.getBucketIndex(title.trim()))?.label
        ?.takeIf { it.length == 1 && it[0] in 'A'..'Z' }
        ?: "#"

    override fun compare(first: String, second: String): Int {
        val initials = compareValuesBy(initial(first), initial(second), { it == "#" }, { it })
        return if (initials != 0) initials else collator.compare(first.trim(), second.trim())
    }
}

private val LibraryTrackOrder = compareBy<Track, String>(LibraryTitleOrder) { it.title }

private fun LibraryPlaylist.tracksInTitleOrder(tracksById: Map<String, Track>): List<Track> =
    trackIds.mapNotNull(tracksById::get).sortedWith(LibraryTrackOrder)

enum class AppThemeMode(val label: String) {
    SYSTEM("\u8ddf\u968f\u7cfb\u7edf"),
    LIGHT("\u4eae\u8272\u6a21\u5f0f"),
    DARK("\u6697\u8272\u6a21\u5f0f"),
}

private data class LibraryAddDraft(
    val tracks: List<Track>,
    val title: String,
    val artist: String,
    val phrases: List<String>,
    val collectionTitle: String,
    val titleFromSuggestion: Boolean = false,
    val artistInput: LibraryArtistInput = LibraryArtistInput.Default,
) {
    val isCollection: Boolean get() = tracks.size > 1
}

private data class PlaylistDeletion(val playlist: LibraryPlaylist, val isCollection: Boolean)

private typealias LibraryRowHeightFraction = Animatable<Float, AnimationVector1D>

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
    headerLogoModifier: Modifier = Modifier,
    awaitLaunchAnimationEnd: suspend () -> Unit = {},
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val playbackStateStore = remember(context) { PlaybackStateStore(context) }
    var selectedTab by remember { mutableStateOf(AppTab.Library) }
    val libraryPagerState = rememberPagerState(initialPage = LibrarySection.Songs.ordinal) {
        LibrarySection.entries.size
    }
    var revealedLibraryRowKey by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(libraryPagerState.currentPage) {
        revealedLibraryRowKey = null
    }
    var searchText by remember { mutableStateOf("") }
    var libraryFilter by remember { mutableStateOf("") }
    var searchRequest by remember { mutableStateOf<SearchRequest?>(null) }
    val loading = searchRequest != null
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var toastMessage by remember { mutableStateOf<Pair<String, Boolean>?>(null) }
    LaunchedEffect(errorMessage, successMessage) {
        val message = errorMessage ?: successMessage ?: return@LaunchedEffect
        toastMessage = message to (errorMessage == message)
        delay(2_000)
        if (errorMessage == message) errorMessage = null
        if (successMessage == message) successMessage = null
        if (toastMessage?.first == message) toastMessage = null
    }
    var page by remember { mutableIntStateOf(1) }
    var resultsQuery by remember { mutableStateOf("") }
    var showBilibiliLogin by remember { mutableStateOf(false) }
    var bilibiliLoginState by remember { mutableStateOf<Boolean?>(null) }
    var bilibiliProfile by remember { mutableStateOf<BilibiliUserProfile?>(null) }
    val isBilibiliLoggedIn = bilibiliLoginState == true
    val bilibiliIsVip = bilibiliProfile?.isVip == true
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
    var playbackPlaylistId by remember { mutableStateOf<String?>(null) }
    var playbackStatus by remember { mutableStateOf("就绪") }
    var playbackLoading by remember { mutableStateOf(false) }
    var playbackIsPlaying by remember { mutableStateOf(false) }
    var showPlayerDetails by remember { mutableStateOf(false) }
    var miniPlayerBounds by remember { mutableStateOf<Rect?>(null) }
    var playerHostOrigin by remember { mutableStateOf(Offset.Zero) }
    val playerTransition = updateTransition(
        targetState = showPlayerDetails && playbackTrack != null,
        label = "Now playing panel",
    )
    val playerPanelProgress = playerTransition.animateFloat(
        transitionSpec = {
            tween(durationMillis = if (targetState) 420 else 360, easing = FastOutSlowInEasing)
        },
        label = "Now playing panel size",
    ) { visible -> if (visible) 1f else 0f }
    val playerLayerVisible = playerTransition.currentState || playerTransition.targetState || playerTransition.isRunning
    var playbackVolume by remember(player) { mutableFloatStateOf(player.volume) }
    var shufflePlayback by remember { mutableStateOf(false) }
    var repeatMode by remember { mutableStateOf(PlaybackRepeatMode.Off) }
    var renameTarget by remember { mutableStateOf<Track?>(null) }
    var renameCollectionId by remember { mutableStateOf<String?>(null) }
    var renameTitle by remember { mutableStateOf("") }
    var renameArtist by remember { mutableStateOf("") }
    var renamingTrack by remember { mutableStateOf(false) }
    var libraryAddDraft by remember { mutableStateOf<LibraryAddDraft?>(null) }
    var addingTrack by remember { mutableStateOf(false) }
    val results = remember { mutableStateListOf<Track>() }
    var libraryTracks by remember { mutableStateOf<List<Track>>(emptyList()) }
    var libraryPlaylists by remember { mutableStateOf<List<LibraryPlaylist>>(emptyList()) }
    var showPlaylistEditor by remember { mutableStateOf(false) }
    var playlistToEdit by remember { mutableStateOf<LibraryPlaylist?>(null) }
    var playlistToDelete by remember { mutableStateOf<PlaylistDeletion?>(null) }
    var playlistDetailId by rememberSaveable { mutableStateOf<String?>(null) }
    var showPlaylistSongs by rememberSaveable { mutableStateOf(false) }
    var savingPlaylist by remember { mutableStateOf(false) }
    var playlistError by remember { mutableStateOf<String?>(null) }
    val playlistOverviewState = rememberLazyListState()
    val libraryTracksById = remember(libraryTracks) { libraryTracks.associateBy { it.libraryId } }
    val openedPlaylist = libraryPlaylists.firstOrNull { it.id == playlistDetailId }
    val openedCollection = remember(libraryTracks, playlistDetailId) {
        playlistDetailId?.let { id -> libraryTracks.filter { it.collectionId == id } }.orEmpty()
    }
    val sortedLibrary = remember(libraryTracks) {
        libraryTracks.sortedWith(LibraryTrackOrder)
    }
    var libraryRefreshId by remember { mutableIntStateOf(0) }
    val videoPages = remember { mutableStateMapOf<String, List<Track>>() }
    val pageRequests = remember { mutableMapOf<String, Deferred<Result<List<Track>>>>() }
    val pageDispatcher = remember { Dispatchers.IO.limitedParallelism(4) }
    val expandedSearchVideos = remember { mutableStateMapOf<String, Unit>() }
    var expandedLibraryArtist by rememberSaveable { mutableStateOf<String?>(null) }
    val searchResultsListState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val headerLogo = EchoPlayLogo.forTheme(isDarkTheme)
    val navigationAccent = if (isDarkTheme) {
        NavigationAccentDark
    } else {
        MaterialTheme.colorScheme.primary
    }
    val colors = MaterialTheme.colorScheme
    val miniPlayerColor = Color.Black.copy(alpha = if (isDarkTheme) 0.32f else 0.10f)
        .compositeOver(colors.surfaceVariant)
    fun onLibraryRowRevealChanged(rowKey: String, revealed: Boolean) {
        if (revealed) {
            revealedLibraryRowKey = rowKey
        } else if (revealedLibraryRowKey == rowKey) {
            revealedLibraryRowKey = null
        }
    }

    val dismissSwipeOnTap = Modifier.pointerInput(Unit) {
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
            if (released && !movedBeyondTouchSlop && revealedLibraryRowKey != null) {
                revealedLibraryRowKey = null
            }
        }
    }
    var playbackRequest by remember { mutableStateOf<Job?>(null) }

    fun updatePlaybackQuality(quality: PlaybackQuality) {
        if (playbackQuality == quality) return
        playbackQuality = quality
        settingsPreferences.edit { putString("playback-quality", quality.name) }
    }

    fun updateBilibiliSession(cookies: String?) {
        repository.updateWebCookies(cookies)
        val loggedIn = hasBilibiliSession(cookies)
        bilibiliLoginState = loggedIn
        if (!loggedIn) {
            bilibiliProfile = null
            if (playbackQuality == PlaybackQuality.HI_RES) updatePlaybackQuality(PlaybackQuality.P1080)
        }
    }

    suspend fun getVideoPages(track: Track): List<Track> {
        val videoId = track.bvid
        videoPages[videoId]?.let { return it }
        val request = pageRequests.getOrPut(videoId) {
            scope.async(start = CoroutineStart.LAZY) {
                try {
                    val pages = withContext(pageDispatcher) { repository.pages(track) }
                    videoPages[videoId] = pages
                    Result.success(pages)
                } catch (exception: CancellationException) {
                    throw exception
                } catch (exception: Exception) {
                    Result.failure(exception)
                } finally {
                    pageRequests.remove(videoId)
                }
            }
        }
        return request.await().getOrThrow()
    }

    fun isTrackPlaying(track: Track, playlistId: String? = null): Boolean =
        playbackIsPlaying && playbackPlaylistId == playlistId && playbackTrack?.libraryId == track.libraryId

    fun savePlaybackState() {
        val current = playbackTrack ?: return
        playbackStateStore.save(
            SavedPlaybackState(
                currentTrack = current,
                queue = playbackQueue,
                positionMs = player.currentPosition,
                shuffleEnabled = shufflePlayback,
                wasPlaying = playbackIsPlaying || player.playWhenReady,
                repeatMode = repeatMode,
                playlistId = playbackPlaylistId,
            ),
        )
    }

    fun playTrack(
        track: Track,
        queue: List<Track> = libraryTracks,
        startPositionMs: Long = 0L,
        resumePlaying: Boolean = true,
        quality: PlaybackQuality = if (playbackQuality == PlaybackQuality.HI_RES && bilibiliProfile?.isVip != true) {
            PlaybackQuality.P1080
        } else playbackQuality,
        playlistId: String? = null,
    ) {
        focusManager.clearFocus()
        PlaybackDiagnostics.record(context, "ui.playRequested", "trackId=${track.libraryId}")
        // Promote the media session service while the Activity is still in
        // the foreground; stream resolution may take several seconds.
        startPlaybackService()
        playbackRequest?.cancel()
        player.stop()
        playbackTrack = track
        playbackQueue = queue
        playbackPlaylistId = playlistId
        playbackStateStore.save(
            SavedPlaybackState(track, queue, startPositionMs, shufflePlayback, resumePlaying, repeatMode, playlistId),
        )
        playbackLoading = true
        playbackIsPlaying = false
        playbackStatus = "正在获取 B 站音轨…"
        playbackRequest = scope.launch {
            try {
                val stream = withContext(Dispatchers.IO) {
                    repository.audioStream(track, quality)
                }
                PlaybackDiagnostics.record(context, "stream.resolved", "trackId=${track.libraryId} urlCount=${stream.urls.size} description=${stream.description}")
                setPlaybackHeaders(stream.headers)
                val mediaMetadata = androidx.media3.common.MediaMetadata.Builder()
                    .setTitle(track.title)
                    .setArtist(track.playbackArtist)
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
                playbackLoading = false
                playbackIsPlaying = false
                playbackStatus = exception.message ?: "获取 B 站音轨失败。"
            }
        }
    }

    fun togglePlayback() {
        focusManager.clearFocus()
        if (player.isPlaying) {
            player.pause()
        } else if (!playbackLoading) {
            if (player.playbackState == Player.STATE_ENDED) player.seekTo(0)
            player.play()
        }
    }

    fun selectPlaybackQuality(quality: PlaybackQuality) {
        if (quality == PlaybackQuality.HI_RES && !bilibiliIsVip) return
        if (playbackQuality == quality) return
        updatePlaybackQuality(quality)
        val currentTrack = playbackTrack ?: return
        val resume = playbackIsPlaying || player.playWhenReady
        val position = player.currentPosition
        playTrack(
            track = currentTrack,
            queue = playbackQueue,
            startPositionMs = position,
            resumePlaying = resume,
            quality = quality,
            playlistId = playbackPlaylistId,
        )
    }

    fun playSearchResult(track: Track) {
        playbackRequest?.cancel()
        playbackLoading = true
        playbackStatus = "正在读取合集分 P…"
        playbackRequest = scope.launch {
            try {
                val pages = getVideoPages(track)
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
        val nextTrack = playbackQueue[(currentIndex + 1) % playbackQueue.size]
        playTrack(nextTrack, playbackQueue, playlistId = playbackPlaylistId)
    }

    fun moveUpcomingTrack(fromId: String, toId: String) {
        val current = playbackTrack ?: return
        val queue = playbackQueue
        val indices = queue.upcomingTrackIndices(current.libraryId, shufflePlayback, repeatMode)
        val upcoming = indices.map(queue::get).toMutableList()
        val from = upcoming.indexOfFirst { it.libraryId == fromId }
        val to = upcoming.indexOfFirst { it.libraryId == toId }
        if (from < 0 || to < 0 || from == to) return

        upcoming.add(to, upcoming.removeAt(from))
        playbackQueue = queue.toMutableList().apply {
            indices.forEachIndexed { index, queueIndex -> this[queueIndex] = upcoming[index] }
        }
        savePlaybackState()
    }

    fun setRepeatMode(mode: PlaybackRepeatMode) {
        repeatMode = mode
        // The player holds only the current song, which can consist of several media items.
        player.repeatMode = if (mode == PlaybackRepeatMode.Single) Player.REPEAT_MODE_ALL else Player.REPEAT_MODE_OFF
    }

    fun setShuffleMode(enabled: Boolean) {
        if (shufflePlayback == enabled) return
        val currentTrack = playbackTrack
        val currentId = currentTrack?.libraryId
        val playlistId = playbackPlaylistId
        val baseQueue = if (playlistId == null) {
            sortedLibrary.ifEmpty { playbackQueue }
        } else {
            val tracksById = libraryTracks.associateBy { it.libraryId }
            libraryPlaylists.find { it.id == playlistId }?.tracksInTitleOrder(tracksById)
                .orEmpty().ifEmpty { libraryTracks.filter { it.collectionId == playlistId } }.ifEmpty { playbackQueue }
        }
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
        if (enabled) setRepeatMode(PlaybackRepeatMode.Off)
        savePlaybackState()
    }

    fun toggleRepeatMode() {
        setRepeatMode(
            when (repeatMode) {
                PlaybackRepeatMode.Off -> PlaybackRepeatMode.List
                PlaybackRepeatMode.List -> PlaybackRepeatMode.Single
                PlaybackRepeatMode.Single -> PlaybackRepeatMode.Off
            },
        )
        if (shufflePlayback) setShuffleMode(false) else savePlaybackState()
    }

    fun playTrackList(
        tracks: List<Track>,
        selectedTrack: Track? = null,
        playlistId: String? = null,
        shuffle: Boolean = shufflePlayback,
    ) {
        val queue = if (shuffle) tracks.shuffled() else tracks
        val firstTrack = selectedTrack ?: queue.firstOrNull() ?: return
        shufflePlayback = shuffle
        if (shuffle) {
            setRepeatMode(PlaybackRepeatMode.Off)
        } else if (repeatMode == PlaybackRepeatMode.Off && queue.size > 1) {
            setRepeatMode(PlaybackRepeatMode.List)
        }
        playTrack(firstTrack, queue, playlistId = playlistId)
    }

    fun playPreviousTrack() {
        val currentTrack = playbackTrack ?: return
        if (player.currentPosition > 3_000L) {
            player.seekTo(0)
            return
        }
        val currentIndex = playbackQueue.indexOfFirst { it.libraryId == currentTrack.libraryId }
        if (currentIndex < 0) return
        val previousIndex = (currentIndex - 1 + playbackQueue.size) % playbackQueue.size
        playTrack(playbackQueue[previousIndex], playbackQueue, playlistId = playbackPlaylistId)
    }

    DisposableEffect(setTrackNavigationActions) {
        setTrackNavigationActions(::playPreviousTrack, ::playNextTrack)
        onDispose { setTrackNavigationActions({}, {}) }
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onVolumeChanged(volume: Float) {
                playbackVolume = volume
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                playbackIsPlaying = isPlaying
                savePlaybackState()
                PlaybackDiagnostics.record(context, "ui.player.isPlaying", "isPlaying=$isPlaying ${PlaybackDiagnostics.playerState(player)}")
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                PlaybackDiagnostics.record(context, "ui.player.state", PlaybackDiagnostics.playerState(player))
                if (playbackState != Player.STATE_ENDED) {
                    savePlaybackState()
                    return
                }

                playbackIsPlaying = false
                val currentId = playbackTrack?.libraryId
                if (repeatMode.advancesQueue(shufflePlayback) && playbackQueue.any { it.libraryId == currentId }) {
                    playNextTrack()
                } else {
                    player.pause()
                    playbackStatus = "播放完毕"
                    savePlaybackState()
                }
            }

            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int,
            ) {
                if (reason == Player.DISCONTINUITY_REASON_SEEK && !playbackLoading) savePlaybackState()
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

    fun refreshLibrary(): Job {
        val refreshId = ++libraryRefreshId
        return scope.launch {
            try {
                val (tracks, playlists) = withContext(Dispatchers.IO) {
                    libraryStore.tracks() to libraryStore.playlists()
                }
                if (refreshId != libraryRefreshId) return@launch
                libraryTracks = tracks
                libraryPlaylists = playlists
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                errorMessage = exception.message ?: "读取曲库失败。"
            }
        }
    }

    fun editPlaylist(playlist: LibraryPlaylist?) {
        playlistToEdit = playlist
        playlistError = null
        showPlaylistEditor = true
    }

    fun openPlaylist(id: String) {
        playlistDetailId = id
        showPlaylistSongs = false
        playlistError = null
        revealedLibraryRowKey = null
        focusManager.clearFocus()
    }

    fun closePlaylist() {
        playlistDetailId = null
        showPlaylistSongs = false
        playlistError = null
        revealedLibraryRowKey = null
    }

    fun playPlaylist(id: String, tracks: List<Track>, track: Track? = null) {
        playTrackList(tracks, selectedTrack = track, playlistId = id, shuffle = false)
    }

    fun <T> updatePlaylists(onSaved: (T) -> Unit, change: () -> T) {
        if (savingPlaylist) return
        savingPlaylist = true
        playlistError = null
        scope.launch {
            try {
                val result = withContext(Dispatchers.IO) { change() }
                refreshLibrary().join()
                onSaved(result)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                playlistError = exception.message ?: "保存播放列表失败。"
            } finally {
                savingPlaylist = false
            }
        }
    }

    fun search(nextPage: Int = 1) {
        val query = if (nextPage == 1) searchText.trim() else resultsQuery
        if (query.isEmpty() || searchRequest != null) return
        scope.launch {
            searchRequest = SearchRequest.Results
            errorMessage = null
            successMessage = null
            if (nextPage == 1) {
                resultsQuery = query
                searchResultsListState.requestScrollToItem(0)
                results.clear()
                expandedSearchVideos.clear()
            }
            try {
                val nextResults = withContext(Dispatchers.IO) {
                    repository.search(query, nextPage)
                }
                val knownVideoIds = results.mapTo(mutableSetOf()) { it.bvid }
                results.addAll(nextResults.filter { knownVideoIds.add(it.bvid) })
                page = nextPage
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                errorMessage = exception.message ?: "搜索失败，请稍后重试。"
            } finally {
                searchRequest = null
            }
        }
    }

    fun requestAddTracks(tracks: List<Track>) {
        if (tracks.isEmpty() || libraryAddDraft != null || addingTrack) return
        focusManager.clearFocus()
        val orderedTracks = tracks.sortedBy(Track::pageNumber)
        val first = orderedTracks.first()
        val videoTitle = first.videoTitle.ifBlank { first.title }
        val videoSuggestions = suggestLibraryTitle(videoTitle)
        val primarySuggestions = if (first.title != videoTitle) {
            suggestLibraryTitle(first.title)
        } else {
            videoSuggestions
        }
        libraryAddDraft = LibraryAddDraft(
            tracks = orderedTracks,
            title = primarySuggestions.title.ifBlank { videoSuggestions.title },
            artist = primarySuggestions.artist.ifBlank { videoSuggestions.artist },
            phrases = libraryAddPhrases(primarySuggestions.phrases + videoSuggestions.phrases),
            collectionTitle = videoSuggestions.title.ifBlank { videoTitle },
        )
        errorMessage = null
        successMessage = null
    }

    fun submitSearch() {
        val query = searchText.trim()
        if (query.isBlank() || searchRequest != null) return
        if (!query.startsWith("http://", ignoreCase = true) &&
            !query.startsWith("https://", ignoreCase = true) &&
            !query.startsWith("www.", ignoreCase = true)
        ) {
            focusManager.clearFocus()
            search()
            return
        }

        scope.launch {
            searchRequest = SearchRequest.VideoUrl
            errorMessage = null
            successMessage = null
            results.clear()
            expandedSearchVideos.clear()
            try {
                val (track, alreadyExists) = withContext(Dispatchers.IO) {
                    val track = repository.videoTrackFromUrl(query)
                    track to libraryStore.containsVideo(track.bvid)
                }
                if (alreadyExists) {
                    errorMessage = "曲库中已存在。"
                } else {
                    requestAddTracks(getVideoPages(track))
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                errorMessage = exception.message ?: "添加 B 站视频到曲库失败。"
            } finally {
                searchRequest = null
            }
        }
    }

    fun addToLibrary(track: Track) {
        requestAddTracks(listOf(track))
    }

    fun addVideoToLibrary(track: Track) {
        scope.launch {
            try {
                requestAddTracks(getVideoPages(track))
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                errorMessage = exception.message ?: "添加视频到曲库失败。"
            }
        }
    }

    fun confirmAddToLibrary() {
        val draft = libraryAddDraft ?: return
        if (addingTrack) return
        val title = draft.title.trim()
        val artist = draft.artist.trim()
        if (title.isBlank()) {
            errorMessage = "歌曲名称不能为空。"
            return
        }
        addingTrack = true
        scope.launch {
            try {
                val added = withContext(Dispatchers.IO) {
                    val first = draft.tracks.first()
                    if (draft.isCollection) {
                        libraryStore.addAll(
                            collectionTracksForLibrary(
                                tracks = draft.tracks,
                                collectionTitle = draft.collectionTitle,
                                firstTitle = title,
                                firstArtist = artist,
                                applyTitleSuggestion = draft.titleFromSuggestion,
                                artistInput = draft.artistInput,
                            ),
                            collectionId = first.bvid,
                        )
                    } else {
                        libraryStore.add(first.copy(title = title, artist = artist))
                    }
                }
                libraryAddDraft = null
                if (!added) {
                    errorMessage = "曲库中已存在。"
                    return@launch
                }
                refreshLibrary().join()
                successMessage = if (draft.isCollection) {
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

    fun togglePages(track: Track) {
        if (track.bvid in expandedSearchVideos) {
            val resultIndex = results.indexOfFirst { it.bvid == track.bvid }
            val collapsedHeaderIndex = if (resultIndex >= 0) {
                results.take(resultIndex).sumOf { previousTrack ->
                    if (previousTrack.bvid in expandedSearchVideos) {
                        2 + videoPages[previousTrack.bvid].orEmpty().size
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
            expandedSearchVideos[track.bvid] = Unit
        }
    }

    fun removeLibraryRows(
        track: Track,
        heightFraction: LibraryRowHeightFraction,
        resetSwipe: suspend () -> Unit,
    ) {
        scope.launch {
            try {
                heightFraction.animateTo(0f, tween(200))
                withContext(Dispatchers.IO) { libraryStore.remove(track) }
                libraryTracks = libraryTracks.filterNot { it.libraryId == track.libraryId }
                if (track.collectionId != null && track.collectionId == playlistDetailId &&
                    libraryTracks.none { it.collectionId == track.collectionId }
                ) {
                    closePlaylist()
                }
                refreshLibrary()
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                errorMessage = exception.message ?: "从曲库移除失败。"
                resetSwipe()
            }
        }
    }

    fun renameInLibrary(track: Track, newTitle: String, newArtist: String) {
        if (renamingTrack) return
        val title = newTitle.trim()
        if (title.isBlank()) {
            errorMessage = "歌曲名称不能为空。"
            return
        }
        val artist = if (newArtist.isBlank() && track.artist == null) null else newArtist.trim()
        renamingTrack = true
        scope.launch {
            try {
                withContext(Dispatchers.IO) { libraryStore.rename(track, title, artist) }
                val index = libraryTracks.indexOfFirst { it.libraryId == track.libraryId }
                val renamedTrack = (libraryTracks.getOrNull(index) ?: track).copy(title = title, artist = artist)
                if (index >= 0) libraryTracks = libraryTracks.toMutableList().apply { set(index, renamedTrack) }
                val currentChanged = playbackTrack?.libraryId == track.libraryId
                val queueChanged = playbackQueue.any { it.libraryId == track.libraryId }
                if (currentChanged) playbackTrack = renamedTrack
                if (queueChanged) {
                    playbackQueue = playbackQueue.map { if (it.libraryId == track.libraryId) renamedTrack else it }
                }
                if (currentChanged || queueChanged) savePlaybackState()
                renameTarget = null
                errorMessage = null
                refreshLibrary()
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                errorMessage = exception.message ?: "重命名失败。"
            } finally {
                renamingTrack = false
            }
        }
    }

    fun renameVideoInLibrary(collectionId: String, newTitle: String) {
        val title = newTitle.trim()
        if (title.isBlank()) {
            errorMessage = "合集名称不能为空。"
            return
        }
        scope.launch {
            try {
                withContext(Dispatchers.IO) { libraryStore.renameVideo(collectionId, title) }
                libraryTracks = libraryTracks.map { track ->
                    if (track.collectionId == collectionId) track.copy(videoTitle = title) else track
                }
                renameCollectionId = null
                errorMessage = null
                refreshLibrary()
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                errorMessage = exception.message ?: "重命名合集失败。"
            }
        }
    }

    LaunchedEffect(selectedTab) {
        if (selectedTab == AppTab.Library || selectedTab == AppTab.UserInfo) refreshLibrary()
    }

    LaunchedEffect(repository) {
        awaitLaunchAnimationEnd()
        updateBilibiliSession(currentBilibiliCookieHeader())
    }

    LaunchedEffect(bilibiliLoginState, selectedTab == AppTab.UserInfo) {
        if (!isBilibiliLoggedIn || (selectedTab != AppTab.UserInfo && bilibiliProfile != null)) {
            return@LaunchedEffect
        }
        val profile = try {
            withContext(Dispatchers.IO) { repository.userProfile() }
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            null
        }
        if (bilibiliLoginState != true) return@LaunchedEffect
        bilibiliProfile = profile
        if (profile?.isVip != true && playbackQuality == PlaybackQuality.HI_RES) {
            updatePlaybackQuality(PlaybackQuality.P1080)
        }
    }

    LaunchedEffect(player) {
        val saved = withContext(Dispatchers.IO) { playbackStateStore.load() } ?: return@LaunchedEffect
        if (playbackRequest != null) return@LaunchedEffect
        shufflePlayback = saved.shuffleEnabled
        setRepeatMode(saved.repeatMode)
        playbackTrack = saved.currentTrack
        playbackQueue = saved.queue
        playbackPlaylistId = saved.playlistId
        if (player.mediaItemCount > 0) {
            // The service owns the live player. Reopening the Activity from
            // its notification must reconnect to that playback instead of
            // replacing it with a paused copy of the saved state.
            playbackIsPlaying = player.isPlaying
            return@LaunchedEffect
        }
        awaitLaunchAnimationEnd()
        if (playbackRequest != null) return@LaunchedEffect
        playTrack(
            track = saved.currentTrack,
            queue = saved.queue,
            startPositionMs = saved.positionMs,
            resumePlaying = false,
            playlistId = saved.playlistId,
        )
    }

    LaunchedEffect(playbackTrack, playbackIsPlaying) {
        if (playbackTrack == null || !playbackIsPlaying) return@LaunchedEffect
        while (true) {
            delay(5_000L)
            savePlaybackState()
        }
    }

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, player) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP && !playbackLoading) savePlaybackState()
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    if (showBilibiliLogin) {
        BackHandler { showBilibiliLogin = false }
        LaunchedEffect(Unit) {
            while (showBilibiliLogin) {
                val cookies = currentBilibiliCookieHeader()
                if (cookies != null) {
                    updateBilibiliSession(cookies)
                    showBilibiliLogin = false
                } else {
                    delay(LOGIN_COOKIE_POLL_INTERVAL_MILLIS)
                }
            }
        }
        BilibiliLoginWebView(
            modifier = Modifier.fillMaxSize(),
            onCookiesChanged = { cookies ->
                updateBilibiliSession(currentBilibiliCookieHeader() ?: cookies)
                if (bilibiliLoginState == true) showBilibiliLogin = false
            },
            onPageError = { errorMessage = it },
        )
        return
    }

    libraryAddDraft?.let { draft ->
        LibraryMetadataDialog(
            accentColor = navigationAccent,
            heading = if (draft.isCollection) "添加合集到曲库" else "添加到曲库",
            titleLabel = "歌曲名称",
            title = draft.title,
            artist = draft.artist,
            phrases = draft.phrases,
            confirmLabel = if (addingTrack) "添加中…" else "加入",
            saving = addingTrack,
            guidance = if (draft.isCollection) {
                "正在编辑第一个分 P。点选提示词可按同一规则调整其余分 P。手动填写歌手会应用到整个合集。"
            } else null,
            onTitleChange = { title ->
                if (!addingTrack) libraryAddDraft = libraryAddDraft?.copy(title = title, titleFromSuggestion = false)
            },
            onArtistChange = { artist ->
                if (!addingTrack) libraryAddDraft = libraryAddDraft?.copy(artist = artist, artistInput = LibraryArtistInput.Manual)
            },
            onTitleSuggestionSelected = { title ->
                if (!addingTrack) libraryAddDraft = libraryAddDraft?.copy(title = title, titleFromSuggestion = true)
            },
            onArtistSuggestionSelected = { artist ->
                if (!addingTrack) libraryAddDraft = libraryAddDraft?.copy(artist = artist, artistInput = LibraryArtistInput.Suggestion)
            },
            onConfirm = { confirmAddToLibrary() },
            onDismiss = { if (!addingTrack) libraryAddDraft = null },
        )
    }

    renameTarget?.let { track ->
        val phrases = remember(track) {
            listOf(track.partTitle, track.videoTitle.ifBlank { track.title })
                .filter(String::isNotBlank)
                .distinct()
                .flatMap { suggestLibraryTitle(it).phrases }
                .distinctBy(String::lowercase)
                .take(8)
        }
        LibraryMetadataDialog(
            accentColor = navigationAccent,
            heading = "重命名歌曲",
            titleLabel = "歌曲名称",
            title = renameTitle,
            artist = renameArtist,
            phrases = phrases,
            confirmLabel = if (renamingTrack) "保存中…" else "保存",
            saving = renamingTrack,
            onTitleChange = { if (!renamingTrack) renameTitle = it },
            onArtistChange = { if (!renamingTrack) renameArtist = it },
            onConfirm = { renameInLibrary(track, renameTitle, renameArtist) },
            onDismiss = { if (!renamingTrack) renameTarget = null },
        )
    }

    renameCollectionId?.let { collectionId ->
        LibraryDialog(
            accentColor = navigationAccent,
            onDismissRequest = { renameCollectionId = null },
            title = { Text("重命名合集") },
            text = {
                OutlinedTextField(
                    value = renameTitle,
                    onValueChange = { renameTitle = it },
                    label = { Text("合集名称") },
                    singleLine = true,
                )
            },
            confirmLabel = "保存",
            onConfirm = { renameVideoInLibrary(collectionId, renameTitle) },
        )
    }

    if (showPlaylistEditor) {
        PlaylistNameDialog(
            playlist = playlistToEdit,
            accentColor = navigationAccent,
            saving = savingPlaylist,
            error = playlistError,
            onSave = { name ->
                val editing = playlistToEdit
                updatePlaylists(onSaved = { id ->
                    showPlaylistEditor = false
                    if (editing == null) openPlaylist(id)
                }) {
                    libraryStore.savePlaylist(editing?.id, name, editing?.trackIds.orEmpty())
                }
            },
            onDismiss = {
                if (!savingPlaylist) {
                    showPlaylistEditor = false
                    playlistError = null
                }
            },
        )
    }

    playlistToDelete?.let { (playlist, isCollection) ->
        LibraryDialog(
            accentColor = navigationAccent,
            onDismissRequest = {
                if (!savingPlaylist) {
                    playlistToDelete = null
                    playlistError = null
                }
            },
            title = { Text(if (isCollection) "删除收藏合集？" else "删除播放列表？") },
            text = {
                val message = if (isCollection) "「${playlist.name}」及其曲目会从曲库移除。"
                else "删除「${playlist.name}」后，歌曲仍会保留在曲库中。"
                Text(playlistError ?: message)
            },
            confirmLabel = if (savingPlaylist) "删除中…" else "删除",
            confirmEnabled = !savingPlaylist,
            dismissEnabled = !savingPlaylist,
            onConfirm = {
                updatePlaylists(
                    onSaved = {
                        if (playlistDetailId == playlist.id) closePlaylist()
                        playlistToDelete = null
                    },
                ) {
                    if (isCollection) libraryStore.removeVideo(playlist.id)
                    else libraryStore.deletePlaylist(playlist.id)
                }
            },
        )
    }

    @Composable
    fun PlayerPage(track: Track, isClosing: Boolean) {
        PlayerDetailsScreen(
            track = track,
            isClosing = isClosing,
            accentColor = navigationAccent,
            isPlaying = playbackIsPlaying,
            isLoading = playbackLoading,
            status = playbackStatus,
            player = player,
            volume = playbackVolume,
            onClose = { showPlayerDetails = false },
            onPlayPause = ::togglePlayback,
            onPrevious = { playPreviousTrack() },
            onNext = { playNextTrack() },
            onSeek = { requestedPosition ->
                val duration = player.duration
                if (duration > 0L && !playbackLoading) {
                    player.seekTo(requestedPosition.coerceIn(0L, duration))
                }
            },
            onVolumeChange = { player.volume = it },
            onVolumeChangeFinished = { playbackStateStore.saveVolume(player.volume) },
            queue = playbackQueue,
            shuffleEnabled = shufflePlayback,
            repeatMode = repeatMode,
            onToggleShuffle = { setShuffleMode(!shufflePlayback) },
            onToggleRepeat = { toggleRepeatMode() },
            onSelectTrack = { playTrack(it, playbackQueue, playlistId = playbackPlaylistId) },
            onMoveTrack = ::moveUpcomingTrack,
        )
    }

    if (selectedTab == AppTab.Library && playlistDetailId != null &&
        (openedPlaylist != null || openedCollection.isNotEmpty())
    ) {
        fun goBackFromPlaylist() {
            if (savingPlaylist) return
            if (showPlaylistSongs) {
                showPlaylistSongs = false
                playlistError = null
            } else {
                closePlaylist()
            }
        }
        BackHandler(onBack = ::goBackFromPlaylist)
        if (showPlaylistSongs && openedPlaylist != null) {
            PlaylistSongPicker(
                playlist = openedPlaylist,
                tracks = sortedLibrary,
                accentColor = navigationAccent,
                saving = savingPlaylist,
                error = playlistError,
                onBack = ::goBackFromPlaylist,
                onSave = { ids ->
                    updatePlaylists(onSaved = {
                        showPlaylistSongs = false
                        if (ids != openedPlaylist.trackIds) successMessage = "已更新播放列表。"
                    }) {
                        libraryStore.savePlaylist(openedPlaylist.id, openedPlaylist.name, ids)
                    }
                },
            )
            return
        }
    }

    @Composable
    fun MainScaffold(modifier: Modifier) = Scaffold(
        modifier = modifier,
        topBar = {},
        bottomBar = {
            Column {
                playbackTrack?.let { track ->
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .onGloballyPositioned { miniPlayerBounds = it.boundsInRoot() }
                            .background(miniPlayerColor)
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .clickable {
                                focusManager.clearFocus()
                                showPlayerDetails = true
                            }
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
                                if (track.playbackArtist.isNotBlank()) {
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        track.playbackArtist,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                            Text(
                                playbackStatus,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        IconButton(
                            onClick = ::togglePlayback,
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
                        val canSkip = playbackQueue.size > 1 &&
                            playbackQueue.any { it.libraryId == track.libraryId }
                        IconButton(
                            onClick = ::playNextTrack,
                            enabled = !playbackLoading && canSkip,
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
                                onClick = {
                                    if (selectedTab == AppTab.Library && tab == AppTab.Library) closePlaylist()
                                    selectedTab = tab
                                },
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
            modifier = Modifier.fillMaxSize().padding(padding)
                .padding(horizontal = if (selectedTab == AppTab.Library) 0.dp else 16.dp)
                .then(dismissSwipeOnTap),
        ) {
            when (selectedTab) {
                AppTab.Search -> {
                    AppPageHeader(title = AppTab.Search.label, logo = headerLogo, logoModifier = headerLogoModifier)
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
                    val hasResults = results.isNotEmpty()
                    SearchPageLayout(
                        docked = hasResults || searchRequest == SearchRequest.Results,
                        showLoading = loading && !hasResults,
                        accentColor = navigationAccent,
                        searchControls = searchControls,
                    ) {
                        AnimatedVisibility(
                            visible = hasResults,
                            enter = fadeIn(tween(durationMillis = 180, delayMillis = 60)),
                            exit = ExitTransition.None,
                        ) {
                            val savedPagesByVideo = remember(libraryTracks) {
                                libraryTracks.groupBy(Track::bvid, Track::pageNumber)
                                    .mapValues { (_, pages) -> pages.toSet() }
                            }
                            val savedCollectionIds = remember(libraryTracks) {
                                libraryTracks.mapNotNullTo(hashSetOf(), Track::collectionId)
                            }
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                state = searchResultsListState,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                results.forEachIndexed { resultIndex, track ->
                                    val isExpanded = track.bvid in expandedSearchVideos
                                    val savedPages = savedPagesByVideo[track.bvid].orEmpty()
                                    val row: @Composable () -> Unit = {
                                        LaunchedEffect(track.bvid) {
                                            try {
                                                getVideoPages(track)
                                            } catch (exception: CancellationException) {
                                                throw exception
                                            } catch (exception: Exception) {
                                                errorMessage = exception.message ?: "读取分 P 信息失败。"
                                            }
                                        }
                                        val pages = videoPages[track.bvid]
                                        val added = track.bvid in savedCollectionIds ||
                                            (pages?.isNotEmpty() == true && pages.all { it.pageNumber in savedPages })
                                        TrackRow(
                                            track = track,
                                            subtitle = listOf(track.author, track.duration).filter(String::isNotBlank)
                                                .joinToString(" · "),
                                            playingIndicatorColor = navigationAccent,
                                            currentlyPlaying = isTrackPlaying(track),
                                            onPlay = { playSearchResult(track) },
                                            expanded = isExpanded,
                                            onTogglePages = pages?.takeIf { it.size > 1 }?.let { { togglePages(track) } },
                                            showCover = true,
                                            trailingContent = {
                                                SearchResultAddButton(added, "加入曲库", navigationAccent) {
                                                    addVideoToLibrary(track)
                                                }
                                            },
                                        )
                                    }
                                    if (isExpanded) {
                                        stickyHeader(key = "search-expanded-${track.bvid}-$resultIndex") {
                                            Box(
                                                modifier = Modifier.fillMaxWidth()
                                                    .background(MaterialTheme.colorScheme.surface),
                                            ) { row() }
                                        }
                                        items(
                                            videoPages[track.bvid].orEmpty(),
                                            key = { "search-page-${track.bvid}-${it.pageNumber}" },
                                            contentType = { "search-page" },
                                        ) { pageTrack ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth()
                                                    .playingTrackHighlight(
                                                        isTrackPlaying(pageTrack),
                                                        navigationAccent,
                                                        showLine = false,
                                                    )
                                                    .padding(start = 20.dp, end = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                if (playbackTrack?.libraryId == pageTrack.libraryId) {
                                                    Icon(
                                                        imageVector = if (playbackIsPlaying) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                                                        contentDescription = "正在播放此分 P",
                                                        tint = navigationAccent,
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
                                                SearchResultAddButton(
                                                    added = pageTrack.pageNumber in savedPages,
                                                    addLabel = "添加 ${pageTrack.title} 到曲库",
                                                    accentColor = navigationAccent,
                                                ) {
                                                    addToLibrary(pageTrack)
                                                }
                                            }
                                            HorizontalDivider()
                                        }
                                        stickyHeader(key = "search-expanded-end-${track.bvid}") {
                                            Spacer(Modifier.fillMaxWidth().height(1.dp))
                                        }
                                    } else {
                                        item(key = "search-${track.bvid}-$resultIndex", contentType = "search-video") { row() }
                                    }
                                }
                                item {
                                    TextButton(
                                        onClick = { search(page + 1) },
                                        enabled = !loading,
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.textButtonColors(contentColor = navigationAccent),
                                    ) {
                                        Text(if (loading) "正在加载…" else "加载更多")
                                    }
                                }
                            }
                        }
                    }
                }

                AppTab.Library -> {
                    @Composable
                    fun ManagedSongRow(
                        track: Track,
                        rowKey: String,
                        swipeToManage: Boolean = false,
                        playlistId: String? = null,
                        onPlay: () -> Unit,
                    ) {
                        SwipeableLibraryTrackRow(
                            track = track,
                            playingIndicatorColor = navigationAccent,
                            isDarkTheme = isDarkTheme,
                            currentlyPlaying = isTrackPlaying(track, playlistId),
                            swipeToManage = swipeToManage,
                            revealed = revealedLibraryRowKey == rowKey,
                            onRevealChanged = { onLibraryRowRevealChanged(rowKey, it) },
                            onPlay = onPlay,
                            onRename = {
                                renameTitle = track.title
                                renameArtist = track.artist.orEmpty()
                                renameTarget = track
                            },
                            onDelete = { height, resetSwipe -> removeLibraryRows(track, height, resetSwipe) },
                        )
                    }

                    val activeDetailId = playlistDetailId?.takeIf { openedPlaylist != null || openedCollection.isNotEmpty() }
                    PlaylistPageTransition(activeDetailId) { detailId, coverModifier, tracksModifier ->
                        if (detailId != null) {
                            val openedPlaylist = libraryPlaylists.firstOrNull { it.id == detailId }
                            val openedCollection = remember(libraryTracks, detailId) {
                                libraryTracks.filter { it.collectionId == detailId }
                            }
                            if (openedPlaylist == null && openedCollection.isEmpty()) return@PlaylistPageTransition
                            val isCollection = openedPlaylist == null
                            val tracks = remember(openedPlaylist, openedCollection, libraryTracksById) {
                                openedPlaylist?.tracksInTitleOrder(libraryTracksById) ?: openedCollection
                            }
                            val name = openedPlaylist?.name ?: openedCollection.first().let { it.videoTitle.ifBlank { it.title } }
                            val candidates = remember(openedPlaylist, tracks, sortedLibrary) {
                                if (openedPlaylist == null) {
                                    emptyList()
                                } else {
                                    val included = openedPlaylist.trackIds.toHashSet()
                                    val artists = tracks.mapNotNull { it.artist?.trim()?.takeIf(String::isNotBlank) }.toHashSet()
                                    sortedLibrary.filterNot { it.libraryId in included }
                                        .sortedByDescending { it.artist?.trim() in artists }
                                }
                            }
                            key(detailId) {
                                PlaylistDetails(
                                    name = name,
                                    tracks = tracks,
                                    candidates = candidates,
                                    accentColor = navigationAccent,
                                    isCollection = isCollection,
                                    saving = savingPlaylist,
                                    onBack = ::closePlaylist,
                                    onPlay = { playPlaylist(detailId, tracks) },
                                    onShare = {
                                        val songList = tracks.joinToString("\n") { track ->
                                            listOf(track.title, track.artist.orEmpty()).filter(String::isNotBlank).joinToString(" · ")
                                        }
                                        val shareText = listOf(name, songList).filter(String::isNotBlank).joinToString("\n\n")
                                        val intent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_SUBJECT, name)
                                            putExtra(Intent.EXTRA_TEXT, shareText)
                                        }
                                        context.startActivity(Intent.createChooser(intent, null))
                                    },
                                    onRename = {
                                        if (openedPlaylist != null) {
                                            editPlaylist(openedPlaylist)
                                        } else {
                                            renameTitle = name
                                            renameCollectionId = detailId
                                        }
                                    },
                                    onDelete = {
                                        playlistError = null
                                        val playlist = openedPlaylist ?: LibraryPlaylist(detailId, name, emptyList())
                                        playlistToDelete = PlaylistDeletion(playlist, isCollection)
                                    },
                                    onChooseSongs = {
                                        playlistError = null
                                        showPlaylistSongs = true
                                    },
                                    onAddSong = { track ->
                                        if (openedPlaylist != null && track.libraryId !in openedPlaylist.trackIds) {
                                            updatePlaylists(onSaved = { successMessage = "已添加到播放列表。" }) {
                                                libraryStore.savePlaylist(detailId, name, openedPlaylist.trackIds + track.libraryId)
                                            }
                                        }
                                    },
                                    onPlaySuggestion = { playTrack(it, listOf(it)) },
                                    error = playlistError.takeUnless { showPlaylistEditor || playlistToDelete != null },
                                    artworkModifier = coverModifier(detailId),
                                    tracksModifier = tracksModifier,
                                ) { track ->
                                    if (isCollection) {
                                        ManagedSongRow(track, "collection-song-${track.libraryId}", playlistId = detailId) {
                                            playPlaylist(detailId, tracks, track)
                                        }
                                    } else {
                                        val playing = isTrackPlaying(track, detailId)
                                        PlaylistTrackItem(
                                            track,
                                            navigationAccent,
                                            modifier = Modifier.clickable(role = Role.Button) { playPlaylist(detailId, tracks, track) }
                                                .playingTrackHighlight(playing, navigationAccent)
                                                .padding(start = 8.dp),
                                        )
                                    }
                                }
                            }
                            return@PlaylistPageTransition
                        }

                        val filter = libraryFilter.trim()
                        val matchingSongs = remember(sortedLibrary, filter) {
                            if (filter.isEmpty()) sortedLibrary else sortedLibrary.filter { it.matchesLibrarySearch(filter) }
                        }
                        var songIndexGestureActive by remember { mutableStateOf(false) }

                        PullDownSearchLayout(
                            filter = libraryFilter,
                            onFilterChange = { libraryFilter = it },
                            accentColor = navigationAccent,
                            searchLabel = "搜索收藏歌曲",
                            accessibilityLabel = "曲库搜索",
                            enabled = libraryPagerState.currentPage != LibrarySection.Playlists.ordinal,
                            fullWidthContent = true,
                            ignoreSearchSwipe = { songIndexGestureActive },
                            header = { titleModifier ->
                                AppPageHeader(AppTab.Library.label, headerLogo, headerLogoModifier, titleModifier)
                            },
                        ) {
                            PrimaryTabRow(
                                selectedTabIndex = libraryPagerState.currentPage,
                                modifier = Modifier.padding(horizontal = 16.dp),
                                containerColor = MaterialTheme.colorScheme.surface,
                                indicator = {
                                    TabRowDefaults.PrimaryIndicator(
                                        Modifier.tabIndicatorOffset(libraryPagerState.currentPage, matchContentSize = true),
                                        color = navigationAccent,
                                    )
                                },
                            ) {
                                LibrarySection.entries.forEach { section ->
                                    Tab(
                                        selected = libraryPagerState.currentPage == section.ordinal,
                                        onClick = {
                                            scope.launch { libraryPagerState.animateScrollToPage(section.ordinal) }
                                        },
                                        text = { Text(section.label) },
                                        selectedContentColor = navigationAccent,
                                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            HorizontalPager(
                                state = libraryPagerState,
                                modifier = Modifier.fillMaxWidth().weight(1f),
                                key = { LibrarySection.entries[it] },
                            ) { pageIndex ->
                                val librarySection = LibrarySection.entries[pageIndex]

                                @Composable
                                fun SongRow(track: Track) {
                                    ManagedSongRow(
                                        track = track,
                                        rowKey = "library-song-${librarySection.name}-${track.libraryId}",
                                        swipeToManage = librarySection == LibrarySection.Songs,
                                        onPlay = { playTrackList(sortedLibrary, selectedTrack = track) },
                                    )
                                }

                                Column(
                                    Modifier.fillMaxSize().padding(
                                        start = 16.dp,
                                        end = if (librarySection == LibrarySection.Songs) 0.dp else 16.dp,
                                    ),
                                ) {
                                    if (librarySection == LibrarySection.Playlists) {
                                        Button(
                                            onClick = { editPlaylist(null) },
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = navigationAccent),
                                        ) {
                                            Icon(Icons.Filled.Add, contentDescription = null)
                                            Text("新建播放列表")
                                        }
                                    }
                                    if (librarySection == LibrarySection.Songs) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(end = 16.dp).padding(vertical = 12.dp),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        ) {
                                            Button(
                                                onClick = { playTrackList(sortedLibrary, shuffle = false) },
                                                modifier = Modifier.weight(1f),
                                                enabled = libraryTracks.isNotEmpty(),
                                                shape = RoundedCornerShape(32.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = navigationAccent),
                                            ) {
                                                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                                                Text("播放")
                                            }
                                            Button(
                                                onClick = { playTrackList(sortedLibrary, shuffle = true) },
                                                modifier = Modifier.weight(1f),
                                                enabled = libraryTracks.isNotEmpty(),
                                                shape = RoundedCornerShape(32.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = navigationAccent),
                                            ) {
                                                Icon(Icons.Filled.Shuffle, contentDescription = null)
                                                Text("随机播放")
                                            }
                                        }
                                    }
                                    val listModifier = Modifier.fillMaxWidth().weight(1f)
                                    when (librarySection) {
                                        LibrarySection.Playlists -> {
                                            val collections = remember(libraryTracks) {
                                                libraryTracks.filter { it.collectionId != null }
                                                    .groupBy { checkNotNull(it.collectionId) }
                                            }
                                            val playlistTracks = remember(libraryPlaylists, libraryTracksById) {
                                                libraryPlaylists.map { it to it.tracksInTitleOrder(libraryTracksById) }
                                            }
                                            LazyColumn(listModifier, state = playlistOverviewState) {
                                                if (collections.isEmpty() && libraryPlaylists.isEmpty()) {
                                                    item { LibraryEmptyMessage("暂无播放列表。") }
                                                }
                                                playlistTracks.forEach { (playlist, tracks) ->
                                                    item(key = "custom-playlist-${playlist.id}") {
                                                        PlaylistRow(
                                                            name = playlist.name,
                                                            tracks = tracks,
                                                            accentColor = navigationAccent,
                                                            onOpen = { openPlaylist(playlist.id) },
                                                            onPlay = { playPlaylist(playlist.id, tracks) },
                                                            artworkModifier = coverModifier(playlist.id),
                                                        )
                                                    }
                                                }
                                                collections.forEach { (collectionId, tracks) ->
                                                    val first = tracks.first()
                                                    item(key = "library-group-$collectionId") {
                                                        PlaylistRow(
                                                            name = first.videoTitle.ifBlank { first.title },
                                                            tracks = tracks,
                                                            accentColor = navigationAccent,
                                                            onOpen = { openPlaylist(collectionId) },
                                                            onPlay = { playPlaylist(collectionId, tracks) },
                                                            artworkModifier = coverModifier(collectionId),
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        LibrarySection.Artists -> {
                                            val artists = remember(matchingSongs) {
                                                val order = compareBy<String> { it.isBlank() }.then(LibraryTitleOrder)
                                                matchingSongs.groupBy { it.artist.orEmpty().trim() }.entries
                                                    .sortedWith { first, second -> order.compare(first.key, second.key) }
                                            }
                                            LazyColumn(listModifier) {
                                                if (artists.isEmpty()) {
                                                    item {
                                                        LibraryEmptyMessage(
                                                            if (libraryTracks.isEmpty()) "暂无歌手。"
                                                            else "没有匹配的歌手。",
                                                        )
                                                    }
                                                }
                                                artists.forEach { (artist, songs) ->
                                                    val expanded = artist == expandedLibraryArtist
                                                    item(key = "library-artist-$artist") {
                                                        ListItem(
                                                            headlineContent = { Text(artist.ifBlank { "未知歌手" }) },
                                                            supportingContent = { Text("${songs.size} 首歌曲") },
                                                            leadingContent = {
                                                                Icon(Icons.Filled.Person, contentDescription = null)
                                                            },
                                                            trailingContent = {
                                                                Icon(
                                                                    if (expanded) Icons.Filled.ExpandLess
                                                                    else Icons.Filled.ExpandMore,
                                                                    contentDescription = null,
                                                                )
                                                            },
                                                            modifier = Modifier
                                                                .clickable(role = Role.Button) {
                                                                    revealedLibraryRowKey = null
                                                                    expandedLibraryArtist = if (expanded) null else artist
                                                                }
                                                                .semantics {
                                                                    stateDescription = if (expanded) "已展开" else "已收起"
                                                                },
                                                        )
                                                        HorizontalDivider()
                                                    }
                                                    if (expanded) {
                                                        items(songs, key = { it.libraryId }) { SongRow(it) }
                                                    }
                                                }
                                            }
                                        }

                                        LibrarySection.Songs -> {
                                            val sections = remember(matchingSongs) {
                                                matchingSongs.groupBy { LibraryTitleOrder.initial(it.title) }
                                            }
                                            LibrarySongList(
                                                sections = sections,
                                                accentColor = navigationAccent,
                                                emptyMessage = if (libraryTracks.isEmpty()) {
                                                    "曲库还是空的。搜索视频后可将曲目添加到这里。"
                                                } else "没有匹配的歌曲。",
                                                onIndexGestureChange = { songIndexGestureActive = it },
                                                modifier = listModifier,
                                                songContent = { SongRow(it) },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                AppTab.UserInfo -> {
                    AppPageHeader(title = AppTab.UserInfo.label, logo = headerLogo, logoModifier = headerLogoModifier)
                    LazyColumn(modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        item {
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
                                    Text("\u767b\u5f55\u72b6\u6001", style = MaterialTheme.typography.titleMedium)
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        val avatar = bilibiliProfile?.avatarUrl.orEmpty()
                                        if (avatar.isNotBlank()) {
                                            AsyncImage(model = avatar, contentDescription = "B\u7ad9\u7528\u6237\u5934\u50cf",
                                                modifier = Modifier.size(40.dp).clip(CircleShape),
                                                contentScale = ContentScale.Crop)
                                        } else {
                                            Icon(Icons.Filled.Person, contentDescription = null,
                                                modifier = Modifier.size(40.dp).clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.surface).padding(8.dp))
                                        }
                                        Column(Modifier.weight(1f)) {
                                            Text(if (isBilibiliLoggedIn) bilibiliProfile?.username.orEmpty().ifBlank { "B\u7ad9\u7528\u6237" } else "\u672a\u767b\u5f55",
                                                style = MaterialTheme.typography.titleMedium,
                                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            if (bilibiliIsVip) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    AsyncImage(
                                                        model = bilibiliProfile?.vipBadgeUrl.orEmpty().ifBlank {
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
                                        Button(
                                            onClick = {
                                                errorMessage = null
                                                if (isBilibiliLoggedIn) {
                                                    val cookieManager = WebCookieManager.getInstance()
                                                    updateBilibiliSession(null)
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
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = navigationAccent),
                                        ) {
                                            Text(
                                                if (isBilibiliLoggedIn) "\u9000\u51fa\u767b\u5f55" else "\u767b\u5f55 B\u7ad9",
                                                maxLines = 1,
                                            )
                                        }
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
                val colors = MaterialTheme.colorScheme
                val background = when {
                    isError && isDarkTheme -> Color(0xFFFFB36B)
                    isError -> colors.error
                    // Darken the accent slightly so the white body text has enough contrast.
                    else -> Color.Black.copy(alpha = 0.08f).compositeOver(navigationAccent)
                }
                val foreground = when {
                    isError && isDarkTheme -> Color(0xFF291708)
                    isError -> colors.onError
                    else -> Color.White
                }
                Text(
                    text = message,
                    color = foreground,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .background(background, RoundedCornerShape(24.dp))
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                )
            }
        }
        }
    }

    Box(
        Modifier.fillMaxSize().clipToBounds()
            .onGloballyPositioned { playerHostOrigin = it.positionInRoot() },
    ) {
        MainScaffold(if (playerLayerVisible) Modifier.clearAndSetSemantics {} else Modifier)
        val blockTouches = Modifier.pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    awaitPointerEvent().changes.forEach { it.consume() }
                }
            }
        }
        if (playerLayerVisible) {
            Box(Modifier.matchParentSize().then(blockTouches))
            val panelInteractive = showPlayerDetails && !playerTransition.isRunning
            NowPlayingPanel(
                progress = playerPanelProgress,
                miniPlayerBounds = miniPlayerBounds?.translate(-playerHostOrigin),
                miniPlayerColor = miniPlayerColor,
                modifier = Modifier.matchParentSize()
                    .then(if (panelInteractive) Modifier else Modifier.clearAndSetSemantics {}),
            ) {
                playbackTrack?.let { PlayerPage(it, isClosing = !showPlayerDetails) }
                if (!panelInteractive) {
                    Box(Modifier.matchParentSize().then(blockTouches))
                }
            }
        }
    }
}

@Composable
private fun LibraryMetadataDialog(
    accentColor: Color,
    heading: String,
    titleLabel: String,
    title: String,
    artist: String,
    phrases: List<String>,
    confirmLabel: String,
    saving: Boolean,
    onTitleChange: (String) -> Unit,
    onArtistChange: (String) -> Unit,
    guidance: String? = null,
    onTitleSuggestionSelected: (String) -> Unit = onTitleChange,
    onArtistSuggestionSelected: (String) -> Unit = onArtistChange,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    LibraryDialog(
        accentColor = accentColor,
        onDismissRequest = onDismiss,
        title = { Text(heading) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                guidance?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                LibraryMetadataField(
                    label = titleLabel,
                    value = title,
                    onValueChange = onTitleChange,
                    onSuggestionSelected = onTitleSuggestionSelected,
                    phrases = phrases,
                    enabled = !saving,
                )
                LibraryMetadataField(
                    label = "歌手",
                    value = artist,
                    onValueChange = onArtistChange,
                    onSuggestionSelected = onArtistSuggestionSelected,
                    phrases = phrases,
                    enabled = !saving,
                )
            }
        },
        confirmLabel = confirmLabel,
        onConfirm = onConfirm,
        confirmEnabled = !saving && title.isNotBlank(),
        dismissEnabled = !saving,
    )
}

@Composable
private fun LibraryMetadataField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    onSuggestionSelected: (String) -> Unit,
    phrases: List<String>,
    enabled: Boolean,
) {
    Column {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(label) },
            singleLine = true,
            enabled = enabled,
        )
        if (phrases.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(phrases) { phrase ->
                    FilterChip(
                        selected = value == phrase,
                        onClick = { onSuggestionSelected(phrase) },
                        enabled = enabled,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                            selectedLabelColor = MaterialTheme.colorScheme.onSurface,
                        ),
                        label = {
                            Text(
                                phrase,
                                modifier = Modifier.widthIn(max = 180.dp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchPageLayout(
    docked: Boolean,
    showLoading: Boolean,
    accentColor: Color,
    searchControls: @Composable () -> Unit,
    resultsContent: @Composable () -> Unit,
) {
    val controlsOffsetFraction by animateFloatAsState(
        targetValue = if (docked) 0f else 0.5f,
        animationSpec = tween(durationMillis = 340, easing = FastOutSlowInEasing),
        label = "Search controls position",
    )

    Layout(
        modifier = Modifier.fillMaxSize().clipToBounds(),
        content = {
            Column(Modifier.fillMaxWidth()) { searchControls() }
            Box(Modifier.fillMaxSize()) {
                resultsContent()
                AnimatedVisibility(
                    visible = showLoading,
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 16.dp),
                    enter = fadeIn(tween(140)),
                    exit = fadeOut(tween(100)),
                ) {
                    CircularProgressIndicator(color = accentColor)
                }
            }
        },
    ) { measurables, constraints ->
        val loose = constraints.copy(minHeight = 0)
        val controls = measurables[0].measure(loose)
        val resultsHeight = constraints.maxHeight - controls.height
        val results = measurables[1].measure(loose.copy(maxHeight = resultsHeight))

        layout(constraints.maxWidth, constraints.maxHeight) {
            val controlsTop = (resultsHeight * controlsOffsetFraction).roundToInt()
            controls.placeRelative(0, controlsTop)
            results.placeRelative(0, controlsTop + controls.height)
        }
    }
}

@Composable
private fun AppPageHeader(
    title: String,
    logo: EchoPlayLogo,
    logoModifier: Modifier,
    titleModifier: Modifier = Modifier,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).then(titleModifier),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(Modifier.size(28.dp).then(logoModifier).clip(CircleShape)) {
                Image(
                    painter = painterResource(logo.background),
                    contentDescription = null,
                    modifier = Modifier.matchParentSize().scale(BADGE_ARTWORK_SCALE),
                )
                Image(
                    painter = painterResource(logo.mark),
                    contentDescription = null,
                    modifier = Modifier.matchParentSize().scale(BADGE_ARTWORK_SCALE),
                )
            }
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.widthIn(max = 132.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
internal fun LibraryEmptyMessage(message: String) {
    Text(
        message,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(vertical = 16.dp),
    )
}

internal fun Track.matchesLibrarySearch(query: String): Boolean =
    title.contains(query, ignoreCase = true) ||
        artist.orEmpty().contains(query, ignoreCase = true) ||
        videoTitle.contains(query, ignoreCase = true)

@Composable
internal fun PullDownSearchLayout(
    filter: String,
    onFilterChange: (String) -> Unit,
    accentColor: Color,
    searchLabel: String,
    accessibilityLabel: String,
    enabled: Boolean = true,
    fullWidthContent: Boolean = false,
    ignoreSearchSwipe: () -> Boolean = { false },
    header: @Composable (Modifier) -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val shouldIgnoreSearchSwipe by rememberUpdatedState(ignoreSearchSwipe)
    var searchVisible by remember(enabled) { mutableStateOf(false) }
    var searchFocused by remember(enabled) { mutableStateOf(false) }
    var touchInProgress by remember { mutableStateOf(false) }
    var activityVersion by remember { mutableIntStateOf(0) }

    fun updateSearchVisibility(visible: Boolean) {
        if (!visible && searchFocused) focusManager.clearFocus()
        searchVisible = visible
    }

    fun updateFilter(value: String) {
        activityVersion++
        onFilterChange(value)
    }

    LaunchedEffect(searchVisible, searchFocused, touchInProgress, activityVersion) {
        if (!searchVisible || searchFocused || touchInProgress) return@LaunchedEffect

        val activityAtStart = activityVersion
        delay(3_000)
        if (searchVisible && !searchFocused && !touchInProgress && activityVersion == activityAtStart) {
            updateSearchVisibility(false)
        }
    }

    val searchGesture = if (enabled) {
        Modifier.pointerInput(focusManager) {
            val swipeDistance = maxOf(36.dp.toPx(), viewConfiguration.touchSlop)
            awaitEachGesture {
                val down = awaitFirstDown(
                    requireUnconsumed = false,
                    pass = PointerEventPass.Final,
                )
                touchInProgress = true
                var verticalDirection = 0f
                var gestureHandled = shouldIgnoreSearchSwipe()
                try {
                    do {
                        val event = awaitPointerEvent(PointerEventPass.Final)
                        val change = event.changes.firstOrNull { it.id == down.id }
                        if (!gestureHandled && change != null) {
                            val movement = change.position - down.position
                            if (verticalDirection == 0f &&
                                movement.getDistance() > viewConfiguration.touchSlop
                            ) {
                                if (abs(movement.y) > abs(movement.x)) {
                                    verticalDirection = movement.y.sign
                                } else {
                                    gestureHandled = true
                                }
                            }
                            if (movement.y * verticalDirection >= swipeDistance) {
                                updateSearchVisibility(verticalDirection > 0f)
                                gestureHandled = true
                            }
                        }
                    } while (event.changes.any { it.pressed })
                } finally {
                    touchInProgress = false
                    activityVersion++
                }
            }
        }
    } else Modifier

    Column(Modifier.fillMaxSize().then(searchGesture)) {
        val titleModifier = if (enabled) {
            Modifier.semantics {
                customActions = listOf(
                    CustomAccessibilityAction(
                        label = if (searchVisible) "收起$accessibilityLabel" else "展开$accessibilityLabel",
                    ) {
                        updateSearchVisibility(!searchVisible)
                        true
                    },
                )
            }
        } else Modifier
        Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) { header(titleModifier) }
        AnimatedVisibility(
            visible = searchVisible,
            enter = expandVertically(animationSpec = tween(220), expandFrom = Alignment.Top) +
                slideInVertically(animationSpec = tween(220)) { -it },
            exit = shrinkVertically(animationSpec = tween(180), shrinkTowards = Alignment.Top) +
                slideOutVertically(animationSpec = tween(180)) { -it },
        ) {
            OutlinedTextField(
                value = filter,
                onValueChange = { updateFilter(it) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).onFocusChanged {
                    searchFocused = it.hasFocus
                    if (it.hasFocus) activityVersion++
                },
                label = { Text(searchLabel) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (filter.isNotEmpty()) {
                        IconButton(onClick = { updateFilter("") }) {
                            Icon(Icons.Filled.Cancel, contentDescription = "清空筛选")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accentColor,
                    focusedLabelColor = accentColor,
                    cursorColor = accentColor,
                ),
            )
        }
        Column(
            modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = if (fullWidthContent) 0.dp else 16.dp),
            content = content,
        )
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
private fun SearchResultAddButton(
    added: Boolean,
    addLabel: String,
    accentColor: Color,
    onAdd: () -> Unit,
) {
    IconButton(
        onClick = onAdd,
        enabled = !added,
        colors = IconButtonDefaults.iconButtonColors(disabledContentColor = accentColor),
    ) {
        Icon(
            imageVector = if (added) Icons.Filled.Check else Icons.Filled.Add,
            contentDescription = if (added) "已加入曲库" else addLabel,
        )
    }
}

private fun Modifier.playingTrackHighlight(playing: Boolean, color: Color, showLine: Boolean = true): Modifier =
    if (playing) drawBehind {
        drawRect(color.copy(alpha = 0.12f))
        if (showLine) {
            val width = 4.dp.toPx().coerceAtMost(size.width)
            val left = if (layoutDirection == LayoutDirection.Ltr) 0f else size.width - width
            drawRect(color, topLeft = Offset(left, 0f), size = Size(width, size.height))
        }
    } else this

@Composable
private fun TrackRow(
    track: Track,
    subtitle: String,
    playingIndicatorColor: Color,
    currentlyPlaying: Boolean = false,
    onPlay: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    expanded: Boolean = false,
    onTogglePages: (() -> Unit)? = null,
    showCover: Boolean = false,
    showBottomDivider: Boolean = true,
    titleMaxLines: Int = 2,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth()
            .playingTrackHighlight(currentlyPlaying, playingIndicatorColor)
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
                    maxLines = titleMaxLines,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitle.isNotBlank()) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (onTogglePages != null) {
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
            trailingContent?.invoke()
        }
        if (showBottomDivider) HorizontalDivider()
    }
}

private enum class LibrarySwipePosition {
    Closed,
    Actions,
    Dismissed,
}

private fun libraryDeleteThreshold(actionWidth: Float, rowWidth: Float, extraDistance: Float): Float =
    actionWidth + minOf(extraDistance, (rowWidth - actionWidth).coerceAtLeast(0f) / 2f)

@Composable
private fun SwipeableLibraryTrackRow(
    track: Track,
    playingIndicatorColor: Color,
    isDarkTheme: Boolean,
    currentlyPlaying: Boolean,
    revealed: Boolean,
    onRevealChanged: (Boolean) -> Unit,
    onPlay: () -> Unit,
    onRename: () -> Unit,
    onDelete: (LibraryRowHeightFraction, suspend () -> Unit) -> Unit,
    swipeToManage: Boolean = false,
) {
    val density = LocalDensity.current
    val dragDirection = if (LocalLayoutDirection.current == LayoutDirection.Ltr) 1f else -1f
    val preferredActionWidthPx = with(density) { 160.dp.toPx() }
    val extraDeleteDistancePx = with(density) { 56.dp.toPx() }
    var rowWidthPx by remember { mutableIntStateOf(0) }
    val actionWidthPx = if (rowWidthPx > 0) {
        minOf(preferredActionWidthPx, rowWidthPx * .65f)
    } else {
        preferredActionWidthPx
    }
    val swipeState = remember(actionWidthPx) {
        AnchoredDraggableState(
            initialValue = if (revealed) LibrarySwipePosition.Actions else LibrarySwipePosition.Closed,
            anchors = DraggableAnchors {
                LibrarySwipePosition.Closed at 0f
                LibrarySwipePosition.Actions at -actionWidthPx
            },
        )
    }
    val swipeSpec = remember { tween<Float>(durationMillis = 200) }
    val swipeScope = rememberCoroutineScope()
    val heightFraction = remember { Animatable(1f) }
    var removing by remember { mutableStateOf(false) }
    val currentOnRevealChanged by rememberUpdatedState(onRevealChanged)
    val currentOnDelete by rememberUpdatedState(onDelete)
    val actionsEnabled = revealed && !removing &&
        swipeState.settledValue == LibrarySwipePosition.Actions
    val rowOffset = Modifier.offset { IntOffset(swipeState.requireOffset().roundToInt(), 0) }
    val rowSwipe = if (swipeToManage && !removing) {
        Modifier.pointerInput(swipeState, actionWidthPx, extraDeleteDistancePx, dragDirection) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                // Leave drags toward the previous page unconsumed for the pager.
                val start = awaitHorizontalTouchSlopOrCancellation(down.id) { change, overSlop ->
                    if (overSlop * dragDirection < 0f && !swipeState.isAnimationRunning) {
                        change.consume()
                        swipeState.dispatchRawDelta(overSlop * dragDirection)
                    }
                } ?: return@awaitEachGesture

                val finished = horizontalDrag(start.id) { change ->
                    swipeState.dispatchRawDelta(change.positionChange().x * dragDirection)
                    change.consume()
                }
                val dragged = -swipeState.requireOffset()
                val endAnchor = swipeState.anchors.positionOf(LibrarySwipePosition.Dismissed)
                val deleteDistance = libraryDeleteThreshold(actionWidthPx, -endAnchor, extraDeleteDistancePx)
                val target = when {
                    !finished -> swipeState.settledValue
                    endAnchor.isFinite() && dragged >= deleteDistance -> LibrarySwipePosition.Dismissed
                    dragged >= actionWidthPx / 2f -> LibrarySwipePosition.Actions
                    else -> LibrarySwipePosition.Closed
                }
                swipeScope.launch { swipeState.animateTo(target, swipeSpec) }
            }
        }
    } else {
        Modifier
    }

    fun removeRow() {
        if (removing) return
        removing = true
        swipeScope.launch {
            if (swipeState.anchors.positionOf(LibrarySwipePosition.Dismissed).isFinite()) {
                swipeState.animateTo(LibrarySwipePosition.Dismissed, swipeSpec)
            }
            currentOnRevealChanged(false)
            currentOnDelete(heightFraction) {
                swipeState.snapTo(LibrarySwipePosition.Closed)
                heightFraction.animateTo(1f, swipeSpec)
                removing = false
            }
        }
    }

    fun actionPanelWidth(): Float = maxOf(actionWidthPx, -swipeState.requireOffset())

    fun remainingRenameWidth(panelWidth: Float): Float {
        val takeoverDistance = libraryDeleteThreshold(
            actionWidthPx,
            rowWidthPx.toFloat(),
            extraDeleteDistancePx,
        ) - actionWidthPx
        val progress = ((panelWidth - actionWidthPx) / takeoverDistance.coerceAtLeast(1f))
            .coerceIn(0f, 1f)
        return actionWidthPx / 2f * (1f - progress)
    }

    LaunchedEffect(swipeState, rowWidthPx) {
        swipeState.updateAnchors(DraggableAnchors {
            LibrarySwipePosition.Closed at 0f
            LibrarySwipePosition.Actions at -actionWidthPx
            if (rowWidthPx > actionWidthPx) LibrarySwipePosition.Dismissed at -rowWidthPx.toFloat()
        })
    }
    LaunchedEffect(swipeState) {
        snapshotFlow { swipeState.settledValue }
            .drop(1)
            .collect { position ->
                if (position == LibrarySwipePosition.Dismissed) {
                    removeRow()
                } else {
                    currentOnRevealChanged(position == LibrarySwipePosition.Actions)
                }
            }
    }
    LaunchedEffect(swipeState, revealed) {
        if (!revealed && !removing && swipeState.settledValue == LibrarySwipePosition.Actions) {
            swipeState.animateTo(LibrarySwipePosition.Closed, swipeSpec)
        }
    }

    val colors = MaterialTheme.colorScheme
    val deleteColor = if (isDarkTheme) colors.errorContainer else colors.error
    val onDeleteColor = if (isDarkTheme) colors.onErrorContainer else colors.onError

    Column(
        Modifier.clipToBounds().layout { measurable, constraints ->
            val row = measurable.measure(constraints)
            val height = (row.height * heightFraction.value).roundToInt()
                .coerceIn(constraints.minHeight, constraints.maxHeight)
            layout(row.width, height) { row.placeRelative(0, 0) }
        },
    ) {
        Box(
            modifier = Modifier.fillMaxWidth()
                .onSizeChanged { rowWidthPx = it.width }
                .clipToBounds()
                .then(rowSwipe),
        ) {
            Spacer(
                Modifier.matchParentSize().drawBehind {
                    val panelWidth = actionPanelWidth()
                    val deleteWidth = (panelWidth - remainingRenameWidth(panelWidth))
                        .coerceAtMost(size.width)
                    val left = if (layoutDirection == LayoutDirection.Ltr) size.width - deleteWidth else 0f
                    drawRect(deleteColor, topLeft = Offset(left, 0f), size = Size(deleteWidth, size.height))
                },
            )
            Layout(
                modifier = Modifier.align(Alignment.CenterEnd).clipToBounds()
                    .then(if (actionsEnabled) Modifier else Modifier.clearAndSetSemantics {}),
                content = {
                    Column(
                        modifier = Modifier
                            .clickable(enabled = actionsEnabled) {
                                onRevealChanged(false)
                                onRename()
                            }
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(Icons.Filled.Edit, contentDescription = "重命名")
                        Text("重命名", style = MaterialTheme.typography.labelSmall, maxLines = 1)
                    }
                    Column(
                        modifier = Modifier
                            .clickable(enabled = actionsEnabled) { removeRow() }
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = "删除", tint = onDeleteColor)
                        Text("删除", style = MaterialTheme.typography.labelSmall, color = onDeleteColor, maxLines = 1)
                    }
                },
            ) { buttons, constraints ->
                val panelWidth = actionPanelWidth().roundToInt().coerceAtMost(constraints.maxWidth)
                val renameWidth = (actionWidthPx / 2f).roundToInt().coerceAtMost(panelWidth)
                val visibleRename = remainingRenameWidth(panelWidth.toFloat()).roundToInt().coerceAtMost(renameWidth)
                val deleteWidth = panelWidth - visibleRename
                val rename = buttons[0].measure(constraints.copy(minWidth = renameWidth, maxWidth = renameWidth))
                val delete = buttons[1].measure(constraints.copy(minWidth = deleteWidth, maxWidth = deleteWidth))
                val height = maxOf(rename.height, delete.height)
                layout(panelWidth, height) {
                    rename.placeRelative(visibleRename - renameWidth, (height - rename.height) / 2)
                    delete.placeRelative(visibleRename, (height - delete.height) / 2)
                }
            }
            // Some part rows are shorter than the actions. Cover the full row while it slides.
            Spacer(
                Modifier.matchParentSize()
                    .then(rowOffset)
                    .background(MaterialTheme.colorScheme.surface),
            )
            TrackRow(
                track = track,
                subtitle = track.artist.orEmpty(),
                playingIndicatorColor = playingIndicatorColor,
                currentlyPlaying = currentlyPlaying,
                onPlay = if (removing) null else {
                    { if (revealed) onRevealChanged(false) else onPlay() }
                },
                showCover = true,
                showBottomDivider = false,
                titleMaxLines = 1,
                modifier = Modifier.align(Alignment.CenterStart).then(rowOffset),
                trailingContent = if (swipeToManage) null else ({
                    IconButton(
                        onClick = {
                            val target = if (revealed) LibrarySwipePosition.Closed else LibrarySwipePosition.Actions
                            swipeScope.launch { swipeState.animateTo(target, swipeSpec) }
                        },
                        enabled = !removing,
                    ) {
                        Icon(
                            Icons.Filled.MoreVert,
                            contentDescription = if (revealed) "收起操作" else "${track.title}的更多操作",
                        )
                    }
                }),
            )
        }
        HorizontalDivider()
        Spacer(Modifier.height(4.dp))
    }
}
