package com.wintercogs.beyonddimensions.DataBase.Stack;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.Unit.StringFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.connection.ConnectionType;

public class ItemStackType implements IStackType<ItemStack> {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(BeyondDimensions.MODID, "stack_type/item");
    private static final long CUSTOM_MAX_STACK_SIZE = Long.MAX_VALUE-1; // 自定义堆叠大小

    @Override
    public ResourceLocation getTypeId() {
        return ID;
    }

    @Override
    public Class<ItemStack> getStackClass() {
        return ItemStack.class;
    }

    @Override
    public boolean isEmpty(ItemStack stack) {
        return stack.isEmpty();
    }

    @Override
    public ItemStack getEmptyStack() {
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack copyStack(ItemStack stack) {
        return stack.copy();
    }

    @Override
    public ItemStack copyStackWithCount(ItemStack stack, long count) {
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
    public long getStackAmount(ItemStack stack) {
        return stack.getCount();
    }

    @Override
    public long getMaxStackSize(ItemStack stack) {
        // 考虑原版物品的堆叠限制
        return Math.min(stack.getMaxStackSize(), getCustomMaxStackSize());
    }

    @Override
    public long getCustomMaxStackSize() {
        // 可配置化的最大堆叠尺寸
        return CUSTOM_MAX_STACK_SIZE;
    }

    @Override
    public ItemStack splitStack(ItemStack stack, long amount) {
        if (amount <= 0) return ItemStack.EMPTY;

        // 计算可分割的数量
        int splitAmount = (int) Math.min(amount, stack.getCount());
        ItemStack split = stack.copy();
        split.setCount(splitAmount);
        stack.shrink(splitAmount);
        return split;
    }

    @Override
    public boolean isSameStack(ItemStack existing, ItemStack other) {
        // 比较物品类型和基础NBT（如盔甲耐久等）
        return ItemStack.isSameItem(existing, other);
    }

    @Override
    public boolean isSameStackSameComponents(ItemStack existing, ItemStack other) {
        // 完全比较包括NBT
        return ItemStack.isSameItemSameComponents(existing, other);
    }

    @Override
    public long mergeStacks(ItemStack existing, ItemStack toInsert, long maxAmount) {
        if (!isSameStackSameComponents(existing, toInsert)) return maxAmount;

        int availableSpace = (int) (getCustomMaxStackSize() - existing.getCount());
        int canAccept = (int) Math.min(maxAmount, Math.min(availableSpace, toInsert.getCount()));

        existing.grow(canAccept);
        toInsert.shrink(canAccept);
        return maxAmount - canAccept;
    }

    @Override
    public void serialize(FriendlyByteBuf buf, ItemStack stack, RegistryAccess levelRegistryAccess) {
        ItemStack.STREAM_CODEC.decode(new RegistryFriendlyByteBuf(buf,levelRegistryAccess, ConnectionType.OTHER) );
    }

    @Override
    public ItemStack deserialize(FriendlyByteBuf buf,RegistryAccess levelRegistryAccess) {
        return ItemStack.STREAM_CODEC.decode(new RegistryFriendlyByteBuf(buf,levelRegistryAccess, ConnectionType.OTHER));
    }

    @Override
    public CompoundTag serializeNBT(ItemStack stack, HolderLookup.Provider levelRegistryAccess) {
        CompoundTag tag = new CompoundTag();
        stack.save(levelRegistryAccess,tag);
        return tag;
    }

    @Override
    public ItemStack deserializeNBT(CompoundTag nbt, HolderLookup.Provider levelRegistryAccess) {
        return ItemStack.parseOptional(levelRegistryAccess,nbt);
    }

    @Override
    public void render(GuiGraphics gui, ItemStack stack, int x, int y) {
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
}

