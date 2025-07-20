package com.wintercogs.beyonddimensions.Menu;

import com.google.common.base.Suppliers;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.IStackType;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.ItemStackType;
import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.Menu.Slot.AbstractStackTypedSlot;
import com.wintercogs.beyonddimensions.Menu.Slot.SlotGroupSync;
import com.wintercogs.beyonddimensions.Packet.QuickDataTagPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.anti_ad.mc.ipn.api.IPNIgnore;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Supplier;

// 定义一些用于 超越维度 模组的ui界面的基本方法。
// 主要是重写网络同步和点击事件，确保父类机制不处理StoredStackSlot的相关内容
@IPNIgnore
public abstract class BDBaseMenu extends AbstractContainerMenu
{

    public final Player player;
    // 用于快速移动时标记玩家背包的槽位索引 如 索引从0开始 背包为54~89
    public int inventoryStartIndex = -1; //索引开始位置 为54
    public int inventoryEndIndex = -1;   //索引结束位置+1 为90

    // 原版槽位想要进行快速转移时的目标槽位范围
    protected int vanillaQuickMoveStartIndex = -1;
    protected int vanillaQuickMoveEndIndex = -1;

    private boolean init = false; // 需要在客户端Menu完成时才能向其发送的操作是否完成的标志
    protected List<AbstractStackTypedSlot> updatedSlots = new ArrayList<>(); // 用于槽位更新
    public List<SlotGroupSync> slotGroupSyncs = new ArrayList<>();

    public boolean isHanding = false; // 用于标记当前是否向服务端发出操作请求却未得到回应 true表示无正在处理未回应，false表示空闲



    protected BDBaseMenu(@Nullable MenuType<?> menuType, int containerId, Inventory playerInventory)
    {
        super(menuType, containerId);
        this.player = playerInventory.player;
    }

    protected void addSlotGroupSync(SlotGroupSync slotGroupSync)
    {
        slotGroupSyncs.add(slotGroupSync);
    }

    @Override
    protected Slot addSlot(Slot slot)
    {
        if(slot instanceof AbstractStackTypedSlot sSlot)
            updatedSlots.add(sSlot);
        return super.addSlot(slot);
    }

