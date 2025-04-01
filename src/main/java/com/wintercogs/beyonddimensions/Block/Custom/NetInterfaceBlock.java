package com.wintercogs.beyonddimensions.Block.Custom;


import com.wintercogs.beyonddimensions.BlockEntity.Custom.NetInterfaceBlockEntity;
import com.wintercogs.beyonddimensions.Gui.NetInterfaceGUI;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public class NetInterfaceBlock extends NetedBlock
{


    public NetInterfaceBlock(Material materialIn)
    {
        super(materialIn);
    }

    @Override
    public boolean hasTileEntity(IBlockState state)
    {
        return true;
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(World world, IBlockState state)
    {
        return new NetInterfaceBlockEntity();
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ)
    {
        super.onBlockActivated(worldIn, pos, state, player, hand, facing, hitX, hitY, hitZ);
        if(!worldIn.isRemote&&!player.isSneaking())
        {
            NetInterfaceGUI.factory.open((EntityPlayerMP) player,pos.getX(),pos.getY(),pos.getZ());
        }
        return true;
    }



    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        // 触发红石信号更新
        world.notifyNeighborsOfStateChange(pos, this, false);

        // 调用父类逻辑（例如清理 TileEntity）
        super.breakBlock(world, pos, state);
    }


    @Override
    public void onNeighborChange(IBlockAccess world, BlockPos pos, BlockPos neighbor) {
        super.onNeighborChange(world, pos, neighbor);
        if (world.getTileEntity(pos) instanceof NetInterfaceBlockEntity) {
            NetInterfaceBlockEntity blockEntity = (NetInterfaceBlockEntity) world.getTileEntity(pos);
            blockEntity.markDirty(); // 或自定义标记更新方法
        }
    }


}
