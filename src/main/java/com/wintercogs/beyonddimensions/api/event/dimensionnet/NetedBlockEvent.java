package com.wintercogs.beyonddimensions.api.event.dimensionnet;

import com.wintercogs.beyonddimensions.common.block.NetedBlock;
import com.wintercogs.beyonddimensions.common.block.entity.NetedBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.Event;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * {@link NetedBlock} 及其子类与维度网络之间的绑定关系变更事件。
 * <p>
 * 事件仅在服务端派发。方块被替换或破坏时，会在方块实体被移除前派发解绑事件。
 */
public abstract class NetedBlockEvent extends Event
{
    private final int netId;
    private final @NotNull ServerLevel level;
    private final @NotNull BlockPos pos;
    private final @NotNull BlockState blockState;
    private final @NotNull NetedBlock block;
    private final @NotNull NetedBlockEntity blockEntity;

    protected NetedBlockEvent(int netId, @NotNull ServerLevel level,
                              @NotNull BlockPos pos, @NotNull BlockState blockState,
                              @NotNull NetedBlock block, @NotNull NetedBlockEntity blockEntity)
    {
        this.netId = netId;
        this.level = Objects.requireNonNull(level);
        this.pos = Objects.requireNonNull(pos).immutable();
        this.blockState = Objects.requireNonNull(blockState);
        this.block = Objects.requireNonNull(block);
        this.blockEntity = Objects.requireNonNull(blockEntity);
    }

    public int getNetId()
    {
        return netId;
    }

    public @NotNull ServerLevel getLevel()
    {
        return level;
    }

    public @NotNull BlockPos getPos()
    {
        return pos;
    }

    public @NotNull BlockState getBlockState()
    {
        return blockState;
    }

    public @NotNull NetedBlock getBlock()
    {
        return block;
    }

    public @NotNull NetedBlockEntity getBlockEntity()
    {
        return blockEntity;
    }

    /** 方块完成绑定后派发。 */
    public static final class Bound extends NetedBlockEvent
    {
        public Bound(int netId, @NotNull ServerLevel level,
                     @NotNull BlockPos pos, @NotNull BlockState blockState,
                     @NotNull NetedBlock block, @NotNull NetedBlockEntity blockEntity)
        {
            super(netId, level, pos, blockState, block, blockEntity);
        }
    }

    /** 方块完成主动解绑或因方块被移除而解绑后派发。 */
    public static final class Unbound extends NetedBlockEvent
    {
        public Unbound(int netId, @NotNull ServerLevel level,
                       @NotNull BlockPos pos, @NotNull BlockState blockState,
                       @NotNull NetedBlock block, @NotNull NetedBlockEntity blockEntity)
        {
            super(netId, level, pos, blockState, block, blockEntity);
        }
    }
}
