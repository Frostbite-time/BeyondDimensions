package com.wintercogs.beyonddimensions.common.menu.widget;

import com.wintercogs.beyonddimensions.api.menu.workbench.StorageWorkbenchTypes;
import com.wintercogs.beyonddimensions.api.storage.handler.IStackHandler;
import com.wintercogs.beyonddimensions.common.menu.BDBaseMenu;
import dev.compixel.forge.sync.MenuAction;
import dev.compixel.forge.sync.MenuSync;
import dev.compixel.sync.state.SyncCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.StonecutterMenu;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 切石机：一个输入槽与产物槽，产物是从输入能切出的配方中选中的那个。选中的配方同步给客户端
 */
public class StonecutterWidget extends WorkstationWidget<StonecutterWidget.Stonecutter>
{
    /**
     * 选中第几个配方
     */
    public static final MenuAction<BDBaseMenu, Integer> SELECT = MenuAction.of("bd_stonecutter_select", SyncCodecs.INT,
            (menu, player, index) -> {
                StonecutterWidget stonecutter = menu.widget(StonecutterWidget.class);
                if (stonecutter == null)
                    return false;
                stonecutter.select(player, index);
                return true;
            });

    public StonecutterWidget(BDBaseMenu menu, IStackHandler storage)
    {
        super(menu, StorageWorkbenchTypes.STONECUTTER, storage, new Stonecutter(menu.player.getInventory(), access(menu.player)), "stonecutter", 1);
        // 原版菜单只有这一个数据：选中的配方
        addData(new DataSlot()
        {
            @Override
            public int get()
            {
                return vanilla.getSelectedRecipeIndex();
            }

            @Override
            public void set(int value)
            {
                vanilla.setData(0, value);
            }
        });
    }

    /**
     * 从当前输入能切出的配方；客户端与服务端各自按输入算出
     */
    public List<StonecutterRecipe> recipes()
    {
        return vanilla.getRecipes();
    }

    /**
     * 选中的配方在 {@link #recipes()} 里的位置，没有选中时为 -1
     */
    public int selected()
    {
        return vanilla.getSelectedRecipeIndex();
    }

    /**
     * 选中配方，产物槽随之更新；客户端先在本地选中，再把 {@link #SELECT} 发给服务器
     */
    public void select(Player player, int index)
    {
        vanilla.clickMenuButton(player, index);
    }

    // 配方查看器转移的是哪个配方，填好输入后就选中它
    @Override
    protected void filled(@Nullable ResourceLocation recipe)
    {
        List<StonecutterRecipe> recipes = recipes();
        for (int i = 0; i < recipes.size(); i++)
        {
            if (recipes.get(i).getId().equals(recipe))
            {
                select(menu.player, i);
                return;
            }
        }
    }

    @Override
    public <M extends BDBaseMenu> void addActions(MenuSync<M> sync)
    {
        sync.action(SELECT);
    }

    /**
     * 原版切石机，输入变了就通知部件
     */
    public static final class Stonecutter extends StonecutterMenu implements Inputs
    {
        private Runnable inputsChanged = () -> {
        };

        Stonecutter(Inventory inventory, ContainerLevelAccess access)
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
            if (container == this.container)
                inputsChanged.run();
        }
    }
}
