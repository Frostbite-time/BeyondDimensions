package com.wintercogs.beyonddimensions.Menu;

import com.wintercogs.beyonddimensions.Api.DataBase.ButtonState;
import com.wintercogs.beyonddimensions.Api.DataBase.Handler.AbstractUnorderedStackHandler;
import com.wintercogs.beyonddimensions.Api.DataBase.Handler.UnorderedStackHandlerKeepZero;
import com.wintercogs.beyonddimensions.Api.DataBase.Handler.UnorderedStackHandlerRemoveZero;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.IStackKey;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.KeyAmount;
import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.Config;
import com.wintercogs.beyonddimensions.Integration.JECharacters.PinInMatches;
import com.wintercogs.beyonddimensions.Menu.Slot.AbstractStackTypedSlot;
import com.wintercogs.beyonddimensions.Menu.Slot.DisorderedSlotGroupSync;
import com.wintercogs.beyonddimensions.Menu.Slot.DisorderedStackTypedSlot;
import com.wintercogs.beyonddimensions.Unit.TinyPinyinUtils;
import com.wintercogs.beyonddimensions.Unit.TooltipHelper;
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
    public AbstractUnorderedStackHandler storage; // 客户端与服务端都使用RemoveZero版本作为实际存储
    public AbstractUnorderedStackHandler viewerStorage; // 在客户端，用于显示物品，允许保留0堆叠
    private ArrayList<Integer> cacheIndex; // 在客户端存储搜索和排序建立的索引结果 降低性能消耗


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
        this(Dimensions_Net_Menu.get(), id, playerInventory, new UnorderedStackHandlerRemoveZero(AbstractUnorderedStackHandler.UiTimestampPolicy.NONE));
    }

    /**
     * 服务端构造函数
     *
     * @param playerInventory 玩家背包
     * @param data            存储信息
     */
    public DimensionsNetMenu(MenuType<?> menuType, int id, Inventory playerInventory, AbstractUnorderedStackHandler data)
    {
        super(menuType, id, playerInventory);

        // 初始化搜索方案
        if (player.level().isClientSide())
        {
            this.maxLines = Config.uiPageNum;
            this.searchText = Config.uiSearch;
        }

        // 初始化维度网络容器
        storage = data; // 此处，客户端使用可移0堆叠，不维护时间对的版本，服务端传入维度网络携带的版本
        viewerStorage = new UnorderedStackHandlerKeepZero(AbstractUnorderedStackHandler.UiTimestampPolicy.NONE); // 由于服务端不实际需要这个，所以双端都给一个无数据用于初始化即可

        addSlotGroupSync(new DisorderedSlotGroupSync(this, slotGroupSyncs.size(), storage)
        {
            @Override
            public void afterLoadChange()
            {
                // 按住shift时锁定排序
                if (!hasShiftDown)
                    updateViewerStorage();
                else
                    updateOnlyCountAndNewViewer();

                TooltipHelper.readAsCache(storage.getStorage(), Item.TooltipContext.of(player.level()), player, TooltipFlag.Default.NORMAL);
                TooltipHelper.readAsCache(storage.getStorage(), Item.TooltipContext.of(player.level()), player, TooltipFlag.Default.ADVANCED);
            }
        });

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
        vanillaQuickMoveStartIndex = storageStartIndex;
        if (player.level().isClientSide())
        {
            for (int row = 0; row < 99; ++row)
            {
                for (int col = 0; col < 9; ++col)
                {
                    DisorderedStackTypedSlot newSlot = new DisorderedStackTypedSlot(this, viewerStorage, -1, inventoryStartIndex, inventoryEndIndex, 8 + col * 18, 25 + row * 18);
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
                    DisorderedStackTypedSlot newSlot = new DisorderedStackTypedSlot(this, storage, -1, inventoryStartIndex, inventoryEndIndex, 8 + col * 18, 25 + row * 18);
                    if (row >= getLines())
                        newSlot.setActive(false);
                    this.addSlot(newSlot);
                }
            }
        }
        storageEndIndex = slots.size();
        vanillaQuickMoveEndIndex = storageEndIndex;


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
                // 仅激活当前应当显示的槽位
                sSlot.setActive(sSlotNum / 9 < getLines());
                sSlotNum++; // 先处理再加数，可以防止最后一个槽位出现问题
            }
        }

        int slotNum = 0;
        for (int i = inventoryStartIndex; i < inventoryEndIndex; ++i)
        {
            Slot slot = slots.get(i);
            // slot不为null
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
        for (KeyAmount stack : this.storage.getStorage())
        {
            this.viewerStorage.insert(stack.key(), stack.amount(), false);
        }
        buildIndexList(new ArrayList<>(viewerStorage.getStorage()), true);
    }

    // 仅仅更新视觉存储的数量信息
    public void updateOnlyCountAndNewViewer()
    {
        // 利用hashMap进行包装
        // 由于viewerStorage的对象来自storage。而storage不会被随意清空
        // hashCode将能被自定义的copy函数一并传递，无需过多性能

        Map<IStackKey<?>, Long> storageMap = new HashMap<>();

        // 填充主存储物品数量 (O(n))
        for (KeyAmount stack : storage.getStorage())
        {
            storageMap.put(stack.key(), stack.amount());
        }
        // 更新查看者存储的数量 (O(m))
        for (KeyAmount viewerStack : viewerStorage.getStorage())
        {
            // 使用哈希表直接查找数量，不存在时默认为0
            long amount = storageMap.getOrDefault(viewerStack.key(), 0L);
            // 这里内部会移除数量为0的情况，到时候再修改为锁定情况，现在先放着不管
            viewerStorage.setAmountByKey(viewerStack.key(), amount);
        }

    }


    // 客户端函数，根据存储构建索引表 用于在动态搜索以及其他
    public void buildIndexList(ArrayList<KeyAmount> itemStorage, boolean needsUpdateCacheIndex)
    {
        if (!this.player.level().isClientSide())
        {
            return;
        }
        // 1 构建正确的索引数据
        if (needsUpdateCacheIndex || cacheIndex == null)
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
        for (int slotIndex = storageStartIndex; listIndex < list.size() && slotIndex < storageEndIndex; slotIndex++)
        {
            ((AbstractStackTypedSlot) slots.get(slotIndex)).setTheSlotIndex(list.get(listIndex));
            listIndex++;
        }
    }

    /**
     * 设置当前菜单searchText，过程中会将其按照英文本地化惯例进行小写化处理
     *
     * @param text 传入的文本
     */
    public void loadSearchText(String text)
    {
        this.searchText = text.toLowerCase(Locale.ENGLISH);
    }

    /**
     * 根据当前的搜索状态、按钮状态对存储进行排序
     * - 仅按需收集字段（显示名/模组ID/Tooltip/时间戳/数量）
     * - 比较器只比较已准备好的字段，不再做昂贵取值
     * - 不做 viewerIndex 兜底（避免额外开销，也避免在“换尾删除”下产生误导）
     */
    public ArrayList<Integer> buildStorageWithCurrentState(ArrayList<KeyAmount> unifiedStorage)
    {
        if (!this.player.level().isClientSide()) return new ArrayList<>();

        // ---- 解析搜索串并确定过滤需求 ----
        final String s = (searchText == null) ? "" : searchText.toLowerCase(Locale.ENGLISH);
        final boolean hasSearch = !s.isEmpty();
        final String[] parts = splitSearch(s);
        final String namePart = parts[0];  // 名称
        final String idPart = parts[1];  // @modid
        final String tipPart = parts[2];  // #tooltip
        final boolean hasSymbol = !(idPart.isEmpty() && tipPart.isEmpty());

        final boolean needNameFilter = hasSearch && (!namePart.isEmpty());
        final boolean needModFilter = hasSearch && (hasSymbol ? !idPart.isEmpty() : !namePart.isEmpty());
        final boolean needTooltipFilter = hasSearch && (hasSymbol ? !tipPart.isEmpty() : !namePart.isEmpty());

        // ---- 决定排序需求（只在需要时准备对应字段）----
        ButtonState primaryState = Config.uiSortButton;
        ButtonState secondaryState = Config.uiSecondSortButton;
        if (primaryState == null) primaryState = ButtonState.SORT_NAME;
        final boolean useSecondary = (secondaryState != null && secondaryState != primaryState);

        final boolean needNameSort = (primaryState == ButtonState.SORT_NAME) || (useSecondary && secondaryState == ButtonState.SORT_NAME);
        final boolean needModidSort = (primaryState == ButtonState.SORT_MODID) || (useSecondary && secondaryState == ButtonState.SORT_MODID);
        final boolean needQtySort = (primaryState == ButtonState.SORT_QUANTITY) || (useSecondary && secondaryState == ButtonState.SORT_QUANTITY);
        final boolean needCTimeSort = (primaryState == ButtonState.SORT_INSERTED_TIME) || (useSecondary && secondaryState == ButtonState.SORT_INSERTED_TIME);
        final boolean needMTimeSort = (primaryState == ButtonState.SORT_MODIFIED_TIME) || (useSecondary && secondaryState == ButtonState.SORT_MODIFIED_TIME);

        // 只有在需要时才拿时间戳 map
        final Map<IStackKey<?>, Long> ctimeMap = needCTimeSort ? storage.getCreationTimeMap() : null;
        final Map<IStackKey<?>, Long> mtimeMap = needMTimeSort ? storage.getLastModifiedTimeMap() : null;

        final ArrayList<Row> rows = new ArrayList<>(unifiedStorage.size());

        // ---- 过滤 + 收集排序键（按需取值，尽量短路）----
        for (int i = 0; i < unifiedStorage.size(); i++)
        {
            KeyAmount ka = unifiedStorage.get(i);
            if (ka == null || ka.isEmpty()) continue;

            IStackKey<?> key = ka.key();

            String displayName = null;   // 仅在需要按名称过滤/排序时取
            String modIdLower = null;   // 过滤用小写
            String modIdSort = null;   // 排序用原字符串

            boolean matched;
            if (!hasSearch)
            {
                matched = true;
            }
            else if (hasSymbol)
            {
                matched = true;
                if (needNameFilter)
                {
                    displayName = key.getRender().getDisplayName(key).getString();
                    if (!checkTextMatches(displayName, namePart)) matched = false;
                }
                if (matched && !idPart.isEmpty())
                {
                    modIdLower = key.getModId().toLowerCase(Locale.ENGLISH);
                    if (!modIdLower.contains(idPart)) matched = false;
                }
                if (matched && !tipPart.isEmpty())
                {
                    if (!checkTooltipMatches(ka, tipPart)) matched = false;
                }
            }
            else
            {
                boolean any = false;
                if (!namePart.isEmpty())
                {
                    if (needNameFilter)
                    {
                        displayName = key.getRender().getDisplayName(key).getString();
                        any |= checkTextMatches(displayName, namePart);
                    }
                    if (!any && needModFilter)
                    {
                        modIdLower = key.getModId().toLowerCase(Locale.ENGLISH);
                        any |= modIdLower.contains(namePart);
                    }
                    if (!any && needTooltipFilter)
                    {
                        any |= checkTooltipMatches(ka, namePart);
                    }
                }
                matched = any;
            }
            if (!matched) continue;

            // 进入排序键收集：仅在需要时取
            if (needNameSort && displayName == null)
            {
                displayName = key.getRender().getDisplayName(key).getString();
            }
            if (needModidSort)
            {
                modIdSort = key.getModId();
            }

            long amt = needQtySort ? ka.amount() : 0L;
            long ctime = (needCTimeSort && ctimeMap != null) ? ctimeMap.getOrDefault(key, 0L) : 0L;
            long mtime = (needMTimeSort && mtimeMap != null) ? mtimeMap.getOrDefault(key, 0L) : 0L;

            rows.add(new Row(i, displayName, modIdSort, amt, ctime, mtime));
        }

        // ---- 排序（无 viewerIndex 兜底）----
        if (!rows.isEmpty())
        {
            final Comparator<Row> primary = buildRowComparator(primaryState);
            if (useSecondary)
            {
                final Comparator<Row> secondary = buildRowComparator(secondaryState);
                rows.sort(primary.thenComparing(secondary));
            }
            else
            {
                rows.sort(primary);
            }
            if (Config.uiReverseButton == ButtonState.ENABLED)
            {
                Collections.reverse(rows);
            }
        }

        // ---- 产出“视觉存储下标”列表 ----
        ArrayList<Integer> result = new ArrayList<>(rows.size());
        for (Row row : rows)
        {
            result.add(row.idx);
        }
        return result;
    }

    /**
     * @param idx       指向 unifiedStorage（即 viewerStorage）的下标
     * @param name      显示名（仅在需要时非 null）
     * @param modIdSort 模组ID（排序用原字符串；仅在需要时非 null）
     * @param amount    数量（仅在需要时有意义）
     * @param ctime     插入时间（仅在需要时有意义）
     * @param mtime     修改时间（仅在需要时有意义）
     */ // 局部行结构：仅保存排序所需键
    private record Row(int idx, String name, String modIdSort, long amount, long ctime, long mtime)
    {
    }

    /**
     * 仅比较 Row 中已准备好的字段；不做任何额外取值或 viewerIndex 兜底
     */
    private Comparator<Row> buildRowComparator(ButtonState state)
    {
        if (state == null)
        {
            // 与旧逻辑一致：默认按名称（这里假定需要时我们已填充了 name）
            return Comparator.comparing((Row r) -> r.name, String::compareTo);
        }
        return switch (state)
        {
            case SORT_QUANTITY -> Comparator.comparingLong((Row r) -> r.amount);
            case SORT_NAME -> Comparator.comparing((Row r) -> r.name, String::compareTo);
            case SORT_MODID -> Comparator.comparing((Row r) -> r.modIdSort, String::compareTo);
            case SORT_INSERTED_TIME -> Comparator.comparingLong((Row r) -> r.ctime);
            case SORT_MODIFIED_TIME -> Comparator.comparingLong((Row r) -> r.mtime);
            default -> Comparator.comparing((Row r) -> r.name, String::compareTo);
        };
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

        if (!Minecraft.getInstance().options.languageCode.startsWith("zh"))
        {
            matchPinyin = false; // 非中文地区默认不匹配
        }
        else if (BeyondDimensions.JECharactersLoaded)
        {
            matchPinyin = PinInMatches.contains(srcText, inputText);
        }
        else
        {
            String allPinyin = TinyPinyinUtils.getAllPinyin(srcText, false).toLowerCase(Locale.ENGLISH);
            String firstPinyin = TinyPinyinUtils.getFirstPinYin(srcText).toLowerCase(Locale.ENGLISH);
            matchPinyin = allPinyin.contains(inputText) || firstPinyin.contains(inputText);
        }
        return matchText || matchPinyin;
    }

    /**
     * 检查文本是否存在于目标物品堆叠
     *
     * @param stack     目标物品堆叠
     * @param matchText 文本
     * @return 结果为真则意味存在
     */
    private boolean checkTooltipMatches(KeyAmount stack, String matchText)
    {
        List<Component> toolTips = TooltipHelper.getTooltipLines(stack,
                Item.TooltipContext.of(player.level()),
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
    private static String[] splitSearch(String s)
    {
        if (s == null) return new String[]{"", "", ""};

        s = s.toLowerCase(Locale.ENGLISH);
        int at = s.indexOf('@');
        int hash = s.indexOf('#');

        // 都没有特殊符号
        if (at < 0 && hash < 0) return new String[]{s, "", ""};

        String namePart = "";
        String idPart = "";
        String tipPart = "";

        // 三种情况：只含@、只含#、都含且顺序不定
        if (at >= 0 && hash >= 0)
        {
            // 同时存在：先找较小的索引拆 namePart
            int first = Math.min(at, hash);
            namePart = s.substring(0, first);

            if (at < hash)
            {                    //  @ ... #
                idPart = s.substring(at + 1, hash);
                tipPart = s.substring(hash + 1);
            }
            else
            {                            //  # ... @
                tipPart = s.substring(hash + 1, at);
                idPart = s.substring(at + 1);
            }
        }
        else if (at >= 0)
        {                   // 只含 @
            namePart = s.substring(0, at);
            idPart = s.substring(at + 1);
        }
        else
        {                                // 只含 #
            namePart = s.substring(0, hash);
            tipPart = s.substring(hash + 1);
        }

        return new String[]{namePart, idPart, tipPart};
    }


    public void updateScrollLineData(int dataSize)
    {
        maxLineData = dataSize / 9;
        if (dataSize % 9 != 0) //如果余数不为0，说明还有一行，加1
        {
            maxLineData++;
        }
        maxLineData -= getLines();
        maxLineData = Math.max(maxLineData, 0);
        lineData = Math.max(lineData, 0);
        lineData = Math.min(lineData, maxLineData);
    }


    @Override
    public boolean stillValid(Player player)
    {
        return true; // 可根据需求修改条件
    }

}

