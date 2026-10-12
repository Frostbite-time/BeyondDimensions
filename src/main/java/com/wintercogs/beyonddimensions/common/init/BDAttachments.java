package com.wintercogs.beyonddimensions.common.init;

import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import com.wintercogs.beyonddimensions.common.menu.PlayerCraftingGrid;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public class BDAttachments
{
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, BDConstants.MODID);

    // 玩家的合成格，死亡后随玩家保留
    public static final Supplier<AttachmentType<PlayerCraftingGrid>> CRAFTING_GRID = ATTACHMENT_TYPES.register(
            "crafting_grid", () -> AttachmentType.serializable(PlayerCraftingGrid::new).copyOnDeath().build()
    );

    public static void register(IEventBus eventBus)
    {
        ATTACHMENT_TYPES.register(eventBus);
    }
}
