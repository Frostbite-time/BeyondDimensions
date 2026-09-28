package com.wintercogs.beyonddimensions.common.menu.widget.slot;

import com.wintercogs.beyonddimensions.api.storage.handler.impl.AbstractUnorderedStackHandler;
import com.wintercogs.beyonddimensions.api.storage.key.IStackKey;
import com.wintercogs.beyonddimensions.common.menu.BDBaseMenu;

import java.util.List;

/**
 * 一组共享同一无序存储的槽位。
 * <p>
 * 服务端只负责把存储的变化标记给菜单的资源同步，差量、分批与会话由同步库处理；
 * 客户端通过 {@link #loadChange} 接收同步结果并写入本地存储。
 */
public class DisorderedSlotGroupSync
{
    /** 同步中某个资源的新数量与排序用时间戳，数量为 0 表示移除 */
    public record Change(IStackKey<?> key, long count, long modified, long inserted) {}

    public final int groupId;
    private final AbstractUnorderedStackHandler storage;
    private AutoCloseable anySubscriber;
    private AutoCloseable deltaSubscriber;

    public DisorderedSlotGroupSync(BDBaseMenu menu, int groupId, AbstractUnorderedStackHandler storage)
    {
        this.groupId = groupId;
        this.storage = storage;
        if (!menu.player.level().isClientSide())
        {
            anySubscriber = storage.subscribeAny(menu, () -> menu.resources().invalidateGroup(groupId));
            deltaSubscriber = storage.subscribeDelta(menu, (key, amount, insert) -> menu.resources().invalidateResource(groupId, key));
        }
    }

    public AbstractUnorderedStackHandler getStorage()
    {
        return storage;
    }

    public int getGroupId()
    {
        return groupId;
    }

    /**
     * 菜单关闭时取消对存储的订阅
     */
    public void dispose()
    {
        closeQuietly(anySubscriber);
        closeQuietly(deltaSubscriber);
        anySubscriber = null;
        deltaSubscriber = null;
    }

    private static void closeQuietly(AutoCloseable subscriber)
    {
        if (subscriber == null)
            return;
        try
        {
            subscriber.close();
        }
        catch (Exception ignored)
        {
            // 订阅关闭失败时存储会在菜单回收后自然释放监听
        }
    }

    /**
     * 客户端：应用一批同步变化
     */
    public void loadChange(List<Change> changes)
    {
        for (Change change : changes)
        {
            storage.setAmountByKey(change.key(), change.count());
            if (storage.hasStack(change.key()))
            {
                storage.setLastModifiedTime(change.key(), change.modified());
                storage.setCreationTime(change.key(), change.inserted());
            }
            else
            {
                storage.getLastModifiedTimeMap().remove(change.key());
                storage.getCreationTimeMap().remove(change.key());
            }
        }
        afterLoadChange(changes.stream().map(Change::key).toList());
    }

    /**
     * 客户端：一批变化应用完成后调用，子类可以据此刷新视图
     */
    public void afterLoadChange(List<IStackKey<?>> changedKeys)
    {
    }
}
