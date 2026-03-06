package com.wintercogs.beyonddimensions.integration.Ars.Caps;

import com.wintercogs.beyonddimensions.integration.Ars.BD_ArsCaps;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;

public class ItemSourceProvider implements ICapabilityProvider
{
    private final ItemStack stack;
    private final ItemSourceContentAdp impl; // 你自己的实现
    private final LazyOptional<ISourceCap> opt;

    public ItemSourceProvider(ItemStack stack)
    {
        this.stack = stack;
        this.impl = new ItemSourceContentAdp(stack);
        this.opt = LazyOptional.of(() -> impl);
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, Direction direction)
    {
        return capability == BD_ArsCaps.SOURCE_CAP ? opt.cast() : LazyOptional.empty();
    }

    public void invalidate()
    {
        opt.invalidate();
    }
}
