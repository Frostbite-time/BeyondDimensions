package com.wintercogs.beyonddimensions.network.packet.c2s;

import com.wintercogs.beyonddimensions.api.machine.Machine;
import com.wintercogs.beyonddimensions.api.machine.MachineSetting;
import com.wintercogs.beyonddimensions.client.gui.MagnetToggleType;
import com.wintercogs.beyonddimensions.common.init.BDMachineSettings;
import com.wintercogs.beyonddimensions.common.item.NetMagnetItem;
import com.wintercogs.beyonddimensions.common.machine.setting.HopperFluidMode;
import com.wintercogs.beyonddimensions.common.machine.setting.HopperItemMode;
import com.wintercogs.beyonddimensions.common.machine.setting.RedStoneControlMode;
import com.wintercogs.beyonddimensions.integration.ModPresence;
import com.wintercogs.beyonddimensions.integration.OtherModIds;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import top.theillusivec4.curios.api.SlotResult;

import java.util.List;
import java.util.function.Supplier;

public record ToggleMagnetPacket(MagnetToggleType toggleType)
{
    private void toggleMagnet(Player player, List<ItemStack> itemStackList)
    {
        for (ItemStack stack : itemStackList)
        {
            if (stack.getItem() instanceof NetMagnetItem magnet)
            {
                Machine machine = magnet.machine(stack);
                switch (toggleType)
                {
                    case ALL ->
                            toggle(player, machine, BDMachineSettings.WORKING.get(), RedStoneControlMode.IGNORE, RedStoneControlMode.NOT_WORKING,
                                    "msg.beyonddimensions.magnet.open", "msg.beyonddimensions.magnet.close");
                    case ITEM ->
                            toggle(player, machine, BDMachineSettings.COLLECT_ITEMS.get(), HopperItemMode.ALLOW, HopperItemMode.DENY,
                                    "msg.beyonddimensions.magnet.itemopen", "msg.beyonddimensions.magnet.itemclose");
                    case FLUID ->
                            toggle(player, machine, BDMachineSettings.COLLECT_FLUIDS.get(), HopperFluidMode.ALLOW, HopperFluidMode.DENY,
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

    private void handle(NetworkEvent.Context context)
    {
        Player player = context.getSender();
        if (player == null)
        {
            return;
        }

        toggleMagnet(player, player.getInventory().items);

        if (ModPresence.isLoaded(OtherModIds.CURIOS))
        {
            top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
                List<ItemStack> curios = handler.findCurios(stack -> !stack.isEmpty())
                        .stream()
                        .map(SlotResult::stack)
                        .toList();

                toggleMagnet(player, curios);
            });
        }
    }


    public static void handle(ToggleMagnetPacket packet, Supplier<NetworkEvent.Context> cxt)
    {
        if (packet != null)
        {
            NetworkEvent.Context context = cxt.get();
            context.enqueueWork(() -> packet.handle(context));
            context.setPacketHandled(true);
        }
    }

    public static void encode(ToggleMagnetPacket packet, FriendlyByteBuf buf)
    {
        buf.writeEnum(packet.toggleType());
    }

    public static ToggleMagnetPacket decode(FriendlyByteBuf buf)
    {
        return new ToggleMagnetPacket(buf.readEnum(MagnetToggleType.class));
    }
}
