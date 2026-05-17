package com.wintercogs.beyonddimensions.integration.module.jade;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.api.dimensionnet.DimensionsNet;
import com.wintercogs.beyonddimensions.api.dimensionnet.NetPermissionlevel;
import com.wintercogs.beyonddimensions.common.block.entity.NetedBlockEntity;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.StreamServerDataProvider;
import snownee.jade.api.config.IPluginConfig;

public enum NetedBlockNetworkProvider implements StreamServerDataProvider<@NotNull BlockAccessor, NetedBlockNetworkProvider.Data>
{
    INSTANCE;

    private static final Identifier UID = BeyondDimensions.makeId("net_info");
    private static final String PERMISSION_NONE = "none";

    @Override
    public Data streamData(BlockAccessor accessor)
    {
        BlockEntity blockEntity = accessor.getBlockEntity();
        if (!(blockEntity instanceof NetedBlockEntity netedBlockEntity))
        {
            return Data.unbound();
        }

        int netId = netedBlockEntity.getNetId();
        if (netId < 0)
        {
            return Data.unbound();
        }

        DimensionsNet net = DimensionsNet.getNetFromId(netId);
        if (net == null)
        {
            return Data.unbound();
        }

        return new Data(true, netId, net.getCustomName(), resolvePermission(net, accessor.getPlayer()));
    }

    @Override
    public @NotNull StreamCodec<RegistryFriendlyByteBuf, Data> streamCodec()
    {
        return Data.STREAM_CODEC;
    }

    @Override
    public boolean shouldRequestData(BlockAccessor accessor)
    {
        return accessor.getBlockEntity() instanceof NetedBlockEntity;
    }

    @Override
    public @NotNull Identifier getUid()
    {
        return UID;
    }

    private static String resolvePermission(DimensionsNet net, Player player)
    {
        if (net.isOwner(player.getUUID()))
        {
            return NetPermissionlevel.Owner.name();
        }
        if (net.isManager(player.getUUID()))
        {
            return NetPermissionlevel.Manager.name();
        }
        if (net.getPlayers().contains(player.getUUID()))
        {
            return NetPermissionlevel.Member.name();
        }
        return PERMISSION_NONE;
    }

    private static Component buildPermissionLabel(String permission)
    {
        if (PERMISSION_NONE.equals(permission))
        {
            return Component.translatable("tooltip.jade.beyonddimensions.net.permission.none");
        }

        try
        {
            return switch (NetPermissionlevel.valueOf(permission))
            {
                case Owner -> Component.translatable("menu.text.beyonddimensions.primary_net_switcher.permission.owner");
                case Manager -> Component.translatable("menu.text.beyonddimensions.primary_net_switcher.permission.manager");
                case Member -> Component.translatable("menu.text.beyonddimensions.primary_net_switcher.permission.member");
            };
        }
        catch (IllegalArgumentException ignored)
        {
            return Component.translatable("tooltip.jade.beyonddimensions.net.permission.none");
        }
    }

    public record Data(boolean bound, int netId, String customName, String permission)
    {
        public static final StreamCodec<RegistryFriendlyByteBuf, Data> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.BOOL,
                Data::bound,
                ByteBufCodecs.VAR_INT,
                Data::netId,
                ByteBufCodecs.STRING_UTF8,
                Data::customName,
                ByteBufCodecs.STRING_UTF8,
                Data::permission,
                Data::new
        );

        public static Data unbound()
        {
            return new Data(false, DimensionsNet.NO_PRIMARY_NET_ID, "", PERMISSION_NONE);
        }
    }

    public static class Client implements IBlockComponentProvider
    {
        public static final Client INSTANCE = new Client();

        @Override
        public void appendTooltip(@NotNull ITooltip tooltip, BlockAccessor accessor, @NotNull IPluginConfig config)
        {
            Data data = NetedBlockNetworkProvider.INSTANCE.decodeFromData(accessor).orElse(null);
            if (data == null)
            {
                if (accessor.getBlockEntity() instanceof NetedBlockEntity blockEntity && blockEntity.getNetId() < 0)
                {
                    tooltip.add(Component.translatable("tooltip.jade.beyonddimensions.net.unbound"));
                }
                return;
            }

            if (!data.bound())
            {
                tooltip.add(Component.translatable("tooltip.jade.beyonddimensions.net.unbound"));
                return;
            }

            Component networkName = DimensionsNet.getNetworkName(data.netId(), data.customName());
            tooltip.add(Component.translatable(
                    "tooltip.jade.beyonddimensions.net.bound",
                    networkName,
                    data.netId(),
                    buildPermissionLabel(data.permission())
            ));
        }

        @Override
        public @NotNull Identifier getUid()
        {
            return UID;
        }
    }
}
