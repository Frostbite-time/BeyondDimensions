package com.wintercogs.beyonddimensions.common.menu.widget;

import com.wintercogs.beyonddimensions.api.ButtonState;
import com.wintercogs.beyonddimensions.api.storage.handler.impl.AbstractUnorderedStackHandler;
import com.wintercogs.beyonddimensions.api.storage.key.IStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.KeyAmount;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * 维度网络UI特化的IStackKey客户端存储。为其指定一个源存储、其负责从源存储处同步数据、应用搜索、排序功能
 * <p>
 * 其可以被理解为一个客户端专用的存储视图，其与真存储区分开的原因是需要在一定时间内提供给客户端一个稳定不变的视图
 * 避免因真存储在服务端与客户端之间的变动导致存储视图频繁闪烁
 */
public class ClientNetStorage extends AbstractUnorderedStackHandler
{
    /**
     * 原有的，对客户端而言绝对真实的存储
     */
    private final AbstractUnorderedStackHandler sourceStorage;

    private final Set<IStackKey<?>> pendingCache = new HashSet<>();

    private boolean mustUpdateAllFromSource = true;

    private final AutoCloseable anySubscriber;
    private final AutoCloseable deltaSubscriber;

    private final ClientNetStorageSearchHelper searchHelper = new ClientNetStorageSearchHelper();

    private @Nullable List<Integer> cacheIndexes = null;

    // 初始值给一个不可能出现的按钮值防止命中
    private SortProperties lastSortProperties = new SortProperties(ButtonState.DISABLED, ButtonState.DISABLED, false);

    public ClientNetStorage(@NotNull AbstractUnorderedStackHandler sourceStorage)
    {
        // 这里初始化为KEEP_ZERO，但是后续调用时，应当在必要时手动设置
        super(ZeroPolicy.KEEP_ZERO, UiTimestampPolicy.NONE);

        this.sourceStorage = sourceStorage;

        this.anySubscriber = this.sourceStorage.subscribeAnyWeak(this, ClientNetStorage::markForceAllUpdate);
        this.deltaSubscriber = this.sourceStorage.subscribeDeltaWeak(this, ClientNetStorage::loadFromDeltaSubscription);
    }

    public void markForceAllUpdate()
    {
        this.mustUpdateAllFromSource = true;
    }

    public void setSearchText(@NotNull String newSearchText)
    {
        Objects.requireNonNull(newSearchText);
        this.searchHelper.loadTexts(newSearchText);
    }

    private void loadFromDeltaSubscription(IStackKey<?> key, long delta, boolean insert)
    {
        if (mustUpdateAllFromSource)
        {
            pendingCache.clear();
            return;
        }

        if (delta != 0)
        {
            pendingCache.add(key);
        }
    }

    public void resolvePendingOrAllUpdate(boolean onlyAmountUpdate)
    {
        boolean anyChanged = false;
        if (mustUpdateAllFromSource)
        {
            pendingCache.clear();
            updateViewFromStorage(onlyAmountUpdate);
            this.mustUpdateAllFromSource = false;
            anyChanged = true;
        }
        else
        {
            Iterator<IStackKey<?>> it = pendingCache.iterator();
            while (it.hasNext())
            {
                IStackKey<?> key = it.next();
                if (key.isEmpty()) continue;

                long newAmount = sourceStorage.getStackByKey(key).amount();
                if (this.hasStack(key))
                {
                    // 视图内已经有这个键，则说明其符合过滤器，直接设置数量
                    anyChanged = true;
                    this.setAmountByKey(key, newAmount);
                }
                else if (!onlyAmountUpdate && matchFilter(key))
                {
                    // 否则，我们要求符合过滤器，且不处于仅数量更新的情况下，才允许向视图内增加新键
                    anyChanged = true;
                    this.setAmountByKey(key, newAmount);
                }

                it.remove();
            }
        }

        if (anyChanged)
        {
            this.cacheIndexes = null;
        }
    }

    /**
     * 从真实存储处更新视图状态
     */
    private void updateViewFromStorage(boolean onlyAmountUpdate)
    {
        // 只更新视图内已有Key的数量，不同步新增key
        if (onlyAmountUpdate)
        {
            for (IStackKey<?> key : this.storage.keySet())
            {
                // 如果存储没有对应key，内部返回0，这里符合我们的意图
                long amount = sourceStorage.getStackByKey(key).amount();
                this.setAmountByKey(key, amount);
            }
        }
        // 完全更新状态
        else
        {
            this.clearStorage();
            for (KeyAmount ka : this.sourceStorage.getStorage())
            {
                if (ka == null || !matchFilter(ka.key())) continue;

                this.setAmountByKey(ka.key(), ka.amount());
            }
        }
    }

