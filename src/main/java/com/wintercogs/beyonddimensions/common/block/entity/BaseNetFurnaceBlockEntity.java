package com.wintercogs.beyonddimensions.common.block.entity;

import com.wintercogs.beyonddimensions.api.capability.helper.ordered.ItemStackTypedHandler;
import com.wintercogs.beyonddimensions.api.capability.helper.wrapper.ItemHandlerWrapper;
import com.wintercogs.beyonddimensions.api.dimensionnet.UnifiedStorage;
import com.wintercogs.beyonddimensions.api.storage.handler.impl.StackHandler;
import com.wintercogs.beyonddimensions.api.storage.key.IStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.KeyAmount;
import com.wintercogs.beyonddimensions.api.storage.key.impl.EnergyStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.impl.FluidStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.impl.ItemStackKey;
import com.wintercogs.beyonddimensions.common.block.BaseNetFurnaceBlock;
import com.wintercogs.beyonddimensions.common.init.BDDataComponents;
import com.wintercogs.beyonddimensions.common.init.BDFluids;
import com.wintercogs.beyonddimensions.common.init.BDItems;
import com.wintercogs.beyonddimensions.common.item.MatterCompressionBall;
import com.wintercogs.beyonddimensions.common.item.XpExchangeItem;
import com.wintercogs.beyonddimensions.common.machine.AutoSortMode;
import com.wintercogs.beyonddimensions.common.machine.PopMode;
import com.wintercogs.beyonddimensions.common.machine.ReceiveMode;
import com.wintercogs.beyonddimensions.common.menu.NetFurnaceMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.CombinedResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Supplier;

public abstract class BaseNetFurnaceBlockEntity<R extends AbstractCookingRecipe> extends BaseMachineBlockEntity implements MenuProvider
{
    /**
     * 同时处理的任务格数
     */
    private static final int capacity = 9;

    public int getCapacity()
    {
        return capacity;
    }

    /**
     * 同时能用的标记格数
     */
    private static final int filterCapacity = 8;

    public int getFilterCapacity()
    {
        return filterCapacity;
    }

    /**
     * 燃料槽个数
     */
    private static final int fuelCapacity = 1;

    public int getFuelCapacity()
    {
        return fuelCapacity;
    }

    /**
     * 是否弹出输出物
     */
    public PopMode popMode = PopMode.STOP;

    /**
     * 是否将输出物送回网络
     */
    public ReceiveMode receiveMode = ReceiveMode.STOP;

    /**
     * 自动整理内容物
     */
    public AutoSortMode sortMode = AutoSortMode.STOP;

    private final RecipeType<R> recipeType;
    private final Component displayName;
    private final List<RecipeManager.CachedCheck<SingleRecipeInput, R>> quickChecks;

    /**
     * 尚未收入网络或发放给玩家的熔炼经验
     */
    private double storedExperience = 0.0D;

    public double getStoredExperience()
    {
        return storedExperience;
    }

    /**
     * 槽位剩余燃烧 tick
     */
    private List<Integer> litTime = new ArrayList<>(Collections.nCopies(capacity, 0));

    public List<Integer> getLitTime()
    {
        return litTime;
    }

    public void setLitTime(List<Integer> litTime)
    {
        this.litTime = litTime;
    }

    /**
     * 槽位燃料总 tick
     */
    private List<Integer> litDuration = new ArrayList<>(Collections.nCopies(capacity, 0));

    public List<Integer> getLitDuration()
    {
        return litDuration;
    }

    public void setLitDuration(List<Integer> litDuration)
    {
        this.litDuration = litDuration;
    }

    /**
     * 槽位为此次配方燃烧的 tick
     */
    private List<Integer> cookTime = new ArrayList<>(Collections.nCopies(capacity, 0));

    public List<Integer> getCookTime()
    {
        return cookTime;
    }

    public void setCookTime(List<Integer> cookTime)
    {
        this.cookTime = cookTime;
    }

    /**
     * 槽位配方所需 tick
     */
    private List<Integer> cookTimeTotal = new ArrayList<>(Collections.nCopies(capacity, 0));

    public List<Integer> getCookTimeTotal()
    {
        return cookTimeTotal;
    }

    public void setCookTimeTotal(List<Integer> cookTimeTotal)
    {
        this.cookTimeTotal = cookTimeTotal;
    }

    /**
     * 输入标记
     */
    private final StackHandler inputFilterSlots = new StackHandler(filterCapacity)
    {
        @Override
        public void onChange()
        {
            if (level != null && !level.isClientSide())
                level.blockEntityChanged(worldPosition);
        }

        @Override
        public boolean isStackValid(int slot, IStackKey<?> key)
        {
            // 仅接收可以熔炼的物品
            return key instanceof ItemStackKey itemKey && level instanceof ServerLevel serverLevel && quickChecks.get(slot).getRecipeFor(new SingleRecipeInput(itemKey.getReadOnlyStack()), serverLevel).isPresent();
        }
    };

