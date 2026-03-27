package com.wintercogs.beyonddimensions.api.dimensionnet;

import net.minecraft.nbt.CompoundTag;

public record PrimaryNetOption(int netId, NetPermissionlevel permission)
{
    private static final String NET_ID = "NetId";
    private static final String PERMISSION = "Permission";

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
                tag.getInt(NET_ID),
                tag.contains(PERMISSION) ? NetPermissionlevel.valueOf(tag.getString(PERMISSION)) : NetPermissionlevel.Member
        );
    }
}
