package com.wintercogs.beyonddimensions.common.menu;

import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuConstructor;
import org.jetbrains.annotations.NotNull;

/**
 * 从一个 BD 界面直接换到另一个，例如存储与合成之间、存储与主网络切换器之间。
 * 原版打开新菜单前会先通知客户端关闭旧菜单：客户端在两个界面之间短暂没有界面，于是收起指针，
 * 新界面出现时再把指针放回窗口中央。这里让服务器直接结束旧菜单，客户端从旧界面直接换到新界面，指针留在原处。
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

    @Override
    public boolean shouldTriggerClientSideContainerClosingOnOpen()
    {
        return false;
    }
}