    public StackHandler getInputFilterSlots()
    {
        return inputFilterSlots;
    }


    /**
     * 燃料标记
     */
    private final StackHandler fuelFilterSlots = new StackHandler(filterCapacity)
    {
        @Override
        public void onChange()
        {
            if (level != null && !level.isClientSide())
                level.blockEntityChanged(worldPosition);
        }

        @Override
        public boolean isStackValid(int slot, IStackKey<?> key)
        {
            // 能量或者可以燃烧的物品能作为燃料标记
            return (key instanceof EnergyStackKey)
                    || (key instanceof FluidStackKey fluidKey && fluidKey.getSource() == Fluids.LAVA)
                    || (key instanceof ItemStackKey itemKey && level != null && itemKey.getReadOnlyStack().getBurnTime(recipeType, level.fuelValues()) > 0);
        }

    };

    public StackHandler getFuelFilterSlots()
    {
        return fuelFilterSlots;
    }


    /**
     * 输入存储
     */
    private final StackHandler inputStorageSlots = new StackHandler(capacity)
    {
        @Override
        public void onChange()
        {
            if (level != null && !level.isClientSide())
                level.blockEntityChanged(worldPosition);
        }

        // 熔炉的特性，只能输入物品
        @Override
        public boolean isStackValid(int slot, IStackKey<?> key)
        {
            // 仅接收可以熔炼的物品
            return key instanceof ItemStackKey itemKey && level instanceof ServerLevel serverLevel && quickChecks.get(slot).getRecipeFor(new SingleRecipeInput(itemKey.getReadOnlyStack()), serverLevel).isPresent();
        }
    };

    public StackHandler getInputStorageSlots()
    {
        return inputStorageSlots;
    }

    /**
     * 输出存储
     */
    private final StackHandler outputStorageSlots = new StackHandler(capacity)
    {
        @Override
        public void onChange()
        {
            if (level != null && !level.isClientSide())
                level.blockEntityChanged(worldPosition);
        }

    };

    public StackHandler getOutputStorageSlots()
    {
        return outputStorageSlots;
    }

    /**
     * 燃料存储
     */
    private final StackHandler fuelStorageSlots = new StackHandler(fuelCapacity)
    {
        @Override
        public void onChange()
        {
            if (level != null && !level.isClientSide())
                level.blockEntityChanged(worldPosition);
        }

        @Override
        public boolean isStackValid(int slot, IStackKey<?> key)
        {
            // 能量或者可以燃烧的物品能作为燃料标记
            return (key instanceof EnergyStackKey)
                    || (key instanceof FluidStackKey fluidKey && fluidKey.getSource() == Fluids.LAVA)
                    || (key instanceof ItemStackKey itemKey && level != null && itemKey.getReadOnlyStack().getBurnTime(recipeType, level.fuelValues()) > 0);
        }
    };

    public StackHandler getFuelStorageSlots()
    {
        return fuelStorageSlots;
    }

    /**
     * 燃料返回物存储
     */
    private final StackHandler fuelReturnSlots = new StackHandler(fuelCapacity)
    {
        @Override
        public void onChange()
        {
            if (level != null && !level.isClientSide())
                level.blockEntityChanged(worldPosition);
        }
    };

    public StackHandler getFuelReturnSlots()
    {
        return fuelReturnSlots;
    }

