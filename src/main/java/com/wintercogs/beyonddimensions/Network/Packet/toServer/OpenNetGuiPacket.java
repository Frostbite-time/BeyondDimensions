package com.wintercogs.beyonddimensions.Network.Packet.toServer;


import com.wintercogs.beyonddimensions.Api.DataBase.DimensionsNet;
import com.wintercogs.beyonddimensions.Api.DataBase.NetMenuType;
import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.Item.Custom.NetTerminalItem;
import com.wintercogs.beyonddimensions.Item.Custom.NetedItem;
import com.wintercogs.beyonddimensions.Menu.DimensionsCraftMenu;
import com.wintercogs.beyonddimensions.Menu.DimensionsNetMenu;
import com.wintercogs.beyonddimensions.Registry.UIRegister;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record OpenNetGuiPacket(String uuid, NetMenuType target)
{

    private void handle(NetworkEvent.Context context)
    {
        //获取玩家上下文
        Player player = context.getSender();

        DimensionsNet net = DimensionsNet.getNetFromPlayer(player);
        if (net != null)
        {
            NetMenuType targetMenu = target();
            if (targetMenu == NetMenuType.NET_CRAFT_MENU)
            {
                player.openMenu(new SimpleMenuProvider(
                        (containerId, playerInventory, _player) -> new DimensionsCraftMenu(UIRegister.Dimensions_Craft_Menu.get(), containerId, playerInventory, net.getUnifiedStorage(), null, null),
                        Component.translatable("menu.title.beyonddimensions.dimensionnetmenu")
                ));
            }
            else if (targetMenu == NetMenuType.NET_MENU)
            {
                player.openMenu(new SimpleMenuProvider(
                        (containerId, playerInventory, _player) -> new DimensionsNetMenu(UIRegister.Dimensions_Net_Menu.get(), containerId, playerInventory, net.getUnifiedStorage()),
                        Component.translatable("menu.title.beyonddimensions.dimensionnetmenu")
                ));
            }
            else if (targetMenu == NetMenuType.NET_CRAFT_TERMINAL)
            {
                ItemStack terminalStack = null;
                if (player.getItemInHand(InteractionHand.MAIN_HAND).getItem() instanceof NetTerminalItem)
                    terminalStack = player.getItemInHand(InteractionHand.MAIN_HAND);
                else if (player.getItemInHand(InteractionHand.OFF_HAND).getItem() instanceof NetTerminalItem)
                    terminalStack = player.getItemInHand(InteractionHand.OFF_HAND);
                else
                {
                    for (ItemStack itemStack : player.getInventory().items)
                    {
                        if (itemStack.getItem() instanceof NetTerminalItem)
                        {
                            terminalStack = itemStack;
                            break;
                        }

                    }

                    if (terminalStack == null && BeyondDimensions.CuriosLoaded)
                    {
                        terminalStack = top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player)
                                .resolve()
                                .flatMap(iCuriosItemHandler ->
                                        iCuriosItemHandler.findFirstCurio(itemStack -> itemStack.getItem() instanceof NetTerminalItem && NetedItem.getNetId(itemStack) >= 0)
                                )
                                .map(slotResult -> slotResult.stack())
                                .orElse(null);
                    }
                }

                if (terminalStack != null)
                {
                    NetTerminalItem.contextMap.put(player, new NetTerminalItem.MenuTriggerContext(InteractionHand.MAIN_HAND, terminalStack));
                    player.openMenu((NetTerminalItem) terminalStack.getItem());
                }
            }

        }
    }


    public static void handle(OpenNetGuiPacket packet, Supplier<NetworkEvent.Context> cxt)
    {
        if (packet != null)
        {
            NetworkEvent.Context context = cxt.get();
            context.enqueueWork(() -> packet.handle(context));
            context.setPacketHandled(true);
        }
    }

    public static void encode(OpenNetGuiPacket packet, FriendlyByteBuf buf)
    {
        buf.writeUtf(packet.uuid());
        buf.writeEnum(packet.target());
    }

    public static OpenNetGuiPacket decode(FriendlyByteBuf buf)
    {
        return new OpenNetGuiPacket(buf.readUtf(), buf.readEnum(NetMenuType.class));
    }
}
