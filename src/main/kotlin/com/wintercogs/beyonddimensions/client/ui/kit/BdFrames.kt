package com.wintercogs.beyonddimensions.client.ui.kit

import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wintercogs.beyonddimensions.client.ui.theme.BdColors
import dev.compixel.ui.theme.ColorValues
import kotlin.math.abs
import kotlin.math.roundToInt

/* 像素立体的几种外观：凸起的外壳与按钮、凹下的输入框，以及屏幕四周发光的边。都以界面像素为单位来画 */

/**
 * 凸起：深色外轮廓（四角各缺一个像素），里面左上一圈亮边、右下一圈暗边。
 * [edge] 不为 null 时外轮廓换成这个颜色，表示悬停或选中
 */
fun Modifier.bdRaised(colors: ColorValues, fill: Color = colors[BdColors.window], edge: Color? = null): Modifier =
    drawBehind {
        val p = 1.dp.toPx()
        val w = size.width
        val h = size.height
        val outline = edge ?: colors[BdColors.outline]
        drawRect(outline, Offset(p, 0f), Size(w - 2 * p, p))
        drawRect(outline, Offset(p, h - p), Size(w - 2 * p, p))
        drawRect(outline, Offset(0f, p), Size(p, h - 2 * p))
        drawRect(outline, Offset(w - p, p), Size(p, h - 2 * p))
        drawRect(fill, Offset(p, p), Size(w - 2 * p, h - 2 * p))
        // 亮边与暗边在右上与左下角各让出一个像素给底色
        val light = colors[BdColors.bevelLight]
        val dark = colors[BdColors.bevelDark]
        drawRect(light, Offset(p, p), Size(w - 3 * p, p))
        drawRect(light, Offset(p, 2 * p), Size(p, h - 4 * p))
        drawRect(dark, Offset(2 * p, h - 2 * p), Size(w - 3 * p, p))
        drawRect(dark, Offset(w - 2 * p, 2 * p), Size(p, h - 4 * p))
    }

/**
 * 凹下：左上一圈暗边、右下一圈亮边，两色在右上与左下角让给底色，像原版的槽位。
 * [edge] 不为 null 时四周换成一圈这个颜色，表示悬停、聚焦或出错
 */
fun Modifier.bdInset(colors: ColorValues, fill: Color = colors[BdColors.sunken], edge: Color? = null): Modifier =
    drawBehind {
        val p = 1.dp.toPx()
        val w = size.width
        val h = size.height
        drawRect(fill)
        val dark = edge ?: colors[BdColors.outline]
        val light = edge ?: colors[BdColors.bevelLight]
        drawRect(dark, Offset.Zero, Size(w - p, p))
        drawRect(dark, Offset(0f, p), Size(p, h - 2 * p))
        drawRect(light, Offset(p, h - p), Size(w - p, p))
        drawRect(light, Offset(w - p, p), Size(p, h - 2 * p))
        if (edge != null) {
            drawRect(edge, Offset(w - p, 0f), Size(p, p))
            drawRect(edge, Offset(0f, h - p), Size(p, p))
        }
    }

/** 一组槽位外的一圈细线。只画四边：槽位底色半透明时，线色不会从槽位底下透出来 */
fun Modifier.bdSlotFrame(colors: ColorValues): Modifier =
    drawBehind {
        val t = 0.5.dp.toPx()
        val w = size.width
        val h = size.height
        val line = colors[BdColors.line]
        drawRect(line, Offset.Zero, Size(w, t))
        drawRect(line, Offset(0f, h - t), Size(w, t))
        drawRect(line, Offset(0f, t), Size(t, h - 2 * t))
        drawRect(line, Offset(w - t, t), Size(t, h - 2 * t))
    }.padding(0.5.dp)

/**
 * 页面所在的屏幕：比外壳深的底色，四周一圈发光的边。屏幕从四边各向里让出一段，露出外壳
 */
fun Modifier.bdScreen(colors: ColorValues, start: Dp, top: Dp, end: Dp, bottom: Dp): Modifier = drawBehind {
    val p = 1.dp.toPx()
    val left = start.toPx()
    val upper = top.toPx()
    val right = size.width - end.toPx()
    val lower = size.height - bottom.toPx()
    drawRect(colors[BdColors.screen], Offset(left, upper), Size(right - left, lower - upper))
    glowLine(colors, left, upper, right - left, horizontal = true, p)
    glowLine(colors, left, lower - p, right - left, horizontal = true, p)
    glowLine(colors, left, upper + p, lower - upper - 2 * p, horizontal = false, p)
    glowLine(colors, right - p, upper + p, lower - upper - 2 * p, horizontal = false, p)
}

/**
 * 一像素宽的发光线，像 BD 方块正面的发光框：两端是 [BdColors.glow]，分四级向中段亮到 [BdColors.glowBright]
 */
internal fun DrawScope.glowLine(
    colors: ColorValues,
    x: Float,
    y: Float,
    length: Float,
    horizontal: Boolean,
    p: Float,
) {
    val count = (length / p).roundToInt()
    if (count <= 0) return
    val dim = colors[BdColors.glow]
    val bright = colors[BdColors.glowBright]
    var start = 0
    while (start < count) {
        val level = glowLevel(start, count)
        var end = start + 1
        while (end < count && glowLevel(end, count) == level) end++
        val color = lerp(dim, bright, level / 3f)
        if (horizontal) drawRect(color, Offset(x + start * p, y), Size((end - start) * p, p))
        else drawRect(color, Offset(x, y + start * p), Size(p, (end - start) * p))
        start = end
    }
}

// 第 [index] 个像素的亮度级：0 在两端，3 在中段
private fun glowLevel(index: Int, count: Int): Int {
    val distance = abs(2f * index - (count - 1)) / (count - 1).coerceAtLeast(1)
    return when {
        distance < .3f -> 3
        distance < .55f -> 2
        distance < .8f -> 1
        else -> 0
    }
}

/**
 * 凹槽里的滑块，用于滚动条与开关：左上一圈亮边、右下一圈暗边，没有外轮廓，由凹槽的边框住，像原版的滚动条滑块
 */
fun Modifier.bdKnob(fill: Color, light: Color, dark: Color): Modifier = drawBehind {
    knob(Offset.Zero, size, fill, light, dark)
}

internal fun DrawScope.knob(topLeft: Offset, size: Size, fill: Color, light: Color, dark: Color) {
    val p = 1.dp.toPx()
    val x = topLeft.x
    val y = topLeft.y
    val w = size.width
    val h = size.height
    drawRect(fill, topLeft, size)
    drawRect(light, Offset(x, y), Size(w - p, p))
    drawRect(light, Offset(x, y + p), Size(p, h - 2 * p))
    drawRect(dark, Offset(x + p, y + h - p), Size(w - p, p))
    drawRect(dark, Offset(x + w - p, y + p), Size(p, h - 2 * p))
}
