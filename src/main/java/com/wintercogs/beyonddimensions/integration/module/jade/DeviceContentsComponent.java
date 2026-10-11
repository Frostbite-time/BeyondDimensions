package com.wintercogs.beyonddimensions.integration.module.jade;

import com.wintercogs.beyonddimensions.api.storage.key.KeyAmount;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.JadeIds;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.IDisplayHelper;
import snownee.jade.api.ui.IElement;
import snownee.jade.api.ui.IElementHelper;
import snownee.jade.api.ui.ScreenDirection;

import java.util.ArrayList;
import java.util.List;

/**
 * 设备内容：种类少时每种一行并带名称，种类多时排成格子。显示上限、每行个数与带名称的阈值沿用 Jade 物品存储的设置
 */
public enum DeviceContentsComponent implements IBlockComponentProvider
{
    INSTANCE;

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config)
    {
        Tag data = accessor.getServerData().get(DeviceContentsProvider.UID.toString());
        if (data == null)
            return;
        List<KeyAmount> contents = accessor.decodeFromNbt(DeviceContentsProvider.CODEC, data).orElse(List.of());
        int shown = Math.min(contents.size(), config.getInt(accessor.showDetails()
                ? JadeIds.UNIVERSAL_ITEM_STORAGE_DETAILED_AMOUNT
                : JadeIds.UNIVERSAL_ITEM_STORAGE_NORMAL_AMOUNT));
        boolean named = contents.size() < config.getInt(JadeIds.UNIVERSAL_ITEM_STORAGE_SHOW_NAME_AMOUNT);
        int perLine = named ? 1 : config.getInt(JadeIds.UNIVERSAL_ITEM_STORAGE_ITEMS_PER_LINE);

        // 名称前的数量右对齐
        Font font = Minecraft.getInstance().font;
        int amountWidth = 0;
        if (named)
        {
            for (int i = 0; i < shown; i++)
                amountWidth = Math.max(amountWidth, font.width(ResourceElement.amountText(contents.get(i).amount())));
        }

        IElementHelper helper = IElementHelper.get();
        List<IElement> line = new ArrayList<>();
        int inLine = 0;
        for (int i = 0; i < shown; i++)
        {
            if (inLine == perLine)
            {
                tooltip.add(line);
                tooltip.setLineMargin(-1, ScreenDirection.DOWN, 0);
                line = new ArrayList<>();
                inLine = 0;
            }
            KeyAmount stack = contents.get(i);
            if (named)
            {
                String amount = ResourceElement.amountText(stack.amount());
                line.add(ResourceElement.small(stack));
                int width = font.width(amount);
                if (width < amountWidth)
                    line.add(helper.spacer(amountWidth - width, 0));
                Component name = IDisplayHelper.get().stripColor(ResourceElement.name(stack.key()));
                line.add(helper.text(Component.literal(amount + "× ").append(name)).message(null));
            }
            else
            {
                line.add(ResourceElement.of(stack));
            }
            inLine++;
        }
        if (!line.isEmpty())
            tooltip.add(line);
    }

    @Override
    public ResourceLocation getUid()
    {
        return DeviceContentsProvider.UID;
    }

    @Override
    public int getDefaultPriority()
    {
        // 与 Jade 的物品存储同一位置
        return 1000;
    }
}
