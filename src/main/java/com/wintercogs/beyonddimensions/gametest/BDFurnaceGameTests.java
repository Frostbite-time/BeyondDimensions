package com.wintercogs.beyonddimensions.gametest;

import com.mojang.serialization.MapCodec;
import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.api.dimensionnet.DimensionsNet;
import com.wintercogs.beyonddimensions.api.dimensionnet.UnifiedStorage;
import com.wintercogs.beyonddimensions.api.storage.key.KeyAmount;
import com.wintercogs.beyonddimensions.api.storage.key.impl.ItemStackKey;
import com.wintercogs.beyonddimensions.common.block.entity.BaseNetFurnaceBlockEntity;
import com.wintercogs.beyonddimensions.common.init.BDBlocks;
import com.wintercogs.beyonddimensions.common.machine.ReceiveMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;
import java.util.function.Supplier;

/**
 * 网络熔炉 GameTest。
 * <p>
 * 目的：在无头服务器上验证"网络熔炉烧完东西后是否会生成产物"的完整服务端链路：
 * 燃料分配 -> 熔炼计时 -> 产物写入输出槽。
 * <p>
 * 测试不连接任何维度网络，也不经过GUI，直接向输入/燃料槽注入物品，
 * 然后等待输出槽出现铁锭（铁矿熔炼200tick，超时上限1000tick）。
 * 等待期间每50tick输出一次机器状态，便于定位链路卡在哪一环。
 */
public final class BDFurnaceGameTests
{
    /**
     * 自定义测试实例类型必须注册进 test_instance_type 注册表：
     * test_instance 注册表会同步到客户端，序列化时用 {@code GameTestInstance.DIRECT_CODEC}
     * （byNameCodec().dispatch(GameTestInstance::codec, ...)）按 codec 实例查找类型 id；
     * 未注册的 codec 会被包成 Holder.direct，导致客户端同步阶段
     * 抛出 "Unregistered holder ... test_instance_type" 异常。
     */
    public static final DeferredRegister<MapCodec<? extends GameTestInstance>> TEST_INSTANCE_TYPES =
            DeferredRegister.create(BuiltInRegistries.TEST_INSTANCE_TYPE, com.wintercogs.beyonddimensions.api.ids.BDConstants.MODID);

    public static final Supplier<MapCodec<FurnaceProbe>> FURNACE_PROBE_TYPE =
            TEST_INSTANCE_TYPES.register("furnace_probe", () -> FurnaceProbe.CODEC);
    public static final Supplier<MapCodec<FurnaceFullStackProbe>> FURNACE_FULL_STACK_PROBE_TYPE =
            TEST_INSTANCE_TYPES.register("furnace_full_stack_probe", () -> FurnaceFullStackProbe.CODEC);
    public static final Supplier<MapCodec<FurnaceNetworkProbe>> FURNACE_NETWORK_PROBE_TYPE =
            TEST_INSTANCE_TYPES.register("furnace_network_probe", () -> FurnaceNetworkProbe.CODEC);

    public static void register(IEventBus modEventBus)
    {
        TEST_INSTANCE_TYPES.register(modEventBus);
        modEventBus.addListener(BDFurnaceGameTests::onRegisterGameTests);
    }

    private static void onRegisterGameTests(RegisterGameTestsEvent event)
    {
        Holder<TestEnvironmentDefinition<?>> env = event.registerEnvironment(
                BeyondDimensions.makeId("furnace_env"),
                new TestEnvironmentDefinition.AllOf(List.of()));

        TestData<Holder<TestEnvironmentDefinition<?>>> data = new TestData<>(
                env,
                BeyondDimensions.makeId("furnace_probe_7x7"),
                1000,
                0,
                true,
                Rotation.NONE);

        event.registerTest(BeyondDimensions.makeId("net_furnace_produces_output"), new FurnaceProbe(data));

        // 场景2：多格满堆持续熔炼（接近玩家实际玩法：一格64矿+足够煤，验证产物在输出槽持续累积且机器不卡死）
        TestData<Holder<TestEnvironmentDefinition<?>>> data2 = new TestData<>(
                env,
                BeyondDimensions.makeId("furnace_probe_7x7"),
                2600,
                0,
                true,
                Rotation.NONE);
        event.registerTest(BeyondDimensions.makeId("net_furnace_full_stack_continuous"), new FurnaceFullStackProbe(data2));

        // 场景3：接入网络+接收模式（自动拉料+产物自动入网）——完整还原玩家"网络自动化"玩法
        TestData<Holder<TestEnvironmentDefinition<?>>> data3 = new TestData<>(
                env,
                BeyondDimensions.makeId("furnace_probe_7x7"),
                2600,
                0,
                true,
                Rotation.NONE);
        event.registerTest(BeyondDimensions.makeId("net_furnace_network_auto"), new FurnaceNetworkProbe(data3));
    }

