package com.wintercogs.beyonddimensions.Integration.EMI.Recipe;

import com.wintercogs.beyonddimensions.Api.DataBase.Stack.EmptyStackKey;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.IStackKey;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.ItemStackKey;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.KeyAmount;
import com.wintercogs.beyonddimensions.Api.config.CommonConfigRuntime;
import com.wintercogs.beyonddimensions.Menu.DimensionsCraftMenu;
import com.wintercogs.beyonddimensions.Menu.Slot.AbstractStackTypedSlot;
import com.wintercogs.beyonddimensions.Packet.RecipeFillC2SPacket;
import dev.emi.emi.api.recipe.EmiPlayerInventory;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.VanillaEmiRecipeCategories;
import dev.emi.emi.api.recipe.handler.EmiCraftContext;
import dev.emi.emi.api.recipe.handler.StandardRecipeHandler;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.Widget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class NetRecipeHandler<T extends DimensionsCraftMenu> implements StandardRecipeHandler<T>
{

    @Override
    public List<Slot> getInputSources(T handler)
    {
        List<Slot> inputSlots = new ArrayList<>();
        for (Slot slot : handler.slots)
        {
            if (!(slot instanceof ResultSlot) && !(slot instanceof AbstractStackTypedSlot))
            {
                inputSlots.add(slot);
            }
        }
        return inputSlots;
    }

    @Override
    public List<Slot> getCraftingSlots(T handler)
    {
        List<Slot> craftingSlots = new ArrayList<>();
        for (int i = handler.craftSlotStartIndex; i < handler.craftSlotEndIndex; ++i)
        {
            craftingSlots.add(handler.slots.get(i));
        }
        return craftingSlots;
    }

    @Override
    public boolean supportsRecipe(EmiRecipe recipe)
    {
        return recipe.getCategory() == VanillaEmiRecipeCategories.CRAFTING && recipe.supportsRecipeTree();
    }

    @Override
    public EmiPlayerInventory getInventory(AbstractContainerScreen<T> screen)
    {
        return collectInventory(screen.getMenu(), CommonConfigRuntime.emiAllowNetworkStorageInfo);
    }

    @Override
    public boolean canCraft(EmiRecipe recipe, EmiCraftContext<T> context)
    {
        if (Screen.hasShiftDown()) return true;

        EmiPlayerInventory actualInventory = collectInventory(context.getScreen().getMenu(), true);
        return actualInventory.canCraft(recipe);
    }

    @Override
    public void render(EmiRecipe recipe, EmiCraftContext<T> context, List<Widget> widgets, GuiGraphics draw)
    {
        EmiPlayerInventory actualInventory = collectInventory(context.getScreen().getMenu(), true);
        StandardRecipeHandler.renderMissing(recipe, actualInventory, widgets, draw);
    }

    private EmiPlayerInventory collectInventory(T menu, boolean includeStorage)
    {
        List<EmiStack> stacks = getInputSources(menu).stream().map(Slot::getItem).map(EmiStack::of).collect(Collectors.toCollection(ArrayList::new));
        if (includeStorage && menu.storage.getStorage() != null)
        {
            for (KeyAmount stack : menu.storage.getStorage())
            {
                if (stack.isEmpty()) continue;
                if (stack.key() instanceof ItemStackKey itemStackKey)
                {
                    stacks.add(EmiStack.of(itemStackKey.getReadOnlyStack(), stack.amount()));
                }
            }
        }
        return new EmiPlayerInventory(stacks);
    }

    @Override
    public boolean craft(EmiRecipe recipe, EmiCraftContext<T> context)
    {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.setScreen(context.getScreen());
        final boolean forcePartialRequest = Screen.hasShiftDown();

        // 1) 取配方与菜单
        final List<EmiIngredient> inputs = recipe.getInputs();
        final T menu = context.getScreen().getMenu();

        final Map<Item, List<Avail>> pool = new HashMap<>();

        for (Slot slot : getInputSources(menu))
        {
            if (slot.hasItem())
            {
                ItemStack s = slot.getItem();
                addAvail(pool, new ItemStackKey(s), s.getCount());
            }
        }

        for (KeyAmount ka : menu.storage.getStorage())
        {
            if (ka == null || ka.isEmpty()) continue;
            if (ka.key() instanceof ItemStackKey isk)
            {
                addAvail(pool, isk, ka.amount());
            }
        }

        for (ItemStack s : menu.player.getInventory().items)
        {
            if (!s.isEmpty())
            {
                addAvail(pool, new ItemStackKey(s), s.getCount());
            }
        }

        // 逐一匹配配方输入，构建要发送的 keys & amounts（顺序即为槽位）
        final ArrayList<IStackKey<?>> outKeys = new ArrayList<>(inputs.size());
        final ArrayList<Long> outAmts = new ArrayList<>(inputs.size());

        boolean hasMissing = false;
        for (EmiIngredient ing : inputs)
        {

            // 空位：放空键
            if (ing.isEmpty())
            {
                outKeys.add(EmptyStackKey.INSTANCE);
                outAmts.add(0L);
                continue;
            }

            final long required = ing.getAmount();
            boolean satisfied = false;
            ItemStackKey bestRepKey = null;
            long bestAvailable = 0L;
            Avail bestPartialAvail = null;

            for (EmiStack alt : ing.getEmiStacks())
            {
                final Item candidateItem = alt.getItemStack().getItem();
                final List<Avail> list = pool.get(candidateItem);
                if (list == null || list.isEmpty()) continue;

                for (Avail avail : list)
                {
                    if (avail.remain <= 0) continue;

                    long available = avail.remain;
                    if (available > bestAvailable)
                    {
                        bestAvailable = available;
                        bestRepKey = avail.key;
                        bestPartialAvail = avail;
                    }

                    if (available < required) continue;

                    consume(avail, required);
                    outKeys.add(avail.key);
                    outAmts.add(required);
                    satisfied = true;
                    break;
                }

                if (satisfied)
                {
                    break;
                }
            }

            if (!satisfied)
            {
                hasMissing = true;
                if (forcePartialRequest && bestRepKey != null && bestPartialAvail != null && bestAvailable > 0)
                {
                    consume(bestPartialAvail, bestAvailable);
                    outKeys.add(bestRepKey);
                    outAmts.add(bestAvailable);
                }
                else
                {
                    outKeys.add(EmptyStackKey.INSTANCE);
                    outAmts.add(0L);
                }
            }
        }

        Player player = minecraft.player;
        if (hasMissing && !forcePartialRequest && player != null)
        {
            player.displayClientMessage(
                    Component.translatable("beyonddimensions.message.insufficient_materials"),
                    true
            );
            return true;
        }

        // 发包请求物品
        PacketDistributor.sendToServer(new RecipeFillC2SPacket(outKeys, outAmts));

        return true;
    }

    private static void addAvail(Map<Item, List<Avail>> pool, ItemStackKey key, long amount)
    {
        if (amount <= 0) return;
        pool.computeIfAbsent(key.getSource(), i -> new ArrayList<>()).add(new Avail(key, amount));
    }

    private static void consume(Avail avail, long amount)
    {
        long take = Math.min(avail.remain, amount);
        if (take > 0)
        {
            avail.remain -= take;
        }
    }

    /**
     * 本地可用条目：避免拷贝 ItemStack，仅记录 Key 与剩余数量
     */
    private static final class Avail
    {
        final ItemStackKey key;
        long remain;

        Avail(ItemStackKey key, long remain)
        {
            this.key = key;
            this.remain = remain;
        }
    }

}
