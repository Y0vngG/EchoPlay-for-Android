package com.echoplay.android

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import kotlin.math.roundToInt

private val PlaylistCoverBounds = BoundsTransform { _, _ ->
    tween(210, easing = FastOutSlowInEasing)
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun PlaylistPageTransition(
    detailId: String?,
    content: @Composable (String?, @Composable (String) -> Modifier, Modifier) -> Unit,
) {
    SharedTransitionLayout(Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = detailId,
            modifier = Modifier.fillMaxSize(),
            transitionSpec = {
                (fadeIn(tween(100, delayMillis = 63)) togetherWith fadeOut(tween(63))).using(null)
            },
            label = "Playlist page",
        ) { visibleDetailId ->
            val visibilityScope = this
            val coverModifier: @Composable (String) -> Modifier = { playlistId ->
                Modifier.sharedBounds(
                    sharedContentState = rememberSharedContentState("playlist-cover-$playlistId"),
                    animatedVisibilityScope = visibilityScope,
                    enter = fadeIn(tween(100, delayMillis = 55)),
                    exit = fadeOut(tween(130, delayMillis = 50)),
                    boundsTransform = PlaylistCoverBounds,
                    resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds(),
                    zIndexInOverlay = 1f,
                )
            }
            val revealTracks = Modifier.animateEnterExit(
                enter = fadeIn(tween(100, delayMillis = 75)) +
                    slideInVertically(tween(120, delayMillis = 67, easing = FastOutSlowInEasing)) { it / 5 },
                exit = fadeOut(tween(55)),
            )
            Box(
                Modifier.fillMaxSize().then(
                    if (visibleDetailId == detailId) Modifier else Modifier.clearAndSetSemantics {},
                ),
            ) {
                content(visibleDetailId, coverModifier, revealTracks)
            }
        }
    }
}

@Composable
private fun LibraryOverlayTheme(accentColor: Color, content: @Composable () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val overlayAccent = if (colors.surface.luminance() < 0.5f) {
        lerp(accentColor, colors.onSurface, 0.28f)
    } else {
        accentColor
    }
    MaterialTheme(colorScheme = colors.copy(primary = overlayAccent), content = content)
}

@Composable
private fun useLargerPhoneActions(): Boolean {
    val size = LocalWindowInfo.current.containerDpSize
    return size.width > 0.dp && size.width < 600.dp && size.height >= size.width * 2.15f
}

@Composable
internal fun LibraryDialog(
    accentColor: Color,
    onDismissRequest: () -> Unit,
    title: @Composable () -> Unit,
    text: @Composable () -> Unit,
    confirmLabel: String,
    onConfirm: () -> Unit,
    confirmEnabled: Boolean = true,
    dismissEnabled: Boolean = true,
) {
    val largerActions = useLargerPhoneActions()
    val dismiss by rememberUpdatedState(onDismissRequest)
    val panelBounds = remember { mutableStateOf(Rect.Zero) }

    LibraryOverlayTheme(accentColor) {
        AlertDialog(
            onDismissRequest = onDismissRequest,
            properties = DialogProperties(
                dismissOnBackPress = dismissEnabled,
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false,
            ),
            modifier = Modifier.fillMaxSize().safeDrawingPadding()
                .pointerInput(dismissEnabled) {
                    if (dismissEnabled) {
                        detectTapGestures { position ->
                            if (position !in panelBounds.value) dismiss()
                        }
                    }
                }
                .layout { measurable, constraints ->
                    val width = minOf((constraints.maxWidth * 0.92f).roundToInt(), 480.dp.roundToPx())
                    val maxHeight = (constraints.maxHeight - 16.dp.roundToPx()).coerceAtLeast(0)
                    val panel = measurable.measure(
                        constraints.copy(minWidth = width, maxWidth = width, minHeight = 0, maxHeight = maxHeight),
                    )
                    val left = (constraints.maxWidth - panel.width) / 2
                    val top = (constraints.maxHeight - panel.height) / 2
                    layout(constraints.maxWidth, constraints.maxHeight) {
                        panelBounds.value = Rect(
                            left.toFloat(), top.toFloat(), (left + panel.width).toFloat(), (top + panel.height).toFloat(),
                        )
                        panel.place(left, top)
                    }
                },
            title = title,
            text = {
                Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) { text() }
            },
            confirmButton = {
                LibraryDialogButton(confirmLabel, largerActions, confirmEnabled, onConfirm)
            },
            dismissButton = {
                LibraryDialogButton("取消", largerActions, dismissEnabled, onDismissRequest)
            },
        )
    }
}

