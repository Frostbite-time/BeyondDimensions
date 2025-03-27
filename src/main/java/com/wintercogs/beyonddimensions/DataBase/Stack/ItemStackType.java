package com.wintercogs.beyonddimensions.DataBase.Stack;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.Unit.StringFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.config.GuiUtils;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.List;

public class ItemStackType implements IStackType<ItemStack> {
    public static final ResourceLocation ID = new ResourceLocation(BeyondDimensions.MODID, "stack_type/item");
    private static final long CUSTOM_MAX_STACK_SIZE = Integer.MAX_VALUE; // 自定义堆叠大小

    private ItemStack stack;

    public ItemStackType()
    {
        stack = ItemStack.EMPTY;
    }

    public ItemStackType(ItemStack stack)
    {
        this.stack = stack;
    }

    @Override
    public IStackType<ItemStack> fromObject(Object key, long amount, int meta,NBTTagCompound dataComponentPatch)
    {
        if(key instanceof Item item)
        {
            ItemStack itemStack = new ItemStack(item, (int) amount,meta,dataComponentPatch);
            return new ItemStackType(itemStack);
        }
        return null;
    }

    @Override
    public ItemStack getStack()
    {
        return stack;
    }

    @Override
    public void setStack(ItemStack stack)
    {
        this.stack = stack.copy();
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
    public boolean isEmpty() {
        return stack.isEmpty();
    }

    @Override
    public ItemStack getEmptyStack() {
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack copyStack() {
        return stack.copy();
    }

    @Override
    public ItemStack copyStackWithCount(long count) {
        ItemStack copy = stack.copy();
        // 处理long到int的转换安全
        if (count > Integer.MAX_VALUE) {
            //throw new IllegalArgumentException("ItemStack count exceeds maximum value: " + count);
            copy.setCount(Integer.MAX_VALUE);
            return copy;
        }
        copy.setCount((int) count);
        return copy;
    }

    @Override
    public IStackType<ItemStack> copy()
    {
        return new ItemStackType(stack.copy());
    }

    @Override
    public IStackType<ItemStack> copyWithCount(long count)
    {
        int copycont;
        // 处理long到int的转换安全
        if (count > Integer.MAX_VALUE) {
            //throw new IllegalArgumentException("ItemStack count exceeds maximum value: " + count);
            copycont = Integer.MAX_VALUE;
        }
        else
        {
            copycont = (int) count;
        }
        ItemStack copy = stack.copy();
        copy.setCount(copycont);
        return new ItemStackType(copy);
    }

    @Override
    public long getStackAmount() {
        return stack.getCount();
    }

    @Override
    public void setStackAmount(long amount)
    {
        // 处理long到int的转换安全
        if (amount > Integer.MAX_VALUE) {
            //throw new IllegalArgumentException("ItemStack count exceeds maximum value: " + count);
            stack.setCount(Integer.MAX_VALUE);
            return;
        }
        stack.setCount((int) amount);
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

        //stack.getCount()作为min的一极，已经保证了数值范围安全，因为stack.getCount()是一个int
        // 计算可分割的数量
        int splitAmount = (int) Math.min(amount, stack.getCount());
        ItemStack split = stack.copy();
        split.setCount(splitAmount);
        stack.shrink(splitAmount);
        return split;
    }

    @Override
    public IStackType<ItemStack> split(long amount)
    {
        if (amount <= 0) return new ItemStackType();

        //stack.getCount()作为min的一极，已经保证了数值范围安全，因为stack.getCount()是一个int
        // 计算可分割的数量
        int splitAmount = (int) Math.min(amount, stack.getCount());
        ItemStack split = stack.copy();
        split.setCount(splitAmount);
        stack.shrink(splitAmount);
        return new ItemStackType(split);
    }

    @Override
    public boolean isSame(IStackType<ItemStack> other) {
        // 比较物品类型和基础NBT（如盔甲耐久等）
        if(!other.getTypeId().equals(this.getTypeId()))
            return false;
        return ItemStack.areItemsEqual(stack, other.getStack());
    }

    @Override
    public boolean isSameTypeSameComponents(IStackType<ItemStack> other) {
        if(!other.getTypeId().equals(this.getTypeId()))
            return false;
        return ItemStack.areItemsEqual(stack, other.getStack()) && ItemStack.areItemStackTagsEqual(stack, other.getStack());
    }

    // 网络序列化
    @Override
    public void serialize(PacketBuffer buf) {
        // 始终写入类型ID
        buf.writeResourceLocation(getTypeId());

        // 写入是否存在物品的标志
        boolean hasItem = !stack.isEmpty();
        buf.writeBoolean(hasItem);

        if (hasItem) {
            // 写入数量
            buf.writeVarInt(stack.getCount());
            // 使用副本避免修改原堆栈
            ItemStack copy = stack.copy();
            copy.setCount(1);
            // 使用OPTIONAL_CODEC处理可能为空的情况
            buf.writeItemStack(copy);
        }
    }

    @Override
    public ItemStackType deserialize(PacketBuffer buf,ResourceLocation typeId) {
        if (!typeId.equals(getTypeId())) {
            return null;// 表示未能读取任何类型
        }

        // 读取是否存在物品的标志
        boolean hasItem = buf.readBoolean();
        if (!hasItem) {
            return new ItemStackType(ItemStack.EMPTY);
        }

        // 读取数量
        int count = buf.readVarInt();
        // 使用OPTIONAL_CODEC解码
        ItemStack stack = null;
        try
        {
            stack = buf.readItemStack();
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
        stack.setCount(count);
        return new ItemStackType(stack);
    }

    @Override
    public NBTTagCompound serializeNBT() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setLong("Amount", getStackAmount());
        stack.setCount(1);
        tag.setTag("Stack",stack.writeToNBT(new NBTTagCompound()));
        return tag;
    }

    @Override
    public ItemStackType deserializeNBT(NBTTagCompound nbt) {
        ItemStackType stack =  new ItemStackType(new ItemStack(nbt.getCompoundTag("Stack")));
        stack.setStackAmount(nbt.getLong("Amount"));
        return stack;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void render(GuiScreen gui, int x, int y) {
        // 渲染物品图标
        RenderHelper.enableGUIStandardItemLighting();
        gui.mc.getRenderItem().renderItemAndEffectIntoGUI(stack, x, y); // 使用RenderItem渲染物品
        gui.mc.getRenderItem().renderItemOverlayIntoGUI(gui.mc.fontRenderer, stack, x, y, ""); // 渲染默认覆盖层（数量/耐久）
        RenderHelper.disableStandardItemLighting();

        String countText = getCountText(getStackAmount());
        float scale = 0.666f;
        GlStateManager.pushMatrix();
        GlStateManager.translate(0, 0, 200);
        GlStateManager.scale(scale, scale, 1.0f);
        // 计算缩放后的坐标（1.12.2的坐标系缩放逻辑）
        int textWidth = gui.mc.fontRenderer.getStringWidth(countText);
        int scaledX = (int)((x + 16 - textWidth * scale + 2) / scale);
        int scaledY = (int)((y + 16 - 5 * scale) / scale);
        // 直接绘制带阴影的文本（对应原版样式）
        gui.mc.fontRenderer.drawStringWithShadow(
                countText,
                scaledX,
                scaledY,
                0xFFFFFF
        );
        GlStateManager.popMatrix();
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);


    }

    @Override
    public String getCountText(long count) {
        if (count <= 0) return "";
        return StringFormat.formatCount(count);
    }

    @Override
    public String getDisplayName()
    {
        return stack.getDisplayName();
    }

    @Override
    public List<String> getTooltipLines(@Nullable EntityPlayer player, ITooltipFlag tooltipFlag)
    {
        List<String> tooltips = stack.getTooltip(player, tooltipFlag);
        tooltips.add("已存储:"+getStackAmount()+"个");
        return tooltips;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void renderTooltip(GuiScreen gui,int mouseX, int mouseY)
    {
        Minecraft mc = Minecraft.getMinecraft();

        // 获取工具提示文本
        List<String> tooltip = this.getTooltipLines(
                mc.player,
                mc.gameSettings.advancedItemTooltips ? ITooltipFlag.TooltipFlags.ADVANCED : ITooltipFlag.TooltipFlags.NORMAL
        );
        // 渲染工具提示（适配1.12.2的绘制方式）
        GuiUtils.drawHoveringText(
                tooltip,
                mouseX,
                mouseY,
                gui.width, // 使用GUI的完整宽度
                gui.height,
                -1, // 最大宽度（-1表示自动）
                mc.fontRenderer
        );

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
        if (stack != null) {
            int i = 31 + stack.getItem().hashCode();
            if(stack.hasTagCompound())
                return i*31 + stack.getTagCompound().hashCode();
            else
                return i;
        } else {
            return 0;
        }
    }
}

