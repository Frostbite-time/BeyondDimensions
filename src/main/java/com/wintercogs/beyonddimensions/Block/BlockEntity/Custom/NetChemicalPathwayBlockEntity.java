package com.wintercogs.beyonddimensions.Block.BlockEntity.Custom;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.Block.BlockEntity.ModBlockEntities;
import com.wintercogs.beyonddimensions.DataBase.DimensionsNet;
import com.wintercogs.beyonddimensions.Integration.Mek.Capability.ChemicalCapabilityHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.ICapabilityProvider;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

import java.lang.reflect.Method;
import java.util.function.BiFunction;

public class NetChemicalPathwayBlockEntity extends NetedBlockEntity
{


    public NetChemicalPathwayBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.NET_CHEMICAL_PATHWAY_BLOCK_ENTITY.get(), pos, blockState);
    }

    //--- 能力注册 (通过事件) ---
    public static void registerCapability(RegisterCapabilitiesEvent event) {
        if(!BeyondDimensions.MekLoaded || ChemicalCapabilityHelper.CHEMICAL == null)
            return;
        try {
            // 获取 registerBlockEntity 方法（通过反射保留泛型信息）
            Method registerMethod = RegisterCapabilitiesEvent.class.getMethod(
                    "registerBlockEntity",
                    BlockCapability.class, // 泛型会被擦除为原始类型
                    BlockEntityType.class,
                    ICapabilityProvider.class
            );

            // 强制转换 Capability 类型
            BlockCapability<?, Direction> chemicalCap = ChemicalCapabilityHelper.CHEMICAL;
            @SuppressWarnings("unchecked")
            BlockCapability<Object, Direction> castCap = (BlockCapability<Object, Direction>) chemicalCap;

            // 注册
            registerMethod.invoke(
                    event,
                    castCap,
                    ModBlockEntities.NET_CHEMICAL_PATHWAY_BLOCK_ENTITY.get(),
                    (ICapabilityProvider<NetChemicalPathwayBlockEntity, Direction, Object>) (be, side) -> {
                        if (be.getNetId() < 0) return null;
                        DimensionsNet net = be.getNet();
                        return (net != null) ? net.getChemicalStorage() : null;
                    }
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to register capability", e);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries)
    {
        super.loadAdditional(tag,registries);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries)
    {
        super.saveAdditional(tag, registries);
    }

}
