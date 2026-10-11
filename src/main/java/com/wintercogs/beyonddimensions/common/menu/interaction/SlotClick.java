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
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

/**
 * 一次槽位点击的只读上下文，由服务端在收到点击后创建。
 * 被点击的资源以服务端存储为准；客户端发来的内容在 {@link #sent()} 里，只用于核对
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

    /**
     * 修饰键 Shift，即快速转移
     */
    public static final int SHIFT = 1;
    /**
     * Ctrl，macOS 上为 Command
     */
    public static final int CTRL = 1 << 1;
    public static final int ALT = 1 << 2;
    public static final int SPACE = 1 << 3;
    /**
     * 双击的第二下，判定标准与原版容器界面相同
     */
    public static final int DOUBLE = 1 << 4;
    /**
     * 全部修饰键与点击标志；服务端拒绝带有其他位的点击
     */
    public static final int MODIFIERS = SHIFT | CTRL | ALT | SPACE | DOUBLE;

    private final BDBaseMenu menu;
    private final Player player;
    private final Slot slot;
    private final @Nullable AbstractStackTypedSlot typed;
    private final Kind kind;
    private final int button;
    private final int modifiers;
    private final long requested;
    private final KeyAmount sent;
    private final KeyAmount clicked;
    private final ItemStack carried;

    private SlotClick(BDBaseMenu menu, Player player, Slot slot, @Nullable AbstractStackTypedSlot typed, Kind kind,
                      int button, int modifiers, long requested, KeyAmount sent)
    {
        this.menu = menu;
        this.player = player;
        this.slot = slot;
        this.typed = typed;
        this.kind = kind;
        this.button = button;
        this.modifiers = modifiers;
        this.requested = requested;
        this.sent = sent;
        this.carried = menu.getCarried().copy();
        this.clicked = switch (kind)
        {
            case STORAGE ->
            {
                IStackKey<?> key = sent.key();
                yield key.isEmpty() ? empty() : new KeyAmount(key, typed.getStorage().getStackByKey(key).amount());
            }
            case ORDERED, FLAG -> typed.getStack();
            case VANILLA -> new KeyAmount(new ItemStackKey(slot.getItem()), slot.getItem().getCount());
        };
    }

    /**
     * @param modifiers 按住的修饰键，见 {@link #SHIFT} 等
     * @param requested 指定取出的数量（右键菜单的"取出 x 个"），没有时为 -1
     */
    public static SlotClick of(BDBaseMenu menu, Player player, Slot slot, KeyAmount sent, int button, int modifiers, long requested)
    {
        if (slot instanceof AbstractStackTypedSlot typed)
        {
            Kind kind = typed.isFake() ? Kind.FLAG : typed instanceof DisorderedStackTypedSlot ? Kind.STORAGE : Kind.ORDERED;
            return new SlotClick(menu, player, slot, typed, kind, button, modifiers, requested, sent);
        }
        return new SlotClick(menu, player, slot, null, Kind.VANILLA, button, modifiers, requested, sent);
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

    /**
     * 点击时按住的修饰键与点击标志的按位组合
     */
    public int modifiers()
    {
        return modifiers;
    }

    /**
     * 是否按住 Shift，即快速转移
     */
    public boolean shift()
    {
        return (modifiers & SHIFT) != 0;
    }

    /**
     * 是否按住 Ctrl（macOS 上为 Command）
     */
    public boolean ctrl()
    {
        return (modifiers & CTRL) != 0;
    }

    public boolean alt()
    {
        return (modifiers & ALT) != 0;
    }

    public boolean space()
    {
        return (modifiers & SPACE) != 0;
    }

    /**
     * 是否为双击的第二下，见 {@link #DOUBLE}
     */
    public boolean doubleClick()
    {
        return (modifiers & DOUBLE) != 0;
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

    /**
     * 被点击的 BD 槽位（存储网格、有序槽与标记槽）；原版槽位为 null
     */
    public @Nullable AbstractStackTypedSlot typedSlot()
    {
        return typed;
    }

    /**
     * BD 槽位所属的存储；原版槽位为 null
     */
    public @Nullable IStackHandler storage()
    {
        return typed == null ? null : typed.getStorage();
    }

    /**
     * 有序槽在存储里的槽位，其他种类为 -1
     */
    public int storageSlot()
    {
        return kind == Kind.ORDERED ? typed.getSlotIndex() : -1;
    }

    /**
     * 存入资源，返回没能存入的部分。存储网格存进整个网络，有序槽只存进这一格
     */
    public KeyAmount insert(IStackKey<?> key, long amount, boolean simulate)
    {
        return switch (kind)
        {
            case STORAGE -> typed.getStorage().insert(key, amount, simulate);
            case ORDERED -> typed.getStorage().insert(typed.getSlotIndex(), key, amount, simulate);
            case FLAG, VANILLA -> new KeyAmount(key, amount);
        };
    }

    /**
     * 取出资源，返回实际取出的部分。有序槽只从这一格取
     */
    public KeyAmount extract(IStackKey<?> key, long amount, boolean simulate)
    {
        return switch (kind)
        {
            case STORAGE -> typed.getStorage().extract(key, amount, simulate, false);
            case ORDERED ->
            {
                KeyAmount inSlot = typed.getStack();
                yield inSlot.isEmpty() || !inSlot.key().isSameTypeSameComponents(key)
                        ? empty()
                        : typed.getStorage().extract(typed.getSlotIndex(), amount, simulate);
            }
            case FLAG, VANILLA -> empty();
        };
    }
}
