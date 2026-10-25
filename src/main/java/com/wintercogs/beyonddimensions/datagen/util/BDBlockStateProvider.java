package com.wintercogs.beyonddimensions.datagen.util;

import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import com.wintercogs.beyonddimensions.common.block.orientation.FacingRule;
import com.wintercogs.beyonddimensions.common.block.orientation.Orientation;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.core.Direction;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.client.model.generators.ConfiguredModel;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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
    protected SixFacedBlock sixFaced(RegistryObject<? extends Block> block)
    {
        return new SixFacedBlock(this, block.get());
    }

    /**
     * 每个方块状态用 model 给出的模型，并按方块的朝向转动；模型要做成正面朝北、顶面朝上
     */
    protected void orientedBlock(Block block, Function<BlockState, ModelFile> model)
    {
        orientedBlock(block, model, null);
    }

    /**
     * 同上；方块状态只能绕 x、y 轴转，正面水平、顶面朝侧面的朝向转不出来，这些状态用 spun 给出的模型：
     * 从正面看已绕正面顺时针转过 90°，正面仍朝北、顶面朝西
     */
    protected void orientedBlock(Block block, Function<BlockState, ModelFile> model, @Nullable Function<BlockState, ModelFile> spun)
    {
        getVariantBuilder(block).forAllStates(state -> {
            Pose pose = pose(FacingRule.orientation(state));
            if (pose.spun() && spun == null)
                throw new IllegalStateException(state + " needs a model turned around its front, which only six-faced blocks generate");
            return ConfiguredModel.builder()
                    .modelFile(pose.spun() ? spun.apply(state) : model.apply(state))
                    .rotationX(pose.x())
                    .rotationY(pose.y())
                    .build();
        });
    }

    /**
     * 方块状态里模型的转法：是否用绕正面转过的模型，再绕 x、y 轴转多少度
     */
    private record Pose(boolean spun, int x, int y)
    {
    }

    private static Pose pose(Orientation orientation)
    {
        for (boolean spun : new boolean[]{false, true})
        {
            Direction top = spun ? Direction.WEST : Direction.UP;
            for (int x = 0; x < 360; x += 90)
                for (int y = 0; y < 360; y += 90)
                {
                    Matrix4f matrix = BlockModelRotation.by(x, y).getRotation().getMatrix();
                    if (Direction.rotate(matrix, Direction.NORTH) == orientation.front() && Direction.rotate(matrix, top) == orientation.top())
                        return new Pose(spun, x, y);
                }
        }
        throw new IllegalStateException("No block model rotation turns north and up into " + orientation);
    }
}
