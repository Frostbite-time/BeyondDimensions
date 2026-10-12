package com.wintercogs.beyonddimensions.common.menu.interaction;

import com.wintercogs.beyonddimensions.api.capability.helper.CapabilityHelper;
import com.wintercogs.beyonddimensions.api.capability.helper.wrapper.IStackHandlerWrapper;
import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import com.wintercogs.beyonddimensions.api.storage.key.IStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.KeyAmount;
import com.wintercogs.beyonddimensions.api.storage.key.StackKeyRegistry;
import com.wintercogs.beyonddimensions.api.storage.key.impl.FluidStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.impl.ItemStackKey;
import com.wintercogs.beyonddimensions.common.init.BDFluids;
import com.wintercogs.beyonddimensions.common.init.BDTags;
import com.wintercogs.beyonddimensions.common.item.XpExchangeItem;
import com.wintercogs.beyonddimensions.common.menu.NetInterfaceBaseMenu;
import com.wintercogs.beyonddimensions.common.menu.interaction.SlotClick.Kind;
import com.wintercogs.beyonddimensions.common.menu.widget.slot.AbstractStackTypedSlot;
import com.wintercogs.beyonddimensions.common.menu.widget.slot.DisorderedStackTypedSlot;
import com.wintercogs.beyonddimensions.common.menu.widget.slot.ItemCapInteractionBlackList;
import com.wintercogs.beyonddimensions.util.BDMath;
import com.wintercogs.beyonddimensions.util.ResourceTransfer;
import com.wintercogs.beyonddimensions.util.XpUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.items.wrapper.InvWrapper;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.ToLongFunction;
import java.util.stream.IntStream;

/**
 * BD 内置的槽位交互，按尝试顺序注册。除标记槽与手上拿着容器外，按住 Shift 的点击只走转移
 */
final class BuiltinSlotInteractions
{
    private BuiltinSlotInteractions()
    {
    }

    static void register()
    {
        add("flag_clear", c -> c.kind() == Kind.FLAG && !c.clicked().isEmpty(), BuiltinSlotInteractions::clearFlag);
        add("flag_contents", c -> c.kind() == Kind.FLAG && c.clicked().isEmpty() && !c.carried().isEmpty() && c.isRight(),
                BuiltinSlotInteractions::flagContents);
        add("flag_item", c -> c.kind() == Kind.FLAG && c.clicked().isEmpty() && !c.carried().isEmpty() && c.isLeft(),
                BuiltinSlotInteractions::flagItem);
        // 空格左键：背包（不含快捷栏）或快捷栏的全部物品、被点击的那种存储资源、这份有序存储的全部资源
        add("move_all_vanilla", c -> c.space() && c.isLeft() && inPlayerInventory(c), BuiltinSlotInteractions::moveAllVanilla);
        add("move_all_stored", c -> c.space() && c.isLeft() && c.kind() == Kind.STORAGE && !c.clicked().isEmpty(),
                BuiltinSlotInteractions::moveAll);
        add("move_all_ordered", c -> c.space() && c.isLeft() && c.kind() == Kind.ORDERED, BuiltinSlotInteractions::moveAllOrdered);
        // 有序槽里是同一种物品时先合并，桶、经验棒与容器也一样
        add("ordered_merge", c -> click(c) && c.kind() == Kind.ORDERED && !c.clicked().isEmpty() && !c.carried().isEmpty()
                        && (c.isLeft() || c.isRight())
                        && c.clicked().key().isSameTypeSameComponents(new ItemStackKey(c.carried())),
                BuiltinSlotInteractions::insertCarried);
        add("xp_rod", c -> click(c) && c.carried().getItem() instanceof XpExchangeItem && !c.isLeft(),
                BuiltinSlotInteractions::xpRod);
        // 手上拿着容器（桶、储罐、电池、背包等）：左键把被点击的资源装进去，右键把里面的东西排进存储。
        // 普通点击转移一个原版堆叠，按住 Shift 一直转移到装满、排空或存储不再收为止
        add("container_fill", c -> holdsContainer(c) && c.isLeft() && !c.clicked().isEmpty() && fillable(c),
                BuiltinSlotInteractions::fillContainer);
        add("container_empty", c -> holdsContainer(c) && c.isRight() && drainable(c),
                BuiltinSlotInteractions::emptyContainer);
        // Shift 左键双击：背包（含快捷栏）或这份有序存储里，与第一下点到的完全相同的全部资源
        add("quick_move_same_vanilla", c -> c.shift() && c.doubleClick() && c.isLeft() && inPlayerInventory(c),
                BuiltinSlotInteractions::quickMoveSameVanilla);
        add("quick_move_same_ordered", c -> c.shift() && c.doubleClick() && c.isLeft() && c.kind() == Kind.ORDERED,
                BuiltinSlotInteractions::quickMoveSameOrdered);
        add("take", c -> click(c) && c.carried().isEmpty() && (c.isLeft() || c.isRight())
                        && c.clicked().key() instanceof ItemStackKey && c.slot().mayPickup(c.player()),
                BuiltinSlotInteractions::take);
        add("ordered_swap", c -> click(c) && c.kind() == Kind.ORDERED && c.isLeft() && !c.carried().isEmpty()
                        && c.clicked().key() instanceof ItemStackKey,
                BuiltinSlotInteractions::swap);
        // 存储网格任何时候都能存入；有序槽只在空格存入，有内容时由合并与交换处理
        add("insert", c -> click(c) && !c.carried().isEmpty() && (c.isLeft() || c.isRight())
                        && (c.kind() == Kind.STORAGE || c.clicked().isEmpty())
                        && c.slot().mayPlace(c.carried()),
                BuiltinSlotInteractions::insertCarried);
        add("quick_move", c -> c.isResource() && c.shift() && !c.clicked().isEmpty(), BuiltinSlotInteractions::quickMove);
        add("quick_move_vanilla", c -> c.kind() == Kind.VANILLA && c.shift(), BuiltinSlotInteractions::quickMoveVanilla);
    }

