package com.wintercogs.beyonddimensions.common.menu;

import com.google.common.base.Suppliers;
import com.wintercogs.beyonddimensions.api.storage.key.KeyAmount;
import com.wintercogs.beyonddimensions.api.storage.key.impl.ItemStackKey;
import com.wintercogs.beyonddimensions.common.menu.interaction.SlotClick;
import com.wintercogs.beyonddimensions.common.menu.interaction.SlotInteractions;
import com.wintercogs.beyonddimensions.common.menu.sync.BDMenuCommands;
import com.wintercogs.beyonddimensions.common.menu.sync.BDMenuResources;
import com.wintercogs.beyonddimensions.common.menu.widget.slot.AbstractStackTypedSlot;
import com.wintercogs.beyonddimensions.common.menu.widget.slot.DisorderedSlotGroupSync;
import dev.compixel.slots.SlotTransferRoutes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import org.anti_ad.mc.ipn.api.IPNIgnore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Supplier;

// 定义一些用于 超越维度 模组的ui界面的基本方法。
// 主要是重写网络同步和点击事件，确保父类机制不处理StoredStackSlot的相关内容
@IPNIgnore
public abstract class BDBaseMenu extends AbstractContainerMenu implements dev.compixel.forge.sync.SyncedMenu
{

    public final Player player;
    private BDMenuResources resources;
    private BDMenuCommands commands;

    public final BDMenuCommands commands()
    {
        if (commands == null) commands = new BDMenuCommands(this);
        return commands;
    }

    private boolean vanillaSnapshot;

    public final boolean isVanillaSnapshot()
    {
        return vanillaSnapshot;
    }

    public final BDMenuResources resources()
    {
        if (resources == null) resources = new BDMenuResources(this);
        return resources;
    }

    @Override
    public abstract dev.compixel.forge.sync.MenuSync<? extends BDBaseMenu> menuSync();

    // 用于快速移动时标记玩家背包的槽位索引 如 索引从0开始 背包为54~89
    public int inventoryStartIndex = -1; //索引开始位置 为54
    public int inventoryEndIndex = -1;   //索引结束位置+1 为90

    // 原版槽位想要进行快速转移时的目标槽位范围
    protected int vanillaQuickMoveStartIndex = -1;
    protected int vanillaQuickMoveEndIndex = -1;

    private SlotTransferRoutes quickMoveRoutes;

    /**
     * Construct after all slots have been added; subclasses can declare named groups/routes.
     */
    protected SlotTransferRoutes createQuickMoveRoutes()
    {
        var builder = SlotTransferRoutes.builder(slots.size());
        int start = vanillaQuickMoveStartIndex, end = vanillaQuickMoveEndIndex;
        if (start < 0 || start >= end || end > slots.size()) return builder.build();
        builder.group("storage", start, end);
        if (start > 0) builder.group("before_storage", 0, start).route("before_storage", "storage");
        if (end < slots.size()) builder.group("after_storage", end, slots.size()).route("after_storage", "storage");
        return builder.build();
    }

    public final SlotTransferRoutes quickMoveRoutes()
    {
        if (quickMoveRoutes == null) quickMoveRoutes = createQuickMoveRoutes();
        return quickMoveRoutes;
    }

    private boolean init = false; // 需要在客户端Menu完成时才能向其发送的操作是否完成的标志
    public List<DisorderedSlotGroupSync> slotGroupSyncs = new ArrayList<>();

    protected BDBaseMenu(@Nullable MenuType<?> menuType, int containerId, Inventory playerInventory)
    {
        super(menuType, containerId);
        this.player = playerInventory.player;
    }

    protected void addSlotGroupSync(DisorderedSlotGroupSync slotGroupSync)
    {
        slotGroupSyncs.add(slotGroupSync);
    }

    @Override
    protected @NotNull Slot addSlot(@NotNull Slot slot)
    {
        quickMoveRoutes = null;
        if (slot instanceof AbstractStackTypedSlot resource && resource.isOrdered()) resources().slotAdded(resource);
        return super.addSlot(slot);
    }

