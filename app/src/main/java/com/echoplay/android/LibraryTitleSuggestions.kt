package com.echoplay.android

internal data class LibraryTitleSuggestions(
    val title: String,
    val artist: String,
    val phrases: List<String>,
)

private val bracketedText = Regex("""\[[^\[\]]*\]|【[^【】]*】|\([^()]*\)|（[^（）]*）""")
private const val listeningPromo = """(?:[数千百十\d]+万级)?装备试听|(?:耳机|音响|无损|高音质)试听"""
private val audioQuality = Regex(
    """\b(?:hi[-\s]?res|flac|alac|wav|dsd|\d+(?:\.\d+)?\s*(?:khz|hz|bits?|kbps)|[48]k)\b|无损|高音质|$listeningPromo""",
    RegexOption.IGNORE_CASE,
)
private val leadingPromo = Regex("""^(?:$listeningPromo)(?:\s*[:：]\s*|\s+)""")
private val titleSeparator = Regex(
    """\s*[|｜／—]+\s*|\s+[-–−－/·•]+\s*|\s*[-–−－/·•]+\s+|(?<=[\p{IsHan}])[-–−－](?=[\p{IsHan}])""",
)
private val quotedTitle = Regex("""《([^《》]+)》|「([^「」]+)」|『([^『』]+)』""")
private val titleLabel = Regex(
    """^(?:歌曲名称|歌曲名|曲名|song|title)\s*[:：]\s*(.+)$""",
    RegexOption.IGNORE_CASE,
)
private val artistLabel = Regex(
    """^(?:歌手|演唱|原唱|artist|vocals?)\s*[:：]\s*(.+)$""",
    RegexOption.IGNORE_CASE,
)
private val whitespace = Regex("""\s+""")

private fun MatchResult.titlePhrase(): String = groupValues.drop(1).first(String::isNotEmpty).trim()

private fun normalizedSuggestionPhrases(phrases: List<String>): List<String> = phrases.map(String::trim)
    .filter { it.isNotEmpty() && !audioQuality.matches(it) }
    .distinctBy(String::lowercase)

private fun separatedSuggestionPhrases(phrases: List<String>): List<String> = phrases
    .flatMap { it.split('.', '【', '】', '“', '”', '-') }
    .map(String::trim)
    .filter(String::isNotEmpty)

internal fun libraryAddPhrases(phrases: List<String>): List<String> {
    val separated = separatedSuggestionPhrases(phrases)
    val words = separated.flatMap { it.split(' ') }
    return normalizedSuggestionPhrases(phrases + separated + words)
}

internal enum class LibraryArtistInput { Default, Manual, Suggestion }

internal fun collectionTracksForLibrary(
    tracks: List<Track>,
    collectionTitle: String,
    firstTitle: String,
    firstArtist: String,
    applyTitleSuggestion: Boolean,
    artistInput: LibraryArtistInput,
): List<Track> {
    val firstTrack = tracks.firstOrNull() ?: return emptyList()
    val inferPartPhrases = applyTitleSuggestion || artistInput == LibraryArtistInput.Suggestion
    val sample = if (inferPartPhrases) suggestLibraryTitle(firstTrack.title).phrases else emptyList()

    return tracks.mapIndexed { index, track ->
        if (index == 0) {
            track.copy(title = firstTitle, artist = firstArtist, videoTitle = collectionTitle)
        } else {
            val phrases = if (inferPartPhrases) suggestLibraryTitle(track.title).phrases else emptyList()
            val title = if (applyTitleSuggestion) correspondingSuggestion(sample, phrases, firstTitle) else null
            val artist = when (artistInput) {
                LibraryArtistInput.Default -> null
                LibraryArtistInput.Manual -> firstArtist.takeIf(String::isNotBlank)
                LibraryArtistInput.Suggestion -> correspondingSuggestion(sample, phrases, firstArtist) ?: firstArtist
            }
            track.copy(title = title ?: track.title, artist = artist ?: track.artist, videoTitle = collectionTitle)
        }
    }
}

private fun correspondingSuggestion(sample: List<String>, phrases: List<String>, suggestion: String): String? {
    val phraseIndex = sample.indexOfFirst { it.equals(suggestion, ignoreCase = true) }
    if (phraseIndex >= 0 && phrases.size == sample.size) return phrases[phraseIndex]

    val sampleSegments = separatedSuggestionPhrases(sample)
    val segments = separatedSuggestionPhrases(phrases)
    if (segments.size != sampleSegments.size) return null

    val segmentIndex = sampleSegments.indexOfFirst { it.equals(suggestion, ignoreCase = true) }
    if (segmentIndex >= 0) return segments[segmentIndex]

    // A word at the edge of a matching phrase can stand for a longer title in another part.
    for ((index, segment) in sampleSegments.withIndex()) {
        val words = segment.split(' ')
        val wordIndex = words.indexOfFirst { it.equals(suggestion, ignoreCase = true) }
        if (wordIndex < 0) continue

        val prefix = words.take(wordIndex).joinToString(" ", postfix = if (wordIndex > 0) " " else "")
        val suffix = words.drop(wordIndex + 1).joinToString(" ", prefix = if (wordIndex < words.lastIndex) " " else "")
        val target = segments[index]
        if (target.length > prefix.length + suffix.length &&
            target.startsWith(prefix, ignoreCase = true) && target.endsWith(suffix, ignoreCase = true)
        ) {
            return target.substring(prefix.length, target.length - suffix.length).trim()
        }
        val targetWords = target.split(' ')
        if (targetWords.size == words.size) return targetWords[wordIndex]
    }
    return null
}

internal fun suggestLibraryTitle(videoTitle: String): LibraryTitleSuggestions {
    val cleaned = videoTitle.replace(bracketedText) { match ->
        if (audioQuality.containsMatchIn(match.value)) " " else match.value
    }.replace(whitespace, " ").trim().replace(leadingPromo, "")
    val quotes = quotedTitle.findAll(cleaned).toList()
    val protectedRanges = (quotes.asSequence() + bracketedText.findAll(cleaned))
        .map { it.range }
        .toList()
    val parts = buildList {
        var start = 0
        for (separator in titleSeparator.findAll(cleaned)) {
            if (protectedRanges.any { separator.range.first in it }) continue
            add(cleaned.substring(start, separator.range.first).trim())
            start = separator.range.last + 1
        }
        add(cleaned.substring(start).trim())
    }.filter(String::isNotEmpty)
    val namedTitle = parts.firstNotNullOfOrNull { titleLabel.matchEntire(it)?.groupValues?.get(1) }
    val namedArtist = parts.firstNotNullOfOrNull { artistLabel.matchEntire(it)?.groupValues?.get(1) }
    val quotedPhrases = quotes.map { it.titlePhrase() }.filter(String::isNotBlank)
    val phrases = parts.flatMap { part ->
        val explicitValue = titleLabel.matchEntire(part)?.groupValues?.get(1)
            ?: artistLabel.matchEntire(part)?.groupValues?.get(1)
        if (explicitValue != null) {
            listOf(quotedTitle.matchEntire(explicitValue)?.titlePhrase() ?: explicitValue)
        } else {
            part.split(quotedTitle)
        }
    }

    return LibraryTitleSuggestions(
        // A separator alone does not indicate whether the uploader wrote song or artist first.
        title = namedTitle?.let { quotedTitle.matchEntire(it)?.titlePhrase() ?: it }
            ?: quotedPhrases.singleOrNull()
            ?: cleaned.ifBlank { videoTitle.trim() },
        artist = namedArtist.orEmpty(),
        phrases = normalizedSuggestionPhrases(quotedPhrases + phrases),
    )
}
