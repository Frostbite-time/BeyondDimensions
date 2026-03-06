package com.wintercogs.beyonddimensions.common.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.wintercogs.beyonddimensions.api.dimensionnet.DimensionsNet;
import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;

@Mod.EventBusSubscriber(modid = BDConstants.MODID)
public final class ServerCommands
{
    private ServerCommands()
    {
    }

    private static final int OP_LEVEL = 2;

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event)
    {
        event.getDispatcher().register(
                Commands.literal("bdtools")
                        .requires(src -> src.hasPermission(OP_LEVEL))
                        .then(
                                Commands.literal("network")
                                        // --------------------
                                        // 设置所有者
                                        // --------------------
                                        .then(
                                                Commands.literal("setOwner")
                                                        .then(
                                                                Commands.argument("netId", IntegerArgumentType.integer(0))
                                                                        .executes(ctx -> setOwner(
                                                                                ctx.getSource(),
                                                                                IntegerArgumentType.getInteger(ctx, "netId"),
                                                                                null
                                                                        ))
                                                                        .then(
                                                                                Commands.argument("player", EntityArgument.player())
                                                                                        .executes(ctx -> setOwner(
                                                                                                ctx.getSource(),
                                                                                                IntegerArgumentType.getInteger(ctx, "netId"),
                                                                                                EntityArgument.getPlayer(ctx, "player")
                                                                                        ))
                                                                        )
                                                        )
                                        )

                                        // --------------------
                                        // 添加/移除管理者
                                        // --------------------
                                        .then(
                                                Commands.literal("addManager")
                                                        .then(
                                                                Commands.argument("netId", IntegerArgumentType.integer(0))
                                                                        .then(
                                                                                Commands.argument("player", EntityArgument.player())
                                                                                        .executes(ctx -> addManager(
                                                                                                ctx.getSource(),
                                                                                                IntegerArgumentType.getInteger(ctx, "netId"),
                                                                                                EntityArgument.getPlayer(ctx, "player")
                                                                                        ))
                                                                        )
                                                        )
                                        )
                                        .then(
                                                Commands.literal("removeManager")
                                                        .then(
                                                                Commands.argument("netId", IntegerArgumentType.integer(0))
                                                                        .then(
                                                                                Commands.argument("player", EntityArgument.player())
                                                                                        .executes(ctx -> removeManager(
                                                                                                ctx.getSource(),
                                                                                                IntegerArgumentType.getInteger(ctx, "netId"),
                                                                                                EntityArgument.getPlayer(ctx, "player")
                                                                                        ))
                                                                        )
                                                        )
                                        )

                                        // --------------------
                                        // 添加/移除玩家
                                        // --------------------
                                        .then(
                                                Commands.literal("addPlayer")
                                                        .then(
                                                                Commands.argument("netId", IntegerArgumentType.integer(0))
                                                                        .then(
                                                                                Commands.argument("player", EntityArgument.player())
                                                                                        .executes(ctx -> addPlayer(
                                                                                                ctx.getSource(),
                                                                                                IntegerArgumentType.getInteger(ctx, "netId"),
                                                                                                EntityArgument.getPlayer(ctx, "player")
                                                                                        ))
                                                                        )
                                                        )
                                        )
                                        .then(
                                                Commands.literal("removePlayer")
                                                        .then(
                                                                Commands.argument("netId", IntegerArgumentType.integer(0))
                                                                        .then(
                                                                                Commands.argument("player", EntityArgument.player())
                                                                                        .executes(ctx -> removePlayer(
                                                                                                ctx.getSource(),
                                                                                                IntegerArgumentType.getInteger(ctx, "netId"),
                                                                                                EntityArgument.getPlayer(ctx, "player")
                                                                                        ))
                                                                        )
                                                        )
                                        )

                                        // --------------------
                                        // 为玩家创建新网络（玩家必须尚未拥有网络）
                                        // --------------------
                                        .then(
                                                Commands.literal("create")
                                                        .then(
                                                                Commands.argument("player", EntityArgument.player())
                                                                        // /bdtools network create <player>
                                                                        .executes(ctx -> createNetForPlayer(
                                                                                ctx.getSource(),
                                                                                EntityArgument.getPlayer(ctx, "player"),
                                                                                null,
                                                                                null
                                                                        ))
                                                                        // /bdtools network create <player> <slotCapacity>
                                                                        .then(
                                                                                Commands.argument("slotCapacity", LongArgumentType.longArg(1))
                                                                                        .executes(ctx -> createNetForPlayer(
                                                                                                ctx.getSource(),
                                                                                                EntityArgument.getPlayer(ctx, "player"),
                                                                                                LongArgumentType.getLong(ctx, "slotCapacity"),
                                                                                                null
                                                                                        ))
                                                                                        // /bdtools network create <player> <slotCapacity> <slotMaxSize>
                                                                                        .then(
                                                                                                Commands.argument("slotMaxSize", IntegerArgumentType.integer(1))
                                                                                                        .executes(ctx -> createNetForPlayer(
                                                                                                                ctx.getSource(),
                                                                                                                EntityArgument.getPlayer(ctx, "player"),
                                                                                                                LongArgumentType.getLong(ctx, "slotCapacity"),
                                                                                                                IntegerArgumentType.getInteger(ctx, "slotMaxSize")
                                                                                                        ))
                                                                                        )
                                                                        )
                                                        )
                                        )

                                        // --------------------
                                        // 按id或玩家删除网络
                                        // --------------------
                                        .then(
                                                Commands.literal("deleteNet")
                                                        .then(
                                                                Commands.argument("netId", IntegerArgumentType.integer(0))
                                                                        .executes(ctx -> deleteNetById(
                                                                                ctx.getSource(),
                                                                                IntegerArgumentType.getInteger(ctx, "netId")
                                                                        ))
                                                        )
                                        )
                                        .then(
                                                Commands.literal("deleteNetByPlayer")
                                                        .then(
                                                                Commands.argument("player", EntityArgument.player())
                                                                        .executes(ctx -> deleteNetByPlayer(
                                                                                ctx.getSource(),
                                                                                EntityArgument.getPlayer(ctx, "player")
                                                                        ))
                                                        )
                                        )
                        )
        );
    }

    // 辅助函数

    private static @Nullable ServerPlayer resolveSourcePlayerOrNull(CommandSourceStack source)
    {
        try
        {
            return source.getPlayer();
        }
        catch (Exception ignored)
        {
            return null;
        }
    }

    private static @Nullable DimensionsNet getNetOrFail(CommandSourceStack source, int netId)
    {
        DimensionsNet net = DimensionsNet.getNetFromId(netId);
        if (net == null)
        {
            source.sendFailure(Component.literal("ID does not correspond to any network (or it was deleted)."));
        }
        return net;
    }

    // 命令执行

    private static int setOwner(CommandSourceStack source, int netId, @Nullable ServerPlayer targetOrNull)
    {
        DimensionsNet net = getNetOrFail(source, netId);
        if (net == null) return 0;

        ServerPlayer target = (targetOrNull != null) ? targetOrNull : resolveSourcePlayerOrNull(source);
        if (target == null)
        {
            source.sendFailure(Component.literal("This command must specify a player when run from console."));
            return 0;
        }

        net.setOwner(target.getUUID());

        source.sendSuccess(
                () -> Component.literal("Set network owner: netId=" + netId + ", player=" + target.getGameProfile().getName()),
                true
        );
        return Command.SINGLE_SUCCESS;
    }

    private static int addManager(CommandSourceStack source, int netId, ServerPlayer target)
    {
        DimensionsNet net = getNetOrFail(source, netId);
        if (net == null) return 0;

        net.addManager(target.getUUID());

        source.sendSuccess(
                () -> Component.literal("Added manager: netId=" + netId + ", player=" + target.getGameProfile().getName()),
                true
        );
        return Command.SINGLE_SUCCESS;
    }

    private static int removeManager(CommandSourceStack source, int netId, ServerPlayer target)
    {
        DimensionsNet net = getNetOrFail(source, netId);
        if (net == null) return 0;

        net.removeManager(target.getUUID());

        source.sendSuccess(
                () -> Component.literal("Removed manager (downgraded to member): netId=" + netId + ", player=" + target.getGameProfile().getName()),
                true
        );
        return Command.SINGLE_SUCCESS;
    }

    private static int addPlayer(CommandSourceStack source, int netId, ServerPlayer target)
    {
        DimensionsNet net = getNetOrFail(source, netId);
        if (net == null) return 0;

        net.addPlayer(target.getUUID());

        source.sendSuccess(
                () -> Component.literal("Added player to network: netId=" + netId + ", player=" + target.getGameProfile().getName()),
                true
        );
        return Command.SINGLE_SUCCESS;
    }

    private static int removePlayer(CommandSourceStack source, int netId, ServerPlayer target)
    {
        DimensionsNet net = getNetOrFail(source, netId);
        if (net == null) return 0;

        net.removePlayer(target.getUUID());

        source.sendSuccess(
                () -> Component.literal("Removed player from network: netId=" + netId + ", player=" + target.getGameProfile().getName()),
                true
        );
        return Command.SINGLE_SUCCESS;
    }

    /**
     * 给指定玩家创建网络：要求对方当前不在任何网络里。
     */
    private static int createNetForPlayer(CommandSourceStack source,
                                          ServerPlayer target,
                                          @Nullable Long slotCapacityOverride,
                                          @Nullable Integer slotMaxSizeOverride)
    {
        // 对方不能已有网络
        DimensionsNet existing = DimensionsNet.getNetFromPlayer(target);
        if (existing != null)
        {
            source.sendFailure(Component.literal("Player already has a network: player=" + target.getGameProfile().getName() + ", netId=" + existing.getId()));
            return 0;
        }

        long slotCapacity = (slotCapacityOverride != null)
                ? slotCapacityOverride
                : Long.MAX_VALUE;

        int slotMaxSize = (slotMaxSizeOverride != null)
                ? slotMaxSizeOverride
                : Integer.MAX_VALUE;

        DimensionsNet created = DimensionsNet.createNewNetForPlayer(target, slotCapacity, slotMaxSize);
        if (created == null)
        {
            source.sendFailure(Component.literal("Failed to create network."));
            return 0;
        }

        source.sendSuccess(
                () -> Component.literal("Created network: netId=" + created.getId()
                        + ", owner=" + target.getGameProfile().getName()
                        + ", slotCapacity=" + slotCapacity
                        + ", slotMaxSize=" + slotMaxSize),
                true
        );
        return Command.SINGLE_SUCCESS;
    }

    private static int deleteNetById(CommandSourceStack source, int netId)
    {
        DimensionsNet net = getNetOrFail(source, netId);
        if (net == null) return 0;

        net.destroySelf();

        source.sendSuccess(
                () -> Component.literal("Deleted network: netId=" + netId + " (marked deleted)."),
                true
        );
        return Command.SINGLE_SUCCESS;
    }

    private static int deleteNetByPlayer(CommandSourceStack source, ServerPlayer target)
    {
        DimensionsNet net = DimensionsNet.getNetFromPlayer(target);
        if (net == null)
        {
            source.sendFailure(Component.literal("Player does not belong to any network: player=" + target.getGameProfile().getName()));
            return 0;
        }

        int netId = net.getId();
        net.destroySelf();

        source.sendSuccess(
                () -> Component.literal("Deleted the network that the player belongs to: player=" + target.getGameProfile().getName() + ", netId=" + netId + "."),
                true
        );
        return Command.SINGLE_SUCCESS;
    }
}