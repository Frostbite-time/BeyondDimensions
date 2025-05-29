package com.wintercogs.beyonddimensions.DataBase.Stack;

import com.mojang.blaze3d.systems.RenderSystem;
import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.Unit.BDMath;
import com.wintercogs.beyonddimensions.Unit.StringFormat;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
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
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.commons.lang3.text.WordUtils;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FluidStackType implements IStackType<FluidStack>
{
    public static final ResourceLocation ID = ResourceLocation.tryBuild(BeyondDimensions.MODID, "stack_type/fluid");
    private static final long CUSTOM_MAX_STACK_SIZE = Long.MAX_VALUE; // 自定义堆叠大小

    private FluidStack stack;
    private long stackSize;

    // 创建空stack
    public FluidStackType()
    {
        stack = FluidStack.EMPTY;
        stackSize = 0;
    }

    // 创建给定stack
    public FluidStackType(FluidStack stack)
    {
        this.stack = stack;
        stackSize = stack.getAmount();
    }

    public FluidStackType(FluidStack stack, long stackSize)
    {
        this.stack = stack;
        this.stackSize = stackSize;
    }


    @Override
    public IStackType<FluidStack> fromObject(Object key, long amount, CompoundTag dataComponentPatch)
    {
        if(key instanceof Fluid fluid)
        {
            FluidStack fluidStack = new FluidStack(fluid, 1,dataComponentPatch);
            return new FluidStackType(fluidStack,amount);
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
        if(!(stack.getRawFluid() == Fluids.EMPTY))
            stack.setAmount(BDMath.clampLongToInt(stackSize));
        return stack;
    }

    @Override
    public void setStack(FluidStack stack)
    {
        this.stack = stack.copy();
        stackSize = stack.getAmount();
        if(!(this.stack.getRawFluid() == Fluids.EMPTY))
            this.stack.setAmount(1);
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
    public String getModId()
    {
        ResourceLocation key = ForgeRegistries.FLUIDS.getKey(stack.getFluid());
        return key != null ? key.getNamespace() : "unknown";
    }

    @Override
    public boolean isEmpty()
    {
        return stack.isEmpty() || stackSize <= 0;
    }

    @Override
    public boolean isEmptyStack()
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
        FluidStack copy = stack.copy();
        if(!(stack.getRawFluid() == Fluids.EMPTY))
            copy.setAmount(BDMath.clampLongToInt(stackSize));
        return copy;
    }

    @Override
    public FluidStack copyStackWithCount(long count)
    {
        FluidStack copy = stack.copy();
        if(!(stack.getRawFluid() == Fluids.EMPTY))
            copy.setAmount(BDMath.clampLongToInt(count));
        return copy;
    }

    @Override
    public IStackType<FluidStack> copy()
    {
        return new FluidStackType(stack.copy(), stackSize);
    }

    @Override
    public IStackType<FluidStack> copyWithCount(long count)
    {
        return new FluidStackType(stack.copy(),count);
    }

    @Override
    public long getStackAmount()
    {
        return stackSize;
    }

    @Override
    public void setStackAmount(long amount)
    {
        stackSize = amount;
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
        return Math.min(64000L, getCustomMaxStackSize());
    }

    @Override
    public long getCustomMaxStackSize()
    {
        return CUSTOM_MAX_STACK_SIZE;
    }

    @Override
    public FluidStack splitStack(long amount)
    {
        if (amount <= 0 || isEmpty()) return FluidStack.EMPTY;

        // 计算可分割的数量
        int splitAmount = BDMath.clampLongToInt(Math.min(amount, stackSize));
        FluidStack split = stack.copy();
        split.setAmount(splitAmount);
        shrink(splitAmount);
        return split;
    }

    @Override
    public IStackType<FluidStack> split(long amount)
    {
        if (amount <= 0 || isEmpty()) return new FluidStackType();

        // 计算可分割的数量
        long splitAmount = Math.min(amount, stackSize);
        FluidStack split = stack.copy();
        shrink(splitAmount);
        return new FluidStackType(split,splitAmount);
    }

    @Override
    public boolean isSame(IStackType<FluidStack> other)
    {
        if(!other.getTypeId().equals(this.getTypeId()))
            return false;
        return stack.getFluid() == other.copyStackWithCount(1).getFluid();
    }

    @Override
    public boolean isSameTypeSameComponents(IStackType<FluidStack> other)
    {
        if(!other.getTypeId().equals(this.getTypeId()))
            return false;
        return stack.isFluidEqual(other.copyStackWithCount(1));
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
            buf.writeVarLong(stackSize);
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
        long count = buf.readVarLong();
        // 使用OPTIONAL_CODEC解码
        FluidStack stack = new FluidStack(FluidStack.readFromPacket(buf),1);
        return new FluidStackType(stack,count);
    }

    @Override
    public CompoundTag serializeNBT()
    {
        CompoundTag tag = new CompoundTag();
        tag.putLong("Amount", getStackAmount());
        tag.put("Stack",new FluidStack(stack,1).writeToNBT(new CompoundTag()));
        return tag;
    }

    @Override
    public IStackType<FluidStack> deserializeNBT(CompoundTag nbt)
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
        String countText = getCountText(stackSize);
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
        return StringFormat.formatBucket(count);
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

        tooltips.add(Component.translatable("istack.beyonddimensions.storage_num.fluid",getStackAmount()));
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