    private interface Rule
    {
        boolean test(SlotClick click);
    }

    private interface Action
    {
        boolean run(SlotClick click);
    }

    private static void add(String name, Rule rule, Action action)
    {
        SlotInteractions.register(ResourceLocation.fromNamespaceAndPath(BDConstants.MODID, name), new SlotInteractions.Interaction()
        {
            @Override
            public boolean applies(SlotClick click)
            {
                return rule.test(click);
            }

            @Override
            public boolean perform(SlotClick click)
            {
                return action.run(click);
            }
        }, SlotInteractions.last());
    }

    // 资源槽上不按 Shift 的点击
    private static boolean click(SlotClick c)
    {
        return c.isResource() && !c.shift();
    }

    // 在资源槽上拿着未列入黑名单的物品；它能不能当容器用，由装入与排出各自判断
    private static boolean holdsContainer(SlotClick c)
    {
        return c.isResource() && !c.carried().isEmpty() && !ItemCapInteractionBlackList.isInBlackList(c.carried().getItem());
    }

    // —— 标记槽 ——

    private static boolean clearFlag(SlotClick c)
    {
        c.typedSlot().setStackDirectly(ItemStackKey.EMPTY, 0);
        return true;
    }

    // 经验棒标记经验流体，其他容器标记它的第一份内容物；都不是时什么也不做
    private static boolean flagContents(SlotClick c)
    {
        IStackKey<?> contents = ResourceContents.of(c.carried());
        if (contents != null)
            c.typedSlot().setStackDirectly(contents, 1);
        return true;
    }

    private static boolean flagItem(SlotClick c)
    {
        c.typedSlot().setStackDirectly(new ItemStackKey(c.carried()), 1);
        return true;
    }

    // —— 经验棒：右键存入、中键取出若干级 ——

    private static boolean xpRod(SlotClick c)
    {
        IStackKey<?> clicked = c.clicked().key();
        boolean xp = clicked instanceof FluidStackKey fluid && fluid.hasTag(BDTags.C_EXPERIENCE);
        int levels = XpExchangeItem.getXpLevelPerAction(c.carried());
        if (c.isRight())
        {
            // 点到经验流体就存进这一种，点到空位或存储网格里的其他资源就存进 BD 的经验流体
            if (xp)
                depositXp(c, clicked, levels);
            else if (c.clicked().isEmpty() || c.kind() == Kind.STORAGE)
                depositXp(c, xpFluid(), levels);
        }
        else if (c.isMiddle() && xp)
        {
            int maxXp = XpUtil.xpToGiveForLevels(c.player(), levels);
            int given = XpExchangeItem.extractExperience(c.storage(), (FluidStackKey) clicked, c.storageSlot(), maxXp);
            if (given > 0)
                c.player().giveExperiencePoints(given);
        }
        return true;
    }

