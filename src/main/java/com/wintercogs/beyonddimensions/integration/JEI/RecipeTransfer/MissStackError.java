package com.wintercogs.beyonddimensions.integration.JEI.RecipeTransfer;

import com.mojang.blaze3d.vertex.PoseStack;
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
        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        poseStack.translate((float) recipeX, (float) recipeY, 0.0F);

        for (IRecipeSlotView slot : this.slots)
        {
            slot.drawHighlight(guiGraphics, HIGHLIGHT_COLOR);
        }

        poseStack.popPose();
    }

    @Override
    public int getMissingCountHint()
    {
        return this.slots.size();
    }
}