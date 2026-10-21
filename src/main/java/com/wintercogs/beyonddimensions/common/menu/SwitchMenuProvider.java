package com.wintercogs.beyonddimensions.common.menu;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuConstructor;
import org.jetbrains.annotations.NotNull;

/**
 * 从一个 BD 界面直接换到另一个。原版打开新菜单前会先让客户端关闭旧菜单，指针被收起后再放回窗口中央；
 * 这里让服务器直接结束旧菜单，指针留在原处
 */
public record SwitchMenuProvider(MenuConstructor constructor, Component title) implements MenuProvider
{
    @Override
    public AbstractContainerMenu createMenu(int containerId, @NotNull Inventory inventory, @NotNull Player player)
    {
        return constructor.createMenu(containerId, inventory, player);
    }

    @Override
    public @NotNull Component getDisplayName()
    {
        return title;
    }

    /**
     * 为玩家打开这个菜单。1.20.1 的 openMenu 遇到已打开的菜单总会通知客户端关闭，所以先在服务器上结束它
     */
    public void open(Player player)
    {
        if (player instanceof ServerPlayer serverPlayer && serverPlayer.containerMenu != serverPlayer.inventoryMenu)
            serverPlayer.doCloseContainer();
        player.openMenu(this);
    }
}
