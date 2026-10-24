package com.wintercogs.beyonddimensions.api.ui.page

/**
 * 一页的大小，单位 dp；窗口在切换页面时以动画变到这一页的大小。
 * 另定大小的页比竖条上的页签矮时，窗口仍留出放下页签的高度（不超过主页面的高度），页面下方空着
 */
sealed interface BdPageSize {
    /** 与主页面一样大，内容放不下时在页内滚动 */
    data object Main : BdPageSize

    /** 固定宽高；屏幕放不下时收进屏幕，页内滚动 */
    data class Fixed(val width: Int, val height: Int) : BdPageSize

    /**
     * 按内容的大小，不超过 [maxWidth]、[maxHeight]（为 null 时只受屏幕限制），超出部分在页内滚动。
     * 内容的外层要有确定的大小，不能用 fillMaxSize 或 weight；内容变大变小时窗口随之变化
     */
    data class Content(val maxWidth: Int? = null, val maxHeight: Int? = null) : BdPageSize
}
