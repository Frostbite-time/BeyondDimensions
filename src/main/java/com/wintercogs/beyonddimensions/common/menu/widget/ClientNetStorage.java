package com.wintercogs.beyonddimensions.common.menu.widget;

import com.wintercogs.beyonddimensions.api.ButtonState;
import com.wintercogs.beyonddimensions.api.storage.handler.impl.AbstractUnorderedStackHandler;
import com.wintercogs.beyonddimensions.api.storage.key.IStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.KeyAmount;
import com.wintercogs.beyonddimensions.api.storage.key.impl.ItemStackKey;
import com.wintercogs.beyonddimensions.util.TooltipHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.CreativeModeTabRegistry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * 维度网络UI特化的IStackKey客户端存储。为其指定一个源存储、其负责从源存储处同步数据、应用搜索、排序功能
 * <p>
 * 其可以被理解为一个客户端专用的存储视图，其与真存储区分开的原因是需要在一定时间内提供给客户端一个稳定不变的视图
 * 避免因真存储在服务端与客户端之间的变动导致存储视图频繁闪烁
 */
public class ClientNetStorage extends AbstractUnorderedStackHandler implements AutoCloseable
{
    private static final int CREATIVE_SORT_LAST = Integer.MAX_VALUE;

    /**
     * 原有的，对客户端而言绝对真实的存储
     */
    private final AbstractUnorderedStackHandler sourceStorage;

    private final Set<IStackKey<?>> pendingCache = new HashSet<>();

    private boolean mustUpdateAllFromSource = true;

    private final AutoCloseable anySubscriber;
    private final AutoCloseable deltaSubscriber;

    private final ClientNetStorageSearchHelper searchHelper = new ClientNetStorageSearchHelper();

    private final Map<Item, CreativeRank> creativeRankCache = new IdentityHashMap<>();
    private boolean creativeRankCacheBuilt = false;

    private @Nullable List<Integer> cacheIndexes = null;

    // 提示框缓存的世代与高级提示框开关，任一变化都会使名称、提示框等派生缓存失效
    private long cacheEpoch = -1;
    private boolean advancedTooltips;

    // 仅显示某一种资源类型，null 表示全部
    private @Nullable ResourceLocation typeFilter = null;

    /**
     * 语言、资源包或高级提示框变化后，丢弃派生缓存并完整重建视图
     *
     * @return 缓存是否被重置
     */
    public boolean refreshContext()
    {
        long epoch = TooltipHelper.epoch();
        boolean advanced = Minecraft.getInstance().options.advancedItemTooltips;
        if (epoch == cacheEpoch && advanced == advancedTooltips)
            return false;
        cacheEpoch = epoch;
        advancedTooltips = advanced;
        searchHelper.clearDerivedCaches();
        creativeRankCache.clear();
        creativeRankCacheBuilt = false;
        mustUpdateAllFromSource = true;
        cacheIndexes = null;
        return true;
    }

    /**
     * 源存储中的这些资源发生了变化
     */
    public void sourceChanged(List<IStackKey<?>> keys)
    {
        // 按时间排序时，任何变化都可能改变顺序
        if (isTimeSort(lastSortProperties.primarySortPolicy()) || isTimeSort(lastSortProperties.secondarySortPolicy()))
            cacheIndexes = null;
        for (IStackKey<?> key : keys)
        {
            if (!sourceStorage.hasStack(key))
                searchHelper.forget(key);
            if (!mustUpdateAllFromSource)
                pendingCache.add(key);
        }
    }

    private static boolean isTimeSort(ButtonState value)
    {
        return value == ButtonState.SORT_INSERTED_TIME || value == ButtonState.SORT_MODIFIED_TIME;
    }

    /**
     * 设置资源类型过滤，null 表示显示全部类型
     */
    public void setTypeFilter(@Nullable ResourceLocation typeId)
    {
        if (Objects.equals(typeFilter, typeId))
            return;
        typeFilter = typeId;
        mustUpdateAllFromSource = true;
        cacheIndexes = null;
    }

