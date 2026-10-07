package com.wintercogs.beyonddimensions.client.ui.theme

import androidx.compose.foundation.LocalContextMenuRepresentation
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wintercogs.beyonddimensions.client.ui.kit.BdContextMenu
import dev.compixel.ui.UiDesign
import dev.compixel.ui.ore.theme.OreColors
import dev.compixel.ui.ore.theme.OreTheme
import dev.compixel.ui.ore.theme.OreTypography
import dev.compixel.ui.theme.ColorSchema
import dev.compixel.ui.theme.ColorValues
import dev.compixel.ui.theme.SchemeOwner
import dev.compixel.ui.theme.colors

/**
 * 超越维度的界面风格：深色半透明的玻璃面板透出背后的游戏画面，配一像素的浅色细线、青色强调色，以及由青到紫的标志渐变。
 *
 * 颜色由 [BdColors] 声明，来自玩家选的配色（assets/beyonddimensions/compixel/schemes/ 下的文件，资源包可以覆盖或增加），
 * 玩家在 CompixelUI 颜色编辑器里的修改叠在最上面。读取：`Bd.colors[BdColors.window]`。
 */
object Bd {
    /** 控件切角的边长 */
    const val CHIP_CUT = 3

    /** 控件的外形：左上与右下切角 */
    val ChipShape = CutCornerShape(topStart = CHIP_CUT.dp, bottomEnd = CHIP_CUT.dp)

    /**
     * [ChipShape] 一像素边框以内的外形，切角正好沿边框斜边的内沿（3 − 2 + √2）。
     * 带边框的控件里有自己底色的子项时，内容先留出一像素再按它裁剪，子项不会盖住边框
     */
    val ChipInnerShape = CutCornerShape(topStart = 2.41.dp, bottomEnd = 2.41.dp)

    val colors: ColorValues
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

/**
 * BD 界面的全部颜色，默认值是玻璃配色。编辑器按这里的分组与先后列出颜色，名称取自语言文件的 color.beyonddimensions.<键>。
 * 强调底色、标志渐变的起点、文字选区与悬停槽位默认跟随别的颜色，配色文件也可以单独指定。
 */
object BdColors : ColorSchema("beyonddimensions") {
    val window = color("window", "window", Color(0xC70C1018))

    /** 标题栏、卡片等略亮的一层 */
    val surface = color("surface", "window", Color(0x0FFFFFFF))

    /** 页签栏、输入框等凹陷的一层 */
    val sunken = color("sunken", "window", Color(0x59000000))

    /** 浮层的底色，比窗口更实，免得下面的内容透上来 */
    val popover = color("popover", "window", Color(0xF2141A26))
    val line = color("line", "window", Color(0x24FFFFFF))
    val lineStrong = color("lineStrong", "window", Color(0x40FFFFFF))

    /** 界面打开时铺满屏幕、压暗背后画面的一层 */
    val backdrop = color("backdrop", "window", Color(0x330A1018))
    val text = color("text", "text", Color(0xFFF2F5F9))
    val muted = color("muted", "text", Color(0xFFA7B1BF))
    val faint = color("faint", "text", Color(0xFF7D8898))

    /** 强调色或危险色实底上的文字 */
    val onAccent = color("onAccent", "text", Color(0xFF0B1220))
    val accent = color("accent", "accent", Color(0xFF22C7F0))
    val accentDeep = color("accentDeep", "accent", Color(0xFF8CE6FF))

    /** 标志渐变的终点；起点见 [cyan] */
    val violet = color("violet", "accent", Color(0xFF6D6AFF))
    val online = color("online", "status", Color(0xFF4FD69A))
    val warning = color("warning", "status", Color(0xFFF2B544))
    val danger = color("danger", "status", Color(0xFFFF6B6B))

    /** 槽位凹槽的底色，以及左上的暗边与右下的亮边 */
    val slot = color("slot", "slots", Color(0x59000000))
    val slotShadow = color("slotShadow", "slots", Color(0x66000000))
    val slotHighlight = color("slotHighlight", "slots", Color(0x1FFFFFFF))

    /** 悬停槽位的描边；悬停时凹槽去掉明暗边 */
    val slotHoverOutline = color("slotHoverOutline", "slots", Color(0x00000000))

