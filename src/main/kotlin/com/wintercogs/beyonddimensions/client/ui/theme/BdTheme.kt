package com.wintercogs.beyonddimensions.client.ui.theme

import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.compixel.ui.ore.theme.OreColors
import dev.compixel.ui.ore.theme.OreTheme
import dev.compixel.ui.ore.theme.OreThemeId

/**
 * 超越维度的界面风格：冰白色的面板、冷色细线、一种天蓝强调色，以及由青到紫的标志渐变。
 *
 * 面板、线条与文字取自 Ore 主题（assets/beyonddimensions/compixel/ore_themes/default.json），
 * 资源包可以覆盖；标志渐变与状态色属于模组自身的识别色，不随主题变化。
 */
object Bd {
    val ThemeId = OreThemeId("beyonddimensions", "default")

    val WindowShape = CutCornerShape(topStart = 7.dp, bottomEnd = 7.dp)
    val ChipShape = CutCornerShape(topStart = 3.dp, bottomEnd = 3.dp)

    val colors: BdColors
        @Composable get() {
            val ore = OreTheme.colors
            return remember(ore) { BdColors.from(ore) }
        }

    val body: TextStyle
        @Composable get() = OreTheme.typography.body

    val title: TextStyle
        @Composable get() = OreTheme.typography.title

    val caption: TextStyle
        @Composable get() = OreTheme.typography.caption

    /** 小号的分组标题，字距加宽 */
    val overline: TextStyle
        @Composable get() = OreTheme.typography.caption.copy(letterSpacing = 1.sp)

    /** 4.5sp 的 Monocraft：GUI 缩放为 2 时每个字体像素正好对应一个屏幕像素 */
    val amount: TextStyle
        @Composable get() = OreTheme.typography.body.copy(fontSize = 4.5.sp, lineHeight = 6.sp)
}

@Immutable
class BdColors(
    val window: Color,
    val surface: Color,
    val sunken: Color,
    val line: Color,
    val lineStrong: Color,
    val text: Color,
    val muted: Color,
    val faint: Color,
    val accent: Color,
    val accentDeep: Color,
    val accentSoft: Color,
    val cell: Color,
    val danger: Color,
) {
    val cyan = Color(0xFF22C7F0)
    val violet = Color(0xFF6D6AFF)
    val signature = Brush.horizontalGradient(listOf(cyan, violet))
    val signatureVertical = Brush.verticalGradient(listOf(cyan, violet))

    val online = Color(0xFF17A56C)
    val onlineSoft = Color(0xFFE6F7EF)
    val onlineLine = Color(0xFFBFE6D4)
    val warning = Color(0xFFC77A12)
    val warningSoft = Color(0xFFFFF4E0)
    val warningLine = Color(0xFFF1D6A6)

    companion object {
        fun from(ore: OreColors) =
            BdColors(
                window = ore.panel,
                surface = ore.raised,
                sunken = ore.trackEmptyLight,
                line = ore.edge,
                lineStrong = ore.frameEdge,
                text = ore.text,
                muted = ore.mutedText,
                faint = ore.disabledText,
                accent = ore.focus,
                accentDeep = ore.primary,
                accentSoft = ore.hovered,
                cell = ore.slot,
                danger = ore.danger,
            )
    }
}
