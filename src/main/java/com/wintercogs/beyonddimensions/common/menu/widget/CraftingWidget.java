package com.wintercogs.beyonddimensions.common.menu.widget;

import com.wintercogs.beyonddimensions.api.menu.widget.BatchCraftWidget;
import com.wintercogs.beyonddimensions.api.menu.widget.MenuWidget;
import com.wintercogs.beyonddimensions.api.menu.widget.RecipeFillWidget;
import com.wintercogs.beyonddimensions.api.menu.workbench.PlayerWorkbenches;
import com.wintercogs.beyonddimensions.api.menu.workbench.StorageWorkbenchTypes;
import com.wintercogs.beyonddimensions.api.storage.handler.IStackHandler;
import com.wintercogs.beyonddimensions.api.storage.key.IStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.KeyAmount;
import com.wintercogs.beyonddimensions.api.storage.key.impl.ItemStackKey;
import com.wintercogs.beyonddimensions.common.init.BDDataComponents;
import com.wintercogs.beyonddimensions.common.init.BDItems;
import com.wintercogs.beyonddimensions.common.item.MatterCompressionBall;
import com.wintercogs.beyonddimensions.common.menu.BDBaseMenu;
import com.wintercogs.beyonddimensions.common.menu.widget.slot.AutoRefillResultSlot;
import com.wintercogs.beyonddimensions.integration.ModPresence;
import com.wintercogs.beyonddimensions.integration.OtherModIds;
import com.wintercogs.beyonddimensions.integration.module.polymorph.PolymorphHelper;
import com.wintercogs.beyonddimensions.util.InventoryHelper;
import dev.compixel.slots.SlotTransferRoutes;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.connection.ConnectionType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 合成区：一个产物槽与 3×3 的合成格。服务端的合成格就是玩家自己保留在 {@link PlayerWorkbenches} 里的那份，
 * 上次保留的物品随之出现；物品退回 {@code storage} 或背包
 */
public class CraftingWidget extends MenuWidget implements BatchCraftWidget, RecipeFillWidget
{
    private static final int MAX_MATTER_BALL_BYTES = 1536 * 1024;

    private final Player player;
    private final IStackHandler storage;
    private final TransientCraftingContainer craftSlots;
    private final ResultContainer resultSlots = new ResultContainer();

    // 扣除一次合成的原料时合成格会逐格改动，扣完之后才重新匹配配方
    private boolean consumingIngredients;
    // 连续合成时沿用的配方：每次合成后先试它，产物等这一批结束后再同步给客户端
    private @Nullable RecipeHolder<CraftingRecipe> batchRecipe;

    public CraftingWidget(BDBaseMenu menu, IStackHandler storage)
    {
        super(menu);
        this.player = menu.player;
        this.storage = storage;
        craftSlots = player.level().isClientSide()
                ? new TransientCraftingContainer(menu, 3, 3)
                : new TransientCraftingContainer(menu, 3, 3, PlayerWorkbenches.of(player).items(StorageWorkbenchTypes.CRAFTING, 9));
        addSlot(new AutoRefillResultSlot(this, player, craftSlots, resultSlots, 0));
        for (int i = 0; i < 9; ++i)
            addSlot(new Slot(craftSlots, i, 0, 0));
        // 保留下来的物品可能正好组成配方
        updateResult(null, true);
    }

    /**
     * 退回物品与补充原料用的存储
     */
    public IStackHandler storage()
    {
        return storage;
    }

    /**
     * 产物槽的编号
     */
    public int resultSlot()
    {
        return start();
    }

    /**
     * 合成格第一个槽位的编号
     */
    public int gridStart()
    {
        return start() + 1;
    }

    /**
     * 合成格最后一个槽位的编号加一
     */
    public int gridEnd()
    {
        return end();
    }

    @Override
    public boolean isResultSlot(int index)
    {
        return index == resultSlot();
    }

    @Override
    public void addQuickMoveRoutes(SlotTransferRoutes.Builder routes)
    {
        routes.group("crafting", start(), end()).route("crafting", "storage");
    }

    @Override
    public boolean ownsContainer(@NotNull Container container)
    {
        return container == craftSlots;
    }

