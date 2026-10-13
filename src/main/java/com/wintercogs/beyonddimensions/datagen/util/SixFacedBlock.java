package com.wintercogs.beyonddimensions.datagen.util;

import com.wintercogs.beyonddimensions.common.block.FacingRule;
import com.wintercogs.beyonddimensions.common.machine.RelativeSide;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.generators.BlockModelBuilder;
import net.neoforged.neoforge.client.model.generators.ModelBuilder;
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
 * （{@link FacingRule#FREE} 的方块正面水平、顶面朝侧面时另用一个绕正面转过 90° 的模型，模型名末尾加 _spun）
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
        Map<String, ModelFile> spun = new HashMap<>();
        provider.orientedBlock(block,
                state -> models.computeIfAbsent(appearance.apply(state), this::model),
                state -> spun.computeIfAbsent(appearance.apply(state), this::spunModel));
        provider.simpleBlockItem(block, models.computeIfAbsent(appearance.apply(block.defaultBlockState()), this::model));
    }

    private String folder(String look)
    {
        return look.isEmpty() ? id : id + "/" + look;
    }

    private ResourceLocation texture(String look, RelativeSide side)
    {
        return shared.getOrDefault(side, provider.modLoc("block/" + folder(look) + "/" + lowercase(side)));
    }

    private ModelFile model(String look)
    {
        return provider.models().cube("block/" + folder(look),
                        texture(look, RelativeSide.BOTTOM), texture(look, RelativeSide.TOP),
                        texture(look, RelativeSide.FRONT), texture(look, RelativeSide.BACK),
                        texture(look, RelativeSide.LEFT), texture(look, RelativeSide.RIGHT))
                .texture("particle", texture(look, particle));
    }

    /**
     * 从正面看绕正面顺时针转过 90° 的模型：正面仍朝北，顶面转到西，左面转到上，各面贴图跟着转
     */
    private ModelFile spunModel(String look)
    {
        BlockModelBuilder model = provider.models().withExistingParent("block/" + folder(look) + "_spun", "minecraft:block/block")
                .texture("particle", texture(look, particle));
        for (RelativeSide side : RelativeSide.values())
            model.texture(lowercase(side), texture(look, side));
        model.element()
                .face(Direction.NORTH).texture("#front").cullface(Direction.NORTH).rotation(ModelBuilder.FaceRotation.CLOCKWISE_90).end()
                .face(Direction.SOUTH).texture("#back").cullface(Direction.SOUTH).rotation(ModelBuilder.FaceRotation.COUNTERCLOCKWISE_90).end()
                .face(Direction.WEST).texture("#top").cullface(Direction.WEST).rotation(ModelBuilder.FaceRotation.COUNTERCLOCKWISE_90).end()
                .face(Direction.DOWN).texture("#right").cullface(Direction.DOWN).rotation(ModelBuilder.FaceRotation.COUNTERCLOCKWISE_90).end()
                .face(Direction.EAST).texture("#bottom").cullface(Direction.EAST).rotation(ModelBuilder.FaceRotation.COUNTERCLOCKWISE_90).end()
                .face(Direction.UP).texture("#left").cullface(Direction.UP).rotation(ModelBuilder.FaceRotation.COUNTERCLOCKWISE_90).end()
                .end();
        return model;
    }
}
