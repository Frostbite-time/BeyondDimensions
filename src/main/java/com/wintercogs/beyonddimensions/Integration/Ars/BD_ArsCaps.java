package com.wintercogs.beyonddimensions.Integration.Ars;


import com.hollingsworth.arsnouveau.api.source.ISourceTile;
import com.hollingsworth.arsnouveau.common.block.tile.ImbuementTile;
import com.hollingsworth.arsnouveau.setup.registry.BlockRegistry;
import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.Integration.Ars.Caps.BESourceProvider;
import com.wintercogs.beyonddimensions.Integration.Ars.Caps.ISourceCap;
import com.wintercogs.beyonddimensions.Integration.Ars.Caps.ItemSourceProvider;
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

public class BD_ArsCaps
{
    public static Capability<ISourceCap> SOURCE_CAP = CapabilityManager.get(new CapabilityToken<ISourceCap>()
    {
    });

    public static void registerCapability(IEventBus modEventBus)
    {
        modEventBus.addListener(BD_ArsCaps::onRegisterCaps);
        MinecraftForge.EVENT_BUS.addGenericListener(ItemStack.class, BD_ArsCaps::attachItemCaps);
        MinecraftForge.EVENT_BUS.addGenericListener(BlockEntity.class, BD_ArsCaps::attachBlockEntityCaps);
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
            e.addCapability(ResourceLocation.tryBuild(BeyondDimensions.MODID, "source"), prov);
            e.addListener(prov::invalidate); // 生命周期同步，重要！
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
                var prov = new BESourceProvider(holder); // 见下文 Provider
                e.addCapability(ResourceLocation.tryBuild(BeyondDimensions.MODID, "source"), prov);
                e.addListener(prov::invalidate); // 跟随 BE 生命周期
            }
        }

        // 也可以：按具体 BE 类、按方块、按标签等灵活判断
    }
}
