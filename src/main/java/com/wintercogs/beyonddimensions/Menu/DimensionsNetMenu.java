package com.wintercogs.beyonddimensions.Menu;

import com.wintercogs.beyonddimensions.Api.DataBase.ButtonName;
import com.wintercogs.beyonddimensions.Api.DataBase.ButtonState;
import com.wintercogs.beyonddimensions.Api.DataBase.DimensionsNet;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.IStackType;
import com.wintercogs.beyonddimensions.Api.DataBase.Storage.UnifiedStorage;
import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.Config;
import com.wintercogs.beyonddimensions.Integration.JECharacters.PinInMatches;
import com.wintercogs.beyonddimensions.Menu.Slot.AbstractStackTypedSlot;
import com.wintercogs.beyonddimensions.Menu.Slot.DisorderedSlotGroupSync;
import com.wintercogs.beyonddimensions.Menu.Slot.DisorderedStackTypedSlot;
import com.wintercogs.beyonddimensions.Unit.TinyPinyinUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 打开维度网络时候所用到的Menu，处理了网络同步以及点击操作等问题
 */
public class DimensionsNetMenu extends BDDisorderedContainerMenu
{
    /// 客户端数据
    public int maxLines = 6; //默认大小
    public int lineData = 0;//从第几行开始渲染？
    public int maxLineData = 0;// 用于记录可以渲染的最大行数，即翻页到底时 当前页面 的第一行位置
    private String searchText = ""; // 客户端搜索框的输入，由GUI管理，需要确保传入时已经小写化
    private HashMap<ButtonName, ButtonState> buttonStateMap = new HashMap<>(); // 客户端的按钮状态
    public UnifiedStorage viewerStorage; // 在客户端，用于显示物品
    private ArrayList<Integer> cacheIndex; // 在客户端存储搜索和排序建立的索引结果 降低性能消耗
    /// 服务端数据
    private ArrayList<IStackType> lastStorage; // 记录截至上一次同步时的存储状态，用于同步数据

    public boolean hasShiftDown = false;

    protected int storageStartIndex;
    protected int storageEndIndex;


    // 构建注册用的信息
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(Registries.MENU, BeyondDimensions.MODID);
    public static final Supplier<MenuType<DimensionsNetMenu>> Dimensions_Net_Menu = MENU_TYPES.register("dimensions_net_menu", () -> IMenuTypeExtension.create(DimensionsNetMenu::new));
    // 我们的辅助函数
    // 我们需要通过IMenuTypeExtension的.create方法才能返回一个menutype，
    // create方法需要传入一个IContainerFactory的内容，而正好我们的构造函数就是IContainerFactory一样的参数。
    // 因为就是这样设计的， 所以传入new就可以了。


    /**
     * 客户端构造函数
     *
     * @param playerInventory 玩家背包
     */
    public DimensionsNetMenu(int id, Inventory playerInventory, FriendlyByteBuf data)
    {
        // 客户端函数，故将Net设为临时Net
        this(Dimensions_Net_Menu.get(), id, playerInventory, new DimensionsNet(true));
    }

    /**
     * 服务端构造函数
     *
     * @param playerInventory 玩家背包
     * @param data            维度网络信息，包含了存储信息
     */
    public DimensionsNetMenu(MenuType<?> menuType, int id, Inventory playerInventory, DimensionsNet data)
    {
        super(menuType, id, playerInventory, data.getUnifiedStorage());

        // 初始化搜索方案
        if (player.level().isClientSide())
        {
            this.maxLines = Config.uiPageNum;
            this.searchText = Config.uiSearch;
        }

        addSlotGroupSync(new DisorderedSlotGroupSync(this,slotGroupSyncs.size(),storage) {
            @Override
            public void afterLoadChange()
            {
                // 按住shift时锁定排序
                if(!hasShiftDown)
                    updateViewerStorage();
                else
                    updateOnlyCountAndNewViewer();
            }
        });

        // 初始化维度网络容器
        viewerStorage = new DimensionsNet(true).getUnifiedStorage(); // 由于服务端不实际需要这个，所以双端都给一个无数据用于初始化即可
        if (!player.level().isClientSide())
        {
            // 初始化lastStorage
            this.lastStorage = new ArrayList<>();
        }

        // 添加玩家背包和快捷栏
        addPlayerInv(playerInventory);

        // 添加存储槽
        addStorageSlots();


    }

