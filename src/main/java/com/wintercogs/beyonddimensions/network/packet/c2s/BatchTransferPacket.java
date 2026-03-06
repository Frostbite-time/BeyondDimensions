package com.wintercogs.beyonddimensions.network.packet.c2s;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.api.storage.handler.impl.AbstractUnorderedStackHandler;
import com.wintercogs.beyonddimensions.api.storage.key.KeyAmount;
import com.wintercogs.beyonddimensions.api.storage.key.impl.ItemStackKey;
import com.wintercogs.beyonddimensions.common.menu.BDBaseMenu;
import com.wintercogs.beyonddimensions.common.menu.DimensionsNetMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record BatchTransferPacket(KeyAmount clickStack, boolean dirToStorage) implements CustomPacketPayload
{
    public static final Type<BatchTransferPacket> TYPE =
            new Type<>(BeyondDimensions.makeId("batch_transfer_packet"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BatchTransferPacket> STREAM_CODEC =
            StreamCodec.composite(
                    KeyAmount.STREAM_CODEC,
                    BatchTransferPacket::clickStack,
                    ByteBufCodecs.BOOL,
                    BatchTransferPacket::dirToStorage,
                    BatchTransferPacket::new
            );

    private void handleInClient(final IPayloadContext context)
    {

    }

    private void handleInServer(final IPayloadContext context)
    {
        if (this.clickStack().key() instanceof ItemStackKey clickItem)
        {
            Player player = context.player();

            if (player.containerMenu instanceof BDBaseMenu menu)
            {
                // 批量转移到存储
                if (this.dirToStorage())
                {
                    for (Slot invSlot : menu.slots)
                    {
                        if (menu.inventoryStartIndex <= invSlot.index && invSlot.index < menu.inventoryEndIndex)
                        {
                            if (clickItem.equals(new ItemStackKey(invSlot.getItem())))
                                menu.customClickHandler(invSlot.index, new KeyAmount(new ItemStackKey(invSlot.getItem()), invSlot.getItem().getCount()), 0, true);
                        }
                    }
                }
                // 存储到背包
                else if (menu instanceof DimensionsNetMenu netMenu)
                {
                    if (!this.clickStack().isEmpty())
                    {
                        AbstractUnorderedStackHandler storage = netMenu.storage;

                        // 遍历目标槽位
                        for (int targetSlotIndex = menu.inventoryStartIndex; targetSlotIndex < menu.inventoryEndIndex && storage.hasStack(clickItem); targetSlotIndex++)
                        {
                            Slot slot = menu.slots.get(targetSlotIndex);

                            KeyAmount extract = storage.extract(clickItem, Integer.MAX_VALUE, false, false); // 防止数量过多无法回插
                            if (extract.toStack() instanceof ItemStack extractedStack)
                            {
                                ItemStack remaining = slot.safeInsert(extractedStack);
                                if (!remaining.isEmpty())
                                    storage.insert(new ItemStackKey(remaining), remaining.getCount(), false);
                            }
                            else  // 防御操作，如果不是物品堆，整个回插
                                storage.insert(extract.key(), extract.amount(), false);
                        }
                    }
                }

                menu.broadcastChanges();
            }
        }
    }

    public static void handle(final BatchTransferPacket packet, final IPayloadContext context)
    {
        if (packet != null)
        {
            PacketFlow direction = context.flow();
            if (direction == PacketFlow.CLIENTBOUND)
            {
                context.enqueueWork(() -> packet.handleInClient(context));
            }
            else if (direction == PacketFlow.SERVERBOUND)
            {
                context.enqueueWork(() -> packet.handleInServer(context));
            }
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }
}
