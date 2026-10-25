package com.wintercogs.beyonddimensions.api.menu.workbench;

import com.wintercogs.beyonddimensions.api.menu.widget.MenuWidget;
import com.wintercogs.beyonddimensions.common.menu.DimensionsNetMenu;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

/**
 * 存储界面工作区里一个工作台在菜单一侧的部分：玩家能否使用它，以及它在菜单里的部件（槽位与逻辑）。
 * 用 {@link StorageWorkbenchTypes#register} 在通用初始化中注册；它的界面在客户端的
 * {@code com.wintercogs.beyonddimensions.api.ui.storage.StorageWorkbenches} 里以同一个 ID 注册
 */
public interface StorageWorkbenchType
{
    /**
     * 总能使用、在菜单里有一个部件的工作台
     */
    static StorageWorkbenchType of(Function<DimensionsNetMenu, ? extends MenuWidget> widget)
    {
        return new StorageWorkbenchType()
        {
            @Override
            public MenuWidget createWidget(DimensionsNetMenu menu)
            {
                return widget.apply(menu);
            }
        };
    }

    /**
     * 打开存储界面时在服务端判断一次：这个玩家能否使用这个工作台，例如用过某个道具之后才能使用。
     * 不能使用的工作台不出现在工作区的下拉框里，菜单里也没有它的部件
     */
    default boolean isAvailable(ServerPlayer player)
    {
        return true;
    }

    /**
     * 工作台在菜单里的部件，客户端与服务端各构造一次；没有槽位的工作台返回 null
     */
    default @Nullable MenuWidget createWidget(DimensionsNetMenu menu)
    {
        return null;
    }
}
