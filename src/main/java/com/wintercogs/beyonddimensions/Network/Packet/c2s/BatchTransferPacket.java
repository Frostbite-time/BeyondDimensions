package com.wintercogs.beyonddimensions.network.Packet.c2s;

import com.wintercogs.beyonddimensions.api.storage.handler.impl.AbstractUnorderedStackHandler;
import com.wintercogs.beyonddimensions.api.storage.key.impl.ItemStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.KeyAmount;
import com.wintercogs.beyonddimensions.common.menu.BDBaseMenu;
import com.wintercogs.beyonddimensions.common.menu.DimensionsNetMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

// 批量转移物品的数据包，仅记载需要转移的物品本身和转移方向
public record BatchTransferPacket(KeyAmount clickStack, boolean dirToStorage)
{

    private void handle(NetworkEvent.Context context)
    {
        if (clickStack().key() instanceof ItemStackKey clickItem)
        {
            Player player = context.getSender();

            if (player.containerMenu instanceof BDBaseMenu menu)
            {
                // 批量转移到存储
                if (dirToStorage())
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
                    if (!clickStack().isEmpty())
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


    public static void handle(BatchTransferPacket packet, Supplier<NetworkEvent.Context> cxt)
    {
        if (packet != null)
        {
            NetworkEvent.Context context = cxt.get();
            context.enqueueWork(() -> packet.handle(context));
            context.setPacketHandled(true);
        }
    }

    public static void encode(BatchTransferPacket packet, FriendlyByteBuf buf)
    {
        KeyAmount.serialize(buf, packet.clickStack());
        buf.writeBoolean(packet.dirToStorage);
    }

    public static BatchTransferPacket decode(FriendlyByteBuf buf)
    {
        KeyAmount clickStack = KeyAmount.deserialize(buf);
        boolean dirToStorage = buf.readBoolean();
        return new BatchTransferPacket(clickStack, dirToStorage);
    }
}
