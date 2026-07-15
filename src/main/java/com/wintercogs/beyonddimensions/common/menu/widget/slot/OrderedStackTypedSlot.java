package com.wintercogs.beyonddimensions.common.menu.widget.slot;

import com.wintercogs.beyonddimensions.api.capability.helper.CapabilityHelper;
import com.wintercogs.beyonddimensions.api.capability.helper.wrapper.FluidHandlerWrapper;
import com.wintercogs.beyonddimensions.api.capability.helper.wrapper.IStackHandlerWrapper;
import com.wintercogs.beyonddimensions.api.capability.helper.wrapper.StackHandlerWrapperHelper;
import com.wintercogs.beyonddimensions.api.storage.handler.IStackHandler;
import com.wintercogs.beyonddimensions.api.storage.key.IStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.KeyAmount;
import com.wintercogs.beyonddimensions.api.storage.key.StackKeyRegistry;
import com.wintercogs.beyonddimensions.api.storage.key.impl.EmptyStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.impl.FluidStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.impl.ItemStackKey;
import com.wintercogs.beyonddimensions.common.init.BDFluids;
import com.wintercogs.beyonddimensions.common.item.XpExchangeItem;
import com.wintercogs.beyonddimensions.common.menu.BDBaseMenu;
import com.wintercogs.beyonddimensions.network.packet.s2c.OrderedStackTypedSlotPacket;
import com.wintercogs.beyonddimensions.util.BDMath;
import com.wintercogs.beyonddimensions.util.XpUtil;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import org.lwjgl.glfw.GLFW;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

public class OrderedStackTypedSlot extends AbstractStackTypedSlot
{
    private KeyAmount lastStack = new KeyAmount(EmptyStackKey.INSTANCE, 0);
    private boolean init = false;

    public OrderedStackTypedSlot(BDBaseMenu menu, IStackHandler stackTypedHandler, int slotIndex, int xPosition, int yPosition)
    {
        super(menu, stackTypedHandler, slotIndex, xPosition, yPosition);
    }

    public OrderedStackTypedSlot(BDBaseMenu menu, IStackHandler stackTypedHandler, int slotIndex, int quickMoveSlotStartIndex, int quickMoveSlotEndIndex, int xPosition, int yPosition)
    {
        super(menu, stackTypedHandler, slotIndex, quickMoveSlotStartIndex, quickMoveSlotEndIndex, xPosition, yPosition);
    }

    @Override
    public boolean isOrdered()
    {
        return true;
    }

