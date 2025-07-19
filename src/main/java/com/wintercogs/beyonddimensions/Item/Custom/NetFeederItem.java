package com.wintercogs.beyonddimensions.Item.Custom;

import com.wintercogs.beyonddimensions.Api.DataBase.Stack.IStackType;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.ItemStackType;
import com.wintercogs.beyonddimensions.Api.DataBase.Storage.UnifiedStorage;
import com.wintercogs.beyonddimensions.DataComponents.ModDataComponents;
import com.wintercogs.beyonddimensions.Machine.FeederMode;
import com.wintercogs.beyonddimensions.Machine.FilterMode;
import com.wintercogs.beyonddimensions.Menu.NetFeederMenu;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NetFeederItem extends BaseMachineItem
{
    public static final int capacity = 36;

    public NetFeederItem(Properties properties)
    {
        super(properties);
    }
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand)
    {
        super.use(level, player, usedHand);
        ItemStack itemstack = player.getItemInHand(usedHand);
        if(usedHand != InteractionHand.MAIN_HAND || player.isShiftKeyDown())
        {
            return InteractionResultHolder.fail(itemstack);
        }

        if(!level.isClientSide())
        {
            player.openMenu(new SimpleMenuProvider((containerId, inv, ServerPlayer) ->
                            new NetFeederMenu(containerId,inv,itemstack),
                            Component.translatable("menu.title.beyonddimensions.feeder_menu")),
                    buf -> buf.writeEnum(usedHand));
        }
        return InteractionResultHolder.sidedSuccess(itemstack,level.isClientSide());
    }

    @Override
    public void checkComponents(ItemStack stack)
    {
        super.checkComponents(stack);
        if(!stack.has(ModDataComponents.ISTACK_SLOTS))
            stack.set(ModDataComponents.ISTACK_SLOTS,new ArrayList<>(Collections.nCopies(capacity,new ItemStackType())));
        if(!stack.has(ModDataComponents.FEEDER_MODE))
            stack.set(ModDataComponents.FEEDER_MODE, FeederMode.NORMAL);

    }

    @Override
    public boolean shouldWork(ItemStack stack, Level level, Entity holder, int slotId, boolean isSelected)
    {
        return super.shouldWork(stack, level, holder, slotId, isSelected)
                && NetedItem.getNet(stack,level.getServer()) != null;
    }

    @Override
    public void workContent(ItemStack stack, Level level, Entity holder, int slotId, boolean isSelected)
    {
        super.workContent(stack, level, holder, slotId, isSelected);

        if(holder instanceof Player player) // 只喂食玩家（实际上是其他实体没有FoodData 2333）
        {
            FeederMode feederMode = stack.getOrDefault(ModDataComponents.FEEDER_MODE,FeederMode.NORMAL);
            List<IStackType> filterSlots = stack.getOrDefault(ModDataComponents.ISTACK_SLOTS,new ArrayList<>());

            FoodData playerFoodState = player.getFoodData();

            // feederModeMatch会进行一次饥饿值判定，决定要不要实际执行
            if(feederModeMatch(playerFoodState,feederMode))
            {
                UnifiedStorage storage = NetedItem.getNet(stack,level.getServer()).getUnifiedStorage();

                // 尝试取出一个Food
                IStackType foodCache = null;
                for(IStackType filter: filterSlots)
                {
                    for(IStackType storedStack: storage.getStorage())
                    {
                        // isSame会在最后变为引用比较，所以无需担心，这个比较即使对于大存储来说也非常迅速
                        if(storedStack instanceof ItemStackType itemStackType
                            && itemStackType.isSame(filter)
                            && itemStackType.getStack().has(DataComponents.FOOD))
                        {
                            foodCache = storedStack.copyWithCount(1);
                            break;
                        }
                    }
                }

                if(foodCache != null)
                {
                    ItemStackType foodToFeed = (ItemStackType)storage.extract(foodCache,false);
                    ItemStack remaining = player.eat(level,foodToFeed.copyStack());
                    if(!remaining.isEmpty())
                    {
                        // 剩余堆叠插送回去
                        ItemStackType remainingAgain = (ItemStackType)storage.insert(new ItemStackType(remaining),false);
                        if(!remainingAgain.isEmpty()) //防止某些带NBT物品改变NBT导致存储的种类不够用
                        {
                            player.drop(remainingAgain.copyStack(),false);
                        }
                    }
                }
                
            }
        }

    }

    private boolean feederModeMatch(FoodData playerFoodState, FeederMode feederMode)
    {
        return switch (feederMode)
        {
            case HUNGER_TO_EAT -> playerFoodState.getFoodLevel() <= 2;
            case NORMAL -> playerFoodState.getFoodLevel() <= 10;
            case SATURATION_KEEP -> playerFoodState.getSaturationLevel() <= 0;
            case CRAZY -> playerFoodState.getFoodLevel()<20;
            default -> false;
        };
    }

    private boolean matchesFilter(List<IStackType> filterSlots,IStackType otherStack)
    {
        switch (FilterMode.WHITE) //喂食器始终白名单
        {

            case BLACK -> {
                for(IStackType stack : filterSlots)
                {
                    if(stack.isSame(otherStack))
                        return false;
                }
                return true;
            }
            case WHITE -> {
                for(IStackType stack : filterSlots)
                {
                    if(stack.isSame(otherStack))
                        return true;
                }
                return false;
            }
            case IGNORE -> {
                return true;
            }

        }
        return false;
    }

    @Override
    public int getTicksPerWork()
    {
        return 10; //每10tick检测一次
    }
}
