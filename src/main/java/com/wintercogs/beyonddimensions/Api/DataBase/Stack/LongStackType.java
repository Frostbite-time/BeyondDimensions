package com.wintercogs.beyonddimensions.Api.DataBase.Stack;

import com.mojang.blaze3d.systems.RenderSystem;
import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.Api.DataBase.LongType.LongType;
import com.wintercogs.beyonddimensions.Unit.BDMath;
import com.wintercogs.beyonddimensions.Unit.StringFormat;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.client.ClientTooltipFlag;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import org.apache.commons.lang3.text.WordUtils;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public abstract class LongStackType<T extends LongType<T>> implements IStackType<T>
{
    private static final long CUSTOM_MAX_STACK_SIZE = Long.MAX_VALUE; // 自定义堆叠大小

    public abstract ResourceLocation getTypeID();
    protected T stack;

    protected int hashCodeCache = 0; // 哈希码缓存
    protected boolean NeedRecalHash = true; // 指示什么时候需要重新计算哈希

    @Override
    public ResourceLocation getTypeId()
    {
        return getTypeID();
    }

    @Override
    public T getStack()
    {
        return stack;
    }

    @Override
    public void setStack(T stack)
    {
        this.stack = stack;
        NeedRecalHash = true;
    }

    @Override
    public Class<T> getStackClass()
    {
        return (Class<T>) stack.getClass();
    }

    @Override
    public Class<?> getSourceClass()
    {
        return stack.getClass();
    }


    @Override
    public String getModId()
    {
        return BeyondDimensions.MODID;
    }

    @Override
    public boolean isEmpty()
    {
        return stack.isEmpty();
    }

    @Override
    public boolean isEmptyStack()
    {
        return stack.isEmpty();
    }

    @Override
    public T copyStack()
    {
        return (T) stack.copy();
    }

    @Override
    public T copyStackWithCount(long count)
    {
        return (T)stack.copyWithAmount(count);
    }

    @Override
    public long getStackAmount()
    {
        return stack.getStackCount();
    }

    @Override
    public void setStackAmount(long amount)
    {
        stack.setStackCount(amount);
    }

    @Override
    public void grow(long amount)
    {
        stack.grow(amount);
    }

    @Override
    public void shrink(long amount)
    {
        stack.shrink(amount);
    }

    @Override
    public long getVanillaMaxStackSize()
    {
        return Long.MAX_VALUE; //决定了其在接口方块中的一次性最大容量
    }

    @Override
    public long getCustomMaxStackSize()
    {
        return CUSTOM_MAX_STACK_SIZE;
    }

    @Override
    public T splitStack(long amount)
    {
        if (amount <= 0) return (T) stack.getEmpty();

        int splitAmount = BDMath.clampLongToInt(Math.min(amount, stack.getStackCount()));
        T split = (T) stack.copy();
        split.setStackCount(splitAmount);
        shrink(splitAmount);
        return split;
    }

    @Override
    public boolean isSame(IStackType<T> other)
    {
        if(!other.getTypeId().equals(this.getTypeId()))
            return false;
        return stack.isSame(other.getStack());
    }

    @Override
    public boolean isSameTypeSameComponents(IStackType<T> other)
    {
        if(!other.getTypeId().equals(this.getTypeId()))
            return false;
        return stack.isSame(other.getStack());
    }

    @Override
    public void render(GuiGraphics gui, int x, int y)
    {
        if(stack.isEmpty())
            return;

        // 渲染图标
        var poseStack = gui.pose(); // 获取渲染的变换矩阵
        poseStack.pushPose(); // 保存矩阵状态

        Fluid fluid = Fluids.WATER;
        if(!fluid.isSame(Fluids.EMPTY))
        {
            IClientFluidTypeExtensions renderProperties = IClientFluidTypeExtensions.of(fluid);
            ResourceLocation fluidStill = renderProperties.getStillTexture();
            Optional<net.minecraft.client.renderer.texture.TextureAtlasSprite> fluidStillSprite = Optional.ofNullable(fluidStill)
                    .map(f -> Minecraft.getInstance()
                            .getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                            .apply(f)
                    )
                    .filter(s -> s.atlasLocation() != net.minecraft.client.renderer.texture.MissingTextureAtlasSprite.getLocation());
            if(fluidStillSprite.isPresent())
            {
                int fluidColor =  0xFF00FF00; // 绿色
                com.wintercogs.beyonddimensions.Render.IngredientRenderer.drawTiledSprite(gui,16,16,fluidColor,16,fluidStillSprite.get(),x,y);
            }
        }


        poseStack.popPose(); // 恢复矩阵状态，结束渲染

        // 渲染数量文本
        String countText = getCountText(getStackAmount());
        float scale = 0.666f; // 文本缩放因数
        var poseStackText = gui.pose();
        poseStackText.pushPose();
        poseStackText.translate(0,0,200); // 确保文本在顶层
        poseStackText.scale(scale,scale,scale); // 文本整体缩放，便于查看
        RenderSystem.disableBlend(); // 禁用混合渲染模式
        final int X = (int)(
                (x + -1 + 16.0f + 2.0f - Minecraft.getInstance().font.width(countText) * 0.666f)
                        * 1.0f / 0.666f
        );
        final int Y = (int)(
                (y + -1 + 16.0f - 5.0f * 0.666f)
                        * 1.0f / 0.666f
        );
        if(!stack.isEmpty())
            gui.drawString(Minecraft.getInstance().font, countText, X, Y, 0xFFFFFF);
        poseStackText.popPose();
    }

    @Override
    public String getCountText(long count)
    {
        if (count < 0) return "";
        return StringFormat.formatCount(count);
    }

    @Override
    public Component getDisplayName()
    {
        return stack.getName();
    }

    @Override
    public List<Component> getTooltipLines(Item.TooltipContext tooltipContext, @Nullable Player player, TooltipFlag tooltipFlag)
    {
        if(stack.isEmpty())
            return List.of(Component.empty());

        List<Component> tooltips = new ArrayList<>();

        Component displayName = getDisplayName();
        tooltips.add(displayName);

        Component modName;
        modName = Component.literal(WordUtils.capitalizeFully(BeyondDimensions.MODID.replace('_', ' '))).withStyle(ChatFormatting.BLUE).withStyle(ChatFormatting.ITALIC);
        tooltips.add(modName);

        tooltips.add(Component.translatable("istack.beyonddimensions.storage_num.long_type",getStackAmount()));
        return tooltips;
    }

    @Override
    public Optional<TooltipComponent> getTooltipImage()
    {
        return Optional.empty();
    }

    @Override
    public void renderTooltip(GuiGraphics gui, Font font, int mouseX, int mouseY)
    {
        var minecraft = Minecraft.getInstance();
        gui.renderTooltip(minecraft.font, this.getTooltipLines(Item.TooltipContext.of(minecraft.level),minecraft.player, ClientTooltipFlag.of(minecraft.options.advancedItemTooltips ? TooltipFlag.Default.ADVANCED : TooltipFlag.Default.NORMAL))
                , getTooltipImage(), ItemStack.EMPTY, mouseX, mouseY);
    }

    @Override
    public boolean equals(Object other)
    {
        if(other instanceof LongStackType otherStack)
        {
            return this.isSameTypeSameComponents(otherStack);
        }
        return false;
    }

    @Override
    public int hashCode() {
        // 基于物品类型和组件生成哈希码
        if(NeedRecalHash)
        {
            hashCodeCache = stack.hashCode();
            NeedRecalHash = false;
        }
        return hashCodeCache;
    }
}
