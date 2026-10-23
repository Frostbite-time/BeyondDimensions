package com.wintercogs.beyonddimensions.client.ui.kit

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import com.wintercogs.beyonddimensions.client.ui.theme.Bd
import com.wintercogs.beyonddimensions.client.ui.theme.BdColors
import dev.compixel.host.ScreenTransition
import kotlin.math.roundToInt

/*
 * 窗口的进退场：打开时从屏幕下方滑上来并淡入，背景同时变暗；关闭时滑下去并淡出。
 * 关闭时玩家立即拿回操作，退场由 CompixelUI 在游戏画面上播完。
 */
private const val OPEN_MILLIS = 180
private const val EXIT_MILLIS = 180

/** 窗口升起的距离占屏幕高度的比例 */
private const val RISE_OF_SCREEN = 0.25f

/**
 * 所有 BD 界面的骨架：像原版一样压暗画面的背景与居中的窗口一起进退场。
 * 滑动在布局阶段移动窗口，原生槽位的命中区域随之更新。[window] 收到可用的屏幕尺寸，用来决定窗口大小
 */
@Composable
fun BdScreenFrame(window: @Composable BoxScope.(available: DpSize) -> Unit) =
    ScreenTransition(
        enter = fadeIn(tween(OPEN_MILLIS, easing = LinearOutSlowInEasing)),
        exit = fadeOut(tween(EXIT_MILLIS, easing = LinearEasing)),
    ) {
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
