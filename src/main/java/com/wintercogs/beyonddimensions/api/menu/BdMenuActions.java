package com.wintercogs.beyonddimensions.api.menu;

import dev.compixel.forge.sync.MenuAction;
import dev.compixel.forge.sync.MenuSync;
import dev.compixel.sync.session.SyncAction;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayList;
import java.util.List;

/**
 * 附属模组给 BD 菜单增加的服务器动作。客户端用注入页上下文的 {@code request} 发出，经 BD 菜单已有的同步通道送到服务器，
 * 处理器在服务器线程带着菜单与玩家运行。同步通道限制每个动作的大小与频率；处理器仍要自己校验收到的值。
 * <p>
 * 两端必须注册同样的动作：在两端都会运行的模组初始化里注册（例如模组的构造函数），之后打开的菜单才带有它们。
 * 动作按菜单类匹配，注册给父类的动作也加到它的子类上，例如注册给 {@code DimensionsNetMenu} 的动作也出现在合成菜单上。
 * id 与已注册的动作重复时立即抛出 {@link IllegalStateException}
 */
public final class BdMenuActions
{
    private record Entry(Class<?> menuClass, MenuAction<?, ?> action)
    {
    }

    private static volatile List<Entry> entries = List.of();

    private BdMenuActions()
    {
    }

    public static synchronized <M extends AbstractContainerMenu> void register(Class<M> menuClass, MenuAction<? super M, ?> action)
    {
        for (Entry entry : entries)
        {
            if (entry.action().id().equals(action.id()))
                throw new IllegalStateException("Menu action " + action.id() + " is already registered for " + entry.menuClass().getName());
        }
        List<Entry> next = new ArrayList<>(entries);
        next.add(new Entry(menuClass, action));
        entries = List.copyOf(next);
    }

    /**
     * BD 的菜单在建立同步通道时调用，把匹配的动作加进去
     */
    @ApiStatus.Internal
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <M extends AbstractContainerMenu> MenuSync<M> attach(M menu, MenuSync<M> sync)
    {
        for (Entry entry : entries)
        {
            if (entry.menuClass().isInstance(menu))
                sync.action((SyncAction) entry.action());
        }
        return sync;
    }
}
