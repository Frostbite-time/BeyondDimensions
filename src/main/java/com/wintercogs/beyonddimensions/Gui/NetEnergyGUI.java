package com.wintercogs.beyonddimensions.Gui;

import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.drawable.UITexture;
import com.cleanroommc.modularui.factory.GuiData;
import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.value.sync.BooleanSyncValue;
import com.cleanroommc.modularui.value.sync.GuiSyncManager;
import com.cleanroommc.modularui.widgets.CycleButtonWidget;
import com.cleanroommc.modularui.widgets.SlotGroupWidget;
import com.cleanroommc.modularui.widgets.ToggleButton;
import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.BlockEntity.Custom.NetEnergyPathwayBlockEntity;
import com.wintercogs.beyonddimensions.BlockEntity.Custom.NetInterfaceBlockEntity;
import com.wintercogs.beyonddimensions.DataBase.Handler.IStackTypedHandler;
import com.wintercogs.beyonddimensions.DataBase.Handler.StackTypedHandler;
import com.wintercogs.beyonddimensions.DataBase.Stack.IStackType;
import com.wintercogs.beyonddimensions.Gui.Factory.PosGuiFactory;

import java.util.List;

public class NetEnergyGUI extends BDOrderedContainerGUI
{

    public static PosGuiFactory factory =  new PosGuiFactory("net_energy_gui",() ->{
        return new NetEnergyGUI();
    });

    @Override
    public ModularPanel buildUI(GuiData guiData, GuiSyncManager guiSyncManager)
    {
        ModularPanel panel = super.buildUI(guiData, guiSyncManager);

        NetEnergyPathwayBlockEntity BE;

        BE = (NetEnergyPathwayBlockEntity) ((PosGuiData)guiData).getTileEntity();

        CycleButtonWidget popButton = new CycleButtonWidget().value(
                new BooleanSyncValue(
                        () -> BE.popMode,
                        var -> {
                            BE.popMode = var;
                        }
                )
        );
        popButton.texture(UITexture.fullImage(BeyondDimensions.MODID,"textures/gui/sprites/widget/pop_mode.png"));

        //popButton.overlay(UITexture.fullImage(BeyondDimensions.MODID,"textures/gui/sprites/widget/sort_asc.png"));
        //popButton.selectedBackground(UITexture.fullImage(BeyondDimensions.MODID,"textures/gui/sprites/widget/sort_asc.png"));
        //popButton.selectedHoverBackground(UITexture.fullImage(BeyondDimensions.MODID,"textures/gui/sprites/widget/sort_asc.png"));

        panel.child(popButton);
        return panel;
    }

    @Override
    public SlotGroupWidget buildStackTypedSlots(IStackTypedHandler stackTypedHandler)
    {
        return null;
    }

    @Override
    protected boolean SeachTextMatch(IStackType stack)
    {
        return false;
    }

    @Override
    protected List<Integer> SortIndexList(List<IStackType> stacksSource, List<Integer> indicesSource)
    {
        return null;
    }
}
