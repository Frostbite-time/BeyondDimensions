package com.wintercogs.beyonddimensions.Gui;

import com.cleanroommc.modularui.api.IGuiHolder;
import com.cleanroommc.modularui.factory.GuiData;
import com.cleanroommc.modularui.factory.SimpleGuiFactory;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.value.sync.GuiSyncManager;
import com.cleanroommc.modularui.widget.sizer.Flex;
import com.cleanroommc.modularui.widgets.ItemSlot;
import com.cleanroommc.modularui.widgets.SlotGroupWidget;
import com.wintercogs.beyonddimensions.DataBase.DimensionsNet;
import com.wintercogs.beyonddimensions.DataBase.Handler.IStackTypedHandler;
import com.wintercogs.beyonddimensions.Gui.Slots.StackTypedSlot;

public class BDBaseGUI implements IGuiHolder<GuiData>
{

    public static SimpleGuiFactory factory =  new SimpleGuiFactory("test",() ->{
        return new BDBaseGUI();
    });

    @Override
    public ModularPanel buildUI(GuiData guiData, GuiSyncManager guiSyncManager)
    {
        // 真实存储
        IStackTypedHandler stackTypedHandler = new DimensionsNet().getUnifiedStorage();

        // 显示存储 双端均使用空初始化 服务器不使用此存储 客户端会在运行中更新
        // 暂时仅初始化不使用，随后待渲染测试结束后使用
        IStackTypedHandler viewerStackTypedHandler = new DimensionsNet().getUnifiedStorage();
        if(!guiData.isClient())
            stackTypedHandler = DimensionsNet.getNetFromPlayer(guiData.getPlayer()).getUnifiedStorage();
        ModularPanel panel = ModularPanel.defaultPanel("test")
                .bindPlayerInventory()
                .child(buildStackTypedSlots(stackTypedHandler));
        return panel;
    }

    public SlotGroupWidget buildStackTypedSlots(IStackTypedHandler stackTypedHandler)
    {
        SlotGroupWidget slotGroupWidget = new SlotGroupWidget();
        ((Flex)slotGroupWidget.flex().coverChildren()).startDefaultMode().leftRel(0.5F);
        slotGroupWidget.flex().bottom(95);
        slotGroupWidget.flex().endDefaultMode();
        slotGroupWidget.debugName("StackTypedSlots");

        String key = "StackTypedSlots";

        // 为其第一个slot添加自定义同步器同步所有slot，其余空置同步器
        for(int i = 0; i < 54; ++i) {
            if(i ==0 )
                slotGroupWidget.child(new StackTypedSlot(i,stackTypedHandler).pos(i%9 *18,i/9 *18).syncHandler(stackTypedHandler).debugName("StackTypedSlot_"+i));
            else
                slotGroupWidget.child(new StackTypedSlot(i,stackTypedHandler).pos(i%9 *18,i/9 *18).debugName("StackTypedSlot_"+i));
        }


        return slotGroupWidget;
    }
}
