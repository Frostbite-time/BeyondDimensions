package com.wintercogs.beyonddimensions.integration.Ars.Caps;

import com.hollingsworth.arsnouveau.api.source.ISourceTile;
import com.wintercogs.beyonddimensions.integration.Ars.BD_ArsCaps;
import net.minecraft.core.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;

public class BESourceProvider implements ICapabilityProvider
{
    private final LazyOptional<ISourceCap> opt;

    public BESourceProvider(ISourceTile be)
    {
        ISourceCap impl = new BlockSourceAdp(be);
        this.opt = LazyOptional.of(() -> impl);
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, Direction direction)
    {
        if (capability == BD_ArsCaps.SOURCE_CAP)
        {
            return BD_ArsCaps.SOURCE_CAP.orEmpty(capability, opt);
        }
        return LazyOptional.empty();
    }

    public void invalidate()
    {
        opt.invalidate();
    }
}
