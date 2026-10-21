package com.wintercogs.beyonddimensions.common.menu;

import com.wintercogs.beyonddimensions.api.dimensionnet.DimensionsNet;
import com.wintercogs.beyonddimensions.api.dimensionnet.NetPermissionlevel;
import com.wintercogs.beyonddimensions.api.dimensionnet.PrimaryNetOption;
import com.wintercogs.beyonddimensions.api.menu.BdMenuActions;
import dev.compixel.forge.sync.MenuSync;
import dev.compixel.forge.sync.SyncedMenu;
import dev.compixel.sync.state.SyncCodec;
import dev.compixel.sync.state.SyncCodecs;
import dev.compixel.sync.state.SyncSchema;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.wintercogs.beyonddimensions.common.init.BDMenus.Primary_Net_Switcher_Menu;

public class PrimaryNetSwitcherMenu extends BDBaseMenu implements SyncedMenu
{
    private static final SyncCodec<String> NAME_CODEC = SyncCodecs.STRING;
    private static final NetPermissionlevel[] PERMISSIONS = NetPermissionlevel.values();
    private static final SyncCodec<PrimaryNetOption> OPTION_CODEC = SyncCodec.of(
            "beyonddimensions:primary_option/1",
            (out, option) -> {
                out.writeInt(option.netId());
                out.writeByte(option.permission().ordinal());
                NAME_CODEC.write(out, option.customName());
            }, in -> {
                int id = in.readInt();
                int permission = in.readUnsignedByte();
                if (permission >= PERMISSIONS.length) throw new java.io.IOException("Invalid network permission");
                return new PrimaryNetOption(id, PERMISSIONS[permission], NAME_CODEC.read(in));
            });
    private static final SyncSchema<PrimaryNetSwitcherMenu> SYNC_SCHEMA =
            SyncSchema.<PrimaryNetSwitcherMenu>builder("beyonddimensions:primary_net_menu", 1)
                    .field("primary", SyncCodecs.INT, menu -> menu.currentPrimaryNetId,
                            (menu, value) -> menu.currentPrimaryNetId = value)
                    .keyedCollection("networks", SyncCodecs.INT, OPTION_CODEC, PrimaryNetOption::netId,
                            menu -> menu.options, (menu, values) -> menu.options = values.stream()
                                    .sorted(java.util.Comparator.comparingInt(PrimaryNetOption::netId)).toList())
                    .build();

    public int currentPrimaryNetId = DimensionsNet.NO_PRIMARY_NET_ID;
    public List<PrimaryNetOption> options = List.of();
    private final MenuSync<PrimaryNetSwitcherMenu> synchronization = BdMenuActions.attach(this, MenuSync.bind(this, SYNC_SCHEMA));

    @Override
    public MenuSync<PrimaryNetSwitcherMenu> menuSync()
    {
        return synchronization;
    }

    public PrimaryNetSwitcherMenu(int id, Inventory playerInventory, FriendlyByteBuf data)
    {
        this(id, playerInventory);
    }

    public PrimaryNetSwitcherMenu(int containerId, Inventory playerInventory)
    {
        super(Primary_Net_Switcher_Menu.get(), containerId, playerInventory);

        if (!player.level().isClientSide())
        {
            refreshSnapshot();
        }
    }

    @Override
    public void broadcastChanges()
    {
        super.broadcastChanges();

        if (player.level().isClientSide()) return;

        refreshSnapshot();
    }

    @Override
    public boolean stillValid(@NotNull Player player)
    {
        return true;
    }

    private void refreshSnapshot()
    {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        currentPrimaryNetId = resolveCurrentPrimaryNetId(serverPlayer);
        List<PrimaryNetOption> next = buildOptions(serverPlayer);
        // 内容没变时沿用原来的不可变列表，库就会跳过集合比较。
        if (!next.equals(options)) options = List.copyOf(next);
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

}
