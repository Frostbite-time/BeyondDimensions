package com.wintercogs.beyonddimensions.common.menu;

import com.wintercogs.beyonddimensions.api.dimensionnet.DimensionsNet;
import com.wintercogs.beyonddimensions.api.dimensionnet.UnifiedStorage;
import com.wintercogs.beyonddimensions.api.menu.BdMenuActions;
import com.wintercogs.beyonddimensions.api.menu.widget.MenuWidget;
import com.wintercogs.beyonddimensions.api.menu.workbench.StorageWorkbenchType;
import com.wintercogs.beyonddimensions.api.menu.workbench.StorageWorkbenchTypes;
import com.wintercogs.beyonddimensions.api.storage.handler.impl.AbstractUnorderedStackHandler;
import com.wintercogs.beyonddimensions.api.storage.handler.impl.UnorderedStackHandlerRemoveZero;
import com.wintercogs.beyonddimensions.api.storage.key.IStackKey;
import com.wintercogs.beyonddimensions.common.menu.sync.BDMenuResources;
import com.wintercogs.beyonddimensions.common.menu.widget.ClientNetStorage;
import com.wintercogs.beyonddimensions.common.menu.widget.CraftingWidget;
import com.wintercogs.beyonddimensions.common.menu.widget.slot.AbstractStackTypedSlot;
import com.wintercogs.beyonddimensions.common.menu.widget.slot.DisorderedSlotGroupSync;
import com.wintercogs.beyonddimensions.common.menu.widget.slot.DisorderedStackTypedSlot;
import com.wintercogs.beyonddimensions.config.ClientConfigRuntime;
import com.wintercogs.beyonddimensions.util.TooltipHelper;
import dev.compixel.forge.sync.MenuSync;
import dev.compixel.slots.SlotTransferRoutes;
import dev.compixel.sync.state.SyncCodecs;
import dev.compixel.sync.state.SyncSchema;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.TooltipFlag;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static com.wintercogs.beyonddimensions.common.init.BDMenus.Dimensions_Net_Menu;

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
        for (MenuWidget widget : widgets())
            widget.addQuickMoveRoutes(routes);
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

    // 客户端镜像：所属网络，由同步 schema 写入
    private int clientNetworkId = DimensionsNet.NO_PRIMARY_NET_ID;
    private String clientNetworkName = "";

    private static final SyncSchema<DimensionsNetMenu> VIEW_SCHEMA = SyncSchema.<DimensionsNetMenu>builder("beyonddimensions:storage_view", 2)
            .field("network_id", SyncCodecs.INT,
                    m -> m.player.level().isClientSide() ? m.clientNetworkId : m.serverNet() == null ? DimensionsNet.NO_PRIMARY_NET_ID : m.serverNet().getId(),
                    (m, value) -> m.clientNetworkId = value)
            .field("network_name", SyncCodecs.STRING,
                    m -> m.player.level().isClientSide() ? m.clientNetworkName : m.serverNet() == null ? "" : m.serverNet().getCustomName(),
                    (m, value) -> m.clientNetworkName = value)
            .build();

    protected int storageStartIndex;
    protected int storageEndIndex;

    // open 正在服务端构造菜单时，它判断出的可用工作台
    private static final ThreadLocal<List<ResourceLocation>> OPENING = new ThreadLocal<>();

    // 这次打开时可用的工作台，以及有槽位的工作台在菜单里的部件
    private final List<ResourceLocation> workbenches;
    private final Map<ResourceLocation, MenuWidget> workbenchWidgets = new LinkedHashMap<>();


    /**
     * 客户端构造函数
     *
     * @param playerInventory 玩家背包
     * @param data            打开数据：服务端判断出的可用工作台，见 {@link #open}
     */
    public DimensionsNetMenu(int id, Inventory playerInventory, @Nullable FriendlyByteBuf data)
    {
        // 客户端函数，故将Net设为临时Net
        this(Dimensions_Net_Menu.get(), id, playerInventory, new UnorderedStackHandlerRemoveZero(AbstractUnorderedStackHandler.UiTimestampPolicy.NONE), readWorkbenches(data));
    }

    /**
     * 服务端构造函数
     *
     * @param playerInventory 玩家背包
     * @param data            存储信息
     */
    public DimensionsNetMenu(MenuType<?> menuType, int id, Inventory playerInventory, AbstractUnorderedStackHandler data)
    {
        this(menuType, id, playerInventory, data, null);
    }

    /**
     * @param workbenches 客户端从打开数据读出的可用工作台；服务端为 null，按玩家判断
     */
    protected DimensionsNetMenu(MenuType<?> menuType, int id, Inventory playerInventory, AbstractUnorderedStackHandler data,
                                @Nullable List<ResourceLocation> workbenches)
    {
        super(menuType, id, playerInventory);

        // 初始化搜索方案
        if (player.level().isClientSide())
        {
            this.maxLines = ClientConfigRuntime.storageRows;
            this.searchText = ClientConfigRuntime.storageSearch;
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
        synchronization = BdMenuActions.attach(this, commands().inventory(BDMenuResources.bind(this, VIEW_SCHEMA)));
        // 工作区的工作台：服务端打开时判断一次哪些可用，客户端照打开数据构造同样的部件，两边的槽位才一一对应。
        // 不经 open 打开时客户端收不到判断结果，两边都挂上全部工作台
        this.workbenches = workbenches != null ? workbenches
                : player instanceof ServerPlayer && OPENING.get() != null ? OPENING.get()
                : StorageWorkbenchTypes.ids();
        for (ResourceLocation workbench : this.workbenches)
        {
            StorageWorkbenchType type = Objects.requireNonNull(StorageWorkbenchTypes.get(workbench),
                    () -> "Storage workbench " + workbench + " is not registered on this side");
            MenuWidget widget = type.createWidget(this);
            if (widget != null)
                workbenchWidgets.put(workbench, addWidget(widget));
        }
        commands().workbenches(synchronization);
        for (MenuWidget widget : widgets())
            widget.addActions(synchronization);
    }

    /**
     * 打开存储菜单：服务端构造菜单时判断哪些工作台可用，随打开数据发给客户端，两边按它构造同样的部件。
     * 存储菜单都要经过这里打开；以其他方式打开的存储菜单不判断，挂上全部工作台
     */
    public static void open(Player player, MenuProvider provider)
    {
        if (!(player instanceof ServerPlayer serverPlayer))
            return;
        // Forge 1.20.1 先写打开数据再构造菜单，所以先判断出可用的工作台，构造菜单时直接用它。
        // openScreen 会先在服务器上结束旧菜单而不通知客户端，从另一个 BD 界面换过来时指针留在原处
        List<ResourceLocation> workbenches = StorageWorkbenchTypes.availableFor(serverPlayer);
        NetworkHooks.openScreen(serverPlayer, new MenuProvider()
        {
            @Override
            public @Nullable AbstractContainerMenu createMenu(int containerId, @NotNull Inventory inventory, @NotNull Player player)
            {
                OPENING.set(workbenches);
                try
                {
                    return provider.createMenu(containerId, inventory, player);
                }
                finally
                {
                    OPENING.remove();
                }
            }

            @Override
            public @NotNull Component getDisplayName()
            {
                return provider.getDisplayName();
            }
        }, buf -> buf.writeCollection(workbenches, FriendlyByteBuf::writeResourceLocation));
    }

    // 打开数据里的可用工作台；没有经过 open 打开时数据为空，按全部已注册的工作台
    protected static List<ResourceLocation> readWorkbenches(@Nullable FriendlyByteBuf data)
    {
        if (data == null || !data.isReadable())
            return StorageWorkbenchTypes.ids();
        return List.copyOf(data.readList(FriendlyByteBuf::readResourceLocation));
    }

    /**
     * 这次打开时玩家能用的工作台，按下拉框的顺序
     */
    public List<ResourceLocation> workbenches()
    {
        return workbenches;
    }

    /**
     * 工作台 {@code workbench} 在菜单里的部件；它不可用或没有槽位时为 null
     */
    public @Nullable MenuWidget workbenchWidget(ResourceLocation workbench)
    {
        return workbenchWidgets.get(workbench);
    }

    /**
     * 合成台：存储菜单始终带着它，工作区收起时它仍在菜单里
     */
    public CraftingWidget crafting()
    {
        return Objects.requireNonNull(widget(CraftingWidget.class), "The storage menu has no crafting widget");
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
                    DisorderedStackTypedSlot newSlot = new DisorderedStackTypedSlot(this, clientNetStorage, -1, inventoryStartIndex, inventoryEndIndex, 0, 0);
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
                    DisorderedStackTypedSlot newSlot = new DisorderedStackTypedSlot(this, storage, -1, inventoryStartIndex, inventoryEndIndex, 0, 0);
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
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 0, 0));
            }
        }
        for (int col = 0; col < 9; ++col)
        {
            this.addSlot(new Slot(playerInventory, col, 0, 0));
        }
        inventoryEndIndex = slots.size();
    }

    // 放大和缩小UI所使用的函数，用于重新确定槽位的激活状态；槽位的位置由界面布局决定
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
    }


    // 指示可渲染的最大行数
    // 便于子类重写
    public int getLines()
    {
        return maxLines;
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

    /**
     * 存储槽位在菜单中的起始编号（共 891 个）
     */
    public int getStorageStartIndex()
    {
        return storageStartIndex;
    }

    public int getStorageEndIndex()
    {
        return storageEndIndex;
    }

    /**
     * 所属网络的编号，未知时为 {@link DimensionsNet#NO_PRIMARY_NET_ID}
     */
    public int networkId()
    {
        return clientNetworkId;
    }

    /**
     * 所属网络的显示名称（自定义名称或默认名称）
     */
    public Component networkName()
    {
        return DimensionsNet.getNetworkName(clientNetworkId, clientNetworkName);
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
                ClientConfigRuntime.storageSort,
                ClientConfigRuntime.storageUseSecondarySort ? ClientConfigRuntime.storageSecondarySort : null,
                ClientConfigRuntime.storageReverse);

        // 2 构建linedata
        updateScrollLineData(indexes.size());
        // 3 填入索引表：槽位按行轮换，翻动一行时只有移出与移入的那一行槽位换内容
        int lines = getLines();
        ArrayList<Integer> indexList = new ArrayList<>();
        for (int i = 0; i < lines * columns; i++)
        {
            int position = (lineData + displayRow(i / columns, lineData, lines)) * columns + i % columns;
            //传入不存在的索引，可以使对应槽位成为空
            indexList.add(position < indexes.size() ? indexes.get(position) : -1);
        }
        // 加载索引表
        loadIndexList(indexList);
    }

    /**
     * 首行为 lineData 时，第 slotRow 行存储槽位显示在网格的第几行。
     * 可见的第 lineData + k 行资源放在第 (lineData + k) % lines 行槽位里
     */
    public static int displayRow(int slotRow, int lineData, int lines)
    {
        return Math.floorMod(slotRow - lineData, lines);
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
     * 切换分类标签（仅客户端）
     */
    public void setCategory(ClientNetStorage.CategoryFilter filter)
    {
        if (clientNetStorage == null)
            return;

        this.clientNetStorage.setCategory(filter);
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


    /**
     * 服务端：存储所属的网络还在，没有被删除或合并掉
     */
    protected boolean networkExists()
    {
        DimensionsNet net = serverNet();
        return net != null && !net.deleted;
    }

    /**
     * 快捷键打开的页面只给网络成员用：网络被删除、或玩家已不是成员时失效，页面随即关闭
     */
    @Override
    public boolean stillValid(@NotNull Player player)
    {
        if (player.level().isClientSide())
            return true;
        return networkExists() && serverNet().getPlayers().contains(player.getUUID());
    }

    @Override
    public void removed(@NotNull Player player)
    {
        super.removed(player);
        if (clientNetStorage != null)
            clientNetStorage.close();
    }
}

