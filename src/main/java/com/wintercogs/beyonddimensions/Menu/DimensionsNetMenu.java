package com.wintercogs.beyonddimensions.Menu;

import com.google.common.base.Suppliers;
import com.mojang.logging.LogUtils;
import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.DataBase.*;
import com.wintercogs.beyonddimensions.Menu.Slot.StoredItemStackSlot;
import com.wintercogs.beyonddimensions.Packet.ItemStoragePacket;
import com.wintercogs.beyonddimensions.Packet.SlotIndexPacket;
import com.wintercogs.beyonddimensions.Packet.SyncItemStoragePacket;
import com.wintercogs.beyonddimensions.Unit.Pinyin4jUtils;
import io.netty.buffer.Unpooled;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.client.Minecraft;
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
import net.neoforged.neoforge.registries.DeferredRegister;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;

import java.util.*;
import java.util.function.Supplier;

// 备注：该菜单所使用的StoredItemStackSlot的物品堆叠处理将会用钩子进行覆写
// 以itemStorage.getItemStorage()的列表为服务端需要向客户端同步的存储数据
// 客户端会需要完整的正确的itemStorage数据，构建slotIndexList并向服务端同步
// 服务端在收到数据后将利用slotIndexList更新slots列表
// 最后AbstractContainerMenu会自动处理双端slot内容的同步
public class DimensionsNetMenu extends AbstractContainerMenu
{

    private static final Logger LOGGER = LogUtils.getLogger();

    // 双端数据
    public final Player player; //用于给player发送数据
    public final DimensionsItemStorage itemStorage;
    // 用于定期向服务端传输数据
    public ArrayList<Integer> slotIndexList = new ArrayList<>();
    // 客户端数据
    private int lines = 6; //渲染的menu行数
    public int lineData = 0;//从第几行开始渲染？
    public int maxLineData = 0;// 用于记录可以渲染的最大行数，即翻页到底时 当前页面 的第一行位置
    private String searchText = "";
    private HashMap<ButtonName,ButtonState> buttonStateMap = new HashMap<>();
    public boolean isHanding = false; // 用于标记当前是否向服务端发出操作请求却未得到回应
                                        //true表示无正在处理未回应，false表示空闲
    public DimensionsItemStorage viewerItemStorage; // 在客户端，用于显示物品
    // 服务端数据
    private ArrayList<StoredItemStack> lastItemStorage;


    // 构建注册用的信息
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(Registries.MENU, BeyondDimensions.MODID);
    public static final Supplier<MenuType<DimensionsNetMenu>> Dimensions_Net_Menu = MENU_TYPES.register("dimensions_net_menu", () -> IMenuTypeExtension.create(DimensionsNetMenu::new));
    // 我们的辅助函数
    // 我们需要通过IMenuTypeExtension的.create方法才能返回一个menutype，
    // create方法需要传入一个IContainerFactory的内容，而正好我们的构造函数就是IContainerFactory一样的参数。
    // 因为就是这样设计的， 所以传入new就可以了。


    // 客户端构造函数，用于从缓冲区读取数据
    public DimensionsNetMenu(int id, Inventory playerInventory, FriendlyByteBuf data)
    {
        //客户端的DimensionsNet不重要，只需要给予正确的槽索引，Menu会自动将内容同步
        this(id, playerInventory, new DimensionsNet(), new SimpleContainerData(1));
    }

