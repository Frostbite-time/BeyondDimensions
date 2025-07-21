package com.wintercogs.beyonddimensions.Network.Packet.ClientOrServer;

import com.wintercogs.beyonddimensions.Api.DataBase.Stack.IStackType;
import com.wintercogs.beyonddimensions.Menu.BDBaseMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record CallSeverClickPacket(int slotIndex , IStackType clickItem, int button, boolean shiftDown)
{
    private void handleServer(NetworkEvent.Context context)
    {
        Player player = context.getSender();
        if (player.containerMenu instanceof BDBaseMenu menu)
        {
            menu.customClickHandler(slotIndex(),clickItem(),button(),shiftDown());
            menu.broadcastChanges();
        }
    }

    @OnlyIn(Dist.CLIENT)
    private void handleClient(NetworkEvent.Context context)
    {

    }


    public static void handle(CallSeverClickPacket packet, Supplier<NetworkEvent.Context> cxt)
    {
        if (packet != null) {
            NetworkEvent.Context context = cxt.get();
            NetworkDirection direction = context.getDirection();
            if(direction == NetworkDirection.PLAY_TO_CLIENT)
            {
                context.enqueueWork(() ->
                        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> packet.handleClient(context))
                );
                context.setPacketHandled(true);
            }
            else if(direction == NetworkDirection.PLAY_TO_SERVER)
            {
                context.enqueueWork(() -> packet.handleServer(context));
                context.setPacketHandled(true);
            }
        }
    }

    public static void encode(CallSeverClickPacket packet, FriendlyByteBuf buf)
    {
        // 按字段声明顺序写入数据
        buf.writeInt(packet.slotIndex());

        // 显式序列化 IStackType（不直接调用其encode方法）
        IStackType stackType = packet.clickItem();
        stackType.serialize(buf); // 直接调用接口的序列化方法

        buf.writeInt(packet.button());
        buf.writeBoolean(packet.shiftDown());

    }

    public static CallSeverClickPacket decode(FriendlyByteBuf buf)
    {
        // 按字段声明顺序读取数据
        int slotIndex = buf.readInt();

        // 显式反序列化 IStackType（不直接调用其decode方法）
        IStackType clickItem = IStackType.deserializeCommon(buf); // 直接调用接口的反序列化方法

        int button = buf.readInt();
        boolean shiftDown = buf.readBoolean();

        return new CallSeverClickPacket(slotIndex, clickItem, button, shiftDown);
    }
}