    public BaseNetFurnaceBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState, RecipeType<R> recipeType, Component displayName)
    {
        super(type, pos, blockState);
        this.recipeType = recipeType;
        this.displayName = displayName;
        this.quickChecks = new ArrayList<>(capacity);
        for (int i = 0; i < capacity; i++)
        {
            this.quickChecks.add(RecipeManager.createCheck(recipeType));
        }
    }

    public static <T extends BaseNetFurnaceBlockEntity<?>> void registerItemHandlerCapability(RegisterCapabilitiesEvent event, Supplier<BlockEntityType<T>> blockEntityType)
    {
        event.registerBlockEntity(
                Capabilities.Item.BLOCK,
                blockEntityType.get(),
                (be, side) -> {
                    ItemStackTypedHandler inputStorage = new ItemStackTypedHandler(be.getInputStorageSlots())
                    {
                        @Override
                        public int extract(int index, ItemResource resource, int amount, @NotNull TransactionContext transaction)
                        {
                            return 0;
                        }

                        @Override
                        public int extract(@NotNull ItemResource resource, int amount, TransactionContext transaction)
                        {
                            return 0;
                        }
                    };

                    ItemStackTypedHandler fuelStorage = new ItemStackTypedHandler(be.getFuelStorageSlots())
                    {
                        @Override
                        public int extract(int index, ItemResource resource, int amount, @NotNull TransactionContext transaction)
                        {
                            return 0;
                        }

                        @Override
                        public int extract(@NotNull ItemResource resource, int amount, TransactionContext transaction)
                        {
                            return 0;
                        }
                    };

                    ItemStackTypedHandler outputStorage = new ItemStackTypedHandler(be.getOutputStorageSlots())
                    {
                        @Override
                        public int insert(int index, ItemResource resource, int amount, @NotNull TransactionContext transaction)
                        {
                            return 0;
                        }

                        @Override
                        public int insert(@NotNull ItemResource resource, int amount, TransactionContext transaction)
                        {
                            return 0;
                        }
                    };

                    ItemStackTypedHandler fuelReturn = new ItemStackTypedHandler(be.getFuelReturnSlots())
                    {
                        @Override
                        public int insert(int index, ItemResource resource, int amount, @NotNull TransactionContext transaction)
                        {
                            return 0;
                        }

                        @Override
                        public int insert(@NotNull ItemResource resource, int amount, TransactionContext transaction)
                        {
                            return 0;
                        }
                    };

                    return new CombinedResourceHandler<>(new ItemStackTypedHandler[]{inputStorage, fuelStorage, outputStorage, fuelReturn});
                }
        );
    }

    @Override
    public int getTicksPerWork()
    {
        return 1;
    }

    @Override
    public boolean shouldWork()
    {
        if (level == null) return false;

        // 无论是否工作，总是先降低燃料持续时间
        litTime.replaceAll(i -> Math.max(0, i - 1));
        // 更新方块状态
        setLit(!litTime.stream().allMatch(t -> t <= 0));

        // 总是保存区块
        level.blockEntityChanged(worldPosition);

        // 输入槽为空 并且 标记槽无物品，可以判为无工作意图
        // 再加上output和fuelreturn，可以正确执行弹出和收纳设置
        return super.shouldWork() &&
                (!inputStorageSlots.isEmpty() || !inputFilterSlots.isEmpty() || !outputStorageSlots.isEmpty() || !fuelReturnSlots.isEmpty() || !fuelStorageSlots.isEmpty() || !fuelFilterSlots.isEmpty());
    }

    @Override
    public void workStart()
    {
        super.workStart();

        var net = getNet();
        UnifiedStorage storage = net == null ? null : net.getUnifiedStorage();
        if (storage != null)
        {
            // 1.尝试按照标记槽位从网络抽取原料
            for (int inputSlot = 0; inputSlot < capacity; inputSlot++)
            {
                if (!inputStorageSlots.getStackBySlot(inputSlot).isEmpty()) continue;

                for (KeyAmount filterStack : inputFilterSlots.getStorage())
                {
                    if (!inputStorageSlots.getStackBySlot(inputSlot).isEmpty()) break; // 如果已经插入过则直接跳过
                    if (!(filterStack.key() instanceof ItemStackKey filterItem) || filterItem.isEmpty()) continue;

                    KeyAmount extracted = storage.extract(filterItem, filterItem.getVanillaMaxStackSize(), false, false);
                    if (extracted.isEmpty()) continue;

                    KeyAmount remaining = inputStorageSlots.insert(inputSlot, extracted.key(), extracted.amount(), false);
                    if (!remaining.isEmpty())
                    {
                        storage.insert(remaining.key(), remaining.amount(), false);
                    }
                }
            }
            // 2.如果开启了自动整理，则每tick进行一次快速整理
            if (sortMode == AutoSortMode.OPEN)
            {
                long[] amounts = new long[capacity]; //种类数量
                Map<IStackKey<?>, List<Integer>> groupSlots = new HashMap<>(); // 所属槽位
                Map<IStackKey<?>, Long> groupTotal = new HashMap<>(); // 种类总数
                List<Integer> emptySlots = new ArrayList<>(); // 标记可用的空槽位

                for (int i = 0; i < capacity; i++)
                {
                    KeyAmount stack = inputStorageSlots.getStackBySlot(i);
                    if (stack.isEmpty())
                    {
                        emptySlots.add(i);
                        continue;
                    }

                    IStackKey<?> key = stack.key();
                    long amount = stack.amount();
                    amounts[i] = amount;
                    groupSlots.computeIfAbsent(key, k -> new ArrayList<>()).add(i);
                    groupTotal.put(key, groupTotal.getOrDefault(key, 0L) + amount);
                }

                // 将同类物品在现有槽与可用空槽间尽量均分：先扩展目标槽列表，再用双指针从盈余槽搬到欠额槽。
                for (Map.Entry<IStackKey<?>, List<Integer>> entry : groupSlots.entrySet())
                {
                    List<Integer> typedSlots = entry.getValue();
                    long total = groupTotal.get(entry.getKey());

                    int k = (int) Math.min(total, typedSlots.size() + emptySlots.size());

                    while (typedSlots.size() < k && !emptySlots.isEmpty())
                    {
                        int idx = emptySlots.remove(emptySlots.size() - 1);
                        typedSlots.add(idx);
                    }

                    long base = total / k;
                    int extra = (int) (total % k);

                    int surplusPtr = 0, deficitPtr = 0;
                    while (true)
                    {
                        while (surplusPtr < k)
                        {
                            int idx = typedSlots.get(surplusPtr);
                            long target = base + (surplusPtr < extra ? 1 : 0);
                            if (amounts[idx] > target) break;
                            surplusPtr++;
                        }

                        while (deficitPtr < k)
                        {
                            int idx = typedSlots.get(deficitPtr);
                            long target = base + (deficitPtr < extra ? 1 : 0);
                            if (amounts[idx] < target) break;
                            deficitPtr++;
                        }

                        if (surplusPtr >= k || deficitPtr >= k) break;

                        int from = typedSlots.get(surplusPtr);
                        int to = typedSlots.get(deficitPtr);

                        long surplus = amounts[from] - (base + (surplusPtr < extra ? 1 : 0));
                        long deficit = (base + (deficitPtr < extra ? 1 : 0)) - amounts[to];
                        long move = Math.min(surplus, deficit);

                        KeyAmount moved = inputStorageSlots.extract(from, move, false);
                        KeyAmount leftover = inputStorageSlots.insert(to, moved.key(), moved.amount(), false);
                        if (!leftover.isEmpty())
                        {
                            // 插入失败时回滚本次移动，避免吞物品。
                            inputStorageSlots.insert(from, leftover.key(), leftover.amount(), false);
                            break;
                        }

                        amounts[from] -= move;
                        amounts[to] += move;
                    }
                }
            }
            // 3.尝试按燃料标记从网络抽取燃料 虽然当前燃料槽仅有一个，但是还是可以继续使用这个方法来方便后续修改
            for (int fuelSlot = 0; fuelSlot < fuelCapacity; fuelSlot++)
            {
                if (!fuelStorageSlots.getStackBySlot(fuelSlot).isEmpty()) continue;

                for (KeyAmount filterStack : fuelFilterSlots.getStorage())
                {
                    if (filterStack.isEmpty()) continue;
                    if (!fuelStorageSlots.getStackBySlot(fuelSlot).isEmpty()) break; // 如果已经插入过则直接跳过

                    KeyAmount extracted = storage.extract(filterStack.key(), filterStack.key().getVanillaMaxStackSize(), false, false);
                    if (extracted.isEmpty()) continue;

                    KeyAmount remaining = fuelStorageSlots.insert(fuelSlot, extracted.key(), extracted.amount(), false);
                    if (!remaining.isEmpty())
                    {
                        storage.insert(remaining.key(), remaining.amount(), false);
                    }
                }
            }
        }
        // 4.尝试将燃料分配到燃烧时间
        for (int litSlot = 0; litSlot < capacity; litSlot++)
        {
            // 燃料已经烧完，并且对应槽位仍然有需要冶炼的物品
            if (litTime.get(litSlot) > 0 || inputStorageSlots.getStackBySlot(litSlot).isEmpty()) continue;

            for (KeyAmount fuelStack : fuelStorageSlots.getStorage())
            {
                if (fuelStack.isEmpty()) continue;

                IStackKey<?> fuelKey = fuelStack.key();
                if (fuelKey instanceof EnergyStackKey)
                {
                    // 每个fe对应1tick燃烧时间
                    int burnTime = (int) Math.min(fuelStack.amount(), 20000);
                    if (burnTime <= 0) continue;

                    fuelStorageSlots.extract(fuelKey, burnTime, false, false);
                    litTime.set(litSlot, burnTime);
                    litDuration.set(litSlot, burnTime);
                }
                else if (fuelKey instanceof FluidStackKey fuelFluid && fuelFluid.getSource() == Fluids.LAVA)
                {
                    // 每mb熔岩对应20tick燃烧时间
                    int burnNum = (int) Math.min(fuelStack.amount(), 1000);
                    int burnTime = burnNum * 20;
                    if (burnTime <= 0) continue;

                    fuelStorageSlots.extract(fuelFluid, burnNum, false, false);
                    litTime.set(litSlot, burnTime);
                    litDuration.set(litSlot, burnTime);
                }
                else if (fuelKey instanceof ItemStackKey fuelItem)
                {
                    int burnTime = fuelItem.getReadOnlyStack().getBurnTime(recipeType, level.fuelValues());
                    if (burnTime <= 0) continue;

                    ItemStackTemplate returnTemplate = fuelItem.getReadOnlyStack().getCraftingRemainder();
                    if (returnTemplate == null)
                    {
                        fuelStorageSlots.extract(fuelItem, 1, false, false);
                        litTime.set(litSlot, burnTime);
                        litDuration.set(litSlot, burnTime);
                        continue;
                    }

                    // 先尝试插入returnItem，如果能插入再消耗
                    ItemStack returnItem = returnTemplate.create();
                    IStackKey<?> returnKey = new ItemStackKey(returnItem);
                    int returnCount = returnItem.getCount();
                    if (!fuelReturnSlots.insert(returnKey, returnCount, true).isEmpty())
                    {
                        // 无法补充燃料，则将双时间设为0
                        litTime.set(litSlot, 0);
                        litDuration.set(litSlot, 0);
                        continue;
                    }

                    fuelReturnSlots.insert(returnKey, returnCount, false);
                    fuelStorageSlots.extract(fuelItem, 1, false, false);
                    litTime.set(litSlot, burnTime);
                    litDuration.set(litSlot, burnTime);
                }
            }
        }
    }

    @Override
    public void workContent()
    {
        super.workContent();
        if (!(level instanceof ServerLevel serverLevel)) return;

        //开始熔炼
        for (int inputSlot = 0; inputSlot < capacity; inputSlot++)
        {
            if (litTime.get(inputSlot) <= 0) continue;

            KeyAmount inputStack = inputStorageSlots.getStackBySlot(inputSlot);
            if (!(inputStack.key() instanceof ItemStackKey inputItem) || inputItem.isEmpty())
            {
                // 如果物品不合法，时间重设为0
                cookTime.set(inputSlot, 0);
                cookTimeTotal.set(inputSlot, 0);
                continue;
            }

            RecipeHolder<R> recipeHolder = quickChecks.get(inputSlot)
                    .getRecipeFor(new SingleRecipeInput(inputItem.getReadOnlyStack()), serverLevel).orElse(null);
            if (recipeHolder == null)
            {
                // 如果不存在recipe，那么时间重设为0
                cookTime.set(inputSlot, 0);
                cookTimeTotal.set(inputSlot, 0);
                continue;
            }

            int totalCookTime = recipeHolder.value().cookingTime();
            cookTimeTotal.set(inputSlot, totalCookTime); // 一旦找到配方，始终重设总时间，以防错误越过
            if (cookTime.get(inputSlot) < totalCookTime)
            {
                cookTime.set(inputSlot, cookTime.get(inputSlot) + 1);
                continue;
            }

            // assemble内部并不实际查看input
            ItemStack resultItem = recipeHolder.value().assemble(new SingleRecipeInput(ItemStack.EMPTY));
            ItemStackKey resultKey = new ItemStackKey(resultItem);
            int resultCount = resultItem.getCount();

            // 如果能完全输出，则输出，并重设熔炼时间
            if (!outputStorageSlots.insert(inputSlot, resultKey, resultCount, true).isEmpty()) continue;

            outputStorageSlots.insert(inputSlot, resultKey, resultCount, false);
            inputStorageSlots.extract(inputSlot, 1, false);
            storedExperience = Math.clamp(recipeHolder.value().experience() + storedExperience, 0.0D, Integer.MAX_VALUE);
            cookTime.set(inputSlot, 0);
            cookTimeTotal.set(inputSlot, totalCookTime);
        }
    }

    @Override
    public void workEnd()
    {
        super.workEnd();
        if (level == null || level.isClientSide()) return;

        // 应用转移模式与弹出模式的设置
        // 优先弹出，再转移

        ArrayList<ItemHandlerWrapper> otherStorages = new ArrayList<>();
        if (popMode == PopMode.OPEN)
        {
            for (Direction dir : Direction.values())
            {
                BlockPos targetPos = this.getBlockPos().relative(dir);
                BlockEntity neighbor = level.getBlockEntity(targetPos);
                if (neighbor == null || neighbor instanceof NetedBlockEntity) continue;

                ResourceHandler<@NotNull ItemResource> otherStorage = level.getCapability(Capabilities.Item.BLOCK, targetPos, dir.getOpposite());
                if (otherStorage != null) otherStorages.add(new ItemHandlerWrapper(otherStorage));
            }
        }

        // 此处的net用于后续收入网络，如果接受模式未打开，此处net直接给null，后续跳过收入网络。
        var net = receiveMode == ReceiveMode.OPEN ? getNet() : null;
        UnifiedStorage storage = net == null ? null : net.getUnifiedStorage();

        // 输出槽处理
        for (int outputSlot = 0; outputSlot < capacity; outputSlot++)
        {
            KeyAmount outputStack = outputStorageSlots.getStackBySlot(outputSlot);
            if (outputStack.isEmpty()) continue;

            // 弹出模式
            for (ItemHandlerWrapper otherStorage : otherStorages)
            {
                for (int otherSlot = 0; otherSlot < otherStorage.getSlots(); otherSlot++)
                {
                    KeyAmount slotNow = outputStorageSlots.getStackBySlot(outputSlot);
                    if (slotNow.isEmpty() || !(slotNow.key() instanceof ItemStackKey itemKey))
                    {
                        break;
                    }

                    int moveCount = (int) Math.min(slotNow.amount(), itemKey.getVanillaMaxStackSize());
                    KeyAmount extracted = outputStorageSlots.extract(outputSlot, moveCount, false);
                    if (extracted.key() instanceof ItemStackKey extractedItemKey)
                    {
                        long remaining = otherStorage.insert(otherSlot, extractedItemKey.copyStackWithCount(extracted.amount()), false);
                        if (remaining > 0L)
                        {
                            outputStorageSlots.insert(outputSlot, extracted.key(), remaining, false);
                        }
                    }
                    else
                    {
                        outputStorageSlots.insert(outputSlot, extracted.key(), extracted.amount(), false);
                    }
                }
            }

            // 转移至网络
            if (storage != null)
            {
                KeyAmount slotNow = outputStorageSlots.getStackBySlot(outputSlot);
                if (slotNow.isEmpty()) continue;

                KeyAmount extracted = outputStorageSlots.extract(outputSlot, slotNow.amount(), false);
                if (extracted.isEmpty()) continue;

                KeyAmount remaining = storage.insert(outputSlot, extracted.key(), extracted.amount(), false);
                if (!remaining.isEmpty())
                {
                    outputStorageSlots.insert(outputSlot, remaining.key(), remaining.amount(), false);
                }
            }
        }

        // 燃料返回槽处理
        for (int returnSlot = 0; returnSlot < fuelCapacity; returnSlot++)
        {
            KeyAmount returnStack = fuelReturnSlots.getStackBySlot(returnSlot);
            if (returnStack.isEmpty()) continue;

            // 弹出模式
            for (ItemHandlerWrapper otherStorage : otherStorages)
            {
                for (int otherSlot = 0; otherSlot < otherStorage.getSlots(); otherSlot++)
                {
                    KeyAmount slotNow = fuelReturnSlots.getStackBySlot(returnSlot);
                    if (slotNow.isEmpty() || !(slotNow.key() instanceof ItemStackKey itemKey))
                    {
                        break;
                    }

                    int moveCount = (int) Math.min(slotNow.amount(), itemKey.getVanillaMaxStackSize());
                    KeyAmount extracted = fuelReturnSlots.extract(returnSlot, moveCount, false);
                    if (extracted.key() instanceof ItemStackKey extractedItemKey)
                    {
                        long remaining = otherStorage.insert(otherSlot, extractedItemKey.copyStackWithCount(extracted.amount()), false);
                        if (remaining > 0L)
                        {
                            fuelReturnSlots.insert(returnSlot, extracted.key(), remaining, false);
                        }
                    }
                    else
                    {
                        fuelReturnSlots.insert(returnSlot, extracted.key(), extracted.amount(), false);
                    }
                }
            }

            // 转移至网络
            if (storage != null)
            {
                KeyAmount slotNow = fuelReturnSlots.getStackBySlot(returnSlot);
                if (slotNow.isEmpty()) continue;

                KeyAmount extracted = fuelReturnSlots.extract(returnSlot, slotNow.amount(), false);
                if (extracted.isEmpty()) continue;

                KeyAmount remaining = storage.insert(returnSlot, extracted.key(), extracted.amount(), false);
                if (!remaining.isEmpty())
                {
                    fuelReturnSlots.insert(returnSlot, remaining.key(), remaining.amount(), false);
                }
            }
        }

        // 燃料槽处理-如果开始接收模式，在不标记能量时，将能量或流体等不方便存取的堆叠收回网络
        // 这会防止能量堵塞在燃料口
        for (int fuelSlot = 0; fuelSlot < fuelCapacity; fuelSlot++)
        {
            KeyAmount fuelStack = fuelStorageSlots.getStackBySlot(fuelSlot);
            if (fuelStack.isEmpty()) continue;
            if (!(fuelStack.key() instanceof EnergyStackKey || fuelStack.key() instanceof FluidStackKey)) continue;
            if (storage == null || fuelFilterSlots.hasStack(fuelStack.key())) continue;

            KeyAmount extracted = fuelStorageSlots.extract(fuelSlot, fuelStack.amount(), false);
            if (extracted.isEmpty()) continue;

            KeyAmount remaining = storage.insert(fuelSlot, extracted.key(), extracted.amount(), false);
            if (!remaining.isEmpty())
            {
                fuelStorageSlots.insert(fuelSlot, remaining.key(), remaining.amount(), false);
            }
        }

        // 自动收入开启时，将配方经验按经验交换棒的规则转换成经验流体。
        // 网络无法接纳的部分仍保存在方块实体中，等待下次收入阶段重试。
        if (storage != null)
        {
            transferStoredExperienceToNetwork(storage);
        }
    }

    private void transferStoredExperienceToNetwork(UnifiedStorage storage)
    {
        if (storedExperience <= 0.0D) return;

        int conversionRate = XpExchangeItem.getConversionRate();
        if (conversionRate <= 0) return;

        double exactFluidAmount = storedExperience * conversionRate;
        long fluidAmount = exactFluidAmount >= Long.MAX_VALUE
                ? Long.MAX_VALUE
                : Math.round(exactFluidAmount);
        if (fluidAmount <= 0L) return;

        FluidStackKey xpFluidKey = new FluidStackKey(new FluidStack(BDFluids.XP_FLUID.source().get(), 1));
        KeyAmount remaining = storage.insert(xpFluidKey, fluidAmount, false);
        long insertedFluid = Math.max(0L, fluidAmount - Math.max(0L, remaining.amount()));
        if (insertedFluid <= 0L) return;

        storedExperience = Math.max(0.0D, storedExperience - insertedFluid / (double) conversionRate);
        setChanged();
    }

    private void awardStoredExperience(Player player)
    {
        if (level == null || level.isClientSide() || receiveMode == ReceiveMode.OPEN || storedExperience <= 0.0D)
            return;

        // 与原版熔炉一致：总经验的小数部分按概率向上取整。
        int experience = (int) Math.floor(storedExperience);
        double fraction = storedExperience - experience;
        if (fraction > 0.0D && level.getRandom().nextDouble() < fraction)
        {
            experience++;
        }

        storedExperience = 0.0D;
        setChanged();
        if (experience > 0)
        {
            player.giveExperiencePoints(experience);
        }
    }

    public void dropContent()
    {
        if (level == null || level.isClientSide()) return;

        List<KeyAmount> dropList = new ArrayList<>();
        StackHandler[] handlers = {inputStorageSlots, outputStorageSlots, fuelStorageSlots, fuelReturnSlots};
        for (StackHandler handler : handlers)
        {
            for (KeyAmount stack : handler.getStorage())
            {
                if (stack.isEmpty()) continue;

                if (stack.key() instanceof ItemStackKey itemKey && itemKey.getSource() instanceof MatterCompressionBall)
                {
                    // 如果内含物质球，直接弹出，防止NBT套娃
                    Block.popResource(level, getBlockPos(), itemKey.copyStackWithCount(stack.amount()));
                    continue;
                }

                dropList.add(stack);
            }
        }

        if (dropList.isEmpty()) return;

        ItemStack ball = new ItemStack(BDItems.MATTER_COMPRESS_BALL.get());
        ball.set(BDDataComponents.ISTACK_SLOTS, dropList);
        Block.popResource(level, getBlockPos(), ball);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state)
    {
        super.preRemoveSideEffects(pos, state);
        if (level instanceof ServerLevel)
        {
            dropContent();
        }
    }


    @Override
    protected void loadAdditional(@NotNull ValueInput input)
    {
        super.loadAdditional(input);
        this.inputFilterSlots.deserializeNBT(input.lookup(), input.read("input_filter_slots", net.minecraft.nbt.CompoundTag.CODEC).orElseGet(net.minecraft.nbt.CompoundTag::new));
        this.fuelFilterSlots.deserializeNBT(input.lookup(), input.read("fuel_filter_slots", net.minecraft.nbt.CompoundTag.CODEC).orElseGet(net.minecraft.nbt.CompoundTag::new));
        this.inputStorageSlots.deserializeNBT(input.lookup(), input.read("input_storage_slots", net.minecraft.nbt.CompoundTag.CODEC).orElseGet(net.minecraft.nbt.CompoundTag::new));
        this.outputStorageSlots.deserializeNBT(input.lookup(), input.read("output_storage_slots", net.minecraft.nbt.CompoundTag.CODEC).orElseGet(net.minecraft.nbt.CompoundTag::new));
        this.fuelStorageSlots.deserializeNBT(input.lookup(), input.read("fuel_storage_slots", net.minecraft.nbt.CompoundTag.CODEC).orElseGet(net.minecraft.nbt.CompoundTag::new));
        this.fuelReturnSlots.deserializeNBT(input.lookup(), input.read("fuel_return_slots", net.minecraft.nbt.CompoundTag.CODEC).orElseGet(net.minecraft.nbt.CompoundTag::new));

        this.litTime = normalizeIntList(input.getIntArray("lit_time").orElseGet(() -> new int[0]), capacity, 0);
        this.litDuration = normalizeIntList(input.getIntArray("lit_duration").orElseGet(() -> new int[0]), capacity, 0);
        this.cookTime = normalizeIntList(input.getIntArray("cook_time").orElseGet(() -> new int[0]), capacity, 0);
        this.cookTimeTotal = normalizeIntList(input.getIntArray("cook_time_total").orElseGet(() -> new int[0]), capacity, 0);

        this.popMode = parseEnum(input.getStringOr("pop_mode", PopMode.STOP.name()), PopMode.class, PopMode.STOP);
        this.receiveMode = parseEnum(input.getStringOr("receive_mode", ReceiveMode.STOP.name()), ReceiveMode.class, ReceiveMode.STOP);
        this.sortMode = parseEnum(input.getStringOr("sort_mode", AutoSortMode.STOP.name()), AutoSortMode.class, AutoSortMode.STOP);
        double loadedExperience = input.getDoubleOr("stored_experience", 0.0D);
        this.storedExperience = Double.isFinite(loadedExperience) && loadedExperience > 0.0D ? loadedExperience : 0.0D;
    }

    @Override
    protected void saveAdditional(@NotNull ValueOutput output)
    {
        super.saveAdditional(output);
        output.store("input_filter_slots", net.minecraft.nbt.CompoundTag.CODEC, this.inputFilterSlots.serializeNBT(lookupProvider()));
        output.store("fuel_filter_slots", net.minecraft.nbt.CompoundTag.CODEC, this.fuelFilterSlots.serializeNBT(lookupProvider()));
        output.store("input_storage_slots", net.minecraft.nbt.CompoundTag.CODEC, this.inputStorageSlots.serializeNBT(lookupProvider()));
        output.store("output_storage_slots", net.minecraft.nbt.CompoundTag.CODEC, this.outputStorageSlots.serializeNBT(lookupProvider()));
        output.store("fuel_storage_slots", net.minecraft.nbt.CompoundTag.CODEC, this.fuelStorageSlots.serializeNBT(lookupProvider()));
        output.store("fuel_return_slots", net.minecraft.nbt.CompoundTag.CODEC, this.fuelReturnSlots.serializeNBT(lookupProvider()));
        output.putIntArray("lit_time", toIntArray(litTime));
        output.putIntArray("lit_duration", toIntArray(litDuration));
        output.putIntArray("cook_time", toIntArray(cookTime));
        output.putIntArray("cook_time_total", toIntArray(cookTimeTotal));
        output.putString("pop_mode", this.popMode.name());
        output.putString("receive_mode", this.receiveMode.name());
        output.putString("sort_mode", this.sortMode.name());
        output.putDouble("stored_experience", this.storedExperience);
    }

    private static int[] toIntArray(List<Integer> values)
    {
        return values.stream().mapToInt(Integer::intValue).toArray();
    }

    private static List<Integer> normalizeIntList(int[] source, int targetSize, int fillValue)
    {
        ArrayList<Integer> result = new ArrayList<>(targetSize);
        for (int i = 0; i < targetSize; i++)
        {
            result.add(i < source.length ? source[i] : fillValue);
        }
        return result;
    }

    private static <E extends Enum<E>> E parseEnum(String name, Class<E> enumClass, E fallback)
    {
        try
        {
            return Enum.valueOf(enumClass, name);
        }
        catch (IllegalArgumentException ex)
        {
            return fallback;
        }
    }

    public void setLit(boolean lit)
    {
        if (level == null || level.isClientSide()) return;

        BlockState state = this.getBlockState();
        if (state.getValue(BaseNetFurnaceBlock.LIT) != lit)
        {
            level.setBlock(
                    worldPosition,
                    state.setValue(BaseNetFurnaceBlock.LIT, lit),
                    Block.UPDATE_CLIENTS
            );
            setChanged(level, worldPosition, state);
        }
    }


    @Override
    public @NotNull Component getDisplayName()
    {
        return displayName;
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player)
    {
        awardStoredExperience(player);
        return new NetFurnaceMenu(containerId, inventory, this);
    }

}
