package com.echoplay.android

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitVerticalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.gestures.verticalDrag
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.Player
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.HorizontalAlignmentLine
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.roundToLong

private val PlayerBoundsTransform = BoundsTransform { _, _ ->
    spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
}

private val PlaybackControlSpacing = 8.dp
private val PlaybackTimelineCenter = HorizontalAlignmentLine(::minOf)

@Composable
internal fun NowPlayingPanel(
    progress: State<Float>,
    miniPlayerBounds: Rect?,
    miniPlayerColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier.drawWithCache {
            val clip = Path()
            val fallbackHeight = 64.dp.toPx()
            val cornerRadius = 24.dp.toPx()
            onDrawWithContent panel@{
                val fraction = progress.value.coerceIn(0f, 1f)
                if (fraction >= 1f) {
                    drawContent()
                } else if (fraction > 0f) {
                    val bounds = playerPanelBounds(miniPlayerBounds, size, fallbackHeight, fraction)
                    clip.rewind()
                    clip.addRoundRect(RoundRect(bounds, CornerRadius(cornerRadius * (1f - fraction))))
                    clipPath(clip) {
                        drawRect(
                            color = lerp(miniPlayerColor, colors.surface, fraction),
                            alpha = (fraction / 0.20f).coerceIn(0f, 1f),
                        )
                        this@panel.drawContent()
                    }
                }
            }
        },
    ) {
        Box(
            Modifier.fillMaxSize().graphicsLayer {
                val fraction = progress.value.coerceIn(0f, 1f)
                val reveal = ((fraction - 0.16f) / 0.62f).coerceIn(0f, 1f)
                alpha = reveal * reveal * (3f - 2f * reveal)
                val scale = 0.68f + 0.32f * fraction
                scaleX = scale
                scaleY = scale
                if (fraction < 1f) {
                    val bounds = playerPanelBounds(miniPlayerBounds, size, 64.dp.toPx(), fraction)
                    translationX = bounds.center.x - size.width / 2f
                    translationY = bounds.center.y - size.height / 2f
                } else {
                    translationX = 0f
                    translationY = 0f
                }
            },
        ) { content() }
    }
}

private fun playerPanelBounds(source: Rect?, size: Size, fallbackHeight: Float, fraction: Float): Rect {
    val start = source?.takeUnless { it.isEmpty }
        ?: Rect(0f, (size.height - fallbackHeight).coerceAtLeast(0f), size.width, size.height)
    val remaining = 1f - fraction
    return Rect(
        left = start.left * remaining,
        top = start.top * remaining,
        right = start.right * remaining + size.width * fraction,
        bottom = start.bottom * remaining + size.height * fraction,
    )
}

internal fun PlaybackRepeatMode.advancesQueue(shuffleEnabled: Boolean): Boolean =
    this == PlaybackRepeatMode.List || (this == PlaybackRepeatMode.Off && shuffleEnabled)

