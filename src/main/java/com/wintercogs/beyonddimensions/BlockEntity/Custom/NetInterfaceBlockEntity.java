package com.wintercogs.beyonddimensions.BlockEntity.Custom;

import com.wintercogs.beyonddimensions.BlockEntity.ModBlockEntities;
import com.wintercogs.beyonddimensions.DataBase.DimensionsNet;
import com.wintercogs.beyonddimensions.DataBase.Handler.StackTypedHandler;
import com.wintercogs.beyonddimensions.DataBase.Stack.IStackType;
import com.wintercogs.beyonddimensions.DataBase.Stack.ItemStackType;
import com.wintercogs.beyonddimensions.DataBase.Storage.TypedHandlerManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

import java.util.function.Function;

public class NetInterfaceBlockEntity extends NetedBlockEntity
{
    public final int transHold = 20;
    public int transTime = 0;

    private final StackTypedHandler stackHandler = new StackTypedHandler(9)
    {
        @Override
        public void onChange()
        {
            setChanged();
        }
    };

    public StackTypedHandler getStackHandler()
    {
        return this.stackHandler;
    }

    public NetInterfaceBlockEntity(BlockPos pos, BlockState blockState)
    {
        super(ModBlockEntities.NET_INTERFACE_BLOCK_ENTITY.get(), pos, blockState);
    }

    //--- 能力注册 (通过事件) ---
    public static void registerCapability(RegisterCapabilitiesEvent event) {
        TypedHandlerManager.BlockCommonCapHandlerMap.forEach(
                (cap,handlerF)->{
                    event.registerBlockEntity(
                            (BlockCapability<? super Object, ? extends Direction>) cap, // 标准物品能力
                            ModBlockEntities.NET_INTERFACE_BLOCK_ENTITY.get(),
                            (be, side) -> {
                                Function handler = TypedHandlerManager.getCommonHandler(cap,StackTypedHandler.class);
                                return handler.apply(be.stackHandler);
                            } // 根据方向返回处理器
                    );
                }
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
                blockEntity.transferToNet();
            }
        }
    }

    public void transferToNet()
    {
        DimensionsNet net = getNet();
        if(net != null)
        {
            for(int i=0; i<9; i++)
            {
                IStackType stack = stackHandler.getStackBySlot(i);
                if(stack !=null &&!stack.isEmpty())
                {
                    net.getUnifiedStorage().insert(stack.copy(),false);
                    stackHandler.setStackDirectly(i, new ItemStackType());
                }
            }
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries)
    {
        super.loadAdditional(tag,registries);
        this.stackHandler.deserializeNBT(registries,tag.getCompound("inventory"));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries)
    {
        super.saveAdditional(tag, registries);
        tag.put("inventory", stackHandler.serializeNBT(registries));
    }
}
