package com.wintercogs.beyonddimensions.common.menu;

import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import com.wintercogs.beyonddimensions.api.storage.handler.IStackHandler;
import com.wintercogs.beyonddimensions.api.storage.handler.impl.AbstractUnorderedStackHandler;
import com.wintercogs.beyonddimensions.api.storage.handler.impl.UnorderedStackHandlerRemoveZero;
import com.wintercogs.beyonddimensions.api.storage.key.IStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.KeyAmount;
import com.wintercogs.beyonddimensions.api.storage.key.impl.ItemStackKey;
import com.wintercogs.beyonddimensions.common.init.BDDataComponents;
import com.wintercogs.beyonddimensions.common.init.BDItems;
import com.wintercogs.beyonddimensions.common.menu.sync.BDMenuResources;
import com.wintercogs.beyonddimensions.common.menu.widget.slot.AbstractStackTypedSlot;
import com.wintercogs.beyonddimensions.common.menu.widget.slot.AutoRefillResultSlot;
import com.wintercogs.beyonddimensions.common.menu.widget.slot.DisorderedStackTypedSlot;
import com.wintercogs.beyonddimensions.integration.ModPresence;
import com.wintercogs.beyonddimensions.integration.OtherModIds;
import com.wintercogs.beyonddimensions.integration.module.polymorph.PolymorphHelper;
import com.wintercogs.beyonddimensions.util.InventoryHelper;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
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
import net.neoforged.neoforge.network.connection.ConnectionType;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.ArrayList;
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


    // 构建注册用的信息
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(Registries.MENU, BDConstants.MODID);
    public static final Supplier<MenuType<DimensionsCraftMenu>> Dimensions_Craft_Menu = MENU_TYPES.register("dimensions_craft_menu", () -> IMenuTypeExtension.create(DimensionsCraftMenu::new));


    /**
     * 客户端构造函数
     *
     * @param playerInventory 玩家背包
     */
    public DimensionsCraftMenu(int id, Inventory playerInventory, FriendlyByteBuf data)
    {
        // 客户端函数，故将Net设为临时Net
        this(Dimensions_Craft_Menu.get(), id, playerInventory, new UnorderedStackHandlerRemoveZero(AbstractUnorderedStackHandler.UiTimestampPolicy.NONE));
    }

    /**
     * 服务端构造函数。合成格直接使用玩家自己的 {@link PlayerCraftingGrid}，上次保留的物品随之出现
     *
     * @param playerInventory 玩家背包
     * @param data            维度网络信息，包含了存储信息
     */
    public DimensionsCraftMenu(MenuType<?> type, int id, Inventory playerInventory, AbstractUnorderedStackHandler data)
    {
        // 利用父类函数处理存储槽位 玩家背包 和一些其他数据添加处理
        super(type, id, playerInventory, data);

        TransientCraftingContainer craftContainer = player.level().isClientSide()
                ? new TransientCraftingContainer(this, 3, 3)
                : new TransientCraftingContainer(this, 3, 3, PlayerCraftingGrid.of(player).items());
        initCraftSlots(playerInventory, craftContainer);
        // 保留下来的物品可能正好组成配方
        slotChangedCraftingGrid(this, player.level(), player, craftSlots, resultSlots, resultSlotIndex);
        commands().crafting(menuSync());
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
        if (!level.isClientSide)
        {
            CraftingInput craftinginput = craftSlots.asCraftInput();
            ServerPlayer serverplayer = (ServerPlayer) player;
            ItemStack itemstack = ItemStack.EMPTY;
            Optional<RecipeHolder<CraftingRecipe>> optional = getRecipe(player, craftinginput, level);
            if (optional.isPresent())
            {

                // 原版过程
                RecipeHolder<CraftingRecipe> recipeholder = (RecipeHolder) optional.get();
                CraftingRecipe craftingrecipe = (CraftingRecipe) recipeholder.value();
                if (resultSlots.setRecipeUsed(level, serverplayer, recipeholder))
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

    public static Optional<RecipeHolder<CraftingRecipe>> getRecipe(Player player, CraftingInput input, Level level)
    {
        if (ModPresence.isLoaded(OtherModIds.POLYMORPH) && player != null)
        {
            return PolymorphHelper.getRecipe(player, RecipeType.CRAFTING, input, level);
        }
        return level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level);
    }

    public void transferRecipe(List<IStackKey<?>> inputKeys, List<Long> amount)
    {
        transferRecipe(inputKeys, amount, false);
    }

    public boolean transferRecipe(List<IStackKey<?>> inputKeys, List<Long> amount, boolean compressOverflow)
    {
        // 请求来自客户端，先完整校验再触碰任何物品
        if (player.level().isClientSide() || player.containerMenu != this || !player.isAlive() || player.isSpectator() || !stillValid(player))
            return false;
        if (inputKeys == null || amount == null || inputKeys.size() != amount.size() || inputKeys.size() > 4096)
            return false;
        boolean packed = compressOverflow && inputKeys.size() > craftSlots.getContainerSize();
        if (!packed && inputKeys.size() > craftSlots.getContainerSize())
            return false;
        for (int i = 0; i < inputKeys.size(); i++)
        {
            IStackKey<?> key = inputKeys.get(i);
            Long need = amount.get(i);
            if (key == null || need == null || need < 0)
                return false;
            // 放入合成格的只能是物品；打包进物质压缩球时才允许其他资源
            if (!packed && need > 0 && !key.isEmpty() && !(key instanceof ItemStackKey))
                return false;
        }
        if (packed && !fitsInMatterBall(inputKeys, amount))
            return false;

        // 清空工艺槽物品
        cleanCraftSlots(PlayerCraftingGrid.of(player).returnToStorage());

        if (compressOverflow && inputKeys.size() > craftSlots.getContainerSize())
        {
            transferRecipeToMatterBall(inputKeys, amount);
            return true;
        }

        final int limit = Math.min(craftSlots.getContainerSize(), inputKeys.size());
        for (int i = 0; i < limit; i++)
        {
            long needL = (i < amount.size() ? amount.get(i) : 0L);
            IStackKey<?> key = inputKeys.get(i);

            if (!(key instanceof ItemStackKey itemStackKey) || needL <= 0) continue;

            int need = (int) Math.min(itemStackKey.getVanillaMaxStackSize(), needL);

            // 这里只有实际执行转移时才会调用copy，且槽位数量有限，整体性能可控
            int remaining = extractFromInventory(player.getInventory(), itemStackKey.copyStack(), need);
            if (remaining > 0) remaining = extractFromStorage(storage, itemStackKey, remaining);

            int got = need - remaining;
            if (got > 0) craftSlots.setItem(i, itemStackKey.copyStackWithCount(got));
        }
        return true;
    }

    // 物质压缩球需要能随物品同步给客户端，编码超出上限的内容不予打包
    private boolean fitsInMatterBall(List<IStackKey<?>> inputKeys, List<Long> amount)
    {
        List<KeyAmount> contents = new ArrayList<>(inputKeys.size());
        for (int i = 0; i < inputKeys.size(); i++)
        {
            contents.add(new KeyAmount(inputKeys.get(i), amount.get(i)));
        }
        ItemStack preview = new ItemStack(BDItems.MATTER_COMPRESS_BALL.get());
        preview.set(BDDataComponents.ISTACK_SLOTS, contents);
        ByteBuf bytes = Unpooled.buffer(256, BDMenuResources.MAX_NATIVE_BYTES);
        try
        {
            ItemStack.OPTIONAL_STREAM_CODEC.encode(new RegistryFriendlyByteBuf(bytes, player.registryAccess(), ConnectionType.NEOFORGE), preview);
            return true;
        }
        catch (RuntimeException tooLarge)
        {
            return false;
        }
        finally
        {
            bytes.release();
        }
    }

    private void transferRecipeToMatterBall(List<IStackKey<?>> inputKeys, List<Long> amount)
    {
        List<KeyAmount> packedContents = new ArrayList<>();
        for (int i = 0; i < inputKeys.size(); i++)
        {
            long needL = i < amount.size() ? amount.get(i) : 0L;
            IStackKey<?> key = inputKeys.get(i);
            if (key == null || key.isEmpty() || needL <= 0L) continue;

            if (key instanceof ItemStackKey itemStackKey)
            {
                int need = (int) Math.min(Integer.MAX_VALUE, needL);
                int remaining = extractFromInventory(player.getInventory(), itemStackKey.copyStack(), need);
                if (remaining > 0) remaining = extractFromStorage(storage, itemStackKey, remaining);

                int extracted = need - remaining;
                if (extracted > 0)
                {
                    packedContents.add(new KeyAmount(itemStackKey, extracted));
                }
            }
            else
            {
                KeyAmount extracted = storage.extract(key, needL, false, false);
                if (!extracted.isEmpty())
                {
                    packedContents.add(extracted);
                }
            }
        }

        if (!packedContents.isEmpty())
        {
            ItemStack matterBall = new ItemStack(BDItems.MATTER_COMPRESS_BALL.get());
            matterBall.set(BDDataComponents.ISTACK_SLOTS, packedContents);
            craftSlots.setItem(0, matterBall);
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
                if (sSlotNum < getColumns() * getLines())
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
            for (ItemStack stack : craftSlots.getItems())
            {
                if (!stack.isEmpty())
                    returnStack(player, storage, stack.copy(), toStorageFirst);
            }
            craftSlots.clearContent();
            resultSlots.clearContent();
        }
    }

    /**
     * 把一堆物品还给玩家：优先存储时依次送回存储、玩家背包，最后掉在玩家脚下；优先背包时先背包再存储。
     * 玩家已经死亡或断开连接时，优先背包的直接掉落。会修改传入的堆叠
     */
    public static void returnStack(Player player, IStackHandler storage, ItemStack stack, boolean toStorageFirst)
    {
        if (toStorageFirst)
        {
            stack.setCount((int) storage.insert(new ItemStackKey(stack), stack.getCount(), false).amount());
            if (!stack.isEmpty())
                InventoryHelper.transferToPlayerInventory(player, stack);
        }
        else if (player.isAlive() && !(player instanceof ServerPlayer serverPlayer && serverPlayer.hasDisconnected()))
        {
            InventoryHelper.transferToPlayerInventory(player, stack);
            if (!stack.isEmpty())
            {
                long remaining = storage.insert(new ItemStackKey(stack), stack.getCount(), false).amount();
                stack.setCount((int) remaining);
            }
        }
        if (!stack.isEmpty())
            player.drop(stack, false);
    }

    /**
     * 关闭菜单：玩家选择保留时，合成格里的物品原样留在 {@link PlayerCraftingGrid}，下次打开任何合成菜单时再出现；否则按退回方向清空
     */
    @Override
    public void removed(@NotNull Player player)
    {
        super.removed(player);
        if (!(player instanceof ServerPlayer))
            return;
        PlayerCraftingGrid grid = PlayerCraftingGrid.of(player);
        if (grid.keep())
            resultSlots.clearContent();
        else
            cleanCraftSlots(grid.returnToStorage());
    }
}
