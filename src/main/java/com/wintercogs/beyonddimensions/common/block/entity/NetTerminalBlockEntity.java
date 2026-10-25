package com.wintercogs.beyonddimensions.common.block.entity;

import com.wintercogs.beyonddimensions.api.dimensionnet.DimensionsNet;
import com.wintercogs.beyonddimensions.common.init.BDBlockEntities;
import com.wintercogs.beyonddimensions.common.menu.DimensionsNetTerminalMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NetTerminalBlockEntity extends NetedBlockEntity implements MenuProvider
{
    // 旧版本存在终端里的合成格物品，打开或拆除终端时还给玩家
    private final NonNullList<ItemStack> craftItems = NonNullList.withSize(9, ItemStack.EMPTY);

    public NetTerminalBlockEntity(BlockPos pos, BlockState blockState)
    {
        super(BDBlockEntities.NET_TERMINAL_BLOCK_ENTITY.get(), pos, blockState);
    }

    @Override
    public @NotNull Component getDisplayName()
    {
        return Component.translatable("menu.title.beyonddimensions.dimensionnetmenu");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, @NotNull Inventory inventory, @NotNull Player player)
    {
        DimensionsNet net = getNet();
        if (net != null)
        {
            if (craftItems.stream().anyMatch(stack -> !stack.isEmpty()))
            {
                DimensionsNetTerminalMenu.returnLegacyCraftItems(player, net.getUnifiedStorage(), craftItems);
                craftItems.clear();
                setChanged();
            }
            return new DimensionsNetTerminalMenu(containerId, inventory, net.getUnifiedStorage(), null, this.getBlockPos());
        }
        return null;
    }

    @Override
    public void load(CompoundTag tag)
    {
        super.load(tag);
        ListTag itemsList = tag.getList("CraftItems", Tag.TAG_COMPOUND);
        for (int i = 0; i < 9; i++)
        {
            CompoundTag itemTag = i < itemsList.size() ? itemsList.getCompound(i) : new CompoundTag();
            ItemStack stack = ItemStack.of(itemTag);
            craftItems.set(i, stack);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag)
    {
        super.saveAdditional(tag);
        if (craftItems.stream().allMatch(ItemStack::isEmpty))
            return;
        ListTag itemsList = new ListTag();
        for (ItemStack stack : craftItems)
        {
            CompoundTag itemTag = stack.save(new CompoundTag());
            itemsList.add(itemTag);
        }
        tag.put("CraftItems", itemsList);
    }

    public void dropContent()
    {
        for (ItemStack stack : craftItems)
        {
            if (!stack.isEmpty())
            {
                Block.popResource(level, getBlockPos(), stack.copy());
            }
        }
    }
}
