package com.wintercogs.beyonddimensions.Network.Packet.toClient;


import com.wintercogs.beyonddimensions.DataBase.Handler.IStackTypedHandler;
import com.wintercogs.beyonddimensions.DataBase.Stack.IStackType;
import com.wintercogs.beyonddimensions.Menu.NetControlMenu;
import com.wintercogs.beyonddimensions.Menu.NetInterfaceBaseMenu;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;


import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class SyncFlagPacket implements IMessage
{
    private List<IStackType> flags;
    private List<Integer> targetIndex;

    public SyncFlagPacket()
    {
    }

    public SyncFlagPacket(List<IStackType> flags, List<Integer> targetIndex)
    {
        this.flags = flags;
        this.targetIndex = targetIndex;
    }

    public List<IStackType> getFlags()
    {
        return flags;
    }

    public List<Integer> getTargetIndex()
    {
        return targetIndex;
    }


    @Override
    public void toBytes(ByteBuf buf) {
        // 写入flags列表
        PacketBuffer buffer = new PacketBuffer(buf);

        buffer.writeInt(flags.size());
        for (IStackType flag : flags)
        {
            flag.serialize(buffer);
        }
        buffer.writeInt(targetIndex.size());
        for (Integer targetIndex : targetIndex)
        {
            buffer.writeInt(targetIndex);
        }

    }
    @Override
    public void fromBytes(ByteBuf buf) {

        PacketBuffer buffer = new PacketBuffer(buf);
        int flagSize = buffer.readInt();
        flags = new ArrayList<>(flagSize);
        for (int i = 0; i < flagSize; i++)
        {
            flags.add(IStackType.deserializeCommon(buffer));
        }
        int indicesSize = buffer.readInt();
        targetIndex = new ArrayList<>(indicesSize);
        for (int i = 0; i < indicesSize; i++)
        {
            targetIndex.add(buffer.readInt());
        }

    }

    @SideOnly(Side.CLIENT)
    public static class SyncFlagPacketHandler implements IMessageHandler<SyncFlagPacket, IMessage>
    {
        @Override
        public IMessage onMessage(SyncFlagPacket message, MessageContext ctx)
        {
            // 确保在客户端主线程执行
            Minecraft.getMinecraft().addScheduledTask(() -> {
                EntityPlayer player = Minecraft.getMinecraft().player;
                if (player.openContainer instanceof NetInterfaceBaseMenu menu)
                {
                    IStackTypedHandler clientStorage = menu.flagStorage;
                    int i = 0;
                    for (IStackType remoteStack : message.flags)
                    {
                        clientStorage.setStackDirectly(message.targetIndex.get(i), remoteStack.copyWithCount(1));
                        i++; // 一次遍历完毕后索引自增
                    }

                    menu.updateViewerStorage();
                }
            });
            return null; // 不需要回复消息
        }
    }

}
