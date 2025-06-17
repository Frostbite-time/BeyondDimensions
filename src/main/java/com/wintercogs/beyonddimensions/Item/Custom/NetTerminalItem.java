package com.wintercogs.beyonddimensions.Item.Custom;

import com.wintercogs.beyonddimensions.Api.DataBase.DimensionsNet;
import com.wintercogs.beyonddimensions.Menu.DimensionsCraftMenuTerminal;
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
                DimensionsNet net = DimensionsNet.getNetFromId(NetedItem.getNetId(itemstack),level.getServer());
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

        // 从上下文映射中获取触发时的物品
        MenuTriggerContext ctx = contextMap.remove(player);
        if (ctx == null) {
            // 没有上下文记录，则退回到原始方法
            ctx = new MenuTriggerContext(player.getUsedItemHand(), player.getItemInHand(player.getUsedItemHand()));
        }
        // 验证物品是否仍是有效的NetTerminalItem
        if (ctx.stack.getItem() != this) {
            return null;
        }
        // 使用上下文中的物品栈
        DimensionsNet net = DimensionsNet.getNetFromId(NetedItem.getNetId(ctx.stack), player.getServer());


        // 从NBT获取合成槽位
        CompoundTag tag = ctx.stack.getOrCreateTag();
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
        return new DimensionsCraftMenuTerminal(containerId, inventory, net, craftSlots, ctx.stack, null);
    }


    // 创建一个内部类来存储触发时的上下文
    public static class MenuTriggerContext {
        public final InteractionHand hand;
        public final ItemStack stack;
        public MenuTriggerContext(InteractionHand hand, ItemStack stack) {
            this.hand = hand;
            this.stack = stack;
        }
    }
}
