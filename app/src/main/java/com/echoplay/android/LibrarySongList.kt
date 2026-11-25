package com.echoplay.android

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow

private val SongIndexWidth = 34.dp

@Composable
internal fun LibrarySongList(
    sections: Map<String, List<Track>>,
    accentColor: Color,
    emptyMessage: String,
    onIndexGestureChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    songContent: @Composable (Track) -> Unit,
) {
    val listState = rememberLazyListState()
    val initials = remember(sections) { sections.keys.toList() }
    val sectionStarts = remember(sections) {
        var nextIndex = 0
        sections.values.map { songs ->
            val start = nextIndex
            nextIndex += songs.size + 1 // The section heading occupies one list item.
            start
        }
    }
    val scrollRequests = remember(sectionStarts) { Channel<Int>(Channel.CONFLATED) }
    var activeInitial by remember(initials) { mutableStateOf<String?>(null) }

    LaunchedEffect(listState, scrollRequests) {
        scrollRequests.receiveAsFlow().collectLatest { listState.animateScrollToItem(it) }
    }

    Box(modifier) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(end = if (initials.isEmpty()) 16.dp else SongIndexWidth),
            state = listState,
        ) {
            if (sections.isEmpty()) item { LibraryEmptyMessage(emptyMessage) }
            sections.forEach { (initial, songs) ->
                item(key = "library-initial-$initial") {
                    Text(
                        text = initial,
                        modifier = Modifier.semantics { heading() }
                            .padding(start = 6.dp, top = 12.dp, bottom = 4.dp)
                            .background(
                                if (activeInitial == initial) accentColor.copy(alpha = 0.16f) else Color.Transparent,
                                CircleShape,
                            ).padding(horizontal = 6.dp),
                        color = accentColor,
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
                items(songs, key = { it.libraryId }) { songContent(it) }
            }
        }
        if (initials.isNotEmpty()) {
            SongInitialIndex(
                initials = initials,
                accentColor = accentColor,
                onGestureChange = { dragging ->
                    if (!dragging) activeInitial = null
                    onIndexGestureChange(dragging)
                },
                onSelect = { index, dragging ->
                    activeInitial = initials[index].takeIf { dragging }
                    scrollRequests.trySend(sectionStarts[index])
                },
                modifier = Modifier.matchParentSize(),
            )
        }
    }
}

@Composable
private fun SongInitialIndex(
    initials: List<String>,
    accentColor: Color,
    onGestureChange: (Boolean) -> Unit,
    onSelect: (Int, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedIndex by remember(initials) { mutableIntStateOf(0) }
    var dragging by remember(initials) { mutableStateOf(false) }
    var previewVisible by remember(initials) { mutableStateOf(false) }
    val currentOnSelect by rememberUpdatedState(onSelect)
    val currentOnGestureChange by rememberUpdatedState(onGestureChange)
    val indexColor = lerp(accentColor, MaterialTheme.colorScheme.onSurface, 0.2f)

    LaunchedEffect(dragging, previewVisible, selectedIndex) {
        if (previewVisible && !dragging) {
            delay(220)
            previewVisible = false
        }
    }

    fun select(index: Int, dragging: Boolean) {
        selectedIndex = index
        previewVisible = true
        currentOnSelect(index, dragging)
    }

    BoxWithConstraints(modifier) {
        val slotHeight = minOf(18.dp, maxHeight / initials.size)
        val railHeight = slotHeight * initials.size
        val labelStyle = MaterialTheme.typography.labelSmall
        val fontSize = with(LocalDensity.current) {
            minOf(labelStyle.fontSize.toDp(), slotHeight * 0.82f).toSp()
        }
        val bubbleSize = minOf(46.dp, maxHeight)
        val bubbleLimit = (maxHeight - bubbleSize) / 2
        val bubbleOffset = (slotHeight * (selectedIndex + 0.5f) - railHeight / 2)
            .coerceIn(-bubbleLimit, bubbleLimit)

        AnimatedVisibility(
            visible = previewVisible,
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = SongIndexWidth + 8.dp)
                .offset(y = bubbleOffset),
            enter = fadeIn(tween(110)) + scaleIn(tween(140), initialScale = 0.85f),
            exit = fadeOut(tween(150)),
        ) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant) {
                Box(Modifier.size(bubbleSize), contentAlignment = Alignment.Center) {
                    Text(initials[selectedIndex], color = indexColor, style = MaterialTheme.typography.titleLarge)
                }
            }
        }

        Column(
            modifier = Modifier.align(Alignment.CenterEnd).width(SongIndexWidth).height(railHeight)
                .pointerInput(initials) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        down.consume()
                        dragging = true
                        currentOnGestureChange(true)
                        var previousIndex = -1

                        fun selectAt(y: Float) {
                            val index = (y * initials.size / size.height).toInt().coerceIn(0, initials.lastIndex)
                            if (index == previousIndex) return
                            previousIndex = index
                            select(index, true)
                        }

                        try {
                            selectAt(down.position.y)
                            do {
                                val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                                if (change.pressed) selectAt(change.position.y)
                                change.consume()
                            } while (change.pressed)
                        } finally {
                            dragging = false
                            currentOnGestureChange(false)
                        }
                    }
                },
        ) {
            initials.forEachIndexed { index, initial ->
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f).semantics(mergeDescendants = true) {
                        contentDescription = "跳到 $initial"
                        role = Role.Button
                        onClick {
                            select(index, false)
                            true
                        }
                    },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = initial,
                        color = indexColor,
                        fontSize = fontSize,
                        maxLines = 1,
                        style = labelStyle,
                        modifier = Modifier.background(
                            if (previewVisible && index == selectedIndex) {
                                indexColor.copy(alpha = 0.16f)
                            } else Color.Transparent,
                            CircleShape,
                        ).padding(horizontal = 5.dp),
                    )
                }
            }
        }
    }
}
