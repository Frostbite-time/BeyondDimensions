package com.wintercogs.beyonddimensions.Mixin;

import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(GuiGraphics.class)
public class blitSpriteMixin
{

    /**
     * 修复九宫格绘制时，右中部分的会错误使用左中部分的宽度
     * <p>
     * Args 布局：
     * 0 TextureAtlasSprite sprite
     * 1 int x
     * 2 int y
     * 3 int z
     * 4 int width      ← 要改的就是它
     * 5 int height
     * 6 int u
     * 7 int v
     * 8 int regionW    (= j，右边框宽)
     * 9 int regionH
     * 10 int totalW
     * 11 int totalH
     */
    @ModifyArgs(
            method = "blitNineSlicedSprite",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;" +
                            "blitTiledSprite(Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;" +
                            "IIIIIIIIIII)V",   // 11 个 I
                    ordinal = 6 // 修改第六次出现时的
            )
    )
    private void beyonddimensions$fixRightMiddleWidth(Args args)
    {
        int j = args.get(8);           // source regionW == 右边框宽度 j
        args.set(4, j);                // 把目标绘制宽度 (index 4) 设为 j
    }
}
