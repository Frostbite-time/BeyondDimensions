package com.wintercogs.beyonddimensions.BlockEntity.Custom;

import com.wintercogs.beyonddimensions.Api.DataBase.DimensionsNet;
import com.wintercogs.beyonddimensions.Menu.DimensionsCraftMenuTerminal;
import com.wintercogs.beyonddimensions.common.init.BDBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NetTerminalBlockEntity extends NetedBlockEntity implements MenuProvider
{

    private final NonNullList<ItemStack> craftItems = NonNullList.withSize(9, ItemStack.EMPTY);

    public NetTerminalBlockEntity(BlockPos pos, BlockState blockState)
    {
        super(BDBlockEntities.NET_TERMINAL_BLOCK_ENTITY.get(), pos, blockState);
    }

    @Override
    public @NotNull Component getDisplayName()
    {
        return Component.translatable("menu.title.beyonddimensions.dimensionnetmenu");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, @NotNull Inventory inventory, @NotNull Player player)
    {
        DimensionsNet net = getNet();
        if (net != null)
        {
            // 在服务端中craftItems作为直接引用传递。保证为同一个列表
            // 而后，craftItems会在Menu被包装，并通过Menu的包装类完成网络同步
            // 最后，利用方块实体进行持久保存
            return new DimensionsCraftMenuTerminal(containerId, inventory, net.getUnifiedStorage(), craftItems, null, this.getBlockPos());
        }
        return null;
    }

    @Override
    protected void loadAdditional(@NotNull ValueInput input)
    {
        super.loadAdditional(input);
        int i = 0;
        for (ItemStack stack : input.listOrEmpty("craft_items", ItemStack.OPTIONAL_CODEC))
        {
            if (i >= craftItems.size())
            {
                break;
            }
            craftItems.set(i, stack);
            i++;
        }
        for (; i < craftItems.size(); i++)
        {
            craftItems.set(i, ItemStack.EMPTY);
        }
    }

    @Override
    protected void saveAdditional(@NotNull ValueOutput output)
    {
        super.saveAdditional(output);
        ValueOutput.TypedOutputList<ItemStack> list = output.list("craft_items", ItemStack.OPTIONAL_CODEC);
        for (ItemStack stack : craftItems)
        {
            list.add(stack);
        }
    }

    public void dropContent()
    {
        for (ItemStack stack : craftItems)
        {
            if (!stack.isEmpty())
            {
                Block.popResource(level, getBlockPos(), stack.copy());
            }
        }
    }
}
