package com.wintercogs.beyonddimensions.datagen.util;

import com.wintercogs.beyonddimensions.common.block.FacingRule;
import com.wintercogs.beyonddimensions.common.machine.RelativeSide;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.generators.ModelFile;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static com.wintercogs.beyonddimensions.api.machine.MachineSetting.lowercase;

/**
 * 六面完整方块的模型、方块状态与物品模型，每个面一张贴图，资源包可以分别替换：
 * <pre>
 * textures/block/&lt;方块 id&gt;/&lt;面&gt;.png          外观不随状态变化
 * textures/block/&lt;方块 id&gt;/&lt;外观&gt;/&lt;面&gt;.png    外观随状态变化时每种外观一套，例如 on 与 off
 * </pre>
 * 面名是 front、back、left、right、top、bottom，相对方块的朝向而言（见 {@link RelativeSide}），没有朝向的方块正面朝北。
 * 模型按正面朝北、顶面朝上生成，方块状态再按方块的朝向转动，所以方块换了 {@link FacingRule} 也不用改这里
 * <pre>{@code
 * sixFaced(BDBlocks.NET_INTERFACE).build();
 * sixFaced(BDBlocks.NET_FURNACE_BLOCK).appearance(state -> state.getValue(BaseNetFurnaceBlock.LIT) ? "on" : "off").build();
 * }</pre>
 */
public final class SixFacedBlock
{
    private final BDBlockStateProvider provider;
    private final Block block;
    private final String id;
    private final Map<RelativeSide, ResourceLocation> shared = new EnumMap<>(RelativeSide.class);
    private Function<BlockState, String> appearance = state -> "";
    private RelativeSide particle = RelativeSide.FRONT;

    SixFacedBlock(BDBlockStateProvider provider, Block block)
    {
        this.provider = provider;
        this.block = block;
        this.id = BuiltInRegistries.BLOCK.getKey(block).getPath();
    }

    /**
     * 外观随方块状态变化：name 给出状态对应的外观名，每个外观一套贴图与一个模型
     */
    public SixFacedBlock appearance(Function<BlockState, String> name)
    {
        this.appearance = name;
        return this;
    }

    /**
     * 这一面在所有外观下都用 texture，例如与别的方块共用一张贴图
     */
    public SixFacedBlock face(RelativeSide side, ResourceLocation texture)
    {
        shared.put(side, texture);
        return this;
    }

    /**
     * 破坏粒子用哪一面的贴图，默认是正面
     */
    public SixFacedBlock particle(RelativeSide side)
    {
        this.particle = side;
        return this;
    }

    public void build()
    {
        Map<String, ModelFile> models = new HashMap<>();
        Function<String, ModelFile> model = look -> models.computeIfAbsent(look, this::model);
        provider.orientedBlock(block, state -> model.apply(appearance.apply(state)));
        provider.simpleBlockItem(block, model.apply(appearance.apply(block.defaultBlockState())));
    }

    private ModelFile model(String look)
    {
        String folder = look.isEmpty() ? id : id + "/" + look;
        Function<RelativeSide, ResourceLocation> texture =
                side -> shared.getOrDefault(side, provider.modLoc("block/" + folder + "/" + lowercase(side)));
        return provider.models().cube("block/" + folder,
                        texture.apply(RelativeSide.BOTTOM), texture.apply(RelativeSide.TOP),
                        texture.apply(RelativeSide.FRONT), texture.apply(RelativeSide.BACK),
                        texture.apply(RelativeSide.LEFT), texture.apply(RelativeSide.RIGHT))
                .texture("particle", texture.apply(particle));
    }
}