@Composable
private fun LibraryDialogButton(label: String, larger: Boolean, enabled: Boolean, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        modifier = if (larger) Modifier.sizeIn(minWidth = 88.dp, minHeight = 56.dp) else Modifier,
    ) {
        val style = LocalTextStyle.current
        Text(label, style = if (larger) style.copy(fontSize = 18.sp, lineHeight = 24.sp) else style)
    }
}

@Composable
internal fun PlaylistNameDialog(
    playlist: LibraryPlaylist?,
    accentColor: Color,
    saving: Boolean,
    error: String?,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable(playlist?.id) { mutableStateOf(playlist?.name.orEmpty()) }
    LibraryDialog(
        accentColor = accentColor,
        onDismissRequest = onDismiss,
        title = { Text(if (playlist == null) "新建播放列表" else "重命名播放列表") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("播放列表名称") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !saving,
                isError = error != null,
                supportingText = error?.let { { Text(it) } },
            )
        },
        confirmLabel = if (saving) "保存中…" else "保存",
        onConfirm = { onSave(name) },
        confirmEnabled = !saving && name.isNotBlank(),
        dismissEnabled = !saving,
    )
}

@Composable
internal fun PlaylistRow(
    name: String,
    tracks: List<Track>,
    accentColor: Color,
    onOpen: () -> Unit,
    onPlay: () -> Unit,
    artworkModifier: Modifier = Modifier,
) {
    Row(
        Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onOpen).padding(vertical = 9.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlaylistArtwork(tracks.firstOrNull { it.coverUrl.isNotBlank() }?.coverUrl, accentColor, artworkModifier.size(56.dp), iconSize = 26)
        PlaylistItemText(name, "${tracks.size} 首歌曲", Modifier.weight(1f))
        IconButton(
            onClick = onPlay,
            enabled = tracks.isNotEmpty(),
            colors = IconButtonDefaults.iconButtonColors(contentColor = accentColor),
        ) {
            Icon(Icons.Filled.PlayArrow, contentDescription = "播放$name")
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
    }
    HorizontalDivider()
}

@Composable
internal fun PlaylistDetails(
    name: String,
    tracks: List<Track>,
    candidates: List<Track>,
    accentColor: Color,
    isCollection: Boolean,
    saving: Boolean,
    onBack: () -> Unit,
    onPlay: () -> Unit,
    onShare: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onChooseSongs: () -> Unit,
    onAddSong: (Track) -> Unit,
    onPlaySuggestion: (Track) -> Unit,
    error: String?,
    artworkModifier: Modifier = Modifier,
    tracksModifier: Modifier = Modifier,
    trackContent: @Composable (Track) -> Unit,
) {
    var search by rememberSaveable { mutableStateOf("") }
    val query = search.trim()
    val searching = query.isNotEmpty()
    val visibleTracks = remember(tracks, query) {
        if (searching) tracks.filter { it.matchesLibrarySearch(query) } else tracks
    }
    val browseState = rememberLazyListState()
    val listState = if (searching) key(query) { rememberLazyListState() } else browseState
    var recommendationOffset by rememberSaveable { mutableIntStateOf(0) }
    val recommendations = remember(candidates, recommendationOffset) {
        if (candidates.size <= 3) candidates else {
            List(3) { candidates[(recommendationOffset + it) % candidates.size] }
        }
    }
    PullDownSearchLayout(
        filter = search,
        onFilterChange = { search = it },
        accentColor = accentColor,
        searchLabel = "搜索歌单歌曲",
        accessibilityLabel = "歌单搜索",
        enabled = !isCollection,
        header = { titleModifier ->
            PlaylistToolbar(
                title = if (isCollection) "收藏合集" else "播放列表",
                onBack = onBack,
                titleModifier = titleModifier,
            ) {
                IconButton(onClick = onShare) {
                    Icon(Icons.Filled.Share, contentDescription = "分享歌单")
                }
                PlaylistMenu(name, saving, onRename, onChooseSongs.takeUnless { isCollection }, onDelete)
            }
        },
    ) {
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            state = listState,
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            if (!searching) {
                item(key = "playlist-cover") {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        PlaylistArtwork(
                            tracks.firstOrNull { it.coverUrl.isNotBlank() }?.coverUrl,
                            accentColor,
                            artworkModifier.widthIn(max = 248.dp).fillMaxWidth().aspectRatio(1f),
                            iconSize = 74,
                        )
                        Text(
                            name,
                            style = MaterialTheme.typography.headlineMedium,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            textAlign = TextAlign.Center,
                        )
                        Text("${tracks.size} 首歌曲", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (tracks.isNotEmpty()) {
                            Button(
                                onClick = onPlay,
                                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                            ) {
                                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                                Text("播放全部")
                            }
                        }
                    }
                }
            } else if (visibleTracks.isEmpty()) {
                item { LibraryEmptyMessage("歌单里没有匹配的歌曲。") }
            }
            items(visibleTracks, key = { "playlist-song-${it.libraryId}" }) { track ->
                Box(Modifier.fillMaxWidth().then(tracksModifier)) { trackContent(track) }
            }
            if (!isCollection && !searching) {
                item(key = "choose-songs") {
                    Row(
                        modifier = Modifier.fillMaxWidth().then(tracksModifier)
                            .clickable(enabled = !saving, role = Role.Button, onClick = onChooseSongs)
                            .padding(vertical = 13.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Icon(Icons.Filled.AddCircleOutline, contentDescription = null, tint = accentColor, modifier = Modifier.size(30.dp))
                        Text("添加音乐", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                    }
                }
                if (recommendations.isNotEmpty()) {
                    item(key = "playlist-suggestions") {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth().then(tracksModifier).padding(top = 10.dp),
                        ) {
                            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text("建议歌曲", style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            "从曲库试听或添加",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    if (candidates.size > recommendations.size) {
                                        IconButton(onClick = { recommendationOffset = (recommendationOffset + 3) % candidates.size }) {
                                            Icon(Icons.Filled.Refresh, contentDescription = "换一批建议")
                                        }
                                    }
                                }
                                recommendations.forEach { track ->
                                    PlaylistTrackItem(track, accentColor, onClick = { onPlaySuggestion(track) }) {
                                        IconButton(onClick = { onAddSong(track) }, enabled = !saving) {
                                            Icon(Icons.Filled.AddCircleOutline, contentDescription = "添加${track.title}", tint = accentColor)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun PlaylistSongPicker(
    playlist: LibraryPlaylist,
    tracks: List<Track>,
    accentColor: Color,
    saving: Boolean,
    error: String?,
    onBack: () -> Unit,
    onSave: (List<String>) -> Unit,
) {
    var search by rememberSaveable(playlist.id) { mutableStateOf("") }
    var selection by rememberSaveable(
        playlist.id,
        stateSaver = listSaver<List<String>, String>(save = { it }, restore = { it }),
    ) { mutableStateOf(playlist.trackIds) }
    val tracksById = remember(tracks) { tracks.associateBy { it.libraryId } }
    val query = search.trim()
    val matches = remember(tracks, query) {
        if (query.isEmpty()) tracks else tracks.filter {
            it.title.contains(query, ignoreCase = true) || it.artist.orEmpty().contains(query, ignoreCase = true)
        }
    }
    val selectedIds = remember(selection) { selection.toHashSet() }
    val selectedTracks = remember(selection, tracksById) { selection.filter { it in tracksById } }
    Surface(Modifier.fillMaxSize()) {
        Column(Modifier.safeDrawingPadding().imePadding().padding(horizontal = 16.dp)) {
            PlaylistToolbar(title = "歌曲", onBack = onBack, backEnabled = !saving) {
                FilledIconButton(
                    onClick = { onSave(selectedTracks) },
                    enabled = !saving,
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = accentColor, contentColor = Color.White),
                ) {
                    Icon(Icons.Filled.Check, contentDescription = if (saving) "保存中…" else "完成")
                }
            }
            LibraryOverlayTheme(accentColor) {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    label = { Text("搜索曲库歌曲") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !saving,
                )
            }
            Text(
                "已选 ${selectedTracks.size} 首 · ${playlist.name}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp, bottom = 5.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            LazyColumn(Modifier.fillMaxWidth().weight(1f)) {
                if (matches.isEmpty()) {
                    item {
                        Text(
                            if (tracks.isEmpty()) "曲库暂无歌曲。" else "没有匹配的歌曲。",
                            modifier = Modifier.padding(vertical = 16.dp),
                        )
                    }
                }
                items(matches, key = { it.libraryId }) { track ->
                    val selected = track.libraryId in selectedIds
                    PlaylistTrackItem(
                        track,
                        accentColor,
                        modifier = Modifier.toggleable(selected, enabled = !saving, role = Role.Checkbox) { checked ->
                            selection = if (checked) selection + track.libraryId else selection - track.libraryId
                        },
                    ) {
                        Icon(
                            if (selected) Icons.Filled.CheckCircle else Icons.Filled.AddCircleOutline,
                            contentDescription = null,
                            tint = if (selected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 11.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun PlaylistTrackItem(
    track: Track,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    trailing: @Composable () -> Unit = {},
) {
    Row(
        modifier = modifier.fillMaxWidth()
            .then(if (onClick == null) Modifier else Modifier.clickable(role = Role.Button, onClick = onClick))
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlaylistArtwork(track.coverUrl, accentColor, Modifier.size(46.dp), iconSize = 22, isPlaylist = false)
        PlaylistItemText(track.title, track.artist.orEmpty(), Modifier.weight(1f))
        trailing()
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f))
}

@Composable
private fun PlaylistToolbar(
    title: String,
    onBack: () -> Unit,
    backEnabled: Boolean = true,
    titleModifier: Modifier = Modifier,
    actions: @Composable () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        IconButton(onClick = onBack, enabled = backEnabled) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
        }
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f).then(titleModifier))
        actions()
    }
}

@Composable
private fun PlaylistMenu(
    name: String,
    saving: Boolean,
    onRename: () -> Unit,
    onEditSongs: (() -> Unit)?,
    onDelete: () -> Unit,
) {
    val largerActions = useLargerPhoneActions()
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(
            onClick = { expanded = true },
            enabled = !saving,
            modifier = if (largerActions) Modifier.size(56.dp) else Modifier,
        ) {
            Icon(
                Icons.Filled.MoreVert,
                contentDescription = "${name}的更多操作",
                modifier = Modifier.size(if (largerActions) 30.dp else 24.dp),
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = if (largerActions) Modifier.widthIn(min = 164.dp) else Modifier,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            PlaylistMenuItem("重命名", largerActions) {
                expanded = false
                onRename()
            }
            if (onEditSongs != null) {
                PlaylistMenuItem("编辑歌曲", largerActions) {
                    expanded = false
                    onEditSongs()
                }
            }
            PlaylistMenuItem("删除", largerActions) {
                expanded = false
                onDelete()
            }
        }
    }
}

@Composable
private fun PlaylistMenuItem(label: String, larger: Boolean, onClick: () -> Unit) {
    DropdownMenuItem(
        onClick = onClick,
        modifier = if (larger) Modifier.sizeIn(minHeight = 56.dp) else Modifier,
        text = {
            val style = LocalTextStyle.current
            Text(label, style = if (larger) style.copy(fontSize = 18.sp, lineHeight = 24.sp) else style)
        },
    )
}

@Composable
private fun PlaylistItemText(title: String, subtitle: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
}

@Composable
private fun PlaylistArtwork(
    coverUrl: String?,
    accentColor: Color,
    modifier: Modifier,
    iconSize: Int,
    isPlaylist: Boolean = true,
) {
    Box(
        modifier.clip(RoundedCornerShape(if (iconSize > 30) 24.dp else 11.dp))
            .background(Brush.linearGradient(listOf(accentColor.copy(alpha = 0.24f), MaterialTheme.colorScheme.surfaceVariant))),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            if (isPlaylist) Icons.AutoMirrored.Filled.QueueMusic else Icons.Filled.MusicNote,
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier.size(iconSize.dp),
        )
        if (!coverUrl.isNullOrBlank()) {
            AsyncImage(coverUrl, contentDescription = null, modifier = Modifier.matchParentSize(), contentScale = ContentScale.Crop)
        }
    }
}
