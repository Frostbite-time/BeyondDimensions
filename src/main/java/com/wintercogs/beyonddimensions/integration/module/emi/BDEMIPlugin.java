package com.wintercogs.beyonddimensions.integration.module.emi;

import com.wintercogs.beyonddimensions.api.menu.workbench.StorageWorkbenchTypes;
import com.wintercogs.beyonddimensions.common.init.BDMenus;
import com.wintercogs.beyonddimensions.common.menu.DimensionsNetMenu;
import com.wintercogs.beyonddimensions.integration.module.emi.exclusion.BDExclusionZones;
import com.wintercogs.beyonddimensions.integration.module.emi.recipe.NetRecipeHandler;
import com.wintercogs.beyonddimensions.integration.module.emi.slothandler.SlotDragHandler;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.VanillaEmiRecipeCategories;
import net.minecraft.world.inventory.MenuType;

@EmiEntrypoint
public class BDEMIPlugin implements EmiPlugin
{
    @Override
    public void register(EmiRegistry registry)
    {
        addRecipeHandlers(registry, BDMenus.Dimensions_Net_Menu.get());
        addRecipeHandlers(registry, BDMenus.Dimensions_Net_Terminal_Menu.get());
        registry.addGenericDragDropHandler(new SlotDragHandler());
        registry.addGenericExclusionArea(new BDExclusionZones());
    }

    // 切石、锻造与铁砧的配方填进对应的工作台，其余配方填进合成台
    private static <T extends DimensionsNetMenu> void addRecipeHandlers(EmiRegistry registry, MenuType<T> type)
    {
        registry.addRecipeHandler(type, NetRecipeHandler.<T>workbench(() -> VanillaEmiRecipeCategories.STONECUTTING, StorageWorkbenchTypes.STONECUTTER, true));
        registry.addRecipeHandler(type, NetRecipeHandler.<T>workbench(() -> VanillaEmiRecipeCategories.SMITHING, StorageWorkbenchTypes.SMITHING, false));
        registry.addRecipeHandler(type, NetRecipeHandler.<T>workbench(() -> VanillaEmiRecipeCategories.ANVIL_REPAIRING, StorageWorkbenchTypes.ANVIL, false));
        registry.addRecipeHandler(type, NetRecipeHandler.<T>crafting());
    }
}
