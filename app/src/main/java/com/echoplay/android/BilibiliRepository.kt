package com.echoplay.android

import java.io.IOException
import java.net.CookieHandler
import java.net.CookieManager
import java.net.CookiePolicy
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.net.URLDecoder
import java.net.URLEncoder
import java.security.MessageDigest
import org.json.JSONArray
import org.json.JSONObject

data class PlaybackStream(
    val urls: List<String>,
    val description: String,
    val headers: Map<String, String>,
)

data class BilibiliUserProfile(
    val username: String,
    val avatarUrl: String,
    val isVip: Boolean,
    val vipBadgeUrl: String,
)

enum class PlaybackQuality(val label: String, val playbackStatus: String) {
    HI_RES("Hi-Res \u65e0\u635f", "Hi-Res"),
    P1080("1080P", "1080P"),
    P360("360P", "360P"),
}

class BilibiliRepository {
    @Volatile
    private var webCookieHeader: String? = null

    fun updateWebCookies(cookieHeader: String?) {
        webCookieHeader = cookieHeader?.takeIf { cookies ->
            cookies.split(';').any { it.trim().startsWith("SESSDATA=") }
        }
    }

    fun userProfile(): BilibiliUserProfile? {
        val data = getJson(NAV_URL).optJSONObject("data") ?: return null
        if (!data.optBoolean("isLogin")) return null
        val avatar = data.optString("face").let { if (it.startsWith("//")) "https:$it" else it }
        val vipLabel = data.optJSONObject("vip_label") ?: data.optJSONObject("vip")?.optJSONObject("label")
        val vipBadge = (vipLabel?.optString("img_label_uri_hans")?.takeIf(String::isNotBlank)
            ?: vipLabel?.optString("img_label_uri_hans_static").orEmpty())
            .let { if (it.startsWith("//")) "https:$it" else it }
        return BilibiliUserProfile(
            username = data.optString("uname"),
            avatarUrl = avatar,
            isVip = data.optInt("vipStatus") == 1 && data.optInt("vipType") > 0,
            vipBadgeUrl = vipBadge,
        )
    }

    fun videoTrackFromUrl(value: String): Track {
        val input = value.trim().let {
            if (it.startsWith("www.", ignoreCase = true)) "https://$it" else it
        }
        val inputUri = runCatching { URI(input) }.getOrElse {
            error("请输入有效的 B 站视频链接。")
        }
        val host = inputUri.host?.lowercase()
            ?: error("请输入有效的 B 站视频链接。")
        if (host != "bilibili.com" && !host.endsWith(".bilibili.com") &&
            host != "b23.tv" && !host.endsWith(".b23.tv")
        ) {
            error("该链接不是 B 站视频链接。")
        }

        val resolvedUri = if (host == "b23.tv" || host.endsWith(".b23.tv")) {
            val connection = URL(input).openConnection() as HttpURLConnection
            connection.connectTimeout = TIMEOUT_MILLIS
            connection.readTimeout = TIMEOUT_MILLIS
            connection.instanceFollowRedirects = true
            connection.setRequestProperty("User-Agent", USER_AGENT)
            try {
                if (connection.responseCode !in 200..399) {
                    error("无法解析 B 站短链接（HTTP ${connection.responseCode}）。")
                }
                connection.url.toURI()
            } finally {
                connection.disconnect()
            }
        } else {
            inputUri
        }
        val resolvedHost = resolvedUri.host?.lowercase()
        if (resolvedHost != "bilibili.com" && resolvedHost != "b23.tv" &&
            resolvedHost?.endsWith(".bilibili.com") != true &&
            resolvedHost?.endsWith(".b23.tv") != true
        ) {
            error("该链接没有指向 B 站视频。")
        }

        val videoId = Regex("/video/(BV[0-9A-Za-z]{10}|av[0-9]+)", RegexOption.IGNORE_CASE)
            .find(resolvedUri.path.orEmpty())?.groupValues?.get(1)
            ?: error("链接中没有找到有效的 B 站视频编号。")
        val requestedPage = resolvedUri.rawQuery.orEmpty()
            .split('&')
            .firstNotNullOfOrNull { parameter ->
                val (key, rawValue) = parameter.split('=', limit = 2)
                    .let { it.first() to it.getOrElse(1) { "" } }
                if (URLDecoder.decode(key, Charsets.UTF_8.name()) == "p") {
                    URLDecoder.decode(rawValue, Charsets.UTF_8.name()).toIntOrNull()
                } else {
                    null
                }
            }?.takeIf { it > 0 } ?: 1

        if (videoId.startsWith("av", ignoreCase = true)) {
            val response = getJson("$VIEW_URL?aid=${videoId.substring(2)}")
            if (response.optInt("code", -1) != 0) {
                error(response.optString("message").ifBlank { "无法读取该 B 站视频。" })
            }
            val data = response.optJSONObject("data") ?: error("B 站没有返回视频信息。")
            val bvid = data.optString("bvid").takeIf(String::isNotBlank)
                ?: error("B 站没有返回有效的视频编号。")
            val owner = data.optJSONObject("owner")
            val cover = data.optString("pic").let {
                if (it.startsWith("//")) "https:$it" else it
            }
            return Track(
                bvid = bvid,
                title = unescapeTitle(data.optString("title")),
                author = owner?.optString("name").orEmpty(),
                duration = formatDuration(data.optInt("duration")),
                coverUrl = cover,
                pageUrl = "https://www.bilibili.com/video/$bvid/?p=$requestedPage",
                pageNumber = requestedPage,
            )
        }

        val bvid = videoId
        return Track(
            bvid = bvid,
            title = bvid,
            author = "",
            duration = "",
            coverUrl = "",
            pageUrl = "https://www.bilibili.com/video/$bvid/?p=$requestedPage",
            pageNumber = requestedPage,
        )
    }

