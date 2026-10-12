package com.wintercogs.beyonddimensions.api.ui.page

import net.minecraft.resources.ResourceLocation
import net.minecraft.world.inventory.AbstractContainerMenu

/**
 * 附属模组注入 BD 界面的页面。每页按菜单类注册，出现在这种菜单（及其子类）的界面上，
 * 页签排在界面自带的页签之后、设置页签之前。在客户端初始化时注册。
 * ```
 * BdPages.register<NetFurnaceMenu>(
 *     ResourceLocation.fromNamespaceAndPath("example", "fuel_stats"),
 *     FuelStatsPage,
 *     BdPages.after(ResourceLocation.fromNamespaceAndPath("example", "overview")),
 * )
 * ```
 */
object BdPages {
    /** 界面自带的主页面 */
    const val MAIN = "main"

    /** 界面自带的设置页；没有设置页的界面没有这一页 */
    const val SETTINGS = "settings"

    /** 界面自带的面配置页；没有面配置的界面没有这一页 */
    const val SIDES = "sides"

    sealed interface Placement

    private data object First : Placement

    private data object Last : Placement

    private data class Before(val id: ResourceLocation) : Placement

    private data class After(val id: ResourceLocation) : Placement

    /** 排在所有已注册的注入页之前 */
    fun first(): Placement = First

    /** 排在所有已注册的注入页之后 */
    fun last(): Placement = Last

    /** 紧挨着排在 [id] 之前 */
    fun before(id: ResourceLocation): Placement = Before(id)

    /** 紧挨着排在 [id] 之后 */
    fun after(id: ResourceLocation): Placement = After(id)

    internal class Entry(val id: ResourceLocation, val menuClass: Class<*>, val page: BdPage<*, *, *>)

    @Volatile
    private var entries = emptyList<Entry>()

    @JvmStatic
    @JvmOverloads
    @Synchronized
    fun <M : AbstractContainerMenu> register(
        id: ResourceLocation,
        menuClass: Class<M>,
        page: BdPage<M, *, *>,
        placement: Placement = last(),
    ) {
        check(entries.none { it.id == id }) { "BD page $id is already registered. Registered, in order: ${ids()}" }
        val next = entries.toMutableList()
        val entry = Entry(id, menuClass, page)
        when (placement) {
            First -> next.add(0, entry)
            Last -> next.add(entry)
            is Before -> next.add(anchor(next, id, placement.id, "before"), entry)
            is After -> next.add(anchor(next, id, placement.id, "after") + 1, entry)
        }
        entries = next.toList()
    }

    inline fun <reified M : AbstractContainerMenu> register(
        id: ResourceLocation,
        page: BdPage<M, *, *>,
        placement: Placement = last(),
    ) = register(id, M::class.java, page, placement)

    /** 已注册的页面，按顺序 */
    fun ids(): List<ResourceLocation> = entries.map { it.id }

    /** 出现在 [menu] 界面上的页面，按顺序 */
    internal fun pagesFor(menu: AbstractContainerMenu): List<Entry> = entries.filter { it.menuClass.isInstance(menu) }

    private fun anchor(list: List<Entry>, id: ResourceLocation, anchor: ResourceLocation, relation: String): Int {
        val index = list.indexOfFirst { it.id == anchor }
        check(index >= 0) {
            "Cannot register BD page $id $relation $anchor: no page $anchor is registered. " +
                    "Registered, in order: ${list.map { it.id }}"
        }
        return index
    }
}
