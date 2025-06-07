package com.wintercogs.beyonddimensions.Gui;

import com.cleanroommc.modularui.api.IGuiHolder;
import com.cleanroommc.modularui.factory.GuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.viewport.LocatedWidget;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.value.sync.GuiSyncManager;
import com.cleanroommc.modularui.widgets.ItemSlot;
import com.cleanroommc.modularui.widgets.SlotGroupWidget;
import com.wintercogs.beyonddimensions.DataBase.DimensionsNet;
import com.wintercogs.beyonddimensions.DataBase.Handler.IStackTypedHandler;
import com.wintercogs.beyonddimensions.DataBase.Stack.IStackType;
import com.wintercogs.beyonddimensions.DataBase.Stack.ItemStackType;
import com.wintercogs.beyonddimensions.Gui.Slots.StackTypedSlot;
import com.wintercogs.beyonddimensions.Gui.Sync.ClickActionSync;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


public abstract class BDBaseGUI implements IGuiHolder<GuiData>
{

//    public static SimpleGuiFactory factory =  new SimpleGuiFactory("test",() ->{
//        return new BDBaseGUI();
//    });

    // 默认初始化为DimensionsNet().getUnifiedStorage()
    //
    protected IStackTypedHandler stackTypedHandler;
    protected IStackTypedHandler viewerStackTypedHandler;
    protected int lines = 6; //渲染的menu行数
    protected int lineData = 0;//从第几行开始渲染？
    protected int maxLineData = 0;// 用于记录可以渲染的最大行数，即翻页到底时 当前页面 的第一行位置

    // UI信息
    protected GuiData guiData;
    protected GuiSyncManager guiSyncManager;
    protected List<StackTypedSlot> slots = new ArrayList<>(); // 直接引用，用于设置索引数据
    private List<Integer> cacheIndex; // 在客户端存储搜索和排序建立的索引结果 降低性能消耗

    @Override
    public ModularPanel buildUI(GuiData guiData, GuiSyncManager guiSyncManager)
    {
        this.guiData = guiData; // 获取引用
        this.guiSyncManager = guiSyncManager;

        // 真实存储
        stackTypedHandler = new DimensionsNet().getUnifiedStorage();

        // 显示存储 双端均使用空初始化 服务器不使用此存储 客户端会在运行中更新
        // 暂时仅初始化不使用，随后待渲染测试结束后使用
        viewerStackTypedHandler = new DimensionsNet().getUnifiedStorage();



        if(!guiData.isClient())
            // 在服务端传入真实存储
            stackTypedHandler = DimensionsNet.getNetFromPlayer(guiData.getPlayer()).getUnifiedStorage();




        // 用于监听shift点击玩家背包事件
        ClickActionSync clickActionSync = new ClickActionSync(){
            @Override
            public void read(PacketBuffer packetBuffer) throws IOException
            {
                super.read(packetBuffer);

                if(!guiSyncManager.isClient())
                {
                    quickMoveHandleInventory(this.slotIndex,this.clickStack,this.button,this.isSlotFake,this.getSyncManager().getPlayer(), stackTypedHandler);
                }
            }
        };

        guiSyncManager.syncValue("inventory_listener",clickActionSync);

        ModularPanel panel = new ModularPanel("net_base"){
            @Override
            public boolean onMousePressed(int mouseButton)
            {
                boolean result = super.onMousePressed(mouseButton);
                if(Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)||Keyboard.isKeyDown(Keyboard.KEY_RSHIFT))
                {
                    for(LocatedWidget widget : getHovering())
                    {
                        if(widget.getElement() instanceof ItemSlot slot)
                        {
                            clickActionSync.isSlotFake = slot.getSyncHandler().isPhantom();
                            clickActionSync.clickStack = new ItemStackType((ItemStack) slot.getIngredient());
                            clickActionSync.isShiftDown = true;
                            clickActionSync.button = mouseButton;
                            // 此为此槽在库存(玩家背包中的索引)
                            clickActionSync.slotIndex = slot.getSlot().getSlotIndex();
                            clickActionSync.syncToServer(0,clickActionSync::write);
                        }
                    }
                }
                return result;
            }
        };


