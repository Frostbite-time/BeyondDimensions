package com.wintercogs.beyonddimensions.client.ui.device

import com.wintercogs.beyonddimensions.common.machine.AutoSortMode
import com.wintercogs.beyonddimensions.common.machine.FeederMode
import com.wintercogs.beyonddimensions.common.machine.FilterMode
import com.wintercogs.beyonddimensions.common.machine.FuzzyMode
import com.wintercogs.beyonddimensions.common.machine.HopperRangeMode
import com.wintercogs.beyonddimensions.common.machine.PopMode
import com.wintercogs.beyonddimensions.common.machine.ReceiveMode
import com.wintercogs.beyonddimensions.common.machine.RedStoneControlMode

/* 各设备共用的模式按钮，切换顺序、图标与提示都与旧版相同 */

private const val TIP = "tooltip.button.beyonddimensions."

private fun Enum<*>.lower() = name.lowercase()

fun filterTab(get: () -> FilterMode, set: (FilterMode) -> Unit) =
    ModeTab(
        listOf(FilterMode.IGNORE, FilterMode.WHITE, FilterMode.BLACK),
        { "${it.lower()}_filter" },
        { "${TIP}filter_mode_${it.lower()}" },
        get,
        set,
    )

fun controlTab(get: () -> RedStoneControlMode, set: (RedStoneControlMode) -> Unit) =
    ModeTab(
        listOf(RedStoneControlMode.IGNORE, RedStoneControlMode.NOT_WORKING, RedStoneControlMode.POWERED, RedStoneControlMode.UNPOWERED),
        { "control_mode_${it.lower()}" },
        { "${TIP}control_mode_${it.lower()}" },
        get,
        set,
    )

/** 漏斗与磁铁的物品、经验、NBT、流体开关；[kind] 是 item、xp、nbt 或 fluid */
fun <T : Enum<T>> allowTab(kind: String, deny: T, allow: T, get: () -> T, set: (T) -> Unit) =
    ModeTab(listOf(deny, allow), { "hopper_${kind}_mode_${it.lower()}" }, { "${TIP}hopper_${kind}_mode_${it.lower()}" }, get, set)

/** 放在左侧第 6 格的范围按钮；漏斗与磁铁的提示文字不同 */
fun rangeTab(tooltipPrefix: String, get: () -> HopperRangeMode, set: (HopperRangeMode) -> Unit) =
    ModeTab(
        listOf(
            HopperRangeMode.RADIUS_LOWEST,
            HopperRangeMode.RADIUS_LOW,
            HopperRangeMode.RADIUS_MID,
            HopperRangeMode.RADIUS_HIGH,
            HopperRangeMode.RADIUS_HIGHEST,
            HopperRangeMode.CHUNK_MODE,
        ),
        { "hopper_range_mode_${rangeName(it)}" },
        { "$TIP${tooltipPrefix}_range_mode_${rangeName(it)}" },
        get,
        set,
        left = true,
        row = 5,
    )

private fun rangeName(mode: HopperRangeMode) =
    if (mode == HopperRangeMode.CHUNK_MODE) "chunk" else mode.name.removePrefix("RADIUS_").lowercase()

fun feederTab(get: () -> FeederMode, set: (FeederMode) -> Unit) =
    ModeTab(
        listOf(FeederMode.HUNGER_TO_EAT, FeederMode.NORMAL, FeederMode.SATURATION_KEEP, FeederMode.CRAZY),
        { "feeder_mode_${it.lower()}" },
        { "${TIP}feeder_mode_${it.lower()}" },
        get,
        set,
    )

fun fuzzyTab(get: () -> FuzzyMode, set: (FuzzyMode) -> Unit) =
    ModeTab(
        listOf(FuzzyMode.DISABLE, FuzzyMode.ENABLE),
        { if (it == FuzzyMode.DISABLE) "hopper_nbt_mode_allow" else "hopper_nbt_mode_deny" },
        { "${TIP}fuzzy_mode_${it.lower()}" },
        get,
        set,
    )

fun receiveTab(get: () -> ReceiveMode, set: (ReceiveMode) -> Unit) =
    ModeTab(
        listOf(ReceiveMode.STOP, ReceiveMode.OPEN),
        { if (it == ReceiveMode.STOP) "net_disable" else "net_absorb" },
        { "${TIP}receive_mode_${it.lower()}" },
        get,
        set,
    )

fun popTab(
    get: () -> PopMode,
    set: (PopMode) -> Unit,
    available: () -> Boolean = { true },
    unavailable: String? = null,
) =
    ModeTab(
        listOf(PopMode.OPEN, PopMode.STOP),
        { if (it == PopMode.OPEN) "popmode_up" else "popmode_down" },
        { if (it == PopMode.OPEN) "${TIP}popmode_on" else "${TIP}popmode_off" },
        get,
        set,
        available,
        unavailable,
    )

fun autoSortTab(get: () -> AutoSortMode, set: (AutoSortMode) -> Unit) =
    ModeTab(listOf(AutoSortMode.OPEN, AutoSortMode.STOP), { "sort_mode_${it.lower()}" }, { "${TIP}sort_mode_${it.lower()}" }, get, set)

const val POP_UNAVAILABLE = "${TIP}popmode_mounted_unavailable"
