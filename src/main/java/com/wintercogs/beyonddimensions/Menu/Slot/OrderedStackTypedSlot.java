package com.wintercogs.beyonddimensions.Menu.Slot;

import com.wintercogs.beyonddimensions.Api.DataBase.Handler.IStackHandler;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.*;
import com.wintercogs.beyonddimensions.Api.DataBase.StackHandlerWrapper.FluidHandlerWrapper;
import com.wintercogs.beyonddimensions.Api.DataBase.StackHandlerWrapper.IStackHandlerWrapper;
import com.wintercogs.beyonddimensions.Api.Registry.CapabilityHelper;
import com.wintercogs.beyonddimensions.Api.Registry.StackHandlerWrapperHelper;
import com.wintercogs.beyonddimensions.Api.Registry.StackKeyRegistry;
import com.wintercogs.beyonddimensions.Fluid.ModFluids;
import com.wintercogs.beyonddimensions.Item.Custom.XpExchangeItem;
import com.wintercogs.beyonddimensions.Menu.BDBaseMenu;
import com.wintercogs.beyonddimensions.Network.Packet.toClient.OrderedStackTypedSlotPacket;
import com.wintercogs.beyonddimensions.Registry.PacketRegister;
import com.wintercogs.beyonddimensions.Tags.ModFluidTags;
import com.wintercogs.beyonddimensions.Util.BDMath;
import com.wintercogs.beyonddimensions.Util.XpUtil;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.*;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