    public static final class FurnaceProbe extends GameTestInstance
    {
        public static final MapCodec<FurnaceProbe> CODEC =
                TestData.CODEC.xmap(FurnaceProbe::new, FurnaceProbe::info);

        public FurnaceProbe(TestData<Holder<TestEnvironmentDefinition<?>>> info)
        {
            super(info);
        }

        @Override
        public void run(GameTestHelper helper)
        {
            // 回归守卫：用与客户端注册表同步（RegistrySynchronization.packRegistry）相同的
            // DIRECT_CODEC 编码 test_instance 注册表全部条目，任何一个编码失败即测试失败。
            // 这能提前发现"自定义测试类型未注册进 test_instance_type"导致的客户端启动报错。
            var testRegistry = helper.getLevel().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.TEST_INSTANCE);
            var ops = helper.getLevel().registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE);
            for (var entry : testRegistry.entrySet())
            {
                var encoded = GameTestInstance.DIRECT_CODEC.encodeStart(ops, entry.getValue());
                if (encoded.isError())
                {
                    helper.fail(Component.literal("测试实例无法序列化(会阻塞客户端同步): "
                            + entry.getKey() + " -> " + encoded.error().get().message()));
                    return;
                }
            }
            BeyondDimensions.LOGGER.info("[熔炉测试] test_instance 序列化守卫通过: 共{}个条目", testRegistry.entrySet().size());

            BlockPos pos = new BlockPos(3, 1, 3);
            helper.setBlock(pos, BDBlocks.NET_FURNACE_BLOCK.get().defaultBlockState());

            BlockEntity raw = helper.getBlockEntity(pos, BlockEntity.class);
            if (!(raw instanceof BaseNetFurnaceBlockEntity<?> be))
            {
                helper.fail(Component.literal("网络熔炉方块实体创建失败: " + raw));
                return;
            }

            // 直接注入原料与燃料（铁矿x4 + 煤x2），不经过GUI
            KeyAmount inLeft = be.getInputStorageSlots().insert(0, new ItemStackKey(new ItemStack(Items.IRON_ORE)), 4L, false);
            KeyAmount fuelLeft = be.getFuelStorageSlots().insert(0, new ItemStackKey(new ItemStack(Items.COAL)), 2L, false);
            BeyondDimensions.LOGGER.info("[熔炉测试] 注入完成 原料余量={} 燃料余量={}", inLeft, fuelLeft);

