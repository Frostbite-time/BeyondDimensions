package com.wintercogs.beyonddimensions.common.menu;

import com.wintercogs.beyonddimensions.api.dimensionnet.DimensionsNet;
import com.wintercogs.beyonddimensions.api.dimensionnet.NetPermissionlevel;
import com.wintercogs.beyonddimensions.api.dimensionnet.PrimaryNetOption;
import com.wintercogs.beyonddimensions.common.init.BDMenus;
import com.wintercogs.beyonddimensions.common.init.BDPackets;
import com.wintercogs.beyonddimensions.network.packet.both.QuickDataTagPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class PrimaryNetSwitcherMenu extends BDBaseMenu
{
    private static final String CURRENT_PRIMARY_NET_ID = "CurrentPrimaryNetId";
    private static final String OPTIONS = "Options";

    public int currentPrimaryNetId = DimensionsNet.NO_PRIMARY_NET_ID;
    public List<PrimaryNetOption> options = List.of();

    private CompoundTag lastSnapshotTag = new CompoundTag();

    public PrimaryNetSwitcherMenu(int id, Inventory playerInventory, net.minecraft.network.FriendlyByteBuf data)
    {
        this(id, playerInventory);
    }

    public PrimaryNetSwitcherMenu(int containerId, Inventory playerInventory)
    {
        this(BDMenus.Primary_Net_Switcher_Menu.get(), containerId, playerInventory);
    }

    public PrimaryNetSwitcherMenu(MenuType<?> menuType, int containerId, Inventory playerInventory)
    {
        super(menuType, containerId, playerInventory);

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
        currentPrimaryNetId = tag.contains(CURRENT_PRIMARY_NET_ID) ? tag.getInt(CURRENT_PRIMARY_NET_ID) : DimensionsNet.NO_PRIMARY_NET_ID;

        ListTag optionList = tag.getList(OPTIONS, 10);
        List<PrimaryNetOption> loadedOptions = new ArrayList<>(optionList.size());
        for (int i = 0; i < optionList.size(); i++)
        {
            loadedOptions.add(PrimaryNetOption.load(optionList.getCompound(i)));
        }
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
            BDPackets.INSTANCE.send(PacketDistributor.PLAYER.with(() -> serverPlayer), new QuickDataTagPacket(snapshotTag));
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
            builtOptions.add(new PrimaryNetOption(net.getId(), resolvePermission(net, playerId)));
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
