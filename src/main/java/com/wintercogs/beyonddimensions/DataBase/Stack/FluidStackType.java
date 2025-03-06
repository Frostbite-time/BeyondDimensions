package com.wintercogs.beyonddimensions.DataBase.Stack;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class FluidStackType implements IStackType<FluidStack>
{
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(BeyondDimensions.MODID, "stack_type/fluid");
    private static final long CUSTOM_MAX_STACK_SIZE = Long.MAX_VALUE-1; // 自定义堆叠大小

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
        return stack.copyWithAmount(Math.toIntExact(getStackAmount()));
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
        return new FluidStackType(stack.copyWithAmount(copycont));
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
        // 流体不属于原版物品，理论上不存在单槽最大上限
        return getCustomMaxStackSize();
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
        return FluidStack.isSameFluid(stack, other.getStack());
    }

    @Override
    public boolean isSameTypeSameComponents(IStackType<FluidStack> other)
    {
        if(!other.getTypeId().equals(this.getTypeId()))
            return false;
        return FluidStack.isSameFluidSameComponents(stack, other.getStack());
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf)
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
            FluidStack copy = stack.copyWithAmount(1);
            // 使用OPTIONAL_CODEC处理可能为空的情况
            FluidStack.OPTIONAL_STREAM_CODEC.encode(buf, copy);
        }
    }

    @Override
    public IStackType<FluidStack> deserialize(RegistryFriendlyByteBuf buf, ResourceLocation typeId)
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
        FluidStack stack = FluidStack.OPTIONAL_STREAM_CODEC.decode(buf)
                .copyWithAmount(count);
        return new FluidStackType(stack);
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider levelRegistryAccess)
    {
        CompoundTag tag = new CompoundTag();
        tag.putLong("Amount", getStackAmount());
        tag.put("Stack",stack.copyWithAmount(1).save(levelRegistryAccess));
        return tag;
    }

    @Override
    public IStackType<FluidStack> deserializeNBT(CompoundTag nbt, HolderLookup.Provider levelRegistryAccess)
    {
        FluidStackType stack =  new FluidStackType(FluidStack.parseOptional(levelRegistryAccess,nbt.getCompound("Stack")));
        stack.setStackAmount(nbt.getLong("Amount"));
        return stack;
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void render(net.minecraft.client.gui.GuiGraphics gui, int x, int y)
    {

    }

    @Override
    public String getCountText(long count)
    {
        return "";
    }

    @Override
    public Component getDisplayName()
    {
        return stack.getHoverName();
    }

    @Override
    public List<Component> getTooltipLines(Item.TooltipContext tooltipContext, @Nullable Player player, TooltipFlag tooltipFlag)
    {
        return List.of(Component.empty());
    }

    @Override
    public Optional<TooltipComponent> getTooltipImage()
    {
        return null;
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void renderTooltip(net.minecraft.client.gui.GuiGraphics gui, net.minecraft.client.gui.Font font, int mouseX, int mouseY)
    {

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
        return FluidStack.hashFluidAndComponents(stack.copyWithAmount(1));
    }
}
