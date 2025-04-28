package com.wintercogs.beyonddimensions.Network.Packet.toServer;

import com.wintercogs.beyonddimensions.Menu.DimensionsCraftMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public record RecipeFillC2SPacket(List<ItemStack> inputs)
{

    private void handle(NetworkEvent.Context context)
    {
        //获取玩家上下文
        Player player = context.getSender();

        if(player.containerMenu instanceof DimensionsCraftMenu menu)
        {
            //服务端处理示意
            //1.解析数组
            //2.为每一个槽位在背包和存储中寻找资源填入

            menu.transferRecipe(inputs());
        }
    }


    public static void handle(RecipeFillC2SPacket packet, Supplier<NetworkEvent.Context> cxt)
    {
        if (packet != null) {
            NetworkEvent.Context context = cxt.get();
            context.enqueueWork(() -> packet.handle(context));
            context.setPacketHandled(true);
        }
    }

    public static void encode(RecipeFillC2SPacket packet, FriendlyByteBuf buf)
    {
        buf.writeInt(packet.inputs().size());  // 先写入列表长度
        for (ItemStack stack : packet.inputs()) {
            buf.writeItem(stack);
        }
    }

    public static RecipeFillC2SPacket decode(FriendlyByteBuf buf)
    {
        int size = buf.readInt();
        List<ItemStack> stacks = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            stacks.add(buf.readItem());
        }
        return new RecipeFillC2SPacket(stacks);
    }
}
