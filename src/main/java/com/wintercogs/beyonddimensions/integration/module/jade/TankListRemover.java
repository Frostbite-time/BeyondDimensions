package com.wintercogs.beyonddimensions.integration.module.jade;

import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

import java.util.List;

/**
 * 去掉其他模组逐个列出储罐的显示：BD 的能力把空槽也算作储罐，网络接口会因此列出整列空储罐。
 * 设备里的资源由 {@link DeviceContentsComponent} 显示
 */
public enum TankListRemover implements IBlockComponentProvider
{
    INSTANCE;

    private static final ResourceLocation UID =
            new ResourceLocation(BDConstants.MODID, "tank_list_remover");
    // Mekanism 为暴露化学品、流体或能量能力的方块逐个列出储罐，四种化学品各有标签
    private static final List<ResourceLocation> TANK_LISTS = List.of(
            new ResourceLocation("mekanism", "gas"),
            new ResourceLocation("mekanism", "infuse_type"),
            new ResourceLocation("mekanism", "pigment"),
            new ResourceLocation("mekanism", "slurry"),
            new ResourceLocation("mekanism", "fluid"),
            new ResourceLocation("mekanism", "energy")
    );

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config)
    {
        TANK_LISTS.forEach(tooltip::remove);
    }

    @Override
    public ResourceLocation getUid()
    {
        return UID;
    }

    @Override
    public boolean isRequired()
    {
        return true;
    }

    @Override
    public int getDefaultPriority()
    {
        // 排在 Mekanism 的显示之后
        return 100;
    }
}
