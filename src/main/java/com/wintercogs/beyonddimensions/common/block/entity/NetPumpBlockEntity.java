package com.wintercogs.beyonddimensions.common.block.entity;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.wintercogs.beyonddimensions.api.capability.helper.CapabilityHelper;
import com.wintercogs.beyonddimensions.api.capability.helper.wrapper.IStackHandlerWrapper;
import com.wintercogs.beyonddimensions.api.capability.helper.wrapper.StackHandlerWrapperHelper;
import com.wintercogs.beyonddimensions.api.dimensionnet.DimensionsNet;
import com.wintercogs.beyonddimensions.api.dimensionnet.UnifiedStorage;
import com.wintercogs.beyonddimensions.api.machine.MachineType;
import com.wintercogs.beyonddimensions.api.storage.handler.impl.StackHandler;
import com.wintercogs.beyonddimensions.api.storage.key.IStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.KeyAmount;
import com.wintercogs.beyonddimensions.api.storage.key.StackKeyRegistry;
import com.wintercogs.beyonddimensions.common.init.BDBlockEntities;
import com.wintercogs.beyonddimensions.common.init.BDMachineSettings;
import com.wintercogs.beyonddimensions.common.init.BDMachineTypes;
import com.wintercogs.beyonddimensions.common.machine.MachineSides;
import com.wintercogs.beyonddimensions.common.machine.setting.FilterMode;
import com.wintercogs.beyonddimensions.common.machine.setting.FuzzyMode;
import com.wintercogs.beyonddimensions.common.menu.NetPumpMenu;
import com.wintercogs.beyonddimensions.util.ResourceTransfer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public class NetPumpBlockEntity extends BaseMachineBlockEntity implements MenuProvider
{
    // 存储相邻方块的能力
    // 按照 typedId -> 堆叠处理器 的结构存储，使用Multimap，因为一个typedId可以对应多个处理器
    private final Multimap<ResourceLocation, Object> handlerCache = ArrayListMultimap.create();
    private boolean needsCapabilityUpdate = true;
    private final Direction[] directions = Direction.values();

    private static final int capacity = 36;
    private final StackHandler filterSlots = new StackHandler(capacity)
    {
        @Override
        public void onChange()
        {
            if (level != null && !level.isClientSide())
                level.blockEntityChanged(worldPosition);
        }
    };

    private static final String FILTERS = "filters";

    @Override
    public MachineType machineType()
    {
        return BDMachineTypes.NET_PUMP.get();
    }

    public FilterMode filterMode()
    {
        return setting(BDMachineSettings.FILTER.get());
    }

    /**
     * 配置卡连同过滤槽一起复制
     */
    @Override
    public void exportConfig(CompoundTag config, HolderLookup.Provider registries)
    {
        super.exportConfig(config, registries);
        config.put(FILTERS, filterSlots.serializeNBT(registries));
    }

    @Override
    public void applyConfig(CompoundTag config, HolderLookup.Provider registries)
    {
        super.applyConfig(config, registries);
        if (config.contains(FILTERS, Tag.TAG_COMPOUND))
            filterSlots.deserializeNBT(registries, config.getCompound(FILTERS));
        setChanged();
    }

    public NetPumpBlockEntity(BlockPos pos, BlockState blockState)
    {
        super(BDBlockEntities.NET_PUMP_BLOCK_ENTITY.get(), pos, blockState);
    }

    @Override
    public boolean shouldWork()
    {
        return super.shouldWork() && getNet() != null;
    }

    @Override
    public int getTicksPerWork()
    {
        return 10;
    }

    @Override
    public void workStart()
    {
        if (level == null || !needsCapabilityUpdate) return;

        handlerCache.clear();

        for (Direction dir : directions)
        {
            // 只从输入的面抽取
            if (!MachineSides.mode(this, dir).allowsInput())
                continue;
            BlockPos targetPos = this.getBlockPos().relative(dir);
            BlockEntity neighbor = level.getBlockEntity(targetPos);
            if (neighbor != null && !(neighbor instanceof NetedBlockEntity))
            {

                CapabilityHelper.BlockCapabilityMap.forEach(
                        (resourceLocation, cap) -> {
                            Object handler = level.getCapability(cap, targetPos, dir.getOpposite());
                            if (handler != null)
                            {
                                handlerCache.put(resourceLocation, handler);
                            }
                        }
                );

            }
        }

        needsCapabilityUpdate = false;
    }

    @Override
    public void workContent()
    {
        handlerCache.forEach(
                (typeId, handler) -> {
                    Function handlerGetter = StackHandlerWrapperHelper.stackWrappers.get(typeId);

                    IStackHandlerWrapper stackHandlerWrapper = (IStackHandlerWrapper) handlerGetter.apply(handler);

                    for (int slot = 0; slot < stackHandlerWrapper.getSlots(); slot++)
                    {
                        Object stack = stackHandlerWrapper.getStackInSlot(slot);
                        IStackKey<?> typeKey = StackKeyRegistry.getType(typeId);
                        KeyAmount ka = typeKey.fromStackObject(stack);
                        if (ka != null && !ka.key().isEmpty() && matchesFilter(ka.key()))
                        {
                            DimensionsNet net = getNet();
                            if (net != null)
                            {
                                UnifiedStorage storage = net.getUnifiedStorage();
                                int index = slot;
                                // 先模拟对方能抽出多少、网络能收下多少，再只抽出收得下的数量
                                ResourceTransfer.move(ka.key(), ka.amount(),
                                        (key, n, simulate) -> new KeyAmount(key, stackHandlerWrapper.extract(index, n, simulate)),
                                        (key, n, simulate) -> new KeyAmount(key, n - stackHandlerWrapper.insert(index, new KeyAmount(key, n).toStack(), simulate)),
                                        offer -> offer.amount() - storage.insert(offer.key(), offer.amount(), true).amount(),
                                        taken -> storage.insert(taken.key(), taken.amount(), false).amount());
                            }
                        }
                    }
                }
        );
    }

    private boolean matchesFilter(IStackKey<?> otherStack)
    {
        boolean fuzzy = setting(BDMachineSettings.FILTER_FUZZY.get()) == FuzzyMode.ENABLE;
        return filterMode().passes(filterSlots.getStorage(), otherStack, fuzzy);
    }

    public void setNeedsCapabilityUpdate()
    {
        needsCapabilityUpdate = true;
    }

    @Override
    protected void onSettingsChanged()
    {
        super.onSettingsChanged();
        setNeedsCapabilityUpdate();
    }

    @Override
    public void invalidateCapabilities()
    {
        super.invalidateCapabilities();
        setNeedsCapabilityUpdate();
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries)
    {
        super.loadAdditional(tag, registries);
        filterSlots.deserializeNBT(registries, tag.getCompound("filter_slots"));
    }

    @Override
    public void onLoad()
    {
        super.onLoad();
        setNeedsCapabilityUpdate();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries)
    {
        super.saveAdditional(tag, registries);
        tag.put("filter_slots", filterSlots.serializeNBT(registries));
    }

    @Override
    public @NotNull Component getDisplayName()
    {
        return Component.translatable("menu.title.beyonddimensions.pump_menu");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, @NotNull Inventory inventory, @NotNull Player player)
    {
        return new NetPumpMenu(containerId, inventory, filterSlots, this);
    }
}
