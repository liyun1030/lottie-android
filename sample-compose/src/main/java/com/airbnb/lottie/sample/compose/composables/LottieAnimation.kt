package com.airbnb.lottie.sample.compose.composables

import android.util.Log
import androidx.annotation.RawRes
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*
import androidx.compose.runtime.dispatch.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.ContextAmbient
import androidx.compose.ui.platform.LifecycleOwnerAmbient
import androidx.compose.ui.unit.dp
import androidx.lifecycle.whenStarted
import com.airbnb.lottie.LottieComposition
import com.airbnb.lottie.LottieCompositionFactory
import com.airbnb.lottie.LottieDrawable

sealed class LottieAnimationSpec {
    class RawRes(@androidx.annotation.RawRes val resId: Int) : LottieAnimationSpec()
    class Url(val url: String) : LottieAnimationSpec()
}

@Composable
fun LottieAnimation(
    spec: LottieAnimationSpec,
    modifier: Modifier = Modifier
) {
    val context = ContextAmbient.current
    var composition: LottieComposition? by remember { mutableStateOf(null) }
    onCommit(spec) {
        var isDisposed = false
        val task = when(spec) {
            is LottieAnimationSpec.RawRes -> LottieCompositionFactory.fromRawRes(context, spec.resId)
            is LottieAnimationSpec.Url -> LottieCompositionFactory.fromUrl(context, spec.url)
        }
        task.addListener { c ->
            if (!isDisposed) composition = c
        }.addFailureListener { e ->
            Log.d("Gabe", "Animation failed to load", e)
        }
        onDispose {
            isDisposed = true
        }
    }

    LottieAnimation(composition, modifier)
}

@Composable
fun LottieAnimation(
    composition: LottieComposition?,
    modifier: Modifier = Modifier
) {
    val drawable = remember { LottieDrawable() }
    val animationTime = animationTimeMillis()
    drawable.progress = (animationTime.value / (drawable.composition?.duration ?: 1f)) % 1f
    onCommit(composition) {
        drawable.composition = composition
    }

    if (composition == null || composition.duration == 0f) return
    drawable.progress = (animationTime.value / (composition.duration)) % 1f

    Canvas(modifier = modifier) {
        drawCanvas { canvas, size ->
            withTransform({
                scale(size.width / composition.bounds.width().toFloat(), size.height / composition.bounds.height().toFloat(), 0f, 0f)
            }) {
                drawable.draw(canvas.nativeCanvas)
            }
        }
    }
}

@Composable
private fun animationTimeMillis(): State<Long> {
    val millisState = remember { mutableStateOf(0L) }
    val lifecycleOwner = LifecycleOwnerAmbient.current
    launchInComposition {
        val startTime = withFrameMillis { it }
        lifecycleOwner.whenStarted {
            while (true) {
                withFrameMillis { frameTime ->
                    millisState.value = frameTime - startTime
                }
            }
        }
    }
    return millisState
}