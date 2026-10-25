package com.wintercogs.beyonddimensions.client.ui.theme

import androidx.compose.foundation.LocalContextMenuRepresentation
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
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
 * 超越维度的界面风格：窗口像 BD 的方块一样是一块机壳，带深色轮廓与一像素的明暗边，
 * 页面嵌在四周发光的屏幕里；按钮凸起，输入框与槽位凹下。
 * 颜色由 [BdColors] 声明，来自玩家选的配色与颜色编辑器里的修改。读取：`Bd.colors[BdColors.window]`
 */
object Bd {
    /** 控件的外形：四角各缺一个像素，像原版的按钮 */
    val ChipShape = CutCornerShape(1.dp)

    val colors: ColorValues
        @Composable get() = LocalBdColors.current

    val body: TextStyle
        @Composable get() = OreTheme.typography.body

    val title: TextStyle
        @Composable get() = OreTheme.typography.title

    val caption: TextStyle
        @Composable get() = OreTheme.typography.caption

    /** 分组标题这类小字 */
    val overline: TextStyle
        @Composable get() = OreTheme.typography.caption

    /** 6sp 的 Monocraft：数字约为原版字体的三分之二大 */
    val amount: TextStyle
        @Composable get() = OreTheme.typography.body.copy(fontSize = 6.sp, lineHeight = 8.sp)
}

/**
 * BD 界面的全部颜色，默认值是玻璃配色。编辑器按这里的分组与先后列出颜色，
 * 名称取自语言文件的 color.beyonddimensions.<键>
 */
object BdColors : ColorSchema("beyonddimensions") {
    val window = color("window", "window", Color(0xA30C1018))

    /** 卡片，以及悬停与选中的按钮 */
    val surface = color("surface", "window", Color(0x14FFFFFF))

    /** 输入框、开关与滚动条的凹槽等凹下的一层 */
    val sunken = color("sunken", "window", Color(0x47000000))

    /** 浮层的底色，比窗口更实，免得下面的内容透上来 */
    val popover = color("popover", "window", Color(0xF2141A26))
    val line = color("line", "window", Color(0x26FFFFFF))
    val lineStrong = color("lineStrong", "window", Color(0x47FFFFFF))

    /** 界面打开时铺满屏幕、压暗背后画面的一层在屏幕顶部的颜色，往下渐变得更实，与原版容器界面相同 */
    val backdrop = color("backdrop", "window", Color(0xC0101010))
    val text = color("text", "text", Color(0xFFF2F5F9))
    val muted = color("muted", "text", Color(0xFFAEB8C6))
    val faint = color("faint", "text", Color(0xFF8591A2))

    /** 强调色或危险色实底上的文字 */
    val onAccent = color("onAccent", "text", Color(0xFF0B1220))
    val accent = color("accent", "accent", Color(0xFF22C7F0))
    val accentDeep = color("accentDeep", "accent", Color(0xFF8CE6FF))

    /** 熔炉导管渐变的终点；起点见 [cyan] */
    val violet = color("violet", "accent", Color(0xFF6D6AFF))
    val online = color("online", "status", Color(0xFF4FD69A))
    val danger = color("danger", "status", Color(0xFFFF6B6B))

    /** 槽位凹槽的底色，以及左上的暗边与右下的亮边 */
    val slot = color("slot", "slots", Color(0x52000000))
    val slotShadow = color("slotShadow", "slots", Color(0x66000000))
    val slotHighlight = color("slotHighlight", "slots", Color(0x1FFFFFFF))

    /** 格子数量的文字与它右下一个字体像素处的投影 */
    val amount = color("amount", "slots", Color.White)
    val amountShadow = color("amountShadow", "slots", Color(0xFF413F54))

    /** 熔炉火焰的三层：外焰、中焰与焰心 */
    val flameOuter = color("flameOuter", "flame", Color(0xFFE2531F))
    val flameMiddle = color("flameMiddle", "flame", Color(0xFFF5A524))
    val flameCore = color("flameCore", "flame", Color(0xFFFFE680))

    /** 面配置里输入、输出与双向的面；禁用的面与普通的格子一样 */
    val sideInput = color("sideInput", "sides", Color(0xFFFF6464))
    val sideOutput = color("sideOutput", "sides", Color(0xFF4C8DFF))
    val sideBoth = color("sideBoth", "sides", Color(0xFFB46CFF))

    val accentSoft = derived("accentSoft", "accent", accent) { it[accent].copy(alpha = .15f) }

    /** 熔炉导管渐变的起点，跟随强调色 */
    val cyan = derived("cyan", "accent", accent) { it[accent] }

    /** 悬停或选中的槽位四角的角标 */
    val marker = derived("marker", "slots", accent) { it[accent] }

    /** 文本框里选中文字的底色 */
    val selection = derived("selection", "text", accent) { it[accent].copy(alpha = .4f) }

    /** 窗口与凸起控件的深色外轮廓 */
    val outline = derived("outline", "window", window) { lerp(it[window].copy(alpha = 1f), Color.Black, .7f) }

    /** 凸起处左上的亮边与右下的暗边；凹下处反过来 */
    val bevelLight = derived("bevelLight", "window", window) { lerp(it[window], Color.White, .14f) }
    val bevelDark = derived("bevelDark", "window", window) { lerp(it[window], Color.Black, .45f) }

    /** 选中或聚焦的控件、悬停的凸起控件换上的外框 */
    val activeEdge = derived("activeEdge", "window", accent) { it[accent] }

    /** 悬停的输入框与开关换上的边 */
    val hoverEdge = derived("hoverEdge", "window", lineStrong) { it[lineStrong] }

    /** 页面所在的屏幕，比外壳深一层 */
    val screen = derived("screen", "window", window) { lerp(it[window], Color.Black, .42f) }

    /** 屏幕四周发光的边：每条边两端是 [glow]，向中段渐亮到 [glowBright] */
    val glow = derived("glow", "accent", accent) { lerp(it[accent], Color.Black, .5f) }
    val glowBright = derived("glowBright", "accent", accent) { lerp(it[accent], Color.White, .3f) }
}

val LocalBdColors = staticCompositionLocalOf { BdColors.defaults }

/** BD 界面的设计：提供玩家所选配色的 BD 颜色与文本框的右键菜单，并按 BD 的颜色为其中的 Ore 控件配色 */
object BdDesign : UiDesign {
    override val owner = SchemeOwner("beyonddimensions", BdColors, default = "casing")

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

/** BD 界面里 Ore 控件的颜色，全部由 BD 的颜色推出，Ore 自己的配色不参与 */
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
        OreColors.slotHover to this[BdColors.slot],
        OreColors.slotHoverEdge to Color.Transparent,
        OreColors.text to this[BdColors.text],
        OreColors.mutedText to this[BdColors.muted],
        OreColors.disabledText to this[BdColors.faint],
        OreColors.primary to this[BdColors.accent],
        OreColors.danger to this[BdColors.danger],
        OreColors.focus to this[BdColors.accent],
        OreColors.selection to this[BdColors.selection],
    )

/** 沿用 Ore 的 Compixel 字体，正文为 9sp：Monocraft 的一个像素正好对应一个界面像素 */
private val BdTypography by lazy {
    val body = OreTypography().body.copy(fontSize = 9.sp, lineHeight = 11.sp)
    OreTypography(body = body, title = body.copy(fontSize = 10.sp, lineHeight = 13.sp), caption = body)
}
