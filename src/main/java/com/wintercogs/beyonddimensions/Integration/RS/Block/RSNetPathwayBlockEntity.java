package com.wintercogs.beyonddimensions.Integration.RS.Block;

import com.wintercogs.beyonddimensions.BlockEntity.Custom.NetedBlockEntity;
import com.wintercogs.beyonddimensions.BlockEntity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import java.util.concurrent.CopyOnWriteArrayList;

public class RSNetPathwayBlockEntity extends NetedBlockEntity
{
    // 给RS外部存储注册方块移除事件
    // 网络切换事件已经注册到父类的runable了
    private final CopyOnWriteArrayList<Runnable> onRemoveTasks = new CopyOnWriteArrayList<>();
    private volatile boolean removalFired = false;

    public RSNetPathwayBlockEntity(BlockPos pos, BlockState blockState)
    {
        super(ModBlockEntities.RS_NET_PATHWAY_BLOCK_ENTITY.get(), pos, blockState);
    }

    // 对外暴露的注册/注销 API
    public void addRemoveTask(Runnable r)
    {
        if (r != null) onRemoveTasks.add(r);
    }

    public void removeRemoveTask(Runnable r)
    {
        if (r != null) onRemoveTasks.remove(r);
    }

    private void fireRemoveTasksOnce()
    {
        if (removalFired) return;
        removalFired = true;
        for (Runnable r : onRemoveTasks)
        {
            try
            {
                r.run();
            }
            catch (Throwable t)
            {
            }
        }
        onRemoveTasks.clear();
    }


    @Override
    public void setRemoved()
    {
        super.setRemoved();
        fireRemoveTasksOnce();
    }

    @Override
    public void onChunkUnloaded()
    {
        super.onChunkUnloaded();
        fireRemoveTasksOnce();
    }

}