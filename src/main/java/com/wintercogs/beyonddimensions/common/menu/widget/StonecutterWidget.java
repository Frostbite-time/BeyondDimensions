package com.wintercogs.beyonddimensions.common.menu.widget;

import com.wintercogs.beyonddimensions.api.menu.widget.BatchCraftWidget;
import com.wintercogs.beyonddimensions.api.menu.workbench.StorageWorkbenchTypes;
import com.wintercogs.beyonddimensions.api.storage.handler.IStackHandler;
import com.wintercogs.beyonddimensions.api.storage.key.impl.ItemStackKey;
import com.wintercogs.beyonddimensions.common.menu.BDBaseMenu;
import dev.compixel.forge.sync.MenuAction;
import dev.compixel.forge.sync.MenuSync;
import dev.compixel.sync.state.SyncCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.StonecutterMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 切石机：一个输入槽与产物槽，产物是从输入能切出的配方中选中的那个。选中的配方同步给客户端。
 * 取出产物时原料只剩最后一个的话先从存储补上，所以能像合成台一样连续制作
 */
public class StonecutterWidget extends WorkstationWidget<StonecutterWidget.Stonecutter> implements BatchCraftWidget
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
        DataSlot selected = addData(new DataSlot()
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
        // 服务端换了输入时原版清掉选中的配方：清掉的值立刻同步，同一刻再选中同一位置的配方（配方查看器填充）也会排在输入之后同步给客户端
        if (menu.player instanceof ServerPlayer)
        {
            int index = menu.dataSlots.indexOf(selected);
            vanilla.onSelectionReset(() -> menu.synchronizeDataSlotToRemote(index, -1));
        }
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

    @Override
    protected Slot menuResultSlot(Slot vanillaResult)
    {
        return new RefillingResultSlot(vanillaResult);
    }

    // 原料只剩最后一个时先从存储、再从背包补一个，这次切割就不会把原料取空，选中的配方也就留着
    private void refillInput(Player player)
    {
        Slot input = menu.slots.get(inputStart());
        ItemStack stack = input.getItem();
        if (stack.getCount() != 1 || stack.getMaxStackSize() < 2)
            return;
        int missing = CraftingWidget.extractFromStorage(storage, new ItemStackKey(stack), 1);
        if (missing > 0)
            missing = CraftingWidget.extractFromInventory(player.getInventory(), stack, missing);
        if (missing == 0)
            input.set(stack.copyWithCount(2));
    }

    /**
     * 产物槽：取出时先补充原料，再由原版的这一格消耗原料。客户端不预测消耗，等服务端同步
     */
    private final class RefillingResultSlot extends Slot
    {
        private final Slot vanillaResult;

        RefillingResultSlot(Slot vanillaResult)
        {
            super(vanillaResult.container, vanillaResult.getContainerSlot(), vanillaResult.x, vanillaResult.y);
            this.vanillaResult = vanillaResult;
        }

        @Override
        public boolean mayPlace(@NotNull ItemStack stack)
        {
            return false;
        }

        @Override
        public void onTake(@NotNull Player player, @NotNull ItemStack stack)
        {
            if (player.level().isClientSide())
                return;
            refillInput(player);
            vanillaResult.onTake(player, stack);
        }
    }

    /**
     * 原版切石机，输入变了、选中的配方因此清掉时通知部件
     */
    public static final class Stonecutter extends StonecutterMenu implements Inputs
    {
        private Runnable inputsChanged = () -> {
        };
        private Runnable selectionReset = () -> {
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

        /**
         * 输入换了物品、原版清掉选中的配方时调用 {@code listener}
         */
        void onSelectionReset(Runnable listener)
        {
            selectionReset = listener;
        }

        @Override
        public void slotsChanged(@NotNull Container container)
        {
            boolean picked = getSelectedRecipeIndex() >= 0;
            super.slotsChanged(container);
            if (picked && getSelectedRecipeIndex() < 0)
                selectionReset.run();
            if (container == this.container)
                inputsChanged.run();
        }
    }
}
