package com.wintercogs.beyonddimensions.common.init;

import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import com.wintercogs.beyonddimensions.common.fluid.XpFluid;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public class BDFluids
{
    public static final DeferredRegister<FluidType> FLUID_TYPES = DeferredRegister.create(ForgeRegistries.Keys.FLUID_TYPES, BDConstants.MODID);

    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(ForgeRegistries.Keys.FLUIDS, BDConstants.MODID);

    public static final List<FluidEntry<ForgeFlowingFluid.Source, ForgeFlowingFluid.Flowing>> ALL = new ArrayList<>();

    public static final FluidEntry<ForgeFlowingFluid.Source, ForgeFlowingFluid.Flowing> XP_FLUID = registerFluid(
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
    public static <S extends ForgeFlowingFluid.Source, F extends ForgeFlowingFluid.Flowing>
    FluidEntry<ForgeFlowingFluid.Source, ForgeFlowingFluid.Flowing> registerFluid(
            String name,
            FluidType.Properties typeProps,
            int argbTint,
            int lightlevel,
            Function<ForgeFlowingFluid.Properties, S> sourceCtor,
            Function<ForgeFlowingFluid.Properties, F> flowingCtor
    )
    {
        // 1) FluidType
        RegistryObject<FluidType> type = FLUID_TYPES.register(name, () -> new FluidType(typeProps)
        {
            @Override
            public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer)
            {
                super.initializeClient(consumer);
                final ResourceLocation still = ResourceLocation.tryBuild(BDConstants.MODID, "block/" + name + "_still");
                final ResourceLocation flow = ResourceLocation.tryBuild(BDConstants.MODID, "block/" + name + "_flow");
                final int tint = argbTint;
                consumer.accept(new IClientFluidTypeExtensions()
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
                });
            }
        });

        // 2) 解决循环依赖：propsRef 先占位，之后回填
        final ForgeFlowingFluid.Properties[] propsRef = new ForgeFlowingFluid.Properties[1];

        // 3) 流体（源/流动）
        RegistryObject<S> source =
                FLUIDS.register(name, () -> sourceCtor.apply(propsRef[0]));
        RegistryObject<F> flowing =
                FLUIDS.register("flowing_" + name, () -> flowingCtor.apply(propsRef[0]));

        // 4) 方块 + 桶
        RegistryObject<LiquidBlock> block =
                BDBlocks.BLOCKS.register(name, () ->
                        new LiquidBlock(source.get(),
                                BlockBehaviour.Properties.copy(Blocks.WATER)
                                        .lightLevel(s -> lightlevel)));

        RegistryObject<Item> bucket =
                BDItems.ITEMS.register(name + "_bucket", () ->
                        new BucketItem(source.get(),
                                new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));

        // 5) 回填 ForgeFlowingFluid.Properties
        propsRef[0] = new ForgeFlowingFluid.Properties(type, source, flowing)
                .bucket(bucket)
                .block(block)
                .slopeFindDistance(4)
                .levelDecreasePerBlock(2);

        FluidEntry entry = new FluidEntry(name, type, source, flowing, block, bucket, argbTint);
        ALL.add(entry);
        return entry;
    }

    // 使用基础的 BaseFlowingFluid
    public static FluidEntry<ForgeFlowingFluid.Source, ForgeFlowingFluid.Flowing> registerSimpleFluid(String name, FluidType.Properties typeProps, int argbTint, int lightlevel)
    {
        return registerFluid(
                name, typeProps, argbTint, lightlevel,
                ForgeFlowingFluid.Source::new,
                ForgeFlowingFluid.Flowing::new
        );
    }

    public static void register(IEventBus modBus)
    {
        FLUID_TYPES.register(modBus);
        FLUIDS.register(modBus);
    }

    public record FluidEntry<S extends ForgeFlowingFluid.Source, F extends ForgeFlowingFluid.Flowing>(
            String name,
            RegistryObject<FluidType> type,
            RegistryObject<S> source,
            RegistryObject<F> flowing,
            RegistryObject<LiquidBlock> block,
            RegistryObject<Item> bucket,
            int argbTint
    )
    {
    }

    @Mod.EventBusSubscriber(modid = BDConstants.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class ClientOnly
    {
        // IClientFluidTypeExtensions在注册type的时候直接挂上

        // 渲染层（半透明）
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent evt)
        {
            evt.enqueueWork(() -> {
                for (var e : BDFluids.ALL)
                {
                    ItemBlockRenderTypes.setRenderLayer((Fluid) e.source().get(), RenderType.translucent());
                    ItemBlockRenderTypes.setRenderLayer((Fluid) e.flowing().get(), RenderType.translucent());
                }
            });
        }
    }
}
