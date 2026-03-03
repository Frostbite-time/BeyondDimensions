package com.wintercogs.beyonddimensions.Menu;

import com.wintercogs.beyonddimensions.Api.DataBase.ButtonState;
import com.wintercogs.beyonddimensions.Api.DataBase.Handler.AbstractUnorderedStackHandler;
import com.wintercogs.beyonddimensions.Api.DataBase.Handler.IStackHandler;
import com.wintercogs.beyonddimensions.Api.DataBase.Handler.UnorderedStackHandlerRemoveZero;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.IStackKey;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.ItemStackKey;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.KeyAmount;
import com.wintercogs.beyonddimensions.Api.config.CommonConfigRuntime;
import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.Menu.Slot.AbstractStackTypedSlot;
import com.wintercogs.beyonddimensions.Menu.Slot.AutoRefillResultSlot;
import com.wintercogs.beyonddimensions.Menu.Slot.DisorderedStackTypedSlot;
import com.wintercogs.beyonddimensions.Util.InventoryHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

// 自带合成台的DimensionsNetMenu
public class DimensionsCraftMenu extends DimensionsNetMenu
{

    protected CraftingContainer craftSlots;
    protected ResultContainer resultSlots;
    public int resultSlotIndex;
    public int craftSlotStartIndex;
    public int craftSlotEndIndex;
    public boolean firstCraftReturnDir = false; // 决定关闭菜单时工艺槽的优先转移方向，true向存储 false背包


    // 构建注册用的信息
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(Registries.MENU, BeyondDimensions.MODID);
    public static final Supplier<MenuType<DimensionsCraftMenu>> Dimensions_Craft_Menu = MENU_TYPES.register("dimensions_craft_menu", () -> IMenuTypeExtension.create(DimensionsCraftMenu::new));


    /**
     * 客户端构造函数
     *
     * @param playerInventory 玩家背包
     */
    public DimensionsCraftMenu(int id, Inventory playerInventory, FriendlyByteBuf data)
    {
        // 客户端函数，故将Net设为临时Net
        this(Dimensions_Craft_Menu.get(), id, playerInventory, new UnorderedStackHandlerRemoveZero(AbstractUnorderedStackHandler.UiTimestampPolicy.NONE), null, null);
    }

    /**
     * 服务端构造函数
     *
     * @param playerInventory 玩家背包
     * @param data            维度网络信息，包含了存储信息
     */
    public DimensionsCraftMenu(MenuType<?> type, int id, Inventory playerInventory, AbstractUnorderedStackHandler data, @Nullable NonNullList<ItemStack> craftItems, @Nullable BlockPos entityPos)
    {
        // 利用父类函数处理存储槽位 玩家背包 和一些其他数据添加处理
        super(type, id, playerInventory, data);

        TransientCraftingContainer craftContainer;
        if (craftItems != null)
        {
            craftContainer = new TransientCraftingContainer(this, 3, 3)
            {
                @Override
                public void setChanged()
                {
                    super.setChanged();
                    if (entityPos != null && !player.level().isClientSide())
                    {
                        player.level().blockEntityChanged(entityPos);
                    }
                }
            };
            for (int i = 0; i < craftItems.size(); i++)
            {
                craftContainer.setItem(i, craftItems.get(i));
            }
        }
        else
            craftContainer = new TransientCraftingContainer(this, 3, 3)
            {
                @Override
                public void setChanged()
                {
                    super.setChanged();
                    if (entityPos != null && !player.level().isClientSide())
                    {
                        player.level().blockEntityChanged(entityPos);
                    }
                }
            };
        initCraftSlots(playerInventory, craftContainer);
    }


    @Override
    protected void addStorageSlots()
    {
        // 默认添加99行，但将99之外的行全部设置为不激活状态，以实现动态增加和减少行数
        storageStartIndex = slots.size();
        vanillaQuickMoveStartIndex = storageStartIndex;
        if (player.level().isClientSide())
        {
            for (int row = 0; row < 99; ++row)
            {
                for (int col = 0; col < 9; ++col)
                {
                    DisorderedStackTypedSlot newSlot = new DisorderedStackTypedSlot(this, clientNetStorage, -1, inventoryStartIndex, inventoryEndIndex, 8 + col * 18, 25 + row * 18);
                    if (row >= getLines())
                        newSlot.setActive(false);
                    this.addSlot(newSlot);
                }
            }
        }
        else
        {
            for (int row = 0; row < 99; ++row)
            {
                for (int col = 0; col < 9; ++col)
                {
                    DisorderedStackTypedSlot newSlot = new DisorderedStackTypedSlot(this, storage, -1, inventoryStartIndex, inventoryEndIndex, 8 + col * 18, 25 + row * 18);
                    if (row >= getLines())
                        newSlot.setActive(false);
                    this.addSlot(newSlot);
                }
            }
        }
        storageEndIndex = slots.size();
        vanillaQuickMoveEndIndex = storageEndIndex;
    }


