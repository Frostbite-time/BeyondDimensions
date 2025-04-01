package com.wintercogs.beyonddimensions.Gui;

import com.cleanroommc.modularui.drawable.UITexture;
import com.cleanroommc.modularui.factory.GuiData;
import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.value.sync.BooleanSyncValue;
import com.cleanroommc.modularui.value.sync.GuiSyncManager;
import com.cleanroommc.modularui.value.sync.ValueSyncHandler;
import com.cleanroommc.modularui.widgets.CycleButtonWidget;
import com.cleanroommc.modularui.widgets.SlotGroupWidget;
import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.BlockEntity.Custom.NetInterfaceBlockEntity;
import com.wintercogs.beyonddimensions.DataBase.Handler.IStackTypedHandler;
import com.wintercogs.beyonddimensions.DataBase.Handler.StackTypedHandler;
import com.wintercogs.beyonddimensions.DataBase.Stack.IStackType;
import com.wintercogs.beyonddimensions.Gui.Slots.StackTypedSlot;
import com.wintercogs.beyonddimensions.Gui.Sync.ClickActionSync;
import com.wintercogs.beyonddimensions.Gui.Sync.OrderedStackTypedHandlerSync;
import com.wintercogs.beyonddimensions.Gui.Widgets.SyncAbleSlotGroupWidget;
import net.minecraft.network.PacketBuffer;

import java.io.IOException;
import java.util.List;

public class NetInterfaceGUI extends BDOrderedContainerGUI
{

    private StackTypedHandler flagStorage;
    private StackTypedHandler flagViewStorage;



    @Override
    public ModularPanel buildUI(GuiData guiData, GuiSyncManager guiSyncManager)
    {
        ModularPanel panel = super.buildUI(guiData, guiSyncManager);
        stackTypedHandler = new StackTypedHandler(9);
        viewerStackTypedHandler = new StackTypedHandler(9);
        flagStorage = new StackTypedHandler(9);
        flagViewStorage = new StackTypedHandler(9);

        lines = 1;

        NetInterfaceBlockEntity BE;

        if(!guiData.isClient())
        {
            if(guiData instanceof PosGuiData posGuiData)
            {
                if(posGuiData.getTileEntity() instanceof NetInterfaceBlockEntity be)
                {
                    // 为服务端更新数据
                    stackTypedHandler = be.getStackHandler();
                    flagStorage = be.getFakeStackHandler();
                }
            }
        }

        BE = (NetInterfaceBlockEntity) ((PosGuiData)guiData).getTileEntity();

        // 突然发现CycleButtonWidget这么好用，自动按图片分割 还同时支持int 枚举 布尔类型进行选择
        CycleButtonWidget popButton = new CycleButtonWidget().value(
                new BooleanSyncValue(
                        () -> BE.popMode,
                        var -> {
                            BE.popMode = var;
                        }
                )
        );
        popButton.texture(UITexture.fullImage(BeyondDimensions.MODID,"textures/gui/sprites/widget/pop_mode.png"));

        panel.child(buildStackTypedSlots(stackTypedHandler))
                .child(buildFlagStackSlots())
                .child(popButton);
        return panel;
    }

    @Override
    public SlotGroupWidget buildStackTypedSlots(IStackTypedHandler stackTypedHandler)
    {
        SyncAbleSlotGroupWidget slotGroupWidget = new SyncAbleSlotGroupWidget();
        slotGroupWidget.flex().startDefaultMode();
        slotGroupWidget.flex().coverChildren();
        slotGroupWidget.flex().leftRel(0.5F);
        slotGroupWidget.flex().bottom(95);
        slotGroupWidget.flex().endDefaultMode();
        slotGroupWidget.debugName("StackTypedSlots");

        // 设置存储同步器
        slotGroupWidget.syncHandler(new OrderedStackTypedHandlerSync(stackTypedHandler));
        ((ValueSyncHandler)slotGroupWidget.getSyncHandler()).setChangeListener(
                ()->{
                    updateViewerStorage();
                }
        );

        String key = "StackTypedSlots";

        for(int i = 0; i < lines*9; ++i) {
            // 设置鼠标事件同步器
            ClickActionSync sync = new ClickActionSync()
            {
                // 重写read函数，进行读取操作
                @Override
                public void read(PacketBuffer packetBuffer) throws IOException
                {
                    super.read(packetBuffer);//读取值
                    if(!guiData.isClient())
                    {

                        customClickHandler(this.slotIndex,this.clickStack,this.button,isSlotFake, guiData.getPlayer(), this.isShiftDown,stackTypedHandler);
                        // 完成处理之后主动要求存储同步到客户端
                        ((ValueSyncHandler<?>) slotGroupWidget.getSyncHandler()).updateCacheFromSource(false);
                    }
                }
            };

            StackTypedSlot slot = new StackTypedSlot(-1,viewerStackTypedHandler).syncHandler(sync);
            slotGroupWidget.child(slot.pos(i%9 *18,i/9 *18).debugName("StackTypedSlot_"+i));
            slots.add(slot);
        }

        return slotGroupWidget;
    }

    public SlotGroupWidget buildFlagStackSlots()
    {
        SyncAbleSlotGroupWidget slotGroupWidget = new SyncAbleSlotGroupWidget();
        slotGroupWidget.flex().startDefaultMode();
        slotGroupWidget.flex().coverChildren();
        slotGroupWidget.flex().leftRel(0.5F);
        slotGroupWidget.flex().bottom(115);
        slotGroupWidget.flex().endDefaultMode();
        slotGroupWidget.debugName("FlagSlots");

        // 设置存储同步器
        slotGroupWidget.syncHandler(new OrderedStackTypedHandlerSync(flagStorage));
        ((ValueSyncHandler)slotGroupWidget.getSyncHandler()).setChangeListener(
                ()->{

                    for (IStackType stack : this.flagViewStorage.getStorage())
                    {
                        stack.setStackAmount(-1);
                    }

                    int index = 0;
                    for (IStackType stack : this.flagStorage.getStorage())
                    {
                        this.flagViewStorage.setStackDirectly(index,stack.copy());
                        index++;
                    }
                }
        );

        String key = "StackTypedSlots";

        for(int i = 0; i < lines*9; ++i) {
            // 设置鼠标事件同步器
            ClickActionSync sync = new ClickActionSync()
            {
                // 重写read函数，进行读取操作
                @Override
                public void read(PacketBuffer packetBuffer) throws IOException
                {
                    super.read(packetBuffer);//读取值
                    if(!guiData.isClient())
                    {

                        customClickHandler(this.slotIndex,this.clickStack,this.button,isSlotFake, guiData.getPlayer(), this.isShiftDown,flagStorage);
                        // 完成处理之后主动要求存储同步到客户端
                        ((ValueSyncHandler<?>) slotGroupWidget.getSyncHandler()).updateCacheFromSource(false);
                    }
                }
            };

            StackTypedSlot slot = new StackTypedSlot(i,flagViewStorage).syncHandler(sync);
            slot.setFake(true);
            slotGroupWidget.child(slot.pos(i%9 *18,i/9 *18).debugName("FlagSlot_"+i));
        }

        return slotGroupWidget;
    }


    // NetInterfaceGUI无需筛选 始终正确
    @Override
    protected boolean SeachTextMatch(IStackType stack)
    {
        return true;
    }

    @Override
    protected List<Integer> SortIndexList(List<IStackType> stacksSource, List<Integer> indicesSource)
    {
        return indicesSource;
    }
}
