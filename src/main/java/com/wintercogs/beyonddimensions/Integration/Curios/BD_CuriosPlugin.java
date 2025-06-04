package com.wintercogs.beyonddimensions.Integration.Curios;

import com.wintercogs.beyonddimensions.Item.ModItems;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import top.theillusivec4.curios.api.CuriosCapability;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurio;

public class BD_CuriosPlugin
{
    // 动态附加物品为饰品
    public static void registerCapabilities(final RegisterCapabilitiesEvent evt)
    {
        evt.registerItem(
                CuriosCapability.ITEM,
                (stack, context) -> new ICurio() {
                    @Override
                    public ItemStack getStack() {
                        return stack; // 必须返回传入的stack
                    }
                    @Override
                    public void curioTick(SlotContext slotContext) {
                        // 在此添加持续生效逻辑

                    }
                },
                ModItems.NET_TERMINAL_ITEM // 目标物品
        );
    }
}
