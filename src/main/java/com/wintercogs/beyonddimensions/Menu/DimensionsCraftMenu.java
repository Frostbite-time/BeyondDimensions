package com.wintercogs.beyonddimensions.Menu;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.DataBase.DimensionsNet;
import com.wintercogs.beyonddimensions.DataBase.Handler.IStackTypedHandler;
import com.wintercogs.beyonddimensions.DataBase.Stack.IStackType;
import com.wintercogs.beyonddimensions.DataBase.Stack.ItemStackType;
import com.wintercogs.beyonddimensions.Menu.Slot.StoredStackSlot;
import com.wintercogs.beyonddimensions.Registry.UIRegister;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

// 自带合成台的DimensionsNetMenu
public class DimensionsCraftMenu extends DimensionsNetMenu
{

    private CraftingContainer craftSlots;
    private ResultContainer resultSlots;
    private int resultSlotIndex;


    /**
     * 客户端构造函数
     * @param playerInventory 玩家背包
     */
    public DimensionsCraftMenu(int id, Inventory playerInventory)
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
        super(UIRegister.Dimensions_Craft_Menu.get(), id,playerInventory,data);

        this.craftSlots = new TransientCraftingContainer(this, 3, 3);
        this.resultSlots = new ResultContainer();

        // 为其添加工艺槽
        this.addSlot(new ResultSlot(playerInventory.player, this.craftSlots, this.resultSlots, 0, 116+4, 127+4));
        resultSlotIndex = slots.size()-1;

        for(int i = 0; i < 3; ++i) {
            for(int j = 0; j < 3; ++j) {
                this.addSlot(new Slot(this.craftSlots, j + i * 3, 26 + j * 18, 113 + i * 18));
            }
        }
    }


    // 工艺槽实现
    protected static void slotChangedCraftingGrid(AbstractContainerMenu menu, Level level, Player player, CraftingContainer container, ResultContainer result, int resultSlotIndex) {
        if (!level.isClientSide) {
            ServerPlayer serverplayer = (ServerPlayer)player;
            ItemStack itemstack = ItemStack.EMPTY;
            Optional<CraftingRecipe> optional = level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, container, level);
            if (optional.isPresent()) {
                CraftingRecipe craftingrecipe = (CraftingRecipe)optional.get();
                if (result.setRecipeUsed(level, serverplayer, craftingrecipe)) {
                    ItemStack itemstack1 = craftingrecipe.assemble(container, level.registryAccess());
                    if (itemstack1.isItemEnabled(level.enabledFeatures())) {
                        itemstack = itemstack1;
                    }
                }
            }

            result.setItem(0, itemstack);
            menu.setRemoteSlot(resultSlotIndex, itemstack);
            serverplayer.connection.send(new ClientboundContainerSetSlotPacket(menu.containerId, menu.incrementStateId(), resultSlotIndex, itemstack));
        }

    }

    public void transferRecipe(List<ItemStack> inputs)
    {
        // 清空工艺槽物品
        // 先尝试放入玩家背包 这个过程中多出来的会掉落
        // 然后尝试放入存储
        // 最后尝试掉落
        if (player instanceof ServerPlayer) {
            List<ItemStack> stacks = craftSlots.getItems();
            for(ItemStack stack : stacks) {
                if(!stack.isEmpty())
                {
                    if (player.isAlive() && !((ServerPlayer)player).hasDisconnected()) {
                        player.getInventory().placeItemBackInInventory(stack);
                    } else {
                        long remaining = storage.insert(new ItemStackType(stack),false).getStackAmount();
                        if(remaining > 0)
                        {
                            stack.setCount((int)remaining);
                            player.drop(stack, false);
                        }
                    }
                }
            }
            craftSlots.clearContent();
            resultSlots.clearContent();
        }


        // 物品转移逻辑
        for (int slotIndex = 0; slotIndex < inputs.size() && slotIndex < craftSlots.getContainerSize(); slotIndex++) {
            ItemStack required = inputs.get(slotIndex);
            if (required.isEmpty()) continue;
            int remaining = required.getCount();
            ItemStack collected = required.copy();
            // 优先从背包提取
            remaining = extractFromInventory(player.getInventory(), collected, remaining);

            // 剩余数量从存储提取
            if (remaining > 0) {
                remaining = extractFromStorage(storage, new ItemStackType(collected), remaining);
            }
            // 设置合成槽物品
            if (remaining < required.getCount()) {
                collected.setCount(required.getCount() - remaining);
                craftSlots.setItem(slotIndex, collected);
            }
        }

    }

    // 从背包提取物品
    private int extractFromInventory(Inventory inventory, ItemStack template, int amount) {
        int remaining = amount;

        // 遍历背包主槽位（0-35）
        for (int i = 0; i < 36 && remaining > 0; i++) {
            ItemStack stack = inventory.getItem(i);
            if (ItemStack.isSameItemSameTags(stack, template)) {
                int extract = Math.min(remaining, stack.getCount());
                stack.shrink(extract);
                remaining -= extract;
                inventory.setItem(i, stack.isEmpty() ? ItemStack.EMPTY : stack);
            }
        }
        return remaining;
    }
    // 从存储提取物品
    private int extractFromStorage(IStackTypedHandler storage, IStackType type, int amount) {
        IStackType extraction = storage.extract(type.copyWithCount(amount), false);
        if (extraction.getStackAmount() > 0) {
            return amount - (int)extraction.getStackAmount();
        }
        return amount;
    }

    @Override
    public void slotsChanged(Container container)
    {
        super.slotsChanged(container);
        slotChangedCraftingGrid(this,player.level(),player,craftSlots,resultSlots, resultSlotIndex);
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


    @Override
    public void removed(Player player)
    {
        super.removed(player);
        // 将合成槽物品优先放入玩家背包 否则掉落
        if (player instanceof ServerPlayer) {
            List<ItemStack> stacks = craftSlots.getItems();
            for(ItemStack stack : stacks) {
                if(!stack.isEmpty())
                {
                    if (player.isAlive() && !((ServerPlayer)player).hasDisconnected()) {
                        player.getInventory().placeItemBackInInventory(stack);
                    } else {
                        long remaining = storage.insert(new ItemStackType(stack),false).getStackAmount();
                        if(remaining > 0)
                        {
                            stack.setCount((int)remaining);
                            player.drop(stack, false);
                        }
                    }
                }
            }
            craftSlots.clearContent();
            resultSlots.clearContent();
        }
    }
}
