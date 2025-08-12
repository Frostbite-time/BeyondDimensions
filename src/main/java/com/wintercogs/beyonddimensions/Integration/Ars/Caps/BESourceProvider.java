package com.wintercogs.beyonddimensions.Integration.Ars.Caps;

import com.hollingsworth.arsnouveau.api.source.ISourceTile;
import com.wintercogs.beyonddimensions.Integration.Ars.BD_ArsCaps;
import net.minecraft.core.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;

public class BESourceProvider implements ICapabilityProvider
{
    private final BlockSourceAdp impl; // 通常包装 BE 自身的存储
    private final LazyOptional<ISourceCap> opt;

    public BESourceProvider(ISourceTile be) {
        this.impl = new BlockSourceAdp(be); // 从 BE 获取实际实现
        this.opt = LazyOptional.of(() -> impl);
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, Direction direction)
    {
        return capability == BD_ArsCaps.SOURCE_CAP ? opt.cast() : LazyOptional.empty();
    }

    public void invalidate() { opt.invalidate(); }

}