    @Override
    public void click(KeyAmount clickStack, int button, Player player)
    {
        ItemStack carriedItem = menu.getCarried().copy();// getCarried方法获取直接引用，所以需要copy防止误操作

        if (clickStack.isEmpty())
        {
            if (!carriedItem.isEmpty())
            {   //槽位物品为空，携带物品存在，将携带物品插入槽位

                AtomicBoolean handled = new AtomicBoolean(false);
                if (carriedItem.getItem() instanceof XpExchangeItem && button != GLFW.GLFW_MOUSE_BUTTON_LEFT)
                {
                    int conversionRate = XpExchangeItem.getConversionRate();
                    double currentLevel = XpUtil.levelAsDouble(player);
                    int wantConversionLevel = XpExchangeItem.getXpLevelPerAction(carriedItem);

                    if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) // 鼠标右键--存入一级
                    {
                        handled.set(true); // 走到这一步说明已经进行了交互
                        long needRemovePlayerXp = XpUtil.xpBetweenLevels(Math.max(currentLevel - wantConversionLevel, 0), currentLevel);
                        int actualRemovePlayerXp = BDMath.clampLongToInt(needRemovePlayerXp);
                        long actualInsertFluid = (long) actualRemovePlayerXp * conversionRate;

                        // 插入当前经验流体
                        KeyAmount remaining = storage.insert(getSlotIndex(), new FluidStackKey(new FluidStack(BDFluids.XP_FLUID.source(), 1)), actualInsertFluid, false);
                        if (!remaining.isEmpty())
                        {
                            int needReturnXp = BDMath.clampLongToInt(remaining.amount() / 20); // 由于前面从int*20，这里除回去
                            actualRemovePlayerXp = actualRemovePlayerXp - needReturnXp;
                        }
                        player.giveExperiencePoints(-actualRemovePlayerXp); // 根据插入的流体给玩家减去经验值
                    }
                }
                // 堆叠数量为1 右键点击 尝试取出内容物并插入
                else if (carriedItem.getCount() == 1 && button == GLFW.GLFW_MOUSE_BUTTON_RIGHT && !ItemCapInteractionBlackList.isInBlackList(carriedItem.getItem()))
                {
                    if (carriedItem.getItem() instanceof BucketItem bucketItem)
                    {
                        Object handler = carriedItem.getCapability(Capabilities.Fluid.ITEM, ItemAccess.forPlayerCursor(player, menu));
                        if (handler != null)
                        {
                            FluidHandlerWrapper stackHandlerWrapper = new FluidHandlerWrapper(handler);

                            if (stackHandlerWrapper.getSlots() > 0)
                            {
                                FluidStack fluidStack = stackHandlerWrapper.getStackInSlot(0);
                                KeyAmount stack = new KeyAmount(new FluidStackKey(fluidStack), fluidStack.getAmount());
                                if (!stack.isEmpty())
                                {
                                    int changedCount = BDMath.clampLongToInt(Math.min(stack.amount(), stack.key().getVanillaMaxStackSize()));
                                    // 进行模拟，桶必须完全清空才被允许操作
                                    int remaining = (int) storage.insert(getSlotIndex(), stack.key(), changedCount, true).amount();
                                    if (remaining <= 0)
                                    {
                                        // 执行实际逻辑
                                        storage.insert(getSlotIndex(), stack.key(), changedCount, false);
                                        menu.setCarried(new ItemStack(Items.BUCKET));
                                        handled.set(true);
                                    }
                                }
                            }
                        }
                    }
                    else
                    {
                        CapabilityHelper.ItemCapabilityMap.forEach((typeId, cap) -> {
                            Object handler = carriedItem.getCapability(cap, ItemAccess.forPlayerCursor(player, menu));
                            if (handler != null)
                            {
                                Function handlerGetter = StackHandlerWrapperHelper.stackWrappers.get(typeId);
                                IStackHandlerWrapper<Object> stackHandlerWrapper = (IStackHandlerWrapper) handlerGetter.apply(handler);

                                if (stackHandlerWrapper.getSlots() > 0)
                                {
                                    for (int index = 0; index < stackHandlerWrapper.getSlots(); index++)
                                    {
                                        IStackKey<?> typeKey = StackKeyRegistry.getType(typeId);
                                        KeyAmount stack = typeKey.fromStackObject(stackHandlerWrapper.getStackInSlot(index));
                                        if (stack != null && !stack.isEmpty())
                                        {
                                            int changedCount = BDMath.clampLongToInt(Math.min(stack.amount(), stack.key().getVanillaMaxStackSize()));
                                            int remaining = (int) storage.insert(getSlotIndex(), stack.key(), changedCount, false).amount();
                                            int actualInsert = changedCount - remaining;

                                            if (actualInsert > 0)
                                            {
                                                long actualExtracts = stackHandlerWrapper.extract(index, actualInsert, false);
                                                if (actualExtracts < actualInsert)
                                                {
                                                    // 对此进行一个回调
                                                    storage.extract(getSlotIndex(), actualInsert - actualExtracts, false);
                                                }
                                                // 重设持有物以应用修改后的handler
                                                stackHandlerWrapper.getContainer()
                                                        .ifPresentOrElse(
                                                                container -> menu.setCarried(container.copy()),
                                                                () -> menu.setCarried(carriedItem.copy()));
                                                handled.set(true);
                                                break;
                                            }
                                        }
                                    }
                                }
                            }
                        });
                    }
                }

                if (!handled.get())
                {
                    int changedCount = button == GLFW.GLFW_MOUSE_BUTTON_LEFT ? carriedItem.getCount() : 1;
                    int remaining = (int) storage.insert(getSlotIndex(), new ItemStackKey(carriedItem), changedCount, false).amount();
                    int actualInsert = changedCount - remaining; // 实际被插入的物品数量

                    int newCount = carriedItem.getCount() - actualInsert; // 实际剩余物品数
                    if (newCount <= 0)
                    {
                        menu.setCarried(ItemStack.EMPTY);
                    }
                    else
                    {
                        ItemStack newCarriedItem = carriedItem.copy();
                        newCarriedItem.setCount(newCount);
                        menu.setCarried(newCarriedItem);
                    }
                }

            }
        }
        else if (mayPickup(player))
        {
            if (carriedItem.isEmpty())
            {   //槽位物品存在，携带物品为空，尝试取出槽位物品
                if (clickStack.key() instanceof ItemStackKey clickKey)
                {
                    // 确保一次取出最大不得超过原版数量
                    int woundChangeNum = BDMath.clampLongToInt(Math.min(clickStack.amount(), clickKey.getVanillaMaxStackSize()));
                    int actualChangeNum = button == GLFW.GLFW_MOUSE_BUTTON_LEFT ? woundChangeNum : (woundChangeNum + 1) / 2;
                    KeyAmount extracted = storage.extract(getSlotIndex(), actualChangeNum, false);
                    if (!extracted.isEmpty() && extracted.toStack() instanceof ItemStack takenItem)
                    {
                        menu.setCarried(takenItem);
                    }
                }
            }
            else if (mayPlace(carriedItem))
            {
                // 槽位物品存在，携带物品存在，当物品为相同类型，尝试插入物品
                if (clickStack.key().isSameTypeSameComponents(new ItemStackKey(carriedItem)))
                {
                    int changedCount = button == GLFW.GLFW_MOUSE_BUTTON_LEFT ? carriedItem.getCount() : 1;
                    int remaining = (int) storage.insert(getSlotIndex(), new ItemStackKey(carriedItem), changedCount, false).amount();
                    int actualInsert = changedCount - remaining; // 实际被插入的物品数量
                    int newCount = carriedItem.getCount() - actualInsert; // 实际剩余物品数
                    if (newCount <= 0)
                    {
                        menu.setCarried(ItemStack.EMPTY);
                    }
                    else
                    {
                        ItemStack newCarriedItem = carriedItem.copy();
                        newCarriedItem.setCount(newCount);
                        menu.setCarried(newCarriedItem);
                    }
                }
                else // 槽位物品存在，携带物品存在，不为相同类型
                {
                    // 先检查是否为经验棒交互
                    if (carriedItem.getItem() instanceof XpExchangeItem && button != GLFW.GLFW_MOUSE_BUTTON_LEFT)
                    {
                        KeyAmount actualStack = getStack();

                        int conversionRate = XpExchangeItem.getConversionRate();
                        double currentLevel = XpUtil.levelAsDouble(player);
                        int wantConversionLevel = XpExchangeItem.getXpLevelPerAction(carriedItem);

                        if (actualStack.key() instanceof FluidStackKey fluidStackKey && fluidStackKey.hasTag(Tags.Fluids.EXPERIENCE))
                        {
                            if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) // 鼠标右键--存入一级
                            {
                                long needRemovePlayerXp = XpUtil.xpBetweenLevels(Math.max(currentLevel - wantConversionLevel, 0), currentLevel);
                                int actualRemovePlayerXp = BDMath.clampLongToInt(needRemovePlayerXp);
                                long actualInsertFluid = (long) actualRemovePlayerXp * conversionRate;

                                // 插入当前经验流体
                                KeyAmount remaining = storage.insert(getSlotIndex(), fluidStackKey, actualInsertFluid, false);
                                if (!remaining.isEmpty())
                                {
                                    int needReturnXp = BDMath.clampLongToInt(remaining.amount() / 20); // 由于前面从int*20，这里除回去
                                    actualRemovePlayerXp = actualRemovePlayerXp - needReturnXp;
                                }
                                player.giveExperiencePoints(-actualRemovePlayerXp); // 根据插入的流体给玩家减去经验值

                            }
                            else if (button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE) // 鼠标中键--取出一级
                            {
                                long needInsertPlayerXp = XpUtil.xpBetweenLevels(currentLevel, currentLevel + wantConversionLevel);
                                int actualInsertPlayerXp = BDMath.clampLongToInt(needInsertPlayerXp);
                                long actualRemoveFluid = actualInsertPlayerXp * conversionRate;

                                // 首先尝试提取指定数量的经验流体
                                KeyAmount extracted = storage.extract(getSlotIndex(), actualRemoveFluid, false);
                                actualInsertPlayerXp = BDMath.clampLongToInt(extracted.amount() / 20);
                                if (actualInsertPlayerXp > 0)
                                {
                                    player.giveExperiencePoints(actualInsertPlayerXp);
                                }
                            }
                        }
                    }
                    // 再检查是否为物品交换
                    else if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT)
                    {
                        KeyAmount actualStack = getStack();
                        if (actualStack.key() instanceof ItemStackKey)
                        {
                            if (carriedItem.getCount() <= getSlotCap() && actualStack.amount() <= actualStack.key().getVanillaMaxStackSize())
                            {
                                // 鼠标携带的数量，小于等于槽位容量
                                // 槽位当前物品数量，小于等于其原版最大数量
                                KeyAmount extract = storage.extract(getSlotIndex(), actualStack.amount(), false);
                                KeyAmount remaining = storage.insert(getSlotIndex(), new ItemStackKey(carriedItem), carriedItem.getCount(), true);
                                if (remaining.isEmpty() && extract.key() instanceof ItemStackKey extractedItemKey)
                                {
                                    // 全部插入时则完成交换
                                    storage.insert(getSlotIndex(), new ItemStackKey(carriedItem), carriedItem.getCount(), false);
                                    menu.setCarried(extractedItemKey.copyStackWithCount(extract.amount()));
                                }
                                else
                                {
                                    // 否则放回取出物
                                    storage.insert(getSlotIndex(), extract.key(), extract.amount(), false);
                                }
                            }
                        }
                    }
                    // 最后检查是否为能力系统交互
                    else if (carriedItem.getCount() == 1 && button == GLFW.GLFW_MOUSE_BUTTON_RIGHT && !ItemCapInteractionBlackList.isInBlackList(carriedItem.getItem()))
                    {
                        if (carriedItem.getItem() instanceof BucketItem)
                        {
                            // 需要分开处理，分别处理
                            // 1.空桶接受
                            // 2.桶向原有区域继续投放
                            if (carriedItem.getItem() == Items.BUCKET) // 空桶接受
                            {
                                if (clickStack.key() instanceof FluidStackKey fluidStackKey)
                                {
                                    Item filledBucket = fluidStackKey.getSource().getBucket();

                                    if (filledBucket != Items.AIR && clickStack.amount() >= 1000)
                                    {
                                        // 执行操作
                                        storage.extract(getSlotIndex(), 1000, false);
                                        menu.setCarried(new ItemStack(filledBucket));
                                    }
                                }
                            }
                            else // 继续投放 insert模拟会自动解决类型不匹配等问题
                            {
                                Object handler = carriedItem.getCapability(Capabilities.Fluid.ITEM, ItemAccess.forPlayerCursor(player, menu));
                                if (handler != null)
                                {
                                    FluidHandlerWrapper stackHandlerWrapper = new FluidHandlerWrapper(handler);

                                    if (stackHandlerWrapper.getSlots() > 0)
                                    {
                                        FluidStack typeStack = stackHandlerWrapper.getStackInSlot(0);
                                        KeyAmount stack = new KeyAmount(new FluidStackKey(typeStack), typeStack.getAmount());
                                        if (!stack.isEmpty())
                                        {
                                            int changedCount = BDMath.clampLongToInt(Math.min(stack.amount(), stack.key().getVanillaMaxStackSize()));
                                            // 进行模拟，桶必须完全清空才被允许操作
                                            int remaining = (int) storage.insert(getSlotIndex(), stack.key(), changedCount, true).amount();
                                            if (remaining <= 0)
                                            {
                                                // 执行实际逻辑
                                                storage.insert(getSlotIndex(), stack.key(), changedCount, false);
                                                menu.setCarried(new ItemStack(Items.BUCKET));
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        else
                        {
                            CapabilityHelper.ItemCapabilityMap.forEach((typeId, cap) -> {
                                // 先查看被点击物品的种类和对应能力种类
                                if (clickStack.key().getTypeId().equals(typeId))
                                {
                                    // 尝试获取对应能力
                                    Object handler = carriedItem.getCapability(cap, ItemAccess.forPlayerCursor(player, menu));
                                    if (handler != null)
                                    {
                                        Function handlerGetter = StackHandlerWrapperHelper.stackWrappers.get(typeId);
                                        IStackHandlerWrapper<Object> stackHandlerWrapper = (IStackHandlerWrapper) handlerGetter.apply(handler);
                                        if (stackHandlerWrapper.getSlots() > 0)
                                        {
                                            // 获取真实最大值 防止数据包伪造
                                            KeyAmount trueStack = storage.getStackBySlot(getSlotIndex());
                                            long tureCount = 0;
                                            if (trueStack.key().isSameTypeSameComponents(clickStack.key()))
                                            {
                                                tureCount = trueStack.amount();
                                            }
                                            int changedCount = BDMath.clampLongToInt(Math.min(tureCount, clickStack.key().getVanillaMaxStackSize()));
                                            int remaining = (int) stackHandlerWrapper.insert(clickStack.toStack(), false);
                                            int actualInsert = changedCount - remaining;
                                            storage.extract(getSlotIndex(), actualInsert, false);
                                            // 重设持有物以应用修改后的handler
                                            stackHandlerWrapper.getContainer()
                                                    .ifPresentOrElse(
                                                            container -> menu.setCarried(container.copy()),
                                                            () -> menu.setCarried(carriedItem.copy()));
                                        }
                                    }
                                }
                            });
                        }
                    }
                }
            }
            else if (clickStack.key().isSameTypeSameComponents(new ItemStackKey(carriedItem)))
            {   // 槽位物品存在，携带物品存在，物品不可放置，为完全相同的物品
                // 此情况在点击维度存储槽时永远不可能发生，如果发生，无需处理
                // 原版逻辑为取出物品到最大上限
                // 保留此情况以便后续使用
            }
        }
    }

    @Override
    protected KeyAmount getQuickMoveStack(KeyAmount clickStack)
    {
        return new KeyAmount(storage.getStackBySlot(theSlot).key(), clickStack.amount());
    }

    @Override
    public KeyAmount safeInsert(IStackKey<?> key, long amount)
    {
        if (key != null)
        {
            // storage的insert应当考虑到一切情况
            return storage.insert(theSlot, key, amount, false);
        }
        return new KeyAmount(ItemStackKey.EMPTY, 0);
    }

    @Override
    public KeyAmount safeExtract(IStackKey<?> key, long amount)
    {
        if (key != null && key.getTypeId().equals(getStack().key().getTypeId()) && key.isSameTypeSameComponents(getStack().key()))
        {
            return storage.extract(theSlot, amount, false);
        }
        return new KeyAmount(EmptyStackKey.INSTANCE, amount);
    }

    @Override
    public void updateChange()
    {
        KeyAmount currentStack = storage.getStackBySlot(this.getSlotIndex());
        if (!init)
        {
            init = true;

            lastStack = currentStack;
            PacketDistributor.sendToPlayer((ServerPlayer) menu.player, new OrderedStackTypedSlotPacket(index, theSlot, lastStack.key(), lastStack.amount()));
        }
        else if (!Objects.equals(currentStack, lastStack))
        {
            lastStack = currentStack;
            PacketDistributor.sendToPlayer((ServerPlayer) menu.player, new OrderedStackTypedSlotPacket(index, theSlot, lastStack.key(), lastStack.amount()));
        }
    }

    @Override
    public void loadChange(int where, IStackKey<?> newStack, long newAmount)
    {
        storage.setStackDirectly(where, newStack, newAmount);
    }
}
