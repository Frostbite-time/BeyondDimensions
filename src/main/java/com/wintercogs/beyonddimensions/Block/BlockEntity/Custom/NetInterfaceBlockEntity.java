package com.wintercogs.beyonddimensions.Block.BlockEntity.Custom;

import com.wintercogs.beyonddimensions.Block.BlockEntity.ModBlockEntities;
import com.wintercogs.beyonddimensions.DataBase.DimensionsNet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.items.ItemStackHandler;

public class NetInterfaceBlockEntity extends NetedBlockEntity implements Container
{
    public final int transHold = 20;
    public int transTime = 0;

    private final ItemStackHandler itemStackHandler = new ItemStackHandler(9)
    {
        @Override
        protected void onContentsChanged(int slot)
        {
            setChanged();
        }
    };

    public NetInterfaceBlockEntity(BlockPos pos, BlockState blockState)
    {
        super(ModBlockEntities.NET_INTERFACE_BLOCK_ENTITY.get(), pos, blockState);
    }

    //--- 能力注册 (通过事件) ---
    public static void registerCapability(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK, // 标准物品能力
                ModBlockEntities.NET_INTERFACE_BLOCK_ENTITY.get(),
                (be, side) -> be.itemStackHandler // 根据方向返回处理器
        );
    }

    // 此方法的签名与 BlockEntityTicker 函数接口的签名匹配.
    public static void tick(Level level, BlockPos pos, BlockState state, NetInterfaceBlockEntity blockEntity) {
        // 你希望在计时期间执行的任何操作.
        // 例如，你可以在这里更改一个制作进度值或消耗能量.
        if(blockEntity.getNetId() != -1)
        {
            blockEntity.transTime++;
            if(blockEntity.transTime>=blockEntity.transHold)
            {
                blockEntity.transTime = 0;
                blockEntity.transferToNet(level);
            }
        }
    }

    public void transferToNet(Level level)
    {
        DimensionsNet net = DimensionsNet.getNetFromId(getNetId(),level);
        if(net != null)
        {
            for(int i=0; i<9; i++)
            {
                ItemStack stack = itemStackHandler.getStackInSlot(i);
                net.addItem(stack,stack.getCount());
                itemStackHandler.setStackInSlot(i, ItemStack.EMPTY);
            }
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries)
    {
        super.loadAdditional(tag,registries);
        this.itemStackHandler.deserializeNBT(registries,tag.getCompound("inventory"));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries)
    {
        super.saveAdditional(tag, registries);
        tag.put("inventory",itemStackHandler.serializeNBT(registries));
    }

    @Override
    public int getContainerSize()
    {
        return 9;
    }

    @Override
    public boolean isEmpty()
    {
        return itemStackHandler.getSlots() == 0;
    }

    @Override
    public ItemStack getItem(int slot)
    {
        return itemStackHandler.getStackInSlot(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int count)
    {
        return itemStackHandler.extractItem(slot,count,false);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot)
    {
        return itemStackHandler.extractItem(slot, itemStackHandler.getStackInSlot(slot).getCount(), false);
    }

    @Override
    public void setItem(int slot, ItemStack itemStack)
    {
        itemStackHandler.setStackInSlot(slot,itemStack);
    }

    @Override
    public boolean stillValid(Player player)
    {
        return true;
    }

    @Override
    public void clearContent()
    {
        for(int i=0; i<9; i++)
        {
            itemStackHandler.setStackInSlot(i, ItemStack.EMPTY);
        }
    }
}