    private static void depositXp(SlotClick c, IStackKey<?> fluid, int levels)
    {
        int rate = XpExchangeItem.getConversionRate();
        double level = XpUtil.levelAsDouble(c.player());
        int xp = BDMath.clampLongToInt(XpUtil.xpBetweenLevels(Math.max(level - levels, 0), level));
        KeyAmount remaining = c.insert(fluid, (long) xp * rate, false);
        if (!remaining.isEmpty())
            xp -= BDMath.clampLongToInt(remaining.amount() / rate);
        c.player().giveExperiencePoints(-xp);
    }

    private static FluidStackKey xpFluid()
    {
        return new FluidStackKey(new FluidStack(BDFluids.XP_FLUID.source(), 1));
    }

    // —— 手上的容器 ——

    // 按住 Shift 时一次点击最多转移的轮数；取之不尽的容器（如创造模式储罐）也会在这里停下
    private static final int MAX_CONTAINER_ROUNDS = 256;

    // 一轮转移的结果：转移的数量，以及转移后的容器（装满的桶会变成另一种物品）
    private record Round(long moved, ItemStack container)
    {
    }

    // 手上的容器能不能装下被点击的资源
    private static boolean fillable(SlotClick c)
    {
        IStackKey<?> key = c.clicked().key();
        IStackHandlerWrapper<Object> handler = wrapper(c.carried().copyWithCount(1), key.getTypeId(), CapabilityHelper.ItemCapabilityMap.get(key.getTypeId()));
        long amount = roundLimit(key.getVanillaMaxStackSize());
        return handler != null && handler.getSlots() > 0 && handler.insert(key.copyStackWithCount(amount), true) < amount;
    }

    // 手上的容器里有没有东西
    private static boolean drainable(SlotClick c)
    {
        ItemStack single = c.carried().copyWithCount(1);
        for (var entry : CapabilityHelper.ItemCapabilityMap.entrySet())
        {
            IStackHandlerWrapper<Object> handler = wrapper(single, entry.getKey(), entry.getValue());
            if (handler == null)
                continue;
            for (int index = 0; index < handler.getSlots(); index++)
            {
                KeyAmount contents = StackKeyRegistry.getType(entry.getKey()).fromStackObject(handler.getStackInSlot(index));
                if (contents != null && !contents.isEmpty())
                    return true;
            }
        }
        return false;
    }

    // 左键：把被点击的资源装进手上的容器
    private static boolean fillContainer(SlotClick c)
    {
        IStackKey<?> key = c.clicked().key();
        long amount = roundLimit(c.shift() ? Long.MAX_VALUE : key.getVanillaMaxStackSize());
        for (int round = 0; round < rounds(c); round++)
            if (useOneContainer(c, single -> fillRound(c, single, key, amount)) <= 0)
                break;
        return true;
    }

    // 右键：把手上容器里的东西排进被点击的存储
    private static boolean emptyContainer(SlotClick c)
    {
        for (int round = 0; round < rounds(c); round++)
            if (useOneContainer(c, single -> drainRound(c, single, c.shift())) <= 0)
                break;
        return true;
    }

    private static int rounds(SlotClick c)
    {
        return c.shift() ? MAX_CONTAINER_ROUNDS : 1;
    }

    // 堆叠对象里的数量多为 int，一轮不超过这个范围
    private static long roundLimit(long amount)
    {
        return Math.min(amount, Integer.MAX_VALUE);
    }

    // 手上是一叠容器时每轮只处理一个：处理后的容器回到手上，手上还有剩余时放进背包。返回这一轮转移的数量
    private static long useOneContainer(SlotClick c, Function<ItemStack, Round> round)
    {
        // 每一轮都读菜单上此刻的手持物品，上一轮可能已经换过
        ItemStack carried = c.menu().getCarried();
        if (carried.isEmpty())
            return 0L;
        Round done = round.apply(carried.copyWithCount(1));
        if (done.moved() <= 0L)
            return 0L;
        if (carried.getCount() == 1)
        {
            c.setCarried(done.container().copy());
        }
        else
        {
            c.setCarried(carried.copyWithCount(carried.getCount() - 1));
            c.player().getInventory().placeItemBackInInventory(done.container().copy());
        }
        return done.moved();
    }

