package com.wintercogs.beyonddimensions.common.menu;

import com.wintercogs.beyonddimensions.api.storage.key.IStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.impl.ItemStackKey;
import com.wintercogs.beyonddimensions.common.menu.widget.slot.AbstractStackTypedSlot;
import com.wintercogs.beyonddimensions.common.menu.widget.slot.DisorderedSlotGroupSync;
import dev.composemc.forge.sync.MenuSync;
import dev.composemc.forge.sync.MinecraftSyncCodecs;
import dev.composemc.sync.MenuSyncOptions;
import dev.composemc.sync.action.ActionLimits;
import dev.composemc.sync.state.SyncCodec;
import dev.composemc.sync.state.SyncCodecs;
import dev.composemc.sync.state.SyncLimits;
import dev.composemc.sync.state.SyncMap;
import dev.composemc.sync.state.SyncSchema;
import dev.composemc.sync.transport.TransferBudget;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 菜单资源同步：服务端为每种资源分配仅在本菜单内有效的句柄，
 * 再以不可变的键值快照同步资源目录、各槽组的数量以及有序槽位的内容。
 * 普通的数量变化只会访问发生变化的资源。
 */
public final class BDMenuResources implements AutoCloseable
{
    /** 资源目录条目：句柄与其对应的资源 */
    public record Definition(long id, IStackKey<?> key) {}

    /** 某个槽组中的一种资源 */
    public record Ref(int group, long resource) {}

    /** 槽组资源的数量与排序用时间戳 */
    public record Amount(Ref ref, long count, long modified, long inserted) {}

    /** 有序槽位的内容，resource 为 0 表示空槽 */
    public record Ordered(int slot, int index, long resource, long count) {}

    public static final int MAX_NATIVE_BYTES = 1536 * 1024;
    public static final int MAX_ACTION_BYTES = 8 * 1024 * 1024;
    private static final int MAX_ENTRIES = 100_000;

    // 资源操作可能携带多个较大的资源键，这些预算属于本模组自己的资源菜单
    private static final MenuSyncOptions OPTIONS = new MenuSyncOptions(
            new SyncLimits(128 * 1024,
                    new TransferBudget(32 * 1024, 4L * 1024 * 1024, 256 * 1024),
                    16, 2 * 1024 * 1024, 32L * 1024 * 1024, 250_000, 200),
            new ActionLimits(16 * 1024,
                    new TransferBudget(64 * 1024, 4L * 1024 * 1024, 256 * 1024),
                    16L * 1024 * 1024, 64, 32, 600, 200, 200),
            100);

    private static final SyncCodec<Ref> REF = SyncCodec.of("beyonddimensions:resource_ref/1",
            (out, value) -> {
                out.writeInt(value.group());
                out.writeLong(value.resource());
            },
            in -> new Ref(in.readInt(), in.readLong()));

    private static final SyncCodec<Amount> AMOUNT = SyncCodec.of("beyonddimensions:resource_amount/1",
            (out, value) -> {
                REF.write(out, value.ref());
                out.writeLong(value.count());
                out.writeLong(value.modified());
                out.writeLong(value.inserted());
            },
            in -> new Amount(REF.read(in), in.readLong(), in.readLong(), in.readLong()));

    private static final SyncCodec<Ordered> ORDERED = SyncCodec.of("beyonddimensions:ordered_resource/1",
            (out, value) -> {
                out.writeInt(value.slot());
                out.writeInt(value.index());
                out.writeLong(value.resource());
                out.writeLong(value.count());
            },
            in -> new Ordered(in.readInt(), in.readInt(), in.readLong(), in.readLong()));

    /** 服务端句柄：记录被多少个数量条目或有序槽位引用 */
    private static final class Handle
    {
        final long id;
        final IStackKey<?> key;
        int references;

        Handle(long id, IStackKey<?> key)
        {
            this.id = id;
            this.key = key;
        }
    }

