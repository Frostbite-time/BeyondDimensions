package com.wintercogs.beyonddimensions.common.menu;

import com.wintercogs.beyonddimensions.common.init.BDAttachments;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.INBTSerializable;
import org.jetbrains.annotations.NotNull;

/**
 * 玩家的合成格：所有带合成区的菜单（O 键打开的合成、存储终端方块与物品）共用这一份，
 * 保存在服务器的玩家数据里，死亡后保留。物品只在这里，不在客户端，也不在方块或终端物品上，所以不会被复制。
 * <p>
 * 打开合成菜单时，合成格直接以 {@link #items()} 为内容；关闭时按 {@link #keep()} 保留，或按 {@link #returnToStorage()} 退回
 */
public final class PlayerCraftingGrid implements INBTSerializable<CompoundTag>
{
    public static final int SIZE = 9;

    private final NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    private boolean keep;
    private boolean returnToStorage;

    public static PlayerCraftingGrid of(Player player)
    {
        return player.getData(BDAttachments.CRAFTING_GRID);
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
    public @NotNull CompoundTag serializeNBT(@NotNull HolderLookup.Provider registries)
    {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        for (ItemStack stack : items)
            list.add(stack.saveOptional(registries));
        tag.put("Items", list);
        tag.putBoolean("Keep", keep);
        tag.putBoolean("ReturnToStorage", returnToStorage);
        return tag;
    }

    @Override
    public void deserializeNBT(@NotNull HolderLookup.Provider registries, @NotNull CompoundTag tag)
    {
        ListTag list = tag.getList("Items", Tag.TAG_COMPOUND);
        for (int i = 0; i < SIZE; i++)
            items.set(i, i < list.size() ? ItemStack.parseOptional(registries, list.getCompound(i)) : ItemStack.EMPTY);
        keep = tag.getBoolean("Keep");
        returnToStorage = tag.getBoolean("ReturnToStorage");
    }
}
