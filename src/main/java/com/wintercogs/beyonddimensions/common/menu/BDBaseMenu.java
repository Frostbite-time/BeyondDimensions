package com.wintercogs.beyonddimensions.common.menu;

import com.google.common.base.Suppliers;
import com.wintercogs.beyonddimensions.api.menu.interaction.SlotClick;
import com.wintercogs.beyonddimensions.api.menu.interaction.SlotInteractions;
import com.wintercogs.beyonddimensions.api.menu.widget.MenuWidget;
import com.wintercogs.beyonddimensions.api.storage.key.KeyAmount;
import com.wintercogs.beyonddimensions.common.menu.sync.BDMenuCommands;
import com.wintercogs.beyonddimensions.common.menu.sync.BDMenuResources;
import com.wintercogs.beyonddimensions.common.menu.widget.slot.AbstractStackTypedSlot;
import com.wintercogs.beyonddimensions.common.menu.widget.slot.DisorderedSlotGroupSync;
import dev.compixel.slots.SlotTransferRoutes;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import org.anti_ad.mc.ipn.api.IPNIgnore;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
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
     * 在所有槽位都添加完之后构建；子类可以声明具名的分组与路线。
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

    public List<DisorderedSlotGroupSync> slotGroupSyncs = new ArrayList<>();

    private final List<MenuWidget> widgets = new ArrayList<>();

    protected BDBaseMenu(@Nullable MenuType<?> menuType, int containerId, Inventory playerInventory)
    {
        super(menuType, containerId);
        this.player = playerInventory.player;
    }

    protected void addSlotGroupSync(DisorderedSlotGroupSync slotGroupSync)
    {
        slotGroupSyncs.add(slotGroupSync);
    }

    /**
     * 菜单里的部件，按添加顺序
     */
    public final List<MenuWidget> widgets()
    {
        return Collections.unmodifiableList(widgets);
    }

    /**
     * 这种类型的部件，菜单里没有时为 null
     */
    public final <W extends MenuWidget> @Nullable W widget(Class<W> type)
    {
        for (MenuWidget widget : widgets)
        {
            if (type.isInstance(widget))
                return type.cast(widget);
        }
        return null;
    }

    /**
     * 登记一个已经加好槽位的部件
     */
    protected final <W extends MenuWidget> W addWidget(W widget)
    {
        widgets.add(widget);
        quickMoveRoutes = null;
        return widget;
    }

    /**
     * 只供 {@link MenuWidget} 添加自己的槽位
     */
    @ApiStatus.Internal
    public final void addWidgetSlot(Slot slot)
    {
        addSlot(slot);
    }

    /**
     * 只供 {@link MenuWidget} 添加要同步的整数
     */
    @ApiStatus.Internal
    public final void addWidgetData(DataSlot data)
    {
        addDataSlot(data);
    }

    @Override
    public void slotsChanged(@NotNull Container container)
    {
        for (MenuWidget widget : widgets)
        {
            if (widget.ownsContainer(container))
            {
                widget.slotsChanged(container);
                return;
            }
        }
        super.slotsChanged(container);
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
        // 数据包里的内容只是请求：先校验，再用它取下标或改动服务端的物品。
        if (player.level().isClientSide() || player.containerMenu != this || !player.isAlive() || player.isSpectator()
                || !stillValid(player) || slotIndex < 0 || slotIndex >= slots.size()
                || button < 0 || button > 2 || (modifiers & ~SlotClick.MODIFIERS) != 0
                || clickedStack == null || clickedStack.amount() < 0
                || clickedStack.amount() > clickedStack.key().getVanillaMaxStackSize()
                || requested > clickedStack.key().getVanillaMaxStackSize()) return;
        SlotInteractions.dispatch(SlotClick.of(this, player, slots.get(slotIndex), clickedStack, button, modifiers, requested));
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

    /**
     * 原版的容器点击不作用于 BD 槽位，它们的点击由 {@link #customClickHandler} 处理。
     * 原版点击会把槽里的物品交给玩家、再让槽位清空自己，而 BD 槽位的内容在存储里，不会跟着清空
     */
    @Override
    public void clicked(int slotId, int button, @NotNull ClickType clickType, @NotNull Player player)
    {
        if (slotId >= 0 && slotId < slots.size() && slots.get(slotId) instanceof AbstractStackTypedSlot)
            return;
        super.clicked(slotId, button, clickType, player);
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
        if (slot instanceof AbstractStackTypedSlot)
            return false;
        for (MenuWidget widget : widgets)
        {
            if (widget.owns(slot.index) && !widget.canTakeForPickAll(slot))
                return false;
        }
        return super.canTakeItemForPickAll(stack, slot);
    }

    @Override
    public void removed(@NotNull Player player)
    {
        super.removed(player);
        for (var group : slotGroupSyncs) group.dispose();
        if (resources != null) resources.close();
        for (MenuWidget widget : widgets) widget.removed(player);
    }
}
