package com.wintercogs.beyonddimensions.datagen.util;

import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import com.wintercogs.beyonddimensions.common.block.FacingRule;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.core.Direction;
import net.minecraft.core.FrontAndTop;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.registries.DeferredBlock;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

import java.util.function.Function;

public abstract class BDBlockStateProvider extends BlockStateProvider
{
    public BDBlockStateProvider(PackOutput output, ExistingFileHelper exFileHelper)
    {
        super(output, BDConstants.MODID, exFileHelper);
    }

    @Override
    public abstract @NotNull String getName();

    /**
     * 六面完整方块，见 {@link SixFacedBlock}；设置完调用 build
     */
    protected SixFacedBlock sixFaced(DeferredBlock<?> block)
    {
        return new SixFacedBlock(this, block.get());
    }

    /**
     * 每个方块状态用 model 给出的模型，并按方块的朝向转动；模型要做成正面朝北、顶面朝上
     */
    protected void orientedBlock(Block block, Function<BlockState, ModelFile> model)
    {
        getVariantBuilder(block).forAllStates(state -> {
            int[] rotation = rotation(FacingRule.orientation(state));
            return ConfiguredModel.builder()
                    .modelFile(model.apply(state))
                    .rotationX(rotation[0])
                    .rotationY(rotation[1])
                    .build();
        });
    }

    /**
     * 把正面朝北、顶面朝上的模型转到 orientation 的 x 与 y 旋转角
     */
    private static int[] rotation(FrontAndTop orientation)
    {
        for (int x = 0; x < 360; x += 90)
            for (int y = 0; y < 360; y += 90)
            {
                Matrix4f matrix = BlockModelRotation.by(x, y).getRotation().getMatrix();
                if (Direction.rotate(matrix, Direction.NORTH) == orientation.front() && Direction.rotate(matrix, Direction.UP) == orientation.top())
                    return new int[]{x, y};
            }
        throw new IllegalStateException("No block model rotation turns north and up into " + orientation);
    }
}
