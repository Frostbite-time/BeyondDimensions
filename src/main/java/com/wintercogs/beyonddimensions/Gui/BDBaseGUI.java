package com.wintercogs.beyonddimensions.Gui;

import com.cleanroommc.modularui.api.IGuiHolder;
import com.cleanroommc.modularui.drawable.UITexture;
import com.cleanroommc.modularui.drawable.keys.StringKey;
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
import com.cleanroommc.modularui.widgets.TextWidget;
import com.cleanroommc.modularui.widgets.textfield.TextEditorWidget;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.DataBase.ButtonState;
import com.wintercogs.beyonddimensions.DataBase.DimensionsNet;
import com.wintercogs.beyonddimensions.DataBase.Handler.IStackTypedHandler;
import com.wintercogs.beyonddimensions.DataBase.Stack.IStackType;
import com.wintercogs.beyonddimensions.Gui.Slots.StackTypedSlot;
import com.wintercogs.beyonddimensions.Unit.TinyPinyinUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.util.ITooltipFlag;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


public class BDBaseGUI implements IGuiHolder<GuiData>
{

    public static SimpleGuiFactory factory =  new SimpleGuiFactory("test",() ->{
        return new BDBaseGUI();
    });

    private IStackTypedHandler stackTypedHandler;
    private IStackTypedHandler viewerStackTypedHandler;
    private int lines = 6; //渲染的menu行数
    public int lineData = 0;//从第几行开始渲染？
    public int maxLineData = 0;// 用于记录可以渲染的最大行数，即翻页到底时 当前页面 的第一行位置
    private String searchText = "";
    private ButtonState reverseState = ButtonState.DISABLED;
    private ButtonState sortState = ButtonState.SORT_DEFAULT;

    private GuiData guiData;
    private List<StackTypedSlot> slots = new ArrayList<>(); // 直接引用，用于设置索引数据

    @Override
    public ModularPanel buildUI(GuiData guiData, GuiSyncManager guiSyncManager)
    {
        this.guiData = guiData; // 获取引用

        // 真实存储
        stackTypedHandler = new DimensionsNet().getUnifiedStorage();

        // 显示存储 双端均使用空初始化 服务器不使用此存储 客户端会在运行中更新
        // 暂时仅初始化不使用，随后待渲染测试结束后使用
        viewerStackTypedHandler = new DimensionsNet().getUnifiedStorage();



        if(!guiData.isClient())
            // 在服务端传入真实存储
            stackTypedHandler = DimensionsNet.getNetFromPlayer(guiData.getPlayer()).getUnifiedStorage();

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

        //添加玩家仓库和存储面板
        ModularPanel panel = ModularPanel.defaultPanel("test")
                .height(230)
                .bindPlayerInventory()
                .child(scrollWidget.child(buildStackTypedSlots(stackTypedHandler)))
                .child(textFieldWidget)
                .child(reverseButton)
                .child(sortMethodButton);


        return panel;
    }



    public SlotGroupWidget buildStackTypedSlots(IStackTypedHandler stackTypedHandler)
    {
        SlotGroupWidget slotGroupWidget = new SlotGroupWidget();
        slotGroupWidget.flex().coverChildren();
        slotGroupWidget.debugName("StackTypedSlots");

        String key = "StackTypedSlots";

        // 为其第一个slot添加自定义同步器同步所有slot，其余空置同步器
        for(int i = 0; i < 54; ++i) {
            if(i ==0 )
            {
                StackTypedSlot slot = new StackTypedSlot(-1,viewerStackTypedHandler);
                slotGroupWidget.child(slot.pos(i%9 *18,i/9 *18).syncHandler(stackTypedHandler).debugName("StackTypedSlot_"+i));
                ((ValueSyncHandler)slot.getSyncHandler()).setChangeListener(
                        ()->{
                            updateViewerStorage();
                        }
                );
                slots.add(slot);
            }
            else
            {
                StackTypedSlot slot = new StackTypedSlot(-1,viewerStackTypedHandler);
                slotGroupWidget.child(slot.pos(i%9 *18,i/9 *18).debugName("StackTypedSlot_"+i));
                slots.add(slot);
            }

        }


        return slotGroupWidget;
    }






    // 更新显存的函数系列


    /**
     * 客户端专用函数，服务端请勿调用<br>
     * 使用当前客户端的真存储来更新视觉存储，然后重构索引以刷新显示
     * 比起buildIndexList开销较大，仅确定真存储有变化时才调用
     */
    public void updateViewerStorage()
    {
        viewerStackTypedHandler.clearStorage();
        for(IStackType stack : this.stackTypedHandler.getStorage())
        {
            this.viewerStackTypedHandler.insert(stack.copy(),false);
        }
        buildIndexList(new ArrayList<>(viewerStackTypedHandler.getStorage()));
    }

