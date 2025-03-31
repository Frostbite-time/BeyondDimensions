package com.wintercogs.beyonddimensions.Gui.Sync;

import com.cleanroommc.modularui.network.NetworkUtils;
import com.cleanroommc.modularui.value.sync.ValueSyncHandler;
import com.wintercogs.beyonddimensions.DataBase.Handler.IStackTypedHandler;
import com.wintercogs.beyonddimensions.DataBase.Stack.IStackType;
import net.minecraft.network.PacketBuffer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class OrderedStackTypedHandlerSync extends ValueSyncHandler<IStackTypedHandler>
{

    private IStackTypedHandler stacks;
    private List<IStackType> lastStacks;

    public OrderedStackTypedHandlerSync(IStackTypedHandler stacks)
    {
        this.stacks = stacks;
        lastStacks = new ArrayList<>();
    }

    @Override
    public void setValue(IStackTypedHandler StackTypeList, boolean setSource, boolean sync)
    {
        this.stacks = StackTypeList;
        if (setSource) {

        }
        if (sync) {
            if (!NetworkUtils.isClient())
            {
                syncToClient(0, this::write);
            }
        }
        onValueChanged();
    }

    @Override
    public IStackTypedHandler getValue()
    {
        return this.stacks;
    }

    @Override
    public boolean updateCacheFromSource(boolean init)
    {
        // 目前逻辑为始终为真
        if (!NetworkUtils.isClient())
        {
            // 测试，仅在开始同步
            //if(init)
            syncToClient(0, this::write);
        }

        return true;
    }

    @Override
    public void write(PacketBuffer packetBuffer) throws IOException
    {
        // 带槽位索引的原子化物品比较
        ArrayList<IStackType> changedItems = new ArrayList<>();
        ArrayList<Long> changedCounts = new ArrayList<>();
        ArrayList<Integer> changedIndices = new ArrayList<>();

        // 创建带索引的深拷贝缓存
        List<IStackType> lastSnapshot = new ArrayList<>();
        for (IStackType stack : this.lastStacks) {
            lastSnapshot.add(stack != null ? stack.copy() : null);
        }

        List<IStackType> currentSnapshot = new ArrayList<>();
        for (IStackType stack : this.stacks.getStorage()) {
            currentSnapshot.add(stack != null ? stack.copy() : null);
        }

        refreshLast(); // 快照结束后立刻更新状态

        // 确保两个快照长度一致（处理动态扩容）
        int maxSlots = Math.max(lastSnapshot.size(), currentSnapshot.size());
        while (lastSnapshot.size() < maxSlots) lastSnapshot.add(null);
        while (currentSnapshot.size() < maxSlots) currentSnapshot.add(null);

        // 逐槽位对比
        for (int slot = 0; slot < maxSlots; slot++) {
            IStackType lastStack = lastSnapshot.get(slot);
            IStackType currentStack = currentSnapshot.get(slot);

            // 检查是否需要更新
            boolean stackChanged = false;

            // 情况1：槽位从非空变成空或反之
            if ((lastStack == null) != (currentStack == null)) {
                stackChanged = true;
            }
            // 情况2：两个槽位都有物品，但类型或组件不同
            else if (lastStack != null && currentStack != null) {
                if (!lastStack.isSameTypeSameComponents(currentStack)) {
                    stackChanged = true;
                }
            }

            // 情况3：数量变化（即使类型相同）
            long delta = (currentStack != null ? currentStack.getStackAmount() : 0L)
                    - (lastStack != null ? lastStack.getStackAmount() : 0L);
            if (delta != 0) {
                stackChanged = true;
            }

            // 记录变化
            if (stackChanged) {
                changedIndices.add(slot);
                changedItems.add(currentStack != null ? currentStack.copy() : null);
                changedCounts.add(delta);
            }
        }



        // 写入变化
        int changes = changedItems.size();
        packetBuffer.writeVarInt(changes);

        // 写入每个变化的物品及其数量差
        for (int i = 0; i < changes; i++) {
            changedItems.get(i).serialize(packetBuffer);
            packetBuffer.writeLong(changedCounts.get(i));
            packetBuffer.writeInt(changedIndices.get(i));
        }

    }

    @Override
    public void read(PacketBuffer packetBuffer) throws IOException
    {
        if(!NetworkUtils.isClient())
            return;

        int changes = packetBuffer.readVarInt();
        List<IStackType> changedItems = new ArrayList<>();
        List<Long> changedCounts = new ArrayList<>();
        List<Integer> changedIndices = new ArrayList<>();

        for(int slot = 0; slot < changes; slot++)
        {
            changedItems.add(IStackType.deserializeCommon(packetBuffer));
            changedCounts.add(packetBuffer.readLong());
            changedIndices.add(packetBuffer.readInt());
        }

        // 根据列表同步数据 - 注：只有在客户端才允许操作，不允许将数据从客户端发送到服务端
        IStackTypedHandler clientStorage = this.stacks;
        int i = 0;
        for (IStackType remoteStack : changedItems)
        {

            if (changedCounts.get(i) > 0)
            {
                clientStorage.insert(changedIndices.get(i), remoteStack.copyWithCount(changedCounts.get(i)), false);
            }
            else
            {
                clientStorage.extract(changedIndices.get(i), -changedCounts.get(i), false);
            }
            i++; // 一次遍历完毕后索引自增
        }

        // 调用change
        onValueChanged();
    }

    public void refreshLast()
    {
        this.lastStacks.clear();
        for(IStackType stack : this.stacks.getStorage())
        {
            this.lastStacks.add(stack.copy());
        }
    }


}
