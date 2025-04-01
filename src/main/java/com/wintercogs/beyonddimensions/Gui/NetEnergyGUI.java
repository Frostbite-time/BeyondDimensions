package com.wintercogs.beyonddimensions.Gui;

import com.cleanroommc.modularui.drawable.UITexture;
import com.cleanroommc.modularui.drawable.keys.StringKey;
import com.cleanroommc.modularui.factory.GuiData;
import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.value.sync.*;
import com.cleanroommc.modularui.widgets.*;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.BlockEntity.Custom.NetEnergyPathwayBlockEntity;
import com.wintercogs.beyonddimensions.DataBase.Handler.IStackTypedHandler;
import com.wintercogs.beyonddimensions.DataBase.Stack.IStackType;
import com.wintercogs.beyonddimensions.Gui.Factory.PosGuiFactory;

import java.util.List;

public class NetEnergyGUI extends BDOrderedContainerGUI
{



    public static PosGuiFactory factory =  new PosGuiFactory("net_energy_gui",() ->{
        return new NetEnergyGUI();
    });

    private double energyProcess = 0;
    private long energyStored = 0;
    private long energyCapacity = 0;

    @Override
    public ModularPanel buildUI(GuiData guiData, GuiSyncManager guiSyncManager)
    {
        ModularPanel panel = super.buildUI(guiData, guiSyncManager);

        stackTypedHandler = null;
        viewerStackTypedHandler = null;

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

        UITexture energyBarTexture = UITexture.fullImage(BeyondDimensions.MODID,"textures/gui/sprites/widget/energy_bar.png");
        ProgressWidget energyStorage = new ProgressWidget().width(160).height(16)
                .leftRel(0.5f)
                .topRel(0.5f)
                .texture(energyBarTexture,160)
                .value(
                        new DoubleSyncValue(
                                () -> BE.getEnergyProgress(),
                                value -> this.energyProcess = value
                        )
                );

        panel.child(popButton).child(energyStorage)
                .child(new TextFieldWidget().width(90).topRel(0.3f).leftRel(0.1f).setNumbersLong(()->0L,()->Long.MAX_VALUE).value(new StringSyncValue(() ->String.valueOf(BE.getEnergyStored()), string -> this.energyStored = Long.valueOf(string) )))
                .child(new TextFieldWidget().width(90).topRel(0.4f).leftRel(0.1f).setNumbersLong(()->0L,()->Long.MAX_VALUE).value(new StringSyncValue(() ->String.valueOf(BE.getEnergyCapacity()), string -> this.energyCapacity = Long.valueOf(string) )));
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
