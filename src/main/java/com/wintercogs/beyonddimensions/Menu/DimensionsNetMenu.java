package com.wintercogs.beyonddimensions.Menu;

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
import com.wintercogs.beyonddimensions.Registry.UIRegister;
import com.wintercogs.beyonddimensions.Unit.TinyPinyinUtils;
import com.wintercogs.beyonddimensions.Unit.TooltipHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.TooltipFlag;

import java.util.*;

/**
 * 打开维度网络时候所用到的Menu，处理了网络同步以及点击操作等问题
 */
public class DimensionsNetMenu extends BDBaseMenu
{
    /// 客户端数据
    public int maxLines = 6; //默认大小
    public int lineData = 0;//从第几行开始渲染？
    public int maxLineData = 0;// 用于记录可以渲染的最大行数，即翻页到底时 当前页面 的第一行位置
    private String searchText = ""; // 客户端搜索框的输入，由GUI管理，需要确保传入时已经小写化
    public UnifiedStorage storage;
    public UnifiedStorage viewerStorage; // 在客户端，用于显示物品
    private ArrayList<Integer> cacheIndex; // 在客户端存储搜索和排序建立的索引结果 降低性能消耗
    private boolean cacheTooltip = false; //客户端使用，用于记录打开UI后的第一次同步是否缓存了工具提示（用于搜索，且第一次同步通常为全量同步，此时处理较好）

    // 客户端用搜索缓存
    private static final int NAME_CACHE_MAX = 10_000;
    private static final LinkedHashMap<IStackType<?>, String> NAME_CACHE = new LinkedHashMap<>(4096, 0.75f, true)
    {
        @Override
        protected boolean removeEldestEntry(Map.Entry<IStackType<?>, String> eldest)
        {
            return size() > NAME_CACHE_MAX;
        }
    };

    public boolean hasShiftDown = false;

    protected int storageStartIndex;
    protected int storageEndIndex;

    /**
     * 客户端构造函数
     * @param playerInventory 玩家背包
     */
    public DimensionsNetMenu(int id, Inventory playerInventory, FriendlyByteBuf data)
    {
        // 客户端函数，故将Net设为临时Net
        this(UIRegister.Dimensions_Net_Menu.get(),id, playerInventory, new DimensionsNet(true));
    }

    /**
     * 服务端构造函数
     * @param playerInventory 玩家背包
     * @param data 维度网络信息，包含了存储信息
     */
    public DimensionsNetMenu(MenuType<?> menuType, int id, Inventory playerInventory, DimensionsNet data)
    {
        super(menuType, id,playerInventory);

        // 初始化搜索方案
        if(player.level().isClientSide())
        {
            this.maxLines = Config.uiPageNum;
            this.searchText = Config.uiSearch;
        }

        // 初始化维度网络容器
        storage = data.getUnifiedStorage();
        viewerStorage = new DimensionsNet(true).getUnifiedStorage(); // 由于服务端不实际需要这个，所以双端都给一个无数据用于初始化即可

        addSlotGroupSync(new DisorderedSlotGroupSync(this,slotGroupSyncs.size(),storage) {
            @Override
            public void afterLoadChange()
            {
                // 按住shift时锁定排序
                if(!hasShiftDown)
                    updateViewerStorage();
                else
                    updateOnlyCountAndNewViewer();

                if(!cacheTooltip)
                {
                    // 调用异步缓存
                    TooltipHelper.readAsCache(storage.getStorage(), player, TooltipFlag.Default.NORMAL);
                    TooltipHelper.readAsCache(storage.getStorage(), player, TooltipFlag.Default.ADVANCED);
                }
            }
        });


        // 添加玩家背包和快捷栏
        addPlayerInv(playerInventory);

        // 添加存储槽
        addStorageSlots();

    }


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

    protected void addStorageSlots()
    {
        // 默认添加99行，但将99之外的行全部设置为不激活状态，以实现动态增加和减少行数
        storageStartIndex = slots.size();
        vanillaQuickMoveStartIndex = storageStartIndex;
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
        vanillaQuickMoveEndIndex = storageEndIndex;
    }

