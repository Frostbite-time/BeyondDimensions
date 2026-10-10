package com.wintercogs.beyonddimensions.network.packet.c2s;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.client.gui.MagnetToggleType;
import com.wintercogs.beyonddimensions.api.machine.BDMachineSettings;
import com.wintercogs.beyonddimensions.api.machine.Machine;
import com.wintercogs.beyonddimensions.api.machine.MachineSetting;
import com.wintercogs.beyonddimensions.common.item.NetMagnetItem;
import com.wintercogs.beyonddimensions.common.machine.HopperFluidMode;
import com.wintercogs.beyonddimensions.common.machine.HopperItemMode;
import com.wintercogs.beyonddimensions.common.machine.RedStoneControlMode;
import com.wintercogs.beyonddimensions.integration.ModPresence;
import com.wintercogs.beyonddimensions.integration.OtherModIds;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;

import java.util.List;

public record ToggleMagnetPacket(MagnetToggleType toggleType) implements CustomPacketPayload
{
    public static final Type<ToggleMagnetPacket> TYPE =
            new Type<>(BeyondDimensions.makeId("toggle_magnet_packet"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ToggleMagnetPacket> STREAM_CODEC = new StreamCodec<RegistryFriendlyByteBuf, ToggleMagnetPacket>()
    {
        @Override
        public @NotNull ToggleMagnetPacket decode(RegistryFriendlyByteBuf registryFriendlyByteBuf)
        {
            return new ToggleMagnetPacket(registryFriendlyByteBuf.readEnum(MagnetToggleType.class));
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, ToggleMagnetPacket toggleMagnetPacket)
        {
            buf.writeEnum(toggleMagnetPacket.toggleType);
        }
    };

    private void handleInClient(final IPayloadContext context)
    {

    }

    private void toggleMagnet(Player player, List<ItemStack> itemStackList)
    {
        for (ItemStack stack : itemStackList)
        {
            if (stack.getItem() instanceof NetMagnetItem magnet)
            {
                Machine machine = magnet.machine(stack);
                switch (toggleType)
                {
                    case ALL -> toggle(player, machine, BDMachineSettings.WORKING.get(), RedStoneControlMode.IGNORE, RedStoneControlMode.NOT_WORKING,
                            "msg.beyonddimensions.magnet.open", "msg.beyonddimensions.magnet.close");
                    case ITEM -> toggle(player, machine, BDMachineSettings.COLLECT_ITEMS.get(), HopperItemMode.ALLOW, HopperItemMode.DENY,
                            "msg.beyonddimensions.magnet.itemopen", "msg.beyonddimensions.magnet.itemclose");
                    case FLUID -> toggle(player, machine, BDMachineSettings.COLLECT_FLUIDS.get(), HopperFluidMode.ALLOW, HopperFluidMode.DENY,
                            "msg.beyonddimensions.magnet.fluidopen", "msg.beyonddimensions.magnet.fluidclose");
                }
            }
        }
    }

    // 在 on 与 off 之间切换一项设置，并告诉玩家切换后的状态
    private static <T> void toggle(Player player, Machine machine, MachineSetting<T> setting, T on, T off, String openedKey, String closedKey)
    {
        boolean opening = !on.equals(machine.setting(setting));
        machine.setSetting(setting, opening ? on : off);
        player.sendSystemMessage(Component.translatable(opening ? openedKey : closedKey));
    }

    private void handleInServer(final IPayloadContext context)
    {
        Player player = context.player();

        toggleMagnet(player, player.getInventory().items);

        if (ModPresence.isLoaded(OtherModIds.CURIOS))
        {
            CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
                List<ItemStack> curios = handler.findCurios(stack -> !stack.isEmpty())
                        .stream()
                        .map(SlotResult::stack)
                        .toList();

                toggleMagnet(player, curios);
            });
        }
    }

    public static void handle(final ToggleMagnetPacket packet, final IPayloadContext context)
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
