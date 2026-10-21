package com.wintercogs.beyonddimensions.client.ui.kit

import androidx.compose.runtime.*
import com.mojang.blaze3d.systems.RenderSystem
import com.wintercogs.beyonddimensions.BeyondDimensions
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.renderer.PostChain
import net.minecraftforge.client.gui.overlay.IGuiOverlay
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.roundToInt

/** BD 的界面：绘制自己之前调用 [BdBackdrop.behindScreen] */
interface BdScreen

/**
 * 窗口背后的模糊：模糊整个游戏画面，强度跟随 [BdScreenFrame] 的进退场。
 * 界面关闭后的退场期间由 [layer] 继续模糊。1.20.1 没有菜单背景模糊，按新版本的默认模糊度、用原版的模糊着色器处理
 */
object BdBackdrop {
    /** 新版本“菜单背景模糊度”的默认值 */
    private const val BLURRINESS = 5f

    /** 一个界面框架的进退场，由 Compose 线程写入、游戏线程读取 */
    private class Frame {
        @Volatile
        var visibility = 0f

        @Volatile
        var exiting = false
    }

    private val frames = CopyOnWriteArrayList<Frame>()

    // 首次模糊时载入，资源重载后丢弃；载入失败时直到下次重载都不再模糊
    private var effect: PostChain? = null
    private var effectFailed = false
    private var effectWidth = -1
    private var effectHeight = -1

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
    val layer = IGuiOverlay { _, graphics, partialTick, _, _ ->
        if (Minecraft.getInstance().screen !is BdScreen) {
            val strength = frames.filter { it.exiting }.maxOfOrNull { it.visibility }
            if (strength != null) blur(graphics, partialTick, strength)
        }
    }

    /** 资源重载后调用：丢弃载入的着色器，下次模糊时重新载入 */
    fun reload() {
        effect?.close()
        effect = null
        effectFailed = false
    }

    private fun blur(graphics: GuiGraphics, partialTick: Float, strength: Float) {
        val minecraft = Minecraft.getInstance()
        // 与新版本相同，半径取整：原版着色器按整像素取样
        val radius = (BLURRINESS * strength).roundToInt()
        if (radius < 1) return
        val effect = effect(minecraft) ?: return
        graphics.flush()
        // 先关掉深度测试，否则模糊结果会挡住之后绘制的界面
        RenderSystem.disableDepthTest()
        for (pass in effect.passes) pass.effect.safeGetUniform("Radius").set(radius.toFloat())
        effect.process(partialTick)
        minecraft.mainRenderTarget.bindWrite(false)
    }

    private fun effect(minecraft: Minecraft): PostChain? {
        val target = minecraft.mainRenderTarget
        val loaded = effect ?: if (effectFailed) return null else try {
            PostChain(minecraft.textureManager, minecraft.resourceManager, target, BeyondDimensions.makeId("shaders/post/backdrop_blur.json"))
                .also { effect = it; effectWidth = -1 }
        } catch (e: Exception) {
            BeyondDimensions.LOGGER.warn("Failed to load the backdrop blur shader", e)
            effectFailed = true
            return null
        }
        if (effectWidth != target.width || effectHeight != target.height) {
            loaded.resize(target.width, target.height)
            effectWidth = target.width
            effectHeight = target.height
        }
        return loaded
    }
}
