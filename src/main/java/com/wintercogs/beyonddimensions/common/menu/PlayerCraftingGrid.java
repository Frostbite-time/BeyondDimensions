package com.wintercogs.beyonddimensions.common.menu;

import com.wintercogs.beyonddimensions.common.init.BDAttachments;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.INBTSerializable;
import org.jetbrains.annotations.NotNull;

/**
 * 玩家的合成格：所有带合成区的菜单共用这一份，保存在服务器的玩家数据里，死亡后保留。
 * 物品只在这里，不在客户端，也不在方块或终端物品上
 */
public final class PlayerCraftingGrid implements INBTSerializable<CompoundTag>
{
    public static final int SIZE = 9;

    private final NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    private boolean keep;
    private boolean returnToStorage;

    public static PlayerCraftingGrid of(Player player)
    {
        return player.getCapability(BDAttachments.CRAFTING_GRID).orElseThrow(() -> new IllegalStateException("Player has no crafting grid"));
    }

    /**
     * 合成格的内容，菜单打开期间就是合成格本身
     */
    public NonNullList<ItemStack> items()
    {
        return items;
    }

    /**
     * 关闭菜单时保留合成格里的物品
     */
    public boolean keep()
    {
        return keep;
    }

    /**
     * 合成格被清空时（关闭且不保留、换入配方）优先退回存储，否则优先退回背包
     */
    public boolean returnToStorage()
    {
        return returnToStorage;
    }

    public void setPreference(boolean returnToStorage, boolean keep)
    {
        this.returnToStorage = returnToStorage;
        this.keep = keep;
    }

    @Override
    public @NotNull CompoundTag serializeNBT()
    {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        for (ItemStack stack : items)
            list.add(stack.save(new CompoundTag()));
        tag.put("items", list);
        tag.putBoolean("keep", keep);
        tag.putBoolean("return_to_storage", returnToStorage);
        return tag;
    }

    @Override
    public void deserializeNBT(@NotNull CompoundTag tag)
    {
        ListTag list = tag.getList("items", Tag.TAG_COMPOUND);
        for (int i = 0; i < SIZE; i++)
            items.set(i, i < list.size() ? ItemStack.of(list.getCompound(i)) : ItemStack.EMPTY);
        keep = tag.getBoolean("keep");
        returnToStorage = tag.getBoolean("return_to_storage");
    }
}
