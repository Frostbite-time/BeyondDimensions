package com.wintercogs.beyonddimensions.Network.Packet.ClientOrServer;

import com.wintercogs.beyonddimensions.DataBase.Stack.IStackType;
import com.wintercogs.beyonddimensions.DataBase.Stack.ItemStackType;
import com.wintercogs.beyonddimensions.Menu.DimensionsNetMenu;
import com.wintercogs.beyonddimensions.Menu.NetEnergyMenu;
import com.wintercogs.beyonddimensions.Menu.NetInterfaceBaseMenu;
import com.wintercogs.beyonddimensions.Registry.PacketRegister;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;


import java.util.function.Supplier;

public class CallSeverClickPacket implements IMessage
{

    private int slotIndex;
    private IStackType clickItem;
    private int button;
    private boolean shiftDown;

    public CallSeverClickPacket(){}

    public CallSeverClickPacket(int slotIndex, IStackType clickItem, int button, boolean shiftDown)
    {
        this.slotIndex = slotIndex;
        this.clickItem = clickItem;
        this.button = button;
        this.shiftDown = shiftDown;
    }

    public int getSlotIndex()
    {
        return slotIndex;
    }

    public IStackType getClickItem()
    {
        return clickItem;
    }

    public int getButton()
    {
        return button;
    }

    public boolean isShiftDown()
    {
        return shiftDown;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        PacketBuffer buffer = new PacketBuffer(buf);

        // 严格按照原始顺序写入
        buffer.writeInt(slotIndex);

        // 直接序列化IStackType
        clickItem.serialize(buffer);

        buffer.writeInt(button);
        buffer.writeBoolean(shiftDown);
    }
    @Override
    public void fromBytes(ByteBuf buf) {
        PacketBuffer buffer = new PacketBuffer(buf);

        // 严格按原始顺序读取
        slotIndex = buffer.readInt();
        clickItem = IStackType.deserializeCommon(buffer);

        button = buffer.readInt();
        shiftDown = buffer.readBoolean();
    }

    public static class CallSeverClickPacketHandlerOnServer implements IMessageHandler<CallSeverClickPacket, IMessage>
    {

        @Override
        public IMessage onMessage(CallSeverClickPacket message, MessageContext ctx)
        {
            // 这是发送到服务器的数据包发送到的玩家
            EntityPlayerMP serverPlayer = ctx.getServerHandler().player;

            // 添加为一个计划任务(Scheduled Task)，在主服务器线程上执行操作
            serverPlayer.getServerWorld().addScheduledTask(() -> {
                if (serverPlayer.openContainer instanceof DimensionsNetMenu menu)
                {
                    menu.customClickHandler(message.slotIndex, message.clickItem,message.button,message.shiftDown);
                    menu.broadcastChanges();
                }
                if(serverPlayer.openContainer instanceof NetInterfaceBaseMenu menu)
                {
                    menu.customClickHandler(message.slotIndex, message.clickItem,message.button,message.shiftDown);
                    menu.broadcastChanges();
                }
            });
            // 回应自身 用于让客户端解除锁定
            return new CallSeverClickPacket(1, new ItemStackType(ItemStack.EMPTY),1,false);
        }
    }


    @SideOnly(value = Side.CLIENT)
    public static class CallSeverClickPacketHandlerOnClient implements IMessageHandler<CallSeverClickPacket, IMessage>
    {

        @Override
        public IMessage onMessage(CallSeverClickPacket message, MessageContext ctx)
        {
            // 确保在客户端主线程执行
            Minecraft.getMinecraft().addScheduledTask(() -> {
                EntityPlayer player = Minecraft.getMinecraft().player;

                if (player.openContainer instanceof DimensionsNetMenu menu)
                {
                    menu.isHanding = false;
                    return; // 当服务器接受到包时，如果玩家打开的不是DimensionsNetMenu，不予理会
                }
                if (player.openContainer instanceof NetInterfaceBaseMenu menu)
                {
                    menu.isHanding = false;
                }
            });
            return null; // 不需要回复消息
        }
    }

}
