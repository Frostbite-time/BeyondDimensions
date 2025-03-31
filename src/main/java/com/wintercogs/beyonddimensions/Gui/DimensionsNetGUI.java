package com.wintercogs.beyonddimensions.Gui;

import com.cleanroommc.modularui.drawable.UITexture;
import com.cleanroommc.modularui.factory.GuiData;
import com.cleanroommc.modularui.factory.SimpleGuiFactory;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.ModularScreen;
import com.cleanroommc.modularui.value.sync.GuiSyncManager;
import com.cleanroommc.modularui.value.sync.ValueSyncHandler;
import com.cleanroommc.modularui.widget.ScrollWidget;
import com.cleanroommc.modularui.widget.scroll.VerticalScrollData;
import com.cleanroommc.modularui.widget.sizer.Flex;
import com.cleanroommc.modularui.widgets.CycleButtonWidget;
import com.cleanroommc.modularui.widgets.SlotGroupWidget;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.DataBase.ButtonState;
import com.wintercogs.beyonddimensions.DataBase.Handler.IStackTypedHandler;
import com.wintercogs.beyonddimensions.DataBase.Stack.IStackType;
import com.wintercogs.beyonddimensions.Gui.Slots.StackTypedSlot;
import com.wintercogs.beyonddimensions.Gui.Sync.ClickActionSync;
import com.wintercogs.beyonddimensions.Gui.Sync.UnorderdStackTypedHandlerSync;
import com.wintercogs.beyonddimensions.Gui.Widgets.SyncAbleSlotGroupWidget;
import com.wintercogs.beyonddimensions.Unit.TinyPinyinUtils;
import net.minecraft.network.PacketBuffer;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class DimensionsNetGUI extends BDDisorderedContainerGUI
{
    public static SimpleGuiFactory factory =  new SimpleGuiFactory("dimensions_net_gui",() ->{
        return new DimensionsNetGUI();
    });

    private String searchText = "";
    private ButtonState reverseState = ButtonState.DISABLED;
    private ButtonState sortState = ButtonState.SORT_DEFAULT;

    @Override
    public ModularPanel buildUI(GuiData guiData, GuiSyncManager guiSyncManager)
    {
        ModularPanel panel = super.buildUI(guiData, guiSyncManager);

        // 搜索输入区域
        TextFieldWidget textFieldWidget = new TextFieldWidget(){

            @Override
            public void onUpdate()
            {
                super.onUpdate();
                searchText = this.getText();
            }
        };
        textFieldWidget.top(5).leftRel(0.5f).width(85);

        // 倒序按钮
        CycleButtonWidget reverseButton = new CycleButtonWidget()
        {
            @Override
            public void next()
            {
                super.next();
                if(reverseState == ButtonState.DISABLED)
                    reverseState = ButtonState.ENABLED;
                else
                    reverseState = ButtonState.DISABLED;
            }
        };
        reverseButton.length(2)
                .texture(UITexture.fullImage(BeyondDimensions.MODID,"textures/gui/sprites/widget/sort_reverse.png"))
                .addTooltip(0,"顺序")
                .addTooltip(1,"倒序");

        // 排序方式按钮
        CycleButtonWidget sortMethodButton = new CycleButtonWidget()
        {
            @Override
            public void next()
            {
                super.next();
                if(sortState==ButtonState.SORT_DEFAULT)
                    sortState = ButtonState.SORT_NAME;
                else if(sortState==ButtonState.SORT_NAME)
                    sortState = ButtonState.SORT_QUANTITY;
                else
                    sortState = ButtonState.SORT_DEFAULT;
            }
        };
        sortMethodButton.left(18)
                .length(3)
                .texture(UITexture.fullImage(BeyondDimensions.MODID,"textures/gui/sprites/widget/sort_group.png"))
                .addTooltip(0,"默认")
                .addTooltip(1,"名称")
                .addTooltip(2,"数量");

        // 添加滚动区域
        // 这个泛型是为了什么？
        ScrollWidget scrollWidget = new ScrollWidget(new VerticalScrollData()){
            @Override
            public boolean onMouseScroll(ModularScreen.UpOrDown scrollDirection, int amount)
            {
                super.onMouseScroll(scrollDirection, amount);
                if (scrollDirection.isUp())
                {
                    lineData--;
                } else if(scrollDirection.isDown())
                {
                    lineData++;
                }
                //ScrollTo会处理lineData小于0的情况 并通知客户端翻页
                buildIndexList(new ArrayList<>(viewerStackTypedHandler.getStorage()));
                return true;
            }
        };
        ((Flex)scrollWidget.flex().coverChildren()).startDefaultMode().leftRel(0.5F);
        scrollWidget.flex().bottom(95);
        scrollWidget.flex().endDefaultMode();
        scrollWidget.debugName("ScrollWidget");

        panel.child(scrollWidget.child(buildStackTypedSlots(stackTypedHandler)))
                .child(textFieldWidget)
                .child(reverseButton)
                .child(sortMethodButton);

        return panel;
    }

    @Override
    public SlotGroupWidget buildStackTypedSlots(IStackTypedHandler stackTypedHandler)
    {
        SyncAbleSlotGroupWidget slotGroupWidget = new SyncAbleSlotGroupWidget();
        slotGroupWidget.flex().coverChildren();
        slotGroupWidget.debugName("StackTypedSlots");

        // 设置存储同步器
        slotGroupWidget.syncHandler(new UnorderdStackTypedHandlerSync(stackTypedHandler));
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

    @Override
    protected boolean SeachTextMatch(IStackType stack)
    {
        if(searchText == null || searchText.isEmpty())
            return true;

        String displayName = stack.getDisplayName().toLowerCase(Locale.ENGLISH);
        String allPinyin = TinyPinyinUtils.getAllPinyin(displayName, false).toLowerCase(Locale.ENGLISH);
        String firstPinyin = TinyPinyinUtils.getFirstPinYin(displayName).toLowerCase(Locale.ENGLISH);
        boolean match = displayName.contains(searchText) ||
                allPinyin.contains(searchText) ||
                firstPinyin.contains(searchText) ||
                checkTooltipMatches(stack,searchText);

        return match;
    }

    @Override
    protected List<Integer> SortIndexList(List<IStackType> stacksSource, List<Integer> indicesSource)
    {
        // 统一排序逻辑，避免重复代码
        ButtonState sortState = this.sortState;
        if (sortState != ButtonState.SORT_DEFAULT) {
            Comparator<IStackType> comparator = sortState == ButtonState.SORT_NAME ?
                    Comparator.comparing(item -> item.getDisplayName()) :
                    Comparator.comparingLong(IStackType::getStackAmount);

            // 生成索引排序映射
            List<IStackType> finalCache = stacksSource;
            List<Integer> indices = IntStream.range(0, stacksSource.size())
                    .boxed()
                    .sorted((a, b) -> comparator.compare(finalCache.get(a), finalCache.get(b)))
                    .collect(Collectors.toList());

            // 这一步排序完成后不再需要缓存
            // 根据排序结果重组索引
            ArrayList<Integer> sortedIndices = new ArrayList<>(indicesSource.size());
            for (int index : indices) {
                sortedIndices.add(indicesSource.get(index));
            }
            indicesSource = sortedIndices;
        }

        // 直接通过排序器处理倒序，避免反转操作
        if (reverseState == ButtonState.ENABLED) {
            Collections.reverse(indicesSource);
        }

        return indicesSource;
    }
}
