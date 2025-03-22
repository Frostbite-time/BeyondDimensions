package com.wintercogs.beyonddimensions.Menu;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.BlockEntity.Custom.NetInterfaceBlockEntity;
import com.wintercogs.beyonddimensions.DataBase.Handler.IStackTypedHandler;
import com.wintercogs.beyonddimensions.DataBase.Handler.StackTypedHandler;
import com.wintercogs.beyonddimensions.DataBase.Stack.IStackType;
import com.wintercogs.beyonddimensions.DataBase.Stack.ItemStackType;
import com.wintercogs.beyonddimensions.Menu.Slot.StoredStackSlot;
import com.wintercogs.beyonddimensions.Packet.PopModeButtonPacket;
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

    public boolean isHanding = false; // 用于标记当前是否向服务端发出操作请求却未得到回应 true表示无正在处理未回应，false表示空闲

    public StackTypedHandler viewerStorage; // 在客户端，用于显示物品
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

        this.popMode = false;
        // 初始化存储容器
        viewerStorage = new StackTypedHandler(9); // 由于服务端不实际需要这个，所以双端都给一个无数据用于初始化即可
        // 初始化标记容器
        this.flagStorage = flagStorage;
        viewerFlagStorage = new StackTypedHandler(9);
        if(!player.level().isClientSide())
        {
            // 初始化lastStorage为全空，以便broadcastChange自动发送初始值
            this.lastStorage = new ArrayList<>();
            for(int i = 0; i < this.storage.getStorage().size(); i++)
            {
                this.lastStorage.add(new ItemStackType());
            }

            // 初始化lastFlagStorage为全空，以便broadcastChange自动发送初始值
            this.lastFlagStorage = new ArrayList<>();
            for(int i = 0; i < this.flagStorage.getStorage().size(); i++)
            {
                this.lastFlagStorage.add(new ItemStackType());
            }

            this.popMode = be.popMode;
            this.be = be;
        }

        // 添加存储槽
        for (int i = 0; i < 9; ++i)
        {
            this.addSlot(new StoredStackSlot(viewerStorage, i, 8 + i * 18, 71));
        }

        // 添加标记槽
        for(int i=0;i<flagStorage.getStorage().size();i++)
        {
            StoredStackSlot flagSlot = new StoredStackSlot(viewerFlagStorage, i, 8 + i * 18, 53);
            flagSlot.setFake(true);
            this.addSlot(flagSlot);
        }

        // 添加背包以及快捷栏
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


    @Override
    protected void updateChange()
    {
        updateStorage();
        updateFlag();
    }

    @Override
    protected void initUpdate()
    {
        PacketDistributor.sendToPlayer((ServerPlayer) player,new PopModeButtonPacket(popMode));
    }

    private void updateStorage()
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



        if (!changedIndices.isEmpty()) {
            // 准备根据列表进行分包处理，每个包不允许大于900KB字节
            List<SyncStoragePacket> packets = new ArrayList<>();
            final int MAX_PACKET_SIZE = 900 * 1024; // 921,600 bytes

            // 预计算所有条目的网络负载
            List<Integer> entrySizes = new ArrayList<>(changedIndices.size());

            // 第一阶段：计算每个条目序列化后的字节数
            for (int i = 0; i < changedIndices.size(); i++) {
                IStackType stack = changedItems.get(i);

                // 创建临时缓冲区计算序列化大小
                FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
                RegistryFriendlyByteBuf registryBuf = new RegistryFriendlyByteBuf(
                        buf,
                        player.level().registryAccess(),
                        ConnectionType.OTHER
                );

                // 序列化物品数据（包括null情况）
                if (stack != null) {
                    stack.serialize(registryBuf);
                }

                // 计算总条目大小 = 槽位索引(4) + 物品数据(n) + 元数据(1)
                int entrySize = Integer.BYTES + buf.readableBytes() + Long.BYTES;
                entrySizes.add(entrySize);
            }

            // 第二阶段：动态分包
            List<Integer> batchIndices = new ArrayList<>();
            List<IStackType> batchItems = new ArrayList<>();
            List<Long> batchCounts = new ArrayList<>();
            int currentBatchSize = 0;

            for (int i = 0; i < changedIndices.size(); i++) {
                int estimatedSize = entrySizes.get(i);

                // 当超过大包限制时提交当前批次
                if (currentBatchSize + estimatedSize > MAX_PACKET_SIZE) {
                    packets.add(new SyncStoragePacket(
                            new ArrayList<>(batchItems),
                            new ArrayList<>(batchCounts),
                            new ArrayList<>(batchIndices)
                    ));

                    batchIndices.clear();
                    batchItems.clear();
                    batchCounts.clear();
                    currentBatchSize = 0;
                }

                // 添加条目到当前批次
                batchIndices.add(changedIndices.get(i));
                batchItems.add(changedItems.get(i));
                batchCounts.add(changedCounts.get(i));
                currentBatchSize += estimatedSize;
            }

            // 提交最后一批
            if (!batchIndices.isEmpty()) {
                packets.add(new SyncStoragePacket(batchItems, batchCounts, batchIndices));
            }

            // 发送所有分包
            for (SyncStoragePacket packet : packets) {
                PacketDistributor.sendToPlayer(
                        (ServerPlayer) player,
                        packet
                );
            }
        }


        // 更新最后快照（需要保持与当前相同的槽位数量）
        this.lastStorage.clear();
        this.lastStorage.addAll(currentSnapshot);
    }

    private void updateFlag()
    {
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
            // 分包处理逻辑
            List<SyncFlagPacket> packets = new ArrayList<>();
            final int MAX_PACKET_SIZE = 900 * 1024; // 921,600 bytes
            // 预计算所有条目的网络负载
            List<Integer> entrySizes = new ArrayList<>(changedFlagIndices.size());

            // 阶段1：计算每个条目序列化后的字节数
            for (int i = 0; i < changedFlagIndices.size(); i++) {
                IStackType stack = changedFlags.get(i);
                // 创建临时缓冲区计算序列化大小
                FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
                RegistryFriendlyByteBuf registryBuf = new RegistryFriendlyByteBuf(
                        buf,
                        player.level().registryAccess(),
                        ConnectionType.OTHER
                );

                // 序列化物品数据（如果存在）
                if (stack != null) {
                    stack.serialize(registryBuf);
                }

                entrySizes.add(registryBuf.readableBytes()+Integer.BYTES);
            }
            // 阶段2：动态分包
            List<Integer> batchIndices = new ArrayList<>();
            List<IStackType> batchFlags = new ArrayList<>();
            int currentBatchSize = 0;
            for (int i = 0; i < changedFlagIndices.size(); i++) {
                int estimatedSize = entrySizes.get(i);

                // 当超过大包限制时提交当前批次
                if (currentBatchSize + estimatedSize > MAX_PACKET_SIZE) {
                    packets.add(new SyncFlagPacket(
                            new ArrayList<>(batchFlags),
                            new ArrayList<>(batchIndices)
                    ));

                    batchIndices.clear();
                    batchFlags.clear();
                    currentBatchSize = 0;
                }

                // 添加条目到当前批次
                batchIndices.add(changedFlagIndices.get(i));
                batchFlags.add(changedFlags.get(i));
                currentBatchSize += estimatedSize;
            }
            // 提交最后一批
            if (!batchIndices.isEmpty()) {
                packets.add(new SyncFlagPacket(batchFlags, batchIndices));
            }
            // 发送所有分包
            for (SyncFlagPacket packet : packets) {
                PacketDistributor.sendToPlayer(
                        (ServerPlayer) player,
                        packet
                );
            }
        }

        // 快照更新保持原逻辑
        this.lastFlagStorage.clear();
        this.lastFlagStorage.addAll(currentFlagSnapshot);
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

        // flag容器
        for(IStackType stack : viewerFlagStorage.getStorage())
        {
            stack.setStackAmount(-1); //设为空
        }
        for(int i=0;i<this.flagStorage.getStorage().size();i++)
        {
            this.viewerFlagStorage.insert(i,flagStorage.getStackBySlot(i),false);
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

}
