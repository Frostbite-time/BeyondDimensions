package com.wintercogs.beyonddimensions.api.event.dimensionnet;

import com.wintercogs.beyonddimensions.common.item.NetedItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.Event;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * {@link NetedItem} 及其子类与维度网络之间的绑定关系变更事件。
 * <p>
 * 调用者需要注意：物品没有可靠且统一的生命周期结束信号，本事件只覆盖通过
 * {@link NetedItem#setNet(ItemStack, net.minecraft.world.entity.player.Player)} 进行的主动绑定和主动解绑。
 * 物品被销毁、清除、合并或以其他方式消失时不会派发解绑事件。
 */
public abstract class NetedItemEvent extends Event
{
    private final int netId;
    private final @NotNull ServerPlayer player;
    private final @NotNull ItemStack stack;
    private final @NotNull NetedItem item;

    protected NetedItemEvent(int netId, @NotNull ServerPlayer player,
                             @NotNull ItemStack stack, @NotNull NetedItem item)
    {
        this.netId = netId;
        this.player = Objects.requireNonNull(player);
        this.stack = Objects.requireNonNull(stack);
        this.item = Objects.requireNonNull(item);
    }

    public int getNetId()
    {
        return netId;
    }

    /** 获取发起此次主动绑定或解绑的服务端玩家。 */
    public @NotNull ServerPlayer getPlayer()
    {
        return player;
    }

    /**
     * 获取发生变更的实时物品堆实例。
     * <p>
     * 该堆叠已经处于事件所描述的绑定状态；监听器不应修改它。
     */
    public @NotNull ItemStack getStack()
    {
        return stack;
    }

    public @NotNull NetedItem getItem()
    {
        return item;
    }

    /** 物品完成主动绑定后派发。 */
    public static final class Bound extends NetedItemEvent
    {
        public Bound(int netId, @NotNull ServerPlayer player,
                     @NotNull ItemStack stack, @NotNull NetedItem item)
        {
            super(netId, player, stack, item);
        }
    }

    /** 物品完成主动解绑后派发。 */
    public static final class Unbound extends NetedItemEvent
    {
        public Unbound(int netId, @NotNull ServerPlayer player,
                       @NotNull ItemStack stack, @NotNull NetedItem item)
        {
            super(netId, player, stack, item);
        }
    }
}
