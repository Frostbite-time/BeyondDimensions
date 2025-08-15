package com.wintercogs.beyonddimensions.Menu.Slot;

import com.wintercogs.beyonddimensions.Api.DataBase.Handler.IStackTypedHandler;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.FluidStackType;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.IStackType;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.ItemStackType;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.StackCreater;
import com.wintercogs.beyonddimensions.Api.DataBase.StackHandlerWrapper.FluidHandlerWrapper;
import com.wintercogs.beyonddimensions.Api.DataBase.StackHandlerWrapper.IStackHandlerWrapper;
import com.wintercogs.beyonddimensions.Api.Registry.CapabilityHelper;
import com.wintercogs.beyonddimensions.Api.Registry.StackHandlerWrapperHelper;
import com.wintercogs.beyonddimensions.Fluid.ModFluids;
import com.wintercogs.beyonddimensions.Item.Custom.XpExchangeItem;
import com.wintercogs.beyonddimensions.Menu.BDBaseMenu;
import com.wintercogs.beyonddimensions.Tags.ModFluidTags;
import com.wintercogs.beyonddimensions.Unit.BDMath;
import com.wintercogs.beyonddimensions.Unit.XpUtil;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import org.lwjgl.glfw.GLFW;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

public class DisorderedStackTypedSlot extends AbstractStackTypedSlot
{

    public DisorderedStackTypedSlot(BDBaseMenu menu, IStackTypedHandler stackTypedHandler, int slotIndex, int xPosition, int yPosition)
    {
        super(menu, stackTypedHandler, slotIndex, xPosition, yPosition);
    }

    public DisorderedStackTypedSlot(BDBaseMenu menu, IStackTypedHandler stackTypedHandler, int slotIndex, int quickMoveSlotStartIndex, int quickMoveSlotEndIndex, int xPosition, int yPosition)
    {
        super(menu, stackTypedHandler, slotIndex, quickMoveSlotStartIndex, quickMoveSlotEndIndex, xPosition, yPosition);
    }

    @Override
    public boolean isOrdered()
    {
        return false;
    }

