package com.wintercogs.beyonddimensions.api.storage.key.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.wintercogs.beyonddimensions.api.storage.key.IStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.IStackRender;
import com.wintercogs.beyonddimensions.api.storage.key.impl.MobStackKey;
import com.wintercogs.beyonddimensions.util.StringFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.client.ClientTooltipFlag;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public final class MobStackKeyRender implements IStackRender
{
    public static final MobStackKeyRender INSTANCE = new MobStackKeyRender();

    private MobStackKeyRender()
    {
    }

    private ItemStack icon(IStackKey<?> key)
    {
        if (key instanceof MobStackKey mob)
        {
            SpawnEggItem egg = SpawnEggItem.byId(mob.entityType());
            if (egg != null) return new ItemStack(egg);
        }
        return new ItemStack(Items.LEAD);
    }

    @Override
    public void render(GuiGraphics gui, IStackKey<?> key, int x, int y)
    {
        gui.renderFakeItem(icon(key), x, y);
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
        int width = Minecraft.getInstance().font.width(text);
        int renderX = (int) ((x - 1 + 18.0f - width * scale) / scale);
        int renderY = (int) ((y - 1 + 16.0f - 5.0f * scale) / scale);
        gui.drawString(Minecraft.getInstance().font, text, renderX, renderY, 0xFFFFFF);
        pose.popPose();
    }

    @Override
    public String getCountText(long count)
    {
        return count < 0 ? "" : StringFormat.formatCount(count);
    }

    @Override
    public Component getDisplayName(IStackKey<?> key)
    {
        return key instanceof MobStackKey mob ? mob.entityType().getDescription() : Component.empty();
    }

    @Override
    public List<Component> getTooltipLines(IStackKey<?> key, long amount, Item.TooltipContext context,
                                           @Nullable net.minecraft.world.entity.player.Player player,
                                           TooltipFlag flag)
    {
        return List.of(getDisplayName(key),
                Component.translatable("istack.beyonddimensions.storage_num.long_type", amount));
    }

    @Override
    public Optional<TooltipComponent> getTooltipImage(IStackKey<?> key)
    {
        return Optional.empty();
    }

    @Override
    public void renderTooltip(GuiGraphics gui, Font font, IStackKey<?> key, long amount, int mouseX, int mouseY)
    {
        var minecraft = Minecraft.getInstance();
        var context = minecraft.level == null ? Item.TooltipContext.EMPTY : Item.TooltipContext.of(minecraft.level);
        gui.renderTooltip(minecraft.font,
                getTooltipLines(key, amount, context, minecraft.player,
                        ClientTooltipFlag.of(minecraft.options.advancedItemTooltips
                                ? TooltipFlag.Default.ADVANCED : TooltipFlag.Default.NORMAL)),
                getTooltipImage(key), icon(key), mouseX, mouseY);
    }
}
