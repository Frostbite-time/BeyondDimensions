package com.wintercogs.beyonddimensions.Integration.Botania;

import com.wintercogs.beyonddimensions.Api.DataBase.Handler.ManaStackTypedHandler;
import com.wintercogs.beyonddimensions.Api.DataBase.Storage.ManaUnifiedStorageHandler;
import com.wintercogs.beyonddimensions.Api.Util.CapCtx;
import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.BlockEntity.Custom.NetInterfaceBlockEntity;
import com.wintercogs.beyonddimensions.BlockEntity.Custom.NetPathwayBlockEntity;
import com.wintercogs.beyonddimensions.Menu.Slot.ItemCapInteractionBlackList;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import vazkii.botania.api.BotaniaForgeCapabilities;
import vazkii.botania.api.mana.spark.SparkAttachable;
import vazkii.botania.common.item.BotaniaItems;

import javax.annotation.Nullable;

public class BD_BotaniaPlugin
{

    public static void registerItemCapBlackList()
    {
        ItemCapInteractionBlackList.addToBlackList(BotaniaItems.manaMirror);
    }

    public static void attachBlockEntityCaps(AttachCapabilitiesEvent<BlockEntity> e)
    {
        BlockEntity be = e.getObject();

        if (be instanceof NetPathwayBlockEntity cBe)
        {
            class Provider implements ICapabilityProvider
            {
                private LazyOptional<SparkAttachable> opt = LazyOptional.empty();

                // 固定的监听实例
                private final Runnable onNetChanged = this::invalidate;
                private boolean listenerRegistered = false;

                @Override
                public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side)
                {
                    if (cap != BotaniaForgeCapabilities.SPARK_ATTACHABLE) return LazyOptional.empty();

                    if (!listenerRegistered)
                    {
                        cBe.addNetChangeTask(onNetChanged);
                        listenerRegistered = true;
                    }

                    if (opt.isPresent())
                    {
                        return BotaniaForgeCapabilities.SPARK_ATTACHABLE.orEmpty(cap, opt);
                    }

                    Level lvl = be.getLevel();
                    var net = cBe.getNet();
                    if (lvl == null || net == null)
                    {
                        return LazyOptional.empty();
                    }

                    opt = LazyOptional.of(() -> new ManaUnifiedStorageHandler(net.getUnifiedStorage(), new CapCtx(lvl, be.getBlockPos(), be)));
                    return BotaniaForgeCapabilities.SPARK_ATTACHABLE.orEmpty(cap, opt);
                }

                void invalidate()
                {
                    if (opt.isPresent()) opt.invalidate();
                    opt = LazyOptional.empty(); // 允许后续重建
                }
            }

            var prov = new Provider();
            e.addCapability(ResourceLocation.tryBuild(BeyondDimensions.MODID, "mana"), prov);
            e.addListener(prov::invalidate); // 方块实体失效/卸载
        }

        if (be instanceof NetInterfaceBlockEntity cIBe)
        {
            class Provider implements ICapabilityProvider
            {
                private LazyOptional<SparkAttachable> opt = LazyOptional.empty();

                private SparkAttachable create()
                {
                    var ctx = new CapCtx(be.getLevel(), be.getBlockPos(), be);
                    return new ManaStackTypedHandler(cIBe.getStackHandler(), ctx);
                }

                @Override
                public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side)
                {
                    if (cap == BotaniaForgeCapabilities.SPARK_ATTACHABLE)
                    {
                        if (!opt.isPresent()) opt = LazyOptional.of(this::create);
                        return BotaniaForgeCapabilities.SPARK_ATTACHABLE.orEmpty(cap, opt);
                    }
                    return LazyOptional.empty();
                }

                void invalidate()
                {
                    if (opt.isPresent()) opt.invalidate();
                    opt = LazyOptional.empty();
                }
            }

            var prov = new Provider();
            e.addCapability(ResourceLocation.tryBuild(BeyondDimensions.MODID, "mana"), prov);
            e.addListener(prov::invalidate);
        }
    }
}
