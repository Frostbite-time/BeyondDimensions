package com.wintercogs.beyonddimensions.common.menu;

import com.wintercogs.beyonddimensions.api.menu.workbench.PlayerWorkbenches;
import com.wintercogs.beyonddimensions.api.storage.handler.IStackHandler;
import com.wintercogs.beyonddimensions.api.storage.handler.impl.AbstractUnorderedStackHandler;
import com.wintercogs.beyonddimensions.api.storage.handler.impl.UnorderedStackHandlerRemoveZero;
import com.wintercogs.beyonddimensions.common.menu.widget.CraftingWidget;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static com.wintercogs.beyonddimensions.common.init.BDMenus.Dimensions_Net_Terminal_Menu;

/**
 * 存储终端（方块或物品）打开的存储菜单，工作台里的物品是玩家自己的 {@link PlayerWorkbenches}
 */
public class DimensionsNetTerminalMenu extends DimensionsNetMenu
{
    private ItemStack terminalStack = null;
    private BlockPos entityPos = null;

    public DimensionsNetTerminalMenu(int id, Inventory playerInventory, @Nullable FriendlyByteBuf data)
    {
        this(id, playerInventory, new UnorderedStackHandlerRemoveZero(AbstractUnorderedStackHandler.UiTimestampPolicy.NONE), null, null, readWorkbenches(data));
    }

    public DimensionsNetTerminalMenu(int id, Inventory playerInventory, AbstractUnorderedStackHandler data, @Nullable ItemStack terminalItem, @Nullable BlockPos entityPos)
    {
        this(id, playerInventory, data, terminalItem, entityPos, null);
    }

    private DimensionsNetTerminalMenu(int id, Inventory playerInventory, AbstractUnorderedStackHandler data, @Nullable ItemStack terminalItem,
                                      @Nullable BlockPos entityPos, @Nullable List<ResourceLocation> workbenches)
    {
        super(Dimensions_Net_Terminal_Menu.get(), id, playerInventory, data, workbenches);
        if (!player.level().isClientSide)
        {
            this.terminalStack = terminalItem;
            this.entityPos = entityPos;
        }
    }

    /**
     * 旧版本的终端把合成格的物品存在自己身上，打开这样的终端时把物品还给玩家
     */
    public static void returnLegacyCraftItems(Player player, IStackHandler storage, Iterable<ItemStack> items)
    {
        for (ItemStack stack : items)
        {
            if (!stack.isEmpty())
                CraftingWidget.returnStack(player, storage, stack.copy(), true);
        }
    }

    /**
     * 终端谁拿到谁能用，不看玩家是不是网络成员；网络被删除、终端被拆除或物品不在时失效
     */
    @Override
    public boolean stillValid(@NotNull Player player)
    {
        if (player.level().isClientSide())
            return true;
        if (!networkExists())
            return false;
        if (entityPos != null)
        {
            BlockEntity be = player.level().getBlockEntity(entityPos);
            return be != null && !be.isRemoved();
        }
        else
        {
            return terminalStack != null && !terminalStack.isEmpty();
        }
    }
}
