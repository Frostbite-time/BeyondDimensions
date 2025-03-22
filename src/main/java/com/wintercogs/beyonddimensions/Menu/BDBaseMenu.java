package com.wintercogs.beyonddimensions.Menu;

import com.google.common.base.Suppliers;
import com.wintercogs.beyonddimensions.DataBase.Handler.IStackTypedHandler;
import com.wintercogs.beyonddimensions.DataBase.Stack.IStackType;
import com.wintercogs.beyonddimensions.Menu.Slot.StoredStackSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Supplier;

// 定义一些用于 超越维度 模组的ui界面的基本方法。
// 主要是重写网络同步和点击事件，确保父类机制不处理StoredStackSlot的相关内容
public abstract class BDBaseMenu extends AbstractContainerMenu
{

    public final IStackTypedHandler storage;
    protected final Player player;



    protected BDBaseMenu(@Nullable MenuType<?> menuType, int containerId, Inventory playerInventory,@Nullable IStackTypedHandler storage)
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

        updateChange();
    }

    // 定义菜单如何同步更改
    protected abstract void updateChange();

    // 自定义点击操作
    public void customClickHandler(int slotIndex, IStackType clickedStack, int button, boolean shiftDown)
    {
        if(this.storage == null)
            return;

        if(shiftDown)
        {
            quickMoveHandle(player,slotIndex,clickedStack,this.storage);
        }
        else
        {
            clickHandle(slotIndex,clickedStack,button,player,this.storage);
        }
    }

    protected abstract ItemStack quickMoveHandle(Player player,int slotIndex, IStackType clickStack, IStackTypedHandler unifiedStorage);

    protected abstract void clickHandle(int slotIndex,IStackType clickStack, int button, Player player, IStackTypedHandler unifiedStorage);

    // 仅标记，需要时重写
    @Override
    public void broadcastFullState()
    {
        super.broadcastFullState();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int i)
    {
        return null;
    }

    @Override
    public boolean stillValid(Player player)
    {
        return false;
    }




//    原用于取消点击实现，后发现在gui类中取消更合适，故注释备用
//    public static class ItemStackedOnOtherHandler
//    {
//        /**
//         * 通过此事件覆写以阻止原有的点击逻辑操作StoredItemStackSlot<br>
//         * 详细逻辑见{@link net.minecraft.world.inventory.AbstractContainerMenu}的doClick方法对tryItemClickBehaviourOverride的使用
//         * @param event 传入的事件，提供一系列基本参数 包括 持有的物品 要处理的物品 正处理的槽位 点击动作 玩家 持有的物品的槽位，不过此处均未用到
//         */
//        @SubscribeEvent
//        public void OnItemStackedHandle(ItemStackedOnOtherEvent event)
//        {
//            event.setCanceled(event.getSlot() instanceof StoredStackSlot);
//        }
//    }
}
