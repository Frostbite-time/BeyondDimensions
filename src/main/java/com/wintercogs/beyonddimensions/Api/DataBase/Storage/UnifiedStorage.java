package com.wintercogs.beyonddimensions.Api.DataBase.Storage;

import com.wintercogs.beyonddimensions.Api.DataBase.DimensionsNet;
import com.wintercogs.beyonddimensions.Api.DataBase.Handler.IStackTypedHandler;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.Chemicals.GasStackType;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.EnergyStackType;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.IStackType;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.ItemStackType;
import com.wintercogs.beyonddimensions.Api.Registry.StackTypeRegistry;
import com.wintercogs.beyonddimensions.Api.Util.HashBPlusList;
import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.Item.Custom.MatterCompressionBall;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 基于{@link IStackTypedHandler}接口的无序存储实现。
 * <p>
 * 具有以下特点：
 * <ul>
 *     <li>每种精确匹配的堆叠类型仅允许占用一个槽位</li>
 *     <li>插入的堆叠会自动分配槽位</li>
 *     <li>实际用来存储的列表大小不实际反映最大槽位数量</li>
 * </ul>
 * <p>
 * 虽然需要一个DimensionsNet作为参数进行构造，但仅用于onChange方法通知数据保存。你可以复制代码，移除DimensionsNet参数，以获得一个可以自定义的无序存储实现
 */
public class UnifiedStorage implements IStackTypedHandler
{
    /**
     * 对应的维度网络
     */
    private DimensionsNet net;

    /**
     * 存储的数据结构
     */
    private final HashBPlusList<IStackType<?>> storage = new HashBPlusList<>(64, 128);

    /**
     * 存储的错误备份结构
     * 每次保存或者读取时，将其设为storage的深克隆
     * 如果在保存时出现错误，则利用其进行一个最近回退处理
     */
    private HashBPlusList<IStackType<?>> backupStorage = new HashBPlusList<>(64, 128);

    /**
     * 为构建分化包装提供良好的性能，其结构为 [资源种类id：对应资源类型的索引列表]
     */
    private final Map<ResourceLocation, List<Integer>> typeIdIndex = new HashMap<>();

    //onchange回调处理==================================================
    @FunctionalInterface // 带上下文版本
    public interface DeltaListener
    {
        void onDelta(IStackType<?> type, long size, boolean insert);
    }

    @FunctionalInterface // 不带上下文版本
    public interface AnyChangeListener
    {
        void onAnyChange();
    }

    // 弱订阅用
    @FunctionalInterface
    public interface QuadConsumer<A, B, C, D>
    {
        void accept(A a, B b, C c, D d);
    }

    // ====== 弱 owner + 回调条目 ======
    private static final class OwnerRef extends WeakReference<Object>
    {
        OwnerRef(Object owner, ReferenceQueue<Object> q)
        {
            super(owner, q);
        }
    }

    // 无信息条目
    private static final class AnyEntry
    {
        final OwnerRef ownerRef;
        final AnyChangeListener listener; // 强回调，但内部请勿强握 owner

        AnyEntry(OwnerRef ref, AnyChangeListener l)
        {
            this.ownerRef = ref;
            this.listener = l;
        }
    }

    // 增量信息条目
    private static final class DeltaEntry
    {
        final OwnerRef ownerRef;
        final DeltaListener listener; // 强回调，但内部请勿强握 owner

        DeltaEntry(OwnerRef ref, DeltaListener l)
        {
            this.ownerRef = ref;
            this.listener = l;
        }
    }

    private final CopyOnWriteArrayList<AnyEntry> anyListeners = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<DeltaEntry> deltaListeners = new CopyOnWriteArrayList<>();

    /**
     * 抑制 any 的“delta 上下文”嵌套计数；>0 时 fireChange() 将被忽略。
     */
    private int deltaContextDepth = 0;

    /**
     * 进入/退出 delta 上下文的小工具
     */
    private void beginDeltaContext()
    {
        deltaContextDepth++;
    }

    private void endDeltaContext()
    {
        deltaContextDepth = Math.max(0, deltaContextDepth - 1);
    }

    private boolean inDeltaContext()
    {
        return deltaContextDepth > 0;
    }

    private final ReferenceQueue<Object> refQueue = new ReferenceQueue<>();

    /**
     * 统一存储的属性 对于真正的存储实例，在序列化和反序列化的时候通过持久化和再赋值确定。
     * <p>
     * 对于临时数据，默认给予最大值
     */
    public long slotCapacity = Long.MAX_VALUE;
    public int slotMaxSize = Integer.MAX_VALUE;

