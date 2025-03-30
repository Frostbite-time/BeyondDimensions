package com.wintercogs.beyonddimensions.Gui.Sync;

import com.cleanroommc.modularui.value.sync.ValueSyncHandler;
import com.wintercogs.beyonddimensions.DataBase.Stack.IStackType;
import net.minecraft.network.PacketBuffer;

import java.io.IOException;

// 此同步器用于帮助传输鼠标点击事件 本身不存储数据
public class ClickActionSync extends ValueSyncHandler<Object>
{

    public boolean isSlotFake;
    public IStackType clickStack;
    public int button;
    public boolean isShiftDown;

    public int slotIndex = -1; //可选项。 主动初始化为-1。 只有是需要使用时才自行初始化


    @Override
    public void setValue(Object o, boolean b, boolean b1)
    {

    }

    @Override
    public boolean updateCacheFromSource(boolean b)
    {
        // 不使用此被动同步
        return false;
    }

    @Override
    public void write(PacketBuffer packetBuffer) throws IOException
    {
        // 鼠标事件必须能传输以下信息
        // 槽位的真假 被点击的物品是什么 变化的数量是什么
        // 点击操作可以用信道分离
        // 玩家信息由传输器提供
        packetBuffer.writeBoolean(isSlotFake);
        packetBuffer.writeBoolean(isShiftDown);
        packetBuffer.writeVarInt(button);
        packetBuffer.writeVarInt(slotIndex);
        clickStack.serialize(packetBuffer);
    }

    @Override
    public void read(PacketBuffer packetBuffer) throws IOException
    {
        this.isSlotFake = packetBuffer.readBoolean();
        this.isShiftDown = packetBuffer.readBoolean();
        this.button = packetBuffer.readVarInt();
        this.slotIndex = packetBuffer.readVarInt();
        this.clickStack = IStackType.deserializeCommon(packetBuffer);
    }

    @Override
    public void readOnClient(int id, PacketBuffer buf) throws IOException
    {
        super.readOnClient(id, buf);
    }

    @Override
    public void readOnServer(int id, PacketBuffer buf) throws IOException
    {
        super.readOnServer(id, buf);
    }

    @Override
    public Object getValue()
    {
        return null;
    }
}