    @Override
    public void slotsChanged(@NotNull Container container)
    {
        if (consumingIngredients)
            return;
        menu.broadcastChanges();
        updateResult(batchRecipe, batchRecipe == null);
    }

    @Override
    public boolean canTakeForPickAll(@NotNull Slot slot)
    {
        return slot.container != resultSlots;
    }

    /**
     * 按合成格重新匹配配方，结果放进产物槽。
     *
     * @param hint 先试的配方，合成格仍然符合它时不再查找
     * @param send 是否立即把产物发给客户端；不发时由之后的同步带上
     */
    private void updateResult(@Nullable RecipeHolder<CraftingRecipe> hint, boolean send)
    {
        Level level = player.level();
        if (level.isClientSide())
            return;
        CraftingInput input = craftSlots.asCraftInput();
        ServerPlayer serverPlayer = (ServerPlayer) player;
        ItemStack result = ItemStack.EMPTY;
        Optional<RecipeHolder<CraftingRecipe>> recipe = hint != null && !input.isEmpty() && hint.value().matches(input, level)
                ? Optional.of(hint) : getRecipe(player, input, level);
        if (recipe.isPresent() && resultSlots.setRecipeUsed(level, serverPlayer, recipe.get()))
        {
            ItemStack assembled = recipe.get().value().assemble(input, level.registryAccess());
            if (assembled.isItemEnabled(level.enabledFeatures()))
                result = assembled;
        }
        resultSlots.setItem(0, result);
        if (send)
        {
            menu.setRemoteSlot(resultSlot(), result);
            serverPlayer.connection.send(new ClientboundContainerSetSlotPacket(menu.containerId, menu.incrementStateId(), resultSlot(), result));
        }
    }

