package com.wintercogs.beyonddimensions.common.init;

import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import com.wintercogs.beyonddimensions.api.menu.workbench.PlayerWorkbenches;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public class BDAttachments
{
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, BDConstants.MODID);

    // 玩家留在工作台里的物品与工作台的偏好，死亡后随玩家保留。早先只有合成格，存档里沿用这个 ID
    public static final Supplier<AttachmentType<PlayerWorkbenches>> WORKBENCHES = ATTACHMENT_TYPES.register(
            "crafting_grid", () -> AttachmentType.serializable(PlayerWorkbenches::new).copyOnDeath().build()
    );

    public static void register(IEventBus eventBus)
    {
        ATTACHMENT_TYPES.register(eventBus);
    }
}