    /** 格子数量标签的底色 */
    val pill = color("pill", "slots", Color(0xA6000000))

    /** 熔炉火焰的三层：外焰、中焰与焰心 */
    val flameOuter = color("flameOuter", "flame", Color(0xFFE2531F))
    val flameMiddle = color("flameMiddle", "flame", Color(0xFFF5A524))
    val flameCore = color("flameCore", "flame", Color(0xFFFFE680))

    val accentSoft = derived("accentSoft", "accent", accent) { it[accent].copy(alpha = .15f) }

    /** 标志渐变的起点 */
    val cyan = derived("cyan", "accent", accent) { it[accent] }

    /** 文本框里选中文字的底色 */
    val selection = derived("selection", "text", accent) { it[accent].copy(alpha = .4f) }

    /** 悬停槽位的底色 */
    val slotHover = derived("slotHover", "slots", slot) { it[slot] }
}

/** 由青到紫的标志渐变 */
val ColorValues.signature: Brush
    get() = Brush.horizontalGradient(listOf(this[BdColors.cyan], this[BdColors.violet]))

val ColorValues.signatureVertical: Brush
    get() = Brush.verticalGradient(listOf(this[BdColors.cyan], this[BdColors.violet]))

val LocalBdColors = staticCompositionLocalOf { BdColors.defaults }

/**
 * BD 界面的设计：提供玩家所选配色的 BD 颜色与文本框的右键菜单，并按 BD 的颜色为其中的 Ore 控件配色。
 * 配色或颜色变化时，打开着的 BD 界面就地换色，输入的文字、滚动位置等都保留。
 */
object BdDesign : UiDesign {
    override val owner = SchemeOwner("beyonddimensions", BdColors, default = "glass")

    @Composable
    override fun Decorate(content: @Composable () -> Unit) {
        val colors = owner.colors()
        val ore = remember(colors) { colors.ore() }
        OreTheme(ore, BdTypography) {
            CompositionLocalProvider(
                LocalBdColors provides colors,
                LocalContextMenuRepresentation provides BdContextMenu,
                content = content,
            )
        }
    }

    override fun preview(): @Composable () -> Unit {
        val preview = BdPreviewContent()
        return { BdPreview(preview) }
    }
}

/**
 * BD 界面里 Ore 控件的颜色，全部由 BD 的颜色推出，Ore 自己的配色不参与。BD 用到的 Ore 控件是槽位凹槽（虚拟槽位与原版槽位），
 * 这里也给出其他 Ore 控件会用到的基本颜色；其余是 Ore 的默认值，或跟随这些颜色。
 */
private fun ColorValues.ore() =
    OreColors.values(
        OreColors.backdrop to this[BdColors.backdrop],
        OreColors.panel to this[BdColors.sunken],
        OreColors.raised to this[BdColors.surface],
        OreColors.hovered to this[BdColors.accentSoft],
        OreColors.edge to this[BdColors.line],
        OreColors.highlight to this[BdColors.slotHighlight],
        OreColors.frameEdge to this[BdColors.lineStrong],
        OreColors.slot to this[BdColors.slot],
        OreColors.slotEdge to this[BdColors.slotShadow],
        OreColors.slotHover to this[BdColors.slotHover],
        OreColors.slotHoverEdge to this[BdColors.slotHoverOutline],
        OreColors.text to this[BdColors.text],
        OreColors.mutedText to this[BdColors.muted],
        OreColors.disabledText to this[BdColors.faint],
        OreColors.primary to this[BdColors.accent],
        OreColors.danger to this[BdColors.danger],
        OreColors.focus to this[BdColors.accent],
        OreColors.selection to this[BdColors.selection],
    )

/**
 * 沿用 Ore 的 Compixel 字体，正文改为 9sp：此时 Monocraft 的一个像素正好对应一个界面像素，
 * 中文等 Unifont 字符是半个像素，和原版一样。
 */
private val BdTypography by lazy {
    val body = OreTypography().body.copy(fontSize = 9.sp, lineHeight = 11.sp)
    OreTypography(body = body, title = body.copy(fontSize = 10.sp, lineHeight = 13.sp), caption = body)
}
