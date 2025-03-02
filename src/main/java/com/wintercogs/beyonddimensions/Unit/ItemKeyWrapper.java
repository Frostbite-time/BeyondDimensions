package com.wintercogs.beyonddimensions.Unit;

import net.minecraft.world.item.ItemStack;

// ItemStack的包装类 用于实现ItemStack的hashMap快速比较
// 注意，为了适应我的设计逻辑。equals与hashCode方法被设计为不考虑数量
public class ItemKeyWrapper {
    private final ItemStack stack;

    public ItemKeyWrapper(ItemStack stack) {
        this.stack = stack.copy();
    }

    public ItemStack getItemStack() {
        return stack.copy(); // 避免外部修改
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ItemKeyWrapper that = (ItemKeyWrapper) o;
        return ItemStack.isSameItemSameComponents(stack, that.stack);
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

