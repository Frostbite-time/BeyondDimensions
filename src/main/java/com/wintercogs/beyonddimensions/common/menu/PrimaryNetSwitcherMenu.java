package com.wintercogs.beyonddimensions.common.menu;

import com.wintercogs.beyonddimensions.api.dimensionnet.DimensionsNet;
import com.wintercogs.beyonddimensions.api.dimensionnet.NetPermissionlevel;
import com.wintercogs.beyonddimensions.api.dimensionnet.PrimaryNetOption;
import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import com.wintercogs.beyonddimensions.network.packet.both.QuickDataTagPacket;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

public class PrimaryNetSwitcherMenu extends BDBaseMenu
{
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(Registries.MENU, BDConstants.MODID);
    public static final Supplier<MenuType<PrimaryNetSwitcherMenu>> PRIMARY_NET_SWITCHER_MENU = MENU_TYPES.register("primary_net_switcher_menu", () -> IMenuTypeExtension.create(PrimaryNetSwitcherMenu::new));

    private static final String CURRENT_PRIMARY_NET_ID = "current_primary_net_id";
    private static final String OPTIONS = "options";

    public int currentPrimaryNetId = DimensionsNet.NO_PRIMARY_NET_ID;
    public List<PrimaryNetOption> options = List.of();

    private CompoundTag lastSnapshotTag = new CompoundTag();

    public PrimaryNetSwitcherMenu(int id, Inventory playerInventory, FriendlyByteBuf data)
    {
        this(id, playerInventory);
    }

    public PrimaryNetSwitcherMenu(int containerId, Inventory playerInventory)
    {
        super(PRIMARY_NET_SWITCHER_MENU.get(), containerId, playerInventory);

        if (!player.level().isClientSide())
        {
            refreshSnapshot();
        }
    }

    @Override
    protected void initUpdate()
    {
        sendSnapshot();
    }

    @Override
    protected void updateChange()
    {
        if (refreshSnapshot())
        {
            sendSnapshot();
        }
    }

    @Override
    protected void writeQuickDataTag(CompoundTag tag)
    {
        super.writeQuickDataTag(tag);
        tag.putInt(CURRENT_PRIMARY_NET_ID, currentPrimaryNetId);

        ListTag optionList = new ListTag();
        for (PrimaryNetOption option : options)
        {
            optionList.add(option.save());
        }
        tag.put(OPTIONS, optionList);
    }

    @Override
    public void readQuickDataTag(CompoundTag tag)
    {
        super.readQuickDataTag(tag);
        currentPrimaryNetId = tag.getIntOr(CURRENT_PRIMARY_NET_ID, DimensionsNet.NO_PRIMARY_NET_ID);

        ListTag optionList = tag.getListOrEmpty(OPTIONS);
        List<PrimaryNetOption> loadedOptions = new ArrayList<>(optionList.size());
        optionList.forEach(element -> element.asCompound().ifPresent(compoundTag -> loadedOptions.add(PrimaryNetOption.load(compoundTag))));
        options = loadedOptions;
    }

    @Override
    public boolean stillValid(@NotNull Player player)
    {
        return true;
    }

    private boolean refreshSnapshot()
    {
        if (!(player instanceof ServerPlayer serverPlayer))
        {
            return false;
        }

        int nextPrimaryNetId = resolveCurrentPrimaryNetId(serverPlayer);
        List<PrimaryNetOption> nextOptions = buildOptions(serverPlayer);
        CompoundTag nextSnapshotTag = createSnapshotTag(nextPrimaryNetId, nextOptions);
        if (Objects.equals(nextSnapshotTag, lastSnapshotTag))
        {
            return false;
        }

        currentPrimaryNetId = nextPrimaryNetId;
        options = nextOptions;
        lastSnapshotTag = nextSnapshotTag.copy();
        return true;
    }

    private void sendSnapshot()
    {
        if (player instanceof ServerPlayer serverPlayer)
        {
            CompoundTag snapshotTag = new CompoundTag();
            writeQuickDataTag(snapshotTag);
            PacketDistributor.sendToPlayer(serverPlayer, new QuickDataTagPacket(snapshotTag));
        }
    }

    private static int resolveCurrentPrimaryNetId(ServerPlayer player)
    {
        DimensionsNet currentPrimaryNet = DimensionsNet.getPrimaryNetFromPlayer(player);
        return currentPrimaryNet == null ? DimensionsNet.NO_PRIMARY_NET_ID : currentPrimaryNet.getId();
    }

    private static List<PrimaryNetOption> buildOptions(ServerPlayer player)
    {
        UUID playerId = player.getUUID();
        List<DimensionsNet> nets = new ArrayList<>(DimensionsNet.getAllNetFromPlayer(player));
        nets.sort((left, right) -> Integer.compare(left.getId(), right.getId()));

        List<PrimaryNetOption> builtOptions = new ArrayList<>(nets.size());
        for (DimensionsNet net : nets)
        {
            builtOptions.add(new PrimaryNetOption(net.getId(), resolvePermission(net, playerId), net.getCustomName()));
        }
        return builtOptions;
    }

    private static NetPermissionlevel resolvePermission(DimensionsNet net, UUID playerId)
    {
        if (net.isOwner(playerId))
        {
            return NetPermissionlevel.Owner;
        }
        if (net.isManager(playerId))
        {
            return NetPermissionlevel.Manager;
        }
        return NetPermissionlevel.Member;
    }

    private static CompoundTag createSnapshotTag(int primaryNetId, List<PrimaryNetOption> options)
    {
        CompoundTag snapshotTag = new CompoundTag();
        snapshotTag.putInt(CURRENT_PRIMARY_NET_ID, primaryNetId);
        ListTag optionList = new ListTag();
        for (PrimaryNetOption option : options)
        {
            optionList.add(option.save());
        }
        snapshotTag.put(OPTIONS, optionList);
        return snapshotTag;
    }
}