    public @Nullable ResourceLocation getTypeFilter()
    {
        return typeFilter;
    }

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
        refreshContext();
        boolean amountsChanged = false, membershipChanged = false;
        if (mustUpdateAllFromSource)
        {
            pendingCache.clear();
            updateViewFromStorage(onlyAmountUpdate);
            this.mustUpdateAllFromSource = false;
            membershipChanged = true;
        }
        else
        {
            Iterator<IStackKey<?>> it = pendingCache.iterator();
            while (it.hasNext())
            {
                IStackKey<?> key = it.next();
                if (key.isEmpty())
                {
                    it.remove();
                    continue;
                }

                long newAmount = sourceStorage.getStackByKey(key).amount();
                long oldAmount = this.getStackByKey(key).amount();
                if (this.hasStack(key))
                {
                    // 视图内已经有这个键，则说明其符合过滤器，直接设置数量
                    amountsChanged |= oldAmount != newAmount;
                    this.setAmountByKey(key, newAmount);
                }
                else if (!onlyAmountUpdate && matchFilter(key))
                {
                    // 否则，我们要求符合过滤器，且不处于仅数量更新的情况下，才允许向视图内增加新键
                    amountsChanged |= oldAmount != newAmount;
                    this.setAmountByKey(key, newAmount);
                }
                membershipChanged |= (oldAmount > 0) != (this.getStackByKey(key).amount() > 0);

                it.remove();
            }
        }

        // 仅数量变化时，只有按数量排序才需要重排
        boolean quantitySort = lastSortProperties.primarySortPolicy() == ButtonState.SORT_QUANTITY
                || lastSortProperties.secondarySortPolicy() == ButtonState.SORT_QUANTITY;
        if (membershipChanged || amountsChanged && quantitySort)
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
        if (refreshContext())
            resolvePendingOrAllUpdate(false);
        primarySortPolicy = isSortPolicy(primarySortPolicy) ? primarySortPolicy : ButtonState.SORT_NAME;
        if (!isSortPolicy(secondarySortPolicy))
            secondarySortPolicy = null;
        if (cacheIndexes != null
                && primarySortPolicy == lastSortProperties.primarySortPolicy()
                && secondarySortPolicy == lastSortProperties.secondarySortPolicy()
                && reverse == lastSortProperties.reverse())
            return cacheIndexes;

        final boolean useSecondary = (secondarySortPolicy != null && secondarySortPolicy != primarySortPolicy);

        final boolean needNameSort = (primarySortPolicy == ButtonState.SORT_NAME) || (useSecondary && secondarySortPolicy == ButtonState.SORT_NAME);
        final boolean needModIdSort = (primarySortPolicy == ButtonState.SORT_MODID) || (useSecondary && secondarySortPolicy == ButtonState.SORT_MODID);
        final boolean needQuantitySort = (primarySortPolicy == ButtonState.SORT_QUANTITY) || (useSecondary && secondarySortPolicy == ButtonState.SORT_QUANTITY);
        final boolean needMaxStackSort = (primarySortPolicy == ButtonState.SORT_MAX_STACK) || (useSecondary && secondarySortPolicy == ButtonState.SORT_MAX_STACK);
        final boolean needCreationTimeSort = (primarySortPolicy == ButtonState.SORT_INSERTED_TIME) || (useSecondary && secondarySortPolicy == ButtonState.SORT_INSERTED_TIME);
        final boolean needModificationTimeSort = (primarySortPolicy == ButtonState.SORT_MODIFIED_TIME) || (useSecondary && secondarySortPolicy == ButtonState.SORT_MODIFIED_TIME);
        final boolean needCreativeTabSort = (primarySortPolicy == ButtonState.SORT_CREATIVE_TAB) || (useSecondary && secondarySortPolicy == ButtonState.SORT_CREATIVE_TAB);

        if (needCreativeTabSort)
        {
            ensureCreativeRankCache();
        }

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
            long maxStack = needMaxStackSort ? key.getVanillaMaxStackSize() : 0L;
            long ctime = (needCreationTimeSort && creationTimeMap != null) ? creationTimeMap.getOrDefault(key, 0L) : 0L;
            long mtime = (needModificationTimeSort && modificationTimeMap != null) ? modificationTimeMap.getOrDefault(key, 0L) : 0L;
            int creativeTabOrder = CREATIVE_SORT_LAST;
            int creativeItemOrder = CREATIVE_SORT_LAST;
            if (needCreativeTabSort)
            {
                CreativeRank rank = getCreativeRank(key);
                if (rank != null)
                {
                    creativeTabOrder = rank.tabOrder;
                    creativeItemOrder = rank.itemOrder;
                }
            }

