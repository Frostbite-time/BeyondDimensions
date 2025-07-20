package com.wintercogs.beyonddimensions.Api.DataBase.Storage;

import com.wintercogs.beyonddimensions.Api.DataBase.DimensionsNet;
import com.wintercogs.beyonddimensions.Api.DataBase.Handler.IStackTypedHandler;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.EnergyStackType;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.IStackType;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.ItemStackType;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.StackCreater;
import com.wintercogs.beyonddimensions.Api.Registry.StackTypeRegistry;
import com.wintercogs.beyonddimensions.Api.Util.HashBPlusList;
import com.wintercogs.beyonddimensions.DataComponents.ModDataComponents;
import com.wintercogs.beyonddimensions.Item.Custom.MatterCompressionBall;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.*;
import java.util.function.Function;


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
    private final HashBPlusList<IStackType> storage = new HashBPlusList<>(64,128);

    /**
     * 为构建分化包装提供良好的性能，其结构为 [资源种类id：对应资源类型的索引列表]
     */
    private final Map<ResourceLocation, List<Integer>> typeIdIndex = new HashMap<>();

    /**
     * 用于存储创建分化包装的函数
     */
    public static final Map<ResourceLocation, Function<UnifiedStorage,Object>> typedHandlerMap = new HashMap<>();

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

    @Override
    public void onChange()
    {
        net.setDirty();
    }

    @Override
    public Object getTypedHandler(ResourceLocation typeId)
    {
        return typedHandlerMap.get(typeId).apply(this);
    }

    @Override
    public int getSlots()
    {
        return storage.size();
    }

    // 返回副本
    @Override
    public IStackType getStackBySlot(int slot)
    {
        return IStackTypedHandler.super.getStackBySlot(slot);
    }

    // 外部不可修改
    @Override
    public List<IStackType> getStorage()
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
    public boolean hasStackType(IStackType other)
    {
        return storage.contains(other);
    }

    @Override
    public void setStackDirectly(int slot, IStackType stack)
    {
        ResourceLocation newTypeId = stack.getTypeId();
        ResourceLocation oldTypeId = getStorage().get(slot).getTypeId();
        storage.set(slot,stack.copy());
        // 更新索引
        typeIdIndex.computeIfAbsent(oldTypeId, k -> new ArrayList<>()).remove(Integer.valueOf(slot));
        typeIdIndex.computeIfAbsent(newTypeId, k -> new ArrayList<>()).add(slot);

        onChange();
    }

    @Override
    public void addStackToIndexDirectly(int slot, IStackType stack)
    {
        //使用add方法增加Stack，并且更新索引
        ResourceLocation newTypeId = stack.getTypeId();
        storage.add(slot,stack.copy());
        // storage自增导致的可能的空索引位置无需管，因为那个位置是null。如果读取必然出错，这是编写时候由其他方法保证的
        typeIdIndex.computeIfAbsent(newTypeId, k -> new ArrayList<>()).add(slot);
        onChange();
    }

    @Override
    public void addStackDirectly(IStackType stack)
    {
        //使用add方法增加Stack，并且更新索引
        ResourceLocation newTypeId = stack.getTypeId();
        int slot = storage.size();
        storage.add(stack.copy());
        typeIdIndex.computeIfAbsent(newTypeId, k -> new ArrayList<>()).add(slot);
        onChange();
    }

    @Override
    public IStackType getStackByStack(IStackType stackType)
    {
        return storage.get(stackType); // 对于不存在的，会返回null
    }

    @Override
    public long getSlotCapacity(int slot)
    {
        if (net.deleted)
            return 0;

        return slotCapacity;
    }

    @Override
    public boolean isStackValid(int slot, IStackType stack)
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
    public IStackType insert(int slot, IStackType stack, boolean simulate)
    {
        return insert(stack,simulate);
    }

    @Override
    public IStackType insert(IStackType stack,boolean simulate) {
        if (stack.isEmpty()) return StackCreater.CreateEmpty(stack.getTypeId());

        // 对物质压缩球的特殊处理
        if(stack instanceof ItemStackType itemStackType)
        {
            if(itemStackType.getStack().getItem() instanceof MatterCompressionBall)
            {
                return unzipMatterBall(itemStackType, simulate);
            }
        }

        // 正常处理
        long remaining = stack.getStackAmount(); // 剩余可被插入的量
        long canInsert = Math.min(getSlotCapacity(0),stack.getCustomMaxStackSize()); // 能被插入的空间

        // 尝试合并现有堆叠
        if(storage.contains(stack))
        {
            IStackType existing = storage.get(stack);
            canInsert = canInsert - existing.getStackAmount();
            long actualInsert = Math.min(remaining,canInsert);
            remaining = remaining-actualInsert;

            if (!simulate) {
                existing.grow(actualInsert);
                onChange();
            }
            return stack.copyWithCount(remaining);
        }

        // 现有堆叠未找到，尝试新增
        if(storage.size() < slotMaxSize)
        {
            long actualInsert = Math.min(remaining,canInsert);
            remaining = remaining-actualInsert;
            if(!simulate)
            {
                IStackType newStack = stack.copyWithCount(actualInsert);
                storage.add(newStack);

                // 更新索引
                int newIndex = storage.size() - 1;
                typeIdIndex.computeIfAbsent(stack.getTypeId(), k -> new ArrayList<>()).add(newIndex);

                onChange();
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
    protected IStackType unzipMatterBall(ItemStackType stack, boolean simulate)
    {
        ItemStack ballStack = stack.copyStack();
        List<IStackType> ballStorage;

        // 再次检测，如果不是物质球则不处理
        if(!(stack.getStack().getItem() instanceof MatterCompressionBall))
            return stack;

        if(ballStack.has(ModDataComponents.ISTACK_SLOTS))
        {
            long ballNum = stack.getStackAmount(); // 乘数，防止未知情况下，多个相同nbt的物质球同时被插入。 虽然一般不可能

            List<IStackType> newBallStorage = new ArrayList<>();
            ballStorage = ballStack.get(ModDataComponents.ISTACK_SLOTS);

            // 首先模拟物质插入
            for(IStackType stackType: ballStorage)
            {
                stackType.setStackAmount(stackType.getStackAmount()*ballNum);
                newBallStorage.add(insert(stackType,true));
            }
            // 假设剩余为空，然后遍历newBallStorage，如果遇见不为空的堆叠，则设置球不为空
            boolean newBallStorageIsEmpty = true;
            for(IStackType stackType: newBallStorage)
            {
                if(!stackType.isEmpty())
                {
                    newBallStorageIsEmpty = false;
                }
            }

            // 如果球为空，则执行insert操作
            if(newBallStorageIsEmpty)
            {
                if(simulate)
                    return new ItemStackType();
                else
                {
                    for(IStackType stackType: ballStorage)
                    {
                        stackType.setStackAmount(stackType.getStackAmount()*ballNum);
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
    public IStackType extract(IStackType stack, boolean simulate) {
        if (stack.isEmpty()) return stack.getEmpty();

        List<Integer> indices = typeIdIndex.get(stack.getTypeId());

        if (storage.contains(stack))
        {
            int storageIndex = storage.indexOf(stack);
            IStackType existing = storage.get(stack);

            long extracted = Math.min(stack.getStackAmount(), existing.getStackAmount());
            IStackType sim = existing.copy();
            IStackType result = sim.split(extracted);

            if (!simulate) {
                existing.shrink(extracted);
                if (existing.getStackAmount() <= 0) {
                    storage.remove(storageIndex);
                    indices.remove(Integer.valueOf(storageIndex));

                    // 更新受影响的索引
                    updateIndicesAfterRemoval(storageIndex);

                    if (indices.isEmpty()) {
                        typeIdIndex.remove(stack.getTypeId());
                    }
                }
                onChange();
            }
            return result;
        }



        return stack.getEmpty();
    }

    /**
     * 当从storage中移除一个堆叠后，更新所有受影响的索引值
     * @param removedIndex 被移除堆叠的索引值
     */
    private void updateIndicesAfterRemoval(int removedIndex) {
        for (List<Integer> indexList : typeIdIndex.values()) {
            for (int i = 0; i < indexList.size(); i++) {
                int currentIndex = indexList.get(i);
                if (currentIndex > removedIndex) {
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
        for(int i = 0; i < storage.size(); i++)
        {
            IStackType stack = storage.get(i);
            if(stack != null && !stack.isEmpty())
            {
                ResourceLocation typeId = stack.getTypeId();
                typeIdIndex.computeIfAbsent(typeId, k -> new ArrayList<>()).add(i);
            }
        }
    }

    // 尝试按槽位导出 返回实际导出量
    @Override
    public IStackType extract(int slot,long amount, boolean simulate) {
        if (slot < 0 || slot >= storage.size()) {
            return null;
        }
        
        IStackType existing = storage.get(slot);
        if (existing.isEmpty()) return existing.getEmpty();

        long extracted = Math.min(amount, existing.getStackAmount());
        IStackType sim = existing.copy();
        IStackType result = sim.split(extracted);
        if (!simulate) {
            existing.shrink(extracted);
            if (existing.getStackAmount() <= 0) {
                ResourceLocation typeId = existing.getTypeId();
                storage.remove(slot);
                
                // 更新索引
                List<Integer> indices = typeIdIndex.get(typeId);
                if (indices != null) {
                    indices.remove(Integer.valueOf(slot));
                    if (indices.isEmpty()) {
                        typeIdIndex.remove(typeId);
                    }
                }
                
                // 更新受影响的索引
                updateIndicesAfterRemoval(slot);
                
                onChange();
            }
        }
        return result;
    }
    // endregion

    // region 序列化方法
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();

        tag.putLong("slotCapacity", this.slotCapacity);
        tag.putInt("slotMaxSize", this.slotMaxSize);

        ListTag stacksTag = new ListTag();

        for (IStackType stack : storage) {
            // 修改后的序列化代码
            if(stack.isEmpty())
                continue; // 不序列化空物品
            CompoundTag stackTag = new CompoundTag();
            // 使用类型安全的序列化方式 将堆叠数据放入"Data"标签
            stackTag.put("TypedStack",stack.serializeNBT(provider));
            stackTag.putString("Type",stack.getTypeId().toString());
            stacksTag.add(stackTag);
        }

        tag.put("Stacks", stacksTag);

        return tag;
    }

    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        storage.clear();
        typeIdIndex.clear();

        // 旧数据兼容
        if(tag.contains("slotCapacity"))
            slotCapacity = tag.getLong("slotCapacity");
        else
            slotCapacity = Long.MAX_VALUE;
        if(tag.contains("slotMaxSize"))
            slotMaxSize = tag.getInt("slotMaxSize");
        else
            slotMaxSize = Integer.MAX_VALUE;

        ListTag stacksTag = tag.getList("Stacks", Tag.TAG_COMPOUND);

        for (Tag t : stacksTag) {
            CompoundTag stackTag = (CompoundTag) t;
            ResourceLocation typeId = ResourceLocation.parse(stackTag.getString("Type"));
            IStackType stackEmpty = StackTypeRegistry.getType(typeId).copy();
            IStackType stackActual = stackEmpty.deserializeNBT(stackTag.getCompound("TypedStack"),provider);
            if(stackActual.isEmpty())
                continue; // 不添加空物品
                
            this.storage.add(stackActual);
            // 更新索引
            typeIdIndex.computeIfAbsent(typeId, k -> new ArrayList<>()).add(storage.size() - 1);
        }
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
        IStackType stack = getStackByStack(EnergyStackType.EMPTY);
        if(stack != null)
            return stack.getStackAmount();
        return 0;
    }

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
    }

    /**
     * 设置槽位数量的上限
     */
    public void setSlotMaxSize(int maxSize)
    {
        this.slotMaxSize = maxSize;
    }
}

