package com.wintercogs.beyonddimensions.Menu;

import com.google.common.base.Suppliers;
import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.DataBase.ButtonName;
import com.wintercogs.beyonddimensions.DataBase.ButtonState;
import com.wintercogs.beyonddimensions.DataBase.DimensionsNet;
import com.wintercogs.beyonddimensions.DataBase.Stack.IStackType;
import com.wintercogs.beyonddimensions.DataBase.Stack.ItemStackType;
import com.wintercogs.beyonddimensions.DataBase.Stack.StackCreater;
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
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.event.ItemStackedOnOtherEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.connection.ConnectionType;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.lwjgl.glfw.GLFW;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 打开维度网络时候所用到的Menu，处理了网络同步以及点击操作等问题
 */
public class DimensionsNetMenu extends AbstractContainerMenu
{
    /// 双端通用数据
    private final Player player; // 打开Menu的玩家实例
    public final UnifiedStorage unifiedStorage; // Menu所对应的存储，服务端通过savedData获取，客户端通过网络请求获取
    /// 客户端数据
    private int lines = 6; //渲染的menu行数
    public int lineData = 0;//从第几行开始渲染？
    public int maxLineData = 0;// 用于记录可以渲染的最大行数，即翻页到底时 当前页面 的第一行位置
    private String searchText = ""; // 客户端搜索框的输入，由GUI管理，需要确保传入时已经小写化
    private HashMap<ButtonName,ButtonState> buttonStateMap = new HashMap<>(); // 客户端的按钮状态
    public boolean isHanding = false; // 用于标记当前是否向服务端发出操作请求却未得到回应 true表示无正在处理未回应，false表示空闲
    public UnifiedStorage viewerUnifiedStorage; // 在客户端，用于显示物品
    /// 服务端数据
    private ArrayList<IStackType> lastItemStorage; // 记录截至上一次同步时的存储状态，用于同步数据


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
        super(Dimensions_Net_Menu.get(), id);
        // 初始化维度网络容器
        this.unifiedStorage = data.getUnifiedStorage();
        viewerUnifiedStorage = new DimensionsNet().getUnifiedStorage(); // 由于服务端不实际需要这个，所以双端都给一个无数据用于初始化即可
        this.player = playerInventory.player;
        if(!player.level().isClientSide())
        {
            // 将lastItemStorage设置为一个深克隆，以便后续进行比较
            this.lastItemStorage = new ArrayList<>();
            for(IStackType stack : this.unifiedStorage.getStorage())
            {
                this.lastItemStorage.add(stack.copy());
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
                this.addSlot(new StoredStackSlot(viewerUnifiedStorage, -1, 8 + col * 18, 27+row * 18));
            }
        }
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
    }

    /**
     * 客户端专用函数，服务端请勿调用<br>
     * 使用当前客户端的真存储来更新视觉存储，然后重构索引以刷新显示
     */
    public void updateViewerStorage()
    {
        viewerUnifiedStorage.clearStorage();
        viewerUnifiedStorage.getTypeIdIndexMap().clear();
        for(IStackType stack : this.unifiedStorage.getStorage())
        {
            this.viewerUnifiedStorage.insert(stack.copy(),false);
        }
        buildIndexList(new ArrayList<>(viewerUnifiedStorage.getStorage()));
    }

    /**
     * 利用当前视觉存储信息重构索引
     * 翻页操作集成在GUI部分
     */
    public final void ScrollTo()
    {
        this.buildIndexList(new ArrayList<>(this.viewerUnifiedStorage.getStorage()));
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

    /**
     * 网络传输
     * 禁止了父类对{@link StoredStackSlot}槽位的传输
     * 自定义了如何将存储从服务端传到客户端
     */
    @Override
    public void broadcastChanges()
    {
        for(int i = 0; i < this.slots.size(); ++i) {
            Slot slot = this.slots.get(i);
            if(slot instanceof StoredStackSlot)
                continue; // 不允许broadcastChanges自动同步StoredItemStackSlot以便自定义处理
            ItemStack itemstack = slot.getItem();
            Objects.requireNonNull(itemstack);
            Supplier<ItemStack> supplier = Suppliers.memoize(itemstack::copy);
            this.triggerSlotListeners(i, itemstack, supplier);
            this.synchronizeSlotToRemote(i, itemstack, supplier);
        }

        this.synchronizeCarriedToRemote();

        for(int j = 0; j < this.dataSlots.size(); ++j) {
            DataSlot dataslot = this.dataSlots.get(j);
            int k = dataslot.get();
            if (dataslot.checkAndClearUpdateFlag()) {
                this.updateDataSlotListeners(j, k);
            }

            this.synchronizeDataSlotToRemote(j, k);
        }

        // 开始运行原子化物品比较
        ArrayList<IStackType> changedItem = new ArrayList<>();
        ArrayList<Long> changedCount = new ArrayList<>();
        // 深克隆2个缓存数组
        ArrayList<IStackType> cacheLast = new ArrayList<>();
        for(IStackType stack : this.lastItemStorage)
        {
            cacheLast.add(stack.copy());
        }
        ArrayList<IStackType> cacheNow = new ArrayList<>();
        for(IStackType stack : this.unifiedStorage.getStorage())
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


    public void refreshLast()
    {
        this.lastItemStorage.clear();
        for(IStackType stack : this.unifiedStorage.getStorage())
        {
            this.lastItemStorage.add(stack.copy());
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
            ArrayList<IStackType> storage = new ArrayList<>(this.unifiedStorage.getStorage()); // 当前存储空间的浅克隆

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

    // 自定义点击操作
    public void customClickHandler(int slotIndex,IStackType clickedStack,int button,boolean shiftDown)
    {
        // 该函数用于处理点击维度存储槽发生的事件
        // 包含shift点击和普通点击
        // 同时，从用到customClickHandler的类中，移除quickMoveStack和OnItemStackedHandle对于维度槽位的操作
        // ps：其实吧。。。有维度槽位才会用到customClickHandler。。。没有维度槽位不需要customClickHandler额外处理
        // 之所以多出customClickHandler是因为维度槽位的容器本体是数组，移除会导致索引变更的闪烁
        UnifiedStorage trueItemStorage = this.unifiedStorage;
        if(shiftDown)
        {
            quickMoveHandle(player,slotIndex,clickedStack,trueItemStorage);
        }
        else
        {
            clickHandle(slotIndex,clickedStack,button,player,trueItemStorage);
        }
    }

    // 自定义的快速移动操作
    // 艹，我自己忘了返回值是什么
    // 此处涉及存储界面的操作，根据不同的物品种类尝试进行不同处理
    protected ItemStack quickMoveHandle(Player player,int slotIndex, IStackType clickStack, UnifiedStorage unifiedStorage)
    {
        ItemStack cacheStack;
        Slot slot = this.slots.get(slotIndex);
        if (slot != null && !clickStack.isEmpty())
        {
            // 物品从存储移动到背包
            if(slot instanceof StoredStackSlot)
            {
                if(clickStack instanceof ItemStackType clickedItem)
                {
                    cacheStack = clickedItem.copyStack();
                    int moveCount = checkCanMoveStackCount(cacheStack, this.lines * 9, this.slots.size(), true);
                    moveCount = Math.min(moveCount,cacheStack.getCount()); // 首先
                    int nowCount = 0;
                    IStackType typedStack = unifiedStorage.getStackByStack(StackCreater.Create(new ItemStackType().getTypeId(),cacheStack.copy(),cacheStack.getCount()));
                    ItemStack nowStack;
                    if(typedStack != null)
                    {
                        nowStack = (ItemStack) typedStack.getStack();
                    }
                    else
                    {
                        return ItemStack.EMPTY;
                    }
                    if(nowStack != null)
                    {
                        nowCount = nowStack.getCount();
                    }
                    moveCount = Math.min(moveCount,nowCount);
                    if(moveCount>=0)
                    {
                        cacheStack.setCount(moveCount);
                        if (!this.moveItemStackTo(cacheStack, this.lines * 9, this.slots.size(), true)) {
                            return ItemStack.EMPTY;
                        }
                        unifiedStorage.extract(StackCreater.Create(new ItemStackType().getTypeId(), clickStack.copyStackWithCount(moveCount),moveCount) ,false);
                    }
                }
                else
                {
                    cacheStack = ItemStack.EMPTY;
                }
            }
            else // 物品由背包移动到存储
            {
                cacheStack = slot.getItem().copy();
                unifiedStorage.insert(StackCreater.Create(new ItemStackType().getTypeId(), cacheStack.copy(),cacheStack.getCount()),false);
                slot.tryRemove(cacheStack.getCount(),Integer.MAX_VALUE-1,player);
            }
            if (cacheStack.isEmpty()) {
                // 对于维度网络通过玩家设置一个EMPTY无影响
                // 对于背包槽位可以用于清空当前槽位物品
                // 对于双方，都可以设置脏数据请求保存
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return ItemStack.EMPTY;
    }

    // 自定义的非快速移动操作
    protected void clickHandle(int slotIndex,IStackType clickStack, int button, Player player, UnifiedStorage unifiedStorage)
    {
        ItemStack carriedItem = this.getCarried().copy();// getCarried方法获取直接引用，所以需要copy防止误操作
        StoredStackSlot slot = (StoredStackSlot) this.slots.get(slotIndex);// clickHandle仅用于处理点击维度槽位的逻辑，如果转换失败，则证明调用逻辑出错

        if (clickStack.isEmpty())
        {
            if (!carriedItem.isEmpty())
            {   //槽位物品为空，携带物品存在，将携带物品插入槽位
                int changedCount = button == GLFW.GLFW_MOUSE_BUTTON_LEFT ? carriedItem.getCount() : 1;
                unifiedStorage.insert(StackCreater.Create(new ItemStackType().getTypeId(), carriedItem.copyWithCount(changedCount),changedCount),false);
                int newCount = carriedItem.getCount() - changedCount;
                if(newCount <=0)
                {
                    setCarried(ItemStack.EMPTY);
                }
                else
                {
                    ItemStack newCarriedItem = carriedItem.copy();
                    newCarriedItem.setCount(newCount);
                    setCarried(newCarriedItem);
                }
            }
        }
        else if (slot.mayPickup(player))
        {
            if(clickStack instanceof ItemStackType clickItem)
            {
                if (carriedItem.isEmpty())
                {   //槽位物品存在，携带物品为空，尝试取出槽位物品

                    // 确保一次取出最大不得超过原版数量
                    int woundChangeNum = (int) Math.min(clickItem.getStackAmount(), clickItem.getVanillaMaxStackSize());
                    int actualChangeNum = button == GLFW.GLFW_MOUSE_BUTTON_LEFT ? woundChangeNum : (woundChangeNum + 1) / 2;
                    ItemStack takenItem = ((ItemStack) unifiedStorage.extract(new ItemStackType(clickItem.copyStackWithCount(actualChangeNum)),false).getStack()).copy();
                    if(takenItem != null)
                    {
                        setCarried(takenItem);
                        unifiedStorage.onChange();
                    }
                }
                else if (slot.mayPlace(carriedItem))
                {   //槽位物品存在，携带物品存在，物品可以放置，尝试将物品放入
                    int changedCount = button == GLFW.GLFW_MOUSE_BUTTON_LEFT ? carriedItem.getCount() : 1;
                    unifiedStorage.insert(StackCreater.Create(new ItemStackType().getTypeId(),carriedItem.copyWithCount(changedCount),changedCount),false);
                    int newCount = carriedItem.getCount() - changedCount;
                    if(newCount <=0)
                    {
                        setCarried(ItemStack.EMPTY);
                    }
                    else
                    {
                        ItemStack newCarriedItem = carriedItem.copy();
                        newCarriedItem.setCount(newCount);
                        setCarried(newCarriedItem);
                    }
                }
                else if (clickStack.isSameTypeSameComponents(new ItemStackType(carriedItem.copy())))
                {   // 槽位物品存在，携带物品存在，物品不可放置，为完全相同的物品
                    // 此情况在点击维度存储槽时永远不可能发生，如果发生，无需处理
                    // 原版逻辑为取出物品到最大上限
                    // 保留此情况以便后续使用
                }
            }
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index)
    {
        return ItemStack.EMPTY;
    }

    // 将stack传输到所有能找到的槽位
    @Override
    protected boolean moveItemStackTo(ItemStack stack, int startIndex, int endIndex, boolean reverseDirection)
    {
        boolean flag = false;
        int i = startIndex;
        if (reverseDirection) {
            i = endIndex - 1;
        }

        while(!stack.isEmpty()) {
            if (reverseDirection) {
                if (i < startIndex) {
                    break;
                }
            } else if (i >= endIndex) {
                break;
            }
            //对普通槽位和存储槽位分开处理
            if(this.slots.get(i) instanceof StoredStackSlot)
            {
                // 物品全部移动到存储，然后手动退出
                unifiedStorage.insert(StackCreater.Create(new ItemStackType().getTypeId(), stack.copy(),stack.getCount()),false);
                flag = true;
                break;
            }
            else
            {
                Slot slot = this.slots.get(i);
                ItemStack itemstack = slot.getItem();
                // 填充同物品槽位
                if (!itemstack.isEmpty() && ItemStack.isSameItemSameComponents(stack, itemstack)) {
                    int j = itemstack.getCount() + stack.getCount();
                    int k = slot.getMaxStackSize(itemstack);
                    if (j <= k) {
                        stack.setCount(0);
                        itemstack.setCount(j);
                        slot.setChanged();
                        flag = true;
                    } else if (itemstack.getCount() < k) {
                        stack.shrink(k - itemstack.getCount());
                        itemstack.setCount(k);
                        slot.setChanged();
                        flag = true;
                    }
                }
                // 填充空槽位
                if (itemstack.isEmpty() && slot.mayPlace(stack)) {
                    int l = slot.getMaxStackSize(stack);
                    slot.setByPlayer(stack.split(Math.min(stack.getCount(), l)));
                    slot.setChanged();
                    flag = true;
                }
            }
            if (reverseDirection) {
                --i;
            } else {
                ++i;
            }
        }
        return flag;
    }

    // 检测全部目标槽位，总共最多能容纳多少个给定种类的物品
    protected int checkCanMoveStackCount(ItemStack stack, int startIndex, int endIndex, boolean reverseDirection)
    {
        int flag = 0;
        int i = startIndex;
        if (reverseDirection) {
            i = endIndex - 1;
        }

        while(!stack.isEmpty()) {
            if (reverseDirection) {
                if (i < startIndex) {
                    break;
                }
            } else if (i >= endIndex) {
                break;
            }
            //对普通槽位和存储槽位分开处理
            if(this.slots.get(i) instanceof StoredStackSlot)
            {
                // 最多可以向维度存储移动多少物品？
                flag = stack.getMaxStackSize();
                break;
            }
            else
            {
                // 最多可以向背包填充多少？
                Slot slot = this.slots.get(i);
                ItemStack itemstack = slot.getItem();
                if (!itemstack.isEmpty() && ItemStack.isSameItemSameComponents(stack, itemstack)) {
                    int k = slot.getMaxStackSize(itemstack);    //槽位可以放入的最大数
                    int maxCanPut = k - itemstack.getCount();
                    flag += maxCanPut;
                }
                if (itemstack.isEmpty() && slot.mayPlace(stack)) {
                    int l = slot.getMaxStackSize(stack);
                    flag += l;
                }
            }
            if (reverseDirection) {
                --i;
            } else {
                ++i;
            }
        }
        return flag;
    }

    public static class ItemStackedOnOtherHandler
    {
        /**
         * 通过此事件覆写以阻止原有的点击逻辑操作StoredItemStackSlot<br>
         * 详细逻辑见{@link net.minecraft.world.inventory.AbstractContainerMenu}的doClick方法对tryItemClickBehaviourOverride的使用
         * @param event 传入的事件，提供一系列基本参数 包括 持有的物品 要处理的物品 正处理的槽位 点击动作 玩家 持有的物品的槽位，不过此处均未用到
         */
        @SubscribeEvent
        public void OnItemStackedHandle(ItemStackedOnOtherEvent event)
        {
            event.setCanceled(event.getSlot() instanceof StoredStackSlot);
        }
    }
}