    // 添加存储槽位
    protected void addStorageSlots()
    {
        // 默认添加99行，但将99之外的行全部设置为不激活状态，以实现动态增加和减少行数
        storageStartIndex = slots.size();
        if(player.level().isClientSide())
        {
            for (int row = 0; row < 99; ++row)
            {
                for (int col = 0; col < 9; ++col)
                {
                    DisorderedStackTypedSlot newSlot = new DisorderedStackTypedSlot(this,viewerStorage,-1,inventoryStartIndex,inventoryEndIndex,  8 + col * 18, 25 + row * 18);
                    if (row >= getLines())
                        newSlot.setActive(false);
                    this.addSlot(newSlot);
                }
            }
        }
        else
        {
            for (int row = 0; row < 99; ++row)
            {
                for (int col = 0; col < 9; ++col)
                {
                    DisorderedStackTypedSlot newSlot = new DisorderedStackTypedSlot(this,storage,-1,inventoryStartIndex,inventoryEndIndex, 8 + col * 18, 25 + row * 18);
                    if (row >= getLines())
                        newSlot.setActive(false);
                    this.addSlot(newSlot);
                }
            }
        }
        storageEndIndex = slots.size();


    }

    // 添加玩家背包
    protected void addPlayerInv(Inventory playerInventory)
    {
        inventoryStartIndex = slots.size();
        for (int row = 0; row < 3; ++row)
        {
            for (int col = 0; col < 9; ++col)
            {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 25 + (getLines() - 1) * 18 + 26 + 6 + row * 18));
            }
        }
        for (int col = 0; col < 9; ++col)
        {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 25 + (getLines() - 1) * 18 + 26 + 6 + 3 * 18 + 4));
        }
        inventoryEndIndex = slots.size();
    }

    // 放大和缩小UI所使用的函数，用于重新确定槽位的激活状态以及槽位的位置
    public void rebuildSlots()
    {
        int sSlotNum = 0;
        for (Slot slot : slots)
        {
            if (slot instanceof AbstractStackTypedSlot sSlot)
            {
                if (sSlotNum / 9 < getLines())
                    sSlot.setActive(true);
                else
                    sSlot.setActive(false);
                sSlotNum++; // 先处理再加数，可以防止最后一个槽位出现问题
            }
        }

        int slotNum = 0;
        for (int i = inventoryStartIndex; i < inventoryEndIndex; ++i)
        {
            Slot slot = slots.get(i);
            if (slot != null)
            {
                if (slotNum / 9 < 3)
                {
                    slot.y = 25 + (getLines() - 1) * 18 + 26 + 6 + slotNum / 9 * 18;
                }
                else
                {
                    slot.y = 25 + (getLines() - 1) * 18 + 26 + 6 + 3 * 18 + 4;
                }


                slotNum++;
            }
        }
    }


    // 指示可渲染的最大行数
    // 便于子类重写
    public int getLines()
    {
        return maxLines;
    }

    public void reduceLines()
    {
        maxLines--;
    }

    public void addLines()
    {
        maxLines++;
    }

    public void setLines(int lines)
    {
        this.maxLines = lines;
    }

    /**
     * 客户端专用函数，服务端请勿调用<br>
     * 使用当前客户端的真存储来更新视觉存储，然后重构索引以刷新显示
     * 比起buildIndexList开销较大，仅确定真存储有变化时才调用
     */
    public void updateViewerStorage()
    {
        viewerStorage.clearStorage();
        for(IStackType stack : this.storage.getStorage())
        {
            this.viewerStorage.insert(stack.copy(),false);
        }
        buildIndexList(new ArrayList<>(viewerStorage.getStorage()),true);
    }

    public static int BREAKPOINT_COUNTER = 0;

    // 仅仅更新视觉存储的数量信息
    public void updateOnlyCountAndNewViewer()
    {
        // 利用hashMap进行包装
        // 由于viewerStorage的对象来自storage。而storage不会被随意清空
        // hashCode将能被自定义的copy函数一并传递，无需过多性能

        Map<IStackType, Long> storageMap = new HashMap<>();

        // 填充主存储物品数量 (O(n))
        for (IStackType stack : storage.getStorage()) {
            storageMap.put(stack, stack.getStackAmount());
        }
        // 更新查看者存储的数量 (O(m))
        for (IStackType viewerStack : viewerStorage.getStorage()) {
            // 使用哈希表直接查找数量，不存在时默认为0
            long amount = storageMap.getOrDefault(viewerStack, 0L);
            viewerStack.setStackAmount(amount);
        }

    }


    // 客户端函数，根据存储构建索引表 用于在动态搜索以及其他
    public void buildIndexList(ArrayList<IStackType> itemStorage, boolean needsUpdateCacheIndex)
    {
        if(!this.player.level().isClientSide())
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
        for (int i = 0; i < getLines() * 9; i++)
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
    // 此函数实际并不安全，其生效的重要条件是 存储槽位必须首先完全添加
    public void loadIndexList(ArrayList<Integer> list)
    {
        int listIndex = 0;
        for(int slotIndex = storageStartIndex;listIndex<list.size() && slotIndex<storageEndIndex;slotIndex++)
        {
            ((AbstractStackTypedSlot) slots.get(slotIndex)).setTheSlotIndex(list.get(listIndex));
            listIndex++;
        }
    }

    /**
     * 设置当前菜单searchText，过程中会将其按照英文本地化惯例进行小写化处理
     * @param text 传入的文本
     */
    public void loadSearchText(String text)
    {
        this.searchText = text.toLowerCase(Locale.ENGLISH);
    }

    /**
     * 根据当前的搜索状态、按钮状态对存储进行排序
     * @param unifiedStorage 要排序的存储
     * @return 完成排序的索引列表
     */
    public ArrayList<Integer> buildStorageWithCurrentState(ArrayList<IStackType> unifiedStorage)
    {
        // 合并过滤空气和搜索逻辑，避免遍历时删除
        ArrayList<IStackType> cache = new ArrayList<>();
        ArrayList<Integer> cacheIndex = new ArrayList<>();
        for (int i = 0; i < unifiedStorage.size(); i++) {
            IStackType stack = unifiedStorage.get(i).copy();
            if (stack == null || stack.isEmpty()) continue;

            // 提前过滤空气，并缓存名称
            String displayName = stack.getDisplayName().getString().toLowerCase(Locale.ENGLISH);

            boolean matchesSearch;

            // 处理搜索逻辑的新规则
            if (searchText == null || searchText.isEmpty()) {
                matchesSearch = true; // 空搜索时默认匹配所有
            } else {
                // 搜索文本转小写保证大小写不敏感
                String lowerSearch = searchText.toLowerCase(Locale.ENGLISH);
                int atIndex = lowerSearch.indexOf('#');

                if (atIndex >= 0) { // 当包含#符号时的处理逻辑
                    // 拆分#前后的部分（不包括#符号）
                    String mainPart = atIndex > 0 ? lowerSearch.substring(0, atIndex) : "";
                    String tooltipPart = (atIndex + 1 < lowerSearch.length()) ?
                            lowerSearch.substring(atIndex + 1) : "";

                    // 主部分匹配逻辑
                    boolean matchesMain = mainPart.isEmpty() ||
                            checkTextMatches(displayName,mainPart); // 主部分为空时视为匹配

                    // 工具提示匹配逻辑
                    boolean matchesTooltip = tooltipPart.isEmpty() || // 工具提示部分为空时视为匹配
                            checkTooltipMatches(stack, tooltipPart);

                    matchesSearch = matchesMain && matchesTooltip;
                } else {
                    // 不含#时的常规匹配逻辑（不检查tooltip）
                    matchesSearch = checkTextMatches(displayName,lowerSearch);
                }
            }

            if (matchesSearch) {
                cache.add(stack);
                cacheIndex.add(i);
            }
        }

        // 统一排序逻辑，避免重复代码
        ButtonState sortState = Config.uiSortButton;
        if (sortState != ButtonState.SORT_DEFAULT) {
            Comparator<IStackType> comparator;
            if(sortState == ButtonState.SORT_NAME)
                comparator = Comparator.comparing(item -> item.getDisplayName().getString());
            else if(sortState == ButtonState.SORT_QUANTITY)
                comparator = Comparator.comparingLong(IStackType::getStackAmount);
            else if(sortState == ButtonState.SORT_MODID)
                comparator = Comparator.comparing(IStackType::getModId);
            else // 保底条件
                comparator = Comparator.comparing(item -> item.getDisplayName().getString());


            // 生成索引排序映射
            ArrayList<IStackType> finalCache = cache;
            List<Integer> indices = IntStream.range(0, cache.size())
                    .parallel()
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
        if (Config.uiReverseButton == ButtonState.ENABLED) {
            Collections.reverse(cacheIndex);
        }

        return cacheIndex;
    }

    /**
     * 检查文本是否匹配名称（同时检查拼音以及原文本）
     * 内部自动统一为小写
     */
    private boolean checkTextMatches(String srcText, String inputText)
    {
        srcText = srcText.toLowerCase(Locale.ENGLISH);
        inputText = inputText.toLowerCase(Locale.ENGLISH);

        boolean matchText = srcText.contains(inputText);

        boolean matchPinyin;

        if(!Minecraft.getInstance().options.languageCode.startsWith("zh"))
        {
            matchPinyin = false; // 非中文地区默认不匹配
        }
        else if(BeyondDimensions.JECharactersLoaded)
        {
            matchPinyin = PinInMatches.contains(srcText, inputText);
        }
        else
        {
            String allPinyin = TinyPinyinUtils.getAllPinyin(srcText, false).toLowerCase(Locale.ENGLISH);
            String firstPinyin = TinyPinyinUtils.getFirstPinYin(srcText).toLowerCase(Locale.ENGLISH);
            matchPinyin = allPinyin.contains(inputText) || firstPinyin.contains(inputText);
        }
        return matchText||matchPinyin;
    }

    /**
     * 检查文本是否存在于目标物品堆叠
     * @param stack 目标物品堆叠
     * @param matchText 文本
     * @return 结果为真则意味存在
     */
    private boolean checkTooltipMatches(IStackType stack, String matchText) {
        List<Component> toolTips = stack.getTooltipLines(
                Item.TooltipContext.of(player.level()),
                player,
                Minecraft.getInstance().options.advancedItemTooltips ?
                        TooltipFlag.Default.ADVANCED : TooltipFlag.Default.NORMAL
        );
        return toolTips.stream()
                .anyMatch(tooltip -> {

                    // 获取原始tooltip文本（小写）
                    String tooltipText = tooltip.getString().toLowerCase(Locale.ENGLISH);
                    return checkTextMatches(tooltipText, matchText);

                });
    }

    public void updateScrollLineData(int dataSize)
    {
        maxLineData = dataSize / 9 ;
        if(dataSize % 9 !=0) //如果余数不为0，说明还有一行，加1
        {
            maxLineData++;
        }
        maxLineData -= getLines();
        maxLineData = Math.max(maxLineData,0);
        lineData = Math.max(lineData,0);
        lineData = Math.min(lineData,maxLineData);
    }


    @Override
    protected void updateChange()
    {

    }

    @Override
    protected void initUpdate()
    {

    }

    @Override
    public boolean stillValid(Player player)
    {
        return true; // 可根据需求修改条件
    }

}