internal fun List<Track>.upcomingTrackIndices(
    currentTrackId: String,
    shuffleEnabled: Boolean,
    repeatMode: PlaybackRepeatMode,
): List<Int> {
    if (!shuffleEnabled && repeatMode == PlaybackRepeatMode.Off) return emptyList()
    val currentIndex = indexOfFirst { it.libraryId == currentTrackId }
    if (currentIndex < 0) return emptyList()
    val following = currentIndex + 1 until size
    return buildList {
        addAll(following)
        addAll(0 until currentIndex)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
internal fun PlayerDetailsScreen(
    track: Track,
    isClosing: Boolean,
    accentColor: Color,
    isPlaying: Boolean,
    isLoading: Boolean,
    status: String,
    player: Player,
    volume: Float,
    onClose: () -> Unit,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onVolumeChangeFinished: () -> Unit,
    queue: List<Track>,
    shuffleEnabled: Boolean,
    repeatMode: PlaybackRepeatMode,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onSelectTrack: (Track) -> Unit,
    onMoveTrack: (String, String) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    var showQueue by rememberSaveable { mutableStateOf(false) }
    BackHandler {
        if (!isClosing) {
            if (showQueue) showQueue = false else onClose()
        }
    }
    val queueListState = rememberLazyListState()
    val queueButtonDiameter by animateDpAsState(
        targetValue = if (showQueue) 40.dp else 48.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "Queue button circle diameter",
    )
    val queueButtonColor by animateColorAsState(
        if (showQueue) accentColor else Color.Transparent,
        tween(180),
        label = "Queue button background",
    )
    val queueIconColor by animateColorAsState(
        if (showQueue) Color.White else accentColor,
        tween(180),
        label = "Queue button icon",
    )
    val queueToggle: @Composable (Modifier) -> Unit = { modifier ->
        IconButton(
            modifier = modifier.drawBehind {
                drawCircle(
                    color = queueButtonColor,
                    radius = minOf(queueButtonDiameter.toPx(), size.minDimension) / 2f,
                )
            }.semantics { selected = showQueue },
            onClick = { showQueue = !showQueue },
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = Color.Transparent,
                contentColor = queueIconColor,
            ),
        ) {
            Icon(
                Icons.AutoMirrored.Filled.QueueMusic,
                contentDescription = if (showQueue) "收起播放列表" else "播放列表",
            )
        }
    }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize().background(colors.surface).background(
            Brush.verticalGradient(
                listOf(accentColor.copy(alpha = 0.28f), colors.surfaceVariant, colors.surface),
            ),
        ).safeDrawingPadding(),
        contentAlignment = Alignment.TopCenter,
    ) {
        val compactHeight = maxHeight < 540.dp
        val coverSize = minOf(maxWidth * 0.78f, maxHeight * 0.36f, 360.dp)
        Column(
            modifier = Modifier.widthIn(max = 640.dp).fillMaxSize()
                .then(if (compactHeight) Modifier.verticalScroll(rememberScrollState()) else Modifier)
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(PlaybackControlSpacing),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Filled.Close, contentDescription = "关闭播放详情", tint = accentColor)
                }
                Text("正在播放", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            }
            SharedTransitionLayout(
                modifier = if (compactHeight) Modifier.fillMaxWidth() else Modifier.weight(1f).fillMaxWidth(),
            ) {
                AnimatedContent(
                    targetState = showQueue,
                    modifier = if (compactHeight) Modifier.fillMaxWidth() else Modifier.fillMaxSize(),
                    transitionSpec = {
                        (fadeIn(tween(170, delayMillis = 65)) togetherWith fadeOut(tween(130)))
                            .using(if (compactHeight) SizeTransform(clip = false) else null)
                    },
                    label = "Playback queue layout",
                ) { queueVisible ->
                    val artwork = Modifier.sharedElement(
                        rememberSharedContentState("Artwork"),
                        animatedVisibilityScope = this@AnimatedContent,
                        boundsTransform = PlayerBoundsTransform,
                    )
                    val trackInfo = Modifier.sharedBounds(
                        rememberSharedContentState("Track information"),
                        animatedVisibilityScope = this@AnimatedContent,
                        boundsTransform = PlayerBoundsTransform,
                        enter = fadeIn(tween(170)),
                        exit = fadeOut(tween(120)),
                    )
                    val queueButton = Modifier.sharedElement(
                        rememberSharedContentState("Queue button"),
                        animatedVisibilityScope = this@AnimatedContent,
                        boundsTransform = PlayerBoundsTransform,
                        zIndexInOverlay = 2f,
                    )
                    Column(
                        modifier = if (compactHeight) Modifier.fillMaxWidth() else Modifier.fillMaxSize(),
                        verticalArrangement = if (queueVisible || compactHeight) Arrangement.spacedBy(8.dp)
                            else Arrangement.SpaceEvenly,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        if (queueVisible) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                PlaybackCover(track, 76.dp, 14.dp, announce = true, modifier = artwork)
                                PlaybackTrackText(
                                    track,
                                    subtitle = track.playbackArtist.ifBlank { status },
                                    modifier = Modifier.weight(1f).then(trackInfo),
                                )
                                queueToggle(queueButton)
                            }
                            PlaybackOrderButtons(
                                shuffleEnabled, repeatMode, accentColor, onToggleShuffle, onToggleRepeat, expanded = true,
                            )
                            val upcoming = remember(queue, track.libraryId, shuffleEnabled, repeatMode) {
                                queue.upcomingTrackIndices(track.libraryId, shuffleEnabled, repeatMode).map(queue::get)
                            }
                            val queueSize = if (compactHeight) Modifier.height(220.dp) else Modifier.weight(1f)
                            UpcomingPlaybackQueue(
                                upcoming = upcoming,
                                singleRepeat = repeatMode == PlaybackRepeatMode.Single,
                                accentColor = accentColor,
                                onSelectTrack = onSelectTrack,
                                onMoveTrack = onMoveTrack,
                                reorderEnabled = showQueue,
                                listState = queueListState,
                                modifier = queueSize.animateEnterExit(
                                    enter = slideInVertically(tween(240)) { it / 16 },
                                    exit = slideOutVertically(tween(160)) { it / 20 },
                                ),
                            )
                        } else {
                            PlaybackCover(track, coverSize, 20.dp, announce = true, modifier = artwork)
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                PlaybackTrackText(
                                    track,
                                    subtitle = track.playbackArtist.ifBlank { status },
                                    modifier = Modifier.weight(1f).then(trackInfo),
                                )
                                PlaybackOrderButtons(shuffleEnabled, repeatMode, accentColor, onToggleShuffle, onToggleRepeat)
                                queueToggle(queueButton)
                            }
                        }
                    }
                }
            }

            val lifecycle = LocalLifecycleOwner.current.lifecycle
            var position by remember(player) { mutableLongStateOf(player.currentPosition.coerceAtLeast(0L)) }
            var duration by remember(player) { mutableLongStateOf(player.duration.coerceAtLeast(0L)) }
            LaunchedEffect(player, lifecycle, isPlaying, isClosing, track.libraryId) {
                lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    fun updateProgress() {
                        position = player.currentPosition.coerceAtLeast(0L)
                        duration = player.duration.coerceAtLeast(0L)
                    }
                    val listener = object : Player.Listener {
                        override fun onEvents(player: Player, events: Player.Events) {
                            if (events.containsAny(
                                    Player.EVENT_POSITION_DISCONTINUITY,
                                    Player.EVENT_TIMELINE_CHANGED,
                                    Player.EVENT_PLAYBACK_STATE_CHANGED,
                                )
                            ) updateProgress()
                        }
                    }
                    player.addListener(listener)
                    try {
                        updateProgress()
                        if (isPlaying && !isClosing) {
                            while (true) {
                                delay(500L)
                                updateProgress()
                            }
                        } else {
                            awaitCancellation()
                        }
                    } finally {
                        player.removeListener(listener)
                    }
                }
            }
            PlaybackControls {
                PlaybackProgress(
                    trackId = track.libraryId,
                    position = position,
                    duration = duration,
                    enabled = duration > 0L && !isLoading,
                    accentColor = accentColor,
                    onSeek = onSeek,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PlaybackTransportButton(
                        Icons.Filled.SkipPrevious, "上一首", accentColor,
                        !isLoading && queue.isNotEmpty() && (queue.size > 1 || position > 3_000L),
                        onPrevious, iconSize = 36.dp,
                    )
                    PlaybackTransportButton(
                        if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        if (isPlaying) "暂停" else "播放",
                        accentColor,
                        !isLoading,
                        onPlayPause,
                        iconSize = 48.dp,
                        buttonSize = 56.dp,
                    )
                    PlaybackTransportButton(
                        Icons.Filled.SkipNext, "下一首", accentColor, !isLoading && queue.size > 1,
                        onNext, iconSize = 36.dp,
                    )
                }

                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Filled.VolumeDown, contentDescription = "音量低", tint = accentColor)
                    Slider(
                        value = volume,
                        onValueChange = onVolumeChange,
                        onValueChangeFinished = onVolumeChangeFinished,
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                            .semantics { contentDescription = "播放音量" },
                        colors = SliderDefaults.colors(
                            thumbColor = accentColor,
                            activeTrackColor = accentColor,
                            inactiveTrackColor = accentColor.copy(alpha = 0.24f),
                        ),
                        thumb = { PlaybackSliderThumb(accentColor) },
                    )
                    Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "音量高", tint = accentColor)
                }
            }
        }
    }
}

