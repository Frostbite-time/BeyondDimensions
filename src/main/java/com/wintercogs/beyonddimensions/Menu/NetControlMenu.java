package com.wintercogs.beyonddimensions.Menu;

import com.wintercogs.beyonddimensions.DataBase.DimensionsNet;
import com.wintercogs.beyonddimensions.DataBase.NetControlAction;
import com.wintercogs.beyonddimensions.DataBase.PlayerPermissionInfo;
import com.wintercogs.beyonddimensions.Network.Packet.toClient.PlayerPermissionInfoPacket;
import com.wintercogs.beyonddimensions.Registry.PacketRegister;
import com.wintercogs.beyonddimensions.Registry.UIRegister;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.UUID;
import java.util.function.Supplier;

public class NetControlMenu extends BDOrderedContainerMenu
{

    // 设为临时，服务端会在初始化时重设
    private DimensionsNet net = new DimensionsNet(true);
    public HashMap<UUID, PlayerPermissionInfo> playerInfo = new HashMap<>();




    /**
     * 客户端构造函数
     * @param playerInventory 玩家背包
     */
    public NetControlMenu(int id, Inventory playerInventory, FriendlyByteBuf data)
    {
        this(id,playerInventory);
    }

    public NetControlMenu(int containerId, Inventory playerInventory)
    {
        super(UIRegister.Net_Control_Menu.get(),containerId, playerInventory,null);

        if(!player.level().isClientSide())
        {
            net = DimensionsNet.getNetFromPlayer(player);
            playerInfo = net.getPlayerPermissionInfoMap(player.level());
        }
    }

    public void handlePlayerAction(UUID receiver, NetControlAction action)
    {
        if(action == NetControlAction.SetOwner)
        {
            // 执行者是所有者，且接收者不为玩家，则可以设置新所有者
            if(net.isOwner(player)&&!player.getUUID().equals(receiver))
            {
                net.setOwner(receiver);
            }
        }
        else if(action == NetControlAction.SetManager)
        {
            // 执行者是所有者，且接收者不为管理员，则可以被添加为管理员
            if(net.isOwner(player)&&!net.isManager(receiver))
            {
                net.addManager(receiver);
            }
        }
        else if(action == NetControlAction.RemoveManager)
        {
            // 执行者是所有者，且接收者为管理员，且接收者并非所有者，则可以被移除管理员权限
            if(net.isOwner(player)&&net.isManager(receiver)&&!net.isOwner(receiver))
            {
                net.removeManager(receiver);
            }
        }
        else if(action == NetControlAction.RemovePlayer)
        {
            // 管理员可以移除任何非管理员
            if(net.isManager(player)&&!net.isManager(receiver))
            {
                net.removePlayer(receiver);
            }
            else if(player.getUUID().equals(receiver)&&!net.isOwner(receiver)) // 任何人都可以直接移除自己，除非是所有者
            {
                net.removePlayer(receiver);
            }
            else if(net.isOwner(player)&&!player.getUUID().equals(receiver)) // 所有者可以移除自己之外的任何人
            {
                net.removePlayer(receiver);
            }
        }
    }

    @Override
    protected void updateChange()
    {
        if(!net.getPlayerPermissionInfoMap(player.level()).equals(this.playerInfo))
        {
            this.playerInfo = this.net.getPlayerPermissionInfoMap(player.level());
            sendPlayerInfo();
        }
    }


    @Override
    protected void initUpdate()
    {
        sendPlayerInfo();
    }

    public void sendPlayerInfo()
    {
        PacketRegister.INSTANCE.send(PacketDistributor.PLAYER.with(()-> (ServerPlayer)player) ,new PlayerPermissionInfoPacket(playerInfo));
    }

    public void loadPlayerInfo(HashMap<UUID, PlayerPermissionInfo> playerInfo)
    {
        this.playerInfo = playerInfo;
    }

    @Override
    public boolean stillValid(Player player)
    {
        return true;
    }
}
