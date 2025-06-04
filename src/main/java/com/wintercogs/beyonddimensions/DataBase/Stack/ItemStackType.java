package com.wintercogs.beyonddimensions.DataBase.Stack;

import com.mojang.blaze3d.systems.RenderSystem;
import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.Unit.BDMath;
import com.wintercogs.beyonddimensions.Unit.StringFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

public class ItemStackType implements IStackType<ItemStack> {
    public static final ResourceLocation ID = ResourceLocation.tryBuild(BeyondDimensions.MODID, "stack_type/item");
    private static final long CUSTOM_MAX_STACK_SIZE = Long.MAX_VALUE; // 自定义堆叠大小

    private ItemStack stack;
    private long stackSize;

    private int hashCodeCache = 0; // 哈希码缓存
    private boolean NeedRecalHash = true; // 指示什么时候需要重新计算哈希

    public ItemStackType()
    {
        stack = ItemStack.EMPTY;
        stackSize = 0;
    }

    public ItemStackType(ItemStack stack)
    {
        this.stack = stack;
        stackSize = stack.getCount();
    }

    public ItemStackType(ItemStack stack, long stackSize)
    {
        this.stack = stack;
        this.stackSize = stackSize;
    }

    @Override
    public IStackType<ItemStack> fromObject(Object key, long amount, CompoundTag dataComponentPatch)
    {
        if(key instanceof Item item)
        {
            ItemStack itemStack;
            if(dataComponentPatch != null)
                itemStack = new ItemStack(item, 1,dataComponentPatch);
            else
                itemStack = new ItemStack(item,1);
            return new ItemStackType(itemStack,amount);
        }
        return null;
    }

    @Override
    public ItemStack getStack()
    {
        stack.setCount(BDMath.clampLongToInt(stackSize));
        return stack;
    }

    @Override
    public void setStack(ItemStack stack)
    {
        this.stack = stack.copy();
        this.stackSize = stack.getCount();
        NeedRecalHash = true;
    }

    @Override
    public ResourceLocation getTypeId() {
        return ID;
    }

    @Override
    public IStackType<ItemStack> getEmpty()
    {
        return new ItemStackType();
    }

    @Override
    public Class<ItemStack> getStackClass() {
        return ItemStack.class;
    }

    @Override
    public Class<?> getSourceClass()
    {
        return Item.class;
    }

    @Override
    public Object getSource()
    {
        return ItemStack.EMPTY.getItem();
    }

    @Override
    public String getModId()
    {
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return key != null ? key.getNamespace() : "unknown";
    }

    @Override
    public boolean isEmpty()
    {
        return stack.isEmpty() || stackSize <= 0;
    }

    @Override
    public boolean isEmptyStack()
    {
        return stack.isEmpty();
    }

