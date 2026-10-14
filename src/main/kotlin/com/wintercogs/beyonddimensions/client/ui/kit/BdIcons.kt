package com.wintercogs.beyonddimensions.client.ui.kit

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * BD 的单色像素图标：一格正好是一个界面像素，与原版界面、像素字体和机壳的像素一样大，任何界面缩放下都整齐。
 * 每行一样长，`#` 填色，`.` 留空
 */
@Immutable
class BdPixelIcon(vararg rows: String) {
    val width = rows.firstOrNull()?.length ?: 0
    val height = rows.size

    /** 填色的格子按行合并成横条，每四个数是 x、y、宽、高 */
    internal val runs: IntArray

    init {
        require(rows.isNotEmpty() && rows.all { it.length == width }) { "Every row of a pixel icon needs the same length" }
        require(rows.all { row -> row.all { it == '#' || it == '.' } }) { "Pixel icons use '#' and '.'" }
        val list = ArrayList<Int>()
        rows.forEachIndexed { y, row ->
            var x = 0
            while (x < width) {
                if (row[x] != '#') {
                    x++
                    continue
                }
                val start = x
                while (x < width && row[x] == '#') x++
                list += listOf(start, y, x - start, 1)
            }
        }
        runs = list.toIntArray()
    }
}

/** 按图标本身的大小画 [icon]，一格一个界面像素；不要再给它设尺寸 */
@Composable
fun BdIcon(icon: BdPixelIcon, modifier: Modifier = Modifier, color: Color) {
    Canvas(modifier.size(icon.width.dp, icon.height.dp)) {
        val p = 1.dp.toPx()
        val runs = icon.runs
        for (i in runs.indices step 4) {
            drawRect(color, Offset(runs[i] * p, runs[i + 1] * p), Size(runs[i + 2] * p, runs[i + 3] * p))
        }
    }
}

/** BD 界面里的全部图标：竖条页签用 10×10，按钮与文字旁的用 7×7 上下 */
object BdIcons {
    /** 主页面：四个存储格 */
    val Main =
        BdPixelIcon(
            "####..####",
            "####..####",
            "####..####",
            "####..####",
            "..........",
            "..........",
            "####..####",
            "####..####",
            "####..####",
            "####..####",
        )

    /** 合成：九宫格 */
    val Crafting =
        BdPixelIcon(
            "##..##..##",
            "##..##..##",
            "..........",
            "..........",
            "##..##..##",
            "##..##..##",
            "..........",
            "..........",
            "##..##..##",
            "##..##..##",
        )

    /** 设置：齿轮 */
    val Settings =
        BdPixelIcon(
            "....##....",
            ".##.##.##.",
            ".########.",
            "..######..",
            "####..####",
            "####..####",
            "..######..",
            ".########.",
            ".##.##.##.",
            "....##....",
        )

    /** 切换：一左一右两个箭头 */
    val Switch =
        BdPixelIcon(
            "......#...",
            "......##..",
            ".########.",
            "......##..",
            "......#...",
            "...#......",
            "..##......",
            ".########.",
            "..##......",
            "...#......",
        )

    /** 配色：调色板，三个颜料孔，右下收窄 */
    val Palette =
        BdPixelIcon(
            "...####..",
            ".#######.",
            "##.##.###",
            "#########",
            "#.#####..",
            "######...",
            ".####....",
            "..##.....",
        )

    val Close =
        BdPixelIcon(
            "#.....#",
            ".#...#.",
            "..#.#..",
            "...#...",
            "..#.#..",
            ".#...#.",
            "#.....#",
        )

    val Search =
        BdPixelIcon(
            ".###...",
            "#...#..",
            "#...#..",
            "#...#..",
            ".####..",
            "....##.",
            ".....##",
        )

    val Plus =
        BdPixelIcon(
            "...#...",
            "...#...",
            "...#...",
            "#######",
            "...#...",
            "...#...",
            "...#...",
        )

    val Minus =
        BdPixelIcon(
            ".......",
            ".......",
            ".......",
            "#######",
            ".......",
            ".......",
            ".......",
        )

    val Check =
        BdPixelIcon(
            ".......",
            "......#",
            ".....##",
            "#...##.",
            "##.##..",
            ".###...",
            "..#....",
        )

    /** 改名与编辑 */
    val Pencil =
        BdPixelIcon(
            ".....#.",
            "....###",
            "...###.",
            "..###..",
            ".###...",
            "###....",
            "##.....",
        )

    /** 没有图标的分类 */
    val Funnel =
        BdPixelIcon(
            ".#####.",
            ".#####.",
            "..###..",
            "..###..",
            "...#...",
            "...#...",
            "...#...",
        )

    /** 说明 */
    val Info =
        BdPixelIcon(
            "..###..",
            ".#...#.",
            "#..#..#",
            "#.....#",
            "#..#..#",
            ".#.#.#.",
            "..###..",
        )

    /** 排序 */
    val Bars =
        BdPixelIcon(
            ".......",
            "#######",
            ".......",
            "#######",
            ".......",
            "#######",
            ".......",
        )

    val ArrowUp =
        BdPixelIcon(
            "...#...",
            "..###..",
            ".#####.",
            "...#...",
            "...#...",
            "...#...",
            "...#...",
        )

    val ArrowDown =
        BdPixelIcon(
            "...#...",
            "...#...",
            "...#...",
            "...#...",
            ".#####.",
            "..###..",
            "...#...",
        )

    val ArrowRight =
        BdPixelIcon(
            ".......",
            "....#..",
            "....##.",
            "#######",
            "....##.",
            "....#..",
            ".......",
        )

    val ChevronLeft =
        BdPixelIcon(
            "...#",
            "..##",
            ".##.",
            "##..",
            ".##.",
            "..##",
            "...#",
        )

    val ChevronRight =
        BdPixelIcon(
            "#...",
            "##..",
            ".##.",
            "..##",
            ".##.",
            "##..",
            "#...",
        )

    val ChevronDown =
        BdPixelIcon(
            "##...##",
            ".##.##.",
            "..###..",
            "...#...",
        )

    /** 刷新：带箭头的圆圈 */
    val Refresh =
        BdPixelIcon(
            "..###.#",
            ".#...##",
            "#...###",
            "#......",
            "#.....#",
            ".#...#.",
            "..###..",
        )

    /** 退回存储：向上的箭头离开托盘 */
    val ToStorage =
        BdPixelIcon(
            "...#...",
            "..###..",
            ".#####.",
            "...#...",
            "...#...",
            "#.....#",
            "#######",
        )

    /** 退回背包：向下的箭头落进托盘 */
    val ToInventory =
        BdPixelIcon(
            "...#...",
            "...#...",
            ".#####.",
            "..###..",
            "...#...",
            "#.....#",
            "#######",
        )
}
