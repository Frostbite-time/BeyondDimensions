package com.wintercogs.beyonddimensions.client.ui.theme

import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.compixel.ui.UiDesign
import dev.compixel.ui.ore.theme.OreDesign
import dev.compixel.ui.ore.theme.OreTheme
import dev.compixel.ui.ore.theme.OreTypography
import dev.compixel.ui.theme.ThemeId
import dev.compixel.ui.theme.ThemeSection
import dev.compixel.ui.theme.current

/**
 * 超越维度的界面风格：深色半透明的玻璃面板透出背后的游戏画面，配一像素的浅色细线、青色强调色，以及由青到紫的标志渐变。
 *
 * 颜色来自主题文件 assets/beyonddimensions/compixel/themes/default.json 的 "beyonddimensions" 段，资源包可以覆盖；
 * 同一文件的 "ore" 段为界面里的 Ore 组件配色。
 */
object Bd {
    val ThemeId: ThemeId = dev.compixel.ui.theme.ThemeId("beyonddimensions", "default")

    /** 控件的外形：左上与右下切角 */
    val ChipShape = CutCornerShape(topStart = 3.dp, bottomEnd = 3.dp)

    /**
     * [ChipShape] 一像素边框以内的外形，切角正好沿边框斜边的内沿（3 − 2 + √2）。
     * 带边框的控件里有自己底色的子项时，内容先留出一像素再按它裁剪，子项不会盖住边框
     */
    val ChipInnerShape = CutCornerShape(topStart = 2.41.dp, bottomEnd = 2.41.dp)

    val colors: BdColors
        @Composable get() = LocalBdColors.current

    val body: TextStyle
        @Composable get() = OreTheme.typography.body

    val title: TextStyle
        @Composable get() = OreTheme.typography.title

    val caption: TextStyle
        @Composable get() = OreTheme.typography.caption

    /** 分组标题与标题栏上方的小字 */
    val overline: TextStyle
        @Composable get() = OreTheme.typography.caption

    /** 4.5sp 的 Monocraft：GUI 缩放为 2 时每个字体像素正好对应一个屏幕像素 */
    val amount: TextStyle
        @Composable get() = OreTheme.typography.body.copy(fontSize = 4.5.sp, lineHeight = 6.sp)
}

@Immutable
data class BdColors(
    val window: Color = Color(0xC70C1018),
    val surface: Color = Color(0x0FFFFFFF),
    val sunken: Color = Color(0x59000000),
    val line: Color = Color(0x24FFFFFF),
    val lineStrong: Color = Color(0x40FFFFFF),
    val text: Color = Color(0xFFF2F5F9),
    val muted: Color = Color(0xFFA7B1BF),
    val faint: Color = Color(0xFF7D8898),
    val accent: Color = Color(0xFF22C7F0),
    val accentDeep: Color = Color(0xFF8CE6FF),
    val accentSoft: Color = Color(0x2622C7F0),
    val danger: Color = Color(0xFFFF6B6B),
    /** 标志渐变的两端 */
    val cyan: Color = Color(0xFF22C7F0),
    val violet: Color = Color(0xFF6D6AFF),
    val online: Color = Color(0xFF4FD69A),
    val warning: Color = Color(0xFFF2B544),
    /** 熔炉火焰的三层：外焰、中焰与焰心 */
    val flameOuter: Color = Color(0xFFE2531F),
    val flameMiddle: Color = Color(0xFFF5A524),
    val flameCore: Color = Color(0xFFFFE680),
    /** 强调色或危险色实底上的文字 */
    val onAccent: Color = Color(0xFF0B1220),
    /** 浮层的底色，比窗口更实，免得下面的内容透上来 */
    val popover: Color = Color(0xF2141A26),
    /** 格子数量标签的底色 */
    val pill: Color = Color(0xA6000000),
) {
    val signature = Brush.horizontalGradient(listOf(cyan, violet))
    val signatureVertical = Brush.verticalGradient(listOf(cyan, violet))
}

/** 主题文件中 BD 自己的颜色，位于 "beyonddimensions" 段；每个字段写成 "#RRGGBB" 或 "#RRGGBBAA" */
object BdThemeSection : ThemeSection<BdColors>("beyonddimensions") {
    override val default = BdColors()

    override fun apply(value: BdColors, layer: Any?): BdColors {
        require(layer is Map<*, *>) { "expected an object" }
        var result = value
        for ((name, hex) in layer) {
            val color = parse(name, hex)
            result =
                when (name) {
                    "window" -> result.copy(window = color)
                    "surface" -> result.copy(surface = color)
                    "sunken" -> result.copy(sunken = color)
                    "line" -> result.copy(line = color)
                    "lineStrong" -> result.copy(lineStrong = color)
                    "text" -> result.copy(text = color)
                    "muted" -> result.copy(muted = color)
                    "faint" -> result.copy(faint = color)
                    "accent" -> result.copy(accent = color)
                    "accentDeep" -> result.copy(accentDeep = color)
                    "accentSoft" -> result.copy(accentSoft = color)
                    "danger" -> result.copy(danger = color)
                    "cyan" -> result.copy(cyan = color)
                    "violet" -> result.copy(violet = color)
                    "online" -> result.copy(online = color)
                    "warning" -> result.copy(warning = color)
                    "flameOuter" -> result.copy(flameOuter = color)
                    "flameMiddle" -> result.copy(flameMiddle = color)
                    "flameCore" -> result.copy(flameCore = color)
                    "onAccent" -> result.copy(onAccent = color)
                    "popover" -> result.copy(popover = color)
                    "pill" -> result.copy(pill = color)
                    else -> throw IllegalArgumentException("$name: unknown color")
                }
        }
        return result
    }

    private fun parse(name: Any?, hex: Any?): Color {
        require(hex is String && hex.matches(Regex("#[0-9a-fA-F]{6}([0-9a-fA-F]{2})?"))) {
            "$name: expected #RRGGBB or #RRGGBBAA"
        }
        val bits = hex.drop(1).toLong(16)
        val argb = if (hex.length == 7) bits or 0xFF000000L else (bits ushr 8) or ((bits and 255) shl 24)
        return Color(argb.toInt())
    }
}

val LocalBdColors = staticCompositionLocalOf { BdColors() }

/** BD 界面的设计：沿用 Ore 为其中的 Ore 组件配色，再提供 BD 自己的颜色 */
object BdDesign : UiDesign {
    @Composable
    override fun Decorate(theme: ThemeId, content: @Composable () -> Unit) {
        OreDesign.Decorate(theme) {
            OreTheme(typography = BdTypography) {
                CompositionLocalProvider(LocalBdColors provides BdThemeSection.current(theme), content = content)
            }
        }
    }
}

/**
 * 沿用 Ore 的 Compixel 字体，正文改为 9sp：此时 Monocraft 的一个像素正好对应一个界面像素，
 * 中文等 Unifont 字符是半个像素，和原版一样。
 */
private val BdTypography by lazy {
    val body = OreTypography().body.copy(fontSize = 9.sp, lineHeight = 11.sp)
    OreTypography(body = body, title = body.copy(fontSize = 10.sp, lineHeight = 13.sp), caption = body)
}
