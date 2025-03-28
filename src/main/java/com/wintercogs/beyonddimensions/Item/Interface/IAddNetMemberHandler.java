package com.wintercogs.beyonddimensions.Item.Interface;

import com.wintercogs.beyonddimensions.DataBase.DimensionsNet;
import net.minecraft.entity.player.EntityPlayer;

public interface IAddNetMemberHandler {
    default boolean AddPlayerToNet(DimensionsNet net, EntityPlayer player)
    {
        if(net != null)
        {
            net.addPlayer(player.getUniqueID());
            return true;
        }

        return false;
    }
}
