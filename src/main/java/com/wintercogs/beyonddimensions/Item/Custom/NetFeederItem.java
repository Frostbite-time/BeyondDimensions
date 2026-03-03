package com.wintercogs.beyonddimensions.Item.Custom;

import com.wintercogs.beyonddimensions.Api.DataBase.Stack.IStackKey;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.ItemStackKey;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.KeyAmount;
import com.wintercogs.beyonddimensions.Api.DataBase.Storage.UnifiedStorage;
import com.wintercogs.beyonddimensions.Machine.FeederMode;
import com.wintercogs.beyonddimensions.Machine.FilterMode;
import com.wintercogs.beyonddimensions.Menu.NetFeederMenu;
import com.wintercogs.beyonddimensions.common.init.BDDataComponents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.EventHooks;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NetFeederItem extends BaseMachineItem
{
    public static final int capacity = 36;

    public NetFeederItem(Properties properties)
    {
        super(properties.stacksTo(1));
    }

    @Override
    public @NotNull InteractionResult use(Level level, Player player, InteractionHand usedHand)
    {
        super.use(level, player, usedHand);
        ItemStack itemstack = player.getItemInHand(usedHand);
        if (usedHand != InteractionHand.MAIN_HAND || player.isShiftKeyDown())
        {
            return InteractionResult.FAIL;
        }

        if (!level.isClientSide())
        {
            player.openMenu(new SimpleMenuProvider((containerId, inv, ServerPlayer) ->
                            new NetFeederMenu(containerId, inv, itemstack),
                            Component.translatable("menu.title.beyonddimensions.feeder_menu")),
                    buf -> buf.writeEnum(usedHand));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void checkComponents(ItemStack stack)
    {
        super.checkComponents(stack);
        if (!stack.has(BDDataComponents.ISTACK_SLOTS))
            stack.set(BDDataComponents.ISTACK_SLOTS, new ArrayList<>(Collections.nCopies(capacity, new KeyAmount(ItemStackKey.EMPTY, 0))));
        if (!stack.has(BDDataComponents.FEEDER_MODE))
            stack.set(BDDataComponents.FEEDER_MODE, FeederMode.NORMAL);
    }

    @Override
    public boolean shouldWork(@NotNull ItemStack stack, @NotNull ServerLevel level, @NotNull Entity entity, @Nullable EquipmentSlot slot)
    {
        return super.shouldWork(stack, level, entity, slot)
                && NetedItem.getNet(stack) != null;
    }

    @Override
    public void workContent(@NotNull ItemStack stack, @NotNull ServerLevel level, @NotNull Entity entity, @Nullable EquipmentSlot slot)
    {
        super.workContent(stack, level, entity, slot);

        if (entity instanceof Player player) // 只喂食玩家（实际上是其他实体没有FoodData 2333）
        {
            FeederMode feederMode = stack.getOrDefault(BDDataComponents.FEEDER_MODE, FeederMode.NORMAL);
            List<KeyAmount> filterSlots = stack.getOrDefault(BDDataComponents.ISTACK_SLOTS, new ArrayList<>());

            FoodData playerFoodState = player.getFoodData();

            // feederModeMatch会进行一次饥饿值判定，决定要不要实际执行
            if (feederModeMatch(playerFoodState, feederMode))
            {
                UnifiedStorage storage = NetedItem.getNet(stack).getUnifiedStorage();

                // 尝试取出一个Food
                KeyAmount foodCache = null;
                for (KeyAmount filter : filterSlots)
                {
                    for (KeyAmount storedStack : storage.getStorage())
                    {
                        // isSame会在最后变为引用比较，所以无需担心，这个比较即使对于大存储来说也非常迅速
                        if (storedStack.key() instanceof ItemStackKey itemStackKey
                                && itemStackKey.isSame(filter.key())
                                && itemStackKey.getReadOnlyStack().get(DataComponents.FOOD) != null)
                        {
                            foodCache = new KeyAmount(storedStack.key(), 1);
                            break;
                        }
                    }
                }

                if (foodCache != null)
                {
                    KeyAmount foodToFeed = storage.extract(foodCache.key(), foodCache.amount(), false, false);
                    if (!foodToFeed.isEmpty() && foodToFeed.key() instanceof ItemStackKey foodKey)
                    {
                        ItemStack foodStack = foodKey.copyStackWithCount(foodCache.amount());
                        Item foodItem = foodStack.getItem();
                        FoodProperties foodProperties = foodStack.get(DataComponents.FOOD);
                        // 实际执行效果前对饱食度和饱和度进行二次判断
                        if (foodProperties != null)
                        {
                            if ((feederMode == FeederMode.SATURATION_KEEP && foodProperties.saturation() > 0)
                                    || (feederMode != FeederMode.SATURATION_KEEP && foodProperties.nutrition() > 0))
                            {
                                ItemStack remaining = EventHooks.onItemUseFinish(player, foodStack.copy(), 0, foodItem.finishUsingItem(foodStack, level, player));
                                if (!remaining.isEmpty())
                                {
                                    // 剩余堆叠插送回去
                                    KeyAmount remainingAgain = storage.insert(new ItemStackKey(remaining), remaining.getCount(), false);
                                    if (!remainingAgain.isEmpty()) //防止某些带NBT物品改变NBT导致存储的种类不够用
                                    {
                                        player.drop((ItemStack) remainingAgain.toStack(), false);
                                    }
                                }
                                return;
                            }
                        }
                        storage.insert(foodToFeed.key(), foodToFeed.amount(), false); // 如果没能步入食用，则在此处将堆叠插回
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
            case CRAZY -> playerFoodState.getFoodLevel() < 20;
            default -> false;
        };
    }

    private boolean matchesFilter(List<KeyAmount> filterSlots, IStackKey<?> otherStack)
    {
        switch (FilterMode.WHITE) //喂食器始终白名单
        {

            case BLACK ->
            {
                for (KeyAmount stack : filterSlots)
                {
                    if (stack.key().isSame(otherStack))
                        return false;
                }
                return true;
            }
            case WHITE ->
            {
                for (KeyAmount stack : filterSlots)
                {
                    if (stack.key().isSame(otherStack))
                        return true;
                }
                return false;
            }
            case IGNORE ->
            {
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
