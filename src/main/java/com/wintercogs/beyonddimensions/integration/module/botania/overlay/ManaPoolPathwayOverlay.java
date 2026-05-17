package com.wintercogs.beyonddimensions.integration.module.botania.overlay;

import com.mojang.blaze3d.systems.RenderSystem;
import com.wintercogs.beyonddimensions.integration.module.botania.block.entity.ManaPoolPathwayBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.client.event.RenderGuiEvent;
import org.lwjgl.opengl.GL11;

public class ManaPoolPathwayOverlay
{
    public static void onRenderGui(RenderGuiEvent.Post e)
    {
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.options.hideGui) return;

        HitResult hit = mc.hitResult;
        if (!(hit instanceof BlockHitResult bhr)) return;

        BlockEntity be = mc.level.getBlockEntity(bhr.getBlockPos());
        if (!(be instanceof ManaPoolPathwayBlockEntity pool))
            return;

        ItemStack held = mc.player.getMainHandItem();
        if (held.isEmpty() || held.getItem() instanceof vazkii.botania.common.item.WandOfTheForestItem)
            return; // 法杖交由植魔自己的逻辑处理

        BlockState below = mc.level.getBlockState(bhr.getBlockPos().below());
        vazkii.botania.api.recipe.ManaInfusionRecipe recipe = pool.getMatchingRecipe(held, below);
        if (recipe == null) return;

        GuiGraphics gui = e.getGuiGraphics();
        int x = mc.getWindow().getGuiScaledWidth() / 2 - 11;
        int y = mc.getWindow().getGuiScaledHeight() / 2 + 10;

        // u = 0表示始终绘制成功的贴图，失败贴图的u为22
        int u = 0;
        int v = 8;

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        // 直接复用 Botania 的贴图和绘制小工具
        vazkii.botania.client.core.helper.RenderHelper.drawTexturedModalRect(gui, vazkii.botania.client.gui.HUDHandler.manaBar, x, y, u, v, 22, 15);
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);

        gui.renderItem(held, x - 20, y);

        ItemStack result = recipe.getResultItem(mc.level.registryAccess());
        gui.renderItem(result, x + 26, y);
        gui.renderItemDecorations(mc.font, result, x + 26, y);

        RenderSystem.disableBlend();
    }
}
