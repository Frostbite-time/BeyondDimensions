package com.wintercogs.beyonddimensions.Menu;

import com.mojang.logging.LogUtils;
import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.DataBase.DimensionsNet;
import com.wintercogs.beyonddimensions.DataBase.NetControlAction;
import com.wintercogs.beyonddimensions.DataBase.PlayerPermissionInfo;
import com.wintercogs.beyonddimensions.Packet.PlayerPermissionInfoPacket;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.UUID;
import java.util.function.Supplier;

public class NetControlMenu extends AbstractContainerMenu
{
    private static final Logger LOGGER = LogUtils.getLogger();

    private final Player player;

    private DimensionsNet net = new DimensionsNet();
    public HashMap<UUID, PlayerPermissionInfo> playerInfo = new HashMap<>();

    // 构建注册用的信息
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(Registries.MENU, BeyondDimensions.MODID);
    public static final Supplier<MenuType<NetControlMenu>> Net_Control_Menu = MENU_TYPES.register("net_control_menu", () -> IMenuTypeExtension.create(NetControlMenu::new));

    /**
     * 客户端构造函数
     * @param playerInventory 玩家背包
     */
    public NetControlMenu(int id, Inventory playerInventory, FriendlyByteBuf data)
    {
        this(id,playerInventory.player);
    }

    public NetControlMenu(int containerId, Player player)
    {
        super(Net_Control_Menu.get(), containerId);
        this.player = player;

        if(!player.level().isClientSide())
        {
            net = DimensionsNet.getNetFromPlayer(player);
            playerInfo = net.getPlayerPermissionInfoMap(player.level());
        }
    }

    public void sendPlayerInfo()
    {
        PacketDistributor.sendToPlayer((ServerPlayer) player,new PlayerPermissionInfoPacket(playerInfo));
    }

    public void loadPlayerInfo(HashMap<UUID, PlayerPermissionInfo> playerInfo)
    {
        this.playerInfo = playerInfo;
    }

    public void handlePlayerAction(UUID receiver, NetControlAction action)
    {
        if(action == NetControlAction.SetOwner)
        {
            LOGGER.info("尝试设为所有者");
            // 执行者是所有者，且接收者不为玩家，则可以设置新所有者
            if(net.isOwner(player)&&!player.getUUID().equals(receiver))
            {
                LOGGER.info("成功设为所有者");
                net.setOwner(receiver);
            }
        }
        else if(action == NetControlAction.SetManager)
        {
            LOGGER.info("尝试设为管理员");
            // 执行者是所有者，且接收者不为管理员，则可以被添加为管理员
            if(net.isOwner(player)&&!net.isManager(receiver))
            {
                LOGGER.info("成功设为管理员");
                net.addManager(receiver);
            }
        }
        else if(action == NetControlAction.RemoveManager)
        {
            LOGGER.info("尝试移除管理员");
            // 执行者是所有者，且接收者为管理员，且接收者并非所有者，则可以被移除管理员权限
            if(net.isOwner(player)&&net.isManager(receiver)&&!net.isOwner(receiver))
            {
                LOGGER.info("成功移除管理员");
                net.removeManager(receiver);
            }
        }
        else if(action == NetControlAction.RemovePlayer)
        {
            LOGGER.info("尝试移除成员");
            // 执行者是管理员，被执行者不是管理员，可以移除成员
            if(net.isManager(player)&&!net.isManager(receiver))
            {
                LOGGER.info("成功移除成员");
                net.removePlayer(receiver);
            }
            else if(player.getUUID().equals(receiver)&&!net.isOwner(receiver)) // 否则，移除者是自己，并且自己不是所有者，可以移除
            {
                LOGGER.info("成功移除成员");
                net.removePlayer(receiver);
            }
        }
    }

    @Override
    public void broadcastChanges()
    {
        super.broadcastChanges();

        if(!net.getPlayerPermissionInfoMap(player.level()).equals(this.playerInfo))
        {
            sendPlayerInfo();
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int i)
    {
        return null;
    }

    @Override
    public boolean stillValid(Player player)
    {
        return true;
    }
}
