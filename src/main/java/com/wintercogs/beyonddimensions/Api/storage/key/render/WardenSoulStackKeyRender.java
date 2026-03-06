package com.wintercogs.beyonddimensions.api.storage.key.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import com.wintercogs.beyonddimensions.api.storage.key.IStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.IStackRender;
import com.wintercogs.beyonddimensions.api.storage.key.impl.WardenSoulStackKey;
import com.wintercogs.beyonddimensions.util.StringFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.client.ClientTooltipFlag;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class WardenSoulStackKeyRender implements IStackRender
{
    public static final WardenSoulStackKeyRender INSTANCE = new WardenSoulStackKeyRender();

    // 粒子状态（静态，避免频繁分配）
    private static long lastCheckedForParticle = 0L;
    private static final List<GuiParticle> particleList = new ArrayList<>();

    private WardenSoulStackKeyRender()
    {
    }

    @Override
    public void render(GuiGraphics gui, IStackKey<?> key, int x, int y)
    {
        var mc = Minecraft.getInstance();
        if (mc.level == null) return;

        // 背景与心跳
        gui.pose().pushPose();
        ResourceLocation warden_rl = ResourceLocation.withDefaultNamespace("textures/entity/warden/warden.png");
        ResourceLocation warden_hear = ResourceLocation.withDefaultNamespace("textures/entity/warden/warden_heart.png");
        gui.blit(warden_rl, x, y, 12.0F, 14.0F, 16, 16, 128, 128);

        gui.pose().pushPose();
        float heart_timing_total = 30.0F;
        float heart_phase = 1.0F - (mc.level.getGameTime() % (long) heart_timing_total) / heart_timing_total;
        RenderSystem.setShaderColor(heart_phase, heart_phase, heart_phase, heart_phase);
        gui.blit(warden_hear, x - 1, y - 1, 11.0F, 13.0F, 18, 18, 128, 128);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        gui.pose().popPose();

        long rotation = mc.level.getGameTime() % 160L - 80L;

        gui.pose().pushPose();
        gui.pose().translate(x, y - 1, 100.0F);
        gui.pose().mulPose(Axis.YP.rotationDegrees((float) rotation));
        gui.blit(warden_rl, 0, 0, 91.0F, 13.0F, 17, 18, 128, 128);
        gui.pose().popPose();

        gui.pose().pushPose();
        gui.pose().translate(x + 16, y + 17, 100.0F);
        gui.pose().mulPose(Axis.ZP.rotationDegrees(180.0F));
        gui.pose().mulPose(Axis.YP.rotationDegrees((float) rotation));
        gui.blit(warden_rl, 0, 0, 91.0F, 13.0F, 17, 18, 128, 128);
        gui.pose().popPose();

        // 粒子层
        gui.pose().scale(0.75F, 0.75F, 0.75F);
        double spawnChance = 0.05;
        int xSize = 8;
        int ySize = 6;
        long now = mc.level.getGameTime();

        if (lastCheckedForParticle != now)
        {
            if (mc.level.random.nextDouble() <= spawnChance)
            {
                particleList.add(new GuiParticle(mc.level.random.nextInt(xSize),
                        ySize - mc.level.random.nextInt(3), now));
            }
            lastCheckedForParticle = now;
        }

        int ageTick = 3;
        if (now % ageTick == 0L)
        {
            particleList.removeIf(p -> (now - p.age) / ageTick > 10L);
        }

        gui.pose().translate(0.0F, 0.0F, 200.0F);
        for (GuiParticle p : particleList.reversed())
        {
            double age = (double) (now - p.age) / (double) ageTick;
            double extraY = (double) (ySize - 32) / 20.0F * age;
            int frame = Math.max(0, Math.min(10, (int) age));
            gui.blit(ResourceLocation.withDefaultNamespace("textures/particle/sculk_soul_" + frame + ".png"),
                    (int) ((x + p.x) * 1.3333334F),
                    (int) (((int) ((y + p.y) + extraY)) * 1.3333334F),
                    0.0F, 0.0F, 16, 16, 16, 16);
        }

        gui.pose().popPose();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
    }

    @Override
    public void renderAmount(GuiGraphics gui, long amount, int x, int y)
    {
        String text = getCountText(amount);
        if (text.isEmpty()) return;

        float scale = 0.666f;
        var pose = gui.pose();
        pose.pushPose();
        pose.translate(0, 0, 200);
        pose.scale(scale, scale, scale);
        RenderSystem.disableBlend();

        int w = Minecraft.getInstance().font.width(text);
        final int X = (int) ((x - 1 + 16.0f + 2.0f - w * 0.666f) / 0.666f);
        final int Y = (int) ((y - 1 + 16.0f - 5.0f * 0.666f) / 0.666f);
        gui.drawString(Minecraft.getInstance().font, text, X, Y, 0xFFFFFF);

        pose.popPose();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
    }

    @Override
    public String getCountText(long count)
    {
        if (count < 0) return "";
        return StringFormat.formatCount(count);
    }

    @Override
    public Component getDisplayName(IStackKey<?> key)
    {
        return WardenSoulStackKey.INSTANCE.getRenderStack().getName();
    }

    @Override
    public List<Component> getTooltipLines(IStackKey<?> key, long amount, Item.TooltipContext tooltipContext,
                                           @Nullable net.minecraft.world.entity.player.Player player,
                                           TooltipFlag tooltipFlag)
    {
        return List.of(
                getDisplayName(key),
                Component.translatable("istack.beyonddimensions.storage_num.long_type", amount)
        );
    }

    @Override
    public Optional<TooltipComponent> getTooltipImage(IStackKey<?> key)
    {
        return Optional.empty();
    }

    @Override
    public void renderTooltip(GuiGraphics gui, Font font, IStackKey<?> key, long amount, int mouseX, int mouseY)
    {
        var mc = Minecraft.getInstance();
        var ctx = mc.level != null ? Item.TooltipContext.of(mc.level) : Item.TooltipContext.EMPTY;
        gui.renderTooltip(
                mc.font,
                getTooltipLines(key, amount, ctx, mc.player,
                        ClientTooltipFlag.of(mc.options.advancedItemTooltips
                                ? TooltipFlag.Default.ADVANCED : TooltipFlag.Default.NORMAL)),
                getTooltipImage(key),
                ItemStack.EMPTY,
                mouseX, mouseY
        );
    }

    // 粒子结构
    private static final class GuiParticle
    {
        final int x;
        final int y;
        final long age;

        GuiParticle(int x, int y, long age)
        {
            this.x = x;
            this.y = y;
            this.age = age;
        }
    }
}