package com.wintercogs.beyonddimensions.common.block.entity;

import com.wintercogs.beyonddimensions.api.dimensionnet.DimensionsNet;
import com.wintercogs.beyonddimensions.api.dimensionnet.UnifiedStorage;
import com.wintercogs.beyonddimensions.api.machine.MachineType;
import com.wintercogs.beyonddimensions.api.storage.handler.impl.StackHandler;
import com.wintercogs.beyonddimensions.api.storage.key.IStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.KeyAmount;
import com.wintercogs.beyonddimensions.common.block.NetStockEmitterBlock;
import com.wintercogs.beyonddimensions.common.init.BDBlockEntities;
import com.wintercogs.beyonddimensions.common.init.BDMachineSettings;
import com.wintercogs.beyonddimensions.common.init.BDMachineTypes;
import com.wintercogs.beyonddimensions.common.machine.EmitMode;
import com.wintercogs.beyonddimensions.common.machine.FuzzyMode;
import com.wintercogs.beyonddimensions.common.menu.NetStockEmitterMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 存量发信器：比较网络里标记资源的数量与设定值，满足发信条件时让方块输出红石信号
 */
public class NetStockEmitterBlockEntity extends BaseMachineBlockEntity implements MenuProvider
{
    private static final String MARK = "mark";
    private static final String THRESHOLD = "threshold";

    // 要监测的资源，只由界面控制
    private final StackHandler markSlot = new StackHandler(1)
    {
        @Override
        public void onChange()
        {
            if (level != null && !level.isClientSide())
                level.blockEntityChanged(worldPosition);
        }
    };

    // 与网络里的数量比较的设定值
    private long threshold = 1;

    public NetStockEmitterBlockEntity(BlockPos pos, BlockState blockState)
    {
        super(BDBlockEntities.NET_STOCK_EMITTER_BLOCK_ENTITY.get(), pos, blockState);
    }

    @Override
    public MachineType machineType()
    {
        return BDMachineTypes.NET_STOCK_EMITTER.get();
    }

    public StackHandler getMarkSlot()
    {
        return markSlot;
    }

    public long getThreshold()
    {
        return threshold;
    }

    public void setThreshold(long threshold)
    {
        long next = Math.max(0, threshold);
        if (this.threshold == next)
            return;
        this.threshold = next;
        setChanged();
    }

    /**
     * 标记的资源在网络里的数量，没有网络或没有标记时为 0。模糊匹配时把同种的各个变体都算上
     */
    public long amount()
    {
        DimensionsNet net = getNet();
        IStackKey<?> key = markSlot.getStackBySlot(0).key();
        if (net == null || key.isEmpty())
            return 0;
        UnifiedStorage storage = net.getUnifiedStorage();
        if (setting(BDMachineSettings.FUZZY.get()) != FuzzyMode.ENABLE)
            return storage.getStackByKey(key).amount();
        long total = 0;
        for (KeyAmount variant : storage.getFuzzyMatches(key))
        {
            total += variant.amount();
            if (total < 0)
                return Long.MAX_VALUE;
        }
        return total;
    }

    /**
     * 此刻是否满足发信条件；没有网络或没有标记时不发信
     */
    public boolean shouldEmit()
    {
        if (getNet() == null || markSlot.getStackBySlot(0).key().isEmpty())
            return false;
        long amount = amount();
        return switch (setting(BDMachineSettings.EMIT.get()))
        {
            case AT_LEAST -> amount >= threshold;
            case BELOW -> amount < threshold;
        };
    }

    @Override
    public int getTicksPerWork()
    {
        return 5;
    }

    @Override
    public void workContent()
    {
        super.workContent();
        if (level == null)
            return;
        boolean emit = shouldEmit();
        BlockState state = getBlockState();
        if (state.getValue(NetStockEmitterBlock.POWERED) != emit)
            level.setBlock(worldPosition, state.setValue(NetStockEmitterBlock.POWERED, emit), Block.UPDATE_ALL);
    }

    /**
     * 配置卡连同标记与设定值一起复制
     */
    @Override
    public void exportConfig(CompoundTag config, HolderLookup.Provider registries)
    {
        super.exportConfig(config, registries);
        config.put(MARK, markSlot.serializeNBT(registries));
        config.putLong(THRESHOLD, threshold);
    }

    @Override
    public void applyConfig(CompoundTag config, HolderLookup.Provider registries)
    {
        super.applyConfig(config, registries);
        if (config.contains(MARK, Tag.TAG_COMPOUND))
            markSlot.deserializeNBT(registries, config.getCompound(MARK));
        if (config.contains(THRESHOLD, Tag.TAG_LONG))
            threshold = Math.max(0, config.getLong(THRESHOLD));
        setChanged();
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries)
    {
        super.loadAdditional(tag, registries);
        markSlot.deserializeNBT(registries, tag.getCompound(MARK));
        if (tag.contains(THRESHOLD, Tag.TAG_LONG))
            threshold = Math.max(0, tag.getLong(THRESHOLD));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries)
    {
        super.saveAdditional(tag, registries);
        tag.put(MARK, markSlot.serializeNBT(registries));
        tag.putLong(THRESHOLD, threshold);
    }

    @Override
    public @NotNull Component getDisplayName()
    {
        return Component.translatable("menu.title.beyonddimensions.stock_emitter_menu");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, @NotNull Inventory inventory, @NotNull Player player)
    {
        return new NetStockEmitterMenu(containerId, inventory, markSlot, this);
    }
}