    @Override
    public void broadcastChanges()
    {
        // 在原版方法上剔除了对AbstractStackTypedSlot的处理
        for (int i = 0; i < this.slots.size(); ++i)
        {
            Slot slot = this.slots.get(i);
            if (slot instanceof AbstractStackTypedSlot)
                continue; // 不允许broadcastChanges自动同步StoredItemStackSlot以便自定义处理
            ItemStack itemstack = slot.getItem();
            Objects.requireNonNull(itemstack);
            Supplier<ItemStack> supplier = Suppliers.memoize(itemstack::copy);
            this.triggerSlotListeners(i, itemstack, supplier);
            this.synchronizeSlotToRemote(i, itemstack, supplier);
        }

        this.synchronizeCarriedToRemote();

        for (int j = 0; j < this.dataSlots.size(); ++j)
        {
            DataSlot dataslot = this.dataSlots.get(j);
            int k = dataslot.get();
            if (dataslot.checkAndClearUpdateFlag())
            {
                this.updateDataSlotListeners(j, k);
            }

            this.synchronizeDataSlotToRemote(j, k);
        }

        // 有序槽位（标记槽、接口与熔炉的资源槽）没有变化订阅，由服务端在每次广播时比对后再同步
        if (!player.level().isClientSide() && resources != null) resources.pollOrdered();
    }

    // 自定义点击操作，交给 SlotInteractions 中注册的交互处理
    public void customClickHandler(int slotIndex, KeyAmount clickedStack, int button, int modifiers)
    {
        customClickHandler(slotIndex, clickedStack, button, modifiers, -1);
    }

    /**
     * @param modifiers 按住的修饰键，见 {@link SlotClick#SHIFT} 等
     * @param requested 指定取出的数量（右键菜单的"取出 x 个"），没有时为 -1
     */
    public void customClickHandler(int slotIndex, KeyAmount clickedStack, int button, int modifiers, long requested)
    {
        // Packet data is a request. Validate before indexing or touching server inventory.
        if (player.level().isClientSide() || player.containerMenu != this || !player.isAlive() || player.isSpectator()
                || !stillValid(player) || slotIndex < 0 || slotIndex >= slots.size()
                || button < 0 || button > 2 || (modifiers & ~SlotClick.MODIFIERS) != 0
                || clickedStack == null || clickedStack.amount() < 0
                || clickedStack.amount() > clickedStack.key().getVanillaMaxStackSize()
                || requested > clickedStack.key().getVanillaMaxStackSize()) return;
        SlotInteractions.dispatch(SlotClick.of(this, player, slots.get(slotIndex), clickedStack, button, modifiers, requested));
    }

