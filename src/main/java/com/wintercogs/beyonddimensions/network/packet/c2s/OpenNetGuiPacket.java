package com.wintercogs.beyonddimensions.network.packet.c2s;


import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.api.dimensionnet.DimensionsNet;
import com.wintercogs.beyonddimensions.client.gui.NetMenuType;
import com.wintercogs.beyonddimensions.common.init.BDMenus;
import com.wintercogs.beyonddimensions.common.item.NetTerminalItem;
import com.wintercogs.beyonddimensions.common.item.NetedItem;
import com.wintercogs.beyonddimensions.common.menu.DimensionsCraftMenu;
import com.wintercogs.beyonddimensions.common.menu.DimensionsNetMenu;
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
                        (containerId, playerInventory, _player) -> new DimensionsCraftMenu(BDMenus.Dimensions_Craft_Menu.get(), containerId, playerInventory, net.getUnifiedStorage(), null, null),
                        Component.translatable("menu.title.beyonddimensions.dimensionnetmenu")
                ));
            }
            else if (targetMenu == NetMenuType.NET_MENU)
            {
                player.openMenu(new SimpleMenuProvider(
                        (containerId, playerInventory, _player) -> new DimensionsNetMenu(BDMenus.Dimensions_Net_Menu.get(), containerId, playerInventory, net.getUnifiedStorage()),
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
