package com.wintercogs.beyonddimensions.common.menu.widget.slot;

import com.wintercogs.beyonddimensions.api.storage.handler.IStackHandler;
import com.wintercogs.beyonddimensions.api.storage.key.IStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.KeyAmount;
import com.wintercogs.beyonddimensions.api.storage.key.impl.EmptyStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.impl.ItemStackKey;
import com.wintercogs.beyonddimensions.common.menu.BDBaseMenu;
import com.wintercogs.beyonddimensions.util.BDMath;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * 专用于IStackKey泛型类的slot组件，内部自带click、quick-click以及数据的网络同步处理，以便于在本模组的不同UI中使用时可以被快速而泛化的添加。
 * <p>请确保其只被添加到BDBaseMenu或BDBaseGUI及其子类</p>
 * <p>如果你需要将其添加到你自定义的菜单或ui，你需要修改他们，以确保会正确调用click函数以及同步函数</p>
 */
public abstract class AbstractStackTypedSlot extends Slot
{
    /**
     * 一个空容器，仅用于欺骗父类构造函数
     */
    private static final Container empty_inv = new SimpleContainer(0);

    /**
     * 对应的实际存储，客户端用于显示数据，服务端用于操作数据
     */
    protected final IStackHandler storage;

    /**
     * 可供快速转移物品的slots区间起始
     * <p>注意：slots指的是Menu的slots列表，索引对应的也是这个列表</p>
     * <p>其使用前应当进行数据校验，不正确的数值应当阻止quickMove函数调用（需自行实现）</p>
     * <p>区间范围：前闭后开</p>
     */
    protected final int quickMoveSlotStartIndex;
    /**
     * 可供快速转移物品的slots区间结束
     */
    protected final int quickMoveSlotEndIndex;

    /**
     * 此slot所对应的存储物在IStackHandler中的具体索引
     */
    protected int theSlot;

    /**
     * 假槽位/标记槽位
     */
    protected boolean fake;

    /**
     * 是否启用
     */
    protected boolean active = true;

    /**
     * 对应的menu，用于提供上下文
     */
    protected final BDBaseMenu menu;


    public AbstractStackTypedSlot(BDBaseMenu menu, IStackHandler storage, int slotIndex, int xPosition, int yPosition)
    {
        super(empty_inv, slotIndex, xPosition, yPosition);
        this.theSlot = slotIndex;
        this.storage = storage;
        this.menu = menu;
        this.quickMoveSlotStartIndex = -1;
        this.quickMoveSlotEndIndex = -1;
    }

    public AbstractStackTypedSlot(BDBaseMenu menu, IStackHandler storage, int slotIndex, int quickMoveSlotStartIndex, int quickMoveSlotEndIndex, int xPosition, int yPosition)
    {
        super(empty_inv, slotIndex, xPosition, yPosition);
        this.theSlot = slotIndex;
        this.storage = storage;
        this.menu = menu;
        this.quickMoveSlotStartIndex = quickMoveSlotStartIndex;
        this.quickMoveSlotEndIndex = quickMoveSlotEndIndex;
    }

    // 自定义的点击和同步处理--------------------------------------------------------------------------------------------------
    // 注意，此处的函数除loadChange外请仅在服务端调用，重写时也只考虑服务端逻辑
    // 客户端请通过loadChange从服务端接收数据，然后处理

    /**
     * 获取容器
     */
    public IStackHandler getStorage()
    {
        return storage;
    }

    /**
     * 是否为有序容器槽位
     */
    public abstract boolean isOrdered();

    /**
     * 当鼠标直接点击此槽位会发生什么
     */
    public abstract void click(KeyAmount clickStack, int button, Player player);

    /**
     * 当鼠标shift点击此槽位会发生什么
     */
    public abstract void quickMove(KeyAmount clickStack, int button, Player player);

    /**
     * 将数据从服务端向客户端发包
     */
    public abstract void updateChange();

    /**
     * 如何接受同步数据
     *
     * @param where     stack应当覆盖的位置
     * @param newKey    对应位置应当出现的新stackKey
     * @param newAmount 对应位置stack应当呈现的数量
     */
    public abstract void loadChange(int where, IStackKey<?> newKey, long newAmount);


    // 其他有用的slot方法或者为slot运行所用的方法-------------------------------------------------------------------------------

    // 获取槽位容量
    public long getSlotCap()
    {
        return storage.getSlotCapacity(theSlot);
    }

    public KeyAmount getTypedStackFromUnifiedStorage()
    {
        return storage.getStackBySlot(getSlotIndex());
    }

    public ItemStack getItemStackFromUnifiedStorage()
    {
        //从当前槽索引取物品
        KeyAmount stackType = storage.getStackBySlot(getSlotIndex());

        if (stackType.key() instanceof ItemStackKey itemStackType)
        {
            ItemStack readOnlyStack = itemStackType.getReadOnlyStack();
            readOnlyStack.setCount(BDMath.clampLongToInt(stackType.amount()));
            return readOnlyStack;
        }
        else
        {
            return ItemStack.EMPTY;
        }
    }

