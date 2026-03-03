package com.wintercogs.beyonddimensions.Integration.Curios;

import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

public class BD_CuriosPlugin
{
    // 动态附加物品为饰品
    public static void registerCapabilities(final RegisterCapabilitiesEvent evt)
    {
//        // 终端
//        evt.registerItem(
//                CuriosCapability.ITEM,
//                (stack, context) -> new ICurio()
//                {
//                    @Override
//                    public ItemStack getStack()
//                    {
//                        return stack; // 必须返回传入的stack
//                    }
//
//                    @Override
//                    public void curioTick(SlotContext slotContext)
//                    {
//                        // 在此添加持续生效逻辑
//                    }
//                },
//                BDItems.NET_TERMINAL_ITEM // 目标物品
//        );
//        // 磁铁
//        evt.registerItem(
//                CuriosCapability.ITEM,
//                (stack, context) -> new ICurio()
//                {
//                    @Override
//                    public ItemStack getStack()
//                    {
//                        return stack; // 必须返回传入的stack
//                    }
//
//                    @Override
//                    public void curioTick(SlotContext slotContext)
//                    {
//                        if (!(slotContext.entity().level() instanceof ServerLevel serverLevel)) return;
//
//                        // 在此添加持续生效逻辑
//                        if (stack.getItem() == BDItems.NET_MAGNET_ITEM.get())
//                        {
//                            NetMagnetItem item = (NetMagnetItem) stack.getItem();
//                            item.inventoryTick(stack, serverLevel, slotContext.entity(), null);
//                        }
//                    }
//                },
//                BDItems.NET_MAGNET_ITEM // 目标物品
//        );
//        // 喂食器
//        evt.registerItem(
//                CuriosCapability.ITEM,
//                (stack, context) -> new ICurio()
//                {
//                    @Override
//                    public ItemStack getStack()
//                    {
//                        return stack; // 必须返回传入的stack
//                    }
//
//                    @Override
//                    public void curioTick(SlotContext slotContext)
//                    {
//                        if (!(slotContext.entity().level() instanceof ServerLevel serverLevel)) return;
//                        // 在此添加持续生效逻辑
//                        if (stack.getItem() == BDItems.NET_FEEDER_ITEM.get())
//                        {
//                            NetFeederItem item = (NetFeederItem) stack.getItem();
//                            item.inventoryTick(stack, serverLevel, slotContext.entity(), null);
//                        }
//                    }
//                },
//                BDItems.NET_FEEDER_ITEM // 目标物品
//        );
//        // 补货器
//        evt.registerItem(
//                CuriosCapability.ITEM,
//                (stack, context) -> new ICurio()
//                {
//                    @Override
//                    public ItemStack getStack()
//                    {
//                        return stack;
//                    }
//
//                    @Override
//                    public void curioTick(SlotContext slotContext)
//                    {
//                        if (!(slotContext.entity().level() instanceof ServerLevel serverLevel)) return;
//                        if (stack.getItem() == BDItems.NET_RESTOCKER_ITEM.get())
//                        {
//                            NetRestockerItem item = (NetRestockerItem) stack.getItem();
//                            item.inventoryTick(stack, serverLevel, slotContext.entity(), null);
//                        }
//                    }
//                },
//                BDItems.NET_RESTOCKER_ITEM
//        );
    }
}
