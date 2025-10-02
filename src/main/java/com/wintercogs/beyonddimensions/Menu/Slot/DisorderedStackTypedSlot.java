package com.wintercogs.beyonddimensions.Menu.Slot;

import com.wintercogs.beyonddimensions.Api.DataBase.Handler.IStackHandler;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.FluidStackKey;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.IStackKey;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.ItemStackKey;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.KeyAmount;
import com.wintercogs.beyonddimensions.Api.DataBase.StackHandlerWrapper.FluidHandlerWrapper;
import com.wintercogs.beyonddimensions.Api.DataBase.StackHandlerWrapper.IStackHandlerWrapper;
import com.wintercogs.beyonddimensions.Api.Registry.CapabilityHelper;
import com.wintercogs.beyonddimensions.Api.Registry.StackHandlerWrapperHelper;
import com.wintercogs.beyonddimensions.Api.Registry.StackKeyRegistry;
import com.wintercogs.beyonddimensions.Fluid.ModFluids;
import com.wintercogs.beyonddimensions.Item.Custom.XpExchangeItem;
import com.wintercogs.beyonddimensions.Menu.BDBaseMenu;
import com.wintercogs.beyonddimensions.Tags.ModFluidTags;
import com.wintercogs.beyonddimensions.Unit.BDMath;
import com.wintercogs.beyonddimensions.Unit.XpUtil;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
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
                if(carriedItem.getItem() instanceof XpExchangeItem && button != GLFW.GLFW_MOUSE_BUTTON_LEFT)
                {
                    int conversionRate = XpExchangeItem.getConversionRate();
                    double currentLevel = XpUtil.levelAsDouble(player);
                    int wantConversionLevel = XpExchangeItem.getXpLevelPerAction(carriedItem);

                    if(button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) // 鼠标右键--存入一级
                    {
                        handled.set(true); // 走到这一步说明已经进行了交互
                        long needRemovePlayerXp = XpUtil.xpBetweenLevels(Math.max(currentLevel-wantConversionLevel,0),currentLevel);
                        int actualRemovePlayerXp = BDMath.clampLongToInt(needRemovePlayerXp);
                        long actualInsertFluid = (long) actualRemovePlayerXp * conversionRate;

                        // 插入当前经验流体
                        KeyAmount remaining = storage.insert(new FluidStackKey(new FluidStack(ModFluids.XP_FLUID.source(),1)),actualInsertFluid,false);
                        if(!remaining.isEmpty())
                        {
                            int needReturnXp = BDMath.clampLongToInt(remaining.amount()/20); // 由于前面从int*20，这里除回去
                            actualRemovePlayerXp = actualRemovePlayerXp - needReturnXp;
                        }
                        player.giveExperiencePoints(-actualRemovePlayerXp); // 根据插入的流体给玩家减去经验值
                    }
                }
                // 再检查是否为能力交互
                else if(carriedItem.getCount()==1 && button== GLFW.GLFW_MOUSE_BUTTON_RIGHT && !ItemCapInteractionBlackList.isInBlackList(carriedItem.getItem()))
                {
                    if(carriedItem.getItem() instanceof BucketItem bucketItem || carriedItem.getItem() instanceof MilkBucketItem)
                    {
                        Object handler = carriedItem.getCapability(Capabilities.FluidHandler.ITEM);
                        if(handler != null)
                        {
                            FluidHandlerWrapper stackHandlerWrapper = new FluidHandlerWrapper(handler);

                            if(stackHandlerWrapper.getSlots()>0)
                            {
                                FluidStack typeStack = stackHandlerWrapper.getStackInSlot(0);
                                KeyAmount stack = new KeyAmount(new FluidStackKey(typeStack),typeStack.getAmount());
                                if(stack.key() != null && !stack.isEmpty())
                                {
                                    int changedCount = BDMath.clampLongToInt(Math.min(stack.amount(),stack.key().getVanillaMaxStackSize()));
                                    // 进行模拟，桶必须完全清空才被允许操作
                                    int remaining = (int)storage.insert(stack.key(),changedCount,true).amount();
                                    if(remaining<=0)
                                    {
                                        // 执行实际逻辑
                                        storage.insert(stack.key(),changedCount,false).amount();
                                        menu.setCarried(new ItemStack(Items.BUCKET));
                                        handled.set(true);
                                    }
                                }
                            }
                        }
                    }
                    else
                    {
                        CapabilityHelper.ItemCapabilityMap.forEach((typeId, cap)->{
                            Object handler = carriedItem.getCapability(cap);
                            if(handler != null)
                            {
                                Function handlerGetter = StackHandlerWrapperHelper.stackWrappers.get(typeId);
                                IStackHandlerWrapper stackHandlerWrapper = (IStackHandlerWrapper) handlerGetter.apply(handler);

                                if(stackHandlerWrapper.getSlots()>0)
                                {
                                    // 一次操作只操作其第一个有效槽位，然后break
                                    for(int index=0;index<stackHandlerWrapper.getSlots();index++)
                                    {
                                        IStackKey<?> typeKey = StackKeyRegistry.getType(typeId);
                                        KeyAmount stack = typeKey.fromStackObject(stackHandlerWrapper.getStackInSlot(index));
                                        if(stack !=null&& !stack.isEmpty())
                                        {
                                            int changedCount = BDMath.clampLongToInt(Math.min(stack.amount(),stack.key().getVanillaMaxStackSize()));
                                            int remaining = (int)storage.insert(stack.key(),changedCount,false).amount();
                                            int actualInsert = changedCount - remaining;
                                            if(actualInsert>0)
                                            {
                                                long actualExtracts = stackHandlerWrapper.extract(index,actualInsert,false);
                                                if(actualExtracts< actualInsert)
                                                {
                                                    // 对此进行一个回调
                                                    storage.extract(stack.key(),actualInsert-actualExtracts,false);
                                                }
                                                menu.setCarried(carriedItem.copy()); // 重设持有物以应用修改后的handler
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
                if(!handled.get())
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
                if(clickStack.key() instanceof ItemStackKey clickKey)
                {
                    //槽位物品存在，携带物品为空，尝试取出槽位物品
                    // 确保一次取出最大不得超过原版数量
                    int woundChangeNum = BDMath.clampLongToInt(Math.min(clickStack.amount(), clickKey.getVanillaMaxStackSize()));
                    int actualChangeNum = button == GLFW.GLFW_MOUSE_BUTTON_LEFT ? woundChangeNum : (woundChangeNum + 1) / 2;
                    ItemStack takenItem = (ItemStack) storage.extract(clickKey, actualChangeNum,false).toStack();
                    if(takenItem != null)
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

                    if(actualStack.key() instanceof FluidStackKey fluidStackKey && fluidStackKey.hasTag(ModFluidTags.C_EXPERIENCE))
                    {
                        handled.set(true); // 走到这一步说明已经进行了交互
                        if(button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) // 鼠标右键--存入一级
                        {
                            long needRemovePlayerXp = XpUtil.xpBetweenLevels(Math.max(currentLevel-wantConversionLevel,0),currentLevel);
                            int actualRemovePlayerXp = BDMath.clampLongToInt(needRemovePlayerXp);
                            long actualInsertFluid = (long) actualRemovePlayerXp * conversionRate;

                            // 插入当前经验流体
                            KeyAmount remaining = storage.insert(fluidStackKey,actualInsertFluid,false);
                            if(!remaining.isEmpty())
                            {
                                int needReturnXp = BDMath.clampLongToInt(remaining.amount()/20); // 由于前面从int*20，这里除回去
                                actualRemovePlayerXp = actualRemovePlayerXp - needReturnXp;
                            }
                            player.giveExperiencePoints(-actualRemovePlayerXp); // 根据插入的流体给玩家减去经验值
                        }
                        else if(button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE) // 鼠标中键--取出一级
                        {
                            long needInsertPlayerXp = XpUtil.xpBetweenLevels(currentLevel,currentLevel+wantConversionLevel);
                            int actualInsertPlayerXp = BDMath.clampLongToInt(needInsertPlayerXp);
                            long actualRemoveFluid = actualInsertPlayerXp * conversionRate;

                            // 首先尝试提取指定数量的经验流体
                            KeyAmount extracted = storage.extract(fluidStackKey, actualRemoveFluid ,false);
                            actualInsertPlayerXp = BDMath.clampLongToInt(extracted.amount()/20);
                            if(actualInsertPlayerXp > 0)
                            {
                                player.giveExperiencePoints(actualInsertPlayerXp);
                            }
                        }
                    }
                    else
                    {
                        if(button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) // 鼠标右键--存入一级
                        {
                            handled.set(true); // 走到这一步说明已经进行了交互
                            long needRemovePlayerXp = XpUtil.xpBetweenLevels(Math.max(currentLevel-wantConversionLevel,0),currentLevel);
                            int actualRemovePlayerXp = BDMath.clampLongToInt(needRemovePlayerXp);
                            long actualInsertFluid = (long) actualRemovePlayerXp * conversionRate;

                            // 插入当前经验流体
                            KeyAmount remaining = storage.insert(new FluidStackKey(new FluidStack(ModFluids.XP_FLUID.source(),1)), actualInsertFluid,false);
                            if(!remaining.isEmpty())
                            {
                                int needReturnXp = BDMath.clampLongToInt(remaining.amount()/20); // 由于前面从int*20，这里除回去
                                actualRemovePlayerXp = actualRemovePlayerXp - needReturnXp;
                            }
                            player.giveExperiencePoints(-actualRemovePlayerXp); // 根据插入的流体给玩家减去经验值
                        }
                    }
                }
                else if(!ItemCapInteractionBlackList.isInBlackList(carriedItem.getItem()))//再检查是否为能力交互
                {
                    // 如果使用一个有存储能力的单个物品，点击右键，
                    // 则，尝试将目标抽入到自身。如果抽取失败
                    // 则，尝试将自身内容物存入网络。
                    // 最后，如果以上两个操作均未进行，则将物品本身存入

                    if(carriedItem.getCount() == 1 && button == GLFW.GLFW_MOUSE_BUTTON_RIGHT)
                    {
                        // 对桶物品进行特殊处理
                        if(carriedItem.getItem() instanceof BucketItem bucket)
                        {
                            // 需要分开处理，分别处理
                            // 1.空桶接受
                            // 2.桶向原有区域继续投放
                            if(bucket == Items.BUCKET) // 空桶接受
                            {
                                if(clickStack.key() instanceof FluidStackKey fluidStackKey)
                                {
                                    Item filledBucket = fluidStackKey.getSource().getBucket();

                                    if(filledBucket != Items.AIR && storage.getStackByKey(fluidStackKey).amount() >= 1000)
                                    {
                                        // 执行操作
                                        storage.extract(fluidStackKey,1000,false);
                                        menu.setCarried(new ItemStack(filledBucket));
                                        handled.set(true);
                                    }
                                }
                            }
                            else // 继续投放 insert模拟会自动解决类型不匹配等问题
                            {
                                Object handler = carriedItem.getCapability(Capabilities.FluidHandler.ITEM);
                                if(handler != null)
                                {
                                    FluidHandlerWrapper stackHandlerWrapper = new FluidHandlerWrapper(handler);

                                    if(stackHandlerWrapper.getSlots()>0)
                                    {
                                        FluidStack typeStack = stackHandlerWrapper.getStackInSlot(0);
                                        KeyAmount stack = new KeyAmount(new FluidStackKey(typeStack),typeStack.getAmount());
                                        if(stack.key() != null && !stack.isEmpty())
                                        {
                                            int changedCount = BDMath.clampLongToInt(Math.min(stack.amount(),stack.key().getVanillaMaxStackSize()));
                                            // 进行模拟，桶必须完全清空才被允许操作
                                            int remaining = (int)storage.insert(stack.key(),changedCount,true).amount();
                                            if(remaining<=0)
                                            {
                                                // 执行实际逻辑
                                                storage.insert(stack.key(),changedCount,false);
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
                            CapabilityHelper.ItemCapabilityMap.forEach((typeId,cap) -> {
                                // 先查看被点击物品的种类和对应能力种类
                                if(clickStack.key().getTypeId().equals(typeId))
                                {
                                    // 尝试获取对应能力
                                    Object handler = carriedItem.getCapability(cap);
                                    if(handler != null)
                                    {
                                        Function handlerGetter = StackHandlerWrapperHelper.stackWrappers.get(typeId);
                                        IStackHandlerWrapper stackHandlerWrapper = (IStackHandlerWrapper) handlerGetter.apply(handler);
                                        if(stackHandlerWrapper.getSlots()>0)
                                        {
                                            KeyAmount actualClickStack = storage.getStackByKey(clickStack.key());// 防止客户端假消息
                                            if(!actualClickStack.isEmpty())
                                            {
                                                int changedCount = BDMath.clampLongToInt(Math.min(actualClickStack.amount(),actualClickStack.key().getVanillaMaxStackSize()));
                                                int remaining = (int)stackHandlerWrapper.insert(actualClickStack.key().copyStackWithCount(changedCount),false);
                                                int actualInsert = changedCount - remaining;
                                                if(actualInsert>0)
                                                {
                                                    storage.extract(actualClickStack.key(),actualInsert,false);
                                                    menu.setCarried(carriedItem.copy()); // 重设持有物以应用修改后的handler
                                                    handled.set(true);
                                                }
                                            }

                                        }
                                    }
                                }
                            });

                            //存入
                            if(!handled.get())
                            {
                                CapabilityHelper.ItemCapabilityMap.forEach((typeId,cap) -> {
                                    Object handler = carriedItem.getCapability(cap);
                                    if(handler != null)
                                    {
                                        Function handlerGetter = StackHandlerWrapperHelper.stackWrappers.get(typeId);
                                        IStackHandlerWrapper stackHandlerWrapper = (IStackHandlerWrapper) handlerGetter.apply(handler);

                                        if(stackHandlerWrapper.getSlots()>0)
                                        {
                                            // 一次操作只操作其第一个有效槽位，然后break
                                            for(int index=0;index<stackHandlerWrapper.getSlots();index++)
                                            {
                                                IStackKey<?> typeKey = StackKeyRegistry.getType(typeId);
                                                KeyAmount stack = typeKey.fromStackObject(stackHandlerWrapper.getStackInSlot(index));
                                                if(stack !=null&& !stack.isEmpty())
                                                {
                                                    int changedCount = BDMath.clampLongToInt(Math.min(stack.amount(),stack.key().getVanillaMaxStackSize()));
                                                    int remaining = (int)storage.insert(stack.key(),changedCount,false).amount();
                                                    int actualInsert = changedCount - remaining;

                                                    if(actualInsert>0)
                                                    {
                                                        long actualExtracts = stackHandlerWrapper.extract(index,actualInsert,false);
                                                        if(actualExtracts< actualInsert)
                                                        {
                                                            // 对此进行一个回调
                                                            storage.extract(stack.key(),actualInsert-actualExtracts,false);
                                                        }
                                                        menu.setCarried(carriedItem.copy()); // 重设持有物以应用修改后的handler
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
                if(!handled.get())
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
    public void quickMove(KeyAmount clickStack, int button, Player player)
    {
        // 虽然当前的默认值不会导致出现问题，但还是添加执行前检查，防止某一天遗漏
        if(!(quickMoveSlotStartIndex >= 0 && quickMoveSlotEndIndex >= 0 && quickMoveSlotStartIndex < quickMoveSlotEndIndex))
            return;
        if (!clickStack.isEmpty())
        {
            // 防止数据包伪造，然后赋予trueStack需要提取的数量
            KeyAmount trueKA = storage.getStackByKey(clickStack.key());
            KeyAmount trueStack = new KeyAmount(trueKA.key(),clickStack.amount());

            // 遍历目标槽位
            for(int targetSlotIndex=quickMoveSlotStartIndex;targetSlotIndex<quickMoveSlotEndIndex && !trueStack.isEmpty();targetSlotIndex++)
            {
                Slot slot = menu.slots.get(targetSlotIndex);
                if(slot instanceof AbstractStackTypedSlot aSlot)
                {
                    // aSlot处理任何情况

                    //首先尝试从存储提取指定堆叠
                    KeyAmount extract = storage.extract(trueStack.key(),trueStack.amount(),false);
                    KeyAmount remaining = aSlot.safeInsert(extract.key(),extract.amount()); // 然后插入到其他堆叠并获取余量
                    if(!remaining.isEmpty())
                        storage.insert(remaining.key(),remaining.amount(),false); // 最后将余量返回
                    trueStack = remaining;

                }
                else
                {
                    // 普通Slot将只处理物品转移
                    if(trueStack.key() instanceof ItemStackKey trueItemTypedKey)
                    {
                        ItemStack extract = (ItemStack) storage.extract(trueItemTypedKey,trueStack.amount(),false).toStack();
                        ItemStack remaining = slot.safeInsert(extract);
                        if(!remaining.isEmpty())
                            storage.insert(new ItemStackKey(remaining) , remaining.getCount(),false);
                        trueStack = new KeyAmount(new ItemStackKey(remaining) , remaining.getCount());
                    }
                }

            }
            setChanged();
        }
    }

    @Override
    public KeyAmount safeInsert(IStackKey<?> stack, long amount)
    {
        if(stack != null)
        {
            return storage.insert(stack,amount,false);
        }
        return new KeyAmount(ItemStackKey.EMPTY,0);

    }

    @Override
    public KeyAmount safeExtract(IStackKey<?> stack, long amount)
    {
        if(stack != null)
        {
            return storage.extract(stack,amount,false);
        }
        return new KeyAmount(ItemStackKey.EMPTY,0);
    }

    // 无序槽位由槽位组负责处理同步
    @Override
    public void updateChange()
    {

    }

    @Override
    public void loadChange(int where ,IStackKey<?> newStack, long newAmount)
    {

    }
}
