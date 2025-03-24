package com.wintercogs.beyonddimensions.Network.Packet.ClientOrServer;

import com.wintercogs.beyonddimensions.DataBase.Stack.IStackType;
import com.wintercogs.beyonddimensions.DataBase.Stack.ItemStackType;
import com.wintercogs.beyonddimensions.Menu.DimensionsNetMenu;
import com.wintercogs.beyonddimensions.Menu.NetInterfaceBaseMenu;
import com.wintercogs.beyonddimensions.Registry.PacketRegister;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

public record CallSeverClickPacket(int slotIndex , IStackType clickItem, int button, boolean shiftDown)
{
    private void handleServer(NetworkEvent.Context context)
    {
        Player player = context.getSender();
        if (player.containerMenu instanceof DimensionsNetMenu menu)
        {
            menu.customClickHandler(slotIndex(),clickItem(),button(),shiftDown());
            menu.broadcastChanges();
            // 这里发包不是让客户端执行操作，而是解除锁定
            PacketRegister.INSTANCE.send(PacketDistributor.PLAYER.with(()->(ServerPlayer) player), new CallSeverClickPacket(1, new ItemStackType(ItemStack.EMPTY),1,false));
            return; // 当服务器接受到包时，如果玩家打开的不是DimensionsNetMenu，不予理会
        }
        if(player.containerMenu instanceof NetInterfaceBaseMenu menu)
        {
            menu.customClickHandler(slotIndex(),clickItem(),button(),shiftDown());
            menu.broadcastChanges();
            // 这里发包不是让客户端执行操作，而是解除锁定
            PacketRegister.INSTANCE.send(PacketDistributor.PLAYER.with(()->(ServerPlayer) player), new CallSeverClickPacket(1, new ItemStackType(ItemStack.EMPTY),1,false));
            return; // 当服务器接受到包时，如果玩家打开的不是DimensionsNetMenu，不予理会
        }
    }

    @OnlyIn(Dist.CLIENT)
    private void handleClient(NetworkEvent.Context context)
    {
        Player player = Minecraft.getInstance().player;
        if (player.containerMenu instanceof DimensionsNetMenu menu)
        {
            menu.isHanding = false;
            return; // 当服务器接受到包时，如果玩家打开的不是DimensionsNetMenu，不予理会
        }
        if (player.containerMenu instanceof NetInterfaceBaseMenu menu)
        {
            menu.isHanding = false;
        }
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
