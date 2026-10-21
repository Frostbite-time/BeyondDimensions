package com.wintercogs.beyonddimensions.common.menu;

import com.wintercogs.beyonddimensions.api.storage.handler.IStackHandler;
import com.wintercogs.beyonddimensions.api.storage.handler.impl.AbstractUnorderedStackHandler;
import com.wintercogs.beyonddimensions.api.storage.handler.impl.UnorderedStackHandlerRemoveZero;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.wintercogs.beyonddimensions.common.init.BDMenus.Dimensions_Craft_Menu_Terminal;

/**
 * 存储终端（方块或物品）打开的合成菜单，合成格是玩家自己的 {@link PlayerCraftingGrid}
 */
public class DimensionsCraftMenuTerminal extends DimensionsCraftMenu
{
    private ItemStack terminalStack = null;
    private BlockPos entityPos = null;

    public DimensionsCraftMenuTerminal(int id, Inventory playerInventory, FriendlyByteBuf data)
    {
        this(id, playerInventory, new UnorderedStackHandlerRemoveZero(AbstractUnorderedStackHandler.UiTimestampPolicy.NONE), null, null);
    }

    public DimensionsCraftMenuTerminal(int id, Inventory playerInventory, AbstractUnorderedStackHandler data, @Nullable ItemStack terminalItem, @Nullable BlockPos entityPos)
    {
        super(Dimensions_Craft_Menu_Terminal.get(), id, playerInventory, data);
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
                returnStack(player, storage, stack.copy(), true);
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
