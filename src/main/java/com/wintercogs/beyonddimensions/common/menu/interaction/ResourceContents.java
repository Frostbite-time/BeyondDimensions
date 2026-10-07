package com.wintercogs.beyonddimensions.common.menu.interaction;

import com.wintercogs.beyonddimensions.api.capability.helper.CapabilityHelper;
import com.wintercogs.beyonddimensions.api.capability.helper.wrapper.IStackHandlerWrapper;
import com.wintercogs.beyonddimensions.api.capability.helper.wrapper.StackHandlerWrapperHelper;
import com.wintercogs.beyonddimensions.api.storage.key.IStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.KeyAmount;
import com.wintercogs.beyonddimensions.api.storage.key.StackKeyRegistry;
import com.wintercogs.beyonddimensions.api.storage.key.impl.FluidStackKey;
import com.wintercogs.beyonddimensions.common.init.BDFluids;
import com.wintercogs.beyonddimensions.common.item.XpExchangeItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.ItemCapability;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

/**
 * 物品携带的资源：经验棒代表经验流体，带存储能力的容器代表它的第一份内容物。标记槽与分类编辑区用它把右键放上的容器换成内容
 */
public final class ResourceContents
{
    private ResourceContents()
    {
    }

    public static @Nullable IStackKey<?> of(ItemStack stack)
    {
        if (stack.getItem() instanceof XpExchangeItem)
            return new FluidStackKey(new FluidStack(BDFluids.XP_FLUID.source(), 1));
        ItemStack single = stack.copyWithCount(1); // 通用机械的物品只在堆叠为 1 时暴露能力
        for (var entry : CapabilityHelper.ItemCapabilityMap.entrySet())
        {
            IStackHandlerWrapper<Object> handler = wrapper(single, entry.getKey(), entry.getValue());
            if (handler == null || handler.getSlots() <= 0)
                continue;
            KeyAmount contents = StackKeyRegistry.getType(entry.getKey()).fromStackObject(handler.getStackInSlot(0));
            if (contents != null && !contents.key().isEmpty())
                return contents.key();
        }
        return null;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    static @Nullable IStackHandlerWrapper<Object> wrapper(ItemStack stack, ResourceLocation typeId, @Nullable ItemCapability<?, Void> capability)
    {
        if (capability == null)
            return null;
        Object handler = stack.getCapability(capability);
        Function getter = StackHandlerWrapperHelper.stackWrappers.get(typeId);
        return handler == null || getter == null ? null : (IStackHandlerWrapper<Object>) getter.apply(handler);
    }
}
