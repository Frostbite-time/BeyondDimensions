package com.wintercogs.beyonddimensions.Network.Packet.toServer;

import com.wintercogs.beyonddimensions.Api.DataBase.Stack.IStackType;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.ItemStackType;
import com.wintercogs.beyonddimensions.Api.DataBase.Storage.UnifiedStorage;
import com.wintercogs.beyonddimensions.Menu.DimensionsNetMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

// 批量转移物品的数据包，仅记载需要转移的物品本身和转移方向
public record BatchTransferPacket(IStackType clickStack, boolean dirToStorage)
{

    private void handle(NetworkEvent.Context context)
    {
        if (clickStack() instanceof ItemStackType clickItem)
        {
            Player player = context.getSender();

            if (player.containerMenu instanceof DimensionsNetMenu menu)
            {
                // 批量转移到存储
                if (dirToStorage)
                {
                    for (Slot invSlot : menu.slots)
                    {
                        if (menu.inventoryStartIndex <= invSlot.index && invSlot.index < menu.inventoryEndIndex)
                        {
                            if (ItemStack.isSameItemSameTags(clickItem.getStack(), invSlot.getItem()))
                                menu.customClickHandler(invSlot.index, new ItemStackType(invSlot.getItem()), 0, true);
                        }
                    }
                }
                //到背包 暂时留空，以后如果需要再写
                else
                {
                    if (!clickStack.isEmpty())
                    {
                        UnifiedStorage storage = menu.storage;
                        // 防止数据包伪造，然后赋予trueStack需要提取的数量
                        IStackType trueStack = storage.getStackByStack(clickStack).copyWithCount(clickStack.getStackAmount());

                        // 遍历目标槽位
                        for (int targetSlotIndex = menu.inventoryStartIndex; targetSlotIndex < menu.inventoryEndIndex && !trueStack.isEmpty(); targetSlotIndex++)
                        {
                            Slot slot = menu.slots.get(targetSlotIndex);

                            if (trueStack instanceof ItemStackType trueItemTypedStack)
                            {
                                ItemStack extract = (ItemStack) storage.extract(trueItemTypedStack, false).getStack();
                                ItemStack remaining = slot.safeInsert(extract);
                                if (!remaining.isEmpty())
                                    storage.insert(new ItemStackType(remaining), false);
                                trueStack = new ItemStackType(remaining.copy());
                            }
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
        packet.clickStack.serialize(buf);
        buf.writeBoolean(packet.dirToStorage);
    }

    public static BatchTransferPacket decode(FriendlyByteBuf buf)
    {
        IStackType clickStack = IStackType.deserializeCommon(buf);
        boolean dirToStorage = buf.readBoolean();
        return new BatchTransferPacket(clickStack, dirToStorage);
    }
}
