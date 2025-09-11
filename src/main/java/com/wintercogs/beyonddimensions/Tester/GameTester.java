package com.wintercogs.beyonddimensions.Tester;

import com.wintercogs.beyonddimensions.Api.DataBase.DimensionsNet;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.ItemStackKey;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.KeyAmount;
import com.wintercogs.beyonddimensions.Api.DataBase.Storage.UnifiedStorage;
import com.wintercogs.beyonddimensions.BeyondDimensions;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Unbreakable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

// 存放一些用于性能测试的函数
public class GameTester {

    public static void OnSeverStartTester(MinecraftServer server)
    {
        // ========== 基础准备 ==========
        List<Item> allItems = BuiltInRegistries.ITEM.stream()
                .filter(item -> item != Items.AIR)
                .collect(Collectors.toList());

        BeyondDimensions.LOGGER.info("============= 存储系统性能测试报告 =============");
        BeyondDimensions.LOGGER.info("物品基数: {}", allItems.size());

        final int totalTestTimes = 700;
        final int warmupIters = 50;

        // 固定种子以保证可重复性
        final Random masterRandom = new Random(42);

        // ========== 预生成所有测试数据 ==========
        List<List<KeyAmount>> allInsertData = new ArrayList<>(totalTestTimes);
        List<int[]> allExtractData = new ArrayList<>(totalTestTimes);

        for (int i = 0; i < totalTestTimes; i++) {
            // 为每一轮构造独立 RNG，保证可复现同时又避免共享状态干扰
            Random roundRng = new Random(masterRandom.nextLong());

            // 打乱物品
            List<Item> shuffled = new ArrayList<>(allItems);
            Collections.shuffle(shuffled, new Random(masterRandom.nextLong()));

            // 生成插入数据：100~300；10% 的条目附带随机 Data Components
            List<KeyAmount> insertList = new ArrayList<>(shuffled.size());
            for (Item item : shuffled) {
                int amount = 100 + roundRng.nextInt(201);

                ItemStack stack = new ItemStack(item, amount);
                // 10% 概率：给该条目附带随机数据组件（1~3 个）
                if (roundRng.nextDouble() < 0.10) {
                    applyRandomComponents(stack, roundRng);
                }

                insertList.add(new KeyAmount(new ItemStackKey(stack), amount));
            }
            allInsertData.add(insertList);

            // 生成提取数据：每槽位尝试提取 0~399
            int[] extractArr = new int[shuffled.size()];
            for (int j = 0; j < extractArr.length; j++) {
                extractArr[j] = roundRng.nextInt(400);
            }
            allExtractData.add(extractArr);
        }

        // ========== 预热（不记录结果；避免 clearStorage() 依赖差异） ==========
        for (int i = 0; i < warmupIters; i++) {
            UnifiedStorage storage = new DimensionsNet(true).getUnifiedStorage();
            List<KeyAmount> data = allInsertData.get(i % allInsertData.size());
            int[] extData = allExtractData.get(i % allExtractData.size());

            // 填充
            for (KeyAmount ka : data) storage.insert(ka.key(), ka.amount(), false);

            // 提取（从后往前，且做边界对齐）
            int slots = Math.min(storage.getSlots(), extData.length);
            for (int s = slots - 1; s >= 0; s--) storage.extract(s, extData[s], false);
        }

        // ========== 正式测试 ==========
        long[] insertTimes = new long[totalTestTimes];
        long[] extractTimes = new long[totalTestTimes];
        long[] combinedTimes = new long[totalTestTimes];

        for (int t = 0; t < totalTestTimes; t++) {
            List<KeyAmount> insertData = allInsertData.get(t);
            int[] extractData = allExtractData.get(t);

            // --- 纯插入 ---
            UnifiedStorage storageInsert = new DimensionsNet(true).getUnifiedStorage();
            long t0 = System.nanoTime();
            for (KeyAmount ka : insertData) storageInsert.insert(ka.key(), ka.amount(), false);
            insertTimes[t] = System.nanoTime() - t0;

            // --- 纯提取（重新填充新容器，避免实现差异 & 状态影响） ---
            UnifiedStorage storageExtract = new DimensionsNet(true).getUnifiedStorage();
            for (KeyAmount ka : insertData) storageExtract.insert(ka.key(), ka.amount(), false);

            long t1 = System.nanoTime();
            int slots = Math.min(storageExtract.getSlots(), extractData.length);
            for (int s = slots - 1; s >= 0; s--) storageExtract.extract(s, extractData[s], false);
            extractTimes[t] = System.nanoTime() - t1;

            // --- 综合（同一容器先插入后提取） ---
            UnifiedStorage storageCombo = new DimensionsNet(true).getUnifiedStorage();
            long t2 = System.nanoTime();
            for (KeyAmount ka : insertData) storageCombo.insert(ka.key(), ka.amount(), false);
            int slots2 = Math.min(storageCombo.getSlots(), extractData.length);
            for (int s = slots2 - 1; s >= 0; s--) storageCombo.extract(s, extractData[s], false);
            combinedTimes[t] = System.nanoTime() - t2;
        }

        // ========== 报告 ==========
        reportPerformance("插入操作", insertTimes, allInsertData.get(0).size());
        reportPerformance("提取操作", extractTimes, allInsertData.get(0).size());
        reportPerformance("插入+提取操作", combinedTimes, allInsertData.get(0).size());
    }

