//package com.wintercogs.beyonddimensions.Integration.EMI.SlotHandler;
//
//import dev.emi.emi.api.EmiDragDropHandler;
//import dev.emi.emi.api.stack.EmiIngredient;
//import net.minecraft.client.gui.GuiGraphics;
//import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
//import net.minecraft.world.inventory.Slot;
//
//import java.util.function.BiConsumer;
//import java.util.function.Predicate;
//
//public class SlotDragHandler implements EmiDragDropHandler<AbstractContainerScreen<?>>
//{
//    private final Predicate<Slot> slotFilter;
//    private final BiConsumer<Slot, EmiIngredient> dropHandler;
//    public SlotDragHandler(Predicate<Slot> slotFilter, BiConsumer<Slot, EmiIngredient> dropHandler) {
//        this.slotFilter = slotFilter;
//        this.dropHandler = dropHandler;
//    }
//    @Override
//    public boolean dropStack(AbstractContainerScreen<?> screen, EmiIngredient ingredient, int x, int y) {
//        // 转换屏幕坐标到容器相对坐标
//        double mouseX = x - screen.getGuiLeft();
//        double mouseY = y - screen.getGuiTop();
//        // 遍历所有槽位检查是否在有效区域内
//        for (Slot slot : screen.getMenu().slots) {
//            if (slotFilter.test(slot) && isMouseOverSlot(screen, slot, mouseX, mouseY)) {
//                dropHandler.accept(slot, ingredient);
//                return true;
//            }
//        }
//        return false;
//    }
//
//    @Override
//    public void render(AbstractContainerScreen<?> screen, EmiIngredient dragged, GuiGraphics draw, int mouseX, int mouseY, float delta)
//    {
//        EmiDragDropHandler.super.render(screen, dragged, draw, mouseX, mouseY, delta);
//    }
//
//    private boolean isMouseOverSlot(AbstractContainerScreen<?> screen, Slot slot,
//                                    double mouseX, double mouseY) {
//        return isPointInRegion(screen, slot.x, slot.y, 16, 16, mouseX, mouseY);
//    }
//    private boolean isPointInRegion(AbstractContainerScreen<?> screen, int x, int y,
//                                    int width, int height, double pointX, double pointY) {
//        return pointX >= x - 1 && pointX < x + width + 1 &&
//                pointY >= y - 1 && pointY < y + height + 1;
//    }
//
//}
