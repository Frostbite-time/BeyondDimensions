package com.wintercogs.beyonddimensions.common.item;

import com.wintercogs.beyonddimensions.api.dimensionnet.DimensionsNet;
import com.wintercogs.beyonddimensions.common.component.ItemStackContents;
import com.wintercogs.beyonddimensions.common.init.BDDataComponents;
import com.wintercogs.beyonddimensions.common.menu.DimensionsCraftMenuTerminal;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.WeakHashMap;

public class NetTerminalItem extends NetedItem implements MenuProvider
{
    public NetTerminalItem(Properties properties)
    {
        super(properties.stacksTo(1));
    }

    public static final Map<Player, MenuTriggerContext> contextMap = new WeakHashMap<>();


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
            if (itemstack.get(BDDataComponents.CRAFT_SLOTS) == null)
                itemstack.set(BDDataComponents.CRAFT_SLOTS, new ItemStackContents(NonNullList.withSize(9, ItemStack.EMPTY)));

            if (itemstack.getOrDefault(BDDataComponents.NET_ID_DATA, -1) >= 0)
            {
                DimensionsNet net = DimensionsNet.getNetFromId(itemstack.getOrDefault(BDDataComponents.NET_ID_DATA, -1));
                if (net != null)
                {
                    contextMap.put(player, new MenuTriggerContext(usedHand, itemstack));
                    player.openMenu(this);
                }
            }
            else
            {
                player.sendSystemMessage(Component.translatable("msg.beyonddimensions.item_need_bound"));
            }

        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public Component getDisplayName()
    {
        return Component.translatable("menu.title.beyonddimensions.dimensionnetmenu");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player)
    {

        // 从上下文映射中获取触发时的物品
        MenuTriggerContext ctx = contextMap.remove(player);
        if (ctx == null)
        {
            // 没有上下文记录，则退回到原始方法
            ctx = new MenuTriggerContext(player.getUsedItemHand(), player.getItemInHand(player.getUsedItemHand()));
        }
        // 验证物品是否仍是有效的NetTerminalItem
        if (ctx.stack.getItem() != this || !ctx.stack.has(BDDataComponents.NET_ID_DATA))
        {
            return null;
        }
        // 使用上下文中的物品栈
        DimensionsNet net = DimensionsNet.getNetFromId(ctx.stack.getOrDefault(BDDataComponents.NET_ID_DATA, -1));
        if (net == null) return null;
        return new DimensionsCraftMenuTerminal(
                containerId,
                inventory,
                net.getUnifiedStorage(),
                ctx.stack.get(BDDataComponents.CRAFT_SLOTS).contents(),
                ctx.stack,
                null
        );
    }

    @Override
    public boolean shouldTriggerClientSideContainerClosingOnOpen()
    {
        return MenuProvider.super.shouldTriggerClientSideContainerClosingOnOpen();
    }

    // 创建一个内部类来存储触发时的上下文
    public static class MenuTriggerContext
    {
        public final InteractionHand hand;
        public final ItemStack stack;

        public MenuTriggerContext(InteractionHand hand, ItemStack stack)
        {
            this.hand = hand;
            this.stack = stack;
        }
    }
}