    @Override
    public void click(IStackType clickStack, int button, Player player)
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
                        IStackType remaining = storage.insert(new FluidStackType(new FluidStack(ModFluids.XP_FLUID.source().get(),1),actualInsertFluid),false);
                        if(!remaining.isEmpty())
                        {
                            int needReturnXp = BDMath.clampLongToInt(remaining.getStackAmount()/20); // 由于前面从int*20，这里除回去
                            actualRemovePlayerXp = actualRemovePlayerXp - needReturnXp;
                        }
                        player.giveExperiencePoints(-actualRemovePlayerXp); // 根据插入的流体给玩家减去经验值
                    }
                }
                // 再检查是否为能力交互
                else if(carriedItem.getCount()==1 && button== GLFW.GLFW_MOUSE_BUTTON_RIGHT && !ItemCapInteractionBlackList.isInBlackList(carriedItem.getItem()))
                {
                    if(carriedItem.getItem() instanceof BucketItem bucketItem)
                    {
                        LazyOptional<?> handler = carriedItem.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM);
                        if(handler.isPresent())
                        {
                            FluidHandlerWrapper stackHandlerWrapper = new FluidHandlerWrapper(handler.resolve().get());

                            if(stackHandlerWrapper.getSlots()>0)
                            {
                                FluidStackType stack = new FluidStackType(stackHandlerWrapper.getStackInSlot(0));
                                if(stack != null && !stack.isEmpty())
                                {
                                    int changedCount = BDMath.clampLongToInt(Math.min(stack.getStackAmount(),stack.getVanillaMaxStackSize()));
                                    // 进行模拟，桶必须完全清空才被允许操作
                                    int remaining = (int)storage.insert(stack.copyWithCount(changedCount),true).getStackAmount();
                                    if(remaining<=0)
                                    {
                                        // 执行实际逻辑
                                        storage.insert(stack.copyWithCount(changedCount),false).getStackAmount();
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
                            LazyOptional<?> handler = carriedItem.getCapability(cap);
                            if(handler.isPresent())
                            {
                                Function handlerGetter = StackHandlerWrapperHelper.stackWrappers.get(typeId);
                                IStackHandlerWrapper stackHandlerWrapper = (IStackHandlerWrapper) handlerGetter.apply(handler.resolve().get());

                                if(stackHandlerWrapper.getSlots()>0)
                                {
                                    // 一次操作只操作其第一个有效槽位，然后break
                                    for(int index=0;index<stackHandlerWrapper.getSlots();index++)
                                    {
                                        IStackType stack = StackCreater.Create(typeId,stackHandlerWrapper.getStackInSlot(index));
                                        if(stack !=null&& !stack.isEmpty())
                                        {
                                            int changedCount = BDMath.clampLongToInt(Math.min(stack.getStackAmount(),stack.getVanillaMaxStackSize()));
                                            int remaining = (int)storage.insert(stack.copyWithCount(changedCount),false).getStackAmount();
                                            int actualInsert = changedCount - remaining;
                                            if(actualInsert>0)
                                            {
                                                long actualExtracts = stackHandlerWrapper.extract(index,actualInsert,false);
                                                if(actualExtracts< actualInsert)
                                                {
                                                    // 对此进行一个回调
                                                    storage.extract(stack.copyWithCount(actualInsert-actualExtracts),false);
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
                    int actualInsert = (int) (changedCount - storage.insert(StackCreater.Create(ItemStackType.ID, carriedItem.copyWithCount(changedCount), changedCount), false).getStackAmount());
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
                if(clickStack instanceof ItemStackType clickItem)
                {
                    //槽位物品存在，携带物品为空，尝试取出槽位物品
                    // 确保一次取出最大不得超过原版数量
                    int woundChangeNum = BDMath.clampLongToInt(Math.min(clickItem.getStackAmount(), clickItem.getVanillaMaxStackSize()));
                    int actualChangeNum = button == GLFW.GLFW_MOUSE_BUTTON_LEFT ? woundChangeNum : (woundChangeNum + 1) / 2;
                    ItemStack takenItem = ((ItemStack) storage.extract(new ItemStackType(clickItem.copyStackWithCount(actualChangeNum)),false).getStack()).copy();
                    if(takenItem != null)
                    {
                        menu.setCarried(takenItem);
                    }
                }
            }
            else if (mayPlace(carriedItem)) // 槽位物品存在，携带物品存在
            {
                AtomicBoolean handled = new AtomicBoolean(false);

                // 先检查是否为经验棒交互
                if (carriedItem.getItem() instanceof XpExchangeItem && button != GLFW.GLFW_MOUSE_BUTTON_LEFT)
                {
                    IStackType actualStack = storage.getStackByStack(clickStack);

                    int conversionRate = XpExchangeItem.getConversionRate();
                    double currentLevel = XpUtil.levelAsDouble(player);
                    int wantConversionLevel = XpExchangeItem.getXpLevelPerAction(carriedItem);

                    if(actualStack instanceof FluidStackType fluidStackType && fluidStackType.hasTag(ModFluidTags.C_EXPERIENCE))
                    {
                        handled.set(true); // 走到这一步说明已经进行了交互
                        if(button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) // 鼠标右键--存入一级
                        {
                            long needRemovePlayerXp = XpUtil.xpBetweenLevels(Math.max(currentLevel-wantConversionLevel,0),currentLevel);
                            int actualRemovePlayerXp = BDMath.clampLongToInt(needRemovePlayerXp);
                            long actualInsertFluid = (long) actualRemovePlayerXp * conversionRate;

                            // 插入当前经验流体
                            IStackType remaining = storage.insert(fluidStackType.copyWithCount(actualInsertFluid),false);
                            if(!remaining.isEmpty())
                            {
                                int needReturnXp = BDMath.clampLongToInt(remaining.getStackAmount()/20); // 由于前面从int*20，这里除回去
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
                            IStackType extracted = storage.extract(fluidStackType.copyWithCount(actualRemoveFluid) ,false);
                            actualInsertPlayerXp = BDMath.clampLongToInt(extracted.getStackAmount()/20);
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
                            IStackType remaining = storage.insert(new FluidStackType(new FluidStack(ModFluids.XP_FLUID.source().get(),1),actualInsertFluid),false);
                            if(!remaining.isEmpty())
                            {
                                int needReturnXp = BDMath.clampLongToInt(remaining.getStackAmount()/20); // 由于前面从int*20，这里除回去
                                actualRemovePlayerXp = actualRemovePlayerXp - needReturnXp;
                            }
                            player.giveExperiencePoints(-actualRemovePlayerXp); // 根据插入的流体给玩家减去经验值
                        }
                    }
                }
                else if(!ItemCapInteractionBlackList.isInBlackList(carriedItem.getItem()))// 再检查是否为能力交互
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
                                if(clickStack instanceof FluidStackType fluidStackType)
                                {
                                    Item filledBucket = fluidStackType.getStack().getFluid().getBucket();

                                    if(filledBucket != null && filledBucket != Items.AIR
                                            && storage.getStackByStack(fluidStackType).getStackAmount()>=1000)
                                    {
                                        // 执行操作
                                        storage.extract(fluidStackType.copyWithCount(1000),false);
                                        menu.setCarried(new ItemStack(filledBucket));
                                        handled.set(true);
                                    }
                                }
                            }
                            else // 继续投放 insert模拟会自动解决类型不匹配等问题
                            {
                                LazyOptional<?> handler = carriedItem.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM);
                                if(handler.isPresent())
                                {
                                    FluidHandlerWrapper stackHandlerWrapper = new FluidHandlerWrapper(handler.resolve().get());

                                    if(stackHandlerWrapper.getSlots()>0)
                                    {
                                        FluidStackType stack = new FluidStackType(stackHandlerWrapper.getStackInSlot(0));
                                        if(stack != null && !stack.isEmpty())
                                        {
                                            int changedCount = BDMath.clampLongToInt(Math.min(stack.getStackAmount(),stack.getVanillaMaxStackSize()));
                                            // 进行模拟，桶必须完全清空才被允许操作
                                            int remaining = (int)storage.insert(stack.copyWithCount(changedCount),true).getStackAmount();
                                            if(remaining<=0)
                                            {
                                                // 执行实际逻辑
                                                storage.insert(stack.copyWithCount(changedCount),false).getStackAmount();
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
                                if(clickStack.getTypeId().equals(typeId))
                                {
                                    // 尝试获取对应能力
                                    LazyOptional<?> handler = carriedItem.getCapability(cap);
                                    if(handler.isPresent())
                                    {
                                        Function handlerGetter = StackHandlerWrapperHelper.stackWrappers.get(typeId);
                                        IStackHandlerWrapper stackHandlerWrapper = (IStackHandlerWrapper) handlerGetter.apply(handler.resolve().get());
                                        if(stackHandlerWrapper.getSlots()>0)
                                        {
                                            IStackType actualClickStack = storage.getStackByStack(clickStack);// 防止客户端假消息
                                            if(actualClickStack != null)
                                            {
                                                int changedCount = BDMath.clampLongToInt(Math.min(actualClickStack.getStackAmount(),actualClickStack.getVanillaMaxStackSize()));
                                                int remaining = (int)stackHandlerWrapper.insert(actualClickStack.copyStackWithCount(changedCount),false);
                                                int actualInsert = changedCount - remaining;
                                                if(actualInsert>0)
                                                {
                                                    storage.extract(actualClickStack.copyWithCount(actualInsert),false);
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
                                    LazyOptional<?> handler = carriedItem.getCapability(cap);
                                    if(handler.isPresent())
                                    {
                                        Function handlerGetter = StackHandlerWrapperHelper.stackWrappers.get(typeId);
                                        IStackHandlerWrapper stackHandlerWrapper = (IStackHandlerWrapper) handlerGetter.apply(handler.resolve().get());

                                        if(stackHandlerWrapper.getSlots()>0)
                                        {
                                            // 一次操作只操作其第一个有效槽位，然后break
                                            for(int index=0;index<stackHandlerWrapper.getSlots();index++)
                                            {
                                                IStackType stack = StackCreater.Create(typeId,stackHandlerWrapper.getStackInSlot(index));
                                                if(stack !=null&& !stack.isEmpty())
                                                {
                                                    int changedCount = BDMath.clampLongToInt(Math.min(stack.getStackAmount(),stack.getVanillaMaxStackSize()));
                                                    int remaining = (int)storage.insert(stack.copyWithCount(changedCount),false).getStackAmount();
                                                    int actualInsert = changedCount - remaining;

                                                    if(actualInsert>0)
                                                    {
                                                        long actualExtracts = stackHandlerWrapper.extract(index,actualInsert,false);
                                                        if(actualExtracts< actualInsert)
                                                        {
                                                            // 对此进行一个回调
                                                            storage.extract(stack.copyWithCount(actualInsert-actualExtracts),false);
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
                    storage.insert(StackCreater.Create(ItemStackType.ID,carriedItem.copyWithCount(changedCount),changedCount),false);
                    int newCount = carriedItem.getCount() - changedCount;
                    if(newCount <=0)
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
            else if (clickStack.isSameTypeSameComponents(new ItemStackType(carriedItem.copy())))
            {   // 槽位物品存在，携带物品存在，物品不可放置，为完全相同的物品
                // 此情况在点击维度存储槽时永远不可能发生，如果发生，无需处理
                // 原版逻辑为取出物品到最大上限
                // 保留此情况以便后续使用
            }

        }
    }

    @Override
    public void quickMove(IStackType clickStack, int button, Player player)
    {
        if (!clickStack.isEmpty())
        {
            // 防止数据包伪造，然后赋予trueStack需要提取的数量
            IStackType trueStack = storage.getStackByStack(clickStack).copyWithCount(clickStack.getStackAmount());

            // 遍历目标槽位
            for(int targetSlotIndex=quickMoveSlotStartIndex;targetSlotIndex<quickMoveSlotEndIndex && !trueStack.isEmpty();targetSlotIndex++)
            {
                Slot slot = menu.slots.get(targetSlotIndex);
                if(slot instanceof AbstractStackTypedSlot aSlot)
                {
                    // aSlot处理任何情况

                    //首先尝试从存储提取指定堆叠
                    IStackType extract = storage.extract(trueStack,false);
                    IStackType remaining = aSlot.safeInsert(extract); // 然后插入到其他堆叠并获取余量
                    if(!remaining.isEmpty())
                        storage.insert(remaining,false); // 最后将余量返回
                    trueStack = remaining.copy();

                }
                else
                {
                    // 普通Slot将只处理物品转移
                    if(trueStack instanceof ItemStackType trueItemTypedStack)
                    {
                        ItemStack extract = (ItemStack) storage.extract(trueItemTypedStack,false).getStack();
                        ItemStack remaining = slot.safeInsert(extract);
                        if(!remaining.isEmpty())
                            storage.insert(new ItemStackType(remaining),false);
                        trueStack = new ItemStackType(remaining.copy());
                    }
                }

            }
            setChanged();
        }
    }

    @Override
    public IStackType safeInsert(IStackType stack)
    {
        if(stack != null)
        {
            return storage.insert(stack,false);
        }
        return new ItemStackType();

    }

    @Override
    public IStackType safeExtract(IStackType stack)
    {
        if(stack != null)
        {
            return storage.extract(stack,false);
        }
        return new ItemStackType();
    }

    // 无序槽位由槽位组负责处理同步
    @Override
    public void updateChange()
    {

    }

    @Override
    public void loadChange(int where ,IStackType newStack, long newAmount)
    {

    }
}