    // 先模拟存储能给多少、容器能收多少，再只取出收得下的数量
    private static Round fillRound(SlotClick c, ItemStack single, IStackKey<?> key, long amount)
    {
        IStackHandlerWrapper<Object> handler = wrapper(single, key.getTypeId(), CapabilityHelper.ItemCapabilityMap.get(key.getTypeId()));
        if (handler == null || handler.getSlots() <= 0)
            return new Round(0L, single);
        KeyAmount moved = extractInto(c, key, amount,
                offer -> offer.amount() - handler.insert(offer.key().copyStackWithCount(offer.amount()), true),
                taken -> handler.insert(taken.key().copyStackWithCount(taken.amount()), false));
        return new Round(moved.amount(), handler.getContainer().orElse(single));
    }

    // 排出容器里第一份存储收得下的内容物：先模拟容器能倒出多少、存储能收下多少，再只倒出收得下的数量
    private static Round drainRound(SlotClick c, ItemStack single, boolean all)
    {
        for (var entry : CapabilityHelper.ItemCapabilityMap.entrySet())
        {
            IStackHandlerWrapper<Object> handler = wrapper(single, entry.getKey(), entry.getValue());
            if (handler == null)
                continue;
            for (int index = 0; index < handler.getSlots(); index++)
            {
                KeyAmount contents = StackKeyRegistry.getType(entry.getKey()).fromStackObject(handler.getStackInSlot(index));
                if (contents == null || contents.isEmpty())
                    continue;
                long amount = roundLimit(all ? Long.MAX_VALUE : Math.min(contents.amount(), contents.key().getVanillaMaxStackSize()));
                int slot = index;
                KeyAmount moved = ResourceTransfer.move(contents.key(), amount,
                        (key, n, simulate) -> new KeyAmount(key, handler.extract(slot, n, simulate)),
                        (key, n, simulate) -> new KeyAmount(key, n - handler.insert(slot, key.copyStackWithCount(n), simulate)),
                        offer -> offer.amount() - c.insert(offer.key(), offer.amount(), true).amount(),
                        taken -> c.insert(taken.key(), taken.amount(), false).amount());
                if (!moved.isEmpty())
                    return new Round(moved.amount(), handler.getContainer().orElse(single));
            }
        }
        return new Round(0L, single);
    }

    private static IStackHandlerWrapper<Object> wrapper(ItemStack stack, ResourceLocation typeId, net.neoforged.neoforge.capabilities.ItemCapability<?, Void> capability)
    {
        return ResourceContents.wrapper(stack, typeId, capability);
    }

    // —— 取出、存入、交换 ——

    // 左键一组，右键一半，右键菜单指定数量；最多一个原版堆叠
    private static boolean take(SlotClick c)
    {
        ItemStackKey key = (ItemStackKey) c.clicked().key();
        int stack = BDMath.clampLongToInt(Math.min(c.clicked().amount(), key.getVanillaMaxStackSize()));
        int amount = c.requested() > 0 ? (int) Math.min(c.requested(), stack) : c.isLeft() ? stack : (stack + 1) / 2;
        KeyAmount taken = c.extract(key, amount, false);
        if (!taken.isEmpty() && taken.toStack() instanceof ItemStack item)
            c.setCarried(item);
        return true;
    }

    // 左键存入整组，右键存入一个
    private static boolean insertCarried(SlotClick c)
    {
        ItemStack carried = c.carried();
        int count = c.isLeft() ? carried.getCount() : 1;
        int inserted = count - (int) c.insert(new ItemStackKey(carried), count, false).amount();
        c.setCarried(carried.getCount() - inserted <= 0 ? ItemStack.EMPTY : carried.copyWithCount(carried.getCount() - inserted));
        return true;
    }

    // 手上的物品与有序槽里的不同物品互换，双方都能整体放下时才换
    private static boolean swap(SlotClick c)
    {
        ItemStack carried = c.carried();
        KeyAmount inSlot = c.clicked();
        if (carried.getCount() > c.typedSlot().getSlotCap() || inSlot.amount() > inSlot.key().getVanillaMaxStackSize())
            return true;
        KeyAmount taken = c.extract(inSlot.key(), inSlot.amount(), false);
        if (c.insert(new ItemStackKey(carried), carried.getCount(), true).isEmpty() && taken.key() instanceof ItemStackKey item)
        {
            c.insert(new ItemStackKey(carried), carried.getCount(), false);
            c.setCarried(item.copyStackWithCount(taken.amount()));
        }
        else
        {
            c.insert(taken.key(), taken.amount(), false);
        }
        return true;
    }