    protected void addPlayerInv(Inventory playerInventory)
    {
        inventoryStartIndex = slots.size();
        for (int row = 0; row < 3; ++row)
        {
            for (int col = 0; col < 9; ++col)
            {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 25+ (getLines()-1)*18 + 26 + 6 + row * 18));
            }
        }
        for (int col = 0; col < 9; ++col)
        {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 25+ (getLines()-1)*18 + 26 + 6 + 3 * 18+ 4));
        }
        inventoryEndIndex = slots.size();
    }


    // 放大和缩小UI所使用的函数，用于重新确定槽位的激活状态以及槽位的位置
    public void rebuildSlots()
    {
        int sSlotNum = 0;
        for(Slot slot : slots)
        {
            if(slot instanceof AbstractStackTypedSlot sSlot)
            {
                if(sSlotNum/9 < getLines())
                    sSlot.setActive(true);
                else
                    sSlot.setActive(false);
                sSlotNum++; // 先处理再加数，可以防止最后一个槽位出现问题
            }
        }

        int slotNum = 0;
        for(int i = inventoryStartIndex; i < inventoryEndIndex; ++i)
        {
            Slot slot = slots.get(i);
            if(slot != null)
            {
                if(slotNum/9<3)
                {
                    slot.y = 25+ (getLines()-1)*18 + 26 + 6 + slotNum/9 * 18;
                }
                else
                {
                    slot.y = 25+ (getLines()-1)*18 + 26 + 6 + 3 * 18+ 4;
                }


                slotNum++;
            }
        }
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
        buildIndexList(new ArrayList<>(viewerStorage.getStorage()), true);
    }

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
    public void buildIndexList(ArrayList<IStackType<?>> itemStorage, boolean needsUpdateCacheIndex)
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
    public ArrayList<Integer> buildStorageWithCurrentState(ArrayList<IStackType<?>> unifiedStorage)
    {
        // 过滤（合并空气与搜索）
        final ArrayList<Keyed> filtered = new ArrayList<>(unifiedStorage.size());
        final boolean needSearch = this.searchText != null && !this.searchText.isEmpty();
        final String lowerSearch = needSearch ? this.searchText.toLowerCase(Locale.ENGLISH) : "";
        final String[] parts = needSearch ? splitSearch(lowerSearch) : new String[]{"","",""};
        final String namePart = parts[0], idPart = parts[1], tipPart = parts[2];
        final boolean hasSymbol = needSearch && !(idPart.isEmpty() && tipPart.isEmpty());

        for (int i = 0; i < unifiedStorage.size(); i++)
        {
            IStackType<?> s = unifiedStorage.get(i);
            if (s == null || s.isEmpty()) continue;

            // 预计算一次 key（名字走缓存）
            final String nameKey = getDisplayNameKeyCached(s);
            final String modIdKey = s.getModId() == null ? "" : s.getModId().toLowerCase(Locale.ENGLISH);

            // 搜索匹配
            if (!needSearch)
            {
                filtered.add(new Keyed(s, i, nameKey, modIdKey, s.getStackAmount()));
                continue;
            }

            if (hasSymbol) // AND 规则
            {
                if (!namePart.isEmpty() && !checkTextMatches(nameKey, namePart)) continue;
                if (!idPart.isEmpty()   && !modIdKey.contains(idPart))           continue;
                if (!tipPart.isEmpty()  && !checkTooltipMatches(s, tipPart))     continue;
                filtered.add(new Keyed(s, i, nameKey, modIdKey, s.getStackAmount()));
            }
            else // OR 规则
            {
                boolean matched = false;
                if (!namePart.isEmpty() && checkTextMatches(nameKey, namePart)) matched = true;
                if (!matched && modIdKey.contains(namePart))                    matched = true;
                if (!matched && checkTooltipMatches(s, namePart))               matched = true;
                if (matched) filtered.add(new Keyed(s, i, nameKey, modIdKey, s.getStackAmount()));
            }
        }

        // 排序
        final ButtonState sortState = Config.uiSortButton;

        Comparator<Keyed> cmp;
        if (sortState == ButtonState.SORT_NAME)
        {
            cmp = Comparator.comparing((Keyed k) -> k.nameKey)
                    .thenComparing(k -> k.modIdKey)
                    .thenComparingLong(k -> k.amount);
        }
        else if (sortState == ButtonState.SORT_QUANTITY)
        {
            cmp = Comparator.comparingLong((Keyed k) -> k.amount)
                    .thenComparing((Keyed k) -> k.nameKey)
                    .thenComparing(k -> k.modIdKey);
        }
        else if (sortState == ButtonState.SORT_MODID)
        {
            cmp = Comparator.comparing((Keyed k) -> k.modIdKey)
                    .thenComparing(k -> k.nameKey)
                    .thenComparingLong(k -> k.amount);
        }
        else // 默认：名字
        {
            cmp = Comparator.comparing((Keyed k) -> k.nameKey);
        }

        // 倒序
        if (Config.uiReverseButton == ButtonState.ENABLED)
        {
            cmp = cmp.reversed();
        }

        filtered.sort(cmp);

        // 回填索引
        ArrayList<Integer> cacheIndex = new ArrayList<>(filtered.size());
        for (Keyed k : filtered)
        {
            cacheIndex.add(k.origIndex);
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
        List<Component> toolTips = TooltipHelper.getTooltipLines(stack,
                player,
                Minecraft.getInstance().options.advancedItemTooltips ? TooltipFlag.Default.ADVANCED : TooltipFlag.Default.NORMAL);

        return toolTips.stream()
                .anyMatch(tooltip -> {

                    // 获取原始tooltip文本（小写）
                    String tooltipText = tooltip.getString().toLowerCase(Locale.ENGLISH);
                    return checkTextMatches(tooltipText, matchText);

                });
    }

    /**
     * 把搜索串拆成 “名称 / 模组ID / Tooltip” 三段，顺序任意。
     * 返回 String[3] ⇒ [namePart, idPart, tooltipPart]，不存在则为空串。
     */
    private static String[] splitSearch(String s) {
        if (s == null) return new String[] {"", "", ""};

        s = s.toLowerCase(Locale.ENGLISH);
        int at   = s.indexOf('@');
        int hash = s.indexOf('#');

        // 都没有特殊符号
        if (at < 0 && hash < 0) return new String[] {s, "", ""};

        String namePart = "";
        String idPart   = "";
        String tipPart  = "";

        // 三种情况：只含@、只含#、都含且顺序不定
        if (at >= 0 && hash >= 0) {
            // 同时存在：先找较小的索引拆 namePart
            int first = Math.min(at, hash);
            namePart  = s.substring(0, first);

            if (at < hash) {                    //  @ ... #
                idPart  = s.substring(at + 1, hash);
                tipPart = s.substring(hash + 1);
            } else {                            //  # ... @
                tipPart = s.substring(hash + 1, at);
                idPart  = s.substring(at + 1);
            }
        } else if (at >= 0) {                   // 只含 @
            namePart = s.substring(0, at);
            idPart   = s.substring(at + 1);
        } else {                                // 只含 #
            namePart = s.substring(0, hash);
            tipPart  = s.substring(hash + 1);
        }

        return new String[] {namePart, idPart, tipPart};
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
    public boolean stillValid(Player player)
    {
        return true; // 可根据需求修改条件
    }

    // 仅用于排序/过滤的快照，避免在比较器里反复取重名信息
    private static final class Keyed
    {
        final IStackType<?> stack;
        final int origIndex; // 原始索引，用来回填到 cacheIndex
        final String nameKey; // 排序/匹配用的“名字key”
        final String modIdKey;
        final long amount;

        Keyed(IStackType<?> s, int idx, String nameKey, String modIdKey, long amount)
        {
            this.stack = s;
            this.origIndex = idx;
            this.nameKey = nameKey;
            this.modIdKey = modIdKey;
            this.amount = amount;
        }
    }

    private static String getDisplayNameKeyCached(IStackType<?> key)
    {
        String val = NAME_CACHE.get(key);
        if (val != null) return val;
        String computed = key.getDisplayName().getString();
        computed = computed.toLowerCase(Locale.ENGLISH);
        // 如果未命中，做一个copy，理论上每次打开UI仅一次全量，后续增量，不会有过多负载
        NAME_CACHE.put(key.copy(), computed);
        return computed;
    }
}

