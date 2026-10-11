package com.wintercogs.beyonddimensions.api.storage.key.render;

import com.wintercogs.beyonddimensions.api.storage.key.IStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.IStackRender;
import com.wintercogs.beyonddimensions.api.storage.key.impl.FluidStackKey;
import com.wintercogs.beyonddimensions.util.IngredientRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.client.ClientTooltipFlag;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FluidStackKeyRender implements IStackRender
{
    public static final FluidStackKeyRender INSTANCE = new FluidStackKeyRender();

    @Override
    public void render(GuiGraphics gui, IStackKey<?> key, int x, int y)
    {
        if (key instanceof FluidStackKey fluidKey)
        {
            // 渲染流体图标（16×16）
            var pose = gui.pose();
            pose.pushPose();

            FluidStack stack = fluidKey.getRenderStack();
            if (!stack.isEmpty())
            {
                var fluid = stack.getFluid();
                IClientFluidTypeExtensions props = IClientFluidTypeExtensions.of(fluid);
                ResourceLocation still = props.getStillTexture(stack);
                // still在此处有null风险，不可移除null检查
                TextureAtlasSprite sprite = still == null ? null :
                        Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(still);

                if (sprite != null && sprite.atlasLocation() != MissingTextureAtlasSprite.getLocation())
                {
                    int tint = IClientFluidTypeExtensions.of(fluid).getTintColor(stack);
                    IngredientRenderer.drawTiledSprite(gui, 16, 16, tint, 16, sprite, x, y);
                }
            }

            pose.popPose();
        }

    }

    @Override
    public Component getDisplayName(IStackKey<?> key)
    {
        if (key instanceof FluidStackKey fluidKey)
        {
            FluidStack stack = fluidKey.getRenderStack();
            return stack.isEmpty() ? Component.empty() : stack.getHoverName();
        }
        return Component.empty();
    }

    @Override
    public List<Component> getTooltipLines(IStackKey<?> key, long amount, Item.TooltipContext tooltipContext,
                                           @Nullable net.minecraft.world.entity.player.Player player,
                                           TooltipFlag tooltipFlag)
    {
        List<Component> lines = new ArrayList<>();
        lines.add(getDisplayName(key));
        lines.add(Component.translatable("istack.beyonddimensions.storage_num.fluid", amount));
        return lines;
    }

    @Override
    public Optional<TooltipComponent> getTooltipImage(IStackKey<?> key)
    {
        // 流体默认无额外 TooltipComponent
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
                        ClientTooltipFlag.of(mc.options.advancedItemTooltips ? TooltipFlag.Default.ADVANCED : TooltipFlag.Default.NORMAL)),
                getTooltipImage(key),
                ItemStack.EMPTY,
                mouseX, mouseY
        );
    }
}