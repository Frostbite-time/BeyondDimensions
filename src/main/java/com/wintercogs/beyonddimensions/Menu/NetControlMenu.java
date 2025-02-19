package com.wintercogs.beyonddimensions.Menu;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.DataBase.DimensionsNet;
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

import java.util.HashMap;
import java.util.UUID;
import java.util.function.Supplier;

public class NetControlMenu extends AbstractContainerMenu
{
    private final Player player;

    private DimensionsNet net = new DimensionsNet();
    public HashMap<UUID, PlayerPermissionInfo> playerInfo;

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