    public UnifiedStorage(DimensionsNet net)
    {
        this.net = net;
    }

    private UnifiedStorage(long capacity, int maxSize)
    {
        this.slotCapacity = capacity;
        this.slotMaxSize = maxSize;
    }

    // 返回一个无法被插入和提取的空的UnifiedStorage，其可被安全的视为一个全空容器
    // 无需提供net，因为其覆写了需要net的方法
    public static UnifiedStorage getEmpty()
    {
        return new UnifiedStorage(0, 0)
        {
            @Override
            public void onChange()
            {

            }

            @Override
            public long getSlotCapacity(int slot)
            {
                return 0;
            }
        };
    }

    // ====== 订阅 API（强订阅）======
    public AutoCloseable subscribeAny(Object owner, AnyChangeListener onAny)
    {
        if (owner == null || onAny == null) throw new IllegalArgumentException();
        drainRefQueue();
        AnyEntry e = new AnyEntry(new OwnerRef(owner, refQueue), onAny);
        anyListeners.add(e);
        return () -> anyListeners.remove(e);
    }

    public AutoCloseable subscribeDelta(Object owner, DeltaListener onDelta)
    {
        if (owner == null || onDelta == null) throw new IllegalArgumentException();
        drainRefQueue();
        DeltaEntry e = new DeltaEntry(new OwnerRef(owner, refQueue), onDelta);
        deltaListeners.add(e);
        return () -> deltaListeners.remove(e);
    }

    // ====== 订阅 API（弱订阅，回调内部也只握弱引用）======
    public <T> AutoCloseable subscribeAnyWeak(T owner, java.util.function.Consumer<T> onAny)
    {
        if (owner == null || onAny == null) throw new IllegalArgumentException();
        drainRefQueue();
        OwnerRef ref = new OwnerRef(owner, refQueue);
        AnyEntry e = new AnyEntry(ref, () -> {
            @SuppressWarnings("unchecked") T o = (T) ref.get();
            if (o != null) onAny.accept(o);
            else drainRefQueue();
        });
        anyListeners.add(e);
        return () -> anyListeners.remove(e);
    }

    public <T> AutoCloseable subscribeDeltaWeak(
            T owner,
            QuadConsumer<T, IStackType<?>, Long, Boolean> onDelta
    )
    {
        if (owner == null || onDelta == null) throw new IllegalArgumentException();
        drainRefQueue();
        OwnerRef ref = new OwnerRef(owner, refQueue);
        DeltaEntry e = new DeltaEntry(ref, (type, size, insert) -> {
            @SuppressWarnings("unchecked") T o = (T) ref.get();
            if (o != null) onDelta.accept(o, type, size, insert);
            else drainRefQueue();
        });
        deltaListeners.add(e);
        return () -> deltaListeners.remove(e);
    }

    // 触发无上下文回调，如果本次更改正在触发上下回调则无视
    protected void fireChange()
    {
        if (inDeltaContext()) return;
        drainRefQueue();
        for (AnyEntry e : anyListeners)
        {
            try
            {
                e.listener.onAnyChange();
            }
            catch (Throwable ignored)
            {
            }
        }
    }

    // 触发上下文回调
    protected void fireDelta(IStackType<?> type, long size, boolean insert)
    {
        drainRefQueue();
        for (DeltaEntry e : deltaListeners)
        {
            try
            {
                e.listener.onDelta(type, size, insert);
            }
            catch (Throwable ignored)
            {
            }
        }
    }

    // 帮助GC
    private void drainRefQueue()
    {
        OwnerRef ref;
        while ((ref = (OwnerRef) refQueue.poll()) != null)
        {
            OwnerRef dead = ref;
            anyListeners.removeIf(e -> e.ownerRef == dead);
            deltaListeners.removeIf(e -> e.ownerRef == dead);
        }
    }

    @Override
    public void onChange()
    {
        net.setDirty();
        fireChange();
    }

    // 注：仅在只有一个内容变化的情况下触发带上下文信息的变化
    // 其触发时，会阻止无上下文信息的二次触发，要确保此信息所携带的内容，就是本次变化的全部内容
    // insert为真则为插入操作，否则为提取
    // size为变化量
    private void onContentChanged(IStackType<?> type, long size, boolean insert)
    {
        beginDeltaContext();
        try
        {
            onChange(); // 走统一入口，但是当处于Delta上下文时，内部的fireChange被略过
        }
        finally
        {
            endDeltaContext();
        }
        fireDelta(type, size, insert); // 发送增量广播
    }


