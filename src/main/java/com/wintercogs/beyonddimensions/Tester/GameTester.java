package com.wintercogs.beyonddimensions.Tester;

import com.wintercogs.beyonddimensions.Api.DataBase.DimensionsNet;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.ItemStackType;
import com.wintercogs.beyonddimensions.Api.DataBase.Storage.UnifiedStorage;
import com.wintercogs.beyonddimensions.BeyondDimensions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

// 存放一些用于性能测试的函数
public class GameTester
{
    public static void OnSeverStartTester(MinecraftServer server)
    {
        // 必要准备，以便测试目标性能而不过多掺杂其他因素

        // 从注册表获取所有非空气物品
        List<Item> allItems = BuiltInRegistries.ITEM.stream()
                .filter(item -> item != Items.AIR)
                .collect(Collectors.toList());


        BeyondDimensions.LOGGER.info("============= 存储系统性能测试报告 =============");

        long totalStartTime = System.nanoTime();
        long tenStartTime = System.nanoTime();
        int totalTestTimes = 2000;

        for(int times = 0;times < totalTestTimes;times++)
        {
            long timesStartTime = System.nanoTime();


            // 创建新存储
            UnifiedStorage storage = new UnifiedStorage(new DimensionsNet(true));
            // 创建随机数生成器
            Random random = new Random();
            // 打乱物品列表保证随机性
            Collections.shuffle(allItems, random);
            // 生成随机物品
            int count = allItems.size();
            for (int i = 0; i < count; i++) {
                Item item = allItems.get(i);
                int amount = 100 + random.nextInt(201); // 生成100-300之间的随机数量

                ItemStackType stack = new ItemStackType(new ItemStack(item, amount));

                storage.insert(stack,false);
            }
            for(int i = storage.getSlots() - 1; i >=0 ; i--)
            {
                storage.extract(i,1,false);
                storage.extract(storage.getStackBySlot(i),false);
            }



            long timesUseTime = System.nanoTime() - timesStartTime;
            double timesMS = Math.round(timesUseTime / 100000.0) / 10.0;
            BeyondDimensions.LOGGER.info("第 {}次 执行时间: {} ms",times, timesMS);
            if(times == 9)
            {
                tenStartTime = System.nanoTime();
            }
        }

        long totalEndTime = System.nanoTime();

        long totalElapsed = totalEndTime - totalStartTime;
        double totalMs = Math.round(totalElapsed / 100000.0) / 10.0;

        BeyondDimensions.LOGGER.info("总结报告：");
        BeyondDimensions.LOGGER.info("本次测试物品种类为：{}",allItems.size());

        BeyondDimensions.LOGGER.info("✅ 总执行时间: {} ms", totalMs);
        BeyondDimensions.LOGGER.info("平均每次执行时间: {} ms", totalMs/totalTestTimes);

        double tenElseMS = Math.round((totalEndTime - tenStartTime)/100000.0) / 10.0;
        BeyondDimensions.LOGGER.info("除去前10次：");
        BeyondDimensions.LOGGER.info("总执行时间: {} ms", tenElseMS);
        BeyondDimensions.LOGGER.info("平均每次执行时间: {} ms", tenElseMS/(totalTestTimes-10));


    }
}
