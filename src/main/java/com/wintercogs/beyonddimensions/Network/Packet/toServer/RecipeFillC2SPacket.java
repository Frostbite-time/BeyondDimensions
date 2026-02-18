package com.wintercogs.beyonddimensions.Network.Packet.toServer;

import com.wintercogs.beyonddimensions.Api.DataBase.Stack.IStackKey;
import com.wintercogs.beyonddimensions.Menu.DimensionsCraftMenu;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static com.wintercogs.beyonddimensions.Api.DataBase.Stack.ItemStackKey.deserializeStackCaps;
import static com.wintercogs.beyonddimensions.Api.DataBase.Stack.ItemStackKey.serializeStackCaps;

public record RecipeFillC2SPacket(List<IStackKey<?>> keys, List<Long> amount)
{

    private void handle(NetworkEvent.Context context)
    {
        //获取玩家上下文
        Player player = context.getSender();

        if (player.containerMenu instanceof DimensionsCraftMenu menu)
        {
            menu.transferRecipe(keys(), amount());
        }
    }


    public static void handle(RecipeFillC2SPacket packet, Supplier<NetworkEvent.Context> cxt)
    {
        if (packet != null)
        {
            NetworkEvent.Context context = cxt.get();
            context.enqueueWork(() -> packet.handle(context));
            context.setPacketHandled(true);
        }
    }

    public static void encode(RecipeFillC2SPacket packet, FriendlyByteBuf buf)
    {
        // keys
        buf.writeVarInt(packet.keys().size());
        for (IStackKey<?> key : packet.keys())
        {
            IStackKey.serializeCommon(buf, key);
        }

        // amount
        buf.writeVarInt(packet.amount().size());
        for (long v : packet.amount())
        {
            buf.writeLong(v);
        }
    }

    public static RecipeFillC2SPacket decode(FriendlyByteBuf buf)
    {
        // keys
        int keysSize = buf.readVarInt();
        List<IStackKey<?>> keys = new ArrayList<>(keysSize);
        for (int i = 0; i < keysSize; i++)
        {
            keys.add(IStackKey.deserializeCommon(buf));
        }

        // amount
        int amtSize = buf.readVarInt();
        List<Long> amount = new ArrayList<>(amtSize);
        for (int i = 0; i < amtSize; i++)
        {
            amount.add(buf.readLong());
        }

        return new RecipeFillC2SPacket(keys, amount);
    }
}
