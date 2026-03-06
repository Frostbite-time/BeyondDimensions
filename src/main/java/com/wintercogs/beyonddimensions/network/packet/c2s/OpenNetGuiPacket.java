package com.wintercogs.beyonddimensions.network.packet.c2s;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.api.dimensionnet.DimensionsNet;
import com.wintercogs.beyonddimensions.client.gui.NetMenuType;
import com.wintercogs.beyonddimensions.common.component.ItemStackContents;
import com.wintercogs.beyonddimensions.common.init.BDDataComponents;
import com.wintercogs.beyonddimensions.common.item.NetTerminalItem;
import com.wintercogs.beyonddimensions.common.menu.DimensionsCraftMenu;
import com.wintercogs.beyonddimensions.common.menu.DimensionsNetMenu;
import com.wintercogs.beyonddimensions.integration.ModPresence;
import com.wintercogs.beyonddimensions.integration.OtherModIds;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.NonNullList;
import net.minecraft.network.Utf8String;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record OpenNetGuiPacket(String uuid, NetMenuType target) implements CustomPacketPayload
{
    public static final Type<OpenNetGuiPacket> TYPE =
            new Type<>(BeyondDimensions.makeId("open_net_gui_packet"));

    public static final StreamCodec<ByteBuf, OpenNetGuiPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8,
                    OpenNetGuiPacket::uuid,
                    new StreamCodec<ByteBuf, NetMenuType>()
                    {
                        @Override
                        public void encode(ByteBuf buf, NetMenuType netMenuType)
                        {
                            Utf8String.write(buf, netMenuType.toString(), 32000);
                        }

                        @Override
                        public NetMenuType decode(ByteBuf buf)
                        {
                            return NetMenuType.valueOf(Utf8String.read(buf, 32000));
                        }
                    },
                    OpenNetGuiPacket::target,
                    OpenNetGuiPacket::new
            );

    private void handleInClient(final IPayloadContext context)
    {

    }

    private void handleInServer(final IPayloadContext context)
    {
        //获取玩家上下文
        Player player = context.player();

        DimensionsNet net = DimensionsNet.getNetFromPlayer(player);
        if (net != null)
        {
            NetMenuType targetMenu = this.target();
            if (targetMenu == NetMenuType.NET_CRAFT_MENU)
            {
                player.openMenu(new SimpleMenuProvider(
                        (containerId, playerInventory, _player) -> new DimensionsCraftMenu(DimensionsCraftMenu.Dimensions_Craft_Menu.get(), containerId, playerInventory, net.getUnifiedStorage(), null, null),
                        Component.translatable("menu.title.beyonddimensions.dimensionnetmenu")
                ));
            }
            else if (targetMenu == NetMenuType.NET_MENU)
            {
                player.openMenu(new SimpleMenuProvider(
                        (containerId, playerInventory, _player) -> new DimensionsNetMenu(DimensionsNetMenu.Dimensions_Net_Menu.get(), containerId, playerInventory, net.getUnifiedStorage()),
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
                    for (ItemStack itemStack : player.getInventory().getNonEquipmentItems())
                    {
                        if (itemStack.getItem() instanceof NetTerminalItem)
                        {
                            terminalStack = itemStack;
                            break;
                        }

                    }

                    if (terminalStack == null && ModPresence.isLoaded(OtherModIds.CURIOS))
                    {
//                        terminalStack = top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player)
//                                .flatMap(iCuriosItemHandler ->
//                                        iCuriosItemHandler.findFirstCurio(itemStack ->
//                                                itemStack.getItem() instanceof NetTerminalItem &&
//                                                        itemStack.has(BDDataComponents.NET_ID_DATA) &&
//                                                        itemStack.get(BDDataComponents.NET_ID_DATA) >= 0
//                                        )
//                                )
//                                .map(slotResult -> slotResult.stack())
//                                .orElse(null);
                    }
                }

                if (terminalStack != null)
                {
                    if (terminalStack.get(BDDataComponents.CRAFT_SLOTS) == null)
                        terminalStack.set(BDDataComponents.CRAFT_SLOTS, new ItemStackContents(NonNullList.withSize(9, ItemStack.EMPTY)));

                    NetTerminalItem.contextMap.put(player, new NetTerminalItem.MenuTriggerContext(InteractionHand.MAIN_HAND, terminalStack));
                    player.openMenu((NetTerminalItem) terminalStack.getItem());
                }
            }
        }
    }

    public static void handle(final OpenNetGuiPacket packet, final IPayloadContext context)
    {
        if (packet != null)
        {
            PacketFlow direction = context.flow();
            if (direction == PacketFlow.CLIENTBOUND)
            {
                context.enqueueWork(() -> packet.handleInClient(context));
            }
            else if (direction == PacketFlow.SERVERBOUND)
            {
                context.enqueueWork(() -> packet.handleInServer(context));
            }
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }
}
