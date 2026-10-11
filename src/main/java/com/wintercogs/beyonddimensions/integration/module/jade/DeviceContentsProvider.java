package com.wintercogs.beyonddimensions.integration.module.jade;

import com.google.common.math.LongMath;
import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import com.wintercogs.beyonddimensions.api.storage.handler.impl.StackHandler;
import com.wintercogs.beyonddimensions.api.storage.key.IStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.KeyAmount;
import com.wintercogs.beyonddimensions.common.block.entity.BaseNetFurnaceBlockEntity;
import com.wintercogs.beyonddimensions.common.block.entity.NetInterfaceBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IServerDataProvider;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 设备自己存放的资源：网络接口的缓存，熔炉的原料、燃料与产物。同种资源合并成一项
 */
public enum DeviceContentsProvider implements IServerDataProvider<BlockAccessor>
{
    INSTANCE;

    static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(BDConstants.MODID, "contents");
    static final StreamCodec<RegistryFriendlyByteBuf, List<KeyAmount>> CODEC =
            KeyAmount.STREAM_CODEC.apply(ByteBufCodecs.list());

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor)
    {
        Map<IStackKey<?>, Long> amounts = new LinkedHashMap<>();
        for (StackHandler storage : storages(accessor.getBlockEntity()))
        {
            for (KeyAmount stack : storage.getStorage())
            {
                if (!stack.isEmpty())
                    amounts.merge(stack.key(), stack.amount(), LongMath::saturatedAdd);
            }
        }
        if (amounts.isEmpty())
            return;

        List<KeyAmount> contents = new ArrayList<>(amounts.size());
        amounts.forEach((key, amount) -> contents.add(new KeyAmount(key, amount)));
        data.put(UID.toString(), accessor.encodeAsNbt(CODEC, contents));
    }

    @Override
    public boolean shouldRequestData(BlockAccessor accessor)
    {
        return !storages(accessor.getBlockEntity()).isEmpty();
    }

    @Override
    public ResourceLocation getUid()
    {
        return UID;
    }

    private static List<StackHandler> storages(BlockEntity blockEntity)
    {
        if (blockEntity instanceof NetInterfaceBlockEntity netInterface)
            return List.of(netInterface.getStackHandler());
        if (blockEntity instanceof BaseNetFurnaceBlockEntity<?> furnace)
            return List.of(furnace.getInputStorageSlots(), furnace.getFuelStorageSlots(),
                    furnace.getOutputStorageSlots(), furnace.getFuelReturnSlots());
        return List.of();
    }
}
