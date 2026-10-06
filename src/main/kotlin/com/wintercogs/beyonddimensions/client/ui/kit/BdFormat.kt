package com.wintercogs.beyonddimensions.client.ui.kit

import java.util.*

private val UNITS = listOf("K" to 1e3, "M" to 1e6, "G" to 1e9, "T" to 1e12, "P" to 1e15, "E" to 1e18)

/** 格子里的数量：最多四个字符，如 999、1.2K、86K、1.3M */
fun formatCompact(amount: Long): String {
    if (amount < 1_000) return amount.toString()
    val (unit, scale) = UNITS.last { amount >= it.second }
    val value = amount / scale
    return if (value < 10) {
        // 截断而不是四舍五入，避免 9.99K 显示成 10.0K
        val tenths = (value * 10).toLong()
        if (tenths % 10 == 0L) "${tenths / 10}$unit" else "${tenths / 10}.${tenths % 10}$unit"
    } else {
        "${value.toLong()}$unit"
    }
}

/** 单独展示的大数值：三位有效数字，如 1.26M、12.6M、126M */
fun formatReadout(amount: Long): String {
    if (amount < 1_000) return amount.toString()
    val (unit, scale) = UNITS.last { amount >= it.second }
    val value = amount / scale
    val digits =
        when {
            value < 10 -> 2
            value < 100 -> 1
            else -> 0
        }
    return String.format(Locale.ROOT, "%.${digits}f%s", value, unit)
}

/** 完整数值，带千位分隔符 */
fun formatExact(amount: Long): String = String.format(Locale.ROOT, "%,d", amount)
