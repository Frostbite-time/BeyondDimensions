package com.wintercogs.beyonddimensions.Menu;

import com.mojang.logging.LogUtils;
import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.DataBase.ButtonState;
import com.wintercogs.beyonddimensions.DataBase.DimensionsItemStorage;
import com.wintercogs.beyonddimensions.DataBase.DimensionsNet;
import com.wintercogs.beyonddimensions.DataBase.StoredItemStack;
import com.wintercogs.beyonddimensions.Menu.Slot.StoredItemStackSlot;
import com.wintercogs.beyonddimensions.Packet.SlotIndexPacket;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.event.ItemStackedOnOtherEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;

import java.util.*;
import java.util.function.Supplier;

// 备注：该菜单所使用的StoredItemStackSlot的物品堆叠处理将会用钩子进行覆写

public class DimensionsNetMenu extends AbstractContainerMenu
{

    private static final Logger LOGGER = LogUtils.getLogger();

    public final Player player; //用于给player发送更新包
    public final DimensionsItemStorage itemStorage;

    private int lines = 5; //渲染的menu行数
    public int lineData = 0;//从第几行开始渲染？
    public ArrayList<Integer> slotIndexList = new ArrayList<>(); // 存储索引的列表，用于在双端传输数据
    private String searchText = "";
    private HashMap<String,ButtonState> buttonStateMap = new HashMap<>();

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
        buttonStateMap.put("ReverseButton",ButtonState.ENABLED);
        buttonStateMap.put("SortMethodButton",ButtonState.SORT_QUANTITY);

        // 开始添加槽位，其中addSlot会为menu自带的列表slots提供slot，
        // 而给slot本身传入的索引则对应其在背包中的索引
        // 添加维度网络槽位 对应slots索引 0~44
        for (int row = 0; row < lines; ++row)
        {
            for (int col = 0; col < 9; ++col)
            {
                this.addSlot(new StoredItemStackSlot(this.itemStorage, col + row * 9, 36 + col * 18, 49+row * 18));
            }
        }
        // 添加玩家物品栏槽位 对应slots索引 45~71
        for (int row = 0; row < 3; ++row)
        {
            for (int col = 0; col < 9; ++col)
            {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 36 + col * 18, 153 + row * 18));
            }
        }
        // 添加快捷栏槽位 对应slots索引 72~80
        for (int col = 0; col < 9; ++col)
        {
            this.addSlot(new Slot(playerInventory, col, 36 + col * 18, 211));
        }
    }

    public final void ScrollTo()
    {
        if (lineData < 0)
        {
            lineData = 0;
        }
        buildIndexList();
    }

    // 双端函数，用于同步数据
    public void loadSearchText(String text)
    {
        this.searchText = text;
    }
    // 双端函数，用于同步数据
    public void loadButtonState(HashMap<String,ButtonState> buttonStateMap)
    {
        this.buttonStateMap = buttonStateMap;
    }

    // 服务端函数，根据存储构建索引表 用于在动态搜索以及其他
    public void buildIndexList()
    {
        if(this.player instanceof LocalPlayer)
        {
            return;//确保只有服务端运行
        }

        //建立浅拷贝缓存，用于排序和筛选数据
        ArrayList<StoredItemStack> cache = new ArrayList<>(this.itemStorage.getItemStorage());
        ArrayList<Integer> cacheIndex = new ArrayList<>(); // 建立一个索引缓存，以防止索引混乱
        for(int i = 0;i<cache.size();i++)
            cacheIndex.add(i);
        //根据搜索框筛选数据
        if(searchText != null && !searchText.isEmpty())
        {
            // 遍历缓存，进行筛选
            for (int i = 0; i < cache.size(); i++) {
                StoredItemStack item = cache.get(i);

                // 判断搜索文本是否包含在物品名称中
                if (item.getVanillaMaxSizeStack().getDisplayName().getString() == null
                        || !item.getVanillaMaxSizeStack().getDisplayName().getString().toLowerCase().contains(searchText.toLowerCase())) {
                    // 不匹配时，从 cache 和 cacheIndex 中同时移除
                    cache.remove(i);
                    cacheIndex.remove(i);
                    i--;  // 移除元素后，需调整索引位置
                }
            }
        }
        //排序按钮筛选
        if(buttonStateMap.get("SortMethodButton")==ButtonState.SORT_DEFAULT)
        {

        }
        else if(buttonStateMap.get("SortMethodButton")==ButtonState.SORT_NAME)
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
        else if(buttonStateMap.get("SortMethodButton")==ButtonState.SORT_QUANTITY)
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
        if(buttonStateMap.get("ReverseButton")==ButtonState.ENABLED)
        {
            Collections.reverse(cacheIndex);  // 反转 cacheIndex
            Collections.reverse(cache);       // 反转 cache
        }

        //填入索引表
        this.slotIndexList.clear();
        for (int i = 0; i < lines * 9; i++)
        {
            //根据翻页数据构建索引列表
            if (i + lineData * 9 < cacheIndex.size())
            {
                int index = cacheIndex.get(i + lineData * 9);
                this.slotIndexList.add(index);
            }
            else
            {
                this.slotIndexList.add(-1); //传入不存在的索引，可以使对应槽位成为空
            }
        }
        updateSlotIndex();
        PacketDistributor.sendToPlayer((ServerPlayer) this.player,new SlotIndexPacket(this.slotIndexList));
        // 发送完数据包后立刻广播更改，而不是等待下一tick的更新。
        // 这可以有效减少槽位更新带来的肉眼可见的闪烁
        // 我要如何避免这件事？
        // 新发现，最好不要手动broadcastChanges，除非你已经完成了在正确时机阻塞数据包的操作
        // 否则，这有可能导致两个broadcastChanges撞上，从而导致客户端崩溃
        //broadcastChanges();
    }

    // 客户端函数，根据传入读取索引表
    public void loadIndexList(ArrayList<Integer> list)
    {
        this.slotIndexList.clear();
        for(int i = 0; i<list.size();i++)
        {
            this.slotIndexList.add(list.get(i));
        }
        updateSlotIndex();
    }

    // 通用函数，根据索引表更新槽位
    public void updateSlotIndex()
    {
        for (int row = 0; row < lines; ++row)
        {
            for (int col = 0; col < 9; ++col)
            {
                ((StoredItemStackSlot) slots.get(col + row * 9)).setTheSlotIndex(this.slotIndexList.get(col + row * 9));
            }
        }
    }


    @Override
    public void setItem(int slotId, int stateId, ItemStack stack)
    {
        super.setItem(slotId, stateId, stack);
        //menu用于插入物品时的函数
        //此段重写仅保留，便于以后查询

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
                LOGGER.info("循环中");
                if (!this.moveItemStackTo(itemStack1, this.lines * 9, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
                sSlot.remove(itemstack.getCount());
            }
            else
            {
                itemstack = slot.getItem().copy();
                itemStack1 = slot.getItem().copy();
                LOGGER.info("循环中");
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
        buildIndexList();
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
                event.setCanceled(false);
                return;
            }
            LOGGER.info("为StoredItemStackSlot拦截鼠标事件");
            // 必须吐槽一句，这carried和click的对应竟然在menu写反了。。
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
                    int j3 = clickaction == ClickAction.PRIMARY ? clickItem.getCount() : (clickItem.getCount() + 1) / 2;
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
                menu.buildIndexList();//鼠标点击事件完成后，更新槽
            }
            event.setCanceled(true);// 标志事件被成功拦截，无需做其他处理
        }
    }
}