    // 获取不超过原版最大堆叠数的Stack，一般仅用于GUI类，可以保留Item实现
    public KeyAmount getVanillaActualStack()
    {
        //从当前槽索引取物品
        KeyAmount stack = getTypedStackFromUnifiedStorage();
        if (stack.isEmpty())
            return stack;
        if (stack.amount() > stack.key().getVanillaMaxStackSize())
        {
            return new KeyAmount(stack.key(), stack.key().getVanillaMaxStackSize());
        }
        else
        {
            return stack;
        }

    }

    // 获取原版最大堆叠数的Stack，一般仅用于GUI类，可以保留Item实现
    public KeyAmount getVanillaMaxSizeStack()
    {
        KeyAmount stack = getTypedStackFromUnifiedStorage();
        if (stack.isEmpty())
            return stack;
        return new KeyAmount(stack.key(), stack.key().getVanillaMaxStackSize());
    }

    public @NotNull KeyAmount getStack()
    {
        if (getSlotIndex() < 0 || getSlotIndex() >= storage.getSlots())
        {
            return new KeyAmount(EmptyStackKey.INSTANCE, 0);
        }
        return storage.getStackBySlot(getSlotIndex());
    }


    // 以下这些重写 覆盖了slot中最基本的要素，以便将Container驱动的inv系统，替换成IStackKey驱动------------------------------
    public void setStackDirectly(IStackKey<?> key, long amount)
    {
        // 绕过一切限制直接设置目标槽位的内容
        // 因为这种操作很难进行数据校验，因此抽象类仅提供空实现
        // 仅在确定需要的时候再重写（如标记槽）
        // 不建议对于非标记槽进行重写，可能导致数据包漏洞
        // 涉及非标记槽的直接设置建议从服务端获取storage进行，而不是通过slot进行
    }

    // 有序槽位只取出当前槽中数量，无序槽位从整个存储中取出
    // 另一种类型的safeInsert，专用于此种槽位
    // 返回余量
    public abstract KeyAmount safeInsert(IStackKey<?> key, long amount);

    // 对于有序槽位，应当从当前槽位中取出对应种类stack的对应数量
    // 对于无序槽位，应当从整个存储中取出对应种类stack的对应数量
    // 返回取出量
    public abstract KeyAmount safeExtract(IStackKey<?> key, long amount);


    @Override
    public @NotNull ItemStack getItem()
    {
        if (getSlotIndex() < 0)
        {
            return ItemStack.EMPTY;
        }
        ItemStack itemStack = getItemStackFromUnifiedStorage();
        if (itemStack.isEmpty())
            return ItemStack.EMPTY;
        return itemStack.copy();

    }

    @Override
    public boolean hasItem()
    {
        //检查当前槽是否为空
        return !storage.getStackBySlot(getSlotIndex()).isEmpty();
    }

    @Override
    public void setChanged()
    {
        // IStackHandler系列均应当在实际变化后自行调用onchange，此处不重复处理
    }

    @Override
    public int getSlotIndex()
    {
        return this.theSlot;
    }

    @Override
    public boolean isSameInventory(@NotNull Slot other)
    {
        if (other instanceof AbstractStackTypedSlot)
        {
            // 比较二者是否是同一个引用  或许以后可以用其他更注重数据的方式比较？
            return this.storage == ((AbstractStackTypedSlot) other).storage;
        }
        return false;
    }

    @Override
    public int getContainerSlot()
    {
        return this.theSlot;
    }

    public void setTheSlotIndex(int index)
    {
        this.theSlot = index;
    }

    public long getItemCount()
    {
        if (getSlotIndex() < 0)
        {
            return -1;
        }
        KeyAmount stack = storage.getStackBySlot(getSlotIndex());
        if (!stack.isEmpty())
        {
            return stack.amount();
        }
        return -1;
    }

    public boolean isFake()
    {
        return fake;
    }

    public void setFake(boolean fake)
    {
        this.fake = fake;
    }

    @Override
    public boolean isActive()
    {
        return active;
    }

    public void setActive(boolean active)
    {
        this.active = active;
    }

    // 仅对原版slot的重写，但不实际使用它们-------------------------------------------------------------------------------------
    // 如果发现意外使用则可能需要重写原版方法
    // 大部分情况下，我们仅提供空实现或其他占位实现，因为我们不希望被原版方法干扰

    @Override
    public void set(@NotNull ItemStack stack)
    {
        // 此方法会在AbstractContainerMenu初始化时被数据包处理调用
    }

    @Override
    public void setByPlayer(@NotNull ItemStack stack)
    {
        // 当玩家拿着物品点击这个槽会发生什么
        // 点击事件交由其他函数处理，此处废弃
    }

    @Override
    public int getMaxStackSize()
    {
        // 获取槽位可存储物品的最大值
        return Integer.MAX_VALUE;
    }

    @Override
    public int getMaxStackSize(@NotNull ItemStack stack)
    {
        // 获取槽位可存储物品的最大值
        return Integer.MAX_VALUE;
    }

    @Override
    public @NotNull ItemStack remove(int amount)
    {
        // 交由点击函数一并处理，此处废弃
        return ItemStack.EMPTY; // 表示没有物品被移除
    }

    @Override
    public @NotNull ItemStack safeInsert(@NotNull ItemStack stack, int increment)
    {
        // 此处废弃
        return stack; // 剩余原有的所有物品，即没有物品被插入
    }

}
