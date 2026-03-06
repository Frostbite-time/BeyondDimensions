package com.wintercogs.beyonddimensions.Integration.Ars.Caps;

import com.hollingsworth.arsnouveau.api.source.ISourceTile;
import com.hollingsworth.arsnouveau.api.source.ISpecialSourceProvider;
import com.wintercogs.beyonddimensions.Integration.Ars.Block.SourcePathwayBlockEntity;
import net.minecraft.core.BlockPos;

public class SourcePathwayProvider implements ISpecialSourceProvider
{
    SourcePathwayBlockEntity sourcePathway;

    public SourcePathwayProvider(SourcePathwayBlockEntity sourcePathway)
    {
        this.sourcePathway = sourcePathway;
    }

    @Override
    public ISourceTile getSource()
    {
        return sourcePathway;
    }

    @Override
    public boolean isValid()
    {
        return !sourcePathway.isRemoved() && sourcePathway.getNet() != null;
    }

    @Override
    public BlockPos getCurrentPos()
    {
        return sourcePathway.getBlockPos();
    }
}