    @Override
    public int getSlots()
    {
        return storage.size();
    }

    // 返回副本
    @Override
    public IStackType<?> getStackBySlot(int slot)
    {
        return IStackTypedHandler.super.getStackBySlot(slot);
    }

    // 外部不可修改
    @Override
    public List<IStackType<?>> getStorage()
    {
        return Collections.unmodifiableList(this.storage);
    }

    // 为UI界面提供一个外部修改方案
    @Override
    public void clearStorage()
    {
        this.storage.clear();
        typeIdIndex.clear();
        onChange();
    }

    @Override
    public boolean hasStackType(IStackType<?> other)
    {
        return storage.contains(other);
    }

    // 仅用于UI，不做任何状态更新，禁止于服务端调用
    public void setStackAmount(IStackType<?> key, long amount)
    {
        IStackType<?> old = getStackByStack(key);
        if (!old.isEmpty())
        {
            if (amount > 0)
                old.setStackAmount(amount);
            else
                extract(key.copyWithCount(old.getStackAmount() + 1), false);
        }
        else if (amount > 0) // old不存在
        {
            insert(key.copyWithCount(amount), false);
        }
    }

    @Override
    public void setStackDirectly(int slot, IStackType<?> stack)
    {
        if (!storage.contains(stack))
        {
            ResourceLocation newTypeId = stack.getTypeId();
            ResourceLocation oldTypeId = getStorage().get(slot).getTypeId();
            storage.set(slot, stack.copy());
            // 更新索引
            typeIdIndex.computeIfAbsent(oldTypeId, k -> new ArrayList<>()).remove(Integer.valueOf(slot));
            typeIdIndex.computeIfAbsent(newTypeId, k -> new ArrayList<>()).add(slot);
            onChange();
        }
    }

    @Override
    public void addStackDirectly(IStackType<?> stack)
    {
        if (!storage.contains(stack))
        {
            ResourceLocation newTypeId = stack.getTypeId();
            int slot = storage.size();
            storage.add(stack.copy());
            typeIdIndex.computeIfAbsent(newTypeId, k -> new ArrayList<>()).add(slot);
            onChange();
        }
    }

    @Override
    public IStackType<?> getStackByStack(IStackType<?> stackType)
    {
        IStackType<?> stack = storage.get(stackType); // 对于不存在的，会返回null
        if (stack != null)
            return stack;
        else
            return stackType.getEmpty(); // 不返回null了，防止错误调用
    }

    @Override
    public long getSlotCapacity(int slot)
    {
        if (net.deleted)
            return 0;

        return slotCapacity;
    }

    @Override
    public boolean isStackValid(int slot, IStackType<?> stack)
    {
        return true;
    }

    @Override
    public boolean isEmpty()
    {
        return storage.isEmpty(); // 由于此结构特殊性质，实际上只看列表长度即可
    }


    // region 核心操作方法

    // 插入stack 返回剩余量
    @Override
    public IStackType<?> insert(int slot, IStackType<?> stack, boolean simulate)
    {
        return insert(stack, simulate);
    }

    @Override
    public IStackType<?> insert(IStackType<?> stack, boolean simulate)
    {
        if (stack.isEmpty()) return stack.getEmpty();

        // 对物质压缩球的特殊处理
        if (stack instanceof ItemStackType itemStackType)
        {
            if (itemStackType.getStack().getItem() instanceof MatterCompressionBall)
            {
                return unzipMatterBall(itemStackType, simulate);
            }
        }

        // 正常处理
        long remaining = stack.getStackAmount(); // 剩余可被插入的量
        long canInsert = Math.min(getSlotCapacity(0), stack.getCustomMaxStackSize()); // 能被插入的空间

        // 尝试合并现有堆叠
        IStackType<?> existing = storage.get(stack);
        if (existing != null && !existing.isEmpty())
        {
            canInsert = Math.max(0L, canInsert - existing.getStackAmount()); // 防止剩余容量为负
            if (canInsert <= 0) // 完全无法合并，直接返回原堆叠
                return stack.copyWithCount(remaining);

            long actualInsert = Math.min(remaining, canInsert);
            remaining = remaining - actualInsert;

            if (!simulate && actualInsert > 0)
            { // 有实际变化，且非模拟操作
                existing.grow(actualInsert);
                onContentChanged(existing, actualInsert, true); //此处无需传递复制值，因为如果existing增加后的size仍为0，则没有增加的必要
            }
            return stack.copyWithCount(remaining); // 返回剩余量
        }

        // 现有堆叠未找到，尝试新增
        if (storage.size() < slotMaxSize)
        {
            long actualInsert = Math.min(remaining, canInsert);
            if (actualInsert <= 0) return stack.copyWithCount(remaining); // 可插入量为0时，不新建槽位（此处也可能是容量导致的0，因此返回原堆叠）

            remaining = remaining - actualInsert;
            if (!simulate)
            {
                IStackType<?> newStack = stack.copyWithCount(actualInsert);
                storage.add(newStack);

                // 更新索引
                int newIndex = storage.size() - 1;
                typeIdIndex.computeIfAbsent(stack.getTypeId(), k -> new ArrayList<>()).add(newIndex);

                onContentChanged(newStack, actualInsert, true); //此处无需传递复制值，如果newStack的size为0，则没有增加的必要
            }
        }
        return stack.copyWithCount(remaining);
    }

