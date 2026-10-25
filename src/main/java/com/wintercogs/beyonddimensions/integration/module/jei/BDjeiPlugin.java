package com.wintercogs.beyonddimensions.integration.module.jei;

import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import com.wintercogs.beyonddimensions.api.menu.workbench.StorageWorkbenchTypes;
import com.wintercogs.beyonddimensions.client.ui.base.BdInventoryScreen;
import com.wintercogs.beyonddimensions.common.init.BDMenus;
import com.wintercogs.beyonddimensions.common.menu.DimensionsNetMenu;
import com.wintercogs.beyonddimensions.common.menu.DimensionsNetTerminalMenu;
import com.wintercogs.beyonddimensions.integration.module.jei.handler.ComposeJeiScreenHandler;
import com.wintercogs.beyonddimensions.integration.module.jei.transfer.MenuUniversalRecipeTransfer;
import com.wintercogs.beyonddimensions.integration.module.jei.transfer.TerminalUniversalRecipeTransfer;
import com.wintercogs.beyonddimensions.integration.module.jei.transfer.WorkbenchRecipeTransferHandler;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.crafting.StonecutterRecipe;

import java.util.Optional;
import java.util.function.Supplier;

@JeiPlugin
public class BDjeiPlugin implements IModPlugin
{
    public static IJeiRuntime jeiRuntime;

    @Override
    public ResourceLocation getPluginUid()
    {
        return ResourceLocation.tryBuild(BDConstants.MODID, "jei_plugin");
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration)
    {
        // 合成、切石、锻造与铁砧的配方填进对应的工作台；其他配方由通用的转移填进合成格
        registerWorkbenches(registration, DimensionsNetMenu.class, BDMenus.Dimensions_Net_Menu);
        registerWorkbenches(registration, DimensionsNetTerminalMenu.class, BDMenus.Dimensions_Net_Terminal_Menu);
        registration.addUniversalRecipeTransferHandler(new MenuUniversalRecipeTransfer());
        registration.addUniversalRecipeTransferHandler(new TerminalUniversalRecipeTransfer());
    }

    // JEI 按菜单的具体类型查找转移，两种存储菜单各注册一份
    private static <C extends DimensionsNetMenu> void registerWorkbenches(IRecipeTransferRegistration registration, Class<C> menu,
                                                                          Supplier<MenuType<C>> type)
    {
        registration.addRecipeTransferHandler(new WorkbenchRecipeTransferHandler<>(menu, type, RecipeTypes.CRAFTING,
                StorageWorkbenchTypes.CRAFTING, recipe -> null), RecipeTypes.CRAFTING);
        registration.addRecipeTransferHandler(new WorkbenchRecipeTransferHandler<>(menu, type, RecipeTypes.STONECUTTING,
                StorageWorkbenchTypes.STONECUTTER, StonecutterRecipe::getId), RecipeTypes.STONECUTTING);
        registration.addRecipeTransferHandler(new WorkbenchRecipeTransferHandler<>(menu, type, RecipeTypes.SMITHING,
                StorageWorkbenchTypes.SMITHING, recipe -> null), RecipeTypes.SMITHING);
        registration.addRecipeTransferHandler(new WorkbenchRecipeTransferHandler<>(menu, type, RecipeTypes.ANVIL,
                StorageWorkbenchTypes.ANVIL, recipe -> null), RecipeTypes.ANVIL);
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration)
    {
        registration.addGhostIngredientHandler(BdInventoryScreen.class, new NetInterfaceGhostHandler<>());
        registration.addGuiScreenHandler(BdInventoryScreen.class, new ComposeJeiScreenHandler<>());
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime)
    {
        BDjeiPlugin.jeiRuntime = jeiRuntime;
    }

    @Override
    public void onRuntimeUnavailable()
    {
        BDjeiPlugin.jeiRuntime = null;
    }

    public static Optional<IJeiRuntime> runtime()
    {
        return Optional.ofNullable(BDjeiPlugin.jeiRuntime);
    }
}
