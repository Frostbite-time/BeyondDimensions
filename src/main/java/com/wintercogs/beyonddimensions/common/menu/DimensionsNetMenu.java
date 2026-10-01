package com.wintercogs.beyonddimensions.common.menu;

import com.wintercogs.beyonddimensions.api.ButtonState;
import com.wintercogs.beyonddimensions.api.dimensionnet.DimensionsNet;
import com.wintercogs.beyonddimensions.api.dimensionnet.UnifiedStorage;
import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import com.wintercogs.beyonddimensions.api.storage.handler.impl.AbstractUnorderedStackHandler;
import com.wintercogs.beyonddimensions.api.storage.handler.impl.UnorderedStackHandlerRemoveZero;
import com.wintercogs.beyonddimensions.api.storage.key.IStackKey;
import com.wintercogs.beyonddimensions.common.menu.widget.ClientNetStorage;
import com.wintercogs.beyonddimensions.common.menu.widget.slot.AbstractStackTypedSlot;
import com.wintercogs.beyonddimensions.common.menu.widget.slot.DisorderedSlotGroupSync;
import com.wintercogs.beyonddimensions.common.menu.widget.slot.DisorderedStackTypedSlot;
import com.wintercogs.beyonddimensions.config.CommonConfigRuntime;
import com.wintercogs.beyonddimensions.util.TooltipHelper;
import dev.compixel.forge.sync.MenuSync;
import dev.compixel.slots.SlotTransferRoutes;
import dev.compixel.sync.state.SyncCodecs;
import dev.compixel.sync.state.SyncSchema;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

/**
 * 打开维度网络时候所用到的Menu，处理了网络同步以及点击操作等问题
 */
public class DimensionsNetMenu extends BDBaseMenu
{
    private final MenuSync<DimensionsNetMenu> synchronization;

    @Override
    public MenuSync<DimensionsNetMenu> menuSync()
    {
        return synchronization;
    }

    @Override
    protected SlotTransferRoutes createQuickMoveRoutes()
    {
        var routes = SlotTransferRoutes.builder(slots.size())
                .group("player", inventoryStartIndex, inventoryEndIndex)
                .group("storage", storageStartIndex, storageEndIndex)
                .route("player", "storage")
                .route("storage", "player");
        // 合成变体在存储槽之后追加合成输入与产物槽
        if (storageEndIndex < slots.size())
            routes.group("crafting", storageEndIndex, slots.size()).route("crafting", "storage");
        return routes.build();
    }

    /// 客户端数据
    public int maxLines = 6; //默认大小
    private int columns = 9;
    public int lineData = 0;//从第几行开始渲染？
    public int maxLineData = 0;// 用于记录可以渲染的最大行数，即翻页到底时 当前页面 的第一行位置
    private String searchText = ""; // 客户端搜索框的输入，由GUI管理，需要确保传入时已经小写化
    public AbstractUnorderedStackHandler storage; // 客户端与服务端都使用RemoveZero版本作为实际存储
    public @Nullable ClientNetStorage clientNetStorage;

    public boolean hasShiftDown = false;

    // 客户端镜像：所属网络与存储上限，由同步 schema 写入
    private int clientNetworkId = DimensionsNet.NO_PRIMARY_NET_ID;
    private String clientNetworkName = "";
    private int clientTypeLimit = Integer.MAX_VALUE;
    private long clientAmountLimit = Long.MAX_VALUE;

    private static final SyncSchema<DimensionsNetMenu> VIEW_SCHEMA = SyncSchema.<DimensionsNetMenu>builder("beyonddimensions:storage_view", 1)
            .field("network.id", SyncCodecs.INT,
                    m -> m.player.level().isClientSide() ? m.clientNetworkId : m.serverNet() == null ? DimensionsNet.NO_PRIMARY_NET_ID : m.serverNet().getId(),
                    (m, value) -> m.clientNetworkId = value)
            .field("network.name", SyncCodecs.string(DimensionsNet.MAX_NETWORK_NAME_LENGTH * 4),
                    m -> m.player.level().isClientSide() ? m.clientNetworkName : m.serverNet() == null ? "" : m.serverNet().getCustomName(),
                    (m, value) -> m.clientNetworkName = value)
            .field("limit.types", SyncCodecs.INT,
                    m -> m.player.level().isClientSide() ? m.clientTypeLimit : m.storage.slotMaxSize,
                    (m, value) -> m.clientTypeLimit = value)
            .field("limit.amount", SyncCodecs.LONG,
                    m -> m.player.level().isClientSide() ? m.clientAmountLimit : m.storage.slotCapacity,
                    (m, value) -> m.clientAmountLimit = value)
            .build();

    protected int storageStartIndex;
    protected int storageEndIndex;