    /**
     * 解压物质球的函数 函数处理应当相当稳定，即使是物质球内有其他物质球也应能正常解压（不过应当尽量避免这种操作）
     * <p>
     * 输入一个物质球堆叠 输入后，模拟是否能完全插入，如果可以，就插入，否则就完全不操作
     * <p>
     * 输出一个物质球堆叠 输出的目的是，如果能完全解压，返回空，否则返回物质球堆叠本身
     */
    protected IStackType<?> unzipMatterBall(ItemStackType stack, boolean simulate)
    {
        ItemStack ballStack = stack.copyStack();
        List<IStackType<?>> ballStorage;

        // 再次检测，如果不是物质球则不处理
        if (!(stack.getStack().getItem() instanceof MatterCompressionBall))
            return stack;

        if (MatterCompressionBall.hasIStackList(ballStack))
        {
            long ballNum = stack.getStackAmount(); // 乘数，防止未知情况下，多个相同nbt的物质球同时被插入。 虽然一般不可能

            List<IStackType<?>> newBallStorage = new ArrayList<>();
            ballStorage = MatterCompressionBall.getIStackList(ballStack);

            // 首先模拟物质插入
            for (IStackType<?> stackType : ballStorage)
            {
                stackType.setStackAmount(stackType.getStackAmount() * ballNum);
                newBallStorage.add(insert(stackType, true));
            }
            // 假设剩余为空，然后遍历newBallStorage，如果遇见不为空的堆叠，则设置球不为空
            boolean newBallStorageIsEmpty = true;
            for (IStackType<?> stackType : newBallStorage)
            {
                if (!stackType.isEmpty())
                {
                    newBallStorageIsEmpty = false;
                }
            }

            // 如果球为空，则执行insert操作
            if (newBallStorageIsEmpty)
            {
                if (simulate)
                    return new ItemStackType();
                else
                {
                    for (IStackType<?> stackType : ballStorage)
                    {
                        stackType.setStackAmount(stackType.getStackAmount() * ballNum);
                        insert(stackType, false);
                    }
                    return new ItemStackType();
                }
            }
            else //如果球不为空，则不进行插入操作，原路返回
            {
                return stack;
            }
        }
        // 如果物质球内未存储堆叠，则直接消耗球
        return new ItemStackType();
    }

    // 尝试按类型导出，返回实际导出量
    @Override
    public IStackType<?> extract(IStackType<?> stack, boolean simulate)
    {
        // 此处模拟操作固定一次copy，非模拟固定2次copy
        if (stack.isEmpty()) return stack.getEmpty();

        List<Integer> indices = typeIdIndex.get(stack.getTypeId());
        if (indices == null || indices.isEmpty()) return stack.getEmpty();

        IStackType<?> existing = storage.get(stack); // 在此list中，从stack来get比直接get索引更快
        if (existing == null || existing.isEmpty()) return stack.getEmpty();

        long available = existing.getStackAmount();
        long extracted = Math.min(stack.getStackAmount(), available); // 导出量为要求量与可用量的最小值
        if (extracted <= 0)
        {
            return stack.getEmpty();
        }

        // 模拟：生成一个“只带数量”的结果副本即可，无需额外快照
        if (simulate)
        {
            return existing.copyWithCount(extracted);
        }

        // 真正执行：在任何修改 existing 之前做一次“类型快照”，避免后续 shrink 导致取空
        IStackType<?> beforeType = existing.copyWithCount(1);
        // 先构造返回值
        IStackType<?> result = beforeType.copyWithCount(extracted);

        // 再进行实际变更
        if (extracted >= available)
        {
            // 全量取走：移除并维护索引
            int storageIndex = storage.indexOf(stack); // 走到这一步时，storageIndex必然存在合适值
            storage.remove(storageIndex);
            indices.remove(Integer.valueOf(storageIndex));
            updateIndicesAfterRemoval(storageIndex);
            if (indices.isEmpty())
            {
                typeIdIndex.remove(stack.getTypeId());
            }
        }
        else
        {
            // 部分取走：只 shrink，不影响 indices
            existing.shrink(extracted);
        }

        // 使用“变更前”的类型快照进行通知，保证一致性
        onContentChanged(beforeType, extracted, false);
        return result;
    }

