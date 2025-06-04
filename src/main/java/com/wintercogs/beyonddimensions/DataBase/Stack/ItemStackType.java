package com.wintercogs.beyonddimensions.DataBase.Stack;

import com.mojang.blaze3d.systems.RenderSystem;
import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.Unit.BDMath;
import com.wintercogs.beyonddimensions.Unit.StringFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.ClientTooltipFlag;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

public class ItemStackType implements IStackType<ItemStack> {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(BeyondDimensions.MODID, "stack_type/item");
    private static final long CUSTOM_MAX_STACK_SIZE = Long.MAX_VALUE; // 自定义堆叠大小

    private ItemStack stack; // 物品stack信息，数量最好时刻保持为1
    private long stackSize; // 扩容，需要确保所有存入取出的终点在此

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
    public IStackType<ItemStack> fromObject(Object key, long amount, DataComponentPatch dataComponentPatch)
    {
        if(key instanceof Item item)
        {
            ItemStack itemStack;
            if(dataComponentPatch != null)
                itemStack = new ItemStack(BuiltInRegistries.ITEM.getHolder(BuiltInRegistries.ITEM.getKey(item)).get(), 1,dataComponentPatch);
            else
                itemStack = new ItemStack(BuiltInRegistries.ITEM.getHolder(BuiltInRegistries.ITEM.getKey(item)).get(), 1);
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
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace();
    }

    @Override
    public boolean isEmpty()
    {
        return stack.isEmpty() || stackSize <= 0;
    }

    @Override
    public boolean isEmptyStack() {
        return stack.isEmpty(); // 这就是为什么要保证stack自身存储数为1。因为我没有办法绕过stack的isEmpty检测获得其item
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
    public ItemStack copyStackWithCount(long count) {
        return stack.copyWithCount(BDMath.clampLongToInt(count));
    }

    @Override
    public IStackType<ItemStack> copy()
    {
        // copy时将哈希码状态一起带上，最大程度降低hash计算负担
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
    public long getStackAmount() {
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
        return new ItemStackType(split,splitAmount);
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
        return ItemStack.isSameItemSameComponents(stack, other.copyStackWithCount(1));
    }

    // 网络序列化
    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
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
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, copy);
        }
    }

    @Override
    public ItemStackType deserialize(RegistryFriendlyByteBuf buf,ResourceLocation typeId) {
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
        ItemStack stack = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
        return new ItemStackType(stack,count);
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider levelRegistryAccess) {
        CompoundTag tag = new CompoundTag();
        tag.putString("Type", ID.toString());
        tag.putLong("Amount", getStackAmount());
        tag.put("Stack",stack.copyWithCount(1).saveOptional(levelRegistryAccess));
        return tag;
    }

    @Override
    public ItemStackType deserializeNBT(CompoundTag nbt, HolderLookup.Provider levelRegistryAccess) {
        ItemStackType stack =  new ItemStackType(ItemStack.parseOptional(levelRegistryAccess,nbt.getCompound("Stack")));
        stack.setStackAmount(nbt.getLong("Amount"));
        return stack;
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void render(net.minecraft.client.gui.GuiGraphics gui,int x, int y) {
        // 渲染物品图标
        var poseStack = gui.pose(); // 获取渲染的变换矩阵
        poseStack.pushPose(); // 保存矩阵状态
        stack.setCount(1); // 设置stack数量，而非实际用于操作的stackSize变量
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
    public List<Component> getTooltipLines(Item.TooltipContext tooltipContext, @Nullable Player player, TooltipFlag tooltipFlag)
    {
        List<Component> tooltips = stack.getTooltipLines(tooltipContext,player,tooltipFlag);
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
        gui.renderTooltip(minecraft.font, this.getTooltipLines(Item.TooltipContext.of(minecraft.level),minecraft.player, ClientTooltipFlag.of(minecraft.options.advancedItemTooltips ? TooltipFlag.Default.ADVANCED : TooltipFlag.Default.NORMAL))
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
            hashCodeCache = ItemStack.hashItemAndComponents(stack);
            NeedRecalHash = false;
        }
        return hashCodeCache;
    }
}

