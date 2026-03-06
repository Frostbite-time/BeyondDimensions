package com.wintercogs.beyonddimensions.network.packet.c2s;

import com.wintercogs.beyonddimensions.api.dimensionnet.DimensionsNet;
import com.wintercogs.beyonddimensions.api.dimensionnet.UnifiedStorage;
import com.wintercogs.beyonddimensions.api.storage.key.IStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.KeyAmount;
import com.wintercogs.beyonddimensions.api.storage.key.impl.ItemStackKey;
import com.wintercogs.beyonddimensions.api.util.BytebufHelper;
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

        IStackKey<?> target = null;
        for (KeyAmount stack : storage.getStorage())
        {
            if (stack.key() instanceof ItemStackKey itemStackKey)
            {
                if (itemStackKey.getSource() == targetStack().getItem())
                {
                    target = itemStackKey;
                    break;
                }
            }
        }

        if (target != null && player.getMainHandItem().isEmpty())
        {
            ItemStack extract = (ItemStack) storage.extract(target, target.getVanillaMaxStackSize(), false, false).toStack();
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
