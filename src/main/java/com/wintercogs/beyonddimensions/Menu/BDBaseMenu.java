package com.wintercogs.beyonddimensions.Menu;

import com.google.common.base.Suppliers;
import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.DataBase.Handler.IStackTypedHandler;
import com.wintercogs.beyonddimensions.DataBase.Stack.IStackType;
import com.wintercogs.beyonddimensions.DataBase.Stack.ItemStackType;
import com.wintercogs.beyonddimensions.DataBase.Stack.StackCreater;
import com.wintercogs.beyonddimensions.Menu.Slot.StoredStackSlot;
import net.minecraft.entity.player.EntityPlayer;


import javax.annotation.Nullable;
import java.util.Objects;
import java.util.function.Supplier;

// 定义一些用于 超越维度 模组的ui界面的基本方法。
// 主要是重写网络同步和点击事件，确保父类机制不处理StoredStackSlot的相关内容
public abstract class BDBaseMenu extends AbstractContainerMenu
{

    public final IStackTypedHandler storage;
    protected final EntityPlayer player;
    // 用于快速移动时标记玩家背包的槽位索引 如 索引从0开始 背包为54~89
    protected int inventoryStartIndex = -1; //索引开始位置 为54
    protected int inventoryEndIndex = -1;   //索引结束位置+1 为90

    private boolean init = false; // 需要在客户端Menu完成时才能向其发送的操作是否完成的标志

    public boolean isHanding = false; // 用于标记当前是否向服务端发出操作请求却未得到回应 true表示无正在处理未回应，false表示空闲



    protected BDBaseMenu(@Nullable MenuType<?> menuType, int containerId, Inventory playerInventory, @Nullable IStackTypedHandler storage)
    {
        super(menuType, containerId);
        this.player = playerInventory.player;
        this.storage = storage;
    }

    @Override
    public void broadcastChanges()
    {
        // 在原版方法上剔除了对StoredStackSlot的处理
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

        if(!init)
        {
            initUpdate();
            init = true;
        }

        updateChange();
    }

    // 定义菜单如何同步更改
    protected abstract void updateChange();

    protected abstract void initUpdate();

    // 自定义点击操作
    public void customClickHandler(int slotIndex, IStackType clickedStack, int button, boolean shiftDown)
    {
        if(this.storage == null)
            return;

        if(inventoryStartIndex <0 || inventoryEndIndex <0)
            BeyondDimensions.LOGGER.info("警告:背包索引设置错误！！！");

        if(shiftDown)
        {
            quickMoveHandle(player,slotIndex,clickedStack,this.storage);
        }
        else
        {
            clickHandle(slotIndex,clickedStack,button,player,this.storage);
        }
    }

    protected abstract ItemStack quickMoveHandle(Player player,int slotIndex, IStackType clickStack, IStackTypedHandler storage);

    protected abstract void clickHandle(int slotIndex,IStackType clickStack, int button, Player player, IStackTypedHandler storage);

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
                if (!itemstack.isEmpty() && ItemStack.isSameItemSameTags(stack, itemstack)) {
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
                if (!itemstack.isEmpty() && ItemStack.isSameItemSameTags(stack, itemstack)) {
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

    // 重写
    @Override
    public abstract boolean stillValid(Player player);

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot)
    {
        if(!(slot instanceof StoredStackSlot))
            return super.canTakeItemForPickAll(stack, slot);
        return false;
    }


}
