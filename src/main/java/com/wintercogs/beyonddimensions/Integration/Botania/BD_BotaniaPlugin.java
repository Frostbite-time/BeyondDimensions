package com.wintercogs.beyonddimensions.Integration.Botania;

import com.wintercogs.beyonddimensions.Api.DataBase.Handler.ManaStackTypedHandler;
import com.wintercogs.beyonddimensions.Api.DataBase.Storage.ManaUnifiedStorageHandler;
import com.wintercogs.beyonddimensions.Api.Util.CapCtx;
import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.BlockEntity.Custom.NetInterfaceBlockEntity;
import com.wintercogs.beyonddimensions.BlockEntity.Custom.NetPathwayBlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import vazkii.botania.api.BotaniaForgeCapabilities;

import javax.annotation.Nullable;

public class BD_BotaniaPlugin
{


    public static void attachBlockEntityCaps(AttachCapabilitiesEvent<BlockEntity> e)
    {
        BlockEntity be = e.getObject();

        if(be instanceof NetPathwayBlockEntity cBe)
        {
            class ManaUnifiedStorageHandlerProvider implements ICapabilityProvider {
                private final ManaUnifiedStorageHandler impl = new ManaUnifiedStorageHandler(cBe.getNet().getUnifiedStorage(),new CapCtx(be.getLevel(), be.getBlockPos(), be));
                private final LazyOptional<ManaUnifiedStorageHandler> opt = LazyOptional.of(() -> impl);

                @Override
                public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
                    return cap == BotaniaForgeCapabilities.SPARK_ATTACHABLE ? opt.cast() : LazyOptional.empty();
                }
                void invalidate() { opt.invalidate(); }
            }
            ManaUnifiedStorageHandlerProvider prov = new ManaUnifiedStorageHandlerProvider();
            e.addCapability(ResourceLocation.tryBuild(BeyondDimensions.MODID, "mana"), prov);
            e.addListener(prov::invalidate);
        }

        if(be instanceof NetInterfaceBlockEntity cIBe)
        {
            class ManaStackTypedHandlerProvider implements ICapabilityProvider {
                private final ManaStackTypedHandler impl = new ManaStackTypedHandler(cIBe.getStackHandler(),new CapCtx(be.getLevel(), be.getBlockPos(), be));
                private final LazyOptional<ManaStackTypedHandler> opt = LazyOptional.of(() -> impl);

                @Override
                public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
                    return cap == BotaniaForgeCapabilities.SPARK_ATTACHABLE ? opt.cast() : LazyOptional.empty();
                }
                void invalidate() { opt.invalidate(); }
            }
            ManaStackTypedHandlerProvider prov = new ManaStackTypedHandlerProvider();
            e.addCapability(ResourceLocation.tryBuild(BeyondDimensions.MODID, "mana"), prov);
            e.addListener(prov::invalidate);
        }
    }
}