    // 构建注册用的信息
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(Registries.MENU, BDConstants.MODID);
    public static final Supplier<MenuType<DimensionsNetMenu>> Dimensions_Net_Menu = MENU_TYPES.register("dimensions_net_menu", () -> IMenuTypeExtension.create(DimensionsNetMenu::new));

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
            this.maxLines = CommonConfigRuntime.uiPageNum;
            this.searchText = CommonConfigRuntime.uiSearch;
        }

        // 初始化维度网络容器
        storage = data;
        if (player.level().isClientSide())
        {
            clientNetStorage = new ClientNetStorage(storage);
        }
        else
        {
            // 服务端传入空挂
            clientNetStorage = null;
        }

        addSlotGroupSync(new DisorderedSlotGroupSync(this, slotGroupSyncs.size(), storage)
        {
            @Override
            public void afterLoadChange(List<IStackKey<?>> changedKeys)
            {
                if (clientNetStorage != null)
                    clientNetStorage.sourceChanged(changedKeys);
                updateViewerStorage(hasShiftDown);
                var changedStacks = changedKeys.stream().map(storage::getStackByKey).filter(value -> !value.isEmpty()).toList();
                TooltipHelper.readAsCache(changedStacks, player, TooltipFlag.Default.NORMAL);
                TooltipHelper.readAsCache(changedStacks, player, TooltipFlag.Default.ADVANCED);
            }
        });

        // 添加玩家背包和快捷栏
        addPlayerInv(playerInventory);

        // 添加存储槽
        addStorageSlots();
        synchronization = commands().inventory(BDMenuResources.bind(this, VIEW_SCHEMA));
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
                    DisorderedStackTypedSlot newSlot = new DisorderedStackTypedSlot(this, clientNetStorage, -1, inventoryStartIndex, inventoryEndIndex, 8 + col * 18, 25 + row * 18);
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
                sSlot.setActive(sSlotNum < columns * getLines());
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
        this.maxLines = Math.max(1, Math.min(lines, 891 / columns));
    }

    // 服务端：存储所属的维度网络；临时或无网络的存储返回 null
    private @Nullable DimensionsNet serverNet()
    {
        return storage instanceof UnifiedStorage unified ? unified.getNet() : null;
    }

    // 客户端读取接口

    /** 存储槽位在菜单中的起始编号（共 891 个） */
    public int getStorageStartIndex()
    {
        return storageStartIndex;
    }

    public int getStorageEndIndex()
    {
        return storageEndIndex;
    }

    public boolean ready()
    {
        return synchronization.hasSnapshot();
    }

    /** 所属网络的编号，未知时为 {@link DimensionsNet#NO_PRIMARY_NET_ID} */
    public int networkId()
    {
        return clientNetworkId;
    }

    /** 所属网络的显示名称（自定义名称或默认名称） */
    public Component networkName()
    {
        return DimensionsNet.getNetworkName(clientNetworkId, clientNetworkName);
    }

    /** 可存放的资源种类上限 */
    public int typeLimit()
    {
        return clientTypeLimit;
    }

    /** 每种资源的数量上限 */
    public long amountLimit()
    {
        return clientAmountLimit;
    }

    public int getColumns()
    {
        return columns;
    }

    /**
     * 客户端视口：只改变显示的列数与行数，协议中的 891 个资源槽位与编号保持不变
     */
    public void setViewport(int columns, int rows)
    {
        if (!player.level().isClientSide()) return;
        int fittedColumns = Math.max(1, Math.min(columns, 99));
        int fittedRows = Math.max(1, Math.min(rows, 891 / fittedColumns));
        if (this.columns == fittedColumns && getLines() == fittedRows) return;
        int firstVisible = lineData * this.columns;
        this.columns = fittedColumns;
        setLines(fittedRows);
        lineData = firstVisible / fittedColumns;
        rebuildSlots();
        buildIndexList();
    }

    /**
     * 客户端专用函数，服务端请勿调用<br>
     * 使用当前客户端的真存储来更新视觉存储，然后重构索引以刷新显示
     * 比起buildIndexList开销较大，仅确定真存储有变化时才调用
     */
    public void updateViewerStorage(boolean onlyAmountUpdate)
    {
        if (clientNetStorage == null) return;

        clientNetStorage.resolvePendingOrAllUpdate(onlyAmountUpdate);
        if (!onlyAmountUpdate) buildIndexList();
    }


    /**
     * 当确定真存储不会变化，但是排序可能发生变化时，调用这个
     */
    public void buildIndexList()
    {
        if (!this.player.level().isClientSide() || clientNetStorage == null)
        {
            return;
        }
        // 1 构建正确的索引数据
        List<Integer> indexes = clientNetStorage.buildSortedIndex(
                CommonConfigRuntime.uiSortButton,
                CommonConfigRuntime.uiSecondSortButton,
                CommonConfigRuntime.uiReverseButton == ButtonState.ENABLED);

        // 2 构建linedata
        updateScrollLineData(indexes.size());
        // 3 填入索引表
        ArrayList<Integer> indexList = new ArrayList<>();
        for (int i = 0; i < getLines() * columns; i++)
        {
            //根据翻页数据构建索引列表
            if (i + lineData * columns < indexes.size())
            {
                int index = indexes.get(i + lineData * columns);
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
        if (clientNetStorage == null) return;

        this.searchText = text.toLowerCase(Locale.ENGLISH);
        this.clientNetStorage.setSearchText(searchText);
    }

    public void markForceAllUpdateClientView()
    {
        if (clientNetStorage == null) return;

        this.clientNetStorage.markForceAllUpdate();
    }

    /**
     * 设置资源类型过滤（仅客户端），null 表示显示全部类型
     */
    public void setTypeFilter(@Nullable ResourceLocation typeId)
    {
        if (clientNetStorage == null)
            return;

        this.clientNetStorage.setTypeFilter(typeId);
    }

    public void updateScrollLineData(int dataSize)
    {
        maxLineData = dataSize / columns;
        if (dataSize % columns != 0) //如果余数不为0，说明还有一行，加1
        {
            maxLineData++;
        }
        maxLineData -= getLines();
        maxLineData = Math.max(maxLineData, 0);
        lineData = Math.max(lineData, 0);
        lineData = Math.min(lineData, maxLineData);
    }


    @Override
    public boolean stillValid(@NotNull Player player)
    {
        return true;
    }

    @Override
    public void removed(@NotNull Player player)
    {
        super.removed(player);
        if (clientNetStorage != null)
            clientNetStorage.close();
    }
}

