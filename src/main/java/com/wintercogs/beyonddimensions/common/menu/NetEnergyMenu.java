package com.wintercogs.beyonddimensions.common.menu;

import com.wintercogs.beyonddimensions.api.dimensionnet.DimensionsNet;
import com.wintercogs.beyonddimensions.api.dimensionnet.UnifiedStorage;
import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import com.wintercogs.beyonddimensions.api.storage.key.impl.EnergyStackKey;
import com.wintercogs.beyonddimensions.common.block.entity.NetEnergyPathwayBlockEntity;
import com.wintercogs.beyonddimensions.common.machine.PopMode;
import com.wintercogs.beyonddimensions.common.machine.RedStoneControlMode;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public class NetEnergyMenu extends BDBaseMenu
{
    public NetEnergyPathwayBlockEntity be;

    public long lastEnergyCapacity = 0;
    public long lastEnergyStored = 0;
    public long lastEnergySpeedState = 0;


    // 构建注册用的信息
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(Registries.MENU, BDConstants.MODID);
    public static final Supplier<MenuType<NetEnergyMenu>> Net_Energy_Menu = MENU_TYPES.register("net_energy_menu", () -> IMenuTypeExtension.create(NetEnergyMenu::new));
    // 我们的辅助函数
    // 我们需要通过IMenuTypeExtension的.create方法才能返回一个menutype，
    // create方法需要传入一个IContainerFactory的内容，而正好我们的构造函数就是IContainerFactory一样的参数。
    // 因为就是这样设计的， 所以传入new就可以了。


    /**
     * 客户端构造函数
     *
     * @param playerInventory 玩家背包
     */
    public NetEnergyMenu(int id, Inventory playerInventory, FriendlyByteBuf data)
    {
        this(id, playerInventory, (NetEnergyPathwayBlockEntity) playerInventory.player.level().getBlockEntity(data.readBlockPos()));
    }

    /**
     * 服务端构造函数
     *
     * @param playerInventory 玩家背包
     */
    public NetEnergyMenu(int id, Inventory playerInventory, NetEnergyPathwayBlockEntity be)
    {
        super(Net_Energy_Menu.get(), id, playerInventory);

        this.be = be;

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
    protected boolean shouldSendQuickData()
    {
        DimensionsNet netCache = be.getNet();
        if (netCache != null)
        {
            UnifiedStorage storage = netCache.getUnifiedStorage();
            if (lastEnergyStored != getEnergyStored(storage)
                    || lastEnergyCapacity != storage.getSlotCapacity(0)
                    || lastEnergySpeedState != getEnergyStored(storage) - lastEnergyStored)
            {
                lastEnergySpeedState = getEnergyStored(storage) - lastEnergyStored;
                lastEnergyStored = getEnergyStored(storage);
                lastEnergyCapacity = storage.getSlotCapacity(0);
                return true;
            }
        }
        else
        {
            if (lastEnergyStored != 0
                    || lastEnergyCapacity != 0
                    || lastEnergySpeedState != 0)
            {
                lastEnergySpeedState = 0;
                lastEnergyStored = 0;
                lastEnergyCapacity = 0;
                return true;
            }
        }
        return false;
    }

    @Override
    protected void writeQuickDataTag(CompoundTag tag)
    {
        super.writeQuickDataTag(tag);
        tag.putString("popMode", be.getPopMode().name());
        tag.putString("controlMode", be.controlMode.name());
        tag.putLong("lastEnergyCapacity", lastEnergyCapacity);
        tag.putLong("lastEnergySpeedState", lastEnergySpeedState);
        tag.putLong("lastEnergyStored", lastEnergyStored);
    }

    @Override
    public void readQuickDataTag(CompoundTag tag)
    {
        super.readQuickDataTag(tag);
        if (player.level().isClientSide())
        {
            this.lastEnergyStored = tag.getLong("lastEnergyStored").orElse(0L);
            this.lastEnergyCapacity = tag.getLong("lastEnergyCapacity").orElse(0L);
            this.lastEnergySpeedState = tag.getLong("lastEnergySpeedState").orElse(0L);
        }
        else
        {
            be.setPopMode(PopMode.valueOf(tag.getString("popMode").orElse(PopMode.STOP.name())));
            be.controlMode = RedStoneControlMode.valueOf(tag.getString("controlMode").orElse(RedStoneControlMode.IGNORE.name()));
            player.level().blockEntityChanged(be.getBlockPos());
            player.level().invalidateCapabilities(be.getBlockPos()); // 改变弹出模式后重新确定是否暴露内部能量存储
            player.level().sendBlockUpdated(be.getBlockPos(), be.getBlockState(), be.getBlockState(), 2);
        }
    }

    @Override
    public boolean stillValid(@NotNull Player player)
    {
        return be != null && !be.isRemoved();
    }

    long getEnergyStored(UnifiedStorage storage)
    {
        return storage.getStackByKey(EnergyStackKey.INSTANCE).amount();
    }
}
