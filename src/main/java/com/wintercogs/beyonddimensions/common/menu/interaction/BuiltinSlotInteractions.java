package com.wintercogs.beyonddimensions.common.menu.interaction;

import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import com.wintercogs.beyonddimensions.api.capability.helper.CapabilityHelper;
import com.wintercogs.beyonddimensions.api.capability.helper.wrapper.FluidHandlerWrapper;
import com.wintercogs.beyonddimensions.api.capability.helper.wrapper.IStackHandlerWrapper;
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
import com.wintercogs.beyonddimensions.common.menu.widget.slot.ItemCapInteractionBlackList;
import com.wintercogs.beyonddimensions.util.BDMath;
import com.wintercogs.beyonddimensions.util.XpUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MilkBucketItem;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.List;
import java.util.function.Function;
import java.util.stream.IntStream;

/**
 * BD 内置的槽位交互，按尝试顺序注册：标记槽、有序槽的同种合并、经验棒、桶、带能力的容器、取出、有序槽的交换、存入兜底，
 * 最后是 Shift 转移。除标记槽外，按住 Shift 的点击只走转移
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
        // 有序槽里是同一种物品时先合并，桶、经验棒与容器也一样
        add("ordered_merge", c -> click(c) && c.kind() == Kind.ORDERED && !c.clicked().isEmpty() && !c.carried().isEmpty()
                        && (c.isLeft() || c.isRight())
                        && c.clicked().key().isSameTypeSameComponents(new ItemStackKey(c.carried())),
                BuiltinSlotInteractions::insertCarried);
        add("xp_rod", c -> click(c) && c.carried().getItem() instanceof XpExchangeItem && !c.isLeft(),
                BuiltinSlotInteractions::xpRod);
        add("bucket_fill", c -> containerClick(c) && c.carried().getItem() == Items.BUCKET
                        && c.clicked().key() instanceof FluidStackKey,
                BuiltinSlotInteractions::fillBucket);
        add("bucket_empty", c -> containerClick(c) && isBucket(c.carried()) && c.carried().getItem() != Items.BUCKET,
                BuiltinSlotInteractions::emptyBucket);
        add("container_fill", c -> containerClick(c) && !isBucket(c.carried()) && !c.clicked().isEmpty(),
                BuiltinSlotInteractions::fillContainer);
        // 有序槽里已有资源时，容器只从这一格抽取，不往里倒
        add("container_empty", c -> containerClick(c) && !isBucket(c.carried())
                        && (c.kind() == Kind.STORAGE || c.clicked().isEmpty()),
                BuiltinSlotInteractions::emptyContainer);
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

    // 拿着单个、未列入黑名单的物品右键：桶与带能力的容器
    private static boolean containerClick(SlotClick c)
    {
        return click(c) && c.isRight() && c.carried().getCount() == 1
                && !ItemCapInteractionBlackList.isInBlackList(c.carried().getItem());
    }

    private static boolean isBucket(ItemStack stack)
    {
        return stack.getItem() instanceof BucketItem || stack.getItem() instanceof MilkBucketItem;
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

    // —— 桶 ——

    // 空桶装满一桶被点击的流体
    private static boolean fillBucket(SlotClick c)
    {
        FluidStackKey fluid = (FluidStackKey) c.clicked().key();
        Item filled = fluid.getSource().getBucket();
        if (filled == Items.AIR || c.clicked().amount() < 1000)
            return false;
        c.extract(fluid, 1000, false);
        c.setCarried(new ItemStack(filled));
        return true;
    }

    // 满桶整桶倒入，放不下整桶就不倒
    private static boolean emptyBucket(SlotClick c)
    {
        Object handler = c.carried().getCapability(Capabilities.FluidHandler.ITEM);
        if (handler == null)
            return false;
        FluidHandlerWrapper bucket = new FluidHandlerWrapper(handler);
        if (bucket.getSlots() <= 0)
            return false;
        FluidStack contents = bucket.getStackInSlot(0);
        if (contents.isEmpty())
            return false;
        FluidStackKey key = new FluidStackKey(contents);
        int amount = BDMath.clampLongToInt(Math.min(contents.getAmount(), key.getVanillaMaxStackSize()));
        if (!c.insert(key, amount, true).isEmpty())
            return false;
        c.insert(key, amount, false);
        c.setCarried(new ItemStack(Items.BUCKET));
        return true;
    }

    // —— 带能力的容器 ——

    // 把被点击的资源抽进手上的容器，最多一个原版堆叠
    private static boolean fillContainer(SlotClick c)
    {
        IStackKey<?> key = c.clicked().key();
        IStackHandlerWrapper<Object> handler = wrapper(c.carried(), key.getTypeId(), CapabilityHelper.ItemCapabilityMap.get(key.getTypeId()));
        if (handler == null || handler.getSlots() <= 0)
            return false;
        int amount = BDMath.clampLongToInt(Math.min(c.clicked().amount(), key.getVanillaMaxStackSize()));
        int accepted = amount - (int) handler.insert(key.copyStackWithCount(amount), false);
        if (accepted <= 0)
            return false;
        c.extract(key, accepted, false);
        setContainer(c, handler);
        return true;
    }

    // 把手上容器的第一份内容物倒出来，最多一个原版堆叠
    private static boolean emptyContainer(SlotClick c)
    {
        for (var entry : CapabilityHelper.ItemCapabilityMap.entrySet())
        {
            IStackHandlerWrapper<Object> handler = wrapper(c.carried(), entry.getKey(), entry.getValue());
            if (handler == null)
                continue;
            for (int index = 0; index < handler.getSlots(); index++)
            {
                KeyAmount contents = StackKeyRegistry.getType(entry.getKey()).fromStackObject(handler.getStackInSlot(index));
                if (contents == null || contents.isEmpty())
                    continue;
                int amount = BDMath.clampLongToInt(Math.min(contents.amount(), contents.key().getVanillaMaxStackSize()));
                int inserted = amount - (int) c.insert(contents.key(), amount, false).amount();
                if (inserted <= 0)
                    continue;
                long drained = handler.extract(index, inserted, false);
                if (drained < inserted)
                    c.extract(contents.key(), inserted - drained, false); // 容器实际给出的更少，把多存的取回
                setContainer(c, handler);
                return true;
            }
        }
        return false;
    }

    private static IStackHandlerWrapper<Object> wrapper(ItemStack stack, ResourceLocation typeId, net.neoforged.neoforge.capabilities.ItemCapability<?, Void> capability)
    {
        return ResourceContents.wrapper(stack, typeId, capability);
    }

    // 容器改动后的物品可能是新的实例（例如装满的桶），重新放回手上
    private static void setContainer(SlotClick c, IStackHandlerWrapper<Object> handler)
    {
        handler.getContainer().ifPresentOrElse(container -> c.setCarried(container.copy()), () -> c.setCarried(c.carried().copy()));
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
        List<Integer> targets = quickMoveTargets(c);
        if (targets.isEmpty())
            return true;
        KeyAmount moving = new KeyAmount(c.clicked().key(), Math.min(c.sent().amount(), c.clicked().amount()));
        for (int targetIndex : targets)
        {
            if (moving.isEmpty())
                break;
            Slot target = c.menu().slots.get(targetIndex);
            if (target instanceof AbstractStackTypedSlot typed)
            {
                KeyAmount extracted = c.extract(moving.key(), moving.amount(), false);
                KeyAmount remaining = typed.safeInsert(extracted.key(), extracted.amount());
                if (!remaining.isEmpty())
                    c.insert(remaining.key(), remaining.amount(), false);
                moving = remaining;
            }
            else if (moving.key() instanceof ItemStackKey item)
            {
                KeyAmount extracted = c.extract(item, moving.amount(), false);
                if (extracted.isEmpty() || !(extracted.toStack() instanceof ItemStack stack))
                    break;
                ItemStack remaining = target.safeInsert(stack);
                if (!remaining.isEmpty())
                    c.insert(new ItemStackKey(remaining), remaining.getCount(), false);
                moving = new KeyAmount(new ItemStackKey(remaining), remaining.getCount());
            }
            else if (moving.key() instanceof FluidStackKey fluid && fluid.getSource().getBucket() != Items.AIR)
            {
                if (fillBucketInto(c, fluid, target))
                    break;
            }
        }
        c.typedSlot().setChanged();
        return true;
    }

    // 从存储取一个空桶和一桶流体，装好放进目标槽位；放不进就都放回。返回是否装成了
    private static boolean fillBucketInto(SlotClick c, FluidStackKey fluid, Slot target)
    {
        KeyAmount extracted = c.extract(fluid, 1000, false);
        if (extracted.amount() != 1000)
        {
            c.insert(extracted.key(), extracted.amount(), false);
            return false;
        }
        KeyAmount bucket = c.storage().extract(new ItemStackKey(new ItemStack(Items.BUCKET)), 1, false, false);
        if (bucket.isEmpty())
        {
            c.insert(extracted.key(), extracted.amount(), false);
            return false;
        }
        if (!target.safeInsert(new ItemStack(fluid.getSource().getBucket())).isEmpty())
        {
            c.insert(extracted.key(), extracted.amount(), false);
            c.storage().insert(bucket.key(), bucket.amount(), false);
            return false;
        }
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
}
