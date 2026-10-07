package com.wintercogs.beyonddimensions.util;

import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.PatchedDataComponentMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.SlotProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CookingFuel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.providers.number.ints.ResolvableInt;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.loot.NeoForgeLootContextParams;

import java.util.Optional;

public class ItemStackHelper
{
    /**
     * 在指定机器和容器上下文中查询物品燃料的燃烧时长。
     * 使用与原版 AbstractFurnaceBlockEntity.getBurnDuration 相同的组件解析入口，
     * 支持数据包定义的条件燃料和 NeoForge 的 QUERIED_STACK 参数。
     *
     * @param stack       燃料堆叠，应保留实际数量及组件
     * @param blockEntity 提供方块状态、位置和服务端世界的机器
     * @param container   查询时的槽位视图；熔炉按0原料、1燃料、2产物提供
     * @return 燃烧时长（tick）；客户端、未设置世界、空堆叠、无燃料组件或数值提供器不存在时返回0
     */
    public static int getFuelBurnTime(ItemStack stack, BlockEntity blockEntity, SlotProvider container)
    {
        if (!(blockEntity.getLevel() instanceof ServerLevel serverLevel) || stack.isEmpty())
        {
            return 0;
        }
        LootContext context = new LootContext.Builder(
                new LootParams.Builder(serverLevel)
                        .withParameter(LootContextParams.BLOCK_STATE, blockEntity.getBlockState())
                        .withParameter(LootContextParams.BLOCK_ENTITY, blockEntity)
                        .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(blockEntity.getBlockPos()))
                        .withParameter(LootContextParams.CONTAINER, container)
                        .withOptionalParameter(NeoForgeLootContextParams.QUERIED_STACK, stack)
                        .create(LootContextParamSets.CONTAINER_PROCESS))
                .create(Optional.empty());
        return ResolvableInt.getFromItem(stack, DataComponents.COOKING_FUEL, CookingFuel::burnTime, context, 0);
    }

    /**
     * 检查此堆叠的组件是否被修改过，即是否有除了默认组件之外的数据
     */
    public static boolean hasExtraComponents(ItemStack stack)
    {
        DataComponentMap comps = stack.getComponents();

        if (comps instanceof PatchedDataComponentMap patched)
        {
            return !patched.isPatchEmpty();
        }
        return false;
    }
}
