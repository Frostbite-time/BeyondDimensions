package com.wintercogs.beyonddimensions.Item.Custom;

import com.wintercogs.beyonddimensions.DataBase.DimensionsNet;
import com.wintercogs.beyonddimensions.DataComponents.ModDataComponents;
import com.wintercogs.beyonddimensions.Menu.DimensionsCraftMenuTerminal;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public class NetTerminalItem extends NetedItem implements MenuProvider
{
    public NetTerminalItem(Properties properties)
    {
        super(properties.component(ModDataComponents.CRAFT_SLOTS, NonNullList.withSize(9,ItemStack.EMPTY)));
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

            if(itemstack.get(ModDataComponents.NET_ID_DATA)>=0)
            {
                DimensionsNet net = DimensionsNet.getNetFromId(itemstack.get(ModDataComponents.NET_ID_DATA),level);
                if (net != null)
                {
                    player.openMenu(this);
                }
            }
            else
            {
                player.sendSystemMessage(Component.translatable("msg.beyonddimensions.item_need_bound"));
            }

        }
        return InteractionResultHolder.sidedSuccess(itemstack,level.isClientSide());
    }

    @Override
    public Component getDisplayName()
    {
        return Component.translatable("menu.title.beyonddimensions.dimensionnetmenu");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId,Inventory inventory, Player player)
    {
        ItemStack itemstack = player.getItemInHand(player.getUsedItemHand());
        DimensionsNet net = DimensionsNet.getNetFromId(itemstack.get(ModDataComponents.NET_ID_DATA),player.level());
        return new DimensionsCraftMenuTerminal(containerId,inventory,net, (NonNullList<ItemStack>) itemstack.get(ModDataComponents.CRAFT_SLOTS), itemstack, null);
    }
}
