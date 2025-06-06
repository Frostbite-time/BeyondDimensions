package com.wintercogs.beyonddimensions.DataBase.Stack;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.Render.IngredientRenderer;
import com.wintercogs.beyonddimensions.Unit.BDMath;
import com.wintercogs.beyonddimensions.Unit.StringFormat;
import mekanism.api.gas.Gas;
import mekanism.api.gas.GasRegistry;
import mekanism.api.gas.GasStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.client.config.GuiUtils;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

// 用于处理通用机械的化学品类
public class ChemicalStackType implements IStackType<GasStack>
{

    public static final ResourceLocation ID = new ResourceLocation(BeyondDimensions.MODID, "stack_type/chemical");
    private static final long CUSTOM_MAX_STACK_SIZE = Long.MAX_VALUE; // 自定义堆叠大小

    private GasStack stack;
    private long stackSize;

    private int hashCodeCache = 0; // 哈希码缓存
    private boolean NeedRecalHash = true; // 指示什么时候需要重新计算哈希

    // 创建空stack
    public ChemicalStackType()
    {
        stack = new GasStack(GasRegistry.getGas(0),0);
        stackSize = 0;
    }

    // 创建给定stack
    public ChemicalStackType(GasStack stack)
    {
        this.stack = stack;
        stackSize = stack.amount;
    }

    public ChemicalStackType(GasStack stack, long stackSize)
    {
        this.stack = stack;
        this.stackSize = stackSize;
    }

    @Override
    public IStackType<GasStack> fromObject(Object key, long amount,int meta, NBTTagCompound dataComponentPatch)
    {
        if(key instanceof Gas chemical)
        {
            GasStack chemicalStack = new GasStack(chemical, 1);
            return new ChemicalStackType(chemicalStack, amount);
        }
        return null;
    }

    @Override
    public ResourceLocation getTypeId()
    {
        return this.ID;
    }

    @Override
    public IStackType<GasStack> getEmpty()
    {
        return new ChemicalStackType();
    }

    @Override
    public GasStack getStack()
    {
        stack.amount = BDMath.clampLongToInt(stackSize);
        return stack;
    }

    @Override
    public void setStack(GasStack stack)
    {
        this.stack = stack.copy();
        stackSize = stack.amount;
        NeedRecalHash = true;
    }

    @Override
    public Class<GasStack> getStackClass()
    {
        return GasStack.class;
    }

    @Override
    public Class<?> getSourceClass()
    {
        return Gas.class;
    }

    @Override
    public Object getSource()
    {
        return GasRegistry.getGas(0);
    }

    @Override
    public boolean isEmpty()
    {
        return stack == null || stack.getGas() == null || stackSize<=0;
    }

    @Override
    public boolean isEmptyStack()
    {
        return stack == null || stack.getGas() == null;
    }

    @Override
    public GasStack getEmptyStack()
    {
        return new GasStack(GasRegistry.getGas(0),0);
    }

    @Override
    public GasStack copyStack()
    {
        GasStack copy = stack.copy();
        copy.amount = BDMath.clampLongToInt(stackSize);
        return copy;
    }

    @Override
    public GasStack copyStackWithCount(long count)
    {
        GasStack copy = stack.copy();
        copy.amount = BDMath.clampLongToInt(count);
        return copy;
    }

    @Override
    public IStackType<GasStack> copy()
    {
        ChemicalStackType copy = new ChemicalStackType(stack.copy(), stackSize);
        copy.NeedRecalHash = this.NeedRecalHash;
        copy.hashCodeCache = this.hashCodeCache;
        return copy;
    }

