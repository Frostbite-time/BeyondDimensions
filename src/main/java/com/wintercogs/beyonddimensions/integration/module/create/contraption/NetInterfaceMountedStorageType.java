package com.wintercogs.beyonddimensions.integration.module.create.contraption;

import com.simibubi.create.api.contraption.storage.item.MountedItemStorageType;
import com.simibubi.create.api.registry.CreateRegistries;
import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import com.wintercogs.beyonddimensions.common.block.entity.NetInterfaceBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.Nullable;

public class NetInterfaceMountedStorageType extends MountedItemStorageType<NetInterfaceMountedStorage>
{
    public static final DeferredRegister<MountedItemStorageType<?>> TYPES = DeferredRegister.create(CreateRegistries.MOUNTED_ITEM_STORAGE_TYPE, BDConstants.MODID);
    public static final DeferredHolder<MountedItemStorageType<?>, NetInterfaceMountedStorageType> NET_INTERFACE = TYPES.register("net_interface", NetInterfaceMountedStorageType::new);

    public static void register(IEventBus modBus)
    {
        TYPES.register(modBus);
    }

    public NetInterfaceMountedStorageType()
    {
        super(NetInterfaceMountedStorage.CODEC);
    }

    @Override
    public @Nullable NetInterfaceMountedStorage mount(Level level, BlockState state, BlockPos pos, @Nullable BlockEntity be)
    {
        if (be instanceof NetInterfaceBlockEntity netInterface)
        {
            return NetInterfaceMountedStorage.fromBlockEntity(netInterface);
        }
        return null;
    }
}
