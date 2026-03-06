package com.wintercogs.beyonddimensions.integration.module.ars.caps;

import net.minecraft.util.Mth;

public class SourceStorage implements ISourceCap
{
    protected int source;
    protected int capacity;
    protected int maxReceive;
    protected int maxExtract;

    public SourceStorage(int capacity)
    {
        this(capacity, capacity, capacity, 0);
    }

    public SourceStorage(int capacity, int maxTransfer)
    {
        this(capacity, maxTransfer, maxTransfer, 0);
    }

    public SourceStorage(int capacity, int maxReceive, int maxExtract)
    {
        this(capacity, maxReceive, maxExtract, 0);
    }

    public SourceStorage(int capacity, int maxReceive, int maxExtract, int source)
    {
        this.capacity = capacity;
        this.maxReceive = maxReceive;
        this.maxExtract = maxExtract;
        this.source = Math.max(0, Math.min(capacity, source));
    }

    public void setMaxExtract(int maxExtract)
    {
        this.maxExtract = maxExtract;
    }

    public void setMaxReceive(int maxReceive)
    {
        this.maxReceive = maxReceive;
    }

    public int getMaxExtract()
    {
        return this.maxExtract;
    }

    public int getMaxReceive()
    {
        return this.maxReceive;
    }

    public void setSource(int source)
    {
        this.source = Math.max(0, Math.min(source, this.capacity));
    }

    public int receiveSource(int toReceive, boolean simulate)
    {
        if (this.canReceive() && toReceive > 0)
        {
            int sourceReceived = Mth.clamp(this.capacity - this.source, 0, Math.min(this.maxReceive, toReceive));
            if (!simulate)
            {
                this.source += sourceReceived;
                this.onContentsChanged();
            }

            return sourceReceived;
        }
        else
        {
            return 0;
        }
    }

    public int extractSource(int toExtract, boolean simulate)
    {
        if (this.canExtract() && toExtract > 0)
        {
            int sourceExtracted = Math.min(this.source, Math.min(this.maxExtract, toExtract));
            if (!simulate)
            {
                this.source -= sourceExtracted;
                this.onContentsChanged();
            }

            return sourceExtracted;
        }
        else
        {
            return 0;
        }
    }

    public int getSource()
    {
        return this.source;
    }

    public void setMaxSource(int max)
    {
        this.capacity = max;
    }

    public int getSourceCapacity()
    {
        return this.capacity;
    }

    public boolean canAcceptSource(int source)
    {
        return this.receiveSource(source, true) > 0;
    }

    public boolean canProvideSource(int source)
    {
        return this.extractSource(source, true) > 0;
    }

    public boolean canReceive()
    {
        return this.maxReceive > 0;
    }

    public boolean canExtract()
    {
        return this.maxExtract > 0;
    }

    public void onContentsChanged()
    {
    }
}
