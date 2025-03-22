package com.wintercogs.beyonddimensions.Menu;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.BlockEntity.Custom.NetInterfaceBlockEntity;
import com.wintercogs.beyonddimensions.DataBase.Handler.IStackTypedHandler;
import com.wintercogs.beyonddimensions.DataBase.Handler.StackTypedHandler;
import com.wintercogs.beyonddimensions.DataBase.Stack.IStackType;
import com.wintercogs.beyonddimensions.DataBase.Stack.ItemStackType;
import com.wintercogs.beyonddimensions.DataBase.Stack.StackCreater;
import com.wintercogs.beyonddimensions.Menu.Slot.StoredStackSlot;
import com.wintercogs.beyonddimensions.Packet.StoragePacket;
import com.wintercogs.beyonddimensions.Packet.SyncFlagPacket;
import com.wintercogs.beyonddimensions.Packet.SyncStoragePacket;
import io.netty.buffer.Unpooled;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.connection.ConnectionType;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

// 网络接口的UI
// 管理一组虚拟槽、以及一组
public class NetInterfaceBaseMenu extends BDOrderedContainerMenu
{
    /// 双端通用数据
    private final Player player; // 打开Menu的玩家实例
    /// 客户端数据
    private int lines = 1; //渲染的menu行数
    public int lineData = 0;//从第几行开始渲染？
    public int maxLineData = 0;// 用于记录可以渲染的最大行数，即翻页到底时 当前页面 的第一行位置
    public boolean isHanding = false; // 用于标记当前是否向服务端发出操作请求却未得到回应 true表示无正在处理未回应，false表示空闲
    public StackTypedHandler viewerStorage; // 在客户端，用于显示物品
    /// 服务端数据
    private ArrayList<IStackType> lastStorage; // 记录截至上一次同步时的存储状态，用于同步数据

    public final StackTypedHandler flagStorage;
    public StackTypedHandler viewerFlagStorage;
    public ArrayList<IStackType> lastFlagStorage;

    public boolean popMode;
    public NetInterfaceBlockEntity be;


    // 构建注册用的信息
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(Registries.MENU, BeyondDimensions.MODID);
    public static final Supplier<MenuType<NetInterfaceBaseMenu>> Net_Interface_Menu = MENU_TYPES.register("net_interface_menu", () -> IMenuTypeExtension.create(NetInterfaceBaseMenu::new));
    // 我们的辅助函数
    // 我们需要通过IMenuTypeExtension的.create方法才能返回一个menutype，
    // create方法需要传入一个IContainerFactory的内容，而正好我们的构造函数就是IContainerFactory一样的参数。
    // 因为就是这样设计的， 所以传入new就可以了。


    /**
     * 客户端构造函数
     * @param playerInventory 玩家背包
     */
    public NetInterfaceBaseMenu(int id, Inventory playerInventory, FriendlyByteBuf data)
    {
        this(id, playerInventory, new StackTypedHandler(9),new StackTypedHandler(9),null,new SimpleContainerData(0));
    }

