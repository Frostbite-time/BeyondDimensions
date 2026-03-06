package com.wintercogs.beyonddimensions.common.item;

import com.wintercogs.beyonddimensions.api.dimensionnet.DimensionsNet;
import com.wintercogs.beyonddimensions.api.dimensionnet.UnifiedStorage;
import com.wintercogs.beyonddimensions.api.storage.key.impl.ItemStackKey;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class TestItem_ItemGenerate extends Item
{
    public TestItem_ItemGenerate(Item.Properties properties)
    {
        super(properties);
    }


    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand)
    {
        if (!level.isClientSide())
        {
            DimensionsNet net = DimensionsNet.getNetFromPlayer(player);
            if (net != null)
            {
                UnifiedStorage storage = net.getUnifiedStorage();
                // 从注册表获取所有非空气物品
                List<Item> allItems = BuiltInRegistries.ITEM.stream()
                        .filter(item -> item != Items.AIR)
                        .collect(Collectors.toList());

                // 创建随机数生成器
                Random random = new Random();

                // 打乱物品列表保证随机性
                Collections.shuffle(allItems, random);

                // 生成100种随机物品
                int count = Math.min(100, allItems.size());

                for (int i = 0; i < count; i++)
                {
                    Item item = allItems.get(i);
                    int amount = 100 + random.nextInt(201); // 生成100-300之间的随机数量

                    ItemStackKey stack = new ItemStackKey(new ItemStack(item, 1));

                    storage.insert(stack, amount, false);
                }
            }
        }


        return InteractionResultHolder.sidedSuccess(player.getItemInHand(usedHand), level.isClientSide());
    }
}
