package com.wintercogs.beyonddimensions.Menu;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.BlockEntity.Custom.NetEnergyPathwayBlockEntity;
import com.wintercogs.beyonddimensions.DataBase.DimensionsNet;
import com.wintercogs.beyonddimensions.DataBase.Storage.EnergyStorage;
import com.wintercogs.beyonddimensions.Packet.EnergyStoragePacket;
import com.wintercogs.beyonddimensions.Packet.PopModeButtonPacket;
import com.wintercogs.beyonddimensions.Registry.UIRegister;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class NetEnergyMenu extends BDOrderedContainerMenu
{

    public boolean popMode;
    public NetEnergyPathwayBlockEntity be;

    private EnergyStorage energyStorage = null; // 注意判断

    public long energyCapacity = 0;
    public long energyStored = 0;



    public static final Supplier<MenuType<NetEnergyMenu>> Net_Energy_Menu = UIRegister.MENU_TYPES.register("net_energy_menu", () -> new MenuType<>(NetEnergyMenu::new, FeatureFlags.DEFAULT_FLAGS));
    // 我们的辅助函数
    // 我们需要通过IMenuTypeExtension的.create方法才能返回一个menutype，
    // create方法需要传入一个IContainerFactory的内容，而正好我们的构造函数就是IContainerFactory一样的参数。
    // 因为就是这样设计的， 所以传入new就可以了。


    /**
     * 客户端构造函数
     *
     * @param playerInventory 玩家背包
     */
    public NetEnergyMenu(int id, Inventory playerInventory)
    {
        this(id, playerInventory, null, new SimpleContainerData(0));
    }

    /**
     * 服务端构造函数
     *
     * @param playerInventory  玩家背包
     * @param uselessContainer 此处无用，传入new SimpleContainerData(0)即可
     */
    public NetEnergyMenu(int id, Inventory playerInventory, NetEnergyPathwayBlockEntity be, SimpleContainerData uselessContainer)
    {
        super(Net_Energy_Menu.get(), id,playerInventory,null);
        // 初始化维度网络容器
        this.popMode = false;
        if (!player.level().isClientSide())
        {
            this.popMode = be.popMode;
            this.be = be;
            DimensionsNet net = be.getNet();
            if (net != null)
                this.energyStorage = be.getNet().getEnergyStorage();
            if (energyStorage != null)
            {
                this.energyCapacity = energyStorage.getRealEnergyCapacity();
                this.energyStored = energyStorage.getRealEnergyStored();
            }

        }

        inventoryStartIndex = slots.size();
        for (int row = 0; row < 3; ++row)
        {
            for (int col = 0; col < 9; ++col)
            {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 93 + row * 18));
            }
        }

        for (int col = 0; col < 9; ++col)
        {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 151));
        }
        inventoryEndIndex = slots.size();
    }

    @Override
    protected void updateChange()
    {
        if (energyStorage != null)
        {
            if (energyStorage.getRealEnergyCapacity() != energyCapacity || energyStored != energyCapacity)
            {
                this.energyCapacity = energyStorage.getRealEnergyCapacity();
                this.energyStored = energyStorage.getRealEnergyStored();
                PacketDistributor.sendToPlayer((ServerPlayer) player, new EnergyStoragePacket(this.energyStored, this.energyCapacity));
            }
        }
    }

    @Override
    protected void initUpdate()
    {
        PacketDistributor.sendToPlayer((ServerPlayer) player,new PopModeButtonPacket(popMode));
    }


    public void loadStorage(long energyCapacity, long energyStored)
    {
        this.energyCapacity = energyCapacity;
        this.energyStored = energyStored;
    }


    @Override
    public boolean stillValid(Player player)
    {
        return true;
    }
}