    /**
     * 当从storage中移除一个堆叠后，更新所有受影响的索引值
     *
     * @param removedIndex 被移除堆叠的索引值
     */
    private void updateIndicesAfterRemoval(int removedIndex)
    {
        for (List<Integer> indexList : typeIdIndex.values())
        {
            for (int i = 0; i < indexList.size(); i++)
            {
                int currentIndex = indexList.get(i);
                if (currentIndex > removedIndex)
                {
                    indexList.set(i, currentIndex - 1);
                }
            }
        }
    }

    /**
     * 当外界对存储列表直接操作后调用（如用于UI界面的数据包发送）
     */
    public void rebuildAllIndices()
    {
        typeIdIndex.clear();
        for (int i = 0; i < storage.size(); i++)
        {
            IStackType<?> stack = storage.get(i);
            if (stack != null && !stack.isEmpty())
            {
                ResourceLocation typeId = stack.getTypeId();
                typeIdIndex.computeIfAbsent(typeId, k -> new ArrayList<>()).add(i);
            }
        }
    }

    // 尝试按槽位导出 返回实际导出量
    @Override
    public IStackType<?> extract(int slot, long amount, boolean simulate)
    {
        if (slot < 0 || slot >= storage.size())
        {
            return null;
        }

        IStackType<?> existing = storage.get(slot);
        if (existing == null || existing.isEmpty())
        {
            return (existing != null) ? existing.getEmpty() : new ItemStackType(); // 如果为null，最终保底返回一个空物品实现，绝对不能返回null
        }

        long available = existing.getStackAmount();
        long extracted = Math.min(amount, available);
        if (extracted <= 0)
        {
            return existing.getEmpty();
        }

        // 模拟：只需1次副本
        if (simulate)
        {
            return existing.copyWithCount(extracted);
        }

        // 非模拟：修改前做“类型快照”（count=1），保证通知一致性
        IStackType<?> beforeType = existing.copyWithCount(1);
        IStackType<?> result = beforeType.copyWithCount(extracted); // 第 2 次 copy：返回值

        if (extracted == available)
        {
            // 全量取走：移除并维护索引
            final ResourceLocation typeId = existing.getTypeId();
            storage.remove(slot);

            List<Integer> indices = typeIdIndex.get(typeId);
            if (indices != null)
            {
                indices.remove(Integer.valueOf(slot));
                if (indices.isEmpty())
                {
                    typeIdIndex.remove(typeId);
                }
            }

            // 下调受影响的索引
            updateIndicesAfterRemoval(slot);
        }
        else
        {
            // 部分取走：只 shrink，不动索引表
            existing.shrink(extracted);
        }

        // 用 变更前 的类型快照进行通知
        onContentChanged(beforeType, extracted, false);
        return result;
    }
    // endregion

