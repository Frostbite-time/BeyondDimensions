package com.wintercogs.beyonddimensions.client.ui.kit

import androidx.compose.runtime.*
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.LayeredDraw
import java.util.concurrent.CopyOnWriteArrayList

/** BD 的界面：绘制自己之前调用 [BdBackdrop.behindScreen] */
interface BdScreen

/**
 * 窗口背后的模糊：像原版菜单一样按“菜单背景模糊度”模糊整个游戏画面，这项设置关闭时不模糊。
 * 强度跟随 [BdScreenFrame] 的进退场：打开时随窗口淡入；关闭后 CompixelUI 还在游戏画面上播放退场，
 * 这段时间由 [layer] 继续模糊，随退场减弱。
 */
object BdBackdrop {
    /** 一个界面框架的进退场，由 Compose 线程写入、游戏线程读取 */
    private class Frame {
        @Volatile
        var visibility = 0f

        @Volatile
        var exiting = false
    }

    private val frames = CopyOnWriteArrayList<Frame>()

    /** 在界面框架的过渡里调用：[visibility] 从 0 到 1，[exiting] 表示界面已经关闭、正在退场 */
    @Composable
    fun Follow(visibility: () -> Float, exiting: () -> Boolean) {
        val frame = remember { Frame() }
        DisposableEffect(frame) {
            frames += frame
            onDispose { frames -= frame }
        }
        LaunchedEffect(frame) {
            snapshotFlow { visibility() to exiting() }.collect { (value, leaving) ->
                frame.visibility = value
                frame.exiting = leaving
            }
        }
    }

    /** BD 界面在绘制自己之前调用；从一个 BD 界面换到另一个时，旧界面的退场与新界面的进场取较强的一方 */
    fun behindScreen(graphics: GuiGraphics, partialTick: Float) =
        blur(graphics, partialTick, frames.maxOfOrNull { it.visibility } ?: 0f)

    /**
     * 放在 CompixelUI 的退场层之下：BD 界面关闭后、退场播放期间继续模糊。
     * 被配方查看器等界面覆盖的 BD 界面不在退场，不参与。
     */
    val layer = LayeredDraw.Layer { graphics, deltaTracker ->
        if (Minecraft.getInstance().screen !is BdScreen) {
            val strength = frames.filter { it.exiting }.maxOfOrNull { it.visibility }
            if (strength != null) blur(graphics, deltaTracker.getGameTimeDeltaPartialTick(false), strength)
        }
    }

    private fun blur(graphics: GuiGraphics, partialTick: Float, strength: Float) {
        val minecraft = Minecraft.getInstance()
        val effect = minecraft.gameRenderer.blurEffect ?: return
        val radius = minecraft.options.menuBackgroundBlurriness().get() * strength
        if (radius < 1f) return
        graphics.flush()
        // 与原版相同：先关掉深度测试，否则模糊结果会挡住之后绘制的界面（NeoForge #1504）
        RenderSystem.disableDepthTest()
        effect.setUniform("Radius", radius)
        effect.process(partialTick)
        minecraft.mainRenderTarget.bindWrite(false)
    }
}
