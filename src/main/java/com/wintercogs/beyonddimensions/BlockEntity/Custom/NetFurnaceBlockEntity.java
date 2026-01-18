package com.wintercogs.beyonddimensions.BlockEntity.Custom;

import com.wintercogs.beyonddimensions.Api.DataBase.Handler.ItemStackTypedHandler;
import com.wintercogs.beyonddimensions.Api.DataBase.Handler.StackTypedHandler;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.EnergyStackType;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.FluidStackType;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.IStackType;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.ItemStackType;
import com.wintercogs.beyonddimensions.Api.DataBase.Storage.UnifiedStorage;
import com.wintercogs.beyonddimensions.Api.Util.CombinedItemHandlerWrapper;
import com.wintercogs.beyonddimensions.Block.Custom.NetFurnaceBlock;
import com.wintercogs.beyonddimensions.BlockEntity.ModBlockEntities;
import com.wintercogs.beyonddimensions.Item.Custom.MatterCompressionBall;
import com.wintercogs.beyonddimensions.Item.ModItems;
import com.wintercogs.beyonddimensions.Machine.AutoSortMode;
import com.wintercogs.beyonddimensions.Machine.PopMode;
import com.wintercogs.beyonddimensions.Machine.ReceiveMode;
import com.wintercogs.beyonddimensions.Menu.NetFurnaceMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class NetFurnaceBlockEntity extends BaseMachineBlockEntity implements MenuProvider
{
    private LazyOptional<IItemHandler> opt = LazyOptional.empty();

    private static final int capacity = 9; // 同时处理的任务格数

    public int getCapacity()
    {
        return capacity;
    }

    private static final int filterCapacity = 8; // 同时能用的标记格数

    public int getFilterCapacity()
    {
        return filterCapacity;
    }

    private static final int fuelCapacity = 1; // 燃料槽个数

    public int getFuelCapacity()
    {
        return fuelCapacity;
    }

    public PopMode popMode = PopMode.STOP;// 是否弹出输出物
    public ReceiveMode receiveMode = ReceiveMode.STOP; // 是否将输出物送回网络
    public AutoSortMode sortMode = AutoSortMode.STOP; // 自动整理内容物
    private int sortCursor = 0; //用来记录当前tick整理到第几个槽位，以将自动整理的处理量平摊到n个tick中

    private List<RecipeManager.CachedCheck<Container, SmeltingRecipe>> quickChecks = new ArrayList<>(Collections.nCopies(capacity, RecipeManager.createCheck(RecipeType.SMELTING)));

    private List<Integer> litTime = new ArrayList<>(Collections.nCopies(capacity, 0)); // 槽位剩余燃烧 tick

    public List<Integer> getLitTime()
    {
        return litTime;
    }

    public void setLitTime(List<Integer> litTime)
    {
        this.litTime = litTime;
    }

    private List<Integer> litDuration = new ArrayList<>(Collections.nCopies(capacity, 0)); // 槽位燃料总 tick

    public List<Integer> getLitDuration()
    {
        return litDuration;
    }

    public void setLitDuration(List<Integer> litDuration)
    {
        this.litDuration = litDuration;
    }

    private List<Integer> cookTime = new ArrayList<>(Collections.nCopies(capacity, 0)); // 槽位为此次配方燃烧的 tick

    public List<Integer> getCookTime()
    {
        return cookTime;
    }

    public void setCookTime(List<Integer> cookTime)
    {
        this.cookTime = cookTime;
    }

    private List<Integer> cookTimeTotal = new ArrayList<>(Collections.nCopies(capacity, 0)); // 槽位配方所需 tick

    public List<Integer> getCookTimeTotal()
    {
        return cookTimeTotal;
    }

    public void setCookTimeTotal(List<Integer> cookTimeTotal)
    {
        this.cookTimeTotal = cookTimeTotal;
    }

    // 输入标记
    private final StackTypedHandler inputFilterSlots = new StackTypedHandler(filterCapacity)
    {
        @Override
        public void onChange()
        {
            if (!level.isClientSide())
                level.blockEntityChanged(worldPosition);
        }

        @Override
        public boolean isStackValid(int slot, IStackType stack)
        {
            // 仅接收可以熔炼的物品
            return stack instanceof ItemStackType itemInput && quickChecks.get(slot).getRecipeFor(new SimpleContainer(itemInput.copyStack()), level).isPresent();
        }
    };

    public StackTypedHandler getInputFilterSlots()
    {
        return inputFilterSlots;
    }

    // 燃料标记
    private final StackTypedHandler fuelFilterSlots = new StackTypedHandler(filterCapacity)
    {
        @Override
        public void onChange()
        {
            if (!level.isClientSide())
                level.blockEntityChanged(worldPosition);
        }

        @Override
        public boolean isStackValid(int slot, IStackType stack)
        {
            // 能量或者可以燃烧的物品能作为燃料标记
            return (stack instanceof EnergyStackType)
                    || (stack instanceof FluidStackType fluidStack && fluidStack.copyStack().getFluid() == Fluids.LAVA)
                    || (stack instanceof ItemStackType itemFuel && ForgeHooks.getBurnTime(itemFuel.getStack(), RecipeType.SMELTING) > 0);
        }

    };

    public StackTypedHandler getFuelFilterSlots()
    {
        return fuelFilterSlots;
    }

    // 输入存储
    private final StackTypedHandler inputStorageSlots = new StackTypedHandler(capacity)
    {
        @Override
        public void onChange()
        {
            if (!level.isClientSide())
                level.blockEntityChanged(worldPosition);
        }

        // 熔炉的特性，只能输入物品
        @Override
        public boolean isStackValid(int slot, IStackType stack)
        {
            // 仅接收可以熔炼的物品
            return stack instanceof ItemStackType itemInput && quickChecks.get(slot).getRecipeFor(new SimpleContainer(itemInput.copyStack()), level).isPresent();
        }
    };

    public StackTypedHandler getInputStorageSlots()
    {
        return inputStorageSlots;
    }

    // 输出存储
    private final StackTypedHandler outputStorageSlots = new StackTypedHandler(capacity)
    {
        @Override
        public void onChange()
        {
            if (!level.isClientSide())
                level.blockEntityChanged(worldPosition);
        }

    };

    public StackTypedHandler getOutputStorageSlots()
    {
        return outputStorageSlots;
    }

    // 燃料存储
    private final StackTypedHandler fuelStorageSlots = new StackTypedHandler(fuelCapacity)
    {
        @Override
        public void onChange()
        {
            if (!level.isClientSide())
                level.blockEntityChanged(worldPosition);
        }

        @Override
        public boolean isStackValid(int slot, IStackType stack)
        {
            // 能量或者可以燃烧的物品能作为燃料标记
            return (stack instanceof EnergyStackType)
                    || (stack instanceof FluidStackType fluidStack && fluidStack.copyStack().getFluid() == Fluids.LAVA)
                    || (stack instanceof ItemStackType itemFuel && ForgeHooks.getBurnTime(itemFuel.getStack(), RecipeType.SMELTING) > 0);
        }
    };

    public StackTypedHandler getFuelStorageSlots()
    {
        return fuelStorageSlots;
    }

    // 燃料返回物存储
    private final StackTypedHandler fuelReturnSlots = new StackTypedHandler(fuelCapacity)
    {
        @Override
        public void onChange()
        {
            if (!level.isClientSide())
                level.blockEntityChanged(worldPosition);
        }
    };

    public StackTypedHandler getFuelReturnSlots()
    {
        return fuelReturnSlots;
    }

    public NetFurnaceBlockEntity(BlockPos pos, BlockState blockState)
    {
        super(ModBlockEntities.NET_FURNACE_BLOCK_ENTITY.get(), pos, blockState);
    }

    //--- 能力注册 (通过事件) ---

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side)
    {
        if (cap != ForgeCapabilities.ITEM_HANDLER)
            return super.getCapability(cap, side);

        // 遍历注册的能力映射表
        //首先对所有实体槽位进行包装
        ItemStackTypedHandler inputStorage = new ItemStackTypedHandler(inputStorageSlots)
        {
            @Override
            public @NotNull ItemStack extractItem(int slot, int count, boolean sim)
            {
                return ItemStack.EMPTY; //禁止提取
            }
        };

        ItemStackTypedHandler fuelStorage = new ItemStackTypedHandler(fuelStorageSlots)
        {
            @Override
            public @NotNull ItemStack extractItem(int slot, int count, boolean sim)
            {
                return ItemStack.EMPTY; //禁止提取
            }
        };

        ItemStackTypedHandler outputStorage = new ItemStackTypedHandler(outputStorageSlots)
        {
            @Override
            public @NotNull ItemStack insertItem(int slot, ItemStack itemStack, boolean sim)
            {
                return itemStack; //禁止插入
            }
        };

        ItemStackTypedHandler fuelReturn = new ItemStackTypedHandler(fuelReturnSlots)
        {
            @Override
            public @NotNull ItemStack insertItem(int slot, ItemStack itemStack, boolean sim)
            {
                return itemStack; //禁止插入
            }
        };

        if (!opt.isPresent())
        {
            CombinedItemHandlerWrapper finalHandler = new CombinedItemHandlerWrapper(new ItemStackTypedHandler[]{inputStorage, fuelStorage, outputStorage, fuelReturn});
            opt = LazyOptional.of(() -> finalHandler).cast();
        }
        return opt.cast();
    }

    @Override
    public void invalidateCaps()
    {
        super.invalidateCaps();
        opt.invalidate();
        opt = LazyOptional.empty();
    }

    @Override
    public int getTicksPerWork()
    {
        return 1;
    }

    @Override
    public boolean shouldWork()
    {
        // 无论是否工作，总是先降低燃料持续时间
        litTime.replaceAll(i -> Math.max(0, i - 1));
        // 更新方块状态
        if (litTime.stream().allMatch(t -> t <= 0))
        {
            setLit(false);
        }
        else
        {
            setLit(true);
        }

        level.blockEntityChanged(worldPosition); //熔炉所在的区块总是需要保存的（比起为每个熔炉都判断燃烧时间，显然让区块始终保存性能更好，毕竟设为需要保存只是一个布尔值设置）

        // 输入槽为空 并且 标记槽无物品，可以判为无工作意图
        // 再加上output和fuelreturn，可以正确执行弹出和收纳设置
        return super.shouldWork() &&
                (!inputStorageSlots.isEmpty() || !inputFilterSlots.isEmpty() || !outputStorageSlots.isEmpty() || !fuelReturnSlots.isEmpty() || !fuelStorageSlots.isEmpty() || !fuelFilterSlots.isEmpty());
    }

    @Override
    public void workStart()
    {
        super.workStart();

        if (getNet() != null)
        {
            UnifiedStorage storage = getNet().getUnifiedStorage();

            // 1.尝试按照标记槽位从网络抽取原料
            for (int inputSlot = 0; inputSlot < capacity; inputSlot++)
            {
                if (inputStorageSlots.getStackBySlot(inputSlot).isEmpty())
                {
                    for (IStackType filterStack : inputFilterSlots.getStorage())
                    {
                        if (!inputStorageSlots.getStackBySlot(inputSlot).isEmpty())
                            break; //如果已经插入过则直接跳过
                        if (filterStack instanceof ItemStackType filterItem && !filterItem.isEmpty())
                        {
                            IStackType extracted = storage.extract(filterItem.copyWithCount(filterItem.getVanillaMaxStackSize()), false);
                            IStackType remaining = inputStorageSlots.insert(inputSlot, extracted, false);
                            if (!remaining.isEmpty())
                            {
                                storage.insert(remaining, false);
                            }
                        }
                    }
                }
            }
            // 2.如果开启了自动整理，则每tick进行一次快速整理
            if (sortMode == AutoSortMode.OPEN)
            {
                IStackType[] stacks = new IStackType[capacity]; // 种类引用 每tick重新获取，无隐藏问题
                long[] amounts = new long[capacity]; //种类数量

                java.util.Map<IStackType, java.util.List<Integer>> groupSlots = new java.util.HashMap<>(); // 所属槽位
                java.util.Map<IStackType, Long> groupTotal = new java.util.HashMap<>(); // 种类总数

                java.util.List<Integer> emptySlots = new java.util.ArrayList<>(); // 标记可用的空槽位

                for (int i = 0; i < capacity; i++)
                {
                    IStackType s = inputStorageSlots.getStackBySlot(i);
                    stacks[i] = s;

                    if (s == null || s.isEmpty())
                    {
                        emptySlots.add(i);
                        amounts[i] = 0;
                        continue;
                    }

                    long amt = s.getStackAmount();
                    amounts[i] = amt;

                    groupSlots.computeIfAbsent(s, k -> new java.util.ArrayList<>()).add(i);
                    groupTotal.put(s, groupTotal.getOrDefault(s, 0L) + amt);
                }
                // 为不同的种类再分配，循环次数小于种类数量，即小于capacity
                for (java.util.Map.Entry<IStackType, java.util.List<Integer>> entry : groupSlots.entrySet())
                {

                    IStackType type = entry.getKey();
                    java.util.List<Integer> typedSlots = entry.getValue();
                    long total = groupTotal.get(type);

                    // 目标槽数 k：现有槽 + 可用空槽，但不超过总量
                    int k = (int) Math.min(total, typedSlots.size() + emptySlots.size());

                    // 把需要的空槽“借”过来
                    while (typedSlots.size() < k && !emptySlots.isEmpty())
                    {
                        int idx = emptySlots.remove(emptySlots.size() - 1); // 取最后一个空槽
                        typedSlots.add(idx);
                        stacks[idx] = type; // 逻辑标记：该槽将容纳同类物品
                        amounts[idx] = 0;
                    }

                    // 计算平均值
                    long base = total / k; // 每个槽位的基本数量
                    int extra = (int) (total % k); // 前extra个槽位平摊余数

                    // 双指针搬运：把“多”的搬给“少”的
                    int surplusPtr = 0, deficitPtr = 0;
                    while (true)
                    { // 实际小于k次

                        // 找下一个盈余槽
                        while (surplusPtr < k)
                        {
                            int idx = typedSlots.get(surplusPtr);
                            long target = base + (surplusPtr < extra ? 1 : 0);
                            if (amounts[idx] > target) break;
                            surplusPtr++;
                        }

                        // 找下一个欠额槽
                        while (deficitPtr < k)
                        {
                            int idx = typedSlots.get(deficitPtr);
                            long target = base + (deficitPtr < extra ? 1 : 0);
                            if (amounts[idx] < target) break;
                            deficitPtr++;
                        }

                        if (surplusPtr >= k || deficitPtr >= k) break; // 已平衡

                        int from = typedSlots.get(surplusPtr);
                        int to = typedSlots.get(deficitPtr);

                        long surplus = amounts[from] - (base + (surplusPtr < extra ? 1 : 0)); // 盈余槽需要减少的
                        long deficit = (base + (deficitPtr < extra ? 1 : 0)) - amounts[to]; // 缺欠额槽需要增加的
                        long move = Math.min(surplus, deficit); // 实际搬运量

                        // 真正提取 & 插入
                        IStackType moved = inputStorageSlots.extract(from, move, false);
                        IStackType leftover = inputStorageSlots.insert(to, moved, false);
                        if (!leftover.isEmpty())
                        {
                            inputStorageSlots.insert(from, leftover, false);
                            break;
                        }

                        // 更新本地计数
                        amounts[from] -= move;
                        amounts[to] += move;
                    }
                }
            }
            // 3.尝试按燃料标记从网络抽取燃料 虽然当前燃料槽仅有一个，但是还是可以继续使用这个方法来方便后续修改
            for (int fuelSlot = 0; fuelSlot < fuelCapacity; fuelSlot++)
            {
                if (fuelStorageSlots.getStackBySlot(fuelSlot).isEmpty())
                {
                    for (IStackType filterStack : fuelFilterSlots.getStorage())
                    {
                        if (filterStack.isEmpty())
                            continue;

                        if (!fuelStorageSlots.getStackBySlot(fuelSlot).isEmpty())
                            break; //如果已经插入过则直接跳过

                        IStackType extracted = storage.extract(filterStack.copyWithCount(filterStack.getVanillaMaxStackSize()), false);
                        IStackType remaining = fuelStorageSlots.insert(fuelSlot, extracted, false);
                        if (!remaining.isEmpty())
                        {
                            storage.insert(remaining, false);
                        }
                    }
                }
            }
        }

        // 4.尝试将燃料分配到燃烧时间
        for (int litSlot = 0; litSlot < capacity; litSlot++)
        {
            // 燃料已经烧完，并且对应槽位仍然有需要冶炼的物品
            if (litTime.get(litSlot) <= 0 && !inputStorageSlots.getStackBySlot(litSlot).isEmpty())
            {
                for (IStackType fuelStack : fuelStorageSlots.getStorage())
                {
                    if (!fuelStack.isEmpty())
                    {
                        if (fuelStack instanceof EnergyStackType fuelEnergy)
                        {
                            // 每个fe对应1tick燃烧时间
                            int burnTime = (int) Math.min(fuelEnergy.getStackAmount(), 20000);
                            if (burnTime > 0)
                            {
                                fuelStorageSlots.extract(fuelEnergy.copyWithCount(burnTime), false);
                                litTime.set(litSlot, burnTime);
                                litDuration.set(litSlot, burnTime);
                            }
                        }
                        else if (fuelStack instanceof FluidStackType fuelFluid && fuelFluid.copyStack().getFluid() == Fluids.LAVA)
                        {
                            // 每mb熔岩对应20tick燃烧时间
                            int burnNum = (int) Math.min(fuelFluid.getStackAmount(), 1000);
                            int burnTime = burnNum * 20;
                            if (burnTime > 0)
                            {
                                fuelStorageSlots.extract(fuelFluid.copyWithCount(burnNum), false);
                                litTime.set(litSlot, burnTime);
                                litDuration.set(litSlot, burnTime);
                            }
                        }
                        else if (fuelStack instanceof ItemStackType fuelItem)
                        {
                            int burnTime = ForgeHooks.getBurnTime(fuelItem.getStack(), RecipeType.SMELTING);
                            if (burnTime > 0)
                            {
                                ItemStack returnItem = fuelItem.copyStack().getCraftingRemainingItem();
                                if (returnItem.isEmpty())
                                {
                                    fuelStorageSlots.extract(fuelItem.copyWithCount(1), false);
                                    litTime.set(litSlot, burnTime);
                                    litDuration.set(litSlot, burnTime);
                                }
                                else // 先尝试插入returnItem，如果能插入再消耗
                                {
                                    // 模拟插入陈功
                                    if (fuelReturnSlots.insert(new ItemStackType(returnItem), true).isEmpty())
                                    {
                                        fuelReturnSlots.insert(new ItemStackType(returnItem), false);
                                        fuelStorageSlots.extract(fuelItem.copyWithCount(1), false);
                                        litTime.set(litSlot, burnTime);
                                        litDuration.set(litSlot, burnTime);
                                    }
                                    else //无法补充燃料，则将双时间设为0
                                    {
                                        litTime.set(litSlot, 0);
                                        litDuration.set(litSlot, 0);
                                    }
                                }

                            }

                        }
                    }
                }
            }
        }
    }

    @Override
    public void workContent()
    {
        super.workContent();
        //开始熔炼
        for (int inputSlot = 0; inputSlot < capacity; inputSlot++)
        {
            if (litTime.get(inputSlot) <= 0)
                continue; // 必须有燃烧才能熔炼

            if (inputStorageSlots.getStackBySlot(inputSlot) instanceof ItemStackType inputItem
                    && !inputItem.isEmpty())
            {
                SmeltingRecipe recipeHolder = quickChecks.get(inputSlot)
                        .getRecipeFor(new SimpleContainer(inputItem.getStack()), level).orElse(null);
                if (recipeHolder != null)
                {
                    // 一旦找到配方，始终重设总时间，以防错误越过
                    cookTimeTotal.set(inputSlot, recipeHolder.getCookingTime());
                    // 熔炼时间正常，并且能正常输出
                    if (cookTime.get(inputSlot) >= cookTimeTotal.get(inputSlot))
                    {
                        ItemStackType resultItem = new ItemStackType(recipeHolder.getResultItem(level.registryAccess()));

                        // 如果能完全输出，则输出，并重设熔炼时间
                        if (outputStorageSlots.insert(inputSlot, resultItem, true).isEmpty())
                        {
                            outputStorageSlots.insert(inputSlot, resultItem, false);
                            inputStorageSlots.extract(inputSlot, 1, false);
                            cookTime.set(inputSlot, 0);
                            cookTimeTotal.set(inputSlot, recipeHolder.getCookingTime());
                        }
                    }
                    else // 存在recipe，且没有完全熔炼，减少熔炼时间 （与此同时，顺便重置总时间，以防万一）
                    {
                        cookTime.set(inputSlot, cookTime.get(inputSlot) + 1);
                    }
                }
                else
                {
                    // 如果不存在recipe，那么时间重设为0
                    cookTime.set(inputSlot, 0);
                    cookTimeTotal.set(inputSlot, 0);
                }
            }
            else
            {
                // 如果物品不合法，时间重设为0
                cookTime.set(inputSlot, 0);
                cookTimeTotal.set(inputSlot, 0);
            }
        }
    }

    @Override
    public void workEnd()
    {
        super.workEnd();
        // 应用转移模式与弹出模式的设置
        // 优先弹出，再转移

        ArrayList<IItemHandler> otherStroages = new ArrayList<>();
        if (popMode == PopMode.OPEN)
        {
            for (Direction dir : Direction.values())
            {
                BlockPos targetPos = this.getBlockPos().relative(dir);
                BlockEntity neighbor = level.getBlockEntity(targetPos);
                if (neighbor != null && !(neighbor instanceof NetedBlockEntity))
                {
                    LazyOptional<IItemHandler> otherStorage = neighbor.getCapability(ForgeCapabilities.ITEM_HANDLER, dir.getOpposite());
                    if (otherStorage.isPresent())
                    {
                        IItemHandler otherHandler = otherStorage.resolve().get();
                        if (otherStorage != null)
                        {
                            otherStroages.add(otherHandler);
                        }
                    }
                }
            }
        }

        // 输出槽处理
        for (int outputSlot = 0; outputSlot < capacity; outputSlot++)
        {
            IStackType outputStack = outputStorageSlots.getStackBySlot(outputSlot);
            if (outputStack != null && !outputStack.isEmpty())
            {
                // 弹出模式（如果弹出模式关闭，这里会由迭代器安全的离开）
                for (IItemHandler otherStorage : otherStroages)
                {
                    //getMaxTransfer会返回一个不大于int最大值的long类型数据，因此可以安全转换
                    for (int otherSlot = 0; otherSlot < otherStorage.getSlots(); otherSlot++)
                    {
                        IStackType extracted = outputStorageSlots.extract(outputSlot, outputStack.getVanillaMaxStackSize(), false);
                        int remaining = otherStorage.insertItem(otherSlot, (ItemStack) extracted.copyStack(), false).getCount();
                        if (remaining > 0)
                        {
                            outputStorageSlots.insert(outputSlot, extracted.copyWithCount(remaining), false);
                        }
                    }
                }

                // 转移至网络
                if (receiveMode == ReceiveMode.OPEN)
                {
                    if (getNet() != null)
                    {
                        UnifiedStorage storage = getNet().getUnifiedStorage();
                        IStackType extracted = outputStorageSlots.extract(outputSlot, outputStack.getStackAmount(), false);
                        IStackType remaining = storage.insert(outputSlot, extracted, false);
                        if (!remaining.isEmpty())
                        {
                            outputStorageSlots.insert(outputSlot, remaining, false);
                        }
                    }
                }
            }
        }

        // 燃料返回槽处理
        for (int returnSlot = 0; returnSlot < fuelCapacity; returnSlot++)
        {
            IStackType returnStack = fuelReturnSlots.getStackBySlot(returnSlot);
            if (returnStack != null && !returnStack.isEmpty())
            {
                // 弹出模式（如果弹出模式关闭，这里会由迭代器安全的离开）
                for (IItemHandler otherStorage : otherStroages)
                {
                    //getMaxTransfer会返回一个不大于int最大值的long类型数据，因此可以安全转换
                    for (int otherSlot = 0; otherSlot < otherStorage.getSlots(); otherSlot++)
                    {
                        IStackType extracted = fuelReturnSlots.extract(returnSlot, returnStack.getVanillaMaxStackSize(), false);
                        int remaining = otherStorage.insertItem(otherSlot, (ItemStack) extracted.copyStack(), false).getCount();
                        if (remaining > 0)
                        {
                            fuelReturnSlots.insert(returnSlot, extracted.copyWithCount(remaining), false);
                        }
                    }
                }

                // 转移至网络
                if (receiveMode == ReceiveMode.OPEN)
                {
                    if (getNet() != null)
                    {
                        UnifiedStorage storage = getNet().getUnifiedStorage();
                        IStackType extracted = fuelReturnSlots.extract(returnSlot, returnStack.getStackAmount(), false);
                        IStackType remaining = storage.insert(returnSlot, extracted, false);
                        if (!remaining.isEmpty())
                        {
                            fuelReturnSlots.insert(returnSlot, remaining, false);
                        }
                    }
                }
            }
        }

        // 燃料槽处理-如果开始接收模式，在不标记能量时，将能量或流体等不方便存取的堆叠收回网络
        // 这会防止能量堵塞在燃料口
        for (int fuelSlot = 0; fuelSlot < fuelCapacity; fuelSlot++)
        {
            IStackType fuelStack = fuelStorageSlots.getStackBySlot(fuelSlot);
            if (fuelStack != null && !fuelStack.isEmpty()
                    && (fuelStack instanceof EnergyStackType || fuelStack instanceof FluidStackType))
            {
                // 转移至网络
                if (receiveMode == ReceiveMode.OPEN)
                {
                    if (getNet() != null)
                    {
                        if (!fuelFilterSlots.hasStackType(fuelStack))
                        {
                            UnifiedStorage storage = getNet().getUnifiedStorage();
                            IStackType extracted = fuelStorageSlots.extract(fuelSlot, fuelStack.getStackAmount(), false);
                            IStackType remaining = storage.insert(fuelSlot, extracted, false);
                            if (!remaining.isEmpty())
                            {
                                fuelStorageSlots.insert(fuelSlot, remaining, false);
                            }
                        }
                    }
                }
            }
        }
    }

    public void dropContent()
    {
        List<IStackType<?>> dropList = new ArrayList<>();
        for (IStackType<?> stack : inputStorageSlots.getStorage())
        {
            if (!stack.isEmpty())
            {
                // 如果内含物质球，直接弹出，防止NBT套娃
                if (stack instanceof ItemStackType itemStackType)
                {
                    if (itemStackType.getStack().getItem() instanceof MatterCompressionBall)
                        Block.popResource(level, getBlockPos(), itemStackType.copyStack());
                    else
                        dropList.add(stack.copy());
                }
                else
                {
                    dropList.add(stack.copy());
                }
            }
        }
        for (IStackType stack : outputStorageSlots.getStorage())
        {
            if (!stack.isEmpty())
            {
                // 如果内含物质球，直接弹出，防止NBT套娃
                if (stack instanceof ItemStackType itemStackType)
                {
                    if (itemStackType.getStack().getItem() instanceof MatterCompressionBall)
                        Block.popResource(level, getBlockPos(), itemStackType.copyStack());
                    else
                        dropList.add(stack.copy());
                }
                else
                {
                    dropList.add(stack.copy());
                }
            }
        }
        for (IStackType stack : fuelStorageSlots.getStorage())
        {
            if (!stack.isEmpty())
            {
                // 如果内含物质球，直接弹出，防止NBT套娃
                if (stack instanceof ItemStackType itemStackType)
                {
                    if (itemStackType.getStack().getItem() instanceof MatterCompressionBall)
                        Block.popResource(level, getBlockPos(), itemStackType.copyStack());
                    else
                        dropList.add(stack.copy());
                }
                else
                {
                    dropList.add(stack.copy());
                }
            }
        }
        for (IStackType stack : fuelReturnSlots.getStorage())
        {
            if (!stack.isEmpty())
            {
                // 如果内含物质球，直接弹出，防止NBT套娃
                if (stack instanceof ItemStackType itemStackType)
                {
                    if (itemStackType.getStack().getItem() instanceof MatterCompressionBall)
                        Block.popResource(level, getBlockPos(), itemStackType.copyStack());
                    else
                        dropList.add(stack.copy());
                }
                else
                {
                    dropList.add(stack.copy());
                }
            }
        }
        ItemStack ball = new ItemStack(ModItems.MATTER_COMPRESS_BALL.get(), 1);
        if (!dropList.isEmpty())
        {
            MatterCompressionBall.setIStackList(ball, dropList);
            Block.popResource(level, getBlockPos(), ball);
        }
    }


    @Override
    public void load(CompoundTag tag)
    {
        super.load(tag);
        this.inputFilterSlots.deserializeNBT(tag.getCompound("input_filter_slots"));
        this.fuelFilterSlots.deserializeNBT(tag.getCompound("fuel_filter_slots"));
        this.inputStorageSlots.deserializeNBT(tag.getCompound("input_storage_slots"));
        this.outputStorageSlots.deserializeNBT(tag.getCompound("output_storage_slots"));
        this.fuelStorageSlots.deserializeNBT(tag.getCompound("fuel_storage_slots"));
        this.fuelReturnSlots.deserializeNBT(tag.getCompound("fuel_return_slots"));
        this.litTime = Arrays.stream(tag.getIntArray("lit_time")).boxed().collect(Collectors.toList());
        this.litDuration = Arrays.stream(tag.getIntArray("lit_duration")).boxed().collect(Collectors.toList());
        this.cookTime = Arrays.stream(tag.getIntArray("cook_time")).boxed().collect(Collectors.toList());
        this.cookTimeTotal = Arrays.stream(tag.getIntArray("cook_time_total")).boxed().collect(Collectors.toList());
        this.popMode = PopMode.valueOf(tag.getString("pop_mode"));
        this.receiveMode = ReceiveMode.valueOf(tag.getString("receive_mode"));
        this.sortMode = tag.contains("sort_mode") ? AutoSortMode.valueOf(tag.getString("sort_mode")) : AutoSortMode.STOP;
    }

    @Override
    protected void saveAdditional(CompoundTag tag)
    {
        super.saveAdditional(tag);
        tag.put("input_filter_slots", this.inputFilterSlots.serializeNBT());
        tag.put("fuel_filter_slots", this.fuelFilterSlots.serializeNBT());
        tag.put("input_storage_slots", this.inputStorageSlots.serializeNBT());
        tag.put("output_storage_slots", this.outputStorageSlots.serializeNBT());
        tag.put("fuel_storage_slots", this.fuelStorageSlots.serializeNBT());
        tag.put("fuel_return_slots", this.fuelReturnSlots.serializeNBT());
        tag.putIntArray("lit_time", litTime);
        tag.putIntArray("lit_duration", litDuration);
        tag.putIntArray("cook_time", cookTime);
        tag.putIntArray("cook_time_total", cookTimeTotal);
        tag.putString("pop_mode", this.popMode.name());
        tag.putString("receive_mode", this.receiveMode.name());
        tag.putString("sort_mode", this.sortMode.name());
    }

    public void setLit(boolean lit)
    {
        if (level == null || level.isClientSide()) return;

        BlockState state = this.getBlockState();
        if (state.getValue(NetFurnaceBlock.LIT) != lit)
        {
            level.setBlock(
                    worldPosition,
                    state.setValue(NetFurnaceBlock.LIT, lit),
                    Block.UPDATE_CLIENTS        // 仅通知客户端 + 保存到区块
            );
            // 如果方块附带其他 NBT 数据，也别忘了：
            setChanged(level, worldPosition, state);
        }
    }

    @Override
    public Component getDisplayName()
    {
        return Component.translatable("menu.title.beyonddimensions.furnace_menu");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player)
    {
        return new NetFurnaceMenu(containerId, inventory, this);
    }

}