            rows.add(new Row(i, displayName, modIdSort, amt, maxStack, ctime, mtime, creativeTabOrder, creativeItemOrder));
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
        this.cacheIndexes = Collections.unmodifiableList(result);
        this.lastSortProperties = new SortProperties(primarySortPolicy, secondarySortPolicy, reverse);
        return this.cacheIndexes;
    }

    // 只有具体的排序方式才参与排序，其他按钮状态（如关闭、默认）不算
    private static boolean isSortPolicy(@Nullable ButtonState value)
    {
        return value != null && value.name().startsWith("SORT_") && value != ButtonState.SORT_DEFAULT;
    }


    /**
     * 类型过滤与搜索过滤
     */
    private boolean matchFilter(IStackKey<?> key)
    {
        if (typeFilter != null && !typeFilter.equals(key.getTypeId()))
            return false;
        return this.searchHelper.matches(key);
    }

    /**
     * 比较 Row 中已准备好的字段
     */
    private Comparator<Row> buildRowComparator(@NotNull ButtonState state)
    {
        return switch (state)
        {
            case SORT_CREATIVE_TAB -> Comparator
                    .comparingInt((Row r) -> r.creativeTabOrder)
                    .thenComparingInt(r -> r.creativeItemOrder);
            case SORT_QUANTITY -> Comparator.comparingLong((Row r) -> r.amount);
            case SORT_MAX_STACK -> Comparator.comparingLong((Row r) -> r.maxStack);
            case SORT_NAME -> Comparator.comparing((Row r) -> r.name, String::compareTo);
            case SORT_MODID -> Comparator.comparing((Row r) -> r.modIdSort, String::compareTo);
            case SORT_INSERTED_TIME -> Comparator.comparingLong((Row r) -> r.ctime);
            case SORT_MODIFIED_TIME -> Comparator.comparingLong((Row r) -> r.mtime);
            default -> Comparator.comparing((Row r) -> r.name, String::compareTo);
        };
    }

    /**
     * @param idx               指向视觉存储的下标
     * @param name              显示名（仅在需要时非 null）
     * @param modIdSort         模组ID（排序用原字符串；仅在需要时非 null）
     * @param amount            数量（仅在需要时有意义）
     * @param maxStack          最大堆叠数（仅在需要时有意义）
     * @param ctime             插入时间（仅在需要时有意义）
     * @param mtime             修改时间（仅在需要时有意义）
     * @param creativeTabOrder  创造模式标签页顺序（仅在需要时有意义）
     * @param creativeItemOrder 创造模式页内顺序（仅在需要时有意义）
     */
    private record Row(int idx, @Nullable String name, @Nullable String modIdSort, long amount, long maxStack,
                       long ctime, long mtime,
                       int creativeTabOrder, int creativeItemOrder)
    {
    }

    private void ensureCreativeRankCache()
    {
        if (creativeRankCacheBuilt) return;

        creativeRankCache.clear();
        int tabOrder = 0;
        for (CreativeModeTab tab : getCreativeTabsInOrder())
        {
            if (!tab.getType().equals(CreativeModeTab.Type.CATEGORY))
            {
                continue;
            }

            int itemOrder = 0;
            for (ItemStack stack : tab.getDisplayItems())
            {
                if (stack == null || stack.isEmpty())
                {
                    continue;
                }

                Item item = stack.getItem();
                creativeRankCache.putIfAbsent(item, new CreativeRank(tabOrder, itemOrder));
                itemOrder++;
            }

            tabOrder++;
        }

        creativeRankCacheBuilt = true;
    }

    private @Nullable CreativeRank getCreativeRank(@NotNull IStackKey<?> key)
    {
        if (!(key instanceof ItemStackKey itemStackKey))
        {
            return null;
        }

        return creativeRankCache.get(itemStackKey.getSource());
    }

    private List<CreativeModeTab> getCreativeTabsInOrder()
    {
        List<CreativeModeTab> orderedTabs = CreativeModeTabRegistry.getSortedCreativeModeTabs();
        if (!orderedTabs.isEmpty())
        {
            return orderedTabs;
        }

        return BuiltInRegistries.CREATIVE_MODE_TAB.stream()
                .sorted(Comparator.comparing(tab -> String.valueOf(BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab))))
                .toList();
    }

    private record CreativeRank(int tabOrder, int itemOrder)
    {
    }

    private record SortProperties(ButtonState primarySortPolicy, ButtonState secondarySortPolicy, boolean reverse)
    {
    }

    /**
     * 取消对源存储的订阅并清空视图
     */
    @Override
    public void close()
    {
        closeQuietly(anySubscriber);
        closeQuietly(deltaSubscriber);
        pendingCache.clear();
        cacheIndexes = null;
        searchHelper.clearDerivedCaches();
        creativeRankCache.clear();
        clearStorage();
    }

    private static void closeQuietly(AutoCloseable subscriber)
    {
        try
        {
            subscriber.close();
        }
        catch (Exception ignored)
        {
            // 订阅只持有弱引用，关闭失败不影响视图本身
        }
    }
}