    // 客户端函数，根据存储构建索引表 用于在动态搜索以及其他
    public void buildIndexList(ArrayList<IStackType> itemStorage)
    {
        if(!guiData.isClient())
        {
            return;
        }
        // 1 构建正确的索引数据
        ArrayList<Integer> cacheIndex = buildStorageWithCurrentState(new ArrayList<>(itemStorage));
        // 2 构建linedata
        updateScrollLineData(cacheIndex.size());
        // 3 填入索引表
        ArrayList<Integer> indexList = new ArrayList<>();
        for (int i = 0; i < lines * 9; i++)
        {
            //根据翻页数据构建索引列表
            if (i + lineData * 9 < cacheIndex.size())
            {
                int index = cacheIndex.get(i + lineData * 9);
                indexList.add(index);
            }
            else
            {
                indexList.add(-1); //传入不存在的索引，可以使对应槽位成为空
            }
        }
        // 加载索引表
        loadIndexList(indexList);
    }

    // 双端函数，根据传入列表构建索引
    public void loadIndexList(ArrayList<Integer> list)
    {
        for(int i = 0; i<list.size();i++)
        {
            ((StackTypedSlot) slots.get(i)).setSlotIndex(list.get(i));
        }
    }


    /**
     * 根据当前的搜索状态、按钮状态对存储进行排序
     * @param unifiedStorage 要排序的存储
     * @return 完成排序的索引列表
     */
    public ArrayList<Integer> buildStorageWithCurrentState(ArrayList<IStackType> unifiedStorage) {
        // 合并过滤空气和搜索逻辑，避免遍历时删除
        ArrayList<IStackType> cache = new ArrayList<>();
        ArrayList<Integer> cacheIndex = new ArrayList<>();
        for (int i = 0; i < unifiedStorage.size(); i++) {
            IStackType stack = unifiedStorage.get(i).copy();
            if (stack == null || stack.isEmpty()) continue;

            // 提前过滤空气，并缓存名称和拼音
            String displayName = stack.getDisplayName().toLowerCase(Locale.ENGLISH);
            String allPinyin = TinyPinyinUtils.getAllPinyin(displayName, false).toLowerCase(Locale.ENGLISH);
            String firstPinyin = TinyPinyinUtils.getFirstPinYin(displayName).toLowerCase(Locale.ENGLISH);
            boolean matchesSearch = searchText == null || searchText.isEmpty() ||
                    displayName.contains(searchText) ||
                    allPinyin.contains(searchText) ||
                    firstPinyin.contains(searchText) ||
                    checkTooltipMatches(stack,searchText);

            if (matchesSearch) {
                cache.add(stack);
                cacheIndex.add(i);
            }
        }

        // 统一排序逻辑，避免重复代码
        ButtonState sortState = this.sortState;
        if (sortState != ButtonState.SORT_DEFAULT) {
            Comparator<IStackType> comparator = sortState == ButtonState.SORT_NAME ?
                    Comparator.comparing(item -> item.getDisplayName()) :
                    Comparator.comparingLong(IStackType::getStackAmount);

            // 生成索引排序映射
            ArrayList<IStackType> finalCache = cache;
            List<Integer> indices = IntStream.range(0, cache.size())
                    .boxed()
                    .sorted((a, b) -> comparator.compare(finalCache.get(a), finalCache.get(b)))
                    .collect(Collectors.toList());

            // 这一步排序完成后不再需要缓存
            // 根据排序结果重组索引
            ArrayList<Integer> sortedIndices = new ArrayList<>(cacheIndex.size());
            for (int index : indices) {
                sortedIndices.add(cacheIndex.get(index));
            }
            cacheIndex = sortedIndices;
        }

        // 直接通过排序器处理倒序，避免反转操作
        if (reverseState == ButtonState.ENABLED) {
            Collections.reverse(cacheIndex);
        }

        return cacheIndex;
    }

    /**
     * 检查文本是否存在于目标物品堆叠
     * @param stack 目标物品堆叠
     * @param matchText 文本
     * @return 结果为真则意味存在
     */
    private boolean checkTooltipMatches(IStackType stack, String matchText) {
        List<String> toolTips = stack.getTooltipLines(
                Minecraft.getMinecraft().player,
                Minecraft.getMinecraft().gameSettings.advancedItemTooltips ? ITooltipFlag.TooltipFlags.ADVANCED : ITooltipFlag.TooltipFlags.NORMAL
        );
        return toolTips.stream()
                .anyMatch(tooltip -> tooltip.toLowerCase(Locale.ENGLISH).contains(matchText));
    }

    public void updateScrollLineData(int dataSize)
    {
        maxLineData = dataSize / 9 ;
        if(dataSize % 9 !=0) //如果余数不为0，说明还有一行，加1
        {
            maxLineData++;
        }
        maxLineData -= lines;
        maxLineData = Math.max(maxLineData,0);
        lineData = Math.max(lineData,0);
        lineData = Math.min(lineData,maxLineData);
    }










}
