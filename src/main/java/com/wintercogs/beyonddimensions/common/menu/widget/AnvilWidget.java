package com.wintercogs.beyonddimensions.common.menu.widget;

import com.wintercogs.beyonddimensions.api.menu.workbench.StorageWorkbenchTypes;
import com.wintercogs.beyonddimensions.api.storage.handler.IStackHandler;
import com.wintercogs.beyonddimensions.common.menu.BDBaseMenu;
import dev.compixel.forge.sync.MenuAction;
import dev.compixel.forge.sync.MenuSync;
import dev.compixel.sync.state.SyncCodecs;
import net.minecraft.SharedConstants;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * 铁砧：要修理的物品、材料与产物槽，加上改名。经验花费同步给客户端
 */
public class AnvilWidget extends WorkstationWidget<AnvilWidget.Anvil>
{
    /**
     * 改名；名称的长度由原版限制
     */
    public static final MenuAction<BDBaseMenu, String> RENAME = MenuAction.of("bd_anvil_rename", SyncCodecs.STRING,
            (menu, player, name) -> {
                AnvilWidget anvil = menu.widget(AnvilWidget.class);
                if (anvil == null)
                    return false;
                anvil.rename(name);
                return true;
            });

    public AnvilWidget(BDBaseMenu menu, IStackHandler storage)
    {
        super(menu, StorageWorkbenchTypes.ANVIL, storage, new Anvil(menu.player.getInventory(), access(menu.player)), "anvil", 2);
        // 原版菜单只有这一个数据：经验花费
        addData(new DataSlot()
        {
            @Override
            public int get()
            {
                return vanilla.getCost();
            }

            @Override
            public void set(int value)
            {
                vanilla.setData(0, value);
            }
        });
    }

    /**
     * 经验花费，0 表示不能合成
     */
    public int cost()
    {
        return vanilla.getCost();
    }

    /**
     * 当前的名称；换了要修理的物品时回到它自己的名字
     */
    public String name()
    {
        return vanilla.name;
    }

    /**
     * 改名，产物随之更新；客户端先在本地改，再把 {@link #RENAME} 发给服务器
     */
    public void rename(String name)
    {
        vanilla.setItemName(name);
    }

    @Override
    public <M extends BDBaseMenu> void addActions(MenuSync<M> sync)
    {
        sync.action(RENAME);
    }

    /**
     * 原版铁砧，换了要修理的物品时把名字设回它自己的名字，与原版界面在放入物品时重填名称框一样；输入变了就通知部件
     */
    public static final class Anvil extends AnvilMenu implements Inputs
    {
        private ItemStack input = ItemStack.EMPTY;
        private String name = "";
        private Runnable inputsChanged = () -> {};

        Anvil(Inventory inventory, ContainerLevelAccess access)
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
            ItemStack current = getSlot(INPUT_SLOT).getItem();
            if (!ItemStack.isSameItemSameTags(current, input))
            {
                input = current.copy();
                setItemName(current.isEmpty() ? "" : current.getHoverName().getString());
            }
            super.slotsChanged(container);
            if (container == inputSlots)
                inputsChanged.run();
        }

        @Override
        public boolean setItemName(@NotNull String itemName)
        {
            boolean changed = super.setItemName(itemName);
            if (changed)
                name = SharedConstants.filterText(itemName);
            return changed;
        }
    }
}