    /**
     * 服务端构造函数
     * @param playerInventory 玩家背包
     * @param uselessContainer 此处无用，传入new SimpleContainerData(0)即可
     */
    public NetInterfaceBaseMenu(int id, Inventory playerInventory, StackTypedHandler storage , StackTypedHandler flagStorage, NetInterfaceBlockEntity be, SimpleContainerData uselessContainer)
    {
        super(Net_Interface_Menu.get(), id,playerInventory,storage);
        // 初始化维度网络容器
        this.popMode = false;
        viewerStorage = new StackTypedHandler(9); // 由于服务端不实际需要这个，所以双端都给一个无数据用于初始化即可
        this.player = playerInventory.player;
        if(!player.level().isClientSide())
        {
            // 将lastItemStorage设置为一个深克隆，以便后续进行比较
            this.lastStorage = new ArrayList<>();
            for(IStackType stack : this.storage.getStorage())
            {
                this.lastStorage.add(new ItemStackType());
            }
            this.popMode = be.popMode;
            this.be = be;
        }

        // 初始化标记容器
        this.flagStorage = flagStorage;
        viewerFlagStorage = new StackTypedHandler(9);
        if(!player.level().isClientSide())
        {
            // 将lastItemStorage设置为一个深克隆，以便后续进行比较
            this.lastFlagStorage = new ArrayList<>();
            for(IStackType stack : this.flagStorage.getStorage())
            {
                // 将flag的last比较初始化设为空。用broadchange自动同步
                this.lastFlagStorage.add(new ItemStackType());
            }
        }

        // 开始添加槽位，其中addSlot会为menu自带的列表slots提供slot，
        // 而给slot本身传入的索引则对应其在背包中的索引
        // 添加维度网络槽位 对应slots索引 0~53
        for (int col = 0; col < 9; ++col)
        {   // 添加槽而不设置数据
            // 由于我们完全不依靠menu自带得方法来同步，所以可以传入一个和实际数据同步所用不一样的Storage
            // 只需要保证我们能及时把数据从实际数据同步到viewerUnifiedStorage
            // 再将slot点击操作重写的物品种类依赖
            this.addSlot(new StoredStackSlot(viewerStorage, col, 8 + col * 18, 71));
        }

        for(int i=0;i<flagStorage.getStorage().size();i++)
        {
            StoredStackSlot flagSlot = new StoredStackSlot(viewerFlagStorage, i, 8 + i * 18, 53);
            flagSlot.setFake(true);
            this.addSlot(flagSlot);
        }

        inventoryStartIndex = slots.size();
        for (int row = 0; row < 3; ++row)
        {
            for (int col = 0; col < 9; ++col)
            {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 123 + row * 18));
            }
        }

        for (int col = 0; col < 9; ++col)
        {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 181));
        }

        inventoryEndIndex = slots.size();
    }

    /**
     * 客户端专用函数，服务端请勿调用<br>
     * 使用当前客户端的真存储来更新视觉存储，然后重构索引以刷新显示
     */
    public void updateViewerStorage()
    {
        // 清空容器
        for(IStackType stack : viewerStorage.getStorage())
        {
            stack.setStackAmount(-1); //设为空
        }
        for(int i = 0; i<this.storage.getStorage().size(); i++)
        {
            this.viewerStorage.insert(i, storage.getStackBySlot(i) ,false);
        }
        buildIndexList(new ArrayList<>(viewerStorage.getStorage()));

        // flag容器
        for(IStackType stack : viewerFlagStorage.getStorage())
        {
            stack.setStackAmount(-1); //设为空
        }
        for(int i=0;i<this.flagStorage.getStorage().size();i++)
        {
            this.viewerFlagStorage.insert(i,flagStorage.getStackBySlot(i),false);
        }
        buildIndexList(new ArrayList<>(viewerFlagStorage.getStorage()));
    }

    /**
     * 利用当前视觉存储信息重构索引
     * 翻页操作集成在GUI部分
     */
    public final void ScrollTo()
    {
        this.buildIndexList(new ArrayList<>(this.viewerStorage.getStorage()));

        this.buildIndexList(new ArrayList<>(this.viewerFlagStorage.getStorage()));
    }



    /**
     * 根据当前的搜索状态、按钮状态对存储进行排序
     * @param unifiedStorage 要排序的存储
     * @return 完成排序的索引列表
     */
    public ArrayList<Integer> buildStorageWithCurrentState(ArrayList<IStackType> unifiedStorage) {
        // 合并过滤空气和搜索逻辑，避免遍历时删除。
        // 不移除空体，但是移除null。因为null会导致很多可能的崩溃。而空体在此处仍有用处
        ArrayList<IStackType> cache = new ArrayList<>();
        ArrayList<Integer> cacheIndex = new ArrayList<>();
        for (int i = 0; i < unifiedStorage.size(); i++) {
            IStackType stack = unifiedStorage.get(i).copy();
            if (stack == null) continue;

            cache.add(stack);
            cacheIndex.add(i);
        }

        return cacheIndex;
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
        //loadIndexList(indexList);
    }


    @Override
    protected void updateChange()
    {
        // 带槽位索引的原子化物品比较
        ArrayList<IStackType> changedItems = new ArrayList<>();
        ArrayList<Long> changedCounts = new ArrayList<>();
        ArrayList<Integer> changedIndices = new ArrayList<>();

        // 创建带索引的深拷贝缓存
        List<@Nullable IStackType> lastSnapshot = new ArrayList<>();
        for (IStackType stack : this.lastStorage) {
            lastSnapshot.add(stack != null ? stack.copy() : null);
        }

        List<@Nullable IStackType> currentSnapshot = new ArrayList<>();
        for (IStackType stack : this.storage.getStorage()) {
            currentSnapshot.add(stack != null ? stack.copy() : null);
        }

        // 确保两个快照长度一致（处理动态扩容）
        int maxSlots = Math.max(lastSnapshot.size(), currentSnapshot.size());
        while (lastSnapshot.size() < maxSlots) lastSnapshot.add(null);
        while (currentSnapshot.size() < maxSlots) currentSnapshot.add(null);

        // 逐槽位对比
        for (int slot = 0; slot < maxSlots; slot++) {
            IStackType lastStack = lastSnapshot.get(slot);
            IStackType currentStack = currentSnapshot.get(slot);

            // 检查是否需要更新
            boolean stackChanged = false;

            // 情况1：槽位从非空变成空或反之
            if ((lastStack == null) != (currentStack == null)) {
                stackChanged = true;
            }
            // 情况2：两个槽位都有物品，但类型或组件不同
            else if (lastStack != null && currentStack != null) {
                if (!lastStack.isSameTypeSameComponents(currentStack)) {
                    stackChanged = true;
                }
            }

            // 情况3：数量变化（即使类型相同）
            long delta = (currentStack != null ? currentStack.getStackAmount() : 0L)
                    - (lastStack != null ? lastStack.getStackAmount() : 0L);
            if (delta != 0) {
                stackChanged = true;
            }

            // 记录变化
            if (stackChanged) {
                changedIndices.add(slot);
                changedItems.add(currentStack != null ? currentStack.copy() : null);
                changedCounts.add(delta);
            }
        }

        // 如果有变化则发送同步包
        if (!changedIndices.isEmpty()) {
            PacketDistributor.sendToPlayer(
                    (ServerPlayer) player,
                    new SyncStoragePacket(changedItems, changedCounts, changedIndices)
            );
        }

        // 更新最后快照（需要保持与当前相同的槽位数量）
        this.lastStorage.clear();
        this.lastStorage.addAll(currentSnapshot);


        // 同步flag容器（修改后版本）

        // 带槽位索引的物品类型变化检测
        ArrayList<IStackType> changedFlags = new ArrayList<>();
        ArrayList<Integer> changedFlagIndices = new ArrayList<>();

        // 创建带索引的深拷贝缓存（保持原逻辑）
        List<@Nullable IStackType> lastFlagSnapshot = new ArrayList<>();
        for (IStackType stack : this.lastFlagStorage) {
            lastFlagSnapshot.add(stack != null ? stack.copy() : null);
        }

        List<@Nullable IStackType> currentFlagSnapshot = new ArrayList<>();
        for (IStackType stack : this.flagStorage.getStorage()) {
            currentFlagSnapshot.add(stack != null ? stack.copy() : null);
        }

        // 处理动态扩容（保持原逻辑）
        int maxFlagSlots = Math.max(lastFlagSnapshot.size(), currentFlagSnapshot.size());
        while (lastFlagSnapshot.size() < maxFlagSlots) lastFlagSnapshot.add(null);
        while (currentFlagSnapshot.size() < maxFlagSlots) currentFlagSnapshot.add(null);

        for (int slot = 0; slot < maxFlagSlots; slot++) {
            IStackType lastStack = lastFlagSnapshot.get(slot);
            IStackType currentStack = currentFlagSnapshot.get(slot);

            // 修改点1：只检查存在性和类型/组件变化
            boolean stackChanged = false;

            // 情况1：物品存在性变化
            if ((lastStack == null) != (currentStack == null)) {
                stackChanged = true;
            }
            // 情况2：类型或组件变化
            else if (lastStack != null && currentStack != null) {
                if (!lastStack.isSameTypeSameComponents(currentStack)) {
                    stackChanged = true;
                }
            }

            // 修改点2：移除数量变化的检查

            if (stackChanged) {
                changedFlagIndices.add(slot);
                // 携带完整的当前状态（包含最新数量）
                changedFlags.add(currentStack != null ? currentStack.copy() : null);
            }
        }

        if (!changedFlagIndices.isEmpty()) {
            // 修改点3：调整数据包结构（移除counts参数）
            PacketDistributor.sendToPlayer(
                    (ServerPlayer) player,
                    new SyncFlagPacket(changedFlags, changedFlagIndices)
            );
        }

        // 快照更新保持原逻辑
        this.lastFlagStorage.clear();
        this.lastFlagStorage.addAll(currentFlagSnapshot);
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


    @Override
    public boolean stillValid(Player player)
    {
        return true; // 可根据需求修改条件
    }


    // 用于设置虚拟槽位的函数
    public void setFlagSlot(int slotIndex, IStackType clickStack, IStackType flagStack)
    {
        StoredStackSlot slot = (StoredStackSlot) this.slots.get(slotIndex);// clickHandle仅用于处理点击维度槽位的逻辑，如果转换失败，则证明调用逻辑出错

        // 处理虚拟槽位
        if(slot.isFake())
        {
            if(flagStack.isEmpty()&&getCarried().isEmpty())
            {
                //flagStorage.getStorage().set(slot.getSlotIndex(),new ItemStackType());
                flagStorage.setStackDirectly(slot.getSlotIndex(), new ItemStackType());
            }
            else
            {
                flagStorage.setStackDirectly(slot.getSlotIndex(),flagStack);
            }
            return; // 结束处理
        }
    }

    // 自定义的非快速移动操作
    protected void clickHandle(int slotIndex,IStackType clickStack, int button, Player player, IStackTypedHandler storage)
    {
        ItemStack carriedItem = this.getCarried().copy();// getCarried方法获取直接引用，所以需要copy防止误操作
        StoredStackSlot slot = (StoredStackSlot) this.slots.get(slotIndex);// clickHandle仅用于处理点击维度槽位的逻辑，如果转换失败，则证明调用逻辑出错

        // 处理虚拟槽位
        if(slot.isFake())
        {
            if(carriedItem.isEmpty())
            {
                flagStorage.setStackDirectly(slot.getSlotIndex(),new ItemStackType());
            }
            else
            {
                flagStorage.setStackDirectly(slot.getSlotIndex(),new ItemStackType(carriedItem.copyWithCount(1)));
            }
            return; // 结束处理
        }
        else
        {
            super.clickHandle(slotIndex,clickStack,button,player,storage);
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
                storage.insert(StackCreater.Create(ItemStackType.ID, stack.copy(),stack.getCount()),false);
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

}
