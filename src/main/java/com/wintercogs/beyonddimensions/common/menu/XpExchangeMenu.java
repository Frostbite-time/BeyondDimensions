package com.wintercogs.beyonddimensions.common.menu;

import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import com.wintercogs.beyonddimensions.client.gui.CommonTextures;
import com.wintercogs.beyonddimensions.common.init.BDDataComponents;
import com.wintercogs.beyonddimensions.common.item.XpExchangeSettings;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public class XpExchangeMenu extends BDBaseMenu
{
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(Registries.MENU, BDConstants.MODID);
    public static final Supplier<MenuType<XpExchangeMenu>> XP_EXCHANGE_MENU = MENU_TYPES.register("xp_exchange_menu", () -> IMenuTypeExtension.create(XpExchangeMenu::new));

    private static final int invSlotStartY = CommonTextures.TOP_BASE_COMMON_HEIGHT + CommonTextures.COMMON_CONNECTION_HEIGHT * 5 + 7;

    public final ItemStack menuStack;

    private boolean lastKeepMode;
    private int lastTargetLevel;

    public XpExchangeMenu(int id, Inventory playerInventory, FriendlyByteBuf data)
    {
        this(id, playerInventory, playerInventory.player.getItemInHand(data.readEnum(InteractionHand.class)));
    }

    public XpExchangeMenu(int containerId, Inventory playerInventory, ItemStack menuStack)
    {
        super(XP_EXCHANGE_MENU.get(), containerId, playerInventory);
        this.menuStack = menuStack;
        XpExchangeSettings.ensureComponents(this.menuStack);
        addPlayerInv(playerInventory);
    }

    private void addPlayerInv(Inventory playerInventory)
    {
        inventoryStartIndex = slots.size();
        for (int row = 0; row < 3; ++row)
        {
            for (int col = 0; col < 9; ++col)
            {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, invSlotStartY + row * 18));
            }
        }
        for (int col = 0; col < 9; ++col)
        {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 4 + invSlotStartY + 3 * 18));
        }
        inventoryEndIndex = slots.size();
    }

    @Override
    public boolean stillValid(@NotNull Player player)
    {
        return menuStack != null && !menuStack.isEmpty();
    }

    @Override
    protected boolean shouldSendQuickData()
    {
        XpExchangeSettings.ensureComponents(menuStack);
        boolean currentKeepMode = menuStack.getOrDefault(BDDataComponents.XP_NET_KEEP_MODE, false);
        int currentTargetLevel = XpExchangeSettings.getTargetLevel(menuStack);
        boolean result = super.shouldSendQuickData()
                || lastKeepMode != currentKeepMode
                || lastTargetLevel != currentTargetLevel;

        if (result)
        {
            lastKeepMode = currentKeepMode;
            lastTargetLevel = currentTargetLevel;
        }

        return result;
    }

    @Override
    protected void writeQuickDataTag(CompoundTag tag)
    {
        super.writeQuickDataTag(tag);
        tag.putBoolean("xp_keep_mode", menuStack.getOrDefault(BDDataComponents.XP_NET_KEEP_MODE, false));
        tag.putInt("xp_target_level", XpExchangeSettings.getTargetLevel(menuStack));
    }

    @Override
    public void readQuickDataTag(CompoundTag tag)
    {
        super.readQuickDataTag(tag);
        XpExchangeSettings.ensureComponents(menuStack);

        if (tag.contains("xp_keep_mode"))
            menuStack.set(BDDataComponents.XP_NET_KEEP_MODE, tag.getBoolean("xp_keep_mode"));

        if (tag.contains("xp_target_level"))
            XpExchangeSettings.setTargetLevel(menuStack, tag.getInt("xp_target_level"));
    }
}