    private final BDBaseMenu menu;
    private final SyncSchema<BDBaseMenu> schema;
    private final List<AbstractStackTypedSlot> orderedSlots = new ArrayList<>();
    private boolean closed;

    // 服务端状态
    private final Map<IStackKey<?>, Handle> handles = new HashMap<>();
    private final Map<Long, Handle> handlesById = new HashMap<>();
    private final Set<Long> retiredHandles = new HashSet<>();
    private final Map<Integer, Set<IStackKey<?>>> trackedKeys = new HashMap<>();
    private final Set<Integer> fullGroups = new HashSet<>();
    private final Map<Integer, Set<IStackKey<?>>> dirtyKeys = new HashMap<>();
    private long nextId = 1;
    private boolean initial = true;
    private boolean orderedDirty = true;

    // 当前快照；客户端还保留已应用的上一份快照用于求差
    private SyncMap<Long, Definition> catalog = SyncMap.empty();
    private SyncMap<Ref, Amount> amounts = SyncMap.empty();
    private SyncMap<Integer, Ordered> ordered = SyncMap.empty();
    private SyncMap<Long, Definition> appliedCatalog = SyncMap.empty();
    private SyncMap<Ref, Amount> appliedAmounts = SyncMap.empty();
    private SyncMap<Integer, Ordered> appliedOrdered = SyncMap.empty();

    // 客户端状态
    private final Map<IStackKey<?>, Long> clientHandles = new HashMap<>();
    private final Map<Long, Integer> clientReferences = new HashMap<>();

    public BDMenuResources(BDBaseMenu menu)
    {
        this.menu = menu;
        var nativeKey = MinecraftSyncCodecs.registry("beyonddimensions:stack_key/1", MAX_NATIVE_BYTES, IStackKey.STREAM_CODEC);
        var definition = SyncCodec.<Definition>of("beyonddimensions:resource_definition/1:" + nativeKey.id(),
                (out, value) -> {
                    out.writeLong(value.id());
                    nativeKey.write(out, value.key());
                },
                in -> new Definition(in.readLong(), nativeKey.read(in)));
        this.schema = SyncSchema.<BDBaseMenu>builder("beyonddimensions:resources", 2)
                .keyedMap("catalog", SyncCodecs.LONG, definition, Definition::id,
                        m -> m.resources().catalog(), (m, value) -> m.resources().catalog = value)
                .keyedMap("amounts", REF, AMOUNT, Amount::ref,
                        m -> m.resources().amounts(), (m, value) -> m.resources().amounts = value)
                .keyedMap("ordered", SyncCodecs.INT, ORDERED, Ordered::slot,
                        m -> m.resources().ordered(), (m, value) -> m.resources().ordered = value)
                .build();
    }

    /** 以页面自己的状态 schema 加上资源同步绑定菜单 */
    public static <M extends BDBaseMenu> MenuSync<M> bind(M menu, SyncSchema<M> view)
    {
        var combined = SyncSchema.<M>builder(view.id(), 1)
                .include("view", view)
                .include("resources", menu.resources().schema)
                .build();
        return MenuSync.bind(menu, combined, OPTIONS).onUpdate(m -> m.resources().applyClient());
    }

    /** 仅同步资源的菜单 */
    public static <M extends BDBaseMenu> MenuSync<M> bind(M menu)
    {
        var schema = SyncSchema.<M>builder("beyonddimensions:resources", 2)
                .include("resources", menu.resources().schema)
                .build();
        return MenuSync.bind(menu, schema, OPTIONS).onUpdate(m -> m.resources().applyClient());
    }

    public void slotAdded(AbstractStackTypedSlot slot)
    {
        orderedSlots.add(slot);
        orderedDirty = true;
    }

    /** 服务端每次广播时检查有序槽位是否与已同步内容不同 */
    public void pollOrdered()
    {
        if (closed || orderedDirty)
            return;
        for (AbstractStackTypedSlot slot : orderedSlots)
        {
            var value = slot.getStack();
            Ordered previous = ordered.get(slot.index);
            long count = value.isEmpty() ? 0 : value.amount();
            if (previous == null
                    || previous.count() != count
                    || (previous.resource() == 0) != value.isEmpty()
                    || previous.resource() != 0 && !handlesById.get(previous.resource()).key.equals(value.key()))
            {
                orderedDirty = true;
                return;
            }
        }
    }