    /**
     * 根据当前存储的状态，以及对应的排序策略，返回一个下标数组，其下标数组将能够对应到Storage
     */
    public List<Integer> buildSortedIndex(ButtonState primarySortPolicy, ButtonState secondarySortPolicy, boolean reverse)
    {
        if (cacheIndexes != null
                && primarySortPolicy == lastSortProperties.primarySortPolicy()
                && secondarySortPolicy == lastSortProperties.secondarySortPolicy()
                && reverse == lastSortProperties.reverse())
            return cacheIndexes;

        if (primarySortPolicy == null) primarySortPolicy = ButtonState.SORT_NAME;
        final boolean useSecondary = (secondarySortPolicy != null && secondarySortPolicy != primarySortPolicy);

        final boolean needNameSort = (primarySortPolicy == ButtonState.SORT_NAME) || (useSecondary && secondarySortPolicy == ButtonState.SORT_NAME);
        final boolean needModIdSort = (primarySortPolicy == ButtonState.SORT_MODID) || (useSecondary && secondarySortPolicy == ButtonState.SORT_MODID);
        final boolean needQuantitySort = (primarySortPolicy == ButtonState.SORT_QUANTITY) || (useSecondary && secondarySortPolicy == ButtonState.SORT_QUANTITY);
        final boolean needCreationTimeSort = (primarySortPolicy == ButtonState.SORT_INSERTED_TIME) || (useSecondary && secondarySortPolicy == ButtonState.SORT_INSERTED_TIME);
        final boolean needModificationTimeSort = (primarySortPolicy == ButtonState.SORT_MODIFIED_TIME) || (useSecondary && secondarySortPolicy == ButtonState.SORT_MODIFIED_TIME);

        final @Nullable Map<IStackKey<?>, Long> creationTimeMap = needCreationTimeSort ? sourceStorage.getCreationTimeMap() : null;
        final @Nullable Map<IStackKey<?>, Long> modificationTimeMap = needModificationTimeSort ? sourceStorage.getLastModifiedTimeMap() : null;

        final ArrayList<Row> rows = new ArrayList<>(this.getStorage().size());

        for (int i = 0; i < this.getStorage().size(); i++)
        {
            KeyAmount ka = this.getStorage().get(i);
            if (ka == null || ka.isEmpty()) continue;

            IStackKey<?> key = ka.key();

            String displayName = null;
            String modIdSort = null;

            if (needNameSort)
            {
                displayName = key.getRender().getDisplayName(key).getString();
            }
            if (needModIdSort)
            {
                modIdSort = key.getModId();
            }

            long amt = needQuantitySort ? ka.amount() : 0L;
            long ctime = (needCreationTimeSort && creationTimeMap != null) ? creationTimeMap.getOrDefault(key, 0L) : 0L;
            long mtime = (needModificationTimeSort && modificationTimeMap != null) ? modificationTimeMap.getOrDefault(key, 0L) : 0L;

            rows.add(new Row(i, displayName, modIdSort, amt, ctime, mtime));
        }

        if (!rows.isEmpty())
        {
            final Comparator<Row> primary = buildRowComparator(primarySortPolicy);
            if (useSecondary)
            {
                final Comparator<Row> secondary = buildRowComparator(secondarySortPolicy);
                rows.sort(primary.thenComparing(secondary));
            }
            else
            {
                rows.sort(primary);
            }
            if (reverse)
            {
                Collections.reverse(rows);
            }
        }

        ArrayList<Integer> result = new ArrayList<>(rows.size());
        for (Row row : rows)
        {
            result.add(row.idx);
        }
        this.cacheIndexes = result;
        this.lastSortProperties = new SortProperties(primarySortPolicy, secondarySortPolicy, reverse);
        return result;
    }


    /**
     * 搜索过滤逻辑
     */
    private boolean matchFilter(IStackKey<?> key)
    {
        return this.searchHelper.matches(key);
    }

    /**
     * 比较 Row 中已准备好的字段
     */
    private Comparator<Row> buildRowComparator(@NotNull ButtonState state)
    {
        return switch (state)
        {
            case SORT_QUANTITY -> Comparator.comparingLong((Row r) -> r.amount);
            case SORT_NAME -> Comparator.comparing((Row r) -> r.name, String::compareTo);
            case SORT_MODID -> Comparator.comparing((Row r) -> r.modIdSort, String::compareTo);
            case SORT_INSERTED_TIME -> Comparator.comparingLong((Row r) -> r.ctime);
            case SORT_MODIFIED_TIME -> Comparator.comparingLong((Row r) -> r.mtime);
            default -> Comparator.comparing((Row r) -> r.name, String::compareTo);
        };
    }

    /**
     * @param idx       指向视觉存储的下标
     * @param name      显示名（仅在需要时非 null）
     * @param modIdSort 模组ID（排序用原字符串；仅在需要时非 null）
     * @param amount    数量（仅在需要时有意义）
     * @param ctime     插入时间（仅在需要时有意义）
     * @param mtime     修改时间（仅在需要时有意义）
     */
    private record Row(int idx, @Nullable String name, @Nullable String modIdSort, long amount, long ctime, long mtime)
    {
    }

    private record SortProperties(ButtonState primarySortPolicy, ButtonState secondarySortPolicy, boolean reverse)
    {
    }
}