    // —— Shift 转移 ——

    // 资源槽的内容按菜单路线移入目标槽位；流体在原版槽位里装成一桶，每次最多一桶
    private static boolean quickMove(SlotClick c)
    {
        move(c, new KeyAmount(c.clicked().key(), Math.min(c.sent().amount(), c.clicked().amount())));
        return true;
    }

    private static void move(SlotClick c, KeyAmount moving)
    {
        List<Integer> targets = quickMoveTargets(c);
        if (targets.isEmpty())
            return;
        for (int targetIndex : targets)
        {
            if (moving.isEmpty())
                break;
            Slot target = c.menu().slots.get(targetIndex);
            KeyAmount moved;
            if (target instanceof AbstractStackTypedSlot typed)
            {
                moved = extractInto(c, moving.key(), moving.amount(),
                        offer -> offer.amount() - typed.safeInsert(offer.key(), offer.amount(), true).amount(),
                        taken -> typed.safeInsert(taken.key(), taken.amount(), false).amount());
            }
            else if (moving.key() instanceof ItemStackKey)
            {
                moved = extractInto(c, moving.key(), moving.amount(),
                        offer -> offer.toStack() instanceof ItemStack stack ? accepts(target, stack) : 0L,
                        taken -> taken.toStack() instanceof ItemStack stack ? target.safeInsert(stack).getCount() : taken.amount());
            }
            else if (moving.key() instanceof FluidStackKey fluid && fluid.getSource().getBucket() != Items.AIR)
            {
                if (fillBucketInto(c, fluid, target))
                    break;
                continue;
            }
            else
            {
                continue;
            }
            moving = new KeyAmount(moving.key(), moving.amount() - moved.amount());
        }
        c.typedSlot().setChanged();
    }

    // 从被点击的资源槽取出放进别处：先模拟能给多少、对方能收多少，再只取出收得下的数量
    private static KeyAmount extractInto(SlotClick c, IStackKey<?> key, long amount, ToLongFunction<KeyAmount> accepts, ToLongFunction<KeyAmount> place)
    {
        return ResourceTransfer.move(key, amount, c::extract, c::insert, accepts, place);
    }

    // 原版槽位没有模拟插入：先看槽位是否允许，再用容器的物品处理器模拟，返回能收下的数量
    private static long accepts(Slot slot, ItemStack stack)
    {
        if (stack.isEmpty() || !slot.mayPlace(stack))
            return 0L;
        return stack.getCount() - new InvWrapper(slot.container).insertItem(slot.getContainerSlot(), stack, true).getCount();
    }

    // 从存储取一个空桶和一桶流体，装好放进目标槽位；先模拟三者都成立再动手。返回是否装成了
    private static boolean fillBucketInto(SlotClick c, FluidStackKey fluid, Slot target)
    {
        ItemStackKey bucket = new ItemStackKey(new ItemStack(Items.BUCKET));
        ItemStack filled = new ItemStack(fluid.getSource().getBucket());
        if (c.extract(fluid, 1000, true).amount() < 1000
                || c.storage().extract(bucket, 1, true, false).isEmpty()
                || accepts(target, filled) < 1)
            return false;
        c.extract(fluid, 1000, false);
        c.storage().extract(bucket, 1, false, false);
        target.safeInsert(filled);
        return true;
    }

    private static List<Integer> quickMoveTargets(SlotClick c)
    {
        AbstractStackTypedSlot slot = c.typedSlot();
        // 存储网格与网络接口按菜单声明的路线转移，其他有序槽沿用自己的目标区间
        if (c.kind() == Kind.STORAGE || c.menu() instanceof NetInterfaceBaseMenu)
            return c.menu().quickMoveRoutes().targets(slot.index);
        int start = slot.quickMoveStart(), end = slot.quickMoveEnd();
        return start >= 0 && start < end ? IntStream.range(start, end).boxed().toList() : List.of();
    }

