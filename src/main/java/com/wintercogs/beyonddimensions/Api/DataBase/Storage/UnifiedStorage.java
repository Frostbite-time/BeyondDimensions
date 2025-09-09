package com.wintercogs.beyonddimensions.Api.DataBase.Storage;

import com.wintercogs.beyonddimensions.Api.DataBase.DimensionsNet;
import com.wintercogs.beyonddimensions.Api.DataBase.Handler.UnorderedStackHandlerRemoveZero;


/**
 * 统一存储（与旧类同名）：
 * - 策略采用“到0即删除”（继承 UnorderedStackHandlerRemoveZero）
 * - 在 onChange() 时对接 DimensionsNet.setDirty()，再广播变更（调用 super.onChange()）
 */
public class UnifiedStorage extends UnorderedStackHandlerRemoveZero {

    /** 对应的维度网络，仅用于持久化脏标记 */
    private final DimensionsNet net;

    public UnifiedStorage(DimensionsNet net, UiTimestampPolicy uiTimestampPolicy) {
        super(uiTimestampPolicy);
        this.net = net;
    }

    public UnifiedStorage(DimensionsNet net, UiTimestampPolicy uiTimestampPolicy, long slotCapacity, int slotMaxSize) {
        super(uiTimestampPolicy,slotCapacity, slotMaxSize);
        this.net = net;
    }

    /** 返回一个“完全不可用”的空实现（0容量、0槽位），对外表现为全空容器且不会做脏标记 */
    public static UnifiedStorage getEmpty() {
        return new UnifiedStorage(null,UiTimestampPolicy.NONE, 0, 0) {
            @Override
            public void onChange() {
                // no-op：空实现不做脏标记也不广播
            }

            @Override
            public long getSlotCapacity(int slot) {
                return 0L;
            }

            @Override
            public boolean isEmpty() {
                return true;
            }
        };
    }

    @Override
    public void onChange() {
        if (net != null) {
            net.setDirty();
        }
        // 调用基类的广播逻辑（Any/Delta 订阅）
        super.onChange();
    }
}