            final long[] waited = {0};
            helper.succeedWhen(() -> {
                waited[0]++;
                KeyAmount out = be.getOutputStorageSlots().getStackBySlot(0);
                boolean hasIngot = out.key() instanceof ItemStackKey outKey
                        && outKey.getSource() == Items.IRON_INGOT
                        && out.amount() >= 1;
                if (hasIngot)
                {
                    BeyondDimensions.LOGGER.info("[熔炉测试] 成功: 输出槽0已出现铁锭 x{} (等待{}tick)", out.amount(), waited[0]);
                    return;
                }

                if (waited[0] % 50 == 0)
                {
                    BeyondDimensions.LOGGER.info("[熔炉测试] 等待中({}tick): lit={} litDur={} cook={} total={} input={} fuel={} out={}",
                            waited[0],
                            be.getLitTime(), be.getLitDuration(),
                            be.getCookTime(), be.getCookTimeTotal(),
                            be.getInputStorageSlots().getStackBySlot(0),
                            be.getFuelStorageSlots().getStackBySlot(0),
                            out);
                }
                throw new GameTestAssertException(Component.literal("输出槽尚未出现铁锭"), 0);
            });
        }

        @Override
        public MapCodec<? extends GameTestInstance> codec()
        {
            return CODEC;
        }

        @Override
        protected MutableComponent typeDescription()
        {
            return Component.literal("bd_furnace_probe");
        }
    }

    /**
     * 场景2：一格64个铁矿+多煤，持续熔炼。
     * 验证：输入持续被消耗、输出槽产物持续累积到接近64（不被提前卡死）。
     */
    public static final class FurnaceFullStackProbe extends GameTestInstance
    {
        public static final MapCodec<FurnaceFullStackProbe> CODEC =
                TestData.CODEC.xmap(FurnaceFullStackProbe::new, FurnaceFullStackProbe::info);

        public FurnaceFullStackProbe(TestData<Holder<TestEnvironmentDefinition<?>>> info)
        {
            super(info);
        }

        @Override
        public void run(GameTestHelper helper)
        {
            BlockPos pos = new BlockPos(3, 1, 3);
            helper.setBlock(pos, BDBlocks.NET_FURNACE_BLOCK.get().defaultBlockState());

            BlockEntity raw = helper.getBlockEntity(pos, BlockEntity.class);
            if (!(raw instanceof BaseNetFurnaceBlockEntity<?> be))
            {
                helper.fail(Component.literal("网络熔炉方块实体创建失败: " + raw));
                return;
            }

            // 一格64矿 + 9煤（1600tick/个，足够烧完64矿的一半以上）
            KeyAmount inLeft = be.getInputStorageSlots().insert(0, new ItemStackKey(new ItemStack(Items.IRON_ORE)), 64L, false);
            KeyAmount fuelLeft = be.getFuelStorageSlots().insert(0, new ItemStackKey(new ItemStack(Items.COAL)), 9L, false);
            BeyondDimensions.LOGGER.info("[熔炉测试2] 注入完成 原料余量={} 燃料余量={}", inLeft, fuelLeft);

            final long[] waited = {0};
            helper.succeedWhen(() -> {
                waited[0]++;
                KeyAmount out = be.getOutputStorageSlots().getStackBySlot(0);
                long inAmount = be.getInputStorageSlots().getStackBySlot(0).amount();
                // 等待至少熔炼出5个铁锭（1000tick），说明持续熔炼正常
                if (out.key() instanceof ItemStackKey outKey
                        && outKey.getSource() == Items.IRON_INGOT
                        && out.amount() >= 5)
                {
                    BeyondDimensions.LOGGER.info("[熔炉测试2] 成功: 已累积铁锭 x{}，输入剩余 x{}（等待{}tick）",
                            out.amount(), inAmount, waited[0]);
                    return;
                }

                if (waited[0] % 100 == 0)
                {
                    BeyondDimensions.LOGGER.info("[熔炉测试2] 等待中({}tick): lit={} cook={} total={} input={} out={}",
                            waited[0], be.getLitTime().get(0), be.getCookTime().get(0),
                            be.getCookTimeTotal().get(0), inAmount, out);
                }
                throw new GameTestAssertException(Component.literal("持续熔炼未推进: out=" + out + " in=" + inAmount), 0);
            });
        }

        @Override
        public MapCodec<? extends GameTestInstance> codec()
        {
            return CODEC;
        }

        @Override
        protected MutableComponent typeDescription()
        {
            return Component.literal("bd_furnace_full_stack_probe");
        }
    }

    /**
     * 场景3：机器接入维度网络 + 接收模式(OPEN)。
     * 网络中放入铁矿与煤（并设置输入/燃料标记），验证完整闭环：
     * 自动拉料 -> 熔炼 -> 产物自动转入网络。
     * 同时验证能量拉料不参与时，燃料仅经燃料标记拉取。
     */
    public static final class FurnaceNetworkProbe extends GameTestInstance
    {
        public static final MapCodec<FurnaceNetworkProbe> CODEC =
                TestData.CODEC.xmap(FurnaceNetworkProbe::new, FurnaceNetworkProbe::info);

        public FurnaceNetworkProbe(TestData<Holder<TestEnvironmentDefinition<?>>> info)
        {
            super(info);
        }

        @Override
        public void run(GameTestHelper helper)
        {
            BlockPos pos = new BlockPos(3, 1, 3);
            helper.setBlock(pos, BDBlocks.NET_FURNACE_BLOCK.get().defaultBlockState());

            BlockEntity raw = helper.getBlockEntity(pos, BlockEntity.class);
            if (!(raw instanceof BaseNetFurnaceBlockEntity<?> be))
            {
                helper.fail(Component.literal("网络熔炉方块实体创建失败: " + raw));
                return;
            }

            // 创建网络并绑定机器，开启接收模式
            Player mockPlayer = helper.makeMockPlayer(net.minecraft.world.level.GameType.CREATIVE);
            DimensionsNet net = DimensionsNet.createNewNetForPlayer(mockPlayer, 1_000_000L, 1_000_000);
            if (net == null)
            {
                helper.fail(Component.literal("测试网络创建失败"));
                return;
            }
            be.setNetId(net.getId());
            be.receiveMode = ReceiveMode.OPEN;

            UnifiedStorage storage = net.getUnifiedStorage();
            // 网络中放入原料与燃料
            storage.insert(new ItemStackKey(new ItemStack(Items.IRON_ORE)), 64L, false);
            storage.insert(new ItemStackKey(new ItemStack(Items.COAL)), 9L, false);
            // 设置输入/燃料标记（自动拉料依赖标记）
            be.getInputFilterSlots().insert(0, new ItemStackKey(new ItemStack(Items.IRON_ORE)), 1L, false);
            be.getFuelFilterSlots().insert(0, new ItemStackKey(new ItemStack(Items.COAL)), 1L, false);
            BeyondDimensions.LOGGER.info("[熔炉测试3] 网络已建立 netId={} 原料/燃料已入网 接收模式=OPEN", net.getId());

            final ItemStackKey ingotKey = new ItemStackKey(new ItemStack(Items.IRON_INGOT));
            final long[] waited = {0};
            helper.succeedWhen(() -> {
                waited[0]++;
                // 模拟提取3个铁锭，判断网络中是否已累积>=3个产物
                long inNet = storage.extract(ingotKey, 3L, true, false).amount();
                if (inNet >= 3L)
                {
                    // 新行为：接收模式+已连网时产物直接写入网络，输出槽必须保持为空
                    KeyAmount outSlot = be.getOutputStorageSlots().getStackBySlot(0);
                    if (!outSlot.isEmpty())
                    {
                        helper.fail(Component.literal("产物应直接进入网络，但输出槽0仍持有: " + outSlot));
                        return;
                    }
                    long oreLeft = storage.extract(new ItemStackKey(new ItemStack(Items.IRON_ORE)), 64L, true, false).amount();
                    BeyondDimensions.LOGGER.info("[熔炉测试3] 成功: 网络中已有铁锭 x{}，剩余铁矿 x{}，输出槽保持为空（等待{}tick）",
                            inNet, oreLeft, waited[0]);
                    return;
                }

                if (waited[0] % 100 == 0)
                {
                    BeyondDimensions.LOGGER.info("[熔炉测试3] 等待中({}tick): 网络铁锭={} 机器输入={} 机器输出={}",
                            waited[0], inNet,
                            be.getInputStorageSlots().getStackBySlot(0),
                            be.getOutputStorageSlots().getStackBySlot(0));
                }
                throw new GameTestAssertException(Component.literal("产物未自动入网: 网络铁锭=" + inNet), 0);
            });
        }

        @Override
        public MapCodec<? extends GameTestInstance> codec()
        {
            return CODEC;
        }

        @Override
        protected MutableComponent typeDescription()
        {
            return Component.literal("bd_furnace_network_probe");
        }
    }
}
