package com.wintercogs.beyonddimensions.api.dimensionnet;

import net.minecraft.nbt.CompoundTag;

public record PrimaryNetOption(int netId, NetPermissionlevel permission)
{
    private static final String NET_ID = "net_id";
    private static final String PERMISSION = "permission";

    public CompoundTag save()
    {
        CompoundTag tag = new CompoundTag();
        tag.putInt(NET_ID, netId);
        tag.putString(PERMISSION, permission.name());
        return tag;
    }

    public static PrimaryNetOption load(CompoundTag tag)
    {
        return new PrimaryNetOption(
                tag.getIntOr(NET_ID, DimensionsNet.NO_PRIMARY_NET_ID),
                NetPermissionlevel.valueOf(tag.getStringOr(PERMISSION, NetPermissionlevel.Member.name()))
        );
    }
}
