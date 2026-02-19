package com.wintercogs.beyonddimensions.Integration.EMI.Recipe;

import com.wintercogs.beyonddimensions.Api.DataBase.Stack.EmptyStackKey;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.IStackKey;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.ItemStackKey;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.KeyAmount;
import com.wintercogs.beyonddimensions.Menu.DimensionsCraftMenu;
import com.wintercogs.beyonddimensions.Menu.Slot.AbstractStackTypedSlot;
import com.wintercogs.beyonddimensions.Network.Packet.toServer.RecipeFillC2SPacket;
import com.wintercogs.beyonddimensions.Registry.PacketRegister;
import dev.emi.emi.api.recipe.EmiPlayerInventory;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.VanillaEmiRecipeCategories;
import dev.emi.emi.api.recipe.handler.EmiCraftContext;
import dev.emi.emi.api.recipe.handler.StandardRecipeHandler;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

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
        List<EmiStack> stacks = getInputSources(screen.getMenu()).stream().map(Slot::getItem).map(EmiStack::of).collect(Collectors.toCollection(ArrayList::new));
        if (screen.getMenu().storage.getStorage() != null)
        {
            for (KeyAmount stack : screen.getMenu().storage.getStorage())
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
        // 将屏幕切回去，保持与原逻辑一致
        Minecraft.getInstance().setScreen(context.getScreen());

        // 1) 取配方与菜单
        final List<EmiIngredient> inputs = recipe.getInputs();
        final T menu = context.getScreen().getMenu();

        // 2) 收集可用物品来源：合成输入槽 + 存储 + 玩家背包
        //    用 Map<Item, List<Avail>> 归并，按 Item 聚合数量，避免创建/复制 ItemStack
        final Map<Item, List<Avail>> pool = new HashMap<>();

        // 简单的加入函数
        final java.util.function.BiConsumer<ItemStackKey, Long> add = (key, amt) -> {
            if (amt == null || amt <= 0) return;
            pool.computeIfAbsent(key.getSource(), i -> new ArrayList<>())
                    .add(new Avail(key, amt));
        };

        // 合成输入槽
        for (Slot slot : getInputSources(menu))
        {
            if (slot.hasItem())
            {
                ItemStack s = slot.getItem();
                add.accept(new ItemStackKey(s), (long) s.getCount());
            }
        }

        // 存储（只接受 ItemStackKey）
        for (KeyAmount ka : menu.storage.getStorage())
        {
            if (ka == null || ka.isEmpty()) continue;
            if (ka.key() instanceof ItemStackKey isk)
            {
                add.accept(isk, ka.amount());
            }
        }

        // 玩家背包
        for (ItemStack s : menu.player.getInventory().items)
        {
            if (!s.isEmpty())
            {
                add.accept(new ItemStackKey(s), (long) s.getCount());
            }
        }

        // 3) 逐一匹配配方输入，构建要发送的 keys & amounts（顺序即为槽位）
        final ArrayList<IStackKey<?>> outKeys = new ArrayList<>(inputs.size());
        final ArrayList<Long> outAmts = new ArrayList<>(inputs.size());

        for (EmiIngredient ing : inputs)
        {

            // 空位：放空键
            if (ing.isEmpty())
            {
                outKeys.add(EmptyStackKey.INSTANCE);
                outAmts.add(0L);
                continue;
            }

            final int required = (int) ing.getAmount();
            boolean satisfied = false;

            // 该 Ingredient 可能有多个可选（如标签展开后的多种物品），择一满足的
            for (EmiStack alt : ing.getEmiStacks())
            {
                final Item candidateItem = alt.getItemStack().getItem();
                final List<Avail> list = pool.get(candidateItem);
                if (list == null || list.isEmpty()) continue;

                long available = 0;
                for (Avail a : list) available += Math.max(0L, a.remain);
                if (available < required) continue;

                // 选择一个代表性 Key：优先选 remain>0 的那一个
                ItemStackKey repKey = null;
                for (Avail a : list)
                {
                    if (a.remain > 0)
                    {
                        repKey = a.key;
                        break;
                    }
                }
                if (repKey == null) continue; // 理论上不会发生，因为 available>=required

                // 在池中扣减数量（客户端本地模拟，不触碰真实物品）
                int left = required;
                for (Avail a : list)
                {
                    if (left <= 0) break;
                    long take = Math.min(a.remain, left);
                    if (take > 0)
                    {
                        a.remain -= take;
                        left -= (int) take;
                    }
                }

                // 写入该槽位的键与数量
                outKeys.add(repKey);
                outAmts.add((long) required);
                satisfied = true;
                break; // 该槽位已满足，进入下一个槽位
            }

            if (!satisfied)
            {
                // 材料不足
                Minecraft.getInstance().player.displayClientMessage(
                        Component.translatable("beyonddimensions.message.insufficient_materials"),
                        true
                );
                return true; // 按原语义：返回 true 以让 UI 流程继续
            }
        }

        // 4) 发送到服务端：新的包体 (List<IStackKey<?>>, List<Long>)
        PacketRegister.INSTANCE.sendToServer(new RecipeFillC2SPacket(outKeys, outAmts));

        return true;
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
