package com.wintercogs.beyonddimensions.common.menu;

import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import com.wintercogs.beyonddimensions.api.storage.handler.IStackHandler;
import com.wintercogs.beyonddimensions.api.storage.handler.impl.AbstractUnorderedStackHandler;
import com.wintercogs.beyonddimensions.api.storage.handler.impl.UnorderedStackHandlerRemoveZero;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * 存储终端（方块或物品）打开的合成菜单。合成格与 O 键打开的合成一样是玩家自己的 {@link PlayerCraftingGrid}，终端本身不保存物品；
 * 这个菜单只负责在终端被拆除或物品不在手上时失效
 */
public class DimensionsCraftMenuTerminal extends DimensionsCraftMenu
{
    private ItemStack terminalStack = null;
    private BlockPos entityPos = null;

    // 构建注册用的信息
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(Registries.MENU, BDConstants.MODID);
    public static final Supplier<MenuType<DimensionsCraftMenuTerminal>> Dimensions_Craft_Menu_Terminal = MENU_TYPES.register("dimensions_craft_menu_terminal", () -> IMenuTypeExtension.create(DimensionsCraftMenuTerminal::new));

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
     * 旧版本的终端把合成格的物品存在自己身上。打开这样的终端时把物品还给玩家：依次送回存储、玩家背包，放不下的掉在玩家脚下
     */
    public static void returnLegacyCraftItems(Player player, IStackHandler storage, Iterable<ItemStack> items)
    {
        for (ItemStack stack : items)
        {
            if (!stack.isEmpty())
                returnStack(player, storage, stack.copy(), true);
        }
    }

    @Override
    public boolean stillValid(@NotNull Player player)
    {
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
