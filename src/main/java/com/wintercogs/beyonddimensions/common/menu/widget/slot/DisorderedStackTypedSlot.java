package com.wintercogs.beyonddimensions.common.menu.widget.slot;

import com.wintercogs.beyonddimensions.api.capability.helper.CapabilityHelper;
import com.wintercogs.beyonddimensions.api.capability.helper.wrapper.FluidHandlerWrapper;
import com.wintercogs.beyonddimensions.api.capability.helper.wrapper.IStackHandlerWrapper;
import com.wintercogs.beyonddimensions.api.capability.helper.wrapper.StackHandlerWrapperHelper;
import com.wintercogs.beyonddimensions.api.storage.handler.IStackHandler;
import com.wintercogs.beyonddimensions.api.storage.key.IStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.KeyAmount;
import com.wintercogs.beyonddimensions.api.storage.key.StackKeyRegistry;
import com.wintercogs.beyonddimensions.api.storage.key.impl.FluidStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.impl.ItemStackKey;
import com.wintercogs.beyonddimensions.common.init.BDFluids;
import com.wintercogs.beyonddimensions.common.item.XpExchangeItem;
import com.wintercogs.beyonddimensions.common.menu.BDBaseMenu;
import com.wintercogs.beyonddimensions.util.BDMath;
import com.wintercogs.beyonddimensions.util.XpUtil;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import org.lwjgl.glfw.GLFW;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

public class DisorderedStackTypedSlot extends AbstractStackTypedSlot
{

    public DisorderedStackTypedSlot(BDBaseMenu menu, IStackHandler stackTypedHandler, int slotIndex, int xPosition, int yPosition)
    {
        super(menu, stackTypedHandler, slotIndex, xPosition, yPosition);
    }

    public DisorderedStackTypedSlot(BDBaseMenu menu, IStackHandler stackTypedHandler, int slotIndex, int quickMoveSlotStartIndex, int quickMoveSlotEndIndex, int xPosition, int yPosition)
    {
        super(menu, stackTypedHandler, slotIndex, quickMoveSlotStartIndex, quickMoveSlotEndIndex, xPosition, yPosition);
    }

    @Override
    public boolean isOrdered()
    {
        return false;
    }