    // 处理非AbstractStackTypedSlot槽位的快速转移
    public ItemStack quickMoveHandle(Player player, int slotIndex, KeyAmount clickStack, List<Integer> targets)
    {
        Slot slot = this.slots.get(slotIndex);
        if (slot != null && slot.mayPickup(player) && !clickStack.isEmpty()
                && clickStack.key().equals(new ItemStackKey(slot.getItem())))
        {
            ItemStack cacheStack;
            // 快速合成处理
            if (slot instanceof ResultSlot resultSlot)
            {
                cacheStack = slot.getItem().copy(); // 完成数据包校验 并锁定本次合成使用的配方成品
                for (int i = 0; !slot.getItem().isEmpty() && // 当合成槽为空时不执行操作
                        slot.getItem().getItem() == cacheStack.getItem() && // 当合成槽物品已经从一个配方变成另一个配方时不执行操作
                        i < slot.getItem().getMaxStackSize() / slot.getItem().getCount(); i++) // 限制单次合成次数
                {
                    // 尝试将物品分别插入背包和快速转移区间，并记录回滚信息
                    ItemStack remaining = cacheStack.copy();
                    Map<Integer, Integer> insertedToInv = new HashMap<>();
                    Map<Integer, Integer> insertedToSlots = new HashMap<>();
                    for (int invSlot = inventoryStartIndex; invSlot < inventoryEndIndex && !remaining.isEmpty(); invSlot++)
                    {
                        Slot targetSlot = slots.get(invSlot);
                        // safeInsert会破坏stack的数量，因此放入一个copy
                        int newSize = targetSlot.safeInsert(remaining.copy()).getCount();
                        if ((remaining.getCount() - newSize) != 0)
                            insertedToInv.put(invSlot, remaining.getCount() - newSize);
                        remaining.setCount(newSize);
                    }

                    // 处理剩余物品
                    for (int targetSlotIndex : targets)
                    {
                        if (remaining.isEmpty()) break;
                        Slot targetSlot = slots.get(targetSlotIndex);
                        int newSize;
                        if (targetSlot instanceof AbstractStackTypedSlot aTargetSlot)
                        {
                            // 此处是安全的转换，remaining不会超出int
                            // AbstractStackTypedSlot槽位的safeInsert是安全的，并且new ItemStackType会进行被动copy
                            newSize = (int) aTargetSlot.safeInsert(new ItemStackKey(remaining), remaining.getCount()).amount();
                        }
                        else
                        {
                            newSize = targetSlot.safeInsert(remaining.copy()).getCount();
                        }
                        if ((remaining.getCount() - newSize) != 0)
                            insertedToSlots.put(targetSlotIndex, remaining.getCount() - newSize);
                        remaining.setCount(newSize);
                    }


                    if (remaining.isEmpty()) // 如果产物被完整取出，则通过safeTake执行一次完整取出流程
                    {
                        ItemStack crafted = resultSlot.safeTake(cacheStack.getCount(), Integer.MAX_VALUE, player);
                        // 如果未取出任何合成物，则执行回滚
                        if (crafted.isEmpty())
                        {
                            for (Map.Entry<Integer, Integer> entry : insertedToInv.entrySet())
                            {
                                Slot afterSlot = slots.get(entry.getKey());
                                afterSlot.safeTake(entry.getValue(), Integer.MAX_VALUE, player);
                            }
                            for (Map.Entry<Integer, Integer> entry : insertedToSlots.entrySet())
                            {
                                Slot afterSlot = slots.get(entry.getKey());
                                if (afterSlot instanceof AbstractStackTypedSlot aSlot)
                                {
                                    aSlot.safeExtract(new ItemStackKey(cacheStack), entry.getValue());
                                }
                                else
                                {
                                    afterSlot.safeTake(entry.getValue(), Integer.MAX_VALUE, player);
                                }
                            }
                        }
                    }
                    else // 没能完整取出所有产物，则将之前插入仓库的物品进行回滚
                    {
                        for (Map.Entry<Integer, Integer> entry : insertedToInv.entrySet())
                        {
                            Slot afterSlot = slots.get(entry.getKey());
                            afterSlot.safeTake(entry.getValue(), Integer.MAX_VALUE, player);
                        }
                        for (Map.Entry<Integer, Integer> entry : insertedToSlots.entrySet())
                        {
                            Slot afterSlot = slots.get(entry.getKey());
                            if (afterSlot instanceof AbstractStackTypedSlot aSlot)
                            {
                                aSlot.safeExtract(new ItemStackKey(cacheStack), entry.getValue());
                            }
                            else
                            {
                                afterSlot.safeTake(entry.getValue(), Integer.MAX_VALUE, player);
                            }
                        }
                    }

                }

            }
            else // 普通快速移动处理
            {
                cacheStack = slot.getItem().copy(); // 完成数据包校验
                ItemStack remaining = cacheStack.copy();
                for (int targetSlotIndex : targets)
                {
                    if (remaining.isEmpty()) break;
                    Slot targetSlot = slots.get(targetSlotIndex);
                    int newSize;
                    if (targetSlot instanceof AbstractStackTypedSlot aTargetSlot)
                    {
                        // 此处是安全的转换，remaining不会超出int
                        newSize = (int) aTargetSlot.safeInsert(new ItemStackKey(remaining), remaining.getCount()).amount();
                    }
                    else
                    {
                        newSize = targetSlot.safeInsert(remaining).getCount();
                    }
                    remaining.setCount(newSize);
                }
                slot.tryRemove(cacheStack.getCount() - remaining.getCount(), Integer.MAX_VALUE, player);
            }

            slot.setChanged();
        }
        return ItemStack.EMPTY;
    }


    // 仅标记，需要时重写
    @Override
    public void broadcastFullState()
    {
        boolean previous = vanillaSnapshot;
        vanillaSnapshot = true;
        try
        {
            super.broadcastFullState();
        }
        finally
        {
            vanillaSnapshot = previous;
        }
    }

    @Override
    public void sendAllDataToRemote()
    {
        boolean previous = vanillaSnapshot;
        vanillaSnapshot = true;
        try
        {
            super.sendAllDataToRemote();
        }
        finally
        {
            vanillaSnapshot = previous;
        }
    }

    // 完全重写快速移动方案
    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int slotIndex)
    {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean moveItemStackTo(@NotNull ItemStack stack, int startIndex, int endIndex, boolean reverseDirection)
    {
        return false;
    }

    // 重写
    @Override
    public abstract boolean stillValid(@NotNull Player player);

    @Override
    public boolean canTakeItemForPickAll(@NotNull ItemStack stack, @NotNull Slot slot)
    {
        if (!(slot instanceof AbstractStackTypedSlot))
            return super.canTakeItemForPickAll(stack, slot);
        return false;
    }

    @Override
    public void removed(@NotNull Player player)
    {
        super.removed(player);
        for (var group : slotGroupSyncs) group.dispose();
        if (resources != null) resources.close();
    }
}
