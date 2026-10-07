package com.wintercogs.beyonddimensions.common.menu.interaction;

import com.wintercogs.beyonddimensions.api.storage.handler.IStackHandler;
import com.wintercogs.beyonddimensions.api.storage.key.IStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.KeyAmount;
import com.wintercogs.beyonddimensions.api.storage.key.impl.EmptyStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.impl.ItemStackKey;
import com.wintercogs.beyonddimensions.common.menu.BDBaseMenu;
import com.wintercogs.beyonddimensions.common.menu.widget.slot.AbstractStackTypedSlot;
import com.wintercogs.beyonddimensions.common.menu.widget.slot.DisorderedStackTypedSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

/**
 * 一次槽位操作的只读上下文，由服务端在收到点击后创建。
 * <p>
 * 被点击的资源以服务端存储为准：存储槽按客户端给出的资源键查真实数量，有序槽与标记槽直接读槽位内容。
 * 客户端发来的原样内容在 {@link #sent()} 里，只用于核对。
 */
public final class SlotClick
{
    /**
     * 槽位种类：存储网格（按资源键存取）、有序资源槽（按槽位存取）、标记槽与原版槽位
     */
    public enum Kind
    {
        STORAGE, ORDERED, FLAG, VANILLA
    }

    private final BDBaseMenu menu;
    private final Player player;
    private final Slot slot;
    private final Kind kind;
    private final int button;
    private final boolean shift;
    private final long requested;
    private final KeyAmount sent;
    private final KeyAmount clicked;
    private final ItemStack carried;

    private SlotClick(BDBaseMenu menu, Player player, Slot slot, Kind kind, int button, boolean shift, long requested, KeyAmount sent)
    {
        this.menu = menu;
        this.player = player;
        this.slot = slot;
        this.kind = kind;
        this.button = button;
        this.shift = shift;
        this.requested = requested;
        this.sent = sent;
        this.carried = menu.getCarried().copy();
        this.clicked = switch (kind)
        {
            case STORAGE ->
            {
                IStackKey<?> key = sent.key();
                yield key.isEmpty() ? empty() : new KeyAmount(key, storage().getStackByKey(key).amount());
            }
            case ORDERED, FLAG -> ((AbstractStackTypedSlot) slot).getStack();
            case VANILLA -> new KeyAmount(new ItemStackKey(slot.getItem()), slot.getItem().getCount());
        };
    }

    /**
     * @param requested 指定取出的数量（右键菜单的"取出 x 个"），没有时为 -1
     */
    public static SlotClick of(BDBaseMenu menu, Player player, Slot slot, KeyAmount sent, int button, boolean shift, long requested)
    {
        Kind kind;
        if (slot instanceof AbstractStackTypedSlot typed)
            kind = typed.isFake() ? Kind.FLAG : slot instanceof DisorderedStackTypedSlot ? Kind.STORAGE : Kind.ORDERED;
        else
            kind = Kind.VANILLA;
        return new SlotClick(menu, player, slot, kind, button, shift, requested, sent);
    }

    private static KeyAmount empty()
    {
        return new KeyAmount(EmptyStackKey.INSTANCE, 0);
    }

    public BDBaseMenu menu()
    {
        return menu;
    }

    public Player player()
    {
        return player;
    }

    public Slot slot()
    {
        return slot;
    }

    public Kind kind()
    {
        return kind;
    }

    public boolean isResource()
    {
        return kind == Kind.STORAGE || kind == Kind.ORDERED;
    }

    public int button()
    {
        return button;
    }

    public boolean isLeft()
    {
        return button == GLFW.GLFW_MOUSE_BUTTON_LEFT;
    }

    public boolean isRight()
    {
        return button == GLFW.GLFW_MOUSE_BUTTON_RIGHT;
    }

    public boolean isMiddle()
    {
        return button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE;
    }

    public boolean shift()
    {
        return shift;
    }

    /**
     * 指定取出的数量，没有指定时为 -1
     */
    public long requested()
    {
        return requested;
    }

    /**
     * 客户端发来的资源与数量，未经核对
     */
    public KeyAmount sent()
    {
        return sent;
    }

    /**
     * 被点击的资源与服务端的真实数量；为空表示点到空位
     */
    public KeyAmount clicked()
    {
        return clicked;
    }

    /**
     * 点击时手上物品的副本；修改它不会影响菜单，改变手上物品用 {@link #setCarried}
     */
    public ItemStack carried()
    {
        return carried;
    }

    public void setCarried(ItemStack stack)
    {
        menu.setCarried(stack);
    }

    public AbstractStackTypedSlot typedSlot()
    {
        return (AbstractStackTypedSlot) slot;
    }

    public IStackHandler storage()
    {
        return typedSlot().getStorage();
    }

    /**
     * 有序槽在存储里的槽位，存储网格为 -1
     */
    public int storageSlot()
    {
        return kind == Kind.ORDERED ? typedSlot().getSlotIndex() : -1;
    }

    /**
     * 存入资源，返回没能存入的部分。存储网格存进整个网络，有序槽只存进这一格
     */
    public KeyAmount insert(IStackKey<?> key, long amount, boolean simulate)
    {
        return kind == Kind.ORDERED
                ? storage().insert(typedSlot().getSlotIndex(), key, amount, simulate)
                : storage().insert(key, amount, simulate);
    }

    /**
     * 取出资源，返回实际取出的部分。有序槽只从这一格取，且只取与槽内相同的资源
     */
    public KeyAmount extract(IStackKey<?> key, long amount, boolean simulate)
    {
        if (kind != Kind.ORDERED)
            return storage().extract(key, amount, simulate, false);
        KeyAmount inSlot = typedSlot().getStack();
        if (inSlot.isEmpty() || !inSlot.key().isSameTypeSameComponents(key))
            return empty();
        return storage().extract(typedSlot().getSlotIndex(), amount, simulate);
    }
}
