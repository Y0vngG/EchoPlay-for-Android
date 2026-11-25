package com.echoplay.android

data class Track(
    val bvid: String,
    val title: String,
    val author: String,
    val duration: String,
    val coverUrl: String,
    val pageUrl: String,
    val pageNumber: Int = 1,
    val partTitle: String = "",
    val videoTitle: String = title,
    val libraryEntryId: String = "",
    val collectionId: String? = null,
    val artist: String? = null,
)

val Track.libraryId: String
    get() = libraryEntryId.ifBlank { "$bvid:$pageNumber" }

internal val Track.playbackArtist: String
    get() = artist ?: author
