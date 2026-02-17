package com.wintercogs.beyonddimensions.Network.Packet.toServer;

import com.wintercogs.beyonddimensions.Api.DataBase.DimensionsNet;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.IStackKey;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.ItemStackType;
import com.wintercogs.beyonddimensions.Api.DataBase.Storage.UnifiedStorage;
import com.wintercogs.beyonddimensions.Api.Util.BytebufHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record PickBlockFromNetPacket(ItemStack targetStack)
{

    private void handle(NetworkEvent.Context context)
    {
        Player player = context.getSender();
        if (!player.getMainHandItem().isEmpty()) return;
        DimensionsNet net = DimensionsNet.getNetFromPlayer(player);
        if (net == null) return;
        UnifiedStorage storage = net.getUnifiedStorage();

        ItemStackType target = null;
        for (IStackKey stack : storage.getStorage())
        {
            if (stack instanceof ItemStackType itemStackType)
            {
                if (itemStackType.getStack().getItem() == targetStack().getItem())
                {
                    target = (ItemStackType) itemStackType.copyWithCount(itemStackType.getVanillaMaxStackSize());
                    break;
                }
            }
        }

        if (target != null && player.getMainHandItem().isEmpty())
        {
            ItemStack extract = ((ItemStackType) storage.extract(target, false)).copyStack();
            player.setItemInHand(InteractionHand.MAIN_HAND, extract);
        }
    }


    public static void handle(PickBlockFromNetPacket packet, Supplier<NetworkEvent.Context> cxt)
    {
        if (packet != null)
        {
            NetworkEvent.Context context = cxt.get();
            context.enqueueWork(() -> packet.handle(context));
            context.setPacketHandled(true);
        }
    }

    public static void encode(PickBlockFromNetPacket packet, FriendlyByteBuf buf)
    {
        BytebufHelper.writeItemBuf(buf, packet.targetStack);
    }

    public static PickBlockFromNetPacket decode(FriendlyByteBuf buf)
    {
        return new PickBlockFromNetPacket(BytebufHelper.readItemBuf(buf));
    }
}
