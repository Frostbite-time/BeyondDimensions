package com.wintercogs.beyonddimensions.Integration.Curios;

import com.wintercogs.beyonddimensions.Item.Custom.NetFeederItem;
import com.wintercogs.beyonddimensions.Item.Custom.NetMagnetItem;
import com.wintercogs.beyonddimensions.Item.Custom.NetRestockerItem;
import com.wintercogs.beyonddimensions.Item.Custom.NetTerminalItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.CuriosCapability;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurio;

public class BD_CuriosPlugin
{
    // 动态附加物品为饰品
    public static void registerCapabilities(final AttachCapabilitiesEvent<ItemStack> evt)
    {
        ItemStack stack = evt.getObject();
        Item item = stack.getItem();
        if (item instanceof NetTerminalItem) //终端
        {
            evt.addCapability(CuriosCapability.ID_ITEM, CuriosApi.createCurioProvider(new ICurio()
            {

                @Override
                public ItemStack getStack()
                {
                    return stack;
                }

                @Override
                public void curioTick(SlotContext slotContext)
                {
                    // 在此处添加tick逻辑
                }
            }));
        }
        else if (item instanceof NetMagnetItem) // 磁铁
        {
            evt.addCapability(CuriosCapability.ID_ITEM, CuriosApi.createCurioProvider(new ICurio()
            {

                @Override
                public ItemStack getStack()
                {
                    return stack;
                }

                @Override
                public void curioTick(SlotContext slotContext)
                {
                    // 在此处添加tick逻辑
                    item.inventoryTick(stack, slotContext.entity().level(), slotContext.entity(), slotContext.index(), false);
                }
            }));
        }
        else if (item instanceof NetFeederItem) // 喂食器
        {
            evt.addCapability(CuriosCapability.ID_ITEM, CuriosApi.createCurioProvider(new ICurio()
            {

                @Override
                public ItemStack getStack()
                {
                    return stack;
                }

                @Override
                public void curioTick(SlotContext slotContext)
                {
                    // 在此处添加tick逻辑
                    item.inventoryTick(stack, slotContext.entity().level(), slotContext.entity(), slotContext.index(), false);
                }
            }));
        }
        else if(item instanceof NetRestockerItem)
        {
            evt.addCapability(CuriosCapability.ID_ITEM, CuriosApi.createCurioProvider(new ICurio()
            {

                @Override
                public ItemStack getStack()
                {
                    return stack;
                }

                @Override
                public void curioTick(SlotContext slotContext)
                {
                    item.inventoryTick(stack, slotContext.entity().level(), slotContext.entity(), slotContext.index(), false);
                }
            }));
        }
    }
}
