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
 * 窗口背后的模糊：按原版的“菜单背景模糊度”模糊整个游戏画面，强度跟随 [BdScreenFrame] 的进退场。
 * 界面关闭后的退场期间由 [layer] 继续模糊
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

    /** BD 界面在绘制自己之前调用；两个界面交替时取较强的一方 */
    fun behindScreen(graphics: GuiGraphics, partialTick: Float) =
        blur(graphics, partialTick, frames.maxOfOrNull { it.visibility } ?: 0f)

    /** 放在 CompixelUI 的退场层之下：BD 界面关闭后的退场期间继续模糊 */
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
