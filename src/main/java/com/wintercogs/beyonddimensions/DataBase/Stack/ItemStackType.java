package com.wintercogs.beyonddimensions.DataBase.Stack;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.Unit.StringFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import javax.annotation.Nullable;
import java.util.List;

public class ItemStackType implements IStackType<ItemStack> {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(BeyondDimensions.MODID, "stack_type/item");
    private static final long CUSTOM_MAX_STACK_SIZE = Long.MAX_VALUE-1; // 自定义堆叠大小

    private ItemStack stack;

    public ItemStackType()
    {
        stack = ItemStack.EMPTY;
    }

    public ItemStackType(ItemStack stack)
    {
        this.stack = stack;
    }

    @Override
    public ItemStack getStack()
    {
        return stack.copyWithCount(Math.toIntExact(getStackAmount()));
    }

    @Override
    public void setStack(ItemStack stack)
    {
        this.stack = stack.copy();
    }

    @Override
    public ResourceLocation getTypeId() {
        return ID;
    }

    @Override
    public IStackType<ItemStack> getEmpty()
    {
        return new ItemStackType();
    }

    @Override
    public Class<ItemStack> getStackClass() {
        return ItemStack.class;
    }

    @Override
    public boolean isEmpty() {
        return stack.isEmpty();
    }

    @Override
    public ItemStack getEmptyStack() {
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack copyStack() {
        return stack.copy();
    }

    @Override
    public ItemStack copyStackWithCount(long count) {
        ItemStack copy = stack.copy();
        // 处理long到int的转换安全
        if (count > Integer.MAX_VALUE) {
            //throw new IllegalArgumentException("ItemStack count exceeds maximum value: " + count);
            copy.setCount(Integer.MAX_VALUE);
            return copy;
        }
        copy.setCount((int) count);
        return copy;
    }

    @Override
    public IStackType<ItemStack> copy()
    {
        return new ItemStackType(stack.copy());
    }

    @Override
    public IStackType<ItemStack> copyWithCount(long count)
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
        return new ItemStackType(stack.copyWithCount(copycont));
    }

    @Override
    public long getStackAmount() {
        return stack.getCount();
    }

    @Override
    public void setStackAmount(long amount)
    {
        // 处理long到int的转换安全
        if (amount > Integer.MAX_VALUE) {
            //throw new IllegalArgumentException("ItemStack count exceeds maximum value: " + count);
            stack.setCount(Integer.MAX_VALUE);
            return;
        }
        stack.setCount((int) amount);
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
    public long getVanillaMaxStackSize() {
        // 考虑原版物品的堆叠限制
        return Math.min(stack.getMaxStackSize(), getCustomMaxStackSize());
    }

    @Override
    public long getCustomMaxStackSize() {
        // 可配置化的最大堆叠尺寸
        return CUSTOM_MAX_STACK_SIZE;
    }

    @Override
    public ItemStack splitStack(long amount) {
        if (amount <= 0) return ItemStack.EMPTY;

        // 计算可分割的数量
        int splitAmount = (int) Math.min(amount, stack.getCount());
        ItemStack split = stack.copy();
        split.setCount(splitAmount);
        stack.shrink(splitAmount);
        return split;
    }

    @Override
    public IStackType<ItemStack> split(long amount)
    {
        if (amount <= 0) return new ItemStackType();

        // 计算可分割的数量
        int splitAmount = (int) Math.min(amount, stack.getCount());
        ItemStack split = stack.copy();
        split.setCount(splitAmount);
        stack.shrink(splitAmount);
        return new ItemStackType(split);
    }

    @Override
    public boolean isSame(IStackType<ItemStack> other) {
        // 比较物品类型和基础NBT（如盔甲耐久等）
        if(!other.getTypeId().equals(this.getTypeId()))
            return false;
        return ItemStack.isSameItem(stack, other.getStack());
    }

    @Override
    public boolean isSameTypeSameComponents(IStackType<ItemStack> other) {
        if(!other.getTypeId().equals(this.getTypeId()))
            return false;
        return ItemStack.isSameItemSameComponents(stack, other.getStack());
    }

    // 网络序列化
    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        // 始终写入类型ID
        buf.writeResourceLocation(getTypeId());

        // 写入是否存在物品的标志
        boolean hasItem = !stack.isEmpty();
        buf.writeBoolean(hasItem);

        if (hasItem) {
            // 写入数量
            buf.writeVarInt(stack.getCount());
            // 使用副本避免修改原堆栈
            ItemStack copy = stack.copyWithCount(1);
            // 使用OPTIONAL_CODEC处理可能为空的情况
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, copy);
        }
    }

    //
    public ItemStackType deserialize(RegistryFriendlyByteBuf buf) {
        // 读取类型ID
        ResourceLocation typeId = buf.readResourceLocation();
        if (!typeId.equals(getTypeId())) {
            return null;// 表示未能读取任何类型
        }

        // 读取是否存在物品的标志
        boolean hasItem = buf.readBoolean();
        if (!hasItem) {
            return new ItemStackType(ItemStack.EMPTY);
        }

        // 读取数量
        int count = buf.readVarInt();
        // 使用OPTIONAL_CODEC解码
        ItemStack stack = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf)
                .copyWithCount(count);
        return new ItemStackType(stack);
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider levelRegistryAccess) {
        CompoundTag tag = new CompoundTag();
        tag.putLong("Amount", getStackAmount());
        tag.put("Stack",stack.copyWithCount(1).save(levelRegistryAccess));
        return tag;
    }

    @Override
    public ItemStackType deserializeNBT(CompoundTag nbt, HolderLookup.Provider levelRegistryAccess) {
        ItemStackType stack =  new ItemStackType(ItemStack.parseOptional(levelRegistryAccess,nbt.getCompound("Stack")));
        stack.setStackAmount(nbt.getLong("Amount"));
        return stack;
    }

    @Override
    public void render(GuiGraphics gui,int x, int y) {
        // 渲染物品图标
        gui.renderFakeItem(stack, x, y);

        // 渲染数量文本
        String countText = getCountText(stack.getCount());
        gui.drawString(Minecraft.getInstance().font,
                countText,
                x + 17 - Minecraft.getInstance().font.width(countText),
                y + 9,
                0xFFFFFF);
    }

    @Override
    public String getCountText(long count) {
        if (count <= 0) return "";
        return StringFormat.formatCount(count);
    }

    @Override
    public Component getDisplayName()
    {
        return stack.getDisplayName();
    }

    @Override
    public List<Component> getTooltipLines(Item.TooltipContext tooltipContext, @Nullable Player player, TooltipFlag tooltipFlag)
    {
        return stack.getTooltipLines(tooltipContext,player,tooltipFlag);
    }

    @Override
    public boolean equals(Object other)
    {
        if(other instanceof ItemStackType otherStack)
        {
            return this.isSameTypeSameComponents(otherStack);
        }
        return false;
    }

    @Override
    public int hashCode() {
        // 基于物品类型和组件生成哈希码
        int hash = stack.getItem().hashCode();
        hash = 31 * hash + (stack.getComponents() != null ?
                stack.getComponents().hashCode() : 0);
        return hash;
    }
}