    public static Optional<RecipeHolder<CraftingRecipe>> getRecipe(Player player, CraftingInput input, Level level)
    {
        if (ModPresence.isLoaded(OtherModIds.POLYMORPH) && player != null)
            return PolymorphHelper.getRecipe(player, RecipeType.CRAFTING, input, level);
        return level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level);
    }

    // 合成格仍然符合这份合成配方时返回它，否则为 null
    private @Nullable RecipeHolder<CraftingRecipe> matching(@Nullable RecipeHolder<?> recipe, CraftingInput input)
    {
        return recipe != null && recipe.value() instanceof CraftingRecipe crafting && crafting.matches(input, player.level())
                ? new RecipeHolder<>(recipe.id(), crafting) : null;
    }

    /**
     * 合成一次后留在合成格各位置的剩余物，按产出这份产物的配方计算；它已不符合合成格时重新查找
     */
    public NonNullList<ItemStack> remainingItems(@Nullable RecipeHolder<?> recipe, CraftingInput input)
    {
        RecipeHolder<CraftingRecipe> crafting = matching(recipe, input);
        return crafting != null ? crafting.value().getRemainingItems(input)
                : player.level().getRecipeManager().getRemainingItemsFor(RecipeType.CRAFTING, input, player.level());
    }

    /**
     * 扣除一次合成的原料：其间合成格逐格改动，不随每一格重新匹配配方，扣完后匹配一次
     */
    public void consumeIngredients(Runnable consume)
    {
        consumingIngredients = true;
        try
        {
            consume.run();
        }
        finally
        {
            consumingIngredients = false;
        }
        updateResult(batchRecipe, batchRecipe == null);
    }

    /**
     * 连续合成：其间每次从产物槽取出都会合成一份，先试这一批的配方，产物在结束后同步一次
     */
    @Override
    public void craftBatch(Runnable crafts)
    {
        batchRecipe = matching(resultSlots.getRecipeUsed(), craftSlots.asCraftInput());
        try
        {
            crafts.run();
        }
        finally
        {
            batchRecipe = null;
            updateResult(null, true);
        }
    }

    @Override
    public void clearItems(boolean toStorageFirst)
    {
        cleanGrid(toStorageFirst);
    }

    @Override
    public List<Slot> inputSlots()
    {
        return menu.slots.subList(gridStart(), gridEnd());
    }

    /**
     * 清空合成格：{@code toStorageFirst} 时先退回存储，否则先退回背包
     */
    public void cleanGrid(boolean toStorageFirst)
    {
        if (!(player instanceof ServerPlayer))
            return;
        for (ItemStack stack : craftSlots.getItems())
        {
            if (!stack.isEmpty())
                returnStack(player, storage, stack.copy(), toStorageFirst);
        }
        craftSlots.clearContent();
        resultSlots.clearContent();
    }

    /**
     * 把一堆物品还给玩家：按偏好依次送回存储与背包，最后掉在玩家脚下；
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
     * 关闭菜单：保留时合成格里的物品留在 {@link PlayerWorkbenches}，否则按退回方向清空。
     * 玩家断线时也留在合成格里：玩家数据在菜单关闭之前就保存了，物品已经在那份数据里，再退回就多出一份
     */
    @Override
    public void removed(@NotNull Player player)
    {
        if (!(player instanceof ServerPlayer serverPlayer))
            return;
        PlayerWorkbenches workbenches = PlayerWorkbenches.of(player);
        if (workbenches.keep() || serverPlayer.hasDisconnected())
            resultSlots.clearContent();
        else
            cleanGrid(workbenches.returnToStorage());
    }

    /**
     * 按配方查看器给出的原料填充合成格：先清空合成格，原料先从背包取，再从存储取。
     * 原料多于九格且允许打包时，全部装进一个压缩物质球
     */
    @Override
    public boolean fillRecipe(List<IStackKey<?>> inputKeys, List<Long> amount, boolean compressOverflow, @Nullable ResourceLocation recipe)
    {
        // 请求来自客户端，先完整校验再触碰任何物品
        if (player.level().isClientSide() || player.containerMenu != menu || !player.isAlive() || player.isSpectator() || !menu.stillValid(player))
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
            // 套到上限的物质球不能再打包进新的物质球
            if (packed && need > 0 && !MatterCompressionBall.canBePacked(key))
                return false;
        }
        if (packed && !fitsInMatterBall(inputKeys, amount))
        {
            // 请求有效但不能照做：把原因告诉玩家并视为已处理
            player.displayClientMessage(Component.translatable("msg.beyonddimensions.matter_compress_ball.too_large"), false);
            return true;
        }

        cleanGrid(PlayerWorkbenches.of(player).returnToStorage());

        if (packed)
        {
            transferRecipeToMatterBall(inputKeys, amount);
            return true;
        }

        final int limit = Math.min(craftSlots.getContainerSize(), inputKeys.size());
        for (int i = 0; i < limit; i++)
        {
            long needL = amount.get(i);
            IStackKey<?> key = inputKeys.get(i);

            if (!(key instanceof ItemStackKey itemStackKey) || needL <= 0) continue;

            int need = (int) Math.min(itemStackKey.getVanillaMaxStackSize(), needL);

            // 只有实际转移时才复制堆叠，且槽位数量有限，整体性能可控
            int remaining = extractFromInventory(player.getInventory(), itemStackKey.copyStack(), need);
            if (remaining > 0) remaining = extractFromStorage(storage, itemStackKey, remaining);

            int got = need - remaining;
            if (got > 0) craftSlots.setItem(i, itemStackKey.copyStackWithCount(got));
        }
        return true;
    }

    // 过大的压缩物质球会让持有它和它附近的玩家断线，编码超出上限的内容不予打包
    private boolean fitsInMatterBall(List<IStackKey<?>> inputKeys, List<Long> amount)
    {
        List<KeyAmount> contents = new ArrayList<>(inputKeys.size());
        for (int i = 0; i < inputKeys.size(); i++)
        {
            contents.add(new KeyAmount(inputKeys.get(i), amount.get(i)));
        }
        ItemStack preview = new ItemStack(BDItems.MATTER_COMPRESS_BALL.get());
        preview.set(BDDataComponents.ISTACK_SLOTS, contents);
        ByteBuf bytes = Unpooled.buffer(256, MAX_MATTER_BALL_BYTES);
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

    // 从背包主槽位取出物品，返回还差多少
    static int extractFromInventory(Inventory inventory, ItemStack template, int amount)
    {
        int remaining = amount;
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

    // 从存储取出物品，返回还差多少
    static int extractFromStorage(IStackHandler storage, IStackKey<?> type, int amount)
    {
        KeyAmount extraction = storage.extract(type, amount, false, false);
        return extraction.amount() > 0 ? amount - (int) extraction.amount() : amount;
    }
}
