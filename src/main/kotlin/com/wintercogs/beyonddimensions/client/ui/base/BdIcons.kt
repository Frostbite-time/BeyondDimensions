package com.wintercogs.beyonddimensions.client.ui.base

import com.wintercogs.beyonddimensions.api.ids.BDConstants
import dev.compixel.forge.item.IconRefresh
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
            ItemIcon.drawn("sprite:$sprite", { graphics -> graphics.blitSprite(sprite, 0, 0, 16, 16) }, IconRefresh.STATIC)
        }

    /** 空的过滤槽里的向下箭头，取自旧版过滤槽贴图 */
    val filterMarker: ItemIcon by lazy {
        val texture = ResourceLocation.fromNamespaceAndPath(BDConstants.MODID, "textures/gui/filter_slots.png")
        ItemIcon.drawn("filter-marker", { graphics -> graphics.blit(texture, 0, 0, 8f, 1f, 16, 16, 176, 18) }, IconRefresh.STATIC)
    }
}
