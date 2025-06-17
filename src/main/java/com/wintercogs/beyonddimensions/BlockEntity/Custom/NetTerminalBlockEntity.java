package com.wintercogs.beyonddimensions.BlockEntity.Custom;

import com.wintercogs.beyonddimensions.Api.DataBase.DimensionsNet;
import com.wintercogs.beyonddimensions.BlockEntity.ModBlockEntities;
import com.wintercogs.beyonddimensions.Menu.DimensionsCraftMenuTerminal;
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
import net.minecraft.world.level.block.state.BlockState;

public class NetTerminalBlockEntity extends NetedBlockEntity implements MenuProvider
{

    private final NonNullList<ItemStack> craftItems = NonNullList.withSize(9, ItemStack.EMPTY);

    public NetTerminalBlockEntity(BlockPos pos, BlockState blockState)
    {
        super(ModBlockEntities.NET_TERMINAL_BLOCK_ENTITY.get(), pos, blockState);
    }

    @Override
    public Component getDisplayName()
    {
        return Component.translatable("menu.title.beyonddimensions.dimensionnetmenu");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player)
    {
        DimensionsNet net = getNet();
        if(net != null)
        {
            // 在服务端中craftItems作为直接引用传递。保证为同一个列表
            // 而后，craftItems会在Menu被包装，并通过Menu的包装类完成网络同步
            // 最后，利用方块实体进行持久保存
            return new DimensionsCraftMenuTerminal(containerId,inventory,net, craftItems, null, this.getBlockPos());
        }
        return null;
    }

    @Override
    public void load(CompoundTag tag)
    {
        super.load(tag);
        ListTag itemsList = tag.getList("CraftItems", Tag.TAG_COMPOUND);
        for (int i = 0; i < 9; i++) {
            CompoundTag itemTag = i < itemsList.size() ? itemsList.getCompound(i) : new CompoundTag();
            ItemStack stack = ItemStack.of(itemTag);
            craftItems.set(i, stack);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag)
    {
        super.saveAdditional(tag);
        ListTag itemsList = new ListTag();
        for (ItemStack stack : craftItems) {
            CompoundTag itemTag = new CompoundTag();
            stack.save(itemTag);
            itemsList.add(itemTag);
        }
        tag.put("CraftItems", itemsList);
    }
}