    // region 序列化方法
    public CompoundTag serializeNBT()
    {
        try
        { // 尝试保存
            CompoundTag tag = new CompoundTag();

            tag.putLong("slotCapacity", this.slotCapacity);
            tag.putInt("slotMaxSize", this.slotMaxSize);

            ListTag stacksTag = new ListTag();

            for (IStackType<?> stack : storage)
            {
                // 修改后的序列化代码
                if (stack.isEmpty())
                    continue; // 不序列化空物品
                CompoundTag stackTag = new CompoundTag();
                // 使用类型安全的序列化方式 将堆叠数据放入"Data"标签
                stackTag.put("TypedStack", stack.serializeNBT());
                stackTag.putString("Type", stack.getTypeId().toString());
                stacksTag.add(stackTag);
            }

            tag.put("Stacks", stacksTag);

            // 保存成功，刷新备份并返回
            backupStorage = deepClone(storage);
            return tag;

        }
        catch (Exception ex)
        {
            BeyondDimensions.LOGGER.error("{}号维度网络保存失败，尝试使用最近的备份并打印错误信息", net.getId(), ex);

            // === 回滚：用备份替换当前内存状态 ===
            storage.clear();
            if (backupStorage != null)
            {
                for (IStackType<?> s : backupStorage) storage.add(s.copy());
            }

            CompoundTag tag = new CompoundTag();

            tag.putLong("slotCapacity", this.slotCapacity);
            tag.putInt("slotMaxSize", this.slotMaxSize);

            ListTag stacksTag = new ListTag();

            for (IStackType<?> stack : storage)
            {
                // 修改后的序列化代码
                if (stack.isEmpty())
                    continue; // 不序列化空物品
                CompoundTag stackTag = new CompoundTag();
                // 使用类型安全的序列化方式 将堆叠数据放入"Data"标签
                stackTag.put("TypedStack", stack.serializeNBT());
                stackTag.putString("Type", stack.getTypeId().toString());
                stacksTag.add(stackTag);
            }

            tag.put("Stacks", stacksTag);

            // 保存成功
            return tag;
        }
    }

    public void deserializeNBT(CompoundTag tag)
    {
        storage.clear();
        typeIdIndex.clear();

        // 旧数据兼容
        if (tag.contains("slotCapacity"))
            slotCapacity = tag.getLong("slotCapacity");
        else
            slotCapacity = Long.MAX_VALUE;
        if (tag.contains("slotMaxSize"))
            slotMaxSize = tag.getInt("slotMaxSize");
        else
            slotMaxSize = Integer.MAX_VALUE;

        ListTag stacksTag = tag.getList("Stacks", Tag.TAG_COMPOUND);

        for (Tag t : stacksTag)
        {
            CompoundTag stackTag = (CompoundTag) t;
            ResourceLocation typeId = ResourceLocation.tryParse(stackTag.getString("Type"));

            // 旧版本兼容
            if (typeId.equals(ResourceLocation.tryBuild(BeyondDimensions.MODID, "stack_type/chemical")))
                typeId = GasStackType.ID;

            IStackType stackEmpty = StackTypeRegistry.getType(typeId).copy();
            IStackType stackActual = stackEmpty.deserializeNBT(stackTag.getCompound("TypedStack"));
            if (stackActual.isEmpty())
                continue; // 不添加空物品

            insert(stackActual, false); // 通过insert函数，而不是直接操作列表，自动处理反序列化中的一些问题并自动更新索引
        }
        backupStorage = deepClone(storage); // 对内容进行备份
    }
    // endregion

    // 辅助方法，用于获取索引表
    public Map<ResourceLocation, List<Integer>> getTypeIdIndexMap()
    {
        return this.typeIdIndex;
    }

    /**
     * 获取资源类型对应的索引列表
     */
    public Optional<List<Integer>> getTypeIdIndexList(ResourceLocation typeId)
    {
        return Optional.ofNullable(this.typeIdIndex.get(typeId))
                .filter(list -> !list.isEmpty());
    }

    /**
     * 快速获取当前网络存储的FE能量总量，辅助方法
     */
    public long getEnergyStored()
    {
        IStackType<?> stack = getStackByStack(EnergyStackType.EMPTY);
        if (stack != null)
            return stack.getStackAmount();
        return 0;
    }

    // 自身属性管理

    /**
     * 检查当前存储是否已经到达槽位数量的最大上限
     */
    public boolean isFullSlotsSize()
    {
        return storage.size() >= slotMaxSize;
    }

    // 理论上来说，以下两个数即使是负数，逻辑也能正常运行，所以没必要再额外检查了

    /**
     * 设置单个槽位可存储容量的上限
     */
    public void setSlotCapacity(long capacity)
    {
        this.slotCapacity = capacity;
        net.setDirty();
    }

    /**
     * 设置槽位数量的上限
     */
    public void setSlotMaxSize(int maxSize)
    {
        this.slotMaxSize = maxSize;
        net.setDirty();
    }

    /**
     * 把 origin 深克隆到一个新的 HashBPlusList
     */
    private static HashBPlusList<IStackType<?>> deepClone(HashBPlusList<IStackType<?>> origin)
    {
        HashBPlusList<IStackType<?>> clone = new HashBPlusList<>(64, 128);
        for (IStackType<?> s : origin)
        {
            clone.add(s.copy());   // IStackType.copy() = 深克隆
        }
        return clone;
    }
}

