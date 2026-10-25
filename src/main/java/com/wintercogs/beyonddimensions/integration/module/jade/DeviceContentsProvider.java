package com.wintercogs.beyonddimensions.integration.module.jade;

import com.google.common.math.LongMath;
import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import com.wintercogs.beyonddimensions.api.storage.handler.impl.StackHandler;
import com.wintercogs.beyonddimensions.api.storage.key.IStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.KeyAmount;
import com.wintercogs.beyonddimensions.common.block.entity.BaseNetFurnaceBlockEntity;
import com.wintercogs.beyonddimensions.common.block.entity.NetInterfaceBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
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

    static final ResourceLocation UID = new ResourceLocation(BDConstants.MODID, "contents");

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

        ListTag contents = new ListTag();
        amounts.forEach((key, amount) -> contents.add(KeyAmount.serializeNBT(new KeyAmount(key, amount))));
        data.put(UID.toString(), contents);
    }

    /**
     * 读出服务端写入的资源，读不出的跳过
     */
    static List<KeyAmount> read(CompoundTag data)
    {
        ListTag tag = data.getList(UID.toString(), Tag.TAG_COMPOUND);
        List<KeyAmount> contents = new ArrayList<>(tag.size());
        for (int i = 0; i < tag.size(); i++)
        {
            KeyAmount stack = KeyAmount.deserializeNBT(tag.getCompound(i));
            if (!stack.isEmpty())
                contents.add(stack);
        }
        return contents;
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