    @Override
    public void broadcastChanges()
    {
        // 在原版方法上剔除了对StoredStackSlot的处理
        for(int i = 0; i < this.slots.size(); ++i) {
            Slot slot = this.slots.get(i);
            if(slot instanceof AbstractStackTypedSlot)
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

        // 确保自定义同步不会被客户端调用
        if(!player.level().isClientSide())
        {
            if(!init)
            {
                initUpdate();
                init = true;
            }

            if(shouldSendQuickData())
            {
                CompoundTag updateTag = new CompoundTag();
                writeQuickDataTag(updateTag);
                PacketDistributor.sendToPlayer((ServerPlayer) player,new QuickDataTagPacket(updateTag));
            }

            setSlotGroupSyncsUpdate();
            abstractSlotsUpdate();
            updateChange();
        }
    }

    // 什么时候应该发送快速更新
    // 你也可以用这个来阻止某一端发送更新
    protected boolean shouldSendQuickData()
    {
        return false;
    }

    // 双端可用的快速更新
    protected void writeQuickDataTag(CompoundTag tag)
    {

    }

    // 双端可用的快速读取
    public void readQuickDataTag(CompoundTag tag)
    {

    }

    public void writeAndSendQuickData()
    {
        CompoundTag updateTag = new CompoundTag();
        writeQuickDataTag(updateTag);
        if(player.level().isClientSide())
        {
            PacketDistributor.sendToServer(new QuickDataTagPacket(updateTag));
        }
        else
        {
            PacketDistributor.sendToPlayer((ServerPlayer)player, new QuickDataTagPacket(updateTag));
        }
    }

    // 槽位更新
    protected void abstractSlotsUpdate()
    {
        for(AbstractStackTypedSlot slot : updatedSlots)
        {
            slot.updateChange();
        }
    }

    // 槽位组更新
    protected void setSlotGroupSyncsUpdate()
    {
        for(SlotGroupSync slotGroupSync : slotGroupSyncs)
        {
            slotGroupSync.updateChange();
        }
    }

    // 仅由服务端发送的更新
    protected void updateChange()
    {

    }

    // 仅由服务端发送一次的更新
    protected void initUpdate()
    {

    }

    // 自定义点击操作
    public void customClickHandler(int slotIndex, IStackType clickedStack, int button, boolean shiftDown)
    {
        if(inventoryStartIndex <0 || inventoryEndIndex <0)
            BeyondDimensions.LOGGER.info("警告:背包索引设置错误！！！");

        if(slots.get(slotIndex) instanceof AbstractStackTypedSlot slot)
        {
            if(shiftDown)
                slot.quickMove(clickedStack,button,player);
            else
                slot.click(clickedStack,button,player);
        }
        else
        {
            // 用于处理原版槽位的快速转移
            if(shiftDown && vanillaQuickMoveEndIndex>=0 && vanillaQuickMoveStartIndex>=0 && vanillaQuickMoveStartIndex < vanillaQuickMoveEndIndex)
            {
                quickMoveHandle(player,slotIndex,clickedStack,vanillaQuickMoveStartIndex,vanillaQuickMoveEndIndex);
            }
        }
    }

    // 处理非AbstractStackTypedSlot槽位的快速转移
    protected ItemStack quickMoveHandle(Player player,int slotIndex, IStackType clickStack, int targetStartIndex, int targetEndIndex)
    {
        Slot slot = this.slots.get(slotIndex);
        if (slot != null && !clickStack.isEmpty()) // 根据客户端信息，无视空槽或者null
        {
            ItemStack cacheStack;
            // 快速合成处理
            if(slot instanceof ResultSlot resultSlot)
            {
                cacheStack = slot.getItem().copy(); // 完成数据包校验
                // !slot.getItem().isEmpty()可以预防除数为0，而后面的则是确定循环执行的次数
                for(int i = 0;  !slot.getItem().isEmpty() && i< slot.getItem().getMaxStackSize()/slot.getItem().getCount(); i++)
                {
                    // 尝试将物品分别插入背包和快速转移区间，并记录回滚信息
                    ItemStack remaining = cacheStack.copy();
                    Map<Integer,Integer> insertedToInv = new HashMap<>();
                    Map<Integer,Integer> insertedToSlots = new HashMap<>();
                    for(int invSlot = inventoryStartIndex; invSlot < inventoryEndIndex && !remaining.isEmpty(); invSlot++)
                    {
                        Slot targetSlot = slots.get(invSlot);
                        // safeInsert会破坏stack的数量，因此放入一个copy
                        int newSize = targetSlot.safeInsert(remaining.copy()).getCount();
                        if((remaining.getCount() - newSize) != 0)
                            insertedToInv.put(invSlot, remaining.getCount() - newSize);
                        remaining.setCount(newSize);
                    }

                    // 处理剩余物品
                    for(int targetSlotIndex = targetStartIndex; targetSlotIndex < targetEndIndex && !remaining.isEmpty(); targetSlotIndex++)
                    {
                        Slot targetSlot = slots.get(targetSlotIndex);
                        int newSize;
                        if(targetSlot instanceof AbstractStackTypedSlot aTargetSlot)
                        {
                            // 此处是安全的转换，remaining不会超出int
                            // AbstractStackTypedSlot槽位的safeInsert是安全的，并且new ItemStackType会进行被动copy
                            newSize = (int)aTargetSlot.safeInsert(new ItemStackType(remaining)).getStackAmount();
                        }
                        else
                        {
                            newSize = targetSlot.safeInsert(remaining.copy()).getCount();
                        }
                        if((remaining.getCount() - newSize) != 0)
                            insertedToSlots.put(targetSlotIndex, remaining.getCount() - newSize);
                        remaining.setCount(newSize);
                    }


                    if(remaining.isEmpty()) // 如果产物被完整取出，则通知resultSlot槽位进行onTake，以移除工艺槽上的原料，并执行后续操作
                    {
                        resultSlot.onTake(player, slot.getItem().copy());
                    }
                    else // 没能完整取出所有产物，则将之前插入仓库的物品进行回滚
                    {
                        for(Map.Entry<Integer,Integer> entry : insertedToInv.entrySet())
                        {
                            Slot afterSlot = slots.get(entry.getKey());
                            afterSlot.safeTake(entry.getValue(),Integer.MAX_VALUE,player);
                        }
                        for(Map.Entry<Integer,Integer> entry : insertedToSlots.entrySet())
                        {
                            Slot afterSlot = slots.get(entry.getKey());
                            if(afterSlot instanceof AbstractStackTypedSlot aSlot)
                            {
                                aSlot.safeExtract(new ItemStackType(cacheStack.copyWithCount(entry.getValue())));
                            }
                            else
                            {
                                afterSlot.safeTake(entry.getValue(),Integer.MAX_VALUE,player);
                            }
                        }
                    }

                }

            }
            else // 普通快速移动处理
            {
                cacheStack = slot.getItem().copy(); // 完成数据包校验
                ItemStack remaining = cacheStack.copy();
                for(int targetSlotIndex = targetStartIndex; targetSlotIndex < targetEndIndex && !remaining.isEmpty(); targetSlotIndex++)
                {
                    Slot targetSlot = slots.get(targetSlotIndex);
                    int newSize;
                    if(targetSlot instanceof AbstractStackTypedSlot aTargetSlot)
                    {
                        // 此处是安全的转换，remaining不会超出int
                        newSize = (int)aTargetSlot.safeInsert(new ItemStackType(remaining)).getStackAmount();
                    }
                    else
                    {
                        newSize = targetSlot.safeInsert(remaining).getCount();
                    }
                    remaining.setCount(newSize);
                }
                slot.tryRemove(cacheStack.getCount() - remaining.getCount(),Integer.MAX_VALUE,player);
            }

            slot.setChanged();
        }
        return ItemStack.EMPTY;
    }


    // 仅标记，需要时重写
    @Override
    public void broadcastFullState()
    {
        super.broadcastFullState();
    }

    // 完全重写快速移动方案
    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex)
    {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean moveItemStackTo(ItemStack stack, int startIndex, int endIndex, boolean reverseDirection)
    {
        return false;
    }

    // 重写
    @Override
    public abstract boolean stillValid(Player player);

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot)
    {
        if(!(slot instanceof AbstractStackTypedSlot))
            return super.canTakeItemForPickAll(stack, slot);
        return false;
    }

}
