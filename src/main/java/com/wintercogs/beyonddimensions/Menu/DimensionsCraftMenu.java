package com.wintercogs.beyonddimensions.Menu;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.DataBase.DimensionsNet;
import com.wintercogs.beyonddimensions.Menu.Slot.StoredStackSlot;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.*;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

// 自带合成台的DimensionsNetMenu
public class DimensionsCraftMenu extends DimensionsNetMenu
{

    private CraftingContainer craftSlots;
    private ResultContainer resultSlots;



    // 构建注册用的信息
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(Registries.MENU, BeyondDimensions.MODID);
    public static final Supplier<MenuType<DimensionsCraftMenu>> Dimensions_Craft_Menu = MENU_TYPES.register("dimensions_craft_menu", () -> IMenuTypeExtension.create(DimensionsCraftMenu::new));


    /**
     * 客户端构造函数
     * @param playerInventory 玩家背包
     */
    public DimensionsCraftMenu(int id, Inventory playerInventory, FriendlyByteBuf data)
    {
        // 客户端函数，故将Net设为临时Net
        this(id, playerInventory, new DimensionsNet(true));
    }

    /**
     * 服务端构造函数
     * @param playerInventory 玩家背包
     * @param data 维度网络信息，包含了存储信息
     */
    public DimensionsCraftMenu(int id, Inventory playerInventory, DimensionsNet data)
    {
        // 利用父类函数处理存储槽位 玩家背包 和一些其他数据添加处理
        super(Dimensions_Craft_Menu.get(), id,playerInventory,data);

        this.craftSlots = new TransientCraftingContainer(this, 3, 3);
        this.resultSlots = new ResultContainer();

        // 为其添加工艺槽
        this.addSlot(new ResultSlot(playerInventory.player, this.craftSlots, this.resultSlots, 0, 116, 127));

        for(int i = 0; i < 3; ++i) {
            for(int j = 0; j < 3; ++j) {
                this.addSlot(new Slot(this.craftSlots, j + i * 3, 26 + j * 18, 113 + i * 18));
            }
        }
    }

    @Override
    protected int getLines()
    {
        return 5;
    }

    @Override
    protected void addStorageSlots()
    {
        for (int row = 0; row < getLines(); ++row)
        {
            for (int col = 0; col < 9; ++col)
            {
                this.addSlot(new StoredStackSlot(viewerStorage, -1, 8 + col * 18, 20+row * 18));
            }
        }
    }


    @Override
    protected void addPlayerInv(Inventory playerInventory)
    {
        inventoryStartIndex = slots.size();
        for (int row = 0; row < 3; ++row)
        {
            for (int col = 0; col < 9; ++col)
            {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 175 + row * 18));
            }
        }
        for (int col = 0; col < 9; ++col)
        {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 233));
        }
        inventoryEndIndex = slots.size();
    }
}
