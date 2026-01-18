package com.wintercogs.beyonddimensions.Integration.AE;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.MEStorage;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.IStackType;
import com.wintercogs.beyonddimensions.Api.DataBase.Storage.UnifiedStorage;
import net.minecraft.network.chat.Component;

public class NetStorage implements MEStorage
{

    private final UnifiedStorage storage;

    public NetStorage(UnifiedStorage storage)
    {
        this.storage = storage;
    }

    // 将BD网络作为同优先级下的优先存储单元
    @Override
    public boolean isPreferredStorageFor(AEKey what, IActionSource source)
    {
        return true;
    }

    // 返回实际插入量
    @Override
    public long insert(AEKey what, long amount, Actionable mode, IActionSource source)
    {
        return AEHelper.fromAEKeyToIStack(what, amount)
                .map(stack -> amount - storage.insert(stack, mode.isSimulate()).getStackAmount())
                .orElse(0L);
    }

    // 返回导出量
    @Override
    public long extract(AEKey what, long amount, Actionable mode, IActionSource source)
    {
        return AEHelper.fromAEKeyToIStack(what, amount)
                .map(stack -> storage.extract(stack, mode.isSimulate()).getStackAmount())
                .orElse(0L);
    }

    @Override
    public void getAvailableStacks(KeyCounter out)
    {
        for (IStackType stack : storage.getStorage())
        {
            AEHelper.fromIStackToAEKey(stack).ifPresent(aeKey -> out.add(aeKey, stack.getStackAmount()));
        }
    }

    @Override
    public Component getDescription()
    {
        return Component.empty();
    }
}
