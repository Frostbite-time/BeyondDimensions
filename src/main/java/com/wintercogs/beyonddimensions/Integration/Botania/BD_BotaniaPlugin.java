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

public class BD_BotaniaPlugin
{


    public static void attachBlockEntityCaps(AttachCapabilitiesEvent<BlockEntity> e)
    {
        BlockEntity be = e.getObject();

        if(be instanceof NetPathwayBlockEntity cBe)
        {
            ICapabilityProvider prov = new ICapabilityProvider()
            {
                @Override
                public <T> LazyOptional<T> getCapability(Capability<T> capability, Direction direction)
                {
                    if(capability == BotaniaForgeCapabilities.SPARK_ATTACHABLE)
                    {
                        if(cBe.getNet() != null)
                        {
                            return LazyOptional.of(() -> new ManaUnifiedStorageHandler(cBe.getNet().getUnifiedStorage(), new CapCtx(be.getLevel(), be.getBlockPos(), direction, be))).cast();
                        }
                    }
                    return LazyOptional.empty();
                }

            };
            e.addCapability(ResourceLocation.tryBuild(BeyondDimensions.MODID, "mana"), prov);
        }

        if(be instanceof NetInterfaceBlockEntity cIBe)
        {
            ICapabilityProvider prov = new ICapabilityProvider()
            {
                @Override
                public <T> LazyOptional<T> getCapability(Capability<T> capability, Direction direction)
                {
                    if(capability == BotaniaForgeCapabilities.SPARK_ATTACHABLE)
                    {
                        return LazyOptional.of(() -> new ManaStackTypedHandler(cIBe.getStackHandler(), new CapCtx(be.getLevel(), be.getBlockPos(), direction, be))).cast();
                    }
                    return LazyOptional.empty();
                }

            };
            e.addCapability(ResourceLocation.tryBuild(BeyondDimensions.MODID, "mana"), prov);
        }
    }
}
