package com.wintercogs.beyonddimensions.Menu;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.DataBase.ButtonName;
import com.wintercogs.beyonddimensions.DataBase.ButtonState;
import com.wintercogs.beyonddimensions.DataBase.DimensionsNet;
import com.wintercogs.beyonddimensions.DataBase.Stack.IStackType;
import com.wintercogs.beyonddimensions.DataBase.Storage.UnifiedStorage;
import com.wintercogs.beyonddimensions.Menu.Slot.StoredStackSlot;
import com.wintercogs.beyonddimensions.Packet.StoragePacket;
import com.wintercogs.beyonddimensions.Packet.SyncStoragePacket;
import com.wintercogs.beyonddimensions.Unit.TinyPinyinUtils;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.connection.ConnectionType;
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
    private int lines = 6; //渲染的menu行数
    public int lineData = 0;//从第几行开始渲染？
    public int maxLineData = 0;// 用于记录可以渲染的最大行数，即翻页到底时 当前页面 的第一行位置
    private String searchText = ""; // 客户端搜索框的输入，由GUI管理，需要确保传入时已经小写化
    private HashMap<ButtonName,ButtonState> buttonStateMap = new HashMap<>(); // 客户端的按钮状态
    public boolean isHanding = false; // 用于标记当前是否向服务端发出操作请求却未得到回应 true表示无正在处理未回应，false表示空闲
    public UnifiedStorage viewerStorage; // 在客户端，用于显示物品
    /// 服务端数据
    private ArrayList<IStackType> lastStorage; // 记录截至上一次同步时的存储状态，用于同步数据


    // 构建注册用的信息
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(Registries.MENU, BeyondDimensions.MODID);
    public static final Supplier<MenuType<DimensionsNetMenu>> Dimensions_Net_Menu = MENU_TYPES.register("dimensions_net_menu", () -> IMenuTypeExtension.create(DimensionsNetMenu::new));
    // 我们的辅助函数
    // 我们需要通过IMenuTypeExtension的.create方法才能返回一个menutype，
    // create方法需要传入一个IContainerFactory的内容，而正好我们的构造函数就是IContainerFactory一样的参数。
    // 因为就是这样设计的， 所以传入new就可以了。


    /**
     * 客户端构造函数
     * @param playerInventory 玩家背包
     */
    public DimensionsNetMenu(int id, Inventory playerInventory, FriendlyByteBuf data)
    {
        this(id, playerInventory, new DimensionsNet(), new SimpleContainerData(0));
    }

    /**
     * 服务端构造函数
     * @param playerInventory 玩家背包
     * @param data 维度网络信息，包含了存储信息
     * @param uselessContainer 此处无用，传入new SimpleContainerData(0)即可
     */
    public DimensionsNetMenu(int id, Inventory playerInventory, DimensionsNet data, SimpleContainerData uselessContainer)
    {
        super(Dimensions_Net_Menu.get(), id,playerInventory,data.getUnifiedStorage());
        // 初始化维度网络容器
        viewerStorage = new DimensionsNet().getUnifiedStorage(); // 由于服务端不实际需要这个，所以双端都给一个无数据用于初始化即可
        if(!player.level().isClientSide())
        {
            // 将lastItemStorage设置为一个深克隆，以便后续进行比较
            this.lastStorage = new ArrayList<>();
            for(IStackType stack : this.storage.getStorage())
            {
                this.lastStorage.add(stack.copy());
            }
        }

        // 初始化搜索方案
        buttonStateMap.put(ButtonName.ReverseButton,ButtonState.DISABLED);
        buttonStateMap.put(ButtonName.SortMethodButton,ButtonState.SORT_DEFAULT);

        // 开始添加槽位，其中addSlot会为menu自带的列表slots提供slot，
        // 而给slot本身传入的索引则对应其在背包中的索引
        // 添加维度网络槽位 对应slots索引 0~53
        for (int row = 0; row < lines; ++row)
        {
            for (int col = 0; col < 9; ++col)
            {   // 添加槽而不设置数据
                // 由于我们完全不依靠menu自带得方法来同步，所以可以传入一个和实际数据同步所用不一样的Storage
                // 只需要保证我们能及时把数据从实际数据同步到viewerUnifiedStorage
                // 再将slot点击操作重写的物品种类依赖
                this.addSlot(new StoredStackSlot(viewerStorage, -1, 8 + col * 18, 27+row * 18));
            }
        }

        inventoryStartIndex = slots.size();
        // 添加玩家物品栏槽位 对应slots索引 54~80
        for (int row = 0; row < 3; ++row)
        {
            for (int col = 0; col < 9; ++col)
            {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 149 + row * 18));
            }
        }
        // 添加快捷栏槽位 对应slots索引 81~89
        for (int col = 0; col < 9; ++col)
        {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 207));
        }
        inventoryEndIndex = slots.size();
    }

    /**
     * 客户端专用函数，服务端请勿调用<br>
     * 使用当前客户端的真存储来更新视觉存储，然后重构索引以刷新显示
     */
    public void updateViewerStorage()
    {
        viewerStorage.clearStorage();
        for(IStackType stack : this.storage.getStorage())
        {
            this.viewerStorage.insert(stack.copy(),false);
        }
        buildIndexList(new ArrayList<>(viewerStorage.getStorage()));
    }

    /**
     * 利用当前视觉存储信息重构索引
     * 翻页操作集成在GUI部分
     */
    public final void ScrollTo()
    {
        this.buildIndexList(new ArrayList<>(this.viewerStorage.getStorage()));
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
     * 设置当前菜单的buttonStateMap
     * @param buttonStateMap 传入的Map
     */
    public void loadButtonState(HashMap<ButtonName,ButtonState> buttonStateMap)
    {
        this.buttonStateMap = buttonStateMap;
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
            String displayName = stack.getDisplayName().getString().toLowerCase(Locale.ENGLISH);
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
        ButtonState sortState = buttonStateMap.get(ButtonName.SortMethodButton);
        if (sortState != ButtonState.SORT_DEFAULT) {
            Comparator<IStackType> comparator = sortState == ButtonState.SORT_NAME ?
                    Comparator.comparing(item -> item.getDisplayName().getString()) :
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
        if (buttonStateMap.get(ButtonName.ReverseButton) == ButtonState.ENABLED) {
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
        List<Component> toolTips = stack.getTooltipLines(
                Item.TooltipContext.of(player.level()),
                player,
                Minecraft.getInstance().options.advancedItemTooltips ?
                        TooltipFlag.Default.ADVANCED : TooltipFlag.Default.NORMAL
        );
        return toolTips.stream()
                .anyMatch(tooltip -> tooltip.getString().toLowerCase(Locale.ENGLISH).contains(matchText));
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

    // 客户端函数，根据存储构建索引表 用于在动态搜索以及其他
    public void buildIndexList(ArrayList<IStackType> itemStorage)
    {
        if(!this.player.level().isClientSide())
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

    @Override
    protected void updateChange()
    {
        // 开始运行原子化物品比较
        ArrayList<IStackType> changedItem = new ArrayList<>();
        ArrayList<Long> changedCount = new ArrayList<>();
        // 深克隆2个缓存数组
        ArrayList<IStackType> cacheLast = new ArrayList<>();
        for(IStackType stack : this.lastStorage)
        {
            cacheLast.add(stack.copy());
        }
        ArrayList<IStackType> cacheNow = new ArrayList<>();
        for(IStackType stack : this.storage.getStorage())
        {
            cacheNow.add(stack.copy());
        }
        // 缓存结束后，立刻更新last列表
        refreshLast();

        // 接下来进行根据物品种类进行比较（即将List使用近似Map的比较方法），种类变化加入changedItem，数量变化加入changedCount，数量变化使用Now-Last
        // 注意要处理last和now中可能导致的由于种类变化不同而改变了索引总数。对于Last有，而Now没有的种类意味着种类加入changedItem，数量为0-Last。反之则为种类加入changedItem，数量为Now-0
        // 对于StoredItemStack类，equals方法可以有效比较2个物品是否为统一种类而不计较数量
        // StoredItemStack.getCount可以获取数量进行进一步比较

        // 为两个缓存数组分别创建Map，使用自定义的包装类作为键
        Map<IStackType, Long> lastMap = new HashMap<>();
        for (IStackType stack : cacheLast) {
            lastMap.put(stack, lastMap.getOrDefault(stack, (long) 0) + stack.getStackAmount());
        }

        Map<IStackType, Long> nowMap = new HashMap<>();
        for (IStackType stack : cacheNow) {
            nowMap.put(stack, nowMap.getOrDefault(stack, (long) 0) + stack.getStackAmount());
        }

        // 比较两个Map的差异
        Set<IStackType> allKeys = new HashSet<>();
        allKeys.addAll(lastMap.keySet());
        allKeys.addAll(nowMap.keySet());

        for (IStackType key : allKeys) {
            long lastCount = lastMap.getOrDefault(key, (long) 0);
            long nowCount = nowMap.getOrDefault(key, (long) 0);
            long delta = nowCount - lastCount;

            if (delta != 0) {
                changedItem.add(key.copy()); // 获取基础物品的拷贝
                changedCount.add(delta);
            }
        }
        if (!changedItem.isEmpty())
        {
            PacketDistributor.sendToPlayer((ServerPlayer) player,new SyncStoragePacket(changedItem,changedCount,new ArrayList<>(0)));
        }
    }

    @Override
    protected void initUpdate()
    {

    }


    public void refreshLast()
    {
        this.lastStorage.clear();
        for(IStackType stack : this.storage.getStorage())
        {
            this.lastStorage.add(stack.copy());
        }
    }

    // 服务端函数，用于将存储空间完整发到客户端
    public void sendStorage()
    {
        // 只有服务端才能发送存储数据给客户端
        if(player instanceof ServerPlayer)
        {
            ArrayList<StoragePacket> splitPackets = new ArrayList<>(); // 用于分割包的列表
            ArrayList<IStackType> currentBatch = new ArrayList<>(); // 用于临时存储每个包的StoredItemStack
            ArrayList<Integer> currentIndices = new ArrayList<>(); // 用于精确记录索引，防止因网络延时导致错误
            int currentPayloadSize = 0; // 当前包大小
            final int MAX_PAYLOAD_SIZE = 900000; // 单个包最大大小  服务端发送到客户端的包不能大于1MiB 此处留下100KB冗余
            ArrayList<IStackType> storage = new ArrayList<>(this.storage.getStorage()); // 当前存储空间的浅克隆

            for(int i = 0;i<storage.size();i++)
            {
                // 计算此次添加的字节数
                IStackType stack = storage.get(i);
                FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer()); // 创建一个 Netty 的 ByteBuf
                RegistryFriendlyByteBuf registryBuf = new RegistryFriendlyByteBuf(buf, player.level().registryAccess(), ConnectionType.OTHER);
                stack.serialize(registryBuf);
                int entrySize = registryBuf.readableBytes() + Integer.BYTES + 1; // 物品的字节数、索引的字节数、结束标记的字节数

                boolean isLastItem = (i == storage.size() - 1);
                // 如果添加之后包会比最大负载大，则分包，然后把物品添加到下一个包
                if (currentPayloadSize + entrySize >= MAX_PAYLOAD_SIZE) {
                    splitPackets.add(new StoragePacket(
                            new ArrayList<>(currentBatch),
                            new ArrayList<>(currentIndices),
                            false
                    ));
                    currentBatch.clear();
                    currentIndices.clear();
                    currentPayloadSize = 0;
                }

                currentBatch.add(stack);
                currentIndices.add(i);
                currentPayloadSize += entrySize;

                // 如果当前添加的是最后一个物品，则分包
                if (isLastItem) {
                    splitPackets.add(new StoragePacket(
                            new ArrayList<>(currentBatch),
                            new ArrayList<>(currentIndices),
                            true
                    ));
                }
            }
            // 整理数据包，然后一次性发送
            if (!splitPackets.isEmpty()) {
                StoragePacket firstPacket = splitPackets.get(0);
                StoragePacket[] remainingPackets = splitPackets.subList(1, splitPackets.size())
                        .toArray(new StoragePacket[0]);

                PacketDistributor.sendToPlayer(
                        (ServerPlayer) this.player,
                        firstPacket,
                        remainingPackets
                );
            }
        }
    }

    // 双端函数，根据传入列表构建索引
    public void loadIndexList(ArrayList<Integer> list)
    {
        for(int i = 0; i<list.size();i++)
        {
            ((StoredStackSlot) slots.get(i)).setTheSlotIndex(list.get(i));
        }
    }

    @Override
    public boolean stillValid(Player player)
    {
        return true; // 可根据需求修改条件
    }

}