@Composable
private fun PlaybackOrderButtons(
    shuffleEnabled: Boolean,
    repeatMode: PlaybackRepeatMode,
    accentColor: Color,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    expanded: Boolean = false,
) {
    Row(
        modifier = if (expanded) Modifier.fillMaxWidth() else Modifier,
        horizontalArrangement = Arrangement.spacedBy(if (expanded) 10.dp else 0.dp),
    ) {
        val buttonWidth = if (expanded) Modifier.weight(1f) else Modifier
        PlaybackOrderButton(
            Icons.Filled.Shuffle,
            description = if (shuffleEnabled) "随机播放已开启" else "开启随机播放",
            active = shuffleEnabled,
            accentColor = accentColor,
            onClick = onToggleShuffle,
            expanded = expanded,
            modifier = buttonWidth,
        )
        PlaybackOrderButton(
            if (repeatMode == PlaybackRepeatMode.Single) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
            description = when (repeatMode) {
                PlaybackRepeatMode.Off -> "开启列表循环"
                PlaybackRepeatMode.List -> "列表循环播放已开启"
                PlaybackRepeatMode.Single -> "单曲循环播放已开启"
            },
            active = repeatMode != PlaybackRepeatMode.Off,
            accentColor = accentColor,
            onClick = onToggleRepeat,
            expanded = expanded,
            modifier = buttonWidth,
        )
    }
}