    @Override
    protected void addPlayerInv(Inventory playerInventory)
    {
        inventoryStartIndex = slots.size();
        for (int row = 0; row < 3; ++row)
        {
            for (int col = 0; col < 9; ++col)
            {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 25 + 62 + (getLines() - 1) * 18 + 26 + 6 + row * 18));
            }
        }
        for (int col = 0; col < 9; ++col)
        {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 25 + 62 + (getLines() - 1) * 18 + 26 + 6 + 3 * 18 + 4));
        }
        inventoryEndIndex = slots.size();
    }

    protected void initCraftSlots(Inventory playerInventory, @Nullable TransientCraftingContainer craftSlots)
    {
        this.craftSlots = Objects.requireNonNullElseGet(craftSlots, () -> new TransientCraftingContainer(this, 3, 3));
        this.resultSlots = new ResultContainer();

        // 为其添加工艺槽
        this.addSlot(new AutoRefillResultSlot(this, playerInventory.player, this.craftSlots, this.resultSlots, 0, 116 + 4, 24 + (getLines() - 1) * 18 + 26 + 21));
        resultSlotIndex = slots.size() - 1;

        craftSlotStartIndex = slots.size();
        for (int i = 0; i < 3; ++i)
        {
            for (int j = 0; j < 3; ++j)
            {
                this.addSlot(new Slot(this.craftSlots, j + i * 3, 26 + j * 18, 24 + (getLines() - 1) * 18 + 26 + 3 + i * 18));
            }
        }
        craftSlotEndIndex = slots.size();
    }


    // 工艺槽实现
    public static void slotChangedCraftingGrid(AbstractContainerMenu menu, Level level, Player player, CraftingContainer craftSlots, ResultContainer resultSlots, int resultSlotIndex)
    {
        if (!level.isClientSide())
        {
            CraftingInput craftinginput = craftSlots.asCraftInput();
            ServerPlayer serverplayer = (ServerPlayer) player;
            ItemStack itemstack = ItemStack.EMPTY;
            Optional<RecipeHolder<@NotNull CraftingRecipe>> optional = getRecipe(player, craftinginput, Objects.requireNonNull(serverplayer.level()));
            if (optional.isPresent())
            {

                // 原版过程
                RecipeHolder<@NotNull CraftingRecipe> recipeholder = optional.get();
                CraftingRecipe craftingrecipe = recipeholder.value();
                if (resultSlots.setRecipeUsed(serverplayer, recipeholder))
                {
                    ItemStack itemstack1 = craftingrecipe.assemble(craftinginput, level.registryAccess());
                    if (itemstack1.isItemEnabled(level.enabledFeatures()))
                    {
                        itemstack = itemstack1;
                    }
                }
            }

            resultSlots.setItem(0, itemstack);
            menu.setRemoteSlot(resultSlotIndex, itemstack);
            serverplayer.connection.send(new ClientboundContainerSetSlotPacket(menu.containerId, menu.incrementStateId(), resultSlotIndex, itemstack));
        }

    }

    @Override
    public boolean canTakeItemForPickAll(@NotNull ItemStack stack, @NotNull Slot slot)
    {
        return slot.container != resultSlots && super.canTakeItemForPickAll(stack, slot);
    }

    public static Optional<RecipeHolder<CraftingRecipe>> getRecipe(Player player, CraftingInput input, ServerLevel serverLevel)
    {
//        if (BeyondDimensions.PolymorphLoaded && player != null)
//        {
//            return PolymorphHelper.getRecipe(player, RecipeType.CRAFTING, input, level);
//        }
        return serverLevel.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, serverLevel);
    }

    public void transferRecipe(List<IStackKey<?>> inputKeys, List<Long> amount)
    {
        // 清空工艺槽物品
        cleanCraftSlots(firstCraftReturnDir);

        final int limit = Math.min(craftSlots.getContainerSize(), inputKeys.size());
        for (int i = 0; i < limit; i++)
        {
            long needL = (i < amount.size() ? amount.get(i) : 0L);
            IStackKey<?> key = inputKeys.get(i);

            if (!(key instanceof ItemStackKey itemStackKey) || needL <= 0) continue;

            int need = (int) Math.min(Integer.MAX_VALUE, needL);

            // 这里只有实际执行转移时才会调用copy，且槽位数量有限，整体性能可控
            int remaining = extractFromInventory(player.getInventory(), itemStackKey.copyStack(), need);
            if (remaining > 0) remaining = extractFromStorage(storage, itemStackKey, remaining);

            int got = need - remaining;
            if (got > 0) craftSlots.setItem(i, itemStackKey.copyStackWithCount(got));
        }
    }

    // 从背包提取物品
    private int extractFromInventory(Inventory inventory, ItemStack template, int amount)
    {
        int remaining = amount;

        // 遍历背包主槽位（0-35）
        for (int i = 0; i < 36 && remaining > 0; i++)
        {
            ItemStack stack = inventory.getItem(i);
            if (ItemStack.isSameItemSameComponents(stack, template))
            {
                int extract = Math.min(remaining, stack.getCount());
                stack.shrink(extract);
                remaining -= extract;
                inventory.setItem(i, stack.isEmpty() ? ItemStack.EMPTY : stack);
            }
        }
        return remaining;
    }

    // 从存储提取物品
    private int extractFromStorage(IStackHandler storage, IStackKey<?> type, int amount)
    {
        KeyAmount extraction = storage.extract(type, amount, false, false);
        if (extraction.amount() > 0)
        {
            return amount - (int) extraction.amount();
        }
        return amount;
    }

    @Override
    public void slotsChanged(Container container)
    {
        super.slotsChanged(container);
        slotChangedCraftingGrid(this, player.level(), player, craftSlots, resultSlots, resultSlotIndex);
    }

    // 放大和缩小UI所使用的函数，用于重新确定槽位的激活状态以及槽位的位置
    public void rebuildSlots()
    {
        int sSlotNum = 0;
        for (Slot slot : slots)
        {
            if (slot instanceof AbstractStackTypedSlot sSlot)
            {
                if (sSlotNum / 9 < getLines())
                    sSlot.setActive(true);
                else
                    sSlot.setActive(false);
                sSlotNum++; // 先处理再加数，可以防止最后一个槽位出现问题
            }
        }

        int slotNum = 0;
        for (int i = inventoryStartIndex; i < inventoryEndIndex; ++i)
        {
            Slot slot = slots.get(i);
            if (slot != null)
            {
                if (slotNum / 9 < 3)
                {
                    slot.y = 25 + 62 + (getLines() - 1) * 18 + 26 + 6 + slotNum / 9 * 18;
                }
                else
                {
                    slot.y = 25 + 62 + (getLines() - 1) * 18 + 26 + 6 + 3 * 18 + 4;
                }


                slotNum++;
            }
        }

        Slot resultSlot = slots.get(resultSlotIndex);
        resultSlot.y = 24 + (getLines() - 1) * 18 + 26 + 21;

        slotNum = 0;
        for (int i = craftSlotStartIndex; i < craftSlotEndIndex; ++i)
        {
            Slot slot = slots.get(i);
            if (slot != null)
            {
                slot.y = 24 + (getLines() - 1) * 18 + 26 + 3 + slotNum / 3 * 18;
                slotNum++;
            }
        }
    }

    public void cleanCraftSlots(boolean toStorageFirst)
    {
        if (player instanceof ServerPlayer)
        {
            List<ItemStack> stacks = craftSlots.getItems();
            for (ItemStack stack : stacks)
            {
                if (!stack.isEmpty())
                {
                    long remaining;
                    if (toStorageFirst)
                    {
                        remaining = storage.insert(new ItemStackKey(stack), stack.getCount(), false).amount();
                        if (remaining > 0)
                        {
                            stack.setCount((int) remaining);
                            remaining = InventoryHelper.transferToPlayerInventory(player, stack.copy()).getCount();
                            if (remaining > 0)
                            {
                                stack.setCount((int) remaining);
                                player.drop(stack, false);
                            }
                        }
                    }
                    else if (player.isAlive() && !((ServerPlayer) player).hasDisconnected())
                    {
                        remaining = InventoryHelper.transferToPlayerInventory(player, stack.copy()).getCount();

                        if (remaining > 0)
                        {
                            stack.setCount((int) remaining);
                            remaining = storage.insert(new ItemStackKey(stack), stack.getCount(), false).amount();
                            if (remaining > 0)
                            {
                                stack.setCount((int) remaining);
                                player.drop(stack, false);
                            }
                        }
                    }
                    else
                    {
                        player.drop(stack, false);
                    }

                }
            }
            craftSlots.clearContent();
            resultSlots.clearContent();
        }
    }

    @Override
    protected boolean shouldSendQuickData()
    {
        return super.shouldSendQuickData();
    }

    @Override
    protected void writeQuickDataTag(CompoundTag tag)
    {
        super.writeQuickDataTag(tag);
        if (player.level().isClientSide())
            firstCraftReturnDir = CommonConfigRuntime.uiCraftReturnButton == ButtonState.ENABLED;
        tag.putBoolean("firstCraftReturnDir", firstCraftReturnDir);
    }

    @Override
    public void readQuickDataTag(CompoundTag tag)
    {
        super.readQuickDataTag(tag);
        if (player.level().isClientSide())
        {
        }
        else
        {
            firstCraftReturnDir = tag.getBoolean("firstCraftReturnDir").orElse(false);
        }
    }

    @Override
    public void removed(@NotNull Player player)
    {
        super.removed(player);
        // 将合成槽物品优先放入玩家背包 否则掉落
        cleanCraftSlots(firstCraftReturnDir);
    }
}
