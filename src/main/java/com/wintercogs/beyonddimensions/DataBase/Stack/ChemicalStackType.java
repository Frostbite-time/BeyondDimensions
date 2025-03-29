package com.wintercogs.beyonddimensions.DataBase.Stack;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.Render.IngredientRenderer;
import com.wintercogs.beyonddimensions.Unit.StringFormat;
import mekanism.api.gas.Gas;
import mekanism.api.gas.GasRegistry;
import mekanism.api.gas.GasStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fluids.Fluid;
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

    // 创建空stack
    public ChemicalStackType()
    {
        stack = new GasStack(GasRegistry.getGas(0),0);
    }

    // 创建给定stack
    public ChemicalStackType(GasStack stack)
    {
        this.stack = stack;
    }

    @Override
    public IStackType<GasStack> fromObject(Object key, long amount,int meta, NBTTagCompound dataComponentPatch)
    {
        if(key instanceof Gas chemical)
        {
            GasStack chemicalStack = new GasStack(chemical, (int) amount);
            return new ChemicalStackType(chemicalStack);
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
        return stack;
    }

    @Override
    public void setStack(GasStack stack)
    {
        this.stack = stack.copy();
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
        return stack.amount <= 0 || stack.getGas() == null;
    }

    @Override
    public GasStack getEmptyStack()
    {
        return new GasStack(GasRegistry.getGas(0),0);
    }

    @Override
    public GasStack copyStack()
    {
        return stack.copy();
    }

    @Override
    public GasStack copyStackWithCount(long count)
    {
        return new GasStack(stack.getGas(), (int)count);
    }

    @Override
    public IStackType<GasStack> copy()
    {
        return new ChemicalStackType(stack.copy());
    }

    @Override
    public IStackType<GasStack> copyWithCount(long count)
    {
        return new ChemicalStackType(new GasStack(stack.getGas(), (int)count));
    }

    @Override
    public long getStackAmount()
    {
        return stack.amount;
    }

    @Override
    public void setStackAmount(long amount)
    {
        stack.amount = (int) amount;
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
        return 64000;
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
        long splitAmount = Math.min(amount, stack.amount);
        GasStack split = stack.copy();
        split.amount = (int) splitAmount;
        stack.amount -= (int)splitAmount;
        return split;
    }

    @Override
    public IStackType<GasStack> split(long amount)
    {
        if (amount <= 0) return new ChemicalStackType();

        // 计算可分割的数量
        long splitAmount = Math.min(amount, stack.amount);
        GasStack split = stack.copy();
        split.amount = (int) splitAmount;
        stack.amount -= (int)splitAmount;
        return new ChemicalStackType(split);
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
        stack.amount = (int)count;
        return new ChemicalStackType(stack);
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
            String countText = getCountText(gasStack.amount);
            float scale = 0.666f;

            GlStateManager.pushMatrix();
            GlStateManager.translate(0, 0, 200);  // 确保文本在顶层
            GlStateManager.scale(scale, scale, 1);

            // 计算位置（根据缩放系数调整）
            int textX = (int) ((x + 16 - 2 - Minecraft.getMinecraft().fontRenderer.getStringWidth(countText) * scale) / scale);
            int textY = (int) ((y + 16 - 8) / scale);

            Minecraft.getMinecraft().fontRenderer.drawString(
                    countText,
                    textX,
                    textY,
                    0xFFFFFF,
                    true
            );

            GlStateManager.popMatrix();
        }
    }

    @Override
    public String getCountText(long count)
    {
        if (count <= 0) return "";
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

        tooltips.add("Mekanism");

        tooltips.add("已存储:"+getStackAmount()+"mB");
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
        int code = 1;
        code = 31 * code + stack.hashCode();
        return code;
    }
}
