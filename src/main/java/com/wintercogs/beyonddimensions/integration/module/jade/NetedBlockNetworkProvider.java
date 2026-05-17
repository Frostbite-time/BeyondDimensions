package com.wintercogs.beyonddimensions.integration.module.jade;

import com.wintercogs.beyonddimensions.api.dimensionnet.DimensionsNet;
import com.wintercogs.beyonddimensions.api.dimensionnet.NetPermissionlevel;
import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import com.wintercogs.beyonddimensions.common.block.entity.NetedBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum NetedBlockNetworkProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor>
{
    INSTANCE;

    private static final ResourceLocation UID = new ResourceLocation(BDConstants.MODID, "net_info");
    private static final String BOUND = "bound";
    private static final String NET_ID = "net_id";
    private static final String CUSTOM_NAME = "custom_name";
    private static final String PERMISSION = "permission";
    private static final String PERMISSION_NONE = "none";

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config)
    {
        CompoundTag data = accessor.getServerData();
        if (!data.contains(BOUND))
        {
            if (accessor.getBlockEntity() instanceof NetedBlockEntity blockEntity && blockEntity.getNetId() < 0)
                tooltip.add(Component.translatable("tooltip.jade.beyonddimensions.net.unbound"));
            return;
        }

        if (!data.getBoolean(BOUND))
        {
            tooltip.add(Component.translatable("tooltip.jade.beyonddimensions.net.unbound"));
            return;
        }

        int netId = data.getInt(NET_ID);
        Component networkName = DimensionsNet.getNetworkName(netId, data.getString(CUSTOM_NAME));
        tooltip.add(Component.translatable(
                "tooltip.jade.beyonddimensions.net.bound",
                networkName,
                netId,
                buildPermissionLabel(data.getString(PERMISSION))
        ));
    }

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor)
    {
        data.putBoolean(BOUND, false);

        BlockEntity blockEntity = accessor.getBlockEntity();
        if (!(blockEntity instanceof NetedBlockEntity netedBlockEntity))
            return;

        int netId = netedBlockEntity.getNetId();
        if (netId < 0)
            return;

        DimensionsNet net = DimensionsNet.getNetFromId(netId);
        if (net == null)
            return;

        data.putBoolean(BOUND, true);
        data.putInt(NET_ID, netId);
        data.putString(CUSTOM_NAME, net.getCustomName());
        data.putString(PERMISSION, resolvePermission(net, accessor.getPlayer()));
    }

    @Override
    public ResourceLocation getUid()
    {
        return UID;
    }

    private static String resolvePermission(DimensionsNet net, Player player)
    {
        if (net.isOwner(player.getUUID()))
            return NetPermissionlevel.Owner.name();
        if (net.isManager(player.getUUID()))
            return NetPermissionlevel.Manager.name();
        if (net.getPlayers().contains(player.getUUID()))
            return NetPermissionlevel.Member.name();
        return PERMISSION_NONE;
    }

    private static Component buildPermissionLabel(String permission)
    {
        if (PERMISSION_NONE.equals(permission))
            return Component.translatable("tooltip.jade.beyonddimensions.net.permission.none");

        try
        {
            return switch (NetPermissionlevel.valueOf(permission))
            {
                case Owner ->
                        Component.translatable("menu.text.beyonddimensions.primary_net_switcher.permission.owner");
                case Manager ->
                        Component.translatable("menu.text.beyonddimensions.primary_net_switcher.permission.manager");
                case Member ->
                        Component.translatable("menu.text.beyonddimensions.primary_net_switcher.permission.member");
            };
        }
        catch (IllegalArgumentException ignored)
        {
            return Component.translatable("tooltip.jade.beyonddimensions.net.permission.none");
        }
    }
}