    @Override
    public ItemStack getEmptyStack()
    {
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack copyStack()
    {
        return stack.copyWithCount(BDMath.clampLongToInt(stackSize));
    }

    @Override
    public ItemStack copyStackWithCount(long count)
    {
        return stack.copyWithCount(BDMath.clampLongToInt(count));
    }

    @Override
    public IStackType<ItemStack> copy()
    {
        ItemStackType copy = new ItemStackType(stack.copy(),stackSize);
        copy.NeedRecalHash = this.NeedRecalHash;
        copy.hashCodeCache = this.hashCodeCache;
        return copy;
    }

    @Override
    public IStackType<ItemStack> copyWithCount(long count)
    {
        ItemStackType copy = new ItemStackType(stack.copy(),count);
        copy.NeedRecalHash = this.NeedRecalHash;
        copy.hashCodeCache = this.hashCodeCache;
        return copy;
    }

    @Override
    public long getStackAmount()
    {
        return stackSize;
    }

    @Override
    public void setStackAmount(long amount)
    {
        stackSize = amount;
    }

    @Override
    public void grow(long amount)
    {
        setStackAmount(getStackAmount()+amount);
    }

    @Override
    public void shrink(long amount)
    {
        grow(-amount);
    }

    @Override
    public long getVanillaMaxStackSize() {
        // 考虑原版物品的堆叠限制
        return Math.min(stack.getMaxStackSize(), getCustomMaxStackSize());
    }

    @Override
    public long getCustomMaxStackSize() {
        // 可配置化的最大堆叠尺寸
        return CUSTOM_MAX_STACK_SIZE;
    }

    @Override
    public ItemStack splitStack(long amount) {
        if (amount <= 0) return ItemStack.EMPTY;

        int splitAmount = BDMath.clampLongToInt(Math.min(amount, stackSize));
        ItemStack split = stack.copy();
        split.setCount(splitAmount);
        shrink(splitAmount);
        return split;
    }

    @Override
    public IStackType<ItemStack> split(long amount)
    {
        if (amount <= 0) return new ItemStackType();

        long splitAmount = Math.min(amount, stackSize);
        ItemStack split = stack.copy();
        shrink(splitAmount);
        return new ItemStackType(split, splitAmount);
    }

    @Override
    public boolean isSame(IStackType<ItemStack> other) {
        // 比较物品类型和基础NBT（如盔甲耐久等）
        if(!other.getTypeId().equals(this.getTypeId()))
            return false;
        return ItemStack.isSameItem(stack, other.copyStackWithCount(1));
    }

    @Override
    public boolean isSameTypeSameComponents(IStackType<ItemStack> other) {
        if(!other.getTypeId().equals(this.getTypeId()))
            return false;
        return ItemStack.isSameItemSameTags(stack, other.copyStackWithCount(1));
    }

    // 网络序列化
    @Override
    public void serialize(FriendlyByteBuf buf) {
        // 始终写入类型ID
        buf.writeResourceLocation(getTypeId());

        // 写入是否存在物品的标志
        boolean hasItem = !stack.isEmpty();
        buf.writeBoolean(hasItem);

        if (hasItem) {
            // 写入数量
            buf.writeVarLong(stackSize);
            // 使用副本避免修改原堆栈
            ItemStack copy = stack.copyWithCount(1);
            // 使用OPTIONAL_CODEC处理可能为空的情况
            buf.writeItem(copy);
        }
    }

    @Override
    public ItemStackType deserialize(FriendlyByteBuf buf,ResourceLocation typeId) {
        if (!typeId.equals(getTypeId())) {
            return null;// 表示未能读取任何类型
        }

        // 读取是否存在物品的标志
        boolean hasItem = buf.readBoolean();
        if (!hasItem) {
            return new ItemStackType(ItemStack.EMPTY);
        }

        // 读取数量
        long count = buf.readVarLong();
        // 使用OPTIONAL_CODEC解码
        ItemStack stack = buf.readItem();
        return new ItemStackType(stack,count);
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Type", ID.toString());
        tag.putLong("Amount", getStackAmount());
        tag.put("Stack",stack.copyWithCount(1).save(new CompoundTag()));
        return tag;
    }

    @Override
    public ItemStackType deserializeNBT(CompoundTag nbt) {
        ItemStackType stack =  new ItemStackType(ItemStack.of(nbt.getCompound("Stack")));
        stack.setStackAmount(nbt.getLong("Amount"));
        return stack;
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void render(net.minecraft.client.gui.GuiGraphics gui,int x, int y) {
        // 渲染物品图标
        var poseStack = gui.pose(); // 获取渲染的变换矩阵
        poseStack.pushPose(); // 保存矩阵状态
        stack.setCount(1);
        gui.renderFakeItem(stack, x, y);
        gui.renderItemDecorations(Minecraft.getInstance().font, stack, x, y, "");
        poseStack.popPose(); // 恢复矩阵状态，结束渲染

        // 渲染数量文本
        String countText = getCountText(getStackAmount());
        float scale = 0.666f; // 文本缩放因数
        var poseStackText = gui.pose();
        poseStackText.pushPose();
        poseStackText.translate(0,0,200); // 确保文本在顶层
        poseStackText.scale(scale,scale,scale); // 文本整体缩放，便于查看
        RenderSystem.disableBlend(); // 禁用混合渲染模式
        final int X = (int)(
                (x + -1 + 16.0f + 2.0f - Minecraft.getInstance().font.width(countText) * 0.666f)
                        * 1.0f / 0.666f
        );
        final int Y = (int)(
                (y + -1 + 16.0f - 5.0f * 0.666f)
                        * 1.0f / 0.666f
        );
        if(!stack.isEmpty())
            gui.drawString(Minecraft.getInstance().font, countText, X, Y, 0xFFFFFF);
        poseStackText.popPose();
    }

    @Override
    public String getCountText(long count) {
        if (count < 0) return "";
        return StringFormat.formatCount(count);
    }

    @Override
    public Component getDisplayName()
    {
        return stack.getDisplayName();
    }

    @Override
    public List<Component> getTooltipLines(@Nullable Player player, TooltipFlag tooltipFlag)
    {
        List<Component> tooltips = stack.getTooltipLines(player,tooltipFlag);
        tooltips.add(Component.translatable("istack.beyonddimensions.storage_num.item", getStackAmount()));
        return tooltips;
    }

    @Override
    public Optional<TooltipComponent> getTooltipImage()
    {
        return stack.getTooltipImage();
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void renderTooltip(net.minecraft.client.gui.GuiGraphics gui,net.minecraft.client.gui.Font font, int mouseX, int mouseY)
    {
        var minecraft = Minecraft.getInstance();
        gui.renderTooltip(minecraft.font, this.getTooltipLines(minecraft.player, minecraft.options.advancedItemTooltips ? TooltipFlag.Default.ADVANCED : TooltipFlag.Default.NORMAL)
                , getTooltipImage(), ItemStack.EMPTY, mouseX, mouseY);
    }

    @Override
    public boolean equals(Object other)
    {
        if(other instanceof ItemStackType otherStack)
        {
            return this.isSameTypeSameComponents(otherStack);
        }
        return false;
    }

    @Override
    public int hashCode() {
        // 基于物品类型和组件生成哈希码
        if(NeedRecalHash)
        {
            if (stack != null) {
                int i = 31 + stack.getItem().hashCode();
                if(stack.hasTag())
                    hashCodeCache = i*31 + stack.getTag().hashCode();
                else
                    hashCodeCache =  i;
            } else {
                hashCodeCache = 0;
            }
            NeedRecalHash = false;
        }
        return hashCodeCache;

    }
}