    public void invalidate()
    {
        if (!closed)
            orderedDirty = true;
    }

    /** 整组重新扫描，用于无法得知具体变化的情况 */
    public void invalidateGroup(int group)
    {
        if (closed)
            return;
        fullGroups.add(group);
        dirtyKeys.remove(group);
    }

    public void invalidateResource(int group, IStackKey<?> key)
    {
        if (closed || initial || fullGroups.contains(group))
            return;
        Set<IStackKey<?>> keys = dirtyKeys.computeIfAbsent(group, ignored -> new HashSet<>());
        keys.add(key);
        if (keys.size() > MAX_ENTRIES)
            invalidateGroup(group);
    }

    public SyncMap<Long, Definition> catalog()
    {
        capture();
        return catalog;
    }

    public SyncMap<Ref, Amount> amounts()
    {
        capture();
        return amounts;
    }

    public SyncMap<Integer, Ordered> ordered()
    {
        capture();
        return ordered;
    }

    /** 句柄对应的资源；0 为空资源，未知句柄返回 null */
    public IStackKey<?> resolve(long id)
    {
        if (id == 0)
            return ItemStackKey.EMPTY;
        if (menu.player.level().isClientSide())
        {
            Definition definition = catalog.get(id);
            return definition == null ? null : definition.key();
        }
        Handle handle = handlesById.get(id);
        return handle == null ? null : handle.key;
    }

    /** 资源当前的句柄，没有句柄时返回 0 */
    public long idOf(IStackKey<?> key)
    {
        if (key.isEmpty())
            return 0;
        if (menu.player.level().isClientSide())
            return clientHandles.getOrDefault(key, 0L);
        Handle handle = handles.get(key);
        return handle == null ? 0 : handle.id;
    }

    // ---- 服务端采集 ----

    private long retain(IStackKey<?> key)
    {
        Handle handle = handles.get(key);
        if (handle == null)
        {
            if (nextId == Long.MAX_VALUE)
                throw new IllegalStateException("Resource handle space exhausted");
            handle = new Handle(nextId++, key);
            handles.put(key, handle);
            handlesById.put(handle.id, handle);
            catalog = catalog.with(handle.id, new Definition(handle.id, key));
        }
        handle.references++;
        return handle.id;
    }

    private void release(long id)
    {
        if (id == 0)
            return;
        Handle handle = Objects.requireNonNull(handlesById.get(id));
        if (--handle.references < 0)
            throw new IllegalStateException("Resource reference underflow");
        if (handle.references == 0)
            retiredHandles.add(id);
    }

    private void capture()
    {
        if (closed || menu.player.level().isClientSide())
            return;
        if (initial)
        {
            for (DisorderedSlotGroupSync group : menu.slotGroupSyncs)
                fullGroups.add(group.getGroupId());
            initial = false;
        }
        if (!orderedDirty && fullGroups.isEmpty() && dirtyKeys.isEmpty())
            return;

        long sourceRows = 0;
        for (DisorderedSlotGroupSync group : menu.slotGroupSyncs)
            sourceRows += group.getStorage().getSlots();
        if (sourceRows > MAX_ENTRIES)
            throw new IllegalStateException("Menu resource entry limit exceeded");

        if (orderedDirty)
        {
            captureOrdered();
            orderedDirty = false;
        }
        for (int id : fullGroups)
        {
            DisorderedSlotGroupSync group = menu.slotGroupSyncs.get(id);
            Set<IStackKey<?>> removed = new HashSet<>(trackedKeys.getOrDefault(id, Set.of()));
            for (var value : group.getStorage().getStorage())
            {
                if (value.key().isEmpty())
                    continue;
                removed.remove(value.key());
                captureAmount(group, value.key());
            }
            for (IStackKey<?> key : removed)
                captureAmount(group, key);
        }
        fullGroups.clear();
        for (var entry : dirtyKeys.entrySet())
        {
            DisorderedSlotGroupSync group = menu.slotGroupSyncs.get(entry.getKey());
            for (IStackKey<?> key : entry.getValue())
                captureAmount(group, key);
        }
        dirtyKeys.clear();
        if (amounts.size() > MAX_ENTRIES)
            throw new IllegalStateException("Menu resource entry limit exceeded");

        // 同一次采集中资源可能在槽位或槽组之间移动，因此等所有新引用都记录后再回收句柄
        for (long id : retiredHandles)
        {
            Handle handle = handlesById.get(id);
            if (handle.references == 0)
            {
                handles.remove(handle.key);
                handlesById.remove(id);
                catalog = catalog.without(id);
            }
        }
        retiredHandles.clear();
    }

