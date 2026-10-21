package com.wintercogs.beyonddimensions.common.init;

import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import com.wintercogs.beyonddimensions.common.menu.PlayerCraftingGrid;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 挂在玩家身上的数据。1.20.1 没有数据附件，用实体能力保存，重生与从末地返回时复制
 */
@Mod.EventBusSubscriber(modid = BDConstants.MODID)
public class BDAttachments
{
    // 玩家的合成格，死亡后随玩家保留
    public static final Capability<PlayerCraftingGrid> CRAFTING_GRID = CapabilityManager.get(new CapabilityToken<>()
    {
    });

    private static final ResourceLocation CRAFTING_GRID_ID = new ResourceLocation(BDConstants.MODID, "crafting_grid");

    public static void register(IEventBus eventBus)
    {
        eventBus.addListener((RegisterCapabilitiesEvent event) -> event.register(PlayerCraftingGrid.class));
    }

    @SubscribeEvent
    public static void attach(AttachCapabilitiesEvent<Entity> event)
    {
        if (event.getObject() instanceof Player)
            event.addCapability(CRAFTING_GRID_ID, new CraftingGridProvider());
    }

    @SubscribeEvent
    public static void copy(PlayerEvent.Clone event)
    {
        Player original = event.getOriginal();
        original.reviveCaps();
        original.getCapability(CRAFTING_GRID).ifPresent(old ->
                event.getEntity().getCapability(CRAFTING_GRID).ifPresent(grid -> grid.deserializeNBT(old.serializeNBT())));
        original.invalidateCaps();
    }

    private static final class CraftingGridProvider implements ICapabilitySerializable<CompoundTag>
    {
        private final PlayerCraftingGrid grid = new PlayerCraftingGrid();
        private final LazyOptional<PlayerCraftingGrid> optional = LazyOptional.of(() -> grid);

        @Override
        public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side)
        {
            return CRAFTING_GRID.orEmpty(cap, optional);
        }

        @Override
        public CompoundTag serializeNBT()
        {
            return grid.serializeNBT();
        }

        @Override
        public void deserializeNBT(CompoundTag tag)
        {
            grid.deserializeNBT(tag);
        }
    }
}
