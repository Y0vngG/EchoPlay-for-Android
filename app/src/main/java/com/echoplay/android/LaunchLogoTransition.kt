package com.echoplay.android

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Outline
import android.graphics.drawable.AdaptiveIconDrawable
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.animation.PathInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.core.animation.doOnEnd
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.core.splashscreen.SplashScreenViewProvider
import androidx.core.view.WindowInsetsControllerCompat
import kotlin.math.roundToInt
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull

internal const val BADGE_ARTWORK_SCALE = 1.5f

internal enum class EchoPlayLogo(@get:DrawableRes val mark: Int, @get:DrawableRes val background: Int) {
    Light(R.drawable.echoplay_mark_light, R.drawable.echoplay_icon_background_light),
    Dark(R.drawable.echoplay_mark, R.drawable.echoplay_icon_background);

    companion object {
        fun forTheme(isDarkTheme: Boolean) = if (isDarkTheme) Dark else Light
    }
}

internal class LaunchLogoTransition(
    private val decor: ViewGroup,
    private val systemBars: WindowInsetsControllerCompat,
) {
    private var badgeBounds: Rect? = null
    private val overlayVisible = mutableStateOf(false)
    private val animationComplete = CompletableDeferred<Unit>()
    private var darkTheme = false

    val badgeModifier = Modifier.onGloballyPositioned { coordinates ->
        val position = coordinates.positionInWindow()
        badgeBounds = Rect(
            position.x,
            position.y,
            position.x + coordinates.size.width,
            position.y + coordinates.size.height,
        )
    }.graphicsLayer { alpha = if (overlayVisible.value) 0f else 1f }

    fun updateTheme(darkTheme: Boolean) {
        this.darkTheme = darkTheme
        updateSystemBars()
    }

    suspend fun awaitEnd() {
        // When Android starts without a splash window, the exit listener is never called.
        withTimeoutOrNull(1_200L) { animationComplete.await() }
        if (overlayVisible.value) animationComplete.await()
    }

    fun play(splash: SplashScreenViewProvider) {
        val badge = badgeBounds
        val icon = splash.iconView
        if (badge == null || icon.width == 0 || !ValueAnimator.areAnimatorsEnabled()) {
            splash.remove()
            updateSystemBars()
            animationComplete.complete(Unit)
            return
        }

        val decorLocation = IntArray(2).also(decor::getLocationInWindow)
        val iconLocation = IntArray(2).also(icon::getLocationInWindow)
        // The platform expands the vector beyond its mask. The compatibility view uses its full bounds.
        val platformArtworkScale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            1f + 2f * AdaptiveIconDrawable.getExtraInsetFraction()
        } else {
            1f
        }
        val context = decor.context
        val launchBackground = if (darkTheme) {
            R.color.echoplay_launch_background_dark
        } else {
            R.color.echoplay_launch_background_light
        }
        val curtain = View(context).apply {
            setBackgroundColor(context.getColor(launchBackground))
            layout(0, 0, decor.width, decor.height)
        }
        val artworkSize = (icon.width * platformArtworkScale).roundToInt()
        val artworkRadius = artworkSize / 2f
        val startX = iconLocation[0] - decorLocation[0] + icon.width / 2f - artworkRadius
        val startY = iconLocation[1] - decorLocation[1] + icon.height / 2f - artworkRadius
        val endX = badge.center.x - decorLocation[0] - artworkRadius
        val endY = badge.center.y - decorLocation[1] - artworkRadius
        val endScale = badge.width * BADGE_ARTWORK_SCALE / artworkSize
        val artwork = LaunchLogoView(context, EchoPlayLogo.forTheme(darkTheme)).apply {
            val exactSize = View.MeasureSpec.makeMeasureSpec(artworkSize, View.MeasureSpec.EXACTLY)
            measure(exactSize, exactSize)
            layout(0, 0, artworkSize, artworkSize)
            translationX = startX
            translationY = startY
        }
        overlayVisible.value = true
        decor.overlay.add(curtain)
        decor.overlay.add(artwork)
        splash.remove()
        updateSystemBars()

        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 440L
            interpolator = PathInterpolator(0.33f, 0f, 0.35f, 1f)
            addUpdateListener { animation ->
                if (!decor.isAttachedToWindow) {
                    animation.cancel()
                } else {
                    val progress = animation.animatedFraction
                    artwork.translationX = startX + (endX - startX) * progress
                    artwork.translationY = startY + (endY - startY) * progress
                    val scale = 1f + (endScale - 1f) * progress
                    artwork.scaleX = scale
                    artwork.scaleY = scale
                    curtain.alpha = 1f - fade(progress, 0.05f, 0.83f)
                    artwork.badgeBackground.alpha = fade(progress, 0.73f, 1f)
                }
            }
            doOnEnd {
                if (decor.isAttachedToWindow) {
                    overlayVisible.value = false
                    decor.postOnAnimation { finish(curtain, artwork) }
                } else {
                    finish(curtain, artwork)
                }
            }
            start()
        }
    }

    private fun finish(curtain: View, artwork: View) {
        overlayVisible.value = false
        decor.overlay.remove(artwork)
        decor.overlay.remove(curtain)
        animationComplete.complete(Unit)
    }

    private fun updateSystemBars() {
        systemBars.isAppearanceLightStatusBars = !darkTheme
        systemBars.isAppearanceLightNavigationBars = !darkTheme
    }

    private fun fade(progress: Float, start: Float, end: Float): Float {
        val fraction = ((progress - start) / (end - start)).coerceIn(0f, 1f)
        return fraction * fraction * (3f - 2f * fraction)
    }
}

private class LaunchLogoView(context: Context, logo: EchoPlayLogo) : FrameLayout(context) {
    val badgeBackground = ImageView(context).apply {
        setImageResource(logo.background)
        scaleType = ImageView.ScaleType.FIT_XY
        alpha = 0f
        outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                val inset = ((view.width - view.width / BADGE_ARTWORK_SCALE) / 2f).roundToInt()
                outline.setOval(inset, inset, view.width - inset, view.height - inset)
            }
        }
        clipToOutline = true
    }

    init {
        val mark = ImageView(context).apply {
            setImageResource(logo.mark)
            scaleType = ImageView.ScaleType.FIT_XY
        }
        addView(badgeBackground, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        addView(mark, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    }
}