        panel.flex().startDefaultMode();
        panel.flex().size(176, 230).align(Alignment.Center);
        panel.flex().endDefaultMode();

        //添加玩家仓库
        panel.bindPlayerInventory();
        // 存储面板请在自己重写子类后添加

        return panel;
    }



    public abstract SlotGroupWidget buildStackTypedSlots(IStackTypedHandler stackTypedHandler);



    // 自定义点击操作
    public void customClickHandler(int slotIndex,IStackType clickedStack, int button,boolean isFake, EntityPlayer player, boolean shiftDown, IStackTypedHandler storage)
    {
        if(storage == null)
            return;

        if(isFake)
        {
            FakeClickHandle(slotIndex,clickedStack,button,isFake,player,storage);
            return;
        }

        if(shiftDown)
        {
            quickMoveHandle(slotIndex,clickedStack,button,isFake,player,storage);
        }
        else
        {
            clickHandle(slotIndex,clickedStack,button,isFake,player,storage);
        }
    }

    protected abstract void quickMoveHandle(int slotIndex,IStackType clickStack, int button, boolean isFakeSlot, EntityPlayer player, IStackTypedHandler storage);


    // 用于处理鼠标事件的函数
    protected abstract void clickHandle(int slotIndex,IStackType clickStack, int button, boolean isFakeSlot, EntityPlayer player, IStackTypedHandler storage);

    protected abstract void FakeClickHandle(int slotIndex,IStackType clickStack, int button, boolean isFakeSlot, EntityPlayer player, IStackTypedHandler storage);


    // 处理背包到存储的快速移动
    protected abstract void quickMoveHandleInventory(int slotIndex,IStackType clickStack, int button, boolean isFakeSlot, EntityPlayer player, IStackTypedHandler storage);





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
        buildIndexList(new ArrayList<>(viewerStackTypedHandler.getStorage()), true);
    }

    // 客户端函数，根据存储构建索引表 用于在动态搜索以及其他
    public void buildIndexList(ArrayList<IStackType> itemStorage, boolean needsUpdateCacheIndex)
    {
        if(!guiData.isClient())
        {
            return;
        }
        // 1 构建正确的索引数据
        if(needsUpdateCacheIndex || cacheIndex == null)
        {
            cacheIndex = buildStorageWithCurrentState(new ArrayList<>(itemStorage));
        }
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
    public List<Integer> buildStorageWithCurrentState(ArrayList<IStackType> unifiedStorage) {
        // 合并过滤空气和搜索逻辑，避免遍历时删除
        ArrayList<IStackType> cache = new ArrayList<>();
        ArrayList<Integer> cacheIndex = new ArrayList<>();
        for (int i = 0; i < unifiedStorage.size(); i++) {
            IStackType stack = unifiedStorage.get(i).copy();
            if (stack == null || stack.isEmpty()) continue;

            // 提前过滤空气，并缓存名称和拼音
            boolean matchesSearch = SeachTextMatch(stack);

            if (matchesSearch) {
                cache.add(stack);
                cacheIndex.add(i);
            }
        }

        // 此函数运行后如有后续操作，请不要再使用cacheIndex，而是改用sortedIndices
        List<Integer> sortedIndices = SortIndexList(cache, cacheIndex);

        return sortedIndices;
    }

    /**
     * 检查文本是否存在于目标物品堆叠
     * @param stack 目标物品堆叠
     * @param matchText 文本
     * @return 结果为真则意味存在
     */
    @SideOnly(Side.CLIENT)
    protected boolean checkTooltipMatches(IStackType stack, String matchText) {
        List<String> toolTips = stack.getTooltipLines(
                Minecraft.getMinecraft().player,
                Minecraft.getMinecraft().gameSettings.advancedItemTooltips ? net.minecraft.client.util.ITooltipFlag.TooltipFlags.ADVANCED : net.minecraft.client.util.ITooltipFlag.TooltipFlags.NORMAL
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


    // 用于设置搜索过滤器 传入stack，内部搜索逻辑由其自行实现
    protected abstract boolean SeachTextMatch(IStackType stack);


    // 用于排序
    protected abstract List<Integer> SortIndexList(List<IStackType> stacksSource,List<Integer> indicesSource);






}
