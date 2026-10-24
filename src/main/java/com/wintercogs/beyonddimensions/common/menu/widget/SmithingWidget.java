package com.wintercogs.beyonddimensions.common.menu.widget;

import com.wintercogs.beyonddimensions.api.menu.workbench.StorageWorkbenchTypes;
import com.wintercogs.beyonddimensions.api.storage.handler.IStackHandler;
import com.wintercogs.beyonddimensions.common.menu.BDBaseMenu;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SmithingMenu;
import org.jetbrains.annotations.NotNull;

/**
 * 锻造台：模板、基础物品与材料三个输入槽和产物槽
 */
public class SmithingWidget extends WorkstationWidget<SmithingWidget.Smithing>
{
    public SmithingWidget(BDBaseMenu menu, IStackHandler storage)
    {
        super(menu, StorageWorkbenchTypes.SMITHING, storage, new Smithing(menu.player.getInventory(), access(menu.player)), "smithing", 3);
    }

    /**
     * 原版锻造台，输入变了就通知部件
     */
    public static final class Smithing extends SmithingMenu implements Inputs
    {
        private Runnable inputsChanged = () -> {};

        Smithing(Inventory inventory, ContainerLevelAccess access)
        {
            super(-1, inventory, access);
        }

        @Override
        public void onInputsChanged(Runnable listener)
        {
            inputsChanged = listener;
        }

        @Override
        public void slotsChanged(@NotNull Container container)
        {
            super.slotsChanged(container);
            if (container == inputSlots)
                inputsChanged.run();
        }
    }
}
