package com.wintercogs.beyonddimensions.Integration.EMI.Recipe;

import com.wintercogs.beyonddimensions.DataBase.Stack.IStackType;
import com.wintercogs.beyonddimensions.DataBase.Stack.ItemStackType;
import com.wintercogs.beyonddimensions.Menu.DimensionsCraftMenu;
import com.wintercogs.beyonddimensions.Menu.Slot.StoredStackSlot;
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
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class NetRecipeHandler<T extends DimensionsCraftMenu> implements StandardRecipeHandler<T>
{

    @Override
    public List<Slot> getInputSources(T handler)
    {
        List<Slot> inputSlots = new ArrayList<>();
        for(Slot slot : handler.slots)
        {
            if(!(slot instanceof ResultSlot) && !(slot instanceof StoredStackSlot))
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
        for(int i = handler.craftSlotStartIndex; i <= handler.craftSlotEndIndex; ++i)
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
        //return new EmiPlayerInventory(getInputSources(screen.getMenu()).stream().map(Slot::getItem).map(EmiStack::of).toList());
        List<EmiStack> stacks = getInputSources(screen.getMenu()).stream().map(Slot::getItem).map(EmiStack::of).collect(Collectors.toCollection(ArrayList::new));
        if(screen.getMenu().storage.getStorage() != null)
        {
            for(IStackType stackType : screen.getMenu().storage.getStorage())
            {
                if(stackType instanceof ItemStackType itemStackType)
                {
                    if(!itemStackType.isEmpty())
                        stacks.add(EmiStack.of(itemStackType.getStack()));
                }
            }
        }
        return new EmiPlayerInventory(stacks);
    }

    @Override
    public boolean craft(EmiRecipe recipe, EmiCraftContext<T> context)
    {
        //return StandardRecipeHandler.super.craft(recipe, context);
        // 对处理逻辑进行修改，将配方数据以及上下文发送到服务端，在服务端将物品转移到合成栏，然后等待broadcastchange同步
        Minecraft.getInstance().setScreen(context.getScreen());
        // 无论如何返回真，以便UI逻辑进行

        //分为以下几步
        //1.获取配方信息
        //2.根据配方信息在背包和存储中搜寻匹配物作为确定的ItemStack
        //3.将ItemStack整合为数组，发送到服务器端进行处理 其数组排列即为顺序本身
        List<EmiIngredient> inputs = recipe.getInputs();
        List<ItemStack> inputElements = new ArrayList<>(); // 每一个位置放置一个确定的ItemStack
        T menu = context.getScreen().getMenu();

        // 获取所有可能的物品来源（常规合成槽+存储槽）
        List<Slot> craftingSlots = getInputSources(menu);
        List<IStackType> storageSlots = menu.storage.getStorage();
        // 创建虚拟库存用于模拟物品匹配
        List<ItemStack> availableItems = new ArrayList<>();

        // 收集常规合成槽物品（假设是输入槽位）
        for (Slot slot : craftingSlots) {
            if (slot.hasItem()) {
                availableItems.add(slot.getItem().copy());
            }
        }
        // 收集存储槽物品
        for (IStackType stackType : storageSlots) {
            if (stackType instanceof ItemStackType itemStackType) {
                ItemStack stack = itemStackType.getStack();
                if (!stack.isEmpty()) {
                    availableItems.add(stack.copy());
                }
            }
        }
        // 收集背包物品
        for(ItemStack itemStack : menu.player.getInventory().items)
        {
            if(!itemStack.isEmpty())
                availableItems.add(itemStack.copy());
        }
        // 匹配配方输入需求
        for (EmiIngredient ingredient : inputs) {

            if(ingredient.isEmpty())
            {
                inputElements.add(ItemStack.EMPTY);
                continue;
            }

            List<ItemStack> matching = new ArrayList<>();

            // 遍历所有可能的物品匹配
            for (ItemStack stack : availableItems) {
                if (!stack.isEmpty() && ingredient.getEmiStacks().stream()
                        .anyMatch(emiStack -> emiStack.getItemStack().getItem() == stack.getItem())) {
                    matching.add(stack);
                }
            }
            // 计算总数量
            int required = (int)ingredient.getAmount();
            int available = matching.stream().mapToInt(ItemStack::getCount).sum();

            if (available >= required) {
                // 创建合并后的堆栈
                ItemStack merged = new ItemStack(matching.get(0).getItem(), required);
                inputElements.add(merged);

                // 从虚拟库存中扣除（仅客户端模拟）
                int remaining = required;
                for (ItemStack stack : matching) {
                    int deduct = Math.min(remaining, stack.getCount());
                    stack.shrink(deduct);
                    remaining -= deduct;
                    if (remaining <= 0) break;
                }
            } else {
                // 材料不足，提前返回
                Minecraft.getInstance().player.displayClientMessage(
                        Component.translatable("beyonddimensions.message.insufficient_materials"), true);
                return true;
            }
        }
        // 发送网络包（需要实现RecipeFillC2SPacket的序列化）
        PacketRegister.INSTANCE.sendToServer(new RecipeFillC2SPacket(inputElements));

        //服务端处理示意
        //1.解析数组
        //2.为每一个槽位在背包和存储中寻找资源填入


        return true;
    }
}
