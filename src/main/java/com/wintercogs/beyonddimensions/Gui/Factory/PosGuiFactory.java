package com.wintercogs.beyonddimensions.Gui.Factory;

import com.cleanroommc.modularui.api.IGuiHolder;
import com.cleanroommc.modularui.factory.GuiData;
import com.cleanroommc.modularui.factory.GuiManager;
import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.factory.SimpleGuiFactory;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.PacketBuffer;

import java.util.function.Supplier;

// 带位置数据的GUI工厂
public class PosGuiFactory extends SimpleGuiFactory
{
    public PosGuiFactory(String name, Supplier<IGuiHolder<GuiData>> guiHolderSupplier)
    {
        super(name, guiHolderSupplier);
    }

    public void open(EntityPlayerMP player,int x,int y, int z)
    {
        GuiManager.open(this, new PosGuiData(player,x,y,z), player);
    }

    @Override
    public void writeGuiData(GuiData guiData, PacketBuffer buffer)
    {
        if(guiData instanceof PosGuiData posGuiData)
        {
            buffer.writeInt(posGuiData.getX());
            buffer.writeInt(posGuiData.getY());
            buffer.writeInt(posGuiData.getZ());
        }
    }

    @Override
    public GuiData readGuiData(EntityPlayer player, PacketBuffer buffer)
    {
        int x = buffer.readInt();
        int y = buffer.readInt();
        int z = buffer.readInt();
        return new PosGuiData(player,x,y,z);
    }
}
