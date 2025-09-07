package com.wintercogs.beyonddimensions.Tester;

import com.wintercogs.beyonddimensions.Api.DataBase.DimensionsNet;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.ItemStackKey;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.KeyAmount;
import com.wintercogs.beyonddimensions.Api.DataBase.Storage.UnifiedStorage;
import com.wintercogs.beyonddimensions.BeyondDimensions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

// 存放一些用于性能测试的函数
public class GameTester
{
    public static void OnSeverStartTester(MinecraftServer server)
    {
        // 必要准备
        List<Item> allItems = BuiltInRegistries.ITEM.stream()
                .filter(item -> item != Items.AIR)
                .collect(Collectors.toList());
        BeyondDimensions.LOGGER.info("============= 存储系统性能测试报告 =============");
        // 预先生成所有测试数据（2000组）
        final int totalTestTimes = 2000;
        List<List<KeyAmount>> allInsertData = new ArrayList<>(totalTestTimes);
        List<int[]> allExtractData = new ArrayList<>(totalTestTimes);
        Random masterRandom = new Random(42); // 固定种子保证可重复性
        for (int i = 0; i < totalTestTimes; i++) {
            // 打乱物品顺序
            List<Item> shuffledItems = new ArrayList<>(allItems);
            Collections.shuffle(shuffledItems, new Random(masterRandom.nextLong()));

            // 生成插入数据
            List<KeyAmount> insertData = new ArrayList<>();
            for (Item item : shuffledItems) {
                int amount = 100 + masterRandom.nextInt(201);
                insertData.add(new KeyAmount(new ItemStackKey(new ItemStack(item, amount)),amount));
            }
            allInsertData.add(insertData);

            // 生成提取数据（每个槽位提取量）
            int[] extractData = new int[allItems.size()];
            for (int j = 0; j < extractData.length; j++) {
                extractData[j] = masterRandom.nextInt(400);
            }
            allExtractData.add(extractData);
        }
        // 预热阶段（不记录结果）
        UnifiedStorage warmupStorage = new UnifiedStorage(new DimensionsNet(true));
        for (int i = 0; i < 50; i++) {
            List<KeyAmount> data = allInsertData.get(0);
            for (KeyAmount stack : data) {
                warmupStorage.insert(stack.key(),stack.amount(), false);
            }
            int[] extData = allExtractData.get(0);
            for (int j = warmupStorage.getSlots() - 1; j >= 0; j--) {
                warmupStorage.extract(j, extData[j], false);
            }
            warmupStorage.clearStorage(); // 假设有清空方法
        }
        // 正式测试
        long[] insertTimes = new long[totalTestTimes];
        long[] extractTimes = new long[totalTestTimes];
        long[] combinedTimes = new long[totalTestTimes];
        for (int times = 0; times < totalTestTimes; times++) {
            UnifiedStorage storage = new UnifiedStorage(new DimensionsNet(true));
            List<KeyAmount> insertData = allInsertData.get(times);
            int[] extractData = allExtractData.get(times);
            // 纯插入测试
            long insertStart = System.nanoTime();
            for (KeyAmount stack : insertData) {
                storage.insert(stack.key(),stack.amount(), false);
            }
            insertTimes[times] = System.nanoTime() - insertStart;
            // 纯提取测试（重置存储状态）
            storage.clearStorage();
            for (KeyAmount stack : insertData) {
                storage.insert(stack.key(),stack.amount(), false); // 重新填充
            }

            long extractStart = System.nanoTime();
            for (int i = storage.getSlots() - 1; i >= 0; i--) {
                storage.extract(i, extractData[i], false);
            }
            extractTimes[times] = System.nanoTime() - extractStart;
            // 综合测试（新建存储）
            UnifiedStorage combinedStorage = new UnifiedStorage(new DimensionsNet(true));
            long combinedStart = System.nanoTime();
            for (KeyAmount stack : insertData) {
                combinedStorage.insert(stack.key(),stack.amount(), false);
            }
            for (int i = combinedStorage.getSlots() - 1; i >= 0; i--) {
                combinedStorage.extract(i, extractData[i], false);
            }
            combinedTimes[times] = System.nanoTime() - combinedStart;
        }
        // 结果计算与报告（示例）
        reportPerformance("插入操作", insertTimes);
        reportPerformance("提取操作", extractTimes);
        reportPerformance("插入+提取操作", combinedTimes);



    }

    // 结果报告方法
    private static void reportPerformance(String name, long[] timings) {
        long total = 0;
        for (long t : timings) {
            total += t;
        }

        // 排除前10%的测试结果（消除预热影响）
        long filteredTotal = 0;
        int excludeCount = timings.length / 10;
        for (int i = excludeCount; i < timings.length; i++) {
            filteredTotal += timings[i];
        }

        double avgTotal = total / 1_000_000.0 / timings.length;
        double avgFiltered = filteredTotal / 1_000_000.0 / (timings.length - excludeCount);

        BeyondDimensions.LOGGER.info("{} 平均耗时: {} ms (排除前{}次后: {} ms)",
                name, avgTotal, excludeCount, avgFiltered);
    }
}