    @Override
    public IStackType<GasStack> copyWithCount(long count)
    {
        ChemicalStackType copy = new ChemicalStackType(stack.copy(), count);
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
    public long getVanillaMaxStackSize()
    {
        // mek化学品同流体，以64桶为原版一个槽的最大单位
        return Math.min(64000L, getCustomMaxStackSize());
    }

    @Override
    public long getCustomMaxStackSize()
    {
        return CUSTOM_MAX_STACK_SIZE;
    }

    @Override
    public GasStack splitStack(long amount)
    {
        if (amount <= 0) return getEmptyStack();

        // 计算可分割的数量
        long splitAmount = BDMath.clampLongToInt(Math.min(amount, stackSize));
        GasStack split = stack.copy();
        split.amount = (int) splitAmount;
        shrink(splitAmount);
        return split;
    }

    @Override
    public IStackType<GasStack> split(long amount)
    {
        if (amount <= 0) return new ChemicalStackType();

        long splitAmount = Math.min(amount, stackSize);
        GasStack split = stack.copy();
        shrink(splitAmount);
        return new ChemicalStackType(split, splitAmount);
    }

    @Override
    public boolean isSame(IStackType<GasStack> other)
    {
        if(!other.getTypeId().equals(this.getTypeId()))
            return false;
        return stack.isGasEqual(other.getStack());
    }

    @Override
    public boolean isSameTypeSameComponents(IStackType<GasStack> other)
    {
        if(!other.getTypeId().equals(this.getTypeId()))
            return false;
        return stack.isGasEqual(other.getStack());
    }

    @Override
    public void serialize(PacketBuffer buf)
    {
        // 始终写入类型ID
        buf.writeResourceLocation(getTypeId());

        // 写入是否存在物品的标志
        boolean hasItem = !isEmpty();
        buf.writeBoolean(hasItem);

        if (hasItem) {
            // 写入数量
            buf.writeVarLong(stack.amount);
            // 使用副本避免修改原堆栈
            GasStack copy = new GasStack(stack.getGas(),1);
            NBTTagCompound tag = copy.write(new NBTTagCompound());
            buf.writeCompoundTag(tag);
        }
    }

    @Override
    public IStackType<GasStack> deserialize(PacketBuffer buf, ResourceLocation typeId)
    {
        if (!typeId.equals(getTypeId())) {
            return null;// 表示未能读取任何类型
        }

        // 读取是否存在物品的标志
        boolean hasItem = buf.readBoolean();
        if (!hasItem) {
            return new ChemicalStackType(getEmptyStack());
        }

        // 读取数量
        long count = buf.readVarLong();
        NBTTagCompound stackNBT;
        try
        {
            stackNBT = buf.readCompoundTag();
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
        GasStack stack = GasStack.readFromNBT(stackNBT);
        return new ChemicalStackType(stack, count);
    }

    @Override
    public NBTTagCompound serializeNBT()
    {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setLong("Amount", getStackAmount());
        tag.setTag("Stack",new GasStack(stack.getGas(),1).write(new NBTTagCompound()));
        return tag;
    }

    @Override
    public IStackType<GasStack> deserializeNBT(NBTTagCompound nbt)
    {
        ChemicalStackType stack =  new ChemicalStackType(GasStack.readFromNBT(nbt.getCompoundTag("Stack")));
        stack.setStackAmount(nbt.getLong("Amount"));
        return stack;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void render(int x, int y)
    {

        GasStack gasStack = this.stack;
        if (gasStack == null&& !isEmpty()) return;

        // 渲染图标
        Gas gas = gasStack.getGas();
        TextureAtlasSprite sprite = gas.getSprite();
        int gasColor = gas.getTint();
        IngredientRenderer.drawTiledSprite(
                16,
                16,
                gasColor,
                16,
                sprite,
                x,
                y
        );

        // 渲染数量文本
        if (gasStack.amount > 0) {
            String countText = getCountText(stackSize);
            float scale = 0.666f;

            GlStateManager.pushMatrix();
            GlStateManager.translate(0, 0, 200);  // 确保文本在顶层
            GlStateManager.scale(scale, scale, 1);

            // 计算位置（根据缩放系数调整）
            final int X = (int)(
                    (x + -2 + 16.0f + 2.0f - Minecraft.getMinecraft().fontRenderer.getStringWidth(countText) * 0.666f)
                            * 1.0f / 0.666f
            );
            final int Y = (int)(
                    (y + -1 + 16.0f - 5.0f * 0.666f)
                            * 1.0f / 0.666f
            );

            if(!isEmpty())
                Minecraft.getMinecraft().fontRenderer.drawString(countText, X, Y, 0xFFFFFF, true);

            GlStateManager.popMatrix();
        }
    }

    @Override
    public String getCountText(long count)
    {
        if (count < 0) return "";
        return StringFormat.formatCount(count);
    }

    @Override
    public String getDisplayName()
    {
        return stack.getGas().getLocalizedName();
    }

    @Override
    public List<String> getTooltipLines(@Nullable EntityPlayer player, ITooltipFlag tooltipFlag)
    {
        if(isEmpty())
            return new ArrayList<>();

        List<String> tooltips = new ArrayList<>();
        Gas chemical = stack.getGas();

        String displayName = getDisplayName();
        tooltips.add(displayName);


        if (Minecraft.getMinecraft().gameSettings.advancedItemTooltips) {
            String id = chemical.getName();
            if (id != null) {
                tooltips.add(TextFormatting.DARK_GRAY + id);
            }
        }

        tooltips.add(TextFormatting.BLUE.toString() + TextFormatting.ITALIC + "Mekanism");

        tooltips.add(I18n.format("stack.beyonddimensions.stored.mb",stack.amount));
        return tooltips;
    }


    @SideOnly(Side.CLIENT)
    @Override
    public void renderTooltip(int mouseX, int mouseY)
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
                mc.displayWidth,
                mc.displayHeight,
                -1, // 最大宽度（-1表示自动）
                mc.fontRenderer
        );
    }

    @Override
    public boolean equals(Object other)
    {
        if(other instanceof ChemicalStackType otherStack)
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
            int code = 1;
            code = 31 * code + stack.hashCode();
            hashCodeCache = code;
            NeedRecalHash = false;
        }
        return hashCodeCache;
    }
}
