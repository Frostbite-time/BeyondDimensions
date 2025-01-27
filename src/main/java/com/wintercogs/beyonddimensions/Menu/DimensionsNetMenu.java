package com.wintercogs.beyonddimensions.Menu;

import com.mojang.logging.LogUtils;
import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.DataBase.*;
import com.wintercogs.beyonddimensions.Menu.Slot.StoredItemStackSlot;
import com.wintercogs.beyonddimensions.Packet.ItemStoragePacket;
import com.wintercogs.beyonddimensions.Packet.SlotIndexPacket;
import com.wintercogs.beyonddimensions.Unit.Pinyin4jUtils;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.SlotAccess;
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
    // 服务端数据



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
        this.player = playerInventory.player;

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
                this.addSlot(new StoredItemStackSlot(this.itemStorage, -1, 8 + col * 18, 29+row * 18));
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

    public final void ScrollTo()
    {
        Thread.ofVirtual().start(()->{
            Minecraft.getInstance().execute(this::buildIndexList);
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
    public ArrayList<Integer> buildStorageWithCurrentState()
    {
        //建立浅拷贝缓存，用于排序和筛选数据
        ArrayList<StoredItemStack> cache = new ArrayList<>(this.itemStorage.getItemStorage());
        ArrayList<Integer> cacheIndex = new ArrayList<>(); // 建立一个索引缓存，以防止索引混乱
        for(int i = 0;i<cache.size();i++)
            cacheIndex.add(i);

        //根据搜索框筛选数据
        if(searchText != null && !searchText.isEmpty())
        {   // searchText会在传入之前执行小写化操作
            // 遍历缓存，进行筛选
            for (int i = 0; i < cache.size(); i++) {
                StoredItemStack item = cache.get(i);
                // 不匹配时，从 cache 和 cacheIndex 中同时移除
                ItemStack itemStack = item.getVanillaMaxSizeStack();
                if (itemStack == null||itemStack == ItemStack.EMPTY) {
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
                                Item.TooltipContext.of(Minecraft.getInstance().level),
                                Minecraft.getInstance().player,
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

    // 服务端函数，根据存储构建索引表 用于在动态搜索以及其他
    public void buildIndexList()
    {
        if(!this.player.level().isClientSide())
        {
            return;
        }
        // 1 构建正确的索引数据
        ArrayList<Integer> cacheIndex = buildStorageWithCurrentState();
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


        // 4 同步数据
        PacketDistributor.sendToServer(new SlotIndexPacket((ArrayList<Integer>) indexList.clone()));
        // 将当前索引值传给服务器，服务端确认后会传回给客户端以应用
    }

    public void sendIndexToSever()
    {
        PacketDistributor.sendToServer(new SlotIndexPacket((ArrayList<Integer>) slotIndexList.clone()));
    }

    @Override
    public void broadcastChanges()
    {
        super.broadcastChanges();
    }

    // 服务端函数，用于将存储空间完整发到客户端
    public void sendStorage()
    {
        // 只有服务端才能发送存储数据给客户端
        if(player instanceof ServerPlayer)
        {
            // 分批次将存储空间内容发送给玩家
            List<ItemStoragePacket> splitPackets = new ArrayList<>(); // 用于分割包
            ArrayList<StoredItemStack> currentBatch = new ArrayList<>(); // 用于分割StoredItemStack
            ArrayList<Integer> currentIndexs = new ArrayList<>(); // 用于精确记录索引，防止因网络延时导致错误
            int currentBatchSize = 0; //检测当前包大小
            ArrayList<StoredItemStack> cacheList = new ArrayList<>(this.itemStorage.getItemStorage());
            LOGGER.info("服务端报告：当前存储大小:{}",cacheList.size());
            for(int i = 0;i<cacheList.size();i++)
            {
                StoredItemStack storedItemStack = cacheList.get(i);
                FriendlyByteBuf friendlyByteBuf = new FriendlyByteBuf(Unpooled.buffer()); // 创建一个 Netty 的 ByteBuf
                RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(friendlyByteBuf, player.level().registryAccess());
                StoredItemStack.STREAM_CODEC.encode(buffer,storedItemStack);
                currentBatchSize += buffer.array().length;
                currentBatchSize += Integer.SIZE;
                currentBatchSize += 1;
                if(currentBatchSize<900000) // 为了规避payload小于1MiB的规则，留下了冗余空间以防计算错误
                {
                    // 如果添加了此次数据仍小于900KB则继续添加
                    currentBatch.add(storedItemStack);
                    currentIndexs.add(i);
                    LOGGER.info("添加了第{}个元素",currentBatch.size());
                    if(i+1 == cacheList.size())
                    {
                        // 代表元素已经全部添加，这是最后一个包，将列表添加之后推出循环进入下一个步骤
                        splitPackets.add(new ItemStoragePacket(new ArrayList<>(currentBatch) ,new ArrayList<>(currentIndexs),true));
                        LOGGER.info("包汇报 大小{}",splitPackets.getLast().storedItemStacks().size());

                        break;
                    }
                }
                else
                {   // 如果添加了数据将会大于1MB则准备数据包，然后将数据添加到下一次处理中
                    splitPackets.add(new ItemStoragePacket(new ArrayList<>(currentBatch),new ArrayList<>(currentIndexs),false));
                    LOGGER.info("包汇报 大小{}",splitPackets.getLast().storedItemStacks().size());
                    currentBatch.clear();
                    currentBatch.add(storedItemStack);
                    currentIndexs.clear();
                    currentIndexs.add(i);
                    LOGGER.info("添加了第{}个元素",currentBatch.size());
                    if(i+1 == cacheList.size())
                    {
                        // 代表元素已经全部添加，这是最后一个包，将列表添加之后推出循环进入下一个步骤
                        splitPackets.add(new ItemStoragePacket(new ArrayList<>(currentBatch),new ArrayList<>(currentIndexs),true));
                        LOGGER.info("包汇报 大小{}",splitPackets.getLast().storedItemStacks().size());
                        break;
                    }
                    currentBatchSize = buffer.array().length;
                    currentBatchSize += Integer.SIZE;
                    currentBatchSize += 1;
                }
            }
            LOGGER.info("服务端准备完成，共有包：{}个",splitPackets.size());
            for(ItemStoragePacket packet :splitPackets)
            {
                LOGGER.info("包检验 大小{}",packet.storedItemStacks().size());
                PacketDistributor.sendToPlayer((ServerPlayer) this.player,packet);
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

    // 通用函数，根据索引表更新槽位
    public void updateSlotIndex(ArrayList<Integer> indexList)
    {
        for (int row = 0; row < lines; ++row)
        {
            for (int col = 0; col < 9; ++col)
            {
                ((StoredItemStackSlot) slots.get(col + row * 9)).setTheSlotIndex(indexList.get(col + row * 9));
            }
        }
    }


    @Override
    public void setItem(int slotId, int stateId, ItemStack stack)
    {
        super.setItem(slotId, stateId, stack);
        //LOGGER.info("setItem被调用");
        //menu用于插入物品时的函数
        //此段重写仅保留，便于以后查询
        //虽然没有太多研究，但这个函数很可能会在远端同步时候调用
        //经过排查，确实会在远程同步时候被客户端调用（服务端初始化是否调用未排查）
    }

    @Override
    public boolean stillValid(Player player)
    {
        return true; // 可根据需求修改条件
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index)
    {
        // 实现快速移动物品逻辑
        // 如果item在容器，移动到背包，在背包则移动到容器
        // 返回被移动的物品对象
        ItemStack itemstack = ItemStack.EMPTY;
        ItemStack itemStack1;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {

            if(slot instanceof StoredItemStackSlot sSlot)
            {
                itemstack = sSlot.getVanillaActualStack();
                itemStack1 = sSlot.getVanillaActualStack();
                if (!this.moveItemStackTo(itemStack1, this.lines * 9, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
                sSlot.remove(itemstack.getCount());
            }
            else
            {
                itemstack = slot.getItem().copy();
                itemStack1 = slot.getItem().copy();
                if (!this.moveItemStackTo(itemStack1, 0, this.lines * 9, false))
                {
                    return ItemStack.EMPTY;
                }
                slot.tryRemove(itemstack.getCount(),Integer.MAX_VALUE-1,player);
            }
            if (itemStack1.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        if(player.level().isClientSide())
        {
            // 非常奇怪，根据调试buildIndexList直接调用会导致下一个tick执行时，本次操作被无效化
            // 通过创建其他线程来创建调用则不会
            // 根据观察，虚拟线程创建到成功调用函数在数百个样本中，少有超过5毫秒，基本上在2毫秒之内
            // 理论上创建其他线程并没有将这次调用挪到下一个tick
            // 所以为什么直接在当前路径直接调用会导致这个问题？太奇怪了
            Thread.ofVirtual().start(()->{
                Minecraft.getInstance().execute(this::buildIndexList);
            });

        }
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

        // 对于可堆叠物品
        if (stack.isStackable()) {
            while(!stack.isEmpty()) {
                if (reverseDirection) {
                    if (i < startIndex) {
                        break;
                    }
                } else if (i >= endIndex) {
                    break;
                }

                //对普通槽位和存储槽位分开处理
                if(this.slots.get(i) instanceof StoredItemStackSlot slot)
                {
                    //为避免重复移动，此处留空
                }
                else
                {   // 填充空槽
                    Slot slot = (Slot)this.slots.get(i);
                    ItemStack itemstack = slot.getItem();
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
                }

                if (reverseDirection) {
                    --i;
                } else {
                    ++i;
                }
            }
        }
        // 如果移动后stack不为空，则继续填充||完成不可堆叠物品的移动
        if (!stack.isEmpty()) {
            if (reverseDirection) {
                i = endIndex - 1;
            } else {
                i = startIndex;
            }

            while(true) {
                if (reverseDirection) {
                    if (i < startIndex) {
                        break;
                    }
                } else if (i >= endIndex) {
                    break;
                }

                if(this.slots.get(i) instanceof StoredItemStackSlot slot)
                {   // 物品由背包移动到存储器
                    slot.setByPlayer(stack);
                    flag = true;
                    break;
                }
                else
                {   // 填充非空槽
                    Slot slot1 = (Slot)this.slots.get(i);
                    ItemStack itemstack1 = slot1.getItem();
                    if (itemstack1.isEmpty() && slot1.mayPlace(stack)) {
                        int l = slot1.getMaxStackSize(stack);
                        slot1.setByPlayer(stack.split(Math.min(stack.getCount(), l)));
                        slot1.setChanged();
                        flag = true;
                        break;
                    }
                }



                if (reverseDirection) {
                    --i;
                } else {
                    ++i;
                }
            }
        }

        return flag;
    }


    public static class ItemStackedOnOtherHandler
    {
        @SubscribeEvent
        public void OnItemStackedHandle(ItemStackedOnOtherEvent event)
        {
            if (!(event.getSlot() instanceof StoredItemStackSlot))
            {
                event.setCanceled(false); // 通知双端点击事件未处理
                return;
            }
            // 必须吐槽一句，这carried和click的对应竟然在menu写反了。。
            // 以后neoforge更新可能还需要重新写这些
            ItemStack clickItem = event.getCarriedItem();
            ItemStack carriedItem = event.getStackedOnItem();
            StoredItemStackSlot slot = (StoredItemStackSlot) event.getSlot();
            Player player = event.getPlayer();
            ClickAction clickaction = event.getClickAction();
            SlotAccess slotAccess = event.getCarriedSlotAccess(); //表示鼠标所携带的slot本身

            //利用覆写处理玩家将物品插入槽位
            //注：对于并非完全替换物品的情况，而更类似添加物品的情况，StoredItemStackSlot不可使用set，而应为setByPlayer
            //前者将直接替换维度网络对应的索引物品，后者会将物品添加到其对应的维度网络
            if (clickItem.isEmpty())
            {
                if (!carriedItem.isEmpty())
                {   //槽位物品为空，携带物品存在，将携带物品插入槽位
                    int i3 = clickaction == ClickAction.PRIMARY ? carriedItem.getCount() : 1;
                    slotAccess.set(slot.safeInsert(carriedItem, i3));
                }
            }
            else if (slot.mayPickup(player))
            {
                if (carriedItem.isEmpty())
                {   //槽位物品存在，携带物品为空，尝试取出槽位物品
                    int num = 0;
                    if(clickItem.getCount() > 64)
                    {
                        num = 64; // 一次取出最大不得超过64，次要按键不得超过32
                    }
                    else
                    {
                        num = clickItem.getCount();
                    }
                    int j3 = clickaction == ClickAction.PRIMARY ? num : (num + 1) / 2;
                    Optional<ItemStack> optional1 = slot.tryRemove(j3, Integer.MAX_VALUE, player);
                    optional1.ifPresent((p_150421_) ->
                    {
                        slotAccess.set(p_150421_);
                        slot.onTake(player, p_150421_);
                    });
                }
                else if (slot.mayPlace(carriedItem))
                {   //槽位物品存在，携带物品存在，物品可以放置，尝试将物品放入
                    int k3 = clickaction == ClickAction.PRIMARY ? carriedItem.getCount() : 1;
                    slotAccess.set(slot.safeInsert(carriedItem, k3));
                }
                else if (ItemStack.isSameItemSameComponents(clickItem, carriedItem))
                {   //槽位物品存在，携带物品存在，物品不可放置，但是为同类型，尝试取出物品到携带槽最大上限
                    Optional<ItemStack> optional = slot.tryRemove(clickItem.getCount(), carriedItem.getMaxStackSize() - carriedItem.getCount(), player);
                    optional.ifPresent((p_150428_) ->
                    {
                        carriedItem.grow(p_150428_.getCount());
                        slot.onTake(player, p_150428_);
                    });
                }
            }
            if(player.containerMenu instanceof DimensionsNetMenu menu)
            {
                if(player.level().isClientSide)
                {
                    Thread.ofVirtual().start(()->{
                        Minecraft.getInstance().execute(menu::buildIndexList);
                    });

                }
            }
            event.setCanceled(true);// 标志事件被成功拦截，无需做其他处理
        }
    }
}

