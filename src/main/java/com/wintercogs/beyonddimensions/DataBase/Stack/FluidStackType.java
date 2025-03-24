package com.wintercogs.beyonddimensions.DataBase.Stack;

import com.mojang.blaze3d.systems.RenderSystem;
import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.Unit.StringFormat;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.tooltip.BundleTooltip;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModList;
import org.apache.commons.lang3.text.WordUtils;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FluidStackType implements IStackType<FluidStack>
{
    public static final ResourceLocation ID = ResourceLocation.tryBuild(BeyondDimensions.MODID, "stack_type/fluid");
    private static final long CUSTOM_MAX_STACK_SIZE = Integer.MAX_VALUE; // 自定义堆叠大小

    private FluidStack stack;

    // 创建空stack
    public FluidStackType()
    {
        stack = FluidStack.EMPTY;
    }

    // 创建给定stack
    public FluidStackType(FluidStack stack)
    {
        this.stack = stack;
    }


    @Override
    public IStackType<FluidStack> fromObject(Object key, long amount, CompoundTag dataComponentPatch)
    {
        if(key instanceof Fluid fluid)
        {
            FluidStack fluidStack = new FluidStack(fluid, (int) amount,dataComponentPatch);
            return new FluidStackType(fluidStack);
        }
        return null;
    }

    @Override
    public ResourceLocation getTypeId()
    {
        return this.ID;
    }

    @Override
    public IStackType<FluidStack> getEmpty()
    {
        return new FluidStackType();
    }

    @Override
    public FluidStack getStack()
    {
        return stack;
    }

    @Override
    public void setStack(FluidStack stack)
    {
        this.stack = stack.copy();
    }

    @Override
    public Class<FluidStack> getStackClass()
    {
        return FluidStack.class;
    }

    @Override
    public Class<?> getSourceClass()
    {
        return Fluid.class;
    }

    @Override
    public Object getSource()
    {
        return FluidStack.EMPTY.getFluid();
    }

    @Override
    public boolean isEmpty()
    {
        return stack.isEmpty();
    }

    @Override
    public FluidStack getEmptyStack()
    {
        return FluidStack.EMPTY;
    }

    @Override
    public FluidStack copyStack()
    {
        return stack.copy();
    }

    @Override
    public FluidStack copyStackWithCount(long count)
    {
        FluidStack copy = stack.copy();
        // 处理long到int的转换安全
        if (count > Integer.MAX_VALUE) {
            //throw new IllegalArgumentException("ItemStack count exceeds maximum value: " + count);
            copy.setAmount(Integer.MAX_VALUE);
            return copy;
        }
        copy.setAmount((int) count);
        return copy;
    }

    @Override
    public IStackType<FluidStack> copy()
    {
        return new FluidStackType(stack.copy());
    }

    @Override
    public IStackType<FluidStack> copyWithCount(long count)
    {
        int copycont;
        // 处理long到int的转换安全
        if (count > Integer.MAX_VALUE) {
            //throw new IllegalArgumentException("ItemStack count exceeds maximum value: " + count);
            copycont = Integer.MAX_VALUE;
        }
        else
        {
            copycont = (int) count;
        }
        return new FluidStackType(new FluidStack(stack,copycont));
    }

    @Override
    public long getStackAmount()
    {
        return stack.getAmount();
    }

    @Override
    public void setStackAmount(long amount)
    {
        // 处理long到int的转换安全
        if (amount > Integer.MAX_VALUE) {
            //throw new IllegalArgumentException("ItemStack count exceeds maximum value: " + count);
            stack.setAmount(Integer.MAX_VALUE);
            return;
        }
        stack.setAmount((int) amount);
    }

    @Override
    public void grow(long amount)
    {
        setStackAmount(getStackAmount()+amount);
    }

    @Override
    public void shrink(long amount)
    {
        grow(-amount);
    }

    @Override
    public long getVanillaMaxStackSize()
    {
        // 流体不属于原版物品，理论上不存在单槽最大上限,此处以64桶为单槽最大单位
        return 64000;
    }

    @Override
    public long getCustomMaxStackSize()
    {
        return CUSTOM_MAX_STACK_SIZE;
    }

    @Override
    public FluidStack splitStack(long amount)
    {
        if (amount <= 0) return FluidStack.EMPTY;

        // 计算可分割的数量
        int splitAmount = (int) Math.min(amount, stack.getAmount());
        FluidStack split = stack.copy();
        split.setAmount(splitAmount);
        stack.shrink(splitAmount);
        return split;
    }

    @Override
    public IStackType<FluidStack> split(long amount)
    {
        if (amount <= 0) return new FluidStackType();

        // 计算可分割的数量
        int splitAmount = (int) Math.min(amount, stack.getAmount());
        FluidStack split = stack.copy();
        split.setAmount(splitAmount);
        stack.shrink(splitAmount);
        return new FluidStackType(split);
    }

    @Override
    public boolean isSame(IStackType<FluidStack> other)
    {
        if(!other.getTypeId().equals(this.getTypeId()))
            return false;
        return stack.getFluid() == other.getStack().getFluid();
    }

    @Override
    public boolean isSameTypeSameComponents(IStackType<FluidStack> other)
    {
        if(!other.getTypeId().equals(this.getTypeId()))
            return false;
        return stack.isFluidEqual(other.getStack());
    }

    @Override
    public void serialize(FriendlyByteBuf buf)
    {
        // 始终写入类型ID
        buf.writeResourceLocation(getTypeId());

        // 写入是否存在物品的标志
        boolean hasItem = !stack.isEmpty();
        buf.writeBoolean(hasItem);

        if (hasItem) {
            // 写入数量
            buf.writeVarInt(stack.getAmount());
            // 使用副本避免修改原堆栈
            FluidStack copy = new FluidStack(stack,1);
            // 使用OPTIONAL_CODEC处理可能为空的情况
            copy.writeToPacket(buf);
        }
    }

    @Override
    public IStackType<FluidStack> deserialize(FriendlyByteBuf buf, ResourceLocation typeId)
    {
        if (!typeId.equals(getTypeId())) {
            return null;// 表示未能读取任何类型
        }

        // 读取是否存在物品的标志
        boolean hasItem = buf.readBoolean();
        if (!hasItem) {
            return new FluidStackType(FluidStack.EMPTY);
        }

        // 读取数量
        int count = buf.readVarInt();
        // 使用OPTIONAL_CODEC解码
        FluidStack stack = new FluidStack(FluidStack.readFromPacket(buf),count);
        return new FluidStackType(stack);
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider levelRegistryAccess)
    {
        CompoundTag tag = new CompoundTag();
        tag.putLong("Amount", getStackAmount());
        tag.put("Stack",new FluidStack(stack,1).writeToNBT(new CompoundTag()));
        return tag;
    }

    @Override
    public IStackType<FluidStack> deserializeNBT(CompoundTag nbt, HolderLookup.Provider levelRegistryAccess)
    {
        FluidStackType stack =  new FluidStackType(FluidStack.loadFluidStackFromNBT(nbt.getCompound("Stack")));
        stack.setStackAmount(nbt.getLong("Amount"));
        return stack;
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void render(net.minecraft.client.gui.GuiGraphics gui, int x, int y)
    {
        // 渲染图标
        var poseStack = gui.pose(); // 获取渲染的变换矩阵
        poseStack.pushPose(); // 保存矩阵状态

        Fluid fluid = stack.getFluid();
        if(!fluid.isSame(Fluids.EMPTY))
        {
            IClientFluidTypeExtensions renderProperties = IClientFluidTypeExtensions.of(fluid);
            ResourceLocation fluidStill = renderProperties.getStillTexture(stack);
            Optional<net.minecraft.client.renderer.texture.TextureAtlasSprite> fluidStillSprite = Optional.ofNullable(fluidStill)
                    .map(f -> Minecraft.getInstance()
                            .getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                            .apply(f)
                    )
                    .filter(s -> s.atlasLocation() != net.minecraft.client.renderer.texture.MissingTextureAtlasSprite.getLocation());
            if(fluidStillSprite.isPresent())
            {
                int fluidColor = IClientFluidTypeExtensions.of(stack.getFluid()).getTintColor();
                com.wintercogs.beyonddimensions.Render.IngredientRenderer.drawTiledSprite(gui,16,16,fluidColor,16,fluidStillSprite.get(),x,y);
            }
        }


        poseStack.popPose(); // 恢复矩阵状态，结束渲染

        // 渲染数量文本
        String countText = getCountText(stack.getAmount());
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
        gui.drawString(Minecraft.getInstance().font,
                countText,
                X,
                Y,
                0xFFFFFF);
        poseStackText.popPose();
    }

    @Override
    public String getCountText(long count)
    {
        if (count <= 0) return "";
        return StringFormat.formatCount(count);
    }

    @Override
    public Component getDisplayName()
    {
        return stack.getDisplayName();
    }

    @Override
    public List<Component> getTooltipLines(@Nullable Player player, TooltipFlag tooltipFlag)
    {
        if(stack.isEmpty())
            return List.of(Component.empty());

        List<Component> tooltips = new ArrayList<>();
        Fluid fluid = stack.getFluid();

        Component displayName = getDisplayName();
        tooltips.add(displayName);

        ResourceLocation resourceLocation =  BuiltInRegistries.FLUID.getKey(fluid);
        if (resourceLocation != null) {
            if (tooltipFlag.isAdvanced()) {
                MutableComponent advancedId = Component.literal(resourceLocation.toString())
                        .withStyle(ChatFormatting.DARK_GRAY);
                tooltips.add(advancedId);
            }
            Optional<? extends ModContainer> container = ModList.get().getModContainerById(resourceLocation.getNamespace());
            Component modName;
            if(container.isPresent())
            {
                modName = Component.literal(container.get().getModInfo().getDisplayName()).withStyle(ChatFormatting.BLUE).withStyle(ChatFormatting.ITALIC);
            }
            else
            {
                container = ModList.get().getModContainerById(resourceLocation.getNamespace().replace('_', '-'));
                if (container.isPresent()) {
                    modName = Component.literal(container.get().getModInfo().getDisplayName()).withStyle(ChatFormatting.BLUE).withStyle(ChatFormatting.ITALIC);
                }
                else
                {
                    modName = Component.literal(WordUtils.capitalizeFully(resourceLocation.getNamespace().replace('_', ' '))).withStyle(ChatFormatting.BLUE).withStyle(ChatFormatting.ITALIC);
                }
            }
            tooltips.add(modName);
        }

        tooltips.add(Component.literal("已存储:"+getStackAmount()+"mB"));
        return tooltips;
    }

    @Override
    public Optional<TooltipComponent> getTooltipImage()
    {
        return Optional.empty();
        //return !stack.has(DataComponents.HIDE_TOOLTIP) && !stack.has(DataComponents.HIDE_ADDITIONAL_TOOLTIP) ? Optional.ofNullable((BundleContents)stack.get(DataComponents.BUNDLE_CONTENTS)).map(BundleTooltip::new) : Optional.empty();
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void renderTooltip(net.minecraft.client.gui.GuiGraphics gui, net.minecraft.client.gui.Font font, int mouseX, int mouseY)
    {
        var minecraft = Minecraft.getInstance();
        gui.renderTooltip(minecraft.font, this.getTooltipLines(minecraft.player, minecraft.options.advancedItemTooltips ? TooltipFlag.Default.ADVANCED : TooltipFlag.Default.NORMAL)
                , getTooltipImage(), ItemStack.EMPTY, mouseX, mouseY);
    }

    @Override
    public boolean equals(Object other)
    {
        if(other instanceof FluidStackType otherStack)
        {
            return this.isSameTypeSameComponents(otherStack);
        }
        return false;
    }

    @Override
    public int hashCode() {
        // 基于物品类型和组件生成哈希码
        return stack.hashCode();
    }
}