    fun search(keyword: String, page: Int): List<Track> {
        require(keyword.isNotBlank()) { "请输入搜索关键词。" }
        require(page > 0) { "搜索页码无效。" }

        warmUpCookies()
        val nav = getJson(NAV_URL).optJSONObject("data")
            ?: error("B 站没有返回搜索签名信息。")
        val images = nav.optJSONObject("wbi_img")
            ?: error("B 站没有返回搜索签名信息。")
        val imageKey = images.optString("img_url").substringAfterLast('/').substringBefore('.')
        val subKey = images.optString("sub_url").substringAfterLast('/').substringBefore('.')
        if (imageKey.isEmpty() || subKey.isEmpty()) {
            error("B 站没有返回有效的搜索签名信息。")
        }

        val mixinKey = buildString {
            val source = imageKey + subKey
            for (index in MIXIN_KEY_TABLE) {
                if (index < source.length) append(source[index])
            }
        }.take(32)
        val signedValues = sortedMapOf(
            "keyword" to keyword,
            "order" to "totalrank",
            "page" to page.toString(),
            "page_size" to PAGE_SIZE.toString(),
            "search_type" to "video",
            "web_location" to "1550101",
            "wts" to (System.currentTimeMillis() / 1000L).toString(),
        )
        val canonicalQuery = signedValues.entries.joinToString("&") { (key, value) ->
            "${encode(key)}=${encode(value.replace(Regex("[!'()*]"), ""))}"
        }
        val signature = md5(canonicalQuery + mixinKey)
        val signedQuery = "$canonicalQuery&w_rid=$signature"
        val response = getJson("$SEARCH_URL?$signedQuery")
        if (response.optInt("code", -1) != 0) {
            error(response.optString("message").ifBlank { "B 站搜索失败（${response.optInt("code")}）。" })
        }

        val results = response.optJSONObject("data")?.optJSONArray("result")
            ?: return emptyList()
        return buildList {
            for (index in 0 until results.length()) {
                val item = results.optJSONObject(index) ?: continue
                val bvid = item.optString("bvid")
                if (bvid.isBlank()) continue
                val title = unescapeTitle(item.optString("title"))
                val cover = item.optString("pic").let {
                    if (it.startsWith("//")) "https:$it" else it
                }
                add(
                    Track(
                        bvid = bvid,
                        title = title,
                        author = item.optString("author"),
                        duration = item.optString("duration"),
                        coverUrl = cover,
                        pageUrl = "https://www.bilibili.com/video/$bvid",
                    ),
                )
            }
        }
    }

    fun pages(track: Track): List<Track> {
        val response = getJson("$VIEW_URL?bvid=${encode(track.bvid)}")
        if (response.optInt("code", -1) != 0) {
            error(response.optString("message").ifBlank { "无法读取该视频的分 P 信息。" })
        }
        val data = response.optJSONObject("data") ?: error("B 站没有返回视频信息。")
        val videoTitle = data.optString("title").ifBlank { track.title }
        val owner = data.optJSONObject("owner")
        val pageList = data.optJSONArray("pages") ?: return listOf(track)
        return buildList {
            for (index in 0 until pageList.length()) {
                val page = pageList.optJSONObject(index) ?: continue
                val number = page.optInt("page")
                if (number < 1) continue
                val partTitle = page.optString("part")
                val title = if (pageList.length() == 1) {
                    videoTitle
                } else {
                    partTitle.takeIf(String::isNotBlank) ?: "P$number"
                }
                add(
                    track.copy(
                        title = title,
                        author = owner?.optString("name")?.ifBlank { track.author } ?: track.author,
                        duration = formatDuration(page.optInt("duration")),
                        pageUrl = "https://www.bilibili.com/video/${track.bvid}/?p=$number",
                        pageNumber = number,
                        partTitle = partTitle,
                        videoTitle = videoTitle,
                    ),
                )
            }
        }.ifEmpty { listOf(track) }
    }

    fun audioStream(track: Track, preferredQuality: PlaybackQuality = PlaybackQuality.HI_RES): PlaybackStream {
        val view = getJson(
            "$VIEW_URL?bvid=${encode(track.bvid)}",
            referer = track.pageUrl,
        )
            .optJSONObject("data") ?: error("B 站没有返回视频信息。")
        val pages = view.optJSONArray("pages") ?: error("B 站没有返回该视频的分 P 信息。")
        val pageIndex = track.pageNumber - 1
        if (pageIndex !in 0 until pages.length()) error("该视频分 P 不存在。")
        val cid = pages.optJSONObject(pageIndex)?.optLong("cid")
            ?.takeIf { it > 0 } ?: error("B 站没有返回该视频分 P 的音频信息。")

        val navResponse = getJson(NAV_URL, referer = track.pageUrl)
        val nav = navResponse.optJSONObject("data")
            ?: error(
                navResponse.optString("message")
                    .ifBlank { "B 站没有返回音频接口签名信息（${navResponse.optInt("code")}）。" },
            )
        val isLoggedIn = nav.optBoolean("isLogin")
        val images = nav.optJSONObject("wbi_img")
            ?: error("B 站没有返回音频接口签名信息。")
        val imageKey = images.optString("img_url").substringAfterLast('/').substringBefore('.')
        val subKey = images.optString("sub_url").substringAfterLast('/').substringBefore('.')
        if (imageKey.isEmpty() || subKey.isEmpty()) {
            error("B 站没有返回有效的音频接口签名信息。")
        }
        val mixinKey = buildString {
            val source = imageKey + subKey
            for (index in MIXIN_KEY_TABLE) {
                if (index >= source.length) error("B 站音频接口签名信息格式异常。")
                append(source[index])
            }
        }.take(32)
        val audioOptions = listOf(
            Triple(4048, 0, true),
            Triple(16, 0, true),
            Triple(16, 64, true),
            Triple(16, 0, false),
            Triple(16, 64, false),
        )
        var audioTracks: JSONArray? = null
        var lastPlayApiError: String? = null
        for ((fnval, quality, signed) in audioOptions) {
            val requestValues = sortedMapOf(
                "bvid" to track.bvid,
                "cid" to cid.toString(),
                "fnval" to fnval.toString(),
                "fnver" to "0",
                "fourk" to "1",
                "qn" to quality.toString(),
            )
            if (signed) requestValues["wts"] = (System.currentTimeMillis() / 1000L).toString()
            val canonicalQuery = requestValues.entries.joinToString("&") { (key, value) ->
                "${encode(key)}=${encode(value.replace(Regex("[!'()*]"), ""))}"
            }
            val query = if (signed) {
                "$canonicalQuery&w_rid=${md5(canonicalQuery + mixinKey)}"
            } else {
                canonicalQuery
            }
            val playUrl = if (signed) PLAY_URL else LEGACY_PLAY_URL
            val response = getJson("$playUrl?$query", referer = track.pageUrl)
            if (response.optInt("code", -1) != 0) {
                lastPlayApiError = response.optString("message")
                    .ifBlank { "错误码 ${response.optInt("code")}" }
                continue
            }
            audioTracks = response.optJSONObject("dash")?.optJSONArray("audio")
                ?.takeIf { it.length() > 0 }
            if (audioTracks != null) break
        }
        val headers = mutableMapOf(
            "User-Agent" to USER_AGENT,
            "Referer" to track.pageUrl,
        )
        webCookieHeader?.let { headers["Cookie"] = it }
        val availableAudioTracks = audioTracks
        if (availableAudioTracks != null) {
            val qualityRanks = mapOf(30251 to 5, 30250 to 4, 30280 to 3, 30232 to 2, 30216 to 1)
            val rankedAudioTracks = (0 until availableAudioTracks.length())
                .mapNotNull { availableAudioTracks.optJSONObject(it) }
            val audioComparator = compareBy<JSONObject> { qualityRanks[it.optInt("id")] ?: 0 }
                .thenBy { it.optLong("bandwidth") }
                .thenBy { it.optString("codecs") }
            val selectedAudio = when (preferredQuality) {
                PlaybackQuality.P360 -> rankedAudioTracks.minWithOrNull(audioComparator)
                PlaybackQuality.P1080 -> rankedAudioTracks
                    .filterNot { it.optInt("id") == 30251 }
                    .maxWithOrNull(audioComparator)
                PlaybackQuality.HI_RES -> rankedAudioTracks
                    .filter { it.optInt("id") == 30251 || it.optInt("id") == 30250 }
                    .maxWithOrNull(audioComparator)
                    ?: rankedAudioTracks.maxWithOrNull(audioComparator)
            }
            if (selectedAudio != null) {
                val streamUrl = selectedAudio.optString("baseUrl")
                    .ifBlank { selectedAudio.optString("base_url") }
                if (streamUrl.isNotBlank()) {
                    return PlaybackStream(
                        urls = listOf(streamUrl),
                        description = preferredQuality.playbackStatus,
                        headers = headers,
                    )
                }
            }
        }

        // Audio-only playback is preferred. If Bilibili returns no DASH audio
        // track, fall back to a progressive audio/video stream at the best
        // quality the account and video allow.
        val requestedVideoQuality = if (preferredQuality == PlaybackQuality.P360) 16 else 80
        val fallbackVideoQualities = if (requestedVideoQuality == 80) {
            listOf(64, 32, 16, 6)
        } else {
            listOf(6)
        }
        val videoQualities = listOf(requestedVideoQuality) + fallbackVideoQualities
        for (videoQuality in videoQualities) {
            for (signed in listOf(true, false)) {
                val requestValues = sortedMapOf(
                    "bvid" to track.bvid,
                    "cid" to cid.toString(),
                    "fnval" to "0",
                    "fnver" to "0",
                    "fourk" to "0",
                    "platform" to "html5",
                    "qn" to videoQuality.toString(),
                )
                if (videoQuality >= 80) requestValues["high_quality"] = "1"
                if (signed) requestValues["wts"] = (System.currentTimeMillis() / 1000L).toString()
                val canonicalQuery = requestValues.entries.joinToString("&") { (key, value) ->
                    "${encode(key)}=${encode(value.replace(Regex("[!'()*]"), ""))}"
                }
                val query = if (signed) {
                    "$canonicalQuery&w_rid=${md5(canonicalQuery + mixinKey)}"
                } else {
                    canonicalQuery
                }
                val playUrl = if (signed) PLAY_URL else LEGACY_PLAY_URL
                val response = getJson("$playUrl?$query", referer = track.pageUrl)
                if (response.optInt("code", -1) != 0) {
                    lastPlayApiError = response.optString("message")
                    continue
                }
                val data = response.optJSONObject("data") ?: response
                val parts = data.optJSONArray("durl") ?: continue
                val urls = (0 until parts.length())
                    .mapNotNull { parts.optJSONObject(it)?.optString("url")?.takeIf(String::isNotBlank) }
                if (urls.isEmpty()) continue

                val description = "\u97f3\u89c6\u9891\uff08${preferredQuality.playbackStatus}\uff09"
                return PlaybackStream(urls, description, headers)
            }
        }

        if (!isLoggedIn) {
            error("B 站请求未携带有效登录状态，请重新登录后再播放。")
        }
        val details = lastPlayApiError?.let { "（播放接口：$it）" }.orEmpty()
        error("Bilibili did not provide an audio-only stream$details")
    }

    private fun getJson(url: String, referer: String = SEARCH_REFERER): JSONObject {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = TIMEOUT_MILLIS
        connection.readTimeout = TIMEOUT_MILLIS
        connection.setRequestProperty("User-Agent", USER_AGENT)
        connection.setRequestProperty("Accept", "application/json, text/plain, */*")
        connection.setRequestProperty("Referer", referer)
        connection.setRequestProperty("Origin", SEARCH_ORIGIN)
        applyWebCookies(connection)
        try {
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
            if (status !in 200..299) {
                if (status == 412) error("B 站暂时拦截了搜索请求（HTTP 412），请稍后重试。")
                error("B 站请求失败（HTTP $status）。")
            }
            return JSONObject(body ?: throw IOException("B 站返回了空响应。"))
        } finally {
            connection.disconnect()
        }
    }

    private fun warmUpCookies() {
        val connection = URL(HOME_URL).openConnection() as HttpURLConnection
        connection.connectTimeout = TIMEOUT_MILLIS
        connection.readTimeout = TIMEOUT_MILLIS
        connection.setRequestProperty("User-Agent", USER_AGENT)
        connection.setRequestProperty("Referer", HOME_URL)
        applyWebCookies(connection)
        try {
            if (connection.responseCode !in 200..299) {
                error("无法准备 B 站搜索会话（HTTP ${connection.responseCode}）。")
            }
            connection.inputStream.use { input -> input.read(ByteArray(4096)) }
        } finally {
            connection.disconnect()
        }
    }

    private fun applyWebCookies(connection: HttpURLConnection) {
        webCookieHeader?.let { connection.setRequestProperty("Cookie", it) }
    }

    private fun unescapeTitle(value: String): String = value
        .replace(SEARCH_HIGHLIGHT_TAG, "")
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&nbsp;", " ")

    private fun formatDuration(seconds: Int): String {
        val hours = seconds / 3600
        val minutes = seconds % 3600 / 60
        val remainingSeconds = seconds % 60
        return if (hours > 0) {
            "%d:%02d:%02d".format(hours, minutes, remainingSeconds)
        } else {
            "%d:%02d".format(minutes, remainingSeconds)
        }
    }

    private fun md5(value: String): String = MessageDigest.getInstance("MD5")
        .digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }

    private fun encode(value: String): String = URLEncoder.encode(value, Charsets.UTF_8.name())
        .replace("+", "%20")

    private companion object {
        val SEARCH_HIGHLIGHT_TAG = Regex("</?em[^>]*>")

        init {
            if (CookieHandler.getDefault() == null) {
                CookieHandler.setDefault(CookieManager(null, CookiePolicy.ACCEPT_ALL))
            }
        }

        const val PAGE_SIZE = 30
        const val TIMEOUT_MILLIS = 15_000
        const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/132.0.0.0 Safari/537.36"
        const val NAV_URL = "https://api.bilibili.com/x/web-interface/nav"
        const val HOME_URL = "https://www.bilibili.com/"
        const val SEARCH_REFERER = "https://search.bilibili.com/"
        const val SEARCH_ORIGIN = "https://search.bilibili.com"
        const val SEARCH_URL = "https://api.bilibili.com/x/web-interface/wbi/search/type"
        const val VIEW_URL = "https://api.bilibili.com/x/web-interface/view"
        const val PLAY_URL = "https://api.bilibili.com/x/player/wbi/playurl"
        const val LEGACY_PLAY_URL = "https://api.bilibili.com/x/player/playurl"
        val MIXIN_KEY_TABLE = intArrayOf(
            46, 47, 18, 2, 53, 8, 23, 32, 15, 50, 10, 31, 58, 3, 45, 35,
            27, 43, 5, 49, 33, 9, 42, 19, 29, 28, 14, 39, 12, 38, 41, 13,
            37, 48, 7, 16, 24, 55, 40, 61, 26, 17, 0, 1, 60, 51, 30, 4,
            22, 25, 54, 21, 56, 59, 6, 63, 57, 62, 11, 36, 20, 34, 44, 52,
        )
    }
}
