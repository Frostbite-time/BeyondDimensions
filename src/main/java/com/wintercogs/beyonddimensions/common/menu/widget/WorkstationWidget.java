package com.wintercogs.beyonddimensions.common.menu.widget;

import com.wintercogs.beyonddimensions.api.menu.widget.ClearableWidget;
import com.wintercogs.beyonddimensions.api.menu.widget.MenuWidget;
import com.wintercogs.beyonddimensions.api.menu.widget.RecipeFillWidget;
import com.wintercogs.beyonddimensions.api.menu.workbench.PlayerWorkbenches;
import com.wintercogs.beyonddimensions.api.storage.handler.IStackHandler;
import com.wintercogs.beyonddimensions.api.storage.key.IStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.impl.ItemStackKey;
import com.wintercogs.beyonddimensions.common.menu.BDBaseMenu;
import dev.compixel.slots.SlotTransferRoutes;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 嵌入一份原版工作台菜单的部件：输入槽与产物槽直接取自原版菜单，配方、消耗、经验与音效都按原版处理。
 * 原版菜单只当作逻辑使用，不会作为玩家的菜单打开。
 * 输入槽里的物品随时记进玩家的 {@link PlayerWorkbenches}：关闭界面时保留的话下次打开放回，否则按退回方向还给玩家
 *
 * @param <V> 原版菜单的类型
 */
public abstract class WorkstationWidget<V extends AbstractContainerMenu & WorkstationWidget.Inputs> extends MenuWidget
        implements ClearableWidget, RecipeFillWidget
{
    /**
     * 由原版菜单的子类实现：输入槽的内容变了就调用 {@code listener}
     */
    public interface Inputs
    {
        void onInputsChanged(Runnable listener);
    }

    protected final V vanilla;
    private final ResourceLocation workbench;
    private final IStackHandler storage;
    private final String group;
    private final int inputs;

    /**
     * @param workbench 工作台的 ID，保留的物品按它存放
     * @param group     Shift 转移的分组名，产物槽的分组在后面加 {@code _result}
     * @param inputs    原版菜单最前面的输入槽个数，紧接着的一格是产物槽
     */
    protected WorkstationWidget(BDBaseMenu menu, ResourceLocation workbench, IStackHandler storage, V vanilla, String group, int inputs)
    {
        super(menu);
        this.vanilla = vanilla;
        this.workbench = workbench;
        this.storage = storage;
        this.group = group;
        this.inputs = inputs;
        for (int i = 0; i <= inputs; i++)
            addSlot(vanilla.slots.get(i));
        // 服务端放回上次保留的物品，之后每次改动都记下来，断线或崩溃时也不会丢
        if (menu.player instanceof ServerPlayer)
        {
            NonNullList<ItemStack> kept = kept();
            for (int i = 0; i < inputs; i++)
            {
                if (!kept.get(i).isEmpty())
                    vanilla.slots.get(i).set(kept.get(i).copy());
            }
            vanilla.onInputsChanged(this::keepInputs);
        }
    }

    /**
     * 原版菜单的位置：服务端用玩家打开时的位置播放音效，客户端不需要
     */
    protected static ContainerLevelAccess access(Player player)
    {
        return player.level().isClientSide() ? ContainerLevelAccess.NULL : ContainerLevelAccess.create(player.level(), player.blockPosition());
    }

    /**
     * 第一个输入槽的编号
     */
    public int inputStart()
    {
        return start();
    }

    /**
     * 产物槽的编号
     */
    public int resultSlot()
    {
        return start() + inputs;
    }

    public boolean isResultSlot(int index)
    {
        return index == resultSlot();
    }

    @Override
    public void addQuickMoveRoutes(SlotTransferRoutes.Builder routes)
    {
        // 输入槽的物品送回存储；产物先进背包，放不下再进存储
        routes.group(group, inputStart(), resultSlot()).route(group, "storage");
        routes.group(group + "_result", resultSlot(), end()).route(group + "_result", "player", "storage");
    }

    @Override
    public boolean canTakeForPickAll(@NotNull Slot slot)
    {
        return !isResultSlot(slot.index);
    }

    @Override
    public void clearItems(boolean toStorageFirst)
    {
        returnInputs(toStorageFirst);
    }

    @Override
    public List<Slot> inputSlots()
    {
        return menu.slots.subList(inputStart(), resultSlot());
    }

    /**
     * 按原料填充输入槽：已有的物品先按退回方向清出去，每格的原料先从背包取，再从存储取；放不进这一格的原料不取
     */
    @Override
    public boolean fillRecipe(List<IStackKey<?>> keys, List<Long> amounts, boolean compress, @Nullable ResourceLocation recipe)
    {
        Player player = menu.player;
        // 请求来自客户端，先完整校验再触碰任何物品
        if (player.level().isClientSide() || player.containerMenu != menu || !player.isAlive() || player.isSpectator() || !menu.stillValid(player))
            return false;
        if (keys.size() != amounts.size() || keys.size() > inputs)
            return false;
        for (int i = 0; i < keys.size(); i++)
        {
            IStackKey<?> key = keys.get(i);
            Long need = amounts.get(i);
            if (key == null || need == null || need < 0 || need > 0 && !key.isEmpty() && !(key instanceof ItemStackKey))
                return false;
        }
        returnInputs(PlayerWorkbenches.of(player).returnToStorage());
        for (int i = 0; i < keys.size(); i++)
        {
            if (!(keys.get(i) instanceof ItemStackKey key) || amounts.get(i) <= 0)
                continue;
            Slot slot = menu.slots.get(inputStart() + i);
            ItemStack template = key.copyStack();
            if (!slot.mayPlace(template))
                continue;
            int need = (int) Math.min(slot.getMaxStackSize(template), amounts.get(i));
            int remaining = CraftingWidget.extractFromInventory(player.getInventory(), template, need);
            if (remaining > 0)
                remaining = CraftingWidget.extractFromStorage(storage, key, remaining);
            if (need > remaining)
                slot.set(key.copyStackWithCount(need - remaining));
        }
        filled(recipe);
        return true;
    }

    /**
     * 按配方填充之后调用，例如切石机选中这个配方
     */
    protected void filled(@Nullable ResourceLocation recipe)
    {
    }

    @Override
    public void removed(@NotNull Player player)
    {
        if (!(player instanceof ServerPlayer serverPlayer))
            return;
        // 保留或断线时物品已经记在玩家数据里，下次打开时放回
        PlayerWorkbenches workbenches = PlayerWorkbenches.of(player);
        if (!workbenches.keep() && !serverPlayer.hasDisconnected())
            returnInputs(workbenches.returnToStorage());
    }

    private NonNullList<ItemStack> kept()
    {
        return PlayerWorkbenches.of(menu.player).items(workbench, inputs);
    }

    private void keepInputs()
    {
        NonNullList<ItemStack> kept = kept();
        for (int i = 0; i < inputs; i++)
            kept.set(i, vanilla.slots.get(i).getItem().copy());
    }

    // 把输入槽里的物品还给玩家：toStorageFirst 时先退回存储，否则先退回背包
    private void returnInputs(boolean toStorageFirst)
    {
        if (!(menu.player instanceof ServerPlayer))
            return;
        for (int i = 0; i < inputs; i++)
        {
            Slot slot = menu.slots.get(inputStart() + i);
            ItemStack stack = slot.getItem();
            if (stack.isEmpty())
                continue;
            slot.set(ItemStack.EMPTY);
            CraftingWidget.returnStack(menu.player, storage, stack.copy(), toStorageFirst);
        }
    }
}
