package com.wintercogs.beyonddimensions.Item.Custom;

import com.wintercogs.beyonddimensions.Api.DataBase.Stack.ItemStackKey;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.KeyAmount;
import com.wintercogs.beyonddimensions.Api.DataBase.StackHandlerWrapper.ItemHandlerWrapper;
import com.wintercogs.beyonddimensions.Api.DataBase.Storage.UnifiedStorage;
import com.wintercogs.beyonddimensions.Machine.FuzzyMode;
import com.wintercogs.beyonddimensions.Machine.ReceiveMode;
import com.wintercogs.beyonddimensions.Menu.NetRestockerMenu;
import com.wintercogs.beyonddimensions.Util.BDMath;
import com.wintercogs.beyonddimensions.common.init.BDDataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NetRestockerItem extends BaseMachineItem
{
    public static final int capacity = 41;

    public NetRestockerItem(Properties properties)
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
            player.openMenu(new SimpleMenuProvider((containerId, inv, serverPlayer) ->
                            new NetRestockerMenu(containerId, inv, itemstack),
                            Component.translatable("menu.title.beyonddimensions.restocker_menu")),
                    buf -> buf.writeEnum(usedHand));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public int getStepTick()
    {
        return 5;
    }

    @Override
    public void checkComponents(ItemStack stack)
    {
        super.checkComponents(stack);
        if (!stack.has(BDDataComponents.ISTACK_SLOTS))
            stack.set(BDDataComponents.ISTACK_SLOTS, new ArrayList<>(Collections.nCopies(capacity, new KeyAmount(ItemStackKey.EMPTY, 0))));
        if (!stack.has(BDDataComponents.FUZZY_MODE))
            stack.set(BDDataComponents.FUZZY_MODE, FuzzyMode.DISABLE);
        if (!stack.has(BDDataComponents.RECEIVE_MODE))
            stack.set(BDDataComponents.RECEIVE_MODE, ReceiveMode.STOP);
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

        UnifiedStorage storage = NetedItem.getNet(stack).getUnifiedStorage();
        List<KeyAmount> templates = stack.getOrDefault(BDDataComponents.ISTACK_SLOTS, new ArrayList<>());

        FuzzyMode fuzzyMode = stack.getOrDefault(BDDataComponents.FUZZY_MODE, FuzzyMode.DISABLE);
        ReceiveMode receiveMode = stack.getOrDefault(BDDataComponents.RECEIVE_MODE, ReceiveMode.STOP);

        if (entity instanceof Player player)
        {
            Inventory inventory = player.getInventory();
            boolean inventoryChanged = false;

            for (int templateSlot = 0; templateSlot < capacity && templateSlot < templates.size(); templateSlot++)
            {
                KeyAmount template = templates.get(templateSlot);

                ItemStack currentStack = getPlayerSlotStack(player, templateSlot);

                if (receiveMode == ReceiveMode.OPEN
                        && !currentStack.isEmpty()
                        && canRecycle(currentStack)
                        && !slotMatchesTemplate(currentStack, template, fuzzyMode))
                {
                    ItemStackKey currentKey = new ItemStackKey(currentStack);
                    KeyAmount remainder = storage.insert(currentKey, currentStack.getCount(), false);
                    int accepted = currentStack.getCount() - BDMath.clampLongToInt(remainder.amount());
                    if (accepted > 0)
                    {
                        currentStack.shrink(accepted);
                        setPlayerSlotStack(player, templateSlot, currentStack.isEmpty() ? ItemStack.EMPTY : currentStack);
                        inventoryChanged = true;
                        currentStack = getPlayerSlotStack(player, templateSlot);
                    }
                }

                if (!(template.key() instanceof ItemStackKey targetKey) || template.isEmpty())
                    continue;

                if (currentStack.isEmpty() && !canPlaceInPlayerTemplateSlot(player, templateSlot, targetKey.getReadOnlyStack()))
                    continue;

                int targetCount = BDMath.clampLongToInt(targetKey.getVanillaMaxStackSize());
                if (targetCount <= 0)
                    continue;

                int missing;
                if (currentStack.isEmpty())
                {
                    missing = targetCount;
                }
                else if (ItemStack.isSameItemSameComponents(currentStack, targetKey.getReadOnlyStack()))
                {
                    missing = targetCount - currentStack.getCount();
                }
                else
                {
                    continue;
                }

                if (missing <= 0)
                    continue;

                KeyAmount extracted = storage.extract(targetKey, missing, false, fuzzyMode == FuzzyMode.ENABLE);
                if (extracted.isEmpty())
                    continue;

                if (!(extracted.key() instanceof ItemStackKey extractedItemKey))
                {
                    storage.insert(extracted.key(), extracted.amount(), false);
                    continue;
                }

                int refillCount = BDMath.clampLongToInt(extracted.amount());
                if (refillCount <= 0)
                    continue;

                ItemStack refill = extractedItemKey.copyStackWithCount(refillCount);

                if (currentStack.isEmpty())
                {
                    if (setPlayerSlotStack(player, templateSlot, refill))
                    {
                        inventoryChanged = true;
                    }
                    else
                    {
                        storage.insert(extracted.key(), extracted.amount(), false);
                    }
                }
                else
                {
                    if (!ItemStack.isSameItemSameComponents(currentStack, refill))
                    {
                        storage.insert(extracted.key(), extracted.amount(), false);
                        continue;
                    }

                    currentStack.grow(refillCount);
                    if (setPlayerSlotStack(player, templateSlot, currentStack))
                        inventoryChanged = true;
                    else
                        storage.insert(extracted.key(), extracted.amount(), false);
                }
            }

            if (inventoryChanged)
            {
                inventory.setChanged();
            }
            return;
        }

        if (!(entity instanceof LivingEntity living))
            return;

        ResourceHandler<@NotNull ItemResource> handler = living.getCapability(Capabilities.Item.ENTITY);
        if (handler == null)
            handler = living.getCapability(Capabilities.Item.ENTITY_AUTOMATION, null);
        if (handler == null)
            return;

        ItemHandlerWrapper wrappedHandler = new ItemHandlerWrapper(handler);

        for (int templateSlot = 0; templateSlot < Math.min(capacity, Math.min(templates.size(), wrappedHandler.getSlots())); templateSlot++)
        {
            KeyAmount template = templates.get(templateSlot);
            ItemStack currentStack = wrappedHandler.getStackInSlot(templateSlot);

            if (receiveMode == ReceiveMode.OPEN
                    && !currentStack.isEmpty()
                    && canRecycle(currentStack)
                    && !slotMatchesTemplate(currentStack, template, fuzzyMode))
            {
                long simulatedExtractAmount = wrappedHandler.extract(templateSlot, currentStack.getCount(), true);
                if (simulatedExtractAmount > 0)
                {
                    int simulatedCount = BDMath.clampLongToInt(simulatedExtractAmount);
                    ItemStack simulatedExtract = currentStack.copyWithCount(simulatedCount);
                    ItemStackKey currentKey = new ItemStackKey(simulatedExtract);
                    KeyAmount remainder = storage.insert(currentKey, simulatedExtract.getCount(), true);
                    int accepted = simulatedExtract.getCount() - BDMath.clampLongToInt(remainder.amount());
                    if (accepted > 0)
                    {
                        long extracted = wrappedHandler.extract(templateSlot, accepted, false);
                        if (extracted > 0)
                        {
                            storage.insert(currentKey, extracted, false);
                            currentStack = wrappedHandler.getStackInSlot(templateSlot);
                        }
                    }
                }
            }

            if (!(template.key() instanceof ItemStackKey targetKey) || template.isEmpty())
                continue;

            int targetCount = BDMath.clampLongToInt(targetKey.getVanillaMaxStackSize());
            if (targetCount <= 0)
                continue;

            int missing;
            if (currentStack.isEmpty())
            {
                missing = targetCount;
            }
            else if (ItemStack.isSameItemSameComponents(currentStack, targetKey.getReadOnlyStack()))
            {
                missing = targetCount - currentStack.getCount();
            }
            else
            {
                continue;
            }

            if (missing <= 0)
                continue;

            KeyAmount extracted = storage.extract(targetKey, missing, false, fuzzyMode == FuzzyMode.ENABLE);
            if (extracted.isEmpty())
                continue;

            if (!(extracted.key() instanceof ItemStackKey extractedItemKey))
            {
                storage.insert(extracted.key(), extracted.amount(), false);
                continue;
            }

            int refillCount = BDMath.clampLongToInt(extracted.amount());
            if (refillCount <= 0)
                continue;

            ItemStack refill = extractedItemKey.copyStackWithCount(refillCount);

            if (!currentStack.isEmpty() && !ItemStack.isSameItemSameComponents(currentStack, refill))
            {
                storage.insert(extracted.key(), extracted.amount(), false);
                continue;
            }

            long leftover = wrappedHandler.insert(templateSlot, refill, false);
            if (leftover > 0L)
            {
                storage.insert(new ItemStackKey(refill), leftover, false);
            }
        }
    }

    private boolean slotMatchesTemplate(ItemStack stackInSlot, KeyAmount template, FuzzyMode fuzzyMode)
    {
        if (stackInSlot.isEmpty() || template.isEmpty() || !(template.key() instanceof ItemStackKey templateKey))
            return false;

        if (fuzzyMode == FuzzyMode.ENABLE)
            return templateKey.isSame(new ItemStackKey(stackInSlot));

        return ItemStack.isSameItemSameComponents(stackInSlot, templateKey.getReadOnlyStack());
    }

    private boolean canRecycle(ItemStack stack)
    {
        return !(stack.getItem() instanceof NetRestockerItem);
    }

    private ItemStack getPlayerSlotStack(Player player, int templateSlot)
    {
        Inventory inventory = player.getInventory();
        if (templateSlot < 27)
            return inventory.getItem(templateSlot + 9);
        if (templateSlot < 36)
            return inventory.getItem(templateSlot - 27);

        return switch (templateSlot)
        {
            case 36 -> player.getItemBySlot(EquipmentSlot.HEAD);
            case 37 -> player.getItemBySlot(EquipmentSlot.CHEST);
            case 38 -> player.getItemBySlot(EquipmentSlot.LEGS);
            case 39 -> player.getItemBySlot(EquipmentSlot.FEET);
            case 40 -> player.getItemBySlot(EquipmentSlot.OFFHAND);
            default -> ItemStack.EMPTY;
        };
    }

    private boolean setPlayerSlotStack(Player player, int templateSlot, ItemStack stack)
    {
        Inventory inventory = player.getInventory();
        if (templateSlot < 27)
        {
            inventory.setItem(templateSlot + 9, stack);
            return true;
        }
        if (templateSlot < 36)
        {
            inventory.setItem(templateSlot - 27, stack);
            return true;
        }

        EquipmentSlot equipmentSlot = switch (templateSlot)
        {
            case 36 -> EquipmentSlot.HEAD;
            case 37 -> EquipmentSlot.CHEST;
            case 38 -> EquipmentSlot.LEGS;
            case 39 -> EquipmentSlot.FEET;
            case 40 -> EquipmentSlot.OFFHAND;
            default -> null;
        };

        if (equipmentSlot == null)
            return false;

        if (!canPlaceInPlayerTemplateSlot(player, templateSlot, stack))
            return false;

        player.setItemSlot(equipmentSlot, stack);
        return true;
    }

    private boolean canPlaceInPlayerTemplateSlot(Player player, int templateSlot, ItemStack stack)
    {
        if (templateSlot < 36)
            return true;

        if (stack.isEmpty())
            return true;

        return switch (templateSlot)
        {
            case 36 -> stack.canEquip(EquipmentSlot.HEAD, player);
            case 37 -> stack.canEquip(EquipmentSlot.CHEST, player);
            case 38 -> stack.canEquip(EquipmentSlot.LEGS, player);
            case 39 -> stack.canEquip(EquipmentSlot.FEET, player);
            case 40 -> true;
            default -> false;
        };
    }

    @Override
    public int getTicksPerWork()
    {
        return 10;
    }
}
