package com.wintercogs.beyonddimensions.api.event.dimensionnet;

import com.wintercogs.beyonddimensions.api.dimensionnet.DimensionsNet;

public abstract class UnifiedStorageEvent extends DimensionsNetEvent
{
    public UnifiedStorageEvent(DimensionsNet net)
    {
        super(net);
    }

    /**
     * 在插入操作实际开始之前执行，可以在此进行预处理
     */
    public static class BeforeInsert extends UnifiedStorageEvent
    {
        public BeforeInsert(DimensionsNet net)
        {
            super(net);
        }
    }

    /**
     * 插入完成后发送，用于对外通知
     */
    public static class onInsert extends UnifiedStorageEvent
    {
        public onInsert(DimensionsNet net)
        {
            super(net);
        }
    }

    /**
     * 在提取操作实际开始之前执行，可以在此进行预处理
     */
    public static class BeforeExtract extends UnifiedStorageEvent
    {
        public BeforeExtract(DimensionsNet net)
        {
            super(net);
        }
    }

    /**
     * 提取完成后发送，用于对外通知
     */
    public static class onExtract extends UnifiedStorageEvent
    {
        public onExtract(DimensionsNet net)
        {
            super(net);
        }
    }
}
