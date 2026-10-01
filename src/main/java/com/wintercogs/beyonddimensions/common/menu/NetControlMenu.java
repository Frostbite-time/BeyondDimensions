package com.wintercogs.beyonddimensions.common.menu;

import com.wintercogs.beyonddimensions.api.dimensionnet.DimensionsNet;
import com.wintercogs.beyonddimensions.api.dimensionnet.NetControlAction;
import com.wintercogs.beyonddimensions.api.dimensionnet.NetPermissionlevel;
import com.wintercogs.beyonddimensions.api.dimensionnet.PlayerPermissionInfo;
import com.wintercogs.beyonddimensions.api.event.dimensionnet.DimensionsNetEvent;
import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import dev.compixel.forge.sync.MenuAction;
import dev.compixel.forge.sync.MenuSync;
import dev.compixel.sync.state.SyncCodec;
import dev.compixel.sync.state.SyncCodecs;
import dev.compixel.sync.state.SyncSchema;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 网络成员与权限管理（无槽位）。状态由服务端持有，每个操作都会按当前网络状态重新校验。
 */
@EventBusSubscriber(modid = BDConstants.MODID)
public class NetControlMenu extends BDBaseMenu
{
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(Registries.MENU, BDConstants.MODID);
    public static final Supplier<MenuType<NetControlMenu>> Net_Control_Menu = MENU_TYPES.register("net_control_menu", () -> IMenuTypeExtension.create(NetControlMenu::new));

    /**
     * 成员及当前玩家可以对其执行的操作（按 NetControlAction 序号的位掩码）
     */
    public record Member(UUID id, String name, NetPermissionlevel role, int actions)
    {
        public boolean allows(NetControlAction action)
        {
            return (actions & (1 << action.ordinal())) != 0;
        }
    }

    /**
     * 操作请求携带客户端看到的角色，角色已变化时服务端拒绝执行
     */
    public record Request(UUID target, NetControlAction action, NetPermissionlevel expectedRole)
    {
    }

    private static final int ACTION_MASK = (1 << NetControlAction.values().length) - 1;
    private static final SyncCodec<String> NAME = SyncCodecs.string(256);
    private static final SyncCodec<NetPermissionlevel> ROLE = SyncCodecs.enumeration(NetPermissionlevel.class);
    private static final SyncCodec<NetControlAction> ACTION = SyncCodecs.enumeration(NetControlAction.class);

    private static final SyncCodec<Member> MEMBER = SyncCodec.of("beyonddimensions:member/1",
            (out, member) -> {
                SyncCodecs.UUID.write(out, member.id());
                NAME.write(out, member.name());
                ROLE.write(out, member.role());
                out.writeByte(member.actions());
            },
            in -> {
                Member member = new Member(SyncCodecs.UUID.read(in), NAME.read(in), ROLE.read(in), in.readUnsignedByte());
                if ((member.actions() & ~ACTION_MASK) != 0)
                    throw new IOException("Invalid member actions");
                return member;
            });

    private static final SyncCodec<Request> REQUEST = SyncCodec.of("beyonddimensions:member_action/1",
            (out, request) -> {
                SyncCodecs.UUID.write(out, request.target());
                ACTION.write(out, request.action());
                ROLE.write(out, request.expectedRole());
            },
            in -> new Request(SyncCodecs.UUID.read(in), ACTION.read(in), ROLE.read(in)));

    private static final MenuAction<NetControlMenu, Request> APPLY = MenuAction.of("member", REQUEST, NetControlMenu::apply);

    private static final MenuAction<NetControlMenu, Boolean> REFRESH = MenuAction.of("refresh", SyncCodecs.BOOLEAN,
            (menu, player, ignored) -> {
                menu.dirty = true;
                return true;
            });

    private static final SyncSchema<NetControlMenu> SCHEMA = SyncSchema.<NetControlMenu>builder("beyonddimensions:net_control", 1)
            .field("network", SyncCodecs.INT, m -> m.networkId, (m, value) -> m.networkId = value)
            .field("name", NAME,
                    m -> m.player.level().isClientSide() || m.net == null ? m.networkName : m.net.getCustomName(),
                    (m, value) -> m.networkName = value)
            .keyedCollection("members", SyncCodecs.UUID, MEMBER, Member::id, m -> m.members, NetControlMenu::replaceMembers)
            .build();

    // 服务端为当前玩家所在的网络，客户端为 null
    private final DimensionsNet net;
    private final MenuSync<NetControlMenu> synchronization;
    private int networkId = -1;
    private String networkName = "";
    private List<Member> members = List.of();
    private Map<UUID, Member> memberIndex = Map.of();
    private boolean dirty = true;
    private boolean lastSpectator;

    /**
     * 客户端构造函数
     */
    public NetControlMenu(int id, Inventory playerInventory, FriendlyByteBuf data)
    {
        this(id, playerInventory);
    }

    public NetControlMenu(int id, Inventory playerInventory)
    {
        super(Net_Control_Menu.get(), id, playerInventory);
        net = player.level().isClientSide() ? null : DimensionsNet.getNetFromPlayer(player);
        if (net != null)
        {
            networkId = net.getId();
            refreshMembers();
        }
        synchronization = MenuSync.bind(this, SCHEMA).action(APPLY).action(REFRESH);
    }