    // 原版槽位（背包、合成产物等）的 Shift 转移
    private static boolean quickMoveVanilla(SlotClick c)
    {
        List<Integer> targets = c.menu().quickMoveRoutes().targets(c.slot().index);
        if (!targets.isEmpty())
            c.menu().quickMoveHandle(c.player(), c.slot().index, c.sent(), targets);
        return true;
    }

    // —— 整批转移：每一格都按它自己的 Shift 转移路线送往下一站 ——

    private static boolean inPlayerInventory(SlotClick c)
    {
        int index = c.slot().index;
        return c.kind() == Kind.VANILLA && index >= c.menu().inventoryStartIndex && index < c.menu().inventoryEndIndex;
    }

    private static boolean inHotbar(Slot slot)
    {
        return slot.container instanceof Inventory && Inventory.isHotbarSlot(slot.getContainerSlot());
    }

    // 点在快捷栏就转移整个快捷栏，点在背包其余部分就转移那一部分
    private static boolean moveAllVanilla(SlotClick c)
    {
        boolean hotbar = inHotbar(c.slot());
        for (int index = c.menu().inventoryStartIndex; index < c.menu().inventoryEndIndex; index++)
            if (inHotbar(c.menu().slots.get(index)) == hotbar)
                moveVanilla(c, index);
        return true;
    }

    // 背包与快捷栏里和第一下点到的物品完全相同的每一组
    private static boolean quickMoveSameVanilla(SlotClick c)
    {
        IStackKey<?> key = c.sent().key();
        if (key.isEmpty())
            return false;
        for (int index = c.menu().inventoryStartIndex; index < c.menu().inventoryEndIndex; index++)
            if (key.equals(new ItemStackKey(c.menu().slots.get(index).getItem())))
                moveVanilla(c, index);
        return true;
    }

    private static void moveVanilla(SlotClick c, int index)
    {
        ItemStack stack = c.menu().slots.get(index).getItem();
        List<Integer> targets = c.menu().quickMoveRoutes().targets(index);
        if (!stack.isEmpty() && !targets.isEmpty())
            c.menu().quickMoveHandle(c.player(), index, new KeyAmount(new ItemStackKey(stack), stack.getCount()), targets);
    }

    // 把这一格的资源反复转移，直到取空或下一站再也放不下
    private static boolean moveAll(SlotClick c)
    {
        IStackKey<?> key = c.clicked().key();
        for (long before = available(c, key); before > 0; )
        {
            move(c, new KeyAmount(key, Math.min(before, key.getVanillaMaxStackSize())));
            long after = available(c, key);
            if (after >= before)
                break;
            before = after;
        }
        return true;
    }

    private static long available(SlotClick c, IStackKey<?> key)
    {
        if (c.kind() == Kind.STORAGE)
            return c.storage().getStackByKey(key).amount();
        KeyAmount inSlot = c.typedSlot().getStack();
        return key.equals(inSlot.key()) ? inSlot.amount() : 0;
    }

    private static boolean moveAllOrdered(SlotClick c)
    {
        for (AbstractStackTypedSlot slot : orderedStorageSlots(c))
            moveAll(at(c, slot));
        return true;
    }

    // 这份有序存储里和第一下点到的资源完全相同的每一格；第一下可能已经把被点击的这一格搬空
    private static boolean quickMoveSameOrdered(SlotClick c)
    {
        IStackKey<?> key = c.sent().key().isEmpty() ? c.clicked().key() : c.sent().key();
        if (key.isEmpty())
            return false;
        for (AbstractStackTypedSlot slot : orderedStorageSlots(c))
            if (key.equals(slot.getStack().key()))
                moveAll(at(c, slot));
        return true;
    }

    // 与被点击的有序槽属于同一份有序存储的全部槽位
    private static List<AbstractStackTypedSlot> orderedStorageSlots(SlotClick c)
    {
        List<AbstractStackTypedSlot> slots = new ArrayList<>();
        for (Slot slot : c.menu().slots)
            if (slot instanceof AbstractStackTypedSlot typed && !typed.isFake() && !(typed instanceof DisorderedStackTypedSlot)
                    && typed.getStorage() == c.storage())
                slots.add(typed);
        return slots;
    }

    // 同一次点击落在另一个槽位上
    private static SlotClick at(SlotClick c, AbstractStackTypedSlot slot)
    {
        return SlotClick.of(c.menu(), c.player(), slot, slot.getStack(), c.button(), c.modifiers(), -1);
    }
}