    private void captureOrdered()
    {
        Set<Integer> seen = new HashSet<>();
        for (AbstractStackTypedSlot slot : orderedSlots)
        {
            seen.add(slot.index);
            var value = slot.getStack();
            Ordered old = ordered.get(slot.index);
            long id;
            if (value.isEmpty())
                id = 0;
            else if (old != null && old.resource() != 0 && handlesById.get(old.resource()).key.equals(value.key()))
                id = old.resource();
            else
                id = retain(value.key());
            if (old != null && old.resource() != id)
                release(old.resource());
            long count = value.isEmpty() ? 0 : value.amount();
            ordered = ordered.with(slot.index, new Ordered(slot.index, slot.getSlotIndex(), id, count));
        }
        for (Ordered old : ordered.values())
        {
            if (!seen.contains(old.slot()))
            {
                release(old.resource());
                ordered = ordered.without(old.slot());
            }
        }
    }

    private void captureAmount(DisorderedSlotGroupSync group, IStackKey<?> key)
    {
        var storage = group.getStorage();
        int groupId = group.getGroupId();
        Handle known = handles.get(key);
        Amount old = known == null ? null : amounts.get(new Ref(groupId, known.id));
        Set<IStackKey<?>> tracked = trackedKeys.computeIfAbsent(groupId, ignored -> new HashSet<>());
        if (storage.hasStack(key))
        {
            long id = old == null ? retain(key) : old.ref().resource();
            tracked.add(key);
            Ref ref = new Ref(groupId, id);
            long modified = storage.getLastModifiedTimeMap().getOrDefault(key, 0L);
            long inserted = storage.getCreationTimeMap().getOrDefault(key, 0L);
            amounts = amounts.with(ref, new Amount(ref, storage.getStackByKey(key).amount(), modified, inserted));
        }
        else if (old != null)
        {
            amounts = amounts.without(old.ref());
            tracked.remove(key);
            release(old.ref().resource());
        }
    }

    // ---- 客户端应用 ----

    private static void countReference(Map<Long, Integer> changes, long previous, long current)
    {
        if (previous == current)
            return;
        if (previous != 0)
            changes.merge(previous, -1, Integer::sum);
        if (current != 0)
            changes.merge(current, 1, Integer::sum);
    }