    @Override
    public MenuSync<NetControlMenu> menuSync()
    {
        return synchronization;
    }

    // 客户端读取接口

    public boolean ready()
    {
        return synchronization.hasSnapshot();
    }

    public int networkId()
    {
        return networkId;
    }

    public String networkName()
    {
        return networkName;
    }

    public List<Member> members()
    {
        return members;
    }

    public boolean request(Request request)
    {
        Member member = memberIndex.get(request.target());
        return synchronization.hasSnapshot()
                && member != null
                && member.role() == request.expectedRole()
                && member.allows(request.action())
                && synchronization.request(APPLY, request).queued();
    }

    public boolean refresh()
    {
        return synchronization.request(REFRESH, true).queued();
    }

    private void replaceMembers(List<Member> values)
    {
        members = values;
        memberIndex = values.stream().collect(Collectors.toUnmodifiableMap(Member::id, member -> member));
    }

    // 服务端逻辑

    private NetPermissionlevel role(UUID id)
    {
        if (net.isOwner(id))
            return NetPermissionlevel.Owner;
        return net.isManager(id) ? NetPermissionlevel.Manager : NetPermissionlevel.Member;
    }

    private boolean allowed(UUID target, NetControlAction action)
    {
        if (net == null || !player.isAlive() || player.isSpectator() || !stillValid(player) || !net.getPlayers().contains(target))
            return false;
        boolean self = player.getUUID().equals(target);
        return switch (action)
        {
            case SetOwner -> net.isOwner(player) && !self;
            case SetManager -> net.isOwner(player) && !net.isManager(target);
            case RemoveManager -> net.isOwner(player) && net.isManager(target) && !net.isOwner(target);
            case RemovePlayer -> !net.isOwner(target)
                    && (self || net.isOwner(player) || net.isManager(player) && !net.isManager(target));
        };
    }

    private boolean apply(ServerPlayer actor, Request request)
    {
        if (actor != player || !allowed(request.target(), request.action()) || role(request.target()) != request.expectedRole())
            return false;
        switch (request.action())
        {
            case SetOwner -> net.setOwner(request.target());
            case SetManager -> net.addManager(request.target());
            case RemoveManager -> net.removeManager(request.target());
            case RemovePlayer ->
            {
                if (actor.getUUID().equals(request.target()))
                    net.leavePlayer(request.target());
                else
                    net.removePlayer(request.target());
            }
        }
        dirty = true;
        return true;
    }

    private void refreshMembers()
    {
        if (net == null || !stillValid(player))
            return;
        dirty = false;
        lastSpectator = player.isSpectator();
        List<Member> next = new ArrayList<>(net.getPlayers().size());
        for (Map.Entry<UUID, PlayerPermissionInfo> entry : net.getPlayerPermissionInfoMap(player.getServer()).entrySet())
        {
            int actions = 0;
            for (NetControlAction action : NetControlAction.values())
            {
                if (allowed(entry.getKey(), action))
                    actions |= 1 << action.ordinal();
            }
            next.add(new Member(entry.getKey(), entry.getValue().name(), entry.getValue().level(), actions));
        }
        next.sort(Comparator.comparing(Member::id));
        if (!next.equals(members))
            replaceMembers(List.copyOf(next));
    }

    @Override
    public void broadcastChanges()
    {
        super.broadcastChanges();

        if (player.level().isClientSide()) return;

        if (dirty || lastSpectator != player.isSpectator())
            refreshMembers();
    }

    @Override
    public boolean stillValid(@NotNull Player viewer)
    {
        if (viewer.level().isClientSide())
            return true;
        return net != null && !net.deleted && net.getPlayers().contains(viewer.getUUID()) && viewer == player;
    }

    // 网络成员变化时刷新所有打开此页面的玩家

    private static void changed(DimensionsNet changedNet)
    {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null)
            return;
        if (!server.isSameThread())
        {
            server.execute(() -> changed(changedNet));
            return;
        }
        for (ServerPlayer viewer : server.getPlayerList().getPlayers())
        {
            if (viewer.containerMenu instanceof NetControlMenu menu && menu.net == changedNet)
            {
                menu.dirty = true;
                if (!menu.stillValid(viewer))
                    viewer.closeContainer();
            }
        }
    }

    @SubscribeEvent
    public static void memberChanged(DimensionsNetEvent.MemberChanged event)
    {
        changed(event.getNet());
    }

    @SubscribeEvent
    public static void ownerChanged(DimensionsNetEvent.OwnerChanged event)
    {
        changed(event.getNet());
    }

    @SubscribeEvent
    public static void destroyed(DimensionsNetEvent.Destroyed event)
    {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null)
            return;
        for (ServerPlayer viewer : server.getPlayerList().getPlayers())
        {
            if (viewer.containerMenu instanceof NetControlMenu menu && menu.networkId == event.getDestroyedId())
                viewer.closeContainer();
        }
    }

    @SubscribeEvent
    public static void loggedIn(PlayerEvent.PlayerLoggedInEvent event)
    {
        if (!(event.getEntity() instanceof ServerPlayer online))
            return;
        for (ServerPlayer viewer : online.server.getPlayerList().getPlayers())
        {
            if (viewer.containerMenu instanceof NetControlMenu menu && menu.net != null && menu.net.getPlayers().contains(online.getUUID()))
                menu.dirty = true;
        }
    }
}
