package com.wintercogs.beyonddimensions.integration.jei.transfer;

import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import net.minecraft.client.gui.GuiGraphics;

import java.util.Collection;

public class MissStackError implements IRecipeTransferError
{
    private static final int HIGHLIGHT_COLOR = 1727987712;
    private final Collection<IRecipeSlotView> slots;

    public MissStackError(Collection<IRecipeSlotView> slots)
    {
        this.slots = slots;
    }

    @Override
    public Type getType()
    {
        return Type.COSMETIC;
    }

    @Override
    public int getButtonHighlightColor()
    {
        return IRecipeTransferError.super.getButtonHighlightColor();
    }

    @Override
    public void showError(GuiGraphics guiGraphics, int mouseX, int mouseY, IRecipeSlotsView recipeSlotsView, int recipeX, int recipeY)
    {
        var poseStack = guiGraphics.pose();
        poseStack.pushMatrix();
        poseStack.translate((float) recipeX, (float) recipeY);

        for (IRecipeSlotView slot : this.slots)
        {
            slot.drawHighlight(guiGraphics, HIGHLIGHT_COLOR);
        }

        poseStack.popMatrix();
    }

    @Override
    public int getMissingCountHint()
    {
        return this.slots.size();
    }
}