    private void applyClient()
    {
        if (closed || !menu.player.level().isClientSide())
            return;

        // 先校验全部引用与目标，再修改任何派生的客户端存储
        Map<Long, Integer> references = new HashMap<>();
        ordered.forEachChange(appliedOrdered, (slot, previous, current) -> {
            if (current != null)
                validateOrdered(current);
            countReference(references, previous == null ? 0 : previous.resource(), current == null ? 0 : current.resource());
        });
        amounts.forEachChange(appliedAmounts, (ref, previous, current) -> {
            if (current != null && (current.count() < 0
                    || !catalog.containsKey(current.ref().resource())
                    || current.ref().group() < 0
                    || current.ref().group() >= menu.slotGroupSyncs.size()))
                throw new IllegalArgumentException("Invalid resource group");
            countReference(references, previous == null ? 0 : previous.ref().resource(), current == null ? 0 : current.ref().resource());
        });
        catalog.forEachChange(appliedCatalog, (id, previous, current) -> {
            if (current != null && (id <= 0 || current.key().isEmpty()
                    || previous != null && !previous.key().equals(current.key())))
                throw new IllegalArgumentException("Invalid or reused resource handle");
            if (current == null && clientReferences.getOrDefault(id, 0) + references.getOrDefault(id, 0) != 0)
                throw new IllegalArgumentException("Removed referenced resource");
        });

        ordered.forEachChange(appliedOrdered, (slot, previous, current) -> {
            Ordered row = current == null ? previous : current;
            var target = (AbstractStackTypedSlot) menu.slots.get(row.slot());
            if (current == null || row.resource() == 0)
                target.getStorage().setStackDirectly(row.index(), ItemStackKey.EMPTY, 0);
            else
                target.getStorage().setStackDirectly(row.index(), catalog.get(row.resource()).key(), row.count());
        });

        // 移除在前、更新在后，同一资源在同一组内只保留最后的结果
        Map<Integer, LinkedHashMap<IStackKey<?>, Amount>> deltas = new HashMap<>();
        amounts.forEachChange(appliedAmounts, (ref, previous, current) -> {
            if (current == null)
                deltas.computeIfAbsent(ref.group(), ignored -> new LinkedHashMap<>())
                        .put(appliedCatalog.get(ref.resource()).key(), new Amount(ref, 0, 0, 0));
        });
        amounts.forEachChange(appliedAmounts, (ref, previous, current) -> {
            if (current != null)
                deltas.computeIfAbsent(ref.group(), ignored -> new LinkedHashMap<>())
                        .put(catalog.get(ref.resource()).key(), current);
        });
        for (var entry : deltas.entrySet())
        {
            List<DisorderedSlotGroupSync.Change> changes = new ArrayList<>(entry.getValue().size());
            for (var change : entry.getValue().entrySet())
            {
                Amount amount = change.getValue();
                changes.add(new DisorderedSlotGroupSync.Change(change.getKey(), amount.count(), amount.modified(), amount.inserted()));
            }
            menu.slotGroupSyncs.get(entry.getKey()).loadChange(changes);
        }

        catalog.forEachChange(appliedCatalog, (id, previous, current) -> {
            if (previous != null && current == null)
                clientHandles.remove(previous.key());
        });
        catalog.forEachChange(appliedCatalog, (id, previous, current) -> {
            if (current != null)
                clientHandles.put(current.key(), id);
        });
        references.forEach((id, delta) -> {
            int count = clientReferences.getOrDefault(id, 0) + delta;
            if (count == 0)
                clientReferences.remove(id);
            else
                clientReferences.put(id, count);
        });
        appliedCatalog = catalog;
        appliedOrdered = ordered;
        appliedAmounts = amounts;
    }

    private void validateOrdered(Ordered value)
    {
        if (value.slot() < 0 || value.slot() >= menu.slots.size()
                || value.count() < 0 || value.resource() < 0
                || value.resource() == 0 && value.count() != 0
                || value.resource() != 0 && !catalog.containsKey(value.resource()))
            throw new IllegalArgumentException("Invalid ordered resource");
        if (!(menu.slots.get(value.slot()) instanceof AbstractStackTypedSlot slot)
                || !slot.isOrdered()
                || slot.getSlotIndex() != value.index())
            throw new IllegalArgumentException("Ordered slot mismatch");
    }

    @Override
    public void close()
    {
        closed = true;
        orderedSlots.clear();
        handles.clear();
        handlesById.clear();
        retiredHandles.clear();
        trackedKeys.clear();
        fullGroups.clear();
        dirtyKeys.clear();
        clientHandles.clear();
        clientReferences.clear();
        catalog = appliedCatalog = SyncMap.empty();
        amounts = appliedAmounts = SyncMap.empty();
        ordered = appliedOrdered = SyncMap.empty();
    }
}