public class OrderedStackTypedSlot extends AbstractStackTypedSlot
{
    private KeyAmount lastStack = new KeyAmount(ItemStackKey.EMPTY, 0);

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
                        KeyAmount remaining = storage.insert(getSlotIndex(), new FluidStackKey(new FluidStack(ModFluids.XP_FLUID.source().get(), 1)), actualInsertFluid, false);
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
                    if (carriedItem.getItem() instanceof BucketItem bucketItem || carriedItem.getItem() instanceof MilkBucketItem)
                    {
                        LazyOptional<IFluidHandlerItem> handler = carriedItem.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM);
                        if (handler.isPresent())
                        {
                            FluidHandlerWrapper stackHandlerWrapper = new FluidHandlerWrapper(handler.resolve().get());

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
                            LazyOptional<?> handler = carriedItem.getCapability(cap);
                            if (handler.isPresent())
                            {
                                Function handlerGetter = StackHandlerWrapperHelper.stackWrappers.get(typeId);
                                IStackHandlerWrapper<Object> stackHandlerWrapper = (IStackHandlerWrapper) handlerGetter.apply(handler.resolve().get());

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
                    ItemStack takenItem = (ItemStack) storage.extract(getSlotIndex(), actualChangeNum, false).toStack();
                    if (takenItem != null)
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

                        if (actualStack.key() instanceof FluidStackKey fluidStackKey && fluidStackKey.hasTag(ModFluidTags.C_EXPERIENCE))
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
                                if (remaining.isEmpty())
                                {
                                    // 全部插入时则完成交换
                                    storage.insert(getSlotIndex(), new ItemStackKey(carriedItem), carriedItem.getCount(), false);
                                    menu.setCarried((ItemStack) extract.toStack());
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
                        if (carriedItem.getItem() instanceof BucketItem || carriedItem.getItem() instanceof MilkBucketItem)
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
                                LazyOptional<?> handler = carriedItem.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM);
                                if (handler.isPresent())
                                {
                                    FluidHandlerWrapper stackHandlerWrapper = new FluidHandlerWrapper(handler.resolve().get());

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
                                    LazyOptional<?> handler = carriedItem.getCapability(cap);
                                    if (handler.isPresent())
                                    {
                                        Function handlerGetter = StackHandlerWrapperHelper.stackWrappers.get(typeId);
                                        IStackHandlerWrapper<Object> stackHandlerWrapper = (IStackHandlerWrapper) handlerGetter.apply(handler.resolve().get());
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
    public void quickMove(KeyAmount clickStack, int button, Player player)
    {
        // 虽然当前的默认值不会导致出现问题，但还是添加执行前检查，防止某一天遗漏
        if (!(quickMoveSlotStartIndex >= 0 && quickMoveSlotEndIndex >= 0 && quickMoveSlotStartIndex < quickMoveSlotEndIndex))
            return;
        if (!clickStack.isEmpty())
        {
            // TODO
            // 这里的trueStack和注释并不正确，实际上后续操作中extract本身就不会提取超出真实数量的值，本身即有数据包验证的效果
            // 这里的trueStack更类似于wannaStack，这里先加上这些注释，后续有空再改名
            // 之前错误的注释：防止数据包伪造，然后赋予trueStack需要提取的数量
            KeyAmount trueStack = new KeyAmount(storage.getStackBySlot(theSlot).key(), clickStack.amount());

            // 遍历目标槽位
            for (int targetSlotIndex = quickMoveSlotStartIndex; targetSlotIndex < quickMoveSlotEndIndex && !trueStack.isEmpty(); targetSlotIndex++)
            {
                Slot slot = menu.slots.get(targetSlotIndex);
                if (slot instanceof AbstractStackTypedSlot aSlot)
                {
                    // aSlot处理任何情况

                    //首先尝试从存储提取指定堆叠
                    KeyAmount extract = safeExtract(trueStack.key(), trueStack.amount());
                    KeyAmount remaining = aSlot.safeInsert(extract.key(), extract.amount()); // 然后插入到其他堆叠并获取余量
                    if (!remaining.isEmpty())
                        safeInsert(remaining.key(), remaining.amount()); // 最后将余量返回
                    trueStack = remaining;

                }
                else // 目标slot为非StackTypedSlot时
                {
                    IStackKey<?> key = trueStack.key();

                    // 物品转移
                    if (key instanceof ItemStackKey trueItemTypedKey)
                    {
                        ItemStack extract = (ItemStack) safeExtract(trueItemTypedKey, trueStack.amount()).toStack();
                        ItemStack remaining = slot.safeInsert(extract);
                        if (!remaining.isEmpty())
                            safeInsert(new ItemStackKey(remaining), remaining.getCount());
                        trueStack = new KeyAmount(new ItemStackKey(remaining), remaining.getCount());
                    }
                    // 移动流体并装桶
                    else if (key instanceof FluidStackKey trueFluidTypedKey && trueFluidTypedKey.getSource().getBucket() != Items.AIR)
                    {
                        KeyAmount extract = safeExtract(trueFluidTypedKey, 1000);
                        if (extract.amount() != 1000)
                        {
                            safeInsert(extract.key(), extract.amount());
                            break;
                        }

                        KeyAmount bucket = storage.extract(new ItemStackKey(new ItemStack(Items.BUCKET)), 1, false, false);
                        if (bucket.isEmpty())
                        {
                            safeInsert(extract.key(), extract.amount());
                            break;
                        }

                        Item bucketItem = trueFluidTypedKey.getSource().getBucket();
                        ItemStack insertStack = new ItemStack(bucketItem);
                        ItemStack remaining = slot.safeInsert(insertStack);
                        if (!remaining.isEmpty())
                        {
                            safeInsert(extract.key(), extract.amount());
                            storage.insert(bucket.key(), bucket.amount(), false);
                            continue;
                        }
                        trueStack = new KeyAmount(trueFluidTypedKey, trueStack.amount() - 1000);
                        break; // 更新trueStack以保持语义相同，但是这里我们break，以确保一次点击最多只成功装桶一次
                    }
                }

            }
            setChanged();
        }
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
        if (lastStack.amount() != currentStack.amount()
                || !lastStack.key().getTypeId().equals(currentStack.key().getTypeId())
                || !lastStack.key().isSameTypeSameComponents(currentStack.key()))
        {
            lastStack = currentStack;
            PacketRegister.INSTANCE.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) menu.player), new OrderedStackTypedSlotPacket(index, theSlot, lastStack.key(), lastStack.amount()));
        }
    }

    @Override
    public void loadChange(int where, IStackKey<?> newStack, long newAmount)
    {
        storage.setStackDirectly(where, newStack, newAmount);
    }
}
