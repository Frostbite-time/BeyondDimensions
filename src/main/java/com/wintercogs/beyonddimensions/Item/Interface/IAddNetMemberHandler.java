package com.wintercogs.beyonddimensions.Item.Interface;

import com.wintercogs.beyonddimensions.DataBase.DimensionsNet;
import net.minecraft.world.entity.player.Player;

public interface IAddNetMemberHandler {
    default boolean AddPlayerToNet(DimensionsNet net, Player player)
    {
        if(net != null)
        {
            net.addPlayer(player.getUUID());
            return true;
        }

        return false;
    }
}