@Composable
private fun PlaybackOrderButton(
    icon: ImageVector,
    description: String,
    active: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    expanded: Boolean,
    modifier: Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val semanticModifier = modifier.semantics { selected = active }
    val tint by animateColorAsState(
        when {
            active && expanded -> Color.White
            active -> accentColor
            else -> colors.onSurfaceVariant
        },
        tween(180),
        label = "Playback mode icon",
    )
    if (expanded) {
        val background by animateColorAsState(
            if (active) accentColor else colors.surfaceContainerHigh,
            tween(180),
            label = "Playback mode background",
        )
        FilledTonalIconButton(
            modifier = semanticModifier,
            onClick = onClick,
            shape = RoundedCornerShape(14.dp),
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = background,
                contentColor = tint,
            ),
        ) {
            PlaybackButtonIcon(icon, description)
        }
    } else {
        IconButton(modifier = semanticModifier, onClick = onClick) {
            PlaybackButtonIcon(icon, description, tint = tint)
        }
    }
}

@Composable
private fun PlaybackTransportButton(
    icon: ImageVector,
    description: String,
    accentColor: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    iconSize: Dp,
    buttonSize: Dp = 48.dp,
) {
    val interactions = remember { MutableInteractionSource() }
    val pressed by interactions.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed) 0.90f else 1f,
        spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
        label = "Playback button press",
    )
    IconButton(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactions,
        modifier = Modifier.size(buttonSize),
        colors = IconButtonDefaults.iconButtonColors(contentColor = accentColor),
    ) {
        PlaybackButtonIcon(
            icon,
            description,
            modifier = Modifier.size(iconSize).graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        )
    }
}

@Composable
private fun PlaybackButtonIcon(
    icon: ImageVector,
    description: String,
    modifier: Modifier = Modifier.size(24.dp),
    tint: Color = LocalContentColor.current,
) {
    AnimatedContent(
        targetState = icon,
        modifier = modifier.semantics { contentDescription = description },
        transitionSpec = {
            (fadeIn(tween(150)) + scaleIn(tween(150), initialScale = 0.84f)) togetherWith
                (fadeOut(tween(95)) + scaleOut(tween(95), targetScale = 0.84f))
        },
        label = "Playback icon",
    ) { visibleIcon ->
        Icon(visibleIcon, contentDescription = null, modifier = Modifier.fillMaxSize(), tint = tint)
    }
}

private class PlaybackQueueDrag(
    val track: Track,
    val artwork: Painter?,
    val original: List<Track>,
    val itemHeight: Int,
    var previewIndex: Int,
    top: Float,
) {
    val trackId = track.libraryId
    var top by mutableFloatStateOf(top)
    var preview by mutableStateOf(original)
}