    @Override
    public void click(KeyAmount clickStack, int button, Player player)
    {
        // 获取slot以及获取携带物品 防止网络包伪造
        ItemStack carriedItem = menu.getCarried().copy();// getCarried方法获取直接引用，所以需要copy防止误操作

        if (clickStack.isEmpty())
        {
            if (!carriedItem.isEmpty())
            {   //槽位物品为空，携带物品存在，将携带物品插入槽位

                AtomicBoolean handled = new AtomicBoolean(false);
                // 先检查是否为经验棒交互
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
                        KeyAmount remaining = storage.insert(new FluidStackKey(new FluidStack(BDFluids.XP_FLUID.source(), 1)), actualInsertFluid, false);
                        if (!remaining.isEmpty())
                        {
                            int needReturnXp = BDMath.clampLongToInt(remaining.amount() / 20); // 由于前面从int*20，这里除回去
                            actualRemovePlayerXp = actualRemovePlayerXp - needReturnXp;
                        }
                        player.giveExperiencePoints(-actualRemovePlayerXp); // 根据插入的流体给玩家减去经验值
                    }
                }
                // 再检查是否为能力交互
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
                                FluidStack typeStack = stackHandlerWrapper.getStackInSlot(0);
                                KeyAmount stack = new KeyAmount(new FluidStackKey(typeStack), typeStack.getAmount());
                                if (!stack.isEmpty())
                                {
                                    int changedCount = BDMath.clampLongToInt(Math.min(stack.amount(), stack.key().getVanillaMaxStackSize()));
                                    // 进行模拟，桶必须完全清空才被允许操作
                                    int remaining = (int) storage.insert(stack.key(), changedCount, true).amount();
                                    if (remaining <= 0)
                                    {
                                        // 执行实际逻辑
                                        storage.insert(stack.key(), changedCount, false);
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
                                    // 一次操作只操作其第一个有效槽位，然后break
                                    for (int index = 0; index < stackHandlerWrapper.getSlots(); index++)
                                    {
                                        IStackKey<?> typeKey = StackKeyRegistry.getType(typeId);
                                        KeyAmount stack = typeKey.fromStackObject(stackHandlerWrapper.getStackInSlot(index));
                                        if (stack != null && !stack.isEmpty())
                                        {
                                            int changedCount = BDMath.clampLongToInt(Math.min(stack.amount(), stack.key().getVanillaMaxStackSize()));
                                            int remaining = (int) storage.insert(stack.key(), changedCount, false).amount();
                                            int actualInsert = changedCount - remaining;
                                            if (actualInsert > 0)
                                            {
                                                long actualExtracts = stackHandlerWrapper.extract(index, actualInsert, false);
                                                if (actualExtracts < actualInsert)
                                                {
                                                    // 对此进行一个回调
                                                    storage.extract(stack.key(), actualInsert - actualExtracts, false, false);
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

                // 最终回退
                if (!handled.get())
                {
                    int changedCount = button == GLFW.GLFW_MOUSE_BUTTON_LEFT ? carriedItem.getCount() : 1;
                    int actualInsert = (int) (changedCount - storage.insert(new ItemStackKey(carriedItem), changedCount, false).amount());
                    int newCount = carriedItem.getCount() - actualInsert;
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
            {
                if (clickStack.key() instanceof ItemStackKey clickKey)
                {
                    //槽位物品存在，携带物品为空，尝试取出槽位物品
                    // 确保一次取出最大不得超过原版数量
                    int woundChangeNum = BDMath.clampLongToInt(Math.min(clickStack.amount(), clickKey.getVanillaMaxStackSize()));
                    int actualChangeNum = button == GLFW.GLFW_MOUSE_BUTTON_LEFT ? woundChangeNum : (woundChangeNum + 1) / 2;
                    ItemStack takenItem = (ItemStack) storage.extract(clickKey, actualChangeNum, false, false).toStack();
                    if (takenItem != null)
                    {
                        menu.setCarried(takenItem);
                    }
                }
            }
            else if (mayPlace(carriedItem)) // 槽位物品存在，携带物品存在
            {
                // 标记是否进行了交互
                AtomicBoolean handled = new AtomicBoolean(false);

                // 先检查是否为经验棒交互
                if (carriedItem.getItem() instanceof XpExchangeItem && button != GLFW.GLFW_MOUSE_BUTTON_LEFT)
                {
                    KeyAmount actualStack = storage.getStackByKey(clickStack.key());

                    int conversionRate = XpExchangeItem.getConversionRate();
                    double currentLevel = XpUtil.levelAsDouble(player);
                    int wantConversionLevel = XpExchangeItem.getXpLevelPerAction(carriedItem);

                    if (actualStack.key() instanceof FluidStackKey fluidStackKey && fluidStackKey.hasTag(Tags.Fluids.EXPERIENCE))
                    {
                        handled.set(true); // 走到这一步说明已经进行了交互
                        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) // 鼠标右键--存入一级
                        {
                            long needRemovePlayerXp = XpUtil.xpBetweenLevels(Math.max(currentLevel - wantConversionLevel, 0), currentLevel);
                            int actualRemovePlayerXp = BDMath.clampLongToInt(needRemovePlayerXp);
                            long actualInsertFluid = (long) actualRemovePlayerXp * conversionRate;

                            // 插入当前经验流体
                            KeyAmount remaining = storage.insert(fluidStackKey, actualInsertFluid, false);
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
                            KeyAmount extracted = storage.extract(fluidStackKey, actualRemoveFluid, false, false);
                            actualInsertPlayerXp = BDMath.clampLongToInt(extracted.amount() / 20);
                            if (actualInsertPlayerXp > 0)
                            {
                                player.giveExperiencePoints(actualInsertPlayerXp);
                            }
                        }
                    }
                    else
                    {
                        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) // 鼠标右键--存入一级
                        {
                            handled.set(true); // 走到这一步说明已经进行了交互
                            long needRemovePlayerXp = XpUtil.xpBetweenLevels(Math.max(currentLevel - wantConversionLevel, 0), currentLevel);
                            int actualRemovePlayerXp = BDMath.clampLongToInt(needRemovePlayerXp);
                            long actualInsertFluid = (long) actualRemovePlayerXp * conversionRate;

                            // 插入当前经验流体
                            KeyAmount remaining = storage.insert(new FluidStackKey(new FluidStack(BDFluids.XP_FLUID.source(), 1)), actualInsertFluid, false);
                            if (!remaining.isEmpty())
                            {
                                int needReturnXp = BDMath.clampLongToInt(remaining.amount() / 20); // 由于前面从int*20，这里除回去
                                actualRemovePlayerXp = actualRemovePlayerXp - needReturnXp;
                            }
                            player.giveExperiencePoints(-actualRemovePlayerXp); // 根据插入的流体给玩家减去经验值
                        }
                    }
                }
                else if (!ItemCapInteractionBlackList.isInBlackList(carriedItem.getItem()))//再检查是否为能力交互
                {
                    // 如果使用一个有存储能力的单个物品，点击右键，
                    // 则，尝试将目标抽入到自身。如果抽取失败
                    // 则，尝试将自身内容物存入网络。
                    // 最后，如果以上两个操作均未进行，则将物品本身存入

                    if (carriedItem.getCount() == 1 && button == GLFW.GLFW_MOUSE_BUTTON_RIGHT)
                    {
                        // 对桶物品进行特殊处理
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

                                    if (filledBucket != Items.AIR && storage.getStackByKey(fluidStackKey).amount() >= 1000)
                                    {
                                        // 执行操作
                                        storage.extract(fluidStackKey, 1000, false, false);
                                        menu.setCarried(new ItemStack(filledBucket));
                                        handled.set(true);
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
                                            int remaining = (int) storage.insert(stack.key(), changedCount, true).amount();
                                            if (remaining <= 0)
                                            {
                                                // 执行实际逻辑
                                                storage.insert(stack.key(), changedCount, false);
                                                menu.setCarried(new ItemStack(Items.BUCKET));
                                                handled.set(true);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        else
                        {
                            // 抽入
                            CapabilityHelper.ItemCapabilityMap.forEach((typeId, cap) -> {
                                // 先查看被点击物品的种类和对应能力种类
                                if (clickStack.key().getTypeId().equals(typeId))
                                {
                                    // 尝试获取对应能力
                                    Object handler = carriedItem.getCapability(cap, ItemAccess.forPlayerCursor(player, menu));
                                    if (handler != null)
                                    {
                                        Function handlerGetter = StackHandlerWrapperHelper.stackWrappers.get(typeId);
                                        IStackHandlerWrapper<Object> stackHandlerWrapper = (IStackHandlerWrapper<Object>) handlerGetter.apply(handler);
                                        if (stackHandlerWrapper.getSlots() > 0)
                                        {
                                            KeyAmount actualClickStack = storage.getStackByKey(clickStack.key());// 防止客户端假消息
                                            if (!actualClickStack.isEmpty())
                                            {
                                                int changedCount = BDMath.clampLongToInt(Math.min(actualClickStack.amount(), actualClickStack.key().getVanillaMaxStackSize()));
                                                int remaining = (int) stackHandlerWrapper.insert(actualClickStack.key().copyStackWithCount(changedCount), false);
                                                int actualInsert = changedCount - remaining;
                                                if (actualInsert > 0)
                                                {
                                                    storage.extract(actualClickStack.key(), actualInsert, false, false);
                                                    // 重设持有物以应用修改后的handler
                                                    stackHandlerWrapper.getContainer()
                                                            .ifPresentOrElse(
                                                                    container -> menu.setCarried(container.copy()),
                                                                    () -> menu.setCarried(carriedItem.copy()));
                                                    handled.set(true);
                                                }
                                            }

                                        }
                                    }
                                }
                            });

                            //存入
                            if (!handled.get())
                            {
                                CapabilityHelper.ItemCapabilityMap.forEach((typeId, cap) -> {
                                    Object handler = carriedItem.getCapability(cap, ItemAccess.forPlayerCursor(player, menu));
                                    if (handler != null)
                                    {
                                        Function handlerGetter = StackHandlerWrapperHelper.stackWrappers.get(typeId);
                                        IStackHandlerWrapper<Object> stackHandlerWrapper = (IStackHandlerWrapper) handlerGetter.apply(handler);

                                        if (stackHandlerWrapper.getSlots() > 0)
                                        {
                                            // 一次操作只操作其第一个有效槽位，然后break
                                            for (int index = 0; index < stackHandlerWrapper.getSlots(); index++)
                                            {
                                                IStackKey<?> typeKey = StackKeyRegistry.getType(typeId);
                                                KeyAmount stack = typeKey.fromStackObject(stackHandlerWrapper.getStackInSlot(index));
                                                if (stack != null && !stack.isEmpty())
                                                {
                                                    int changedCount = BDMath.clampLongToInt(Math.min(stack.amount(), stack.key().getVanillaMaxStackSize()));
                                                    int remaining = (int) storage.insert(stack.key(), changedCount, false).amount();
                                                    int actualInsert = changedCount - remaining;

                                                    if (actualInsert > 0)
                                                    {
                                                        long actualExtracts = stackHandlerWrapper.extract(index, actualInsert, false);
                                                        if (actualExtracts < actualInsert)
                                                        {
                                                            // 对此进行一个回调
                                                            storage.extract(stack.key(), actualInsert - actualExtracts, false, false);
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
                    }
                }

                // 最终回退处理（无任何交互时，放回此物品）
                if (!handled.get())
                {
                    //槽位物品存在，携带物品存在，物品可以放置，尝试将物品放入
                    int changedCount = button == GLFW.GLFW_MOUSE_BUTTON_LEFT ? carriedItem.getCount() : 1;
                    int actualInsert = (int) (changedCount - storage.insert(new ItemStackKey(carriedItem), changedCount, false).amount());
                    int newCount = carriedItem.getCount() - actualInsert;
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
            else if (clickStack.key().isSameTypeSameComponents(new ItemStackKey(carriedItem)))
            {   // 槽位物品存在，携带物品存在，物品不可放置，为完全相同的物品
                // 此情况在点击维度存储槽时永远不可能发生，如果发生，无需处理
                // 原版逻辑为取出物品到最大上限
                // 保留此情况以便后续使用
            }

        }
    }

    @Override
    public KeyAmount safeInsert(IStackKey<?> stack, long amount)
    {
        if (stack != null)
        {
            return storage.insert(stack, amount, false);
        }
        return new KeyAmount(ItemStackKey.EMPTY, 0);

    }

    @Override
    public KeyAmount safeExtract(IStackKey<?> stack, long amount)
    {
        if (stack != null)
        {
            return storage.extract(stack, amount, false, false);
        }
        return new KeyAmount(ItemStackKey.EMPTY, 0);
    }

    // 无序槽位由槽位组负责处理同步
    @Override
    public void updateChange()
    {

    }

    @Override
    public void loadChange(int where, IStackKey<?> newStack, long newAmount)
    {

    }
}