    // 构造函数，用于初始化容器
    public DimensionsNetMenu(int id, Inventory playerInventory, DimensionsNet data, SimpleContainerData uselessContainer)
    {
        super(Dimensions_Net_Menu.get(), id);
        // 初始化维度网络容器
        this.itemStorage = data.getItemStorage();
        viewerItemStorage = new DimensionsNet().getItemStorage(); // 由于服务端不实际需要这个，所以双端都给一个无数据用于初始化即可
        this.player = playerInventory.player;
        if(!player.level().isClientSide())
        {
            // 将lastItemStorage设置为一个深克隆，以便后续进行比较
            this.lastItemStorage = new ArrayList<>();
            for(StoredItemStack storedItemStack : this.itemStorage.getItemStorage())
            {
                this.lastItemStorage.add(new StoredItemStack(storedItemStack));
            }
        }

        // 初始化搜索方案
        buttonStateMap.put(ButtonName.ReverseButton,ButtonState.DISABLED);
        buttonStateMap.put(ButtonName.SortMethodButton,ButtonState.SORT_DEFAULT);

        // 开始添加槽位，其中addSlot会为menu自带的列表slots提供slot，
        // 而给slot本身传入的索引则对应其在背包中的索引
        // 添加维度网络槽位 对应slots索引 0~44
        for (int row = 0; row < lines; ++row)
        {
            for (int col = 0; col < 9; ++col)
            {   // 添加槽而不设置数据
                // 由于我们完全不依靠menu自带得方法来同步，所以可以传入一个和实际数据同步所用不一样的Storage
                // 只需要保证我们能及时把数据从实际数据同步到viewerItemStorage
                // 再将slot点击操作重写的物品种类依赖
                // 我们再也不用把索引表在本地与云端传来传去
                this.addSlot(new StoredItemStackSlot(viewerItemStorage, -1, 8 + col * 18, 29+row * 18));
            }
        }
        // 添加玩家物品栏槽位 对应slots索引 45~71
        for (int row = 0; row < 3; ++row)
        {
            for (int col = 0; col < 9; ++col)
            {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 151 + row * 18));
            }
        }
        // 添加快捷栏槽位 对应slots索引 72~80
        for (int col = 0; col < 9; ++col)
        {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 209));
        }
    }

    public void setHanding()
    {
        this.isHanding = true;
        Thread.ofVirtual().start(() ->{
            try
            {
                Thread.sleep(500);
                Minecraft.getInstance().execute(() -> this.isHanding = false);
            } catch (InterruptedException e)
            {
                throw new RuntimeException(e);
            }

        });
    }

    // 客户端函数，使用当前存储更新视觉存储并重建索引
    public void updateViewerStorage()
    {
        viewerItemStorage.getItemStorage().clear();
        for(StoredItemStack storedItemStack : this.itemStorage.getItemStorage())
        {
            this.viewerItemStorage.addItem(new StoredItemStack(storedItemStack));
        }
        buildIndexList(new ArrayList<>(viewerItemStorage.getItemStorage()));
    }

    public final void ScrollTo()
    {
        Thread.ofVirtual().start(()->{
            Minecraft.getInstance().execute(() -> this.buildIndexList(new ArrayList<>(this.viewerItemStorage.getItemStorage())));
        });
    }

    // 双端函数，用于同步数据
    public void loadSearchText(String text)
    {
        this.searchText = text;
    }
    // 双端函数，用于同步数据
    public void loadButtonState(HashMap<ButtonName,ButtonState> buttonStateMap)
    {
        this.buttonStateMap = buttonStateMap;
    }

    // 辅助方法，用于根据当前搜索状态等建立缓存索引表以及更新翻页数据
    public ArrayList<Integer> buildStorageWithCurrentState(ArrayList<StoredItemStack> itemStorage)
    {
        //建立浅拷贝缓存，用于排序和筛选数据
        ArrayList<StoredItemStack> cache = new ArrayList<>(itemStorage);
        ArrayList<Integer> cacheIndex = new ArrayList<>(); // 建立一个索引缓存，以防止索引混乱
        for(int i = 0;i<cache.size();i++)
            cacheIndex.add(i);

        for (int i = 0; i < cache.size(); i++)
        {
            StoredItemStack item = cache.get(i);
            // 不匹配时，从 cache 和 cacheIndex 中同时移除
            ItemStack itemStack = item.getActualStack();
            if (itemStack == null||itemStack.isEmpty()) {
                // 移除空气和空引用
                cache.remove(i);
                cacheIndex.remove(i);
                i--;  // 移除元素后，需调整索引位置
                continue;
            }
        }

        //根据搜索框筛选数据
        if(searchText != null && !searchText.isEmpty())
        {   // searchText会在传入之前执行小写化操作
            // 遍历缓存，进行筛选
            for (int i = 0; i < cache.size(); i++) {
                StoredItemStack item = cache.get(i);
                // 不匹配时，从 cache 和 cacheIndex 中同时移除
                ItemStack itemStack = item.getActualStack();
                if (itemStack == null||itemStack.isEmpty()) {
                    // 移除空气和空引用
                    cache.remove(i);
                    cacheIndex.remove(i);
                    i--;  // 移除元素后，需调整索引位置
                    continue;
                }
                else
                {
                    boolean isfind = false;
                    String displayName = itemStack.getDisplayName().getString().toLowerCase(Locale.ENGLISH);

                    // 检查 显示名称or全拼or拼音首字母 是否符合
                    if (displayName.contains(searchText) ||
                            Pinyin4jUtils.getAllPinyin(displayName, false).contains(searchText) ||
                            Pinyin4jUtils.getFirstPinYin(displayName).contains(searchText))
                    {
                        isfind = true;
                    }
                    else
                    {
                        // 检查工具提示
                        List<Component> toolTips = itemStack.getTooltipLines(
                                Item.TooltipContext.of(player.level()),
                                player,
                                Minecraft.getInstance().options.advancedItemTooltips ? TooltipFlag.Default.ADVANCED
                                        : TooltipFlag.Default.NORMAL);

                        for (Component tooltip : toolTips) {
                            if (tooltip.getString().toLowerCase(Locale.ENGLISH).contains(searchText)) {
                                isfind = true;
                                break;
                            }
                        }
                    }

                    if (!isfind) {
                        cache.remove(i);
                        cacheIndex.remove(i);
                        i--; // 移除元素后，需调整索引位置
                        continue;
                    }
                }

            }
        }

        //排序按钮筛选
        if(buttonStateMap.get(ButtonName.SortMethodButton)==ButtonState.SORT_DEFAULT)
        {

        }
        else if(buttonStateMap.get(ButtonName.SortMethodButton)==ButtonState.SORT_NAME)
        {
            ArrayList<Integer> sortCache = new ArrayList<>();
            // 按名称排序
            for(int i = 0;i<cache.size();i++)
            {
                sortCache.add(i);
            }
            // 按名称升序，首先根据cache名称排序缓存数组，然后通过缓存数组排序cacheIndex，最后让cache根据名称自排序
            Collections.sort(sortCache, Comparator.comparing(index -> cache.get(index).getItemStack().getDisplayName().getString())); // 按名称排序
            ArrayList<Integer> toSortCacheIndex = new ArrayList<>(cacheIndex);
            for(int i = 0;i<cacheIndex.size();i++)
            {
                cacheIndex.set(i,toSortCacheIndex.get(sortCache.get(i)));
            }
            Collections.sort(cache, Comparator.comparing(item -> item.getItemStack().getDisplayName().getString())); // 升序
        }
        else if(buttonStateMap.get(ButtonName.SortMethodButton)==ButtonState.SORT_QUANTITY)
        {
            ArrayList<Integer> sortCache = new ArrayList<>();
            // 按数量排序
            for(int i = 0;i<cache.size();i++)
            {
                sortCache.add(i);
            }
            // 按数量升序，首先根据cache名称排序缓存数组，然后通过缓存数组排序cacheIndex，最后让cache按数量自排序
            sortCache.sort(Comparator.comparingLong(index -> cache.get(index).getCount())); // 按数量
            ArrayList<Integer> toSortCacheIndex = new ArrayList<>(cacheIndex);
            for(int i = 0;i<cacheIndex.size();i++)
            {
                cacheIndex.set(i,toSortCacheIndex.get(sortCache.get(i)));
            }
            cache.sort(Comparator.comparingLong(StoredItemStack::getCount)); // 升序
        }

        //倒序按钮筛选
        if(buttonStateMap.get(ButtonName.ReverseButton)==ButtonState.ENABLED)
        {
            Collections.reverse(cacheIndex);  // 反转 cacheIndex
            Collections.reverse(cache);       // 反转 cache
        }
        return cacheIndex;
    }

    public void updateScrollLineData(int dataSize)
    {
        maxLineData = dataSize / 9 ;
        if(dataSize % 9 !=0)
        {
            maxLineData++;
        }
        maxLineData -= lines;
        if(maxLineData < 0)
        {
            maxLineData = 0;
        }

        if (lineData < 0)
        {
            lineData = 0;
        }
        if (lineData > maxLineData)
        {
            lineData = maxLineData;
        }
    }

    // 客户端函数，根据存储构建索引表 用于在动态搜索以及其他
    public void buildIndexList(ArrayList<StoredItemStack> itemStorage)
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

        loadIndexList(indexList);
        // 4 同步数据
        //PacketDistributor.sendToServer(new SlotIndexPacket((ArrayList<Integer>) indexList.clone()));
        // 将当前索引值传给服务器，服务端确认后会传回给客户端以应用
    }

    public ArrayList<Integer> buildIndexListNoPacket(ArrayList<StoredItemStack> itemStorage)
    {
        if(!this.player.level().isClientSide())
        {
            return null;
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

        return new ArrayList<>(indexList);
    }

    public void sendIndexToSever()
    {
        PacketDistributor.sendToServer(new SlotIndexPacket((ArrayList<Integer>) slotIndexList.clone()));
    }

    @Override
    public void broadcastChanges()
    {
        for(int i = 0; i < this.slots.size(); ++i) {
            Slot slot = (Slot)this.slots.get(i);
            if(slot instanceof StoredItemStackSlot)
                continue; // 不允许broadcastChanges自动同步StoredItemStackSlot以便自定义处理
            ItemStack itemstack = (slot).getItem();
            Objects.requireNonNull(itemstack);
            Supplier<ItemStack> supplier = Suppliers.memoize(itemstack::copy);
            this.triggerSlotListeners(i, itemstack, supplier);
            this.synchronizeSlotToRemote(i, itemstack, supplier);
        }

        this.synchronizeCarriedToRemote();

        for(int j = 0; j < this.dataSlots.size(); ++j) {
            DataSlot dataslot = (DataSlot)this.dataSlots.get(j);
            int k = dataslot.get();
            if (dataslot.checkAndClearUpdateFlag()) {
                this.updateDataSlotListeners(j, k);
            }

            this.synchronizeDataSlotToRemote(j, k);
        }

        // 开始运行原子化物品比较
        ArrayList<StoredItemStack> changedItem = new ArrayList<>();
        ArrayList<Integer> changedCount = new ArrayList<>();
        // 深克隆2个缓存数组
        ArrayList<StoredItemStack> cacheLast = new ArrayList<>();
        for(StoredItemStack storedItemStack : this.lastItemStorage)
        {
            cacheLast.add(new StoredItemStack(storedItemStack));
        }
        ArrayList<StoredItemStack> cacheNow = new ArrayList<>();
        for(StoredItemStack storedItemStack : this.itemStorage.getItemStorage())
        {
            cacheNow.add(new StoredItemStack(storedItemStack));
        }
        // 缓存结束后，立刻更新last列表
        this.lastItemStorage.clear();
        for(StoredItemStack storedItemStack : this.itemStorage.getItemStorage())
        {
            this.lastItemStorage.add(new StoredItemStack(storedItemStack));
        }

        // 接下来进行根据物品种类进行比较（即将List使用近似Map的比较方法），种类变化加入changedItem，数量变化加入changedCount，数量变化使用Now-Last
        // 注意要处理last和now中可能导致的由于种类变化不同而改变了索引总数。对于Last有，而Now没有的种类意味着种类加入changedItem，数量为0-Last。反之则为种类加入changedItem，数量为Now-0
        // 对于StoredItemStack类，equals方法可以有效比较2个物品是否为统一种类而不计较数量
        // StoredItemStack.getCount可以获取数量进行进一步比较
        // 创建合并数量Map（处理同类物品数量叠加）

        // 注：以下代码为deepseek提供，未经检验
        Map<StoredItemStack, Integer> lastMap = new HashMap<>();
        for (StoredItemStack item : cacheLast) {
            boolean found = false;
            for (StoredItemStack key : lastMap.keySet()) {
                if (key.equals(item)) {
                    lastMap.put(key, (int) (lastMap.get(key) + item.getCount()));
                    found = true;
                    break;
                }
            }
            if (!found) {
                lastMap.put(new StoredItemStack(item), (int) item.getCount());
            }
        }

        Map<StoredItemStack, Integer> nowMap = new HashMap<>();
        for (StoredItemStack item : cacheNow) {
            boolean found = false;
            for (StoredItemStack key : nowMap.keySet()) {
                if (key.equals(item)) {
                    nowMap.put(key, (int) (nowMap.get(key) + item.getCount()));
                    found = true;
                    break;
                }
            }
            if (!found) {
                nowMap.put(new StoredItemStack(item), (int) item.getCount());
            }
        }

        // 比较两个Map的差异
        Set<StoredItemStack> allKeys = new HashSet<>();
        allKeys.addAll(lastMap.keySet());
        allKeys.addAll(nowMap.keySet());

        for (StoredItemStack key : allKeys) {
            int lastCount = lastMap.getOrDefault(key, 0);
            int nowCount = nowMap.getOrDefault(key, 0);
            int delta = nowCount - lastCount;

            if (delta != 0) {
                changedItem.add(key);
                changedCount.add(delta);
            }
        }
        if (!changedItem.isEmpty())
        {
            PacketDistributor.sendToPlayer((ServerPlayer) player,new SyncItemStoragePacket(changedItem,changedCount));
        }

    }

    // 在手动执行物品增添后使用此函数可以阻止自动远程同步
    public void refreshLast()
    {
        this.lastItemStorage.clear();
        for(StoredItemStack storedItemStack : this.itemStorage.getItemStorage())
        {
            this.lastItemStorage.add(new StoredItemStack(storedItemStack));
        }
    }

    // 服务端函数，用于将存储空间完整发到客户端
    public void sendStorage()
    {
        // 只有服务端才能发送存储数据给客户端
        if(player instanceof ServerPlayer)
        {
            ArrayList<ItemStoragePacket> splitPackets = new ArrayList<>(); // 用于分割包的列表
            ArrayList<StoredItemStack> currentBatch = new ArrayList<>(); // 用于临时存储每个包的StoredItemStack
            ArrayList<Integer> currentIndices = new ArrayList<>(); // 用于精确记录索引，防止因网络延时导致错误
            int currentPayloadSize = 0; // 当前包大小
            final int MAX_PAYLOAD_SIZE = 900000; // 单个包最大大小  服务端发送到客户端的包不能大于1MiB 此处留下100KB冗余
            ArrayList<StoredItemStack> storage = new ArrayList<>(this.itemStorage.getItemStorage()); // 当前存储空间的浅克隆

            for(int i = 0;i<storage.size();i++)
            {
                // 计算此次添加的字节数
                StoredItemStack stack = storage.get(i);
                FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer()); // 创建一个 Netty 的 ByteBuf
                RegistryFriendlyByteBuf registryBuf = new RegistryFriendlyByteBuf(buf, player.level().registryAccess());
                StoredItemStack.STREAM_CODEC.encode(registryBuf,stack);
                int entrySize = registryBuf.array().length + Integer.BYTES + 1; // 物品的字节数、索引的字节数、结束标记的字节数

                boolean isLastItem = (i == storage.size() - 1);
                // 如果添加之后包会比最大负载大，则分包，然后把物品添加到下一个包
                if (currentPayloadSize + entrySize >= MAX_PAYLOAD_SIZE) {
                    splitPackets.add(new ItemStoragePacket(
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
                    splitPackets.add(new ItemStoragePacket(
                            new ArrayList<>(currentBatch),
                            new ArrayList<>(currentIndices),
                            true
                    ));
                }
            }
            // 整理数据包，然后一次性发送
            if (!splitPackets.isEmpty()) {
                ItemStoragePacket firstPacket = splitPackets.get(0);
                ItemStoragePacket[] remainingPackets = splitPackets.subList(1, splitPackets.size())
                        .toArray(new ItemStoragePacket[0]);

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
            ((StoredItemStackSlot) slots.get(i)).setTheSlotIndex(list.get(i));
        }
    }

    @Override
    public boolean stillValid(Player player)
    {
        return true; // 可根据需求修改条件
    }

    public ArrayList<StoredItemStack> customClickHandler(int slotIndex,ItemStack clickItem,int button,boolean shiftDown)
    {
        // 该函数用于处理点击维度存储槽发生的事件
        // 包含shift点击和普通点击
        // 同时，从用到customClickHandler的类中，移除quickMoveStack和OnItemStackedHandle对于维度槽位的操作
        // ps：其实吧。。。有维度槽位才会用到customClickHandler。。。没有维度槽位不需要customClickHandler额外处理
        // 之所以多出customClickHandler是因为维度槽位的容器本体是数组，移除会导致索引变更的闪烁
        DimensionsItemStorage trueItemStorage = this.itemStorage;
        if(shiftDown)
        {
            quickMoveHandle(player,slotIndex,clickItem,trueItemStorage);
        }
        else
        {
            clickHandle(slotIndex,clickItem,button,player,trueItemStorage);
        }
        return null;
    }

    protected ItemStack quickMoveHandle(Player player,int slotIndex, ItemStack clickItem, DimensionsItemStorage itemStorage)
    {
        ItemStack cacheStack;
        Slot slot = this.slots.get(slotIndex);
        if (slot != null && !clickItem.isEmpty())
        {
            // 物品从存储移动到背包
            if(slot instanceof StoredItemStackSlot)
            {
                // moveItemStackTo会改变itemStack1，所以我们给出clickItem的深克隆
                cacheStack = clickItem.copy();
                int moveCount = checkCanMoveStackCount(cacheStack, this.lines * 9, this.slots.size(), true);
                moveCount = Math.min(moveCount,cacheStack.getCount());
                cacheStack.setCount(moveCount);
                if (!this.moveItemStackTo(cacheStack, this.lines * 9, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
                // 最后，为了防止moveItemStackTo结束后itemStack1变成了空物品，取原本未被改变的clickItem配合算出的移除数量来移除物品
                itemStorage.removeItem(clickItem,moveCount);
            }
            else // 物品由背包移动到存储
            {
                cacheStack = slot.getItem().copy();
                itemStorage.addItem(cacheStack,cacheStack.getCount());
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

    protected void clickHandle(int slotIndex,ItemStack clickItem, int button, Player player, DimensionsItemStorage itemStorage)
    {
        ItemStack carriedItem = this.getCarried().copy();// getCarried方法获取直接引用，所以需要copy防止误操作
        StoredItemStackSlot slot = (StoredItemStackSlot) this.slots.get(slotIndex);// clickHandle仅用于处理点击维度槽位的逻辑，如果转换失败，则证明调用逻辑出错

        if (clickItem.isEmpty())
        {
            if (!carriedItem.isEmpty())
            {   //槽位物品为空，携带物品存在，将携带物品插入槽位
                int changedCount = button == GLFW.GLFW_MOUSE_BUTTON_LEFT ? carriedItem.getCount() : 1;
                itemStorage.addItem(carriedItem,changedCount);
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
            if (carriedItem.isEmpty())
            {   //槽位物品存在，携带物品为空，尝试取出槽位物品

                // 确保一次取出最大不得超过原版数量
                int woundChangeNum = Math.min(clickItem.getCount(), clickItem.getMaxStackSize());
                int actualChangeNum = button == GLFW.GLFW_MOUSE_BUTTON_LEFT ? woundChangeNum : (woundChangeNum + 1) / 2;
                ItemStack takenItem = itemStorage.removeItem(clickItem,actualChangeNum);
                if(takenItem != null)
                {
                    setCarried(takenItem);
                    itemStorage.OnChange();
                }
            }
            else if (slot.mayPlace(carriedItem))
            {   //槽位物品存在，携带物品存在，物品可以放置，尝试将物品放入
                int changedCount = button == GLFW.GLFW_MOUSE_BUTTON_LEFT ? carriedItem.getCount() : 1;
                itemStorage.addItem(carriedItem,changedCount);
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
            else if (ItemStack.isSameItemSameComponents(clickItem, carriedItem))
            {   // 槽位物品存在，携带物品存在，物品不可放置，为完全相同的物品
                // 此情况在点击维度存储槽时永远不可能发生，如果发生，无需处理
                // 原版逻辑为取出物品到最大上限
                // 保留此情况以便后续使用
            }
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index)
    {
        return ItemStack.EMPTY;
    }

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
            if(this.slots.get(i) instanceof StoredItemStackSlot)
            {
                // 物品全部移动到存储，然后手动退出
                itemStorage.addItem(stack,stack.getCount());
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
            if(this.slots.get(i) instanceof StoredItemStackSlot)
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
            event.setCanceled(event.getSlot() instanceof StoredItemStackSlot);
        }
    }
}

