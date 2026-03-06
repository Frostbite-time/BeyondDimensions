package com.wintercogs.beyonddimensions.integration.module.ars.caps;

public interface ISourceCap
{
    boolean canAcceptSource(int var1);

    boolean canProvideSource(int var1);

    int getMaxExtract();

    int getMaxReceive();

    default boolean canExtract()
    {
        return this.canProvideSource(1);
    }

    default boolean canReceive()
    {
        return this.canAcceptSource(1);
    }

    int getSource();

    int getSourceCapacity();

    default int getMaxSource()
    {
        return this.getSourceCapacity();
    }

    void setSource(int var1);

    void setMaxSource(int var1);

    int receiveSource(int var1, boolean var2);

    int extractSource(int var1, boolean var2);
}
