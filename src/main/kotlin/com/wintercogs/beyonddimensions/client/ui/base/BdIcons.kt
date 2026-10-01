package com.wintercogs.beyonddimensions.client.ui.base

import com.wintercogs.beyonddimensions.api.ids.BDConstants
import dev.compixel.forge.drawing.NativeRefresh
import dev.compixel.forge.item.ItemIcon
import net.minecraft.resources.ResourceLocation

/**
 * BD 原有的界面图标（textures/gui/sprites/widget 下的精灵图），以原生绘制放进 Ore 按钮等组件里显示。
 * 只在游戏线程创建；同一张图共用一个图标，资源包替换图标后随资源重载一起更新。
 */
object BdIcons {
    private val icons = HashMap<String, ItemIcon>()

    fun sprite(name: String): ItemIcon =
        icons.getOrPut(name) {
            val sprite = ResourceLocation.fromNamespaceAndPath(BDConstants.MODID, "widget/$name")
            ItemIcon.drawn("sprite:$sprite", { graphics -> graphics.blitSprite(sprite, 0, 0, 16, 16) }, NativeRefresh.STATIC)
        }

    /** 空的过滤槽里的向下箭头，取自旧版过滤槽贴图 */
    val filterMarker: ItemIcon by lazy {
        val texture = ResourceLocation.fromNamespaceAndPath(BDConstants.MODID, "textures/gui/filter_slots.png")
        ItemIcon.drawn("filter-marker", { graphics -> graphics.blit(texture, 0, 0, 8f, 1f, 16, 16, 176, 18) }, NativeRefresh.STATIC)
    }

    private val furnace = ResourceLocation.fromNamespaceAndPath(BDConstants.MODID, "textures/gui/net_furnace.png")
    private val cooked = ResourceLocation.fromNamespaceAndPath(BDConstants.MODID, "widget/work_done_v")
    private val burning = ResourceLocation.fromNamespaceAndPath(BDConstants.MODID, "widget/furnace_work_v")

    /**
     * 熔炉的冶炼箭头（14×19），[rows] 是从上往下已完成的行数。空闲的灰色箭头取自旧版熔炉背景，进度叠在上面；
     * 箭头比图标高，拆成上 16 行（[lower] 为 false）与下 3 行两个图标以保留原图像素。每种进度只绘制一次。
     */
    fun cookArrow(rows: Int, lower: Boolean): ItemIcon =
        icons.getOrPut("cook:$rows:$lower") {
            val top = if (lower) 16 else 0
            val height = if (lower) 3 else 16
            val done = (rows - top).coerceIn(0, height)
            ItemIcon.drawn(
                "cook-arrow",
                { graphics ->
                    graphics.blit(furnace, 1, 0, 32f, (61 + top).toFloat(), 14, height, 230, 210)
                    if (done > 0) graphics.blitSprite(cooked, 14, 19, 0, top, 1, 0, 14, done)
                },
                NativeRefresh.STATIC,
            )
        }

    /** 熔炉的燃料火焰（14×14），[rows] 是从下往上仍在燃烧的行数；灰色火焰取自旧版熔炉背景 */
    fun fuelFlame(rows: Int): ItemIcon =
        icons.getOrPut("fuel:$rows") {
            ItemIcon.drawn(
                "fuel-flame",
                { graphics ->
                    graphics.blit(furnace, 1, 1, 31f, 109f, 14, 14, 230, 210)
                    if (rows > 0) graphics.blitSprite(burning, 14, 14, 0, 14 - rows, 1, 15 - rows, 14, rows)
                },
                NativeRefresh.STATIC,
            )
        }
}