@Composable
private fun UpcomingPlaybackQueue(
    upcoming: List<Track>,
    singleRepeat: Boolean,
    accentColor: Color,
    onSelectTrack: (Track) -> Unit,
    onMoveTrack: (String, String) -> Unit,
    reorderEnabled: Boolean,
    listState: LazyListState,
    modifier: Modifier = Modifier,
) {
    val currentOnMove by rememberUpdatedState(onMoveTrack)
    val currentUpcoming by rememberUpdatedState(upcoming)
    val canReorder by rememberUpdatedState(reorderEnabled)
    var activeDrag by remember { mutableStateOf<PlaybackQueueDrag?>(null) }
    val drag = activeDrag?.takeIf { reorderEnabled && it.original == upcoming }
    val displayed = drag?.preview ?: upcoming
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    val artworkSize = with(LocalDensity.current) { 44.dp.roundToPx() }
    val visibleArtwork = remember { mutableMapOf<String, Painter>() }
    val edgeSize = with(LocalDensity.current) { 56.dp.toPx() }
    val maxScrollPerSecond = with(LocalDensity.current) { 720.dp.toPx() }

    fun updatePreview(session: PlaybackQueueDrag) {
        val center = session.top + session.itemHeight / 2f
        val slot = listState.layoutInfo.visibleItemsInfo.minByOrNull {
            abs(it.offset + it.size / 2f - center)
        }?.index ?: return
        val tracks = session.preview
        val from = session.previewIndex
        if (slot !in tracks.indices || from == slot) return
        val firstVisible = listState.firstVisibleItemIndex
        if (firstVisible in minOf(from, slot)..maxOf(from, slot)) {
            listState.requestScrollToItem(firstVisible, listState.firstVisibleItemScrollOffset)
        }
        session.previewIndex = slot
        session.preview = tracks.toMutableList().apply {
            add(slot, removeAt(from))
        }
    }

    fun edgeScrollFraction(session: PlaybackQueueDrag): Float {
        val layout = listState.layoutInfo
        val edge = minOf(edgeSize, layout.viewportSize.height * 0.25f)
        if (edge <= 0f) return 0f
        val center = session.top + session.itemHeight / 2f
        val distanceFromTop = center - layout.viewportStartOffset
        val distanceFromBottom = layout.viewportEndOffset - center
        return when {
            distanceFromTop < edge && listState.canScrollBackward -> (distanceFromTop / edge - 1f).coerceAtLeast(-1f)
            distanceFromBottom < edge && listState.canScrollForward -> (1f - distanceFromBottom / edge).coerceAtMost(1f)
            else -> 0f
        }
    }

    LaunchedEffect(activeDrag, reorderEnabled, upcoming, listState, edgeSize, maxScrollPerSecond) {
        val session = activeDrag ?: return@LaunchedEffect
        if (!reorderEnabled || session.original != upcoming) {
            activeDrag = null
            return@LaunchedEffect
        }
        snapshotFlow { edgeScrollFraction(session) != 0f }.collectLatest { shouldScroll ->
            if (!shouldScroll) return@collectLatest
            var previousFrame = withFrameNanos { it }
            while (activeDrag === session) {
                val frame = withFrameNanos { it }
                val elapsed = (frame - previousFrame).coerceIn(0L, 33_333_333L) / 1_000_000_000f
                previousFrame = frame
                val fraction = edgeScrollFraction(session)
                if (fraction == 0f) break
                listState.scrollBy(fraction * maxScrollPerSecond * elapsed)
                if (activeDrag === session) updatePreview(session)
            }
        }
    }

    Column(modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("待播放", style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
            Text("${upcoming.size} 首", color = colors.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
        }
        if (singleRepeat && upcoming.isNotEmpty()) {
            Text(
                "当前歌曲正在单曲循环",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }
        if (upcoming.isEmpty()) {
            Text(
                if (singleRepeat) "当前歌曲正在单曲循环" else "没有待播放歌曲。",
                modifier = Modifier.padding(vertical = 12.dp),
                color = colors.onSurfaceVariant,
            )
        }
        Box(Modifier.weight(1f).fillMaxWidth().clipToBounds()) {
            LazyColumn(Modifier.fillMaxSize(), state = listState) {
                itemsIndexed(displayed, key = { _, track -> track.libraryId }) { index, track ->
                    val artwork = if (track.coverUrl.isNotBlank()) {
                        val request = remember(context, track.coverUrl, artworkSize) {
                            ImageRequest.Builder(context).data(track.coverUrl).size(artworkSize).build()
                        }
                        rememberAsyncImagePainter(request, contentScale = ContentScale.Crop)
                    } else null
                    DisposableEffect(track.libraryId, artwork) {
                        if (artwork != null) visibleArtwork[track.libraryId] = artwork
                        onDispose {
                            if (visibleArtwork[track.libraryId] === artwork) visibleArtwork.remove(track.libraryId)
                        }
                    }
                    val lifted = drag?.trackId == track.libraryId
                    val actions = if (drag == null && reorderEnabled) buildList {
                        displayed.getOrNull(index - 1)?.let { previous ->
                            add(CustomAccessibilityAction("上移") {
                                onMoveTrack(track.libraryId, previous.libraryId)
                                true
                            })
                        }
                        displayed.getOrNull(index + 1)?.let { next ->
                            add(CustomAccessibilityAction("下移") {
                                onMoveTrack(track.libraryId, next.libraryId)
                                true
                            })
                        }
                    } else emptyList()
                    Column(
                        Modifier.animateItem(
                            fadeInSpec = null,
                            fadeOutSpec = null,
                            placementSpec = if (lifted) null else spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMedium,
                            ),
                        ).then(if (lifted) Modifier.graphicsLayer { alpha = 0f } else Modifier),
                    ) {
                        PlaybackQueueRow(
                            track,
                            artwork,
                            showHandle = upcoming.size > 1,
                            actions = actions,
                            modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable(
                                enabled = drag == null && reorderEnabled,
                                role = Role.Button,
                            ) { onSelectTrack(track) },
                        )
                        HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.45f))
                    }
                }
            }
            if (drag != null) {
                PlaybackQueueRow(
                    drag.track,
                    drag.artwork,
                    showHandle = true,
                    actions = emptyList(),
                    modifier = Modifier
                        .offset {
                            IntOffset(0, (drag.top - listState.layoutInfo.viewportStartOffset).roundToInt())
                        }
                        .shadow(5.dp, RoundedCornerShape(12.dp))
                        .background(colors.surfaceContainerHighest, RoundedCornerShape(12.dp))
                        .clearAndSetSemantics {},
                )
            }
            if (reorderEnabled && upcoming.size > 1) {
                Box(
                    Modifier.align(Alignment.TopEnd).width(44.dp).fillMaxHeight().pointerInput(listState) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            if (!canReorder || activeDrag != null) return@awaitEachGesture
                            val layout = listState.layoutInfo
                            val y = down.position.y + layout.viewportStartOffset
                            val item = layout.visibleItemsInfo.firstOrNull {
                                y >= it.offset && y < it.offset + it.size
                            } ?: return@awaitEachGesture
                            val original = currentUpcoming
                            val track = original.getOrNull(item.index)?.takeIf { it.libraryId == item.key }
                                ?: return@awaitEachGesture
                            val start = awaitVerticalTouchSlopOrCancellation(down.id) { change, _ ->
                                change.consume()
                            } ?: return@awaitEachGesture
                            if (!canReorder || activeDrag != null || currentUpcoming != original) return@awaitEachGesture
                            val session = PlaybackQueueDrag(
                                track,
                                visibleArtwork[track.libraryId],
                                original,
                                item.size,
                                item.index,
                                item.offset + start.position.y - down.position.y,
                            )
                            activeDrag = session
                            try {
                                updatePreview(session)
                                val finished = verticalDrag(start.id) { change ->
                                    val delta = change.positionChange().y
                                    change.consume()
                                    if (activeDrag === session) {
                                        session.top += delta
                                        updatePreview(session)
                                    }
                                }
                                if (finished && activeDrag === session && canReorder && currentUpcoming == original) {
                                    val target = original.getOrNull(session.previewIndex)
                                    if (target != null && target.libraryId != session.trackId) {
                                        currentOnMove(session.trackId, target.libraryId)
                                    }
                                }
                            } finally {
                                if (activeDrag === session) activeDrag = null
                            }
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun PlaybackQueueRow(
    track: Track,
    artwork: Painter?,
    showHandle: Boolean,
    actions: List<CustomAccessibilityAction>,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier.fillMaxWidth().padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlaybackCover(track, 44.dp, 8.dp, announce = false, painter = artwork)
        PlaybackTrackText(track, track.playbackArtist, Modifier.weight(1f), compact = true)
        if (showHandle) {
            Icon(
                Icons.Filled.DragHandle,
                contentDescription = "调整${track.title}的播放顺序",
                tint = colors.onSurfaceVariant,
                modifier = Modifier.size(44.dp).semantics { customActions = actions }.padding(10.dp),
            )
        }
    }
}

@Composable
private fun PlaybackTrackText(
    track: Track,
    subtitle: String,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    Column(modifier) {
        Text(
            track.title,
            style = if (compact) MaterialTheme.typography.titleSmall else MaterialTheme.typography.headlineSmall,
            maxLines = if (compact) 1 else 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (subtitle.isNotBlank()) {
            Text(
                subtitle,
                style = if (compact) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun PlaybackCover(
    track: Track,
    size: Dp,
    radius: Dp,
    announce: Boolean,
    modifier: Modifier = Modifier,
    painter: Painter? = null,
) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier.size(size).clip(RoundedCornerShape(radius)).background(colors.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (painter != null) {
            Image(
                painter,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else if (track.coverUrl.isNotBlank()) {
            AsyncImage(
                model = track.coverUrl,
                contentDescription = if (announce) "${track.title} 封面" else null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Icon(
                Icons.Filled.MusicNote,
                contentDescription = null,
                modifier = Modifier.size(minOf(size * 0.5f, 72.dp)),
                tint = colors.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PlaybackControls(content: @Composable () -> Unit) {
    Layout(content = content, modifier = Modifier.fillMaxWidth()) { measurables, constraints ->
        val loose = constraints.copy(minHeight = 0)
        val (progress, transport, volume) = measurables.map { it.measure(loose) }
        val volumeTop = progress.height + transport.height + 2 * PlaybackControlSpacing.roundToPx()
        val timelineCenter = progress[PlaybackTimelineCenter]
        val volumeCenter = volumeTop + volume.height / 2f
        val transportTop = ((timelineCenter + volumeCenter - transport.height) / 2f).roundToInt()

        layout(maxOf(progress.width, transport.width, volume.width), volumeTop + volume.height) {
            progress.placeRelative(0, 0)
            transport.placeRelative(0, transportTop)
            volume.placeRelative(0, volumeTop)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaybackProgress(
    trackId: String,
    position: Long,
    duration: Long,
    enabled: Boolean,
    accentColor: Color,
    onSeek: (Long) -> Unit,
) {
    var previewFraction by remember(trackId, duration, enabled) { mutableStateOf<Float?>(null) }
    val playedFraction = if (duration > 0L) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f
    val displayPosition = previewFraction?.let { (duration * it).roundToLong() } ?: position
    val colors = SliderDefaults.colors(
        thumbColor = accentColor,
        activeTrackColor = accentColor,
        inactiveTrackColor = accentColor.copy(alpha = 0.24f),
    )
    val timeColor = MaterialTheme.colorScheme.onSurfaceVariant

    Layout(
        modifier = Modifier.fillMaxWidth(),
        content = {
            Slider(
                value = previewFraction ?: playedFraction,
                onValueChange = { previewFraction = it },
                onValueChangeFinished = {
                    previewFraction?.let { onSeek((duration * it).roundToLong()) }
                    previewFraction = null
                },
                enabled = enabled,
                modifier = Modifier.semantics { contentDescription = "播放进度" },
                colors = colors,
                thumb = { PlaybackSliderThumb(if (enabled) colors.thumbColor else colors.disabledThumbColor) },
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(formatPlaybackTime(displayPosition), color = timeColor)
                Text(
                    "-${formatPlaybackTime((duration - displayPosition).coerceAtLeast(0L))}",
                    color = timeColor,
                )
            }
        },
    ) { measurables, constraints ->
        val loose = constraints.copy(minHeight = 0)
        val slider = measurables[0].measure(loose)
        val times = measurables[1].measure(loose)
        layout(
            width = maxOf(slider.width, times.width),
            height = slider.height + times.height,
            alignmentLines = mapOf(PlaybackTimelineCenter to slider.height / 2),
        ) {
            slider.placeRelative(0, 0)
            times.placeRelative(0, slider.height)
        }
    }
}

@Composable
private fun PlaybackSliderThumb(color: Color) {
    Box(
        Modifier.size(width = 4.dp, height = 44.dp)
            .background(color, RoundedCornerShape(2.dp)),
    )
}

private fun formatPlaybackTime(milliseconds: Long): String {
    val totalSeconds = milliseconds.coerceAtLeast(0L) / 1_000L
    return "%d:%02d".format(totalSeconds / 60L, totalSeconds % 60L)
}
