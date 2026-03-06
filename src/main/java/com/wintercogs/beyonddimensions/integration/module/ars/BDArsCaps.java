package com.wintercogs.beyonddimensions.integration.module.ars;


import com.hollingsworth.arsnouveau.api.source.ISourceTile;
import com.hollingsworth.arsnouveau.common.block.tile.ImbuementTile;
import com.hollingsworth.arsnouveau.setup.registry.BlockRegistry;
import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import com.wintercogs.beyonddimensions.integration.module.ars.caps.BESourceProvider;
import com.wintercogs.beyonddimensions.integration.module.ars.caps.ISourceCap;
import com.wintercogs.beyonddimensions.integration.module.ars.caps.ItemSourceProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.IEventBus;

public class BDArsCaps
{
    public static Capability<ISourceCap> SOURCE_CAP = CapabilityManager.get(new CapabilityToken<ISourceCap>()
    {
    });

    public static void registerCapability(IEventBus modEventBus)
    {
        modEventBus.addListener(BDArsCaps::onRegisterCaps);
        MinecraftForge.EVENT_BUS.addGenericListener(ItemStack.class, BDArsCaps::attachItemCaps);
        MinecraftForge.EVENT_BUS.addGenericListener(BlockEntity.class, BDArsCaps::attachBlockEntityCaps);
    }

    public static void onRegisterCaps(RegisterCapabilitiesEvent e)
    {
        e.register(ISourceCap.class);
    }

    // 为魔源罐和创造魔源罐附加物品能力
    public static void attachItemCaps(AttachCapabilitiesEvent<ItemStack> e)
    {
        ItemStack stack = e.getObject();

        // 魔源罐和创造魔源罐
        if (stack.getItem() == BlockRegistry.SOURCE_JAR.get().asItem() || stack.getItem() == BlockRegistry.CREATIVE_SOURCE_JAR.get().asItem())
        {
            var prov = new ItemSourceProvider(stack);
            e.addCapability(ResourceLocation.tryBuild(BDConstants.MODID, "source"), prov);
            e.addListener(prov::invalidate);
        }
    }

    // 为绝大部分魔源方块附加方块实体能力
    public static void attachBlockEntityCaps(AttachCapabilitiesEvent<BlockEntity> e)
    {
        BlockEntity be = e.getObject();

        // 给所有实现了某接口的方块实体统一附加
        if (be instanceof ISourceTile holder)
        {
            // ImbuementTile指灌注室，其实现的传输速率为0，注册无意义。且其内部本身时刻生产魔源，有失平衡
            if (!(holder instanceof ImbuementTile))
            {
                var prov = new BESourceProvider(holder);
                e.addCapability(ResourceLocation.tryBuild(BDConstants.MODID, "source"), prov);
                e.addListener(prov::invalidate);
            }
        }

        // 也可以：按具体 BE 类、按方块、按标签等灵活判断
    }
}
