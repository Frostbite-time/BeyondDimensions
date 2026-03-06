package com.wintercogs.beyonddimensions.common.init;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.common.fluid.XpFluid;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@EventBusSubscriber(modid = BeyondDimensions.MODID, value = Dist.CLIENT)
public class BDFluids
{
    public static final DeferredRegister<FluidType> FLUID_TYPES = DeferredRegister.create(NeoForgeRegistries.FLUID_TYPES, BeyondDimensions.MODID);

    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(Registries.FLUID, BeyondDimensions.MODID);

    public static final List<FluidEntry> ALL = new ArrayList<>();

    public static final FluidEntry XP_FLUID = registerFluid(
            "xp_fluid",
            FluidType.Properties.create()
                    .lightLevel(10)
                    .density(800)
                    .viscosity(1500),
            0xFFFFFFFF, // tint是乘法，传入白色，保持原有纹理
            10,
            XpFluid.Source::new,
            XpFluid.Flowing::new
    );

    // 可传入具体的 Source/Flowing 类构造器
    public static <S extends BaseFlowingFluid.Source, F extends BaseFlowingFluid.Flowing>
    FluidEntry registerFluid(
            String name,
            FluidType.Properties typeProps,
            int argbTint,
            int lightlevel,
            Function<BaseFlowingFluid.Properties, S> sourceCtor,
            Function<BaseFlowingFluid.Properties, F> flowingCtor
    )
    {
        // 1) FluidType
        DeferredHolder<FluidType, FluidType> type = FLUID_TYPES.register(name, () -> new FluidType(typeProps));

        // 2) 解决循环依赖：propsRef 先占位，之后回填
        final BaseFlowingFluid.Properties[] propsRef = new BaseFlowingFluid.Properties[1];

        // 3) 流体（源/流动）
        DeferredHolder<Fluid, S> source =
                FLUIDS.register(name, () -> sourceCtor.apply(propsRef[0]));
        DeferredHolder<Fluid, F> flowing =
                FLUIDS.register("flowing_" + name, () -> flowingCtor.apply(propsRef[0]));

        // 4) 方块 + 桶
        DeferredHolder<Block, LiquidBlock> block =
                BDBlocks.BLOCKS.register(name, () ->
                        new LiquidBlock(source.get(),
                                BlockBehaviour.Properties.ofFullCopy(Blocks.WATER)
                                        .lightLevel(s -> lightlevel)));

        DeferredHolder<Item, Item> bucket =
                BDItems.ITEMS.register(name + "_bucket", () ->
                        new BucketItem(source.get(),
                                new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));

        // 5) 回填 BaseFlowingFluid.Properties
        propsRef[0] = new BaseFlowingFluid.Properties(type, source, flowing)
                .bucket(bucket)
                .block(block)
                .slopeFindDistance(4)
                .levelDecreasePerBlock(2);

        FluidEntry entry = new FluidEntry(name, type, source, flowing, block, bucket, argbTint);
        ALL.add(entry);
        return entry;
    }

    // 使用基础的 BaseFlowingFluid
    public static FluidEntry registerSimpleFluid(String name, FluidType.Properties typeProps, int argbTint, int lightlevel)
    {
        return registerFluid(
                name, typeProps, argbTint, lightlevel,
                BaseFlowingFluid.Source::new,
                BaseFlowingFluid.Flowing::new
        );
    }

    public static void register(IEventBus modBus)
    {
        FLUID_TYPES.register(modBus);
        FLUIDS.register(modBus);
    }

    public record FluidEntry(
            String name,
            DeferredHolder<FluidType, FluidType> type,
            DeferredHolder<Fluid, ? extends BaseFlowingFluid.Source> source,
            DeferredHolder<Fluid, ? extends BaseFlowingFluid.Flowing> flowing,
            DeferredHolder<Block, LiquidBlock> block,
            DeferredHolder<Item, Item> bucket,
            int argbTint
    )
    {
    }

    //注册每个 FluidType 的贴图 & 颜色（替代 initializeClient）
    @SubscribeEvent
    public static void onRegisterClientExtensions(RegisterClientExtensionsEvent event)
    {
        for (var e : BDFluids.ALL)
        {
            final ResourceLocation still = ResourceLocation.tryBuild(BeyondDimensions.MODID, "block/" + e.name() + "_still");
            final ResourceLocation flow = ResourceLocation.tryBuild(BeyondDimensions.MODID, "block/" + e.name() + "_flow");
            final int tint = e.argbTint();

            event.registerFluidType(new IClientFluidTypeExtensions()
            {
                @Override
                public ResourceLocation getStillTexture()
                {
                    return still;
                }

                @Override
                public ResourceLocation getFlowingTexture()
                {
                    return flow;
                }

                @Override
                public int getTintColor()
                {
                    return tint;
                }
            }, e.type().get());
        }
    }

    //渲染层（半透明）
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent evt)
    {
        evt.enqueueWork(() -> {
            for (var e : BDFluids.ALL)
            {
                net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(e.source().get(), net.minecraft.client.renderer.RenderType.translucent());
                net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(e.flowing().get(), net.minecraft.client.renderer.RenderType.translucent());
            }
        });
    }
}
