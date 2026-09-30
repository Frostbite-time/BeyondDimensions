package com.wintercogs.beyonddimensions.client.ui.base

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.ceil
import kotlin.math.floor
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.renderer.Rect2i

/**
 * 面板外的按钮（模式按钮、存储界面的侧边栏）所占的区域，供 JEI、EMI 避让，如同旧版上报的按钮区域。
 * 布局时在 Compose 线程写入，由游戏线程读取。
 */
class OutsideAreas {
    private val areas = ConcurrentHashMap<Any, Rect>()

    /** 界面坐标下的各区域；Compose 的像素按界面与窗口的比例换算，与槽位坐标的换算一致 */
    fun rects(screen: Screen): List<Rect2i> {
        val window = Minecraft.getInstance().window
        val x = screen.width.toDouble() / window.width.coerceAtLeast(1)
        val y = screen.height.toDouble() / window.height.coerceAtLeast(1)
        return areas.values.map { area ->
            val left = floor(area.left * x).toInt()
            val top = floor(area.top * y).toInt()
            Rect2i(left, top, ceil(area.right * x).toInt() - left, ceil(area.bottom * y).toInt() - top)
        }
    }

    internal fun put(key: Any, area: Rect) {
        areas[key] = area
    }

    internal fun remove(key: Any) {
        areas.remove(key)
    }
}

val LocalOutsideAreas = staticCompositionLocalOf<OutsideAreas?> { null }

/** 把所修饰的内容登记为面板外的区域；离开组合时撤销 */
@Composable
fun outsideArea(): Modifier {
    val areas = LocalOutsideAreas.current ?: return Modifier
    val key = remember { Any() }
    DisposableEffect(areas, key) { onDispose { areas.remove(key) } }
    return remember(areas, key) { Modifier.onGloballyPositioned { areas.put(key, it.boundsInRoot()) } }
}
