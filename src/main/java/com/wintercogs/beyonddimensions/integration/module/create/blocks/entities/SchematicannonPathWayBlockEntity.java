package com.wintercogs.beyonddimensions.integration.module.create.blocks.entities;

import com.simibubi.create.content.schematics.cannon.MaterialChecklist;
import com.simibubi.create.content.schematics.cannon.SchematicannonBlockEntity;
import com.wintercogs.beyonddimensions.api.dimensionnet.DimensionsNet;
import com.wintercogs.beyonddimensions.api.storage.key.KeyAmount;
import com.wintercogs.beyonddimensions.api.storage.key.impl.ItemStackKey;
import com.wintercogs.beyonddimensions.common.block.entity.NetedBlockEntity;
import com.wintercogs.beyonddimensions.common.init.BDBlockEntities;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class SchematicannonPathWayBlockEntity extends NetedBlockEntity
{
    /**
     * 描述了能力暴露细节
     */
    @Nullable NetedSchematicannonItemHandler schematicannonItemHandler = null;

    /**
     * 描述了应当暴露哪些物品
     */
    @NotNull List<ItemStack> allSchematicannonItems = new ArrayList<>();

    /**
     * 描述了可以进行暴露的方向
     */
    @NotNull Set<Direction> allowedDirections = new HashSet<>();

    /**
     * 记录MaterialChecklist的内部状态，我们只记录并比较required和damageRequired字段即可
     */
    @NotNull Map<Direction, MaterialChecklist> otherChecklists = new HashMap<>();

    public SchematicannonPathWayBlockEntity(BlockPos pos, BlockState blockState)
    {
        super(BDBlockEntities.SCHEMATICANNON_PATHWAY_BLOCK_ENTITY.get(), pos, blockState);
    }

    /**
     * 能力注册
     */
    public static void registerCapability(RegisterCapabilitiesEvent event)
    {
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                BDBlockEntities.SCHEMATICANNON_PATHWAY_BLOCK_ENTITY.get(),
                (be, direction) -> {
                    if (be instanceof SchematicannonPathWayBlockEntity schematicannonPathWayBlockEntity
                            && direction != null
                            && schematicannonPathWayBlockEntity.schematicannonItemHandler != null
                            && schematicannonPathWayBlockEntity.allowedDirections.contains(direction))
                    {
                        return schematicannonPathWayBlockEntity.schematicannonItemHandler;
                    }
                    return null;
                }
        );
    }

    /**
     * 每 tick 主动刷新：根据 checklist 快照是否变化决定是否重建能力
     */
    public void updateCap()
    {
        if (level == null || level.isClientSide()) return;

        DimensionsNet net = getNet();
        if (net == null)
        {
            clearCap();
            otherChecklists.clear();
            invalidateCapabilities();
            return;
        }

        boolean checklistsChanged = false;
        // 这一轮实际检测到有大炮的方向
        EnumSet<Direction> seenDirections = EnumSet.noneOf(Direction.class);

        // 扫描六向方块，查找 Create 的 SchematicannonBlockEntity
        for (Direction dir : Direction.values())
        {
            BlockPos otherPos = worldPosition.relative(dir);
            if (!level.isLoaded(otherPos)) continue;

            BlockEntity be = level.getBlockEntity(otherPos);
            if (be instanceof SchematicannonBlockEntity schematicannon)
            {
                seenDirections.add(dir);

                MaterialChecklist current = schematicannon.checklist;
                if (current == null) continue; // 理论上不会是 null

                MaterialChecklist cached = otherChecklists.get(dir);
                if (cached == null || !sameChecklistKeys(cached, current))
                {
                    // key 集合不同，用当前 checklist 的 key 创建一份快照
                    otherChecklists.put(dir, cloneChecklistKeys(current));
                    checklistsChanged = true;
                }
            }
        }

        // 把这轮没有再看到大炮的方向从缓存里移除
        Iterator<Map.Entry<Direction, MaterialChecklist>> it = otherChecklists.entrySet().iterator();
        while (it.hasNext())
        {
            Map.Entry<Direction, MaterialChecklist> entry = it.next();
            if (!seenDirections.contains(entry.getKey()))
            {
                it.remove();
                checklistsChanged = true;
            }
        }

        // 如果现在附近没有任何带 checklist 的大炮，能力清空
        if (otherChecklists.isEmpty())
        {
            if (schematicannonItemHandler != null
                    || !allSchematicannonItems.isEmpty()
                    || !allowedDirections.isEmpty())
            {
                clearCap();
                invalidateCapabilities();
            }
            return;
        }

        // 允许暴露的方向就是当前缓存里所有有大炮的方向
        Set<Direction> newAllowedDirections = new HashSet<>(otherChecklists.keySet());

        // 如果 checklist key 集合没变，并且暴露方向也没变，而且 handler 不为 null，直接早退
        if (!checklistsChanged
                && schematicannonItemHandler != null
                && allowedDirections.equals(newAllowedDirections))
        {
            return;
        }

        // 重新根据缓存的 checklist 快照构建物品快照
        List<ItemStack> newSnapshot = new ArrayList<>();
        Set<Item> seenItems = new HashSet<>();

        for (MaterialChecklist snapshot : otherChecklists.values())
        {
            Object2IntMap<Item> required = snapshot.required;
            Object2IntMap<Item> damageRequired = snapshot.damageRequired;

            for (Item item : required.keySet())
            {
                if (seenItems.add(item))
                {
                    newSnapshot.add(new ItemStack(item));
                }
            }
            for (Item item : damageRequired.keySet())
            {
                if (seenItems.add(item))
                {
                    newSnapshot.add(new ItemStack(item));
                }
            }
        }

        // 理论上 otherChecklists 非空时这里也至少有一个 item，不过稳妥起见还是兜一手
        if (newSnapshot.isEmpty() || newAllowedDirections.isEmpty())
        {
            clearCap();
            invalidateCapabilities();
            return;
        }

        boolean snapshotChanged = !allSchematicannonItems.equals(newSnapshot)
                || !allowedDirections.equals(newAllowedDirections)
                || schematicannonItemHandler == null;

        if (!snapshotChanged)
        {
            return;
        }

        allSchematicannonItems.clear();
        allSchematicannonItems.addAll(newSnapshot);

        allowedDirections.clear();
        allowedDirections.addAll(newAllowedDirections);

        schematicannonItemHandler = new NetedSchematicannonItemHandler(allSchematicannonItems, net);
        invalidateCapabilities();
    }

    public void clearCap()
    {
        schematicannonItemHandler = null;
        allSchematicannonItems.clear();
        allowedDirections.clear();
    }

    // 网络更变时立刻更新能力
    @Override
    public void setChanged()
    {
        super.setChanged();
        updateCap();
    }

    public static void tick(Level level, BlockPos pos, BlockState state, SchematicannonPathWayBlockEntity blockEntity)
    {
        if (level == null || level.isClientSide()) return;

        blockEntity.updateCap();
    }

    /**
     * 从一个 checklist 拷贝 required / damageRequired 的 key 集合 到一个新的 MaterialChecklist。
     */
    private static MaterialChecklist cloneChecklistKeys(MaterialChecklist source)
    {
        MaterialChecklist copy = new MaterialChecklist();
        for (Item item : source.required.keySet())
        {
            copy.required.put(item, 1);
        }
        for (Item item : source.damageRequired.keySet())
        {
            copy.damageRequired.put(item, 1);
        }
        copy.blocksNotLoaded = source.blocksNotLoaded;
        return copy;
    }

    /**
     * 只比较 required / damageRequired 的 key 集合是否相等
     */
    private static boolean sameChecklistKeys(MaterialChecklist a, MaterialChecklist b)
    {
        return a.required.keySet().equals(b.required.keySet())
                && a.damageRequired.keySet().equals(b.damageRequired.keySet());
    }

    /**
     * 描述了根据快照从网络中存取物品的类
     */
    public static class NetedSchematicannonItemHandler implements IItemHandler
    {
        @NotNull
        DimensionsNet net;

        @NotNull
        List<ItemStack> stacksSnapshot;

        public NetedSchematicannonItemHandler(@NotNull List<ItemStack> stacks, @NotNull DimensionsNet net)
        {
            this.stacksSnapshot = stacks;
            this.net = net;
        }

        @Override
        public int getSlots()
        {
            return stacksSnapshot.size();
        }

        @Override
        public @NotNull ItemStack getStackInSlot(int slot)
        {
            if (slot < 0 || slot >= stacksSnapshot.size()) return ItemStack.EMPTY;
            ItemStack snapStack = stacksSnapshot.get(slot);
            KeyAmount ka = net.getUnifiedStorage().getStackByKey(new ItemStackKey(snapStack));
            if (ka.isEmpty()) return ItemStack.EMPTY;
            if (!(ka.key() instanceof ItemStackKey itemKey)) return ItemStack.EMPTY;

            return itemKey.copyStackWithCount(ka.amount());
        }

        @Override
        public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack itemStack, boolean simulate)
        {
            KeyAmount remaining = net.getUnifiedStorage().insert(new ItemStackKey(itemStack), itemStack.getCount(), simulate);
            if (remaining.isEmpty()) return ItemStack.EMPTY;
            if (!(remaining.key() instanceof ItemStackKey itemKey)) return ItemStack.EMPTY;

            return itemKey.copyStackWithCount(remaining.amount());
        }

        @Override
        public @NotNull ItemStack extractItem(int slot, int count, boolean simulate)
        {
            if (slot < 0 || slot >= stacksSnapshot.size() || count <= 0) return ItemStack.EMPTY;

            KeyAmount extracted = net.getUnifiedStorage().extract(new ItemStackKey(stacksSnapshot.get(slot)), count, simulate, false);
            if (extracted.isEmpty()) return ItemStack.EMPTY;
            if (!(extracted.key() instanceof ItemStackKey itemKey)) return ItemStack.EMPTY;

            return itemKey.copyStackWithCount(extracted.amount());
        }

        @Override
        public int getSlotLimit(int slot)
        {
            return 99;
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack itemStack)
        {
            return true;
        }
    }
}
