package com.wintercogs.beyonddimensions.Gui.Sync;

import com.cleanroommc.modularui.network.NetworkUtils;
import com.cleanroommc.modularui.value.sync.ValueSyncHandler;
import com.wintercogs.beyonddimensions.DataBase.Handler.IStackTypedHandler;
import com.wintercogs.beyonddimensions.DataBase.Stack.IStackType;
import net.minecraft.network.PacketBuffer;

import java.io.IOException;
import java.util.*;

// 用于为UnifiedStorage这种对顺序弱要求的存储进行同步
public class UnorderdStackTypedHandlerSync extends ValueSyncHandler<IStackTypedHandler>
{
    private IStackTypedHandler stacks; // 存储
    private List<IStackType> lastStacks; // 上一次存储 用于对比

    public UnorderdStackTypedHandlerSync(IStackTypedHandler stacks)
    {
        this.stacks = stacks;
        lastStacks = new ArrayList<IStackType>(); //初始化为一个空列表，以进行自动同步
    }

    /**
     * 更新当前值，但是函数在此不该被调用
     *
     * @param StackTypeList     新值
     * @param setSource 源是否应该被新值更新
     * @param sync      新值是否应该被同步到 客户端/服务端
     */
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

    // 获取当前值
    @Override
    public IStackTypedHandler getValue()
    {
        return stacks;
    }

    // 本意用于检测存储和缓存的区别
    // 此处直接用于每tick更新
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

    // 把数据写入包中
    @Override
    public void write(PacketBuffer packetBuffer) throws IOException
    {
        // 写入所有变化值 分为两个列表，变化的物品和变化的数量
        // 开始运行原子化物品比较
        ArrayList<IStackType> changedItem = new ArrayList<>();
        ArrayList<Long> changedCount = new ArrayList<>();
        // 深克隆2个缓存数组
        ArrayList<IStackType> cacheLast = new ArrayList<>();
        for(IStackType stack : this.lastStacks)
        {
            cacheLast.add(stack.copy());
        }
        ArrayList<IStackType> cacheNow = new ArrayList<>();
        for(IStackType stack : this.stacks.getStorage())
        {
            cacheNow.add(stack.copy());
        }
        // 缓存结束后，立刻更新last列表
        refreshLast();

        // 为两个缓存数组分别创建Map，使用自定义的包装类作为键
        Map<IStackType, Long> lastMap = new HashMap<>();
        for (IStackType stack : cacheLast) {
            lastMap.put(stack, lastMap.getOrDefault(stack, (long) 0) + stack.getStackAmount());
        }

        Map<IStackType, Long> nowMap = new HashMap<>();
        for (IStackType stack : cacheNow) {
            nowMap.put(stack, nowMap.getOrDefault(stack, (long) 0) + stack.getStackAmount());
        }

        // 比较两个Map的差异
        Set<IStackType> allKeys = new HashSet<>();
        allKeys.addAll(lastMap.keySet());
        allKeys.addAll(nowMap.keySet());

        for (IStackType key : allKeys) {
            long lastCount = lastMap.getOrDefault(key, (long) 0);
            long nowCount = nowMap.getOrDefault(key, (long) 0);
            long delta = nowCount - lastCount;

            if (delta != 0) {
                changedItem.add(key.copy()); // 获取基础物品的拷贝
                changedCount.add(delta);
            }
        }
        // 至此，变化值完全写入changedItem与changedCount




        // 将变化写入PacketBuffer中

        // 写入变化数量
        int changes = changedItem.size();
        packetBuffer.writeVarInt(changes);

        // 写入每个变化的物品及其数量差
        for (int i = 0; i < changes; i++) {
            // 序列化物品类型
            changedItem.get(i).serialize(packetBuffer);
            // 写入对应的数量变化值
            packetBuffer.writeVarLong(changedCount.get(i));
        }


    }

    // 读取数据，并且更新值
    @Override
    public void read(PacketBuffer packetBuffer) throws IOException
    {
        if(!NetworkUtils.isClient())
        {
            return;
        }

        // 读取列表
        int changes = packetBuffer.readVarInt();
        List<IStackType> changedItem = new ArrayList<>(changes);
        List<Long> changedCount = new ArrayList<>(changes);

        for (int i = 0; i < changes; i++) {
            changedItem.add(IStackType.deserializeCommon(packetBuffer));
            changedCount.add(packetBuffer.readVarLong());
        }

        // 根据列表同步数据 - 注：只有在客户端才允许操作，不允许将数据从客户端发送到服务端
        IStackTypedHandler clientStorage = this.stacks;
        int i = 0;
        for (IStackType remoteStack : changedItem)
        {

            // 如果当前存储存在此物品
            if (clientStorage.hasStackType(remoteStack))
            {
                if (changedCount.get(i) > 0)
                {
                    clientStorage.insert(remoteStack.copyWithCount(changedCount.get(i)), false);
                }
                else
                {
                    clientStorage.extract(remoteStack.copyWithCount(-changedCount.get(i)), false);
                }
            }
            else // 如果当前存储不存在此物品
            {
                if (changedCount.get(i) > 0)
                {
                    clientStorage.insert(remoteStack.copyWithCount(changedCount.get(i)), false);
                }
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
