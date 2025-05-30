package com.wintercogs.beyonddimensions.Item.Custom;

import com.wintercogs.beyonddimensions.DataBase.DimensionsNet;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class NetTerminalItem extends NetedItem implements MenuProvider
{

    public NetTerminalItem(Properties properties)
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

            if(NetedItem.getNetId(itemstack)>=0)
            {
                DimensionsNet net = DimensionsNet.getNetFromId(NetedItem.getNetId(itemstack),level);
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
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player)
    {
        ItemStack itemstack = player.getItemInHand(player.getUsedItemHand());
        int netId = NetedItem.getNetId(itemstack);
        DimensionsNet net = DimensionsNet.getNetFromId(netId, player.level());
        // 从NBT获取合成槽位
        CompoundTag tag = itemstack.getOrCreateTag();
        if (!tag.contains("craft_slots", Tag.TAG_LIST)) {
            // 初始化默认的9个空槽位
            ListTag slots = new ListTag();
            for (int i = 0; i < 9; i++) {
                slots.add(ItemStack.EMPTY.save(new CompoundTag()));
            }
            tag.put("craft_slots", slots);
        }
        ListTag slotsTag = tag.getList("craft_slots", Tag.TAG_COMPOUND);
        NonNullList<ItemStack> craftSlots = NonNullList.withSize(9, ItemStack.EMPTY);
        for (int i = 0; i < slotsTag.size() && i < 9; i++) {
            craftSlots.set(i, ItemStack.of(slotsTag.getCompound(i)));
        }
        return new DimensionsCraftMenuTerminal(containerId, inventory, net, craftSlots, itemstack);
    }
}