    /**
     * 给 10% 的条目随机附加 1~3 个数据组件。
     * 注意：以下示例只使用了在多版本中最稳定的组件类型；如某组件在你版本不存在，直接删掉对应分支即可。
     */
    private static void applyRandomComponents(ItemStack stack, Random rng) {
        int howMany = 1 + rng.nextInt(3); // 1~3
        for (int i = 0; i < howMany; i++) {
            int pick = rng.nextInt(3);
            switch (pick) {
                case 0:
                    // 自定义名称：一定存在且最安全
                    stack.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,
                            net.minecraft.network.chat.Component.literal("BDTest#" + rng.nextInt(1_000_000)));
                    break;
                case 1:
                    // 是否不可破坏（若你版本没有 UNBREAKABLE，删掉该分支即可）
                    try {
                        stack.set(DataComponents.UNBREAKABLE,new Unbreakable(true));
                    } catch (Throwable ignored) {
                        // 某些版本没有该组件时，静默跳过即可
                    }
                    break;
                case 2:
                    // 修复花费（若你版本没有 REPAIR_COST，删掉该分支即可）
                    try {
                        stack.set(net.minecraft.core.component.DataComponents.REPAIR_COST, rng.nextInt(16));
                    } catch (Throwable ignored) {
                    }
                    break;
            }
        }
    }

    // ========== 统计 & 报告 ==========
    private static void reportPerformance(String name, long[] timingsNanos, int opsPerRun) {
        long[] arr = timingsNanos.clone();
        java.util.Arrays.sort(arr);

        double meanMs      = nanosToMs(mean(timingsNanos));
        double p50Ms       = nanosToMs(percentile(arr, 50));
        double p90Ms       = nanosToMs(percentile(arr, 90));
        double p99Ms       = nanosToMs(percentile(arr, 99));
        double trimmed5Ms  = nanosToMs(trimmedMean(arr, 0.05));
        double meanNsPerOp = mean(timingsNanos) / Math.max(1, opsPerRun);

        String msg = String.format(
                java.util.Locale.ROOT,
                "%s => mean: %.3f ms | p50: %.3f ms | p90: %.3f ms | p99: %.3f ms | trimmed(5%%): %.3f ms | ≈ %.1f ns/op",
                name, meanMs, p50Ms, p90Ms, p99Ms, trimmed5Ms, meanNsPerOp
        );
        BeyondDimensions.LOGGER.info(msg);
    }
    // ========== 简单统计工具 ==========
    private static double nanosToMs(double nanos) {
        return nanos / 1_000_000.0;
    }

    private static double mean(long[] a) {
        long sum = 0;
        for (long v : a) sum += v;
        return (double) sum / a.length;
    }
    private static double percentile(long[] sorted, int p) {
        if (sorted.length == 0) return 0;
        double pos = (p / 100.0) * (sorted.length - 1);
        int i = (int) Math.floor(pos);
        int j = Math.min(i + 1, sorted.length - 1);
        double frac = pos - i;
        return sorted[i] * (1.0 - frac) + sorted[j] * frac;
    }

    private static double trimmedMean(long[] sorted, double trimRatio) {
        int n = sorted.length;
        int cut = (int) Math.floor(n * trimRatio);
        int from = cut;
        int to = Math.max(from, n - cut); // 半开区间 [from, to)
        if (from >= to) return mean(sorted);
        long sum = 0;
        int cnt = 0;
        for (int i = from; i < to; i++) { sum += sorted[i]; cnt++; }
        return (double) sum / Math.max(cnt, 1);
    }
}

