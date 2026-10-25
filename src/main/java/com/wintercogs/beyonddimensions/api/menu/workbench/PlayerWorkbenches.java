package com.wintercogs.beyonddimensions.api.menu.workbench;

import com.wintercogs.beyonddimensions.common.init.BDAttachments;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.INBTSerializable;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 玩家留在工作台里的物品与工作台的偏好：所有存储界面共用这一份，保存在服务器的玩家数据里，死亡后保留。
 * 物品只在这里，不在客户端，也不在方块或终端物品上。
 * 有槽位的工作台把要保留的物品放在 {@link #items} 里；关闭界面时 {@link #keep()} 为 false 就按 {@link #returnToStorage()} 退回
 */
public final class PlayerWorkbenches implements INBTSerializable<CompoundTag>
{
    private final Map<ResourceLocation, NonNullList<ItemStack>> items = new LinkedHashMap<>();
    private boolean keep;
    private boolean returnToStorage;

    public static PlayerWorkbenches of(Player player)
    {
        Optional<PlayerWorkbenches> workbenches = player.getCapability(BDAttachments.WORKBENCHES).resolve();
        if (workbenches.isPresent())
            return workbenches.get();
        // 玩家实体被移除时（下线、死亡、换维度），Forge 先作废能力再关闭打开的菜单，而关闭菜单时仍要读写这份数据。
        // 像复制玩家数据时一样临时恢复能力，取到后再作废
        player.reviveCaps();
        try
        {
            return player.getCapability(BDAttachments.WORKBENCHES).orElseThrow(() -> new IllegalStateException("Player has no workbench data"));
        }
        finally
        {
            player.invalidateCaps();
        }
    }

    /**
     * 工作台 {@code workbench} 保留的物品，共 {@code size} 格；菜单打开期间可以直接当作它的容器内容
     */
    public NonNullList<ItemStack> items(ResourceLocation workbench, int size)
    {
        NonNullList<ItemStack> list = items.get(workbench);
        if (list != null && list.size() == size)
            return list;
        NonNullList<ItemStack> resized = NonNullList.withSize(size, ItemStack.EMPTY);
        if (list != null)
        {
            for (int i = 0; i < Math.min(size, list.size()); i++)
                resized.set(i, list.get(i));
        }
        items.put(workbench, resized);
        return resized;
    }

    /**
     * 关闭界面时保留工作台里的物品
     */
    public boolean keep()
    {
        return keep;
    }

    /**
     * 工作台被清空时（关闭且不保留、换入配方）物品优先退回存储，否则优先退回背包
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
        CompoundTag workbenches = new CompoundTag();
        for (var entry : items.entrySet())
        {
            if (entry.getValue().stream().allMatch(ItemStack::isEmpty))
                continue;
            workbenches.put(entry.getKey().toString(), save(entry.getValue()));
        }
        tag.put("workbenches", workbenches);
        tag.putBoolean("keep", keep);
        tag.putBoolean("return_to_storage", returnToStorage);
        return tag;
    }

    @Override
    public void deserializeNBT(@NotNull CompoundTag tag)
    {
        items.clear();
        CompoundTag workbenches = tag.getCompound("workbenches");
        for (String key : workbenches.getAllKeys())
        {
            ResourceLocation id = ResourceLocation.tryParse(key);
            if (id != null)
                items.put(id, load(workbenches.getList(key, Tag.TAG_COMPOUND)));
        }
        // 0.8.2 之前只有合成格，物品直接存在 items 里
        if (tag.contains("items", Tag.TAG_LIST) && !items.containsKey(StorageWorkbenchTypes.CRAFTING))
            items.put(StorageWorkbenchTypes.CRAFTING, load(tag.getList("items", Tag.TAG_COMPOUND)));
        keep = tag.getBoolean("keep");
        returnToStorage = tag.getBoolean("return_to_storage");
    }

    private static ListTag save(NonNullList<ItemStack> stacks)
    {
        ListTag list = new ListTag();
        for (ItemStack stack : stacks)
            list.add(stack.save(new CompoundTag()));
        return list;
    }

    private static NonNullList<ItemStack> load(ListTag list)
    {
        NonNullList<ItemStack> stacks = NonNullList.withSize(list.size(), ItemStack.EMPTY);
        for (int i = 0; i < list.size(); i++)
            stacks.set(i, ItemStack.of(list.getCompound(i)));
        return stacks;
    }
}
