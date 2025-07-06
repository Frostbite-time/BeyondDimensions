package com.wintercogs.beyonddimensions.Menu;

import com.wintercogs.beyonddimensions.Api.DataBase.DimensionsNet;
import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.DataComponents.Custom.ItemStackContents;
import com.wintercogs.beyonddimensions.DataComponents.ModDataComponents;
import com.wintercogs.beyonddimensions.Item.Custom.NetTerminalItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public class DimensionsCraftMenuTerminal extends DimensionsCraftMenu
{
    private ItemStack terminalStack = null;
    private BlockPos entityPos = null;

    // 构建注册用的信息
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(Registries.MENU, BeyondDimensions.MODID);
    public static final Supplier<MenuType<DimensionsCraftMenuTerminal>> Dimensions_Craft_Menu_Terminal = MENU_TYPES.register("dimensions_craft_menu_terminal", () -> IMenuTypeExtension.create(DimensionsCraftMenuTerminal::new));

    public DimensionsCraftMenuTerminal(int id, Inventory playerInventory, FriendlyByteBuf data)
    {
        this(id, playerInventory,  new DimensionsNet(true), null, null, null);
    }

    public DimensionsCraftMenuTerminal(int id, Inventory playerInventory, DimensionsNet data, NonNullList<ItemStack> craftItems, @Nullable ItemStack terminalItem, @Nullable BlockPos entityPos)
    {
        super(Dimensions_Craft_Menu_Terminal.get(), id,playerInventory,data, craftItems, entityPos);
        if(!player.level().isClientSide)
        {
            this.terminalStack = terminalItem;
            this.entityPos = entityPos;
        }
    }

    @Override
    protected void initCraftSlots(Inventory playerInventory, @Nullable TransientCraftingContainer craftSlots)
    {
        super.initCraftSlots(playerInventory, craftSlots);
        // 父函数处理完毕后更新一次结果槽
        DimensionsCraftMenu.slotChangedCraftingGrid(this,player.level(),player,craftSlots,resultSlots,resultSlotIndex);
    }

    @Override
    public void removed(Player player)
    {
        // 处理光标物品
        if (player instanceof ServerPlayer) {
            ItemStack itemstack = this.getCarried();
            if (!itemstack.isEmpty()) {
                if (player.isAlive() && !((ServerPlayer)player).hasDisconnected()) {
                    player.getInventory().placeItemBackInInventory(itemstack);
                } else {
                    player.drop(itemstack, false);
                }

                this.setCarried(ItemStack.EMPTY);
            }
        }

        if(player instanceof ServerPlayer)
        {
            // 处理合成槽物品
            NonNullList<ItemStack> nonNullList = NonNullList.withSize(9, ItemStack.EMPTY);
            for (int i = 0; i < craftSlots.getItems().size(); i++) {
                ItemStack stack = craftSlots.getItems().get(i);
                nonNullList.set(i, stack);
            }
            if(terminalStack != null && terminalStack.getItem() instanceof NetTerminalItem)
                terminalStack.set(ModDataComponents.CRAFT_SLOTS, new ItemStackContents(nonNullList));
        }

    }

    @Override
    public boolean stillValid(Player player)
    {
        if(entityPos != null)
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
