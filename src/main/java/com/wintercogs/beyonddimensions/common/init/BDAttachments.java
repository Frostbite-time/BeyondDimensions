package com.wintercogs.beyonddimensions.common.init;

import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import com.wintercogs.beyonddimensions.api.menu.workbench.PlayerWorkbenches;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.*;
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
    // 玩家留在工作台里的物品与工作台的偏好，死亡后随玩家保留。早先只有合成格，存档里沿用这个 ID
    public static final Capability<PlayerWorkbenches> WORKBENCHES = CapabilityManager.get(new CapabilityToken<>()
    {
    });

    private static final ResourceLocation WORKBENCHES_ID = new ResourceLocation(BDConstants.MODID, "crafting_grid");

    public static void register(IEventBus eventBus)
    {
        eventBus.addListener((RegisterCapabilitiesEvent event) -> event.register(PlayerWorkbenches.class));
    }

    @SubscribeEvent
    public static void attach(AttachCapabilitiesEvent<Entity> event)
    {
        if (event.getObject() instanceof Player)
            event.addCapability(WORKBENCHES_ID, new WorkbenchesProvider());
    }

    @SubscribeEvent
    public static void copy(PlayerEvent.Clone event)
    {
        Player original = event.getOriginal();
        original.reviveCaps();
        original.getCapability(WORKBENCHES).ifPresent(old ->
                event.getEntity().getCapability(WORKBENCHES).ifPresent(workbenches -> workbenches.deserializeNBT(old.serializeNBT())));
        original.invalidateCaps();
    }

    private static final class WorkbenchesProvider implements ICapabilitySerializable<CompoundTag>
    {
        private final PlayerWorkbenches workbenches = new PlayerWorkbenches();
        private final LazyOptional<PlayerWorkbenches> optional = LazyOptional.of(() -> workbenches);

        @Override
        public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side)
        {
            return WORKBENCHES.orEmpty(cap, optional);
        }

        @Override
        public CompoundTag serializeNBT()
        {
            return workbenches.serializeNBT();
        }

        @Override
        public void deserializeNBT(CompoundTag tag)
        {
            workbenches.deserializeNBT(tag);
        }
    }
}
