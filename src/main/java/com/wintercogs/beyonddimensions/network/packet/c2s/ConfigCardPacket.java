package com.wintercogs.beyonddimensions.network.packet.c2s;

import com.wintercogs.beyonddimensions.api.machine.Machine;
import com.wintercogs.beyonddimensions.api.machine.MachineItem;
import com.wintercogs.beyonddimensions.common.item.MachineConfigCardItem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * 请求使用配置卡：pos 为空时作用于主手的物品机器（卡在副手），否则作用于该位置的方块机器（卡在主手）
 */
public record ConfigCardPacket(Operation operation, Optional<BlockPos> pos)
{
    public enum Operation
    {
        COPY,
        PASTE,
        /**
         * 粘贴到这台方块机器以及与它相连的全部同类机器
         */
        PASTE_CONNECTED
    }

    public static ConfigCardPacket block(Operation operation, BlockPos pos)
    {
        return new ConfigCardPacket(operation, Optional.of(pos));
    }

    public static ConfigCardPacket item(boolean paste)
    {
        return new ConfigCardPacket(paste ? Operation.PASTE : Operation.COPY, Optional.empty());
    }

    public static void handle(ConfigCardPacket packet, Supplier<NetworkEvent.Context> cxt)
    {
        NetworkEvent.Context context = cxt.get();
        context.enqueueWork(() -> {
            Player player = context.getSender();
            if (player != null)
                packet.handleInServer(player);
        });
        context.setPacketHandled(true);
    }

    public static void encode(ConfigCardPacket packet, FriendlyByteBuf buf)
    {
        buf.writeEnum(packet.operation());
        buf.writeOptional(packet.pos(), FriendlyByteBuf::writeBlockPos);
    }

    public static ConfigCardPacket decode(FriendlyByteBuf buf)
    {
        return new ConfigCardPacket(buf.readEnum(Operation.class), buf.readOptional(FriendlyByteBuf::readBlockPos));
    }

    private void handleInServer(Player player)
    {
        if (player.isSpectator())
            return;
        if (pos.isEmpty())
            useOnItem(player);
        else
            useOnBlock(player, pos.get());
    }

    private void useOnItem(Player player)
    {
        ItemStack card = player.getItemInHand(InteractionHand.OFF_HAND);
        ItemStack stack = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (!(card.getItem() instanceof MachineConfigCardItem) || !(stack.getItem() instanceof MachineItem item))
            return;
        Machine machine = item.machine(stack);
        if (operation == Operation.COPY)
            MachineConfigCardItem.copy(player, machine, card);
        else if (operation == Operation.PASTE)
            MachineConfigCardItem.paste(player, List.of(machine), card);
    }

    private void useOnBlock(Player player, BlockPos target)
    {
        ItemStack card = player.getItemInHand(InteractionHand.MAIN_HAND);
        Level level = player.level();
        if (!(card.getItem() instanceof MachineConfigCardItem)
                || !level.isLoaded(target) || !player.canReach(target, 1.0) || !level.mayInteract(player, target)
                || !(level.getBlockEntity(target) instanceof Machine machine))
            return;
        switch (operation)
        {
            case COPY -> MachineConfigCardItem.copy(player, machine, card);
            case PASTE -> MachineConfigCardItem.paste(player, List.of(machine), card);
            case PASTE_CONNECTED ->
            {
                List<Machine> machines = new ArrayList<>();
                for (BlockPos pos : MachineConfigCardItem.connected(level, target))
                    if (level.mayInteract(player, pos) && level.getBlockEntity(pos) instanceof Machine connected)
                        machines.add(connected);
                MachineConfigCardItem.paste(player, machines, card);
            }
        }
    }

}
