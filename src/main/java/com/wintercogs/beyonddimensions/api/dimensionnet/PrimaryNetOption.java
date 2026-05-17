package com.wintercogs.beyonddimensions.api.dimensionnet;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public record PrimaryNetOption(int netId, @NotNull NetPermissionlevel permission, @NotNull String customName)
{
    private static final String NET_ID = "net_id";
    private static final String PERMISSION = "permission";
    private static final String CUSTOM_NAME = "custom_name";

    public CompoundTag save()
    {
        CompoundTag tag = new CompoundTag();
        tag.putInt(NET_ID, netId);
        tag.putString(PERMISSION, permission.name());
        if (!customName.isEmpty())
        {
            tag.putString(CUSTOM_NAME, customName);
        }
        return tag;
    }

    public Component getNetworkName()
    {
        return DimensionsNet.getNetworkName(netId, customName);
    }

    public static PrimaryNetOption load(CompoundTag tag)
    {
        return new PrimaryNetOption(
                tag.getIntOr(NET_ID, DimensionsNet.NO_PRIMARY_NET_ID),
                NetPermissionlevel.valueOf(tag.getStringOr(PERMISSION, NetPermissionlevel.Member.name())),
                tag.getStringOr(CUSTOM_NAME, "")
        );
    }
}
