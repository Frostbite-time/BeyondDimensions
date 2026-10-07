package com.wintercogs.beyonddimensions.client.ui.kit

import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import com.wintercogs.beyonddimensions.client.ui.theme.Bd
import com.wintercogs.beyonddimensions.client.ui.theme.BdColors
import dev.compixel.host.ScreenTransition
import kotlin.math.roundToInt

/*
 * 参照基岩版的容器界面：打开时窗口从屏幕下方先快后慢地滑上来并淡入，背景同时变暗、变模糊；关闭时先慢后快地滑下去并淡出。
 * 关闭时玩家立即拿回操作，退场由 CompixelUI 在游戏画面上播完。
 */
private const val OPEN_MILLIS = 180
private const val EXIT_MILLIS = 180

/** 窗口升起的距离占屏幕高度的比例 */
private const val RISE_OF_SCREEN = 0.25f

/**
 * 所有 BD 界面的骨架：变暗、模糊的背景（模糊见 [BdBackdrop]）与居中的窗口一起淡入淡出，窗口同时从下方升起、退场时滑回下方。
 * 滑动在布局阶段移动窗口，原生槽位的命中区域随之更新，动画中途也能点准。
 *
 * [window] 收到可用的屏幕尺寸，用来决定窗口大小；它的 BoxScope 就是窗口本身的范围。
 */
@Composable
fun BdScreenFrame(window: @Composable BoxScope.(available: DpSize) -> Unit) =
    ScreenTransition(
        enter = fadeIn(tween(OPEN_MILLIS, easing = LinearOutSlowInEasing)),
        exit = fadeOut(tween(EXIT_MILLIS, easing = LinearEasing)),
    ) {
        // 背后模糊的强度与背景一起淡入淡出
        val visibility by
            transition.animateFloat(
                {
                    if (targetState == EnterExitState.Visible) tween(OPEN_MILLIS, easing = LinearOutSlowInEasing)
                    else tween(EXIT_MILLIS, easing = LinearEasing)
                },
                label = "BD backdrop",
            ) {
                if (it == EnterExitState.Visible) 1f else 0f
            }
        BdBackdrop.Follow({ visibility }, { transition.targetState == EnterExitState.PostExit })
        BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val rise = (constraints.maxHeight * RISE_OF_SCREEN).roundToInt()
            val available = DpSize(maxWidth, maxHeight)
            Box(Modifier.matchParentSize().background(Bd.colors[BdColors.backdrop]))
            Box(
                Modifier.animateEnterExit(
                    enter = slideInVertically(tween(OPEN_MILLIS, easing = LinearOutSlowInEasing)) { rise },
                    exit = slideOutVertically(tween(EXIT_MILLIS, easing = FastOutLinearInEasing)) { rise },
                )
            ) {
                window(available)
            }
        }
    }
