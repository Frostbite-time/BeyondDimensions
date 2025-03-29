package com.wintercogs.beyonddimensions.DataBase.Stack;

import com.cleanroommc.modularui.utils.Color;
import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.Render.IngredientRenderer;
import com.wintercogs.beyonddimensions.Unit.StringFormat;
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
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.client.config.GuiUtils;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.commons.lang3.text.WordUtils;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FluidStackType implements IStackType<FluidStack>
{
    public static final ResourceLocation ID = new ResourceLocation(BeyondDimensions.MODID, "stack_type/fluid");
    private static final long CUSTOM_MAX_STACK_SIZE = Integer.MAX_VALUE; // 自定义堆叠大小

    private FluidStack stack;

    // 创建空stack
    public FluidStackType()
    {
        stack = new FluidStack(FluidRegistry.WATER, 0);
    }

    // 创建给定stack
    public FluidStackType(FluidStack stack)
    {
        this.stack = stack;
    }


    @Override
    public IStackType<FluidStack> fromObject(Object key, long amount,int meta, NBTTagCompound dataComponentPatch)
    {
        if(key instanceof Fluid fluid)
        {
            FluidStack fluidStack = new FluidStack(fluid, (int) amount,dataComponentPatch);
            return new FluidStackType(fluidStack);
        }
        return null;
    }

    @Override
    public ResourceLocation getTypeId()
    {
        return this.ID;
    }

    @Override
    public IStackType<FluidStack> getEmpty()
    {
        return new FluidStackType();
    }

    @Override
    public FluidStack getStack()
    {
        return stack;
    }

    @Override
    public void setStack(FluidStack stack)
    {
        this.stack = stack.copy();
    }

    @Override
    public Class<FluidStack> getStackClass()
    {
        return FluidStack.class;
    }

    @Override
    public Class<?> getSourceClass()
    {
        return Fluid.class;
    }

    @Override
    public Object getSource()
    {
        return FluidRegistry.WATER;
    }

    @Override
    public boolean isEmpty()
    {
        return getStackAmount()<=0;
    }

    @Override
    public FluidStack getEmptyStack()
    {
        return new FluidStack(FluidRegistry.WATER, 0);
    }

    @Override
    public FluidStack copyStack()
    {
        return stack.copy();
    }

    @Override
    public FluidStack copyStackWithCount(long count)
    {
        FluidStack copy = stack.copy();
        // 处理long到int的转换安全
        if (count > Integer.MAX_VALUE) {
            //throw new IllegalArgumentException("ItemStack count exceeds maximum value: " + count);
            copy.amount = Integer.MAX_VALUE;
            return copy;
        }
        copy.amount = (int) count;
        return copy;
    }

    @Override
    public IStackType<FluidStack> copy()
    {
        return new FluidStackType(stack.copy());
    }

    @Override
    public IStackType<FluidStack> copyWithCount(long count)
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
        return new FluidStackType(new FluidStack(stack,copycont));
    }

    @Override
    public long getStackAmount()
    {
        return stack.amount;
    }

    @Override
    public void setStackAmount(long amount)
    {
        // 处理long到int的转换安全
        if (amount > Integer.MAX_VALUE) {
            //throw new IllegalArgumentException("ItemStack count exceeds maximum value: " + count);
            stack.amount = Integer.MAX_VALUE;
            return;
        }
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
        // 流体不属于原版物品，理论上不存在单槽最大上限,此处以64桶为单槽最大单位
        return 64000;
    }

    @Override
    public long getCustomMaxStackSize()
    {
        return CUSTOM_MAX_STACK_SIZE;
    }

    @Override
    public FluidStack splitStack(long amount)
    {
        if (amount <= 0) return getEmptyStack();

        // 计算可分割的数量
        int splitAmount = (int) Math.min(amount, stack.amount);
        FluidStack split = stack.copy();
        split.amount = splitAmount;
        stack.amount -= splitAmount;
        return split;
    }

    @Override
    public IStackType<FluidStack> split(long amount)
    {
        if (amount <= 0) return new FluidStackType();

        // 计算可分割的数量
        int splitAmount = (int) Math.min(amount, stack.amount);
        FluidStack split = stack.copy();
        split.amount = splitAmount;
        stack.amount -= splitAmount;
        return new FluidStackType(split);
    }

    @Override
    public boolean isSame(IStackType<FluidStack> other)
    {
        if(!other.getTypeId().equals(this.getTypeId()))
            return false;
        return stack.getFluid() == other.getStack().getFluid();
    }

    @Override
    public boolean isSameTypeSameComponents(IStackType<FluidStack> other)
    {
        if(!other.getTypeId().equals(this.getTypeId()))
            return false;
        return stack.isFluidEqual(other.getStack());
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
            buf.writeVarInt(stack.amount);
            // 使用副本避免修改原堆栈
            FluidStack copy = new FluidStack(stack,1);
            // 创建NBT并写入
            NBTTagCompound nbt = new NBTTagCompound();
            copy.writeToNBT(nbt); // 调用1.12.2的NBT序列化方法
            buf.writeCompoundTag(nbt);

        }
    }

    @Override
    public IStackType<FluidStack> deserialize(PacketBuffer buf, ResourceLocation typeId)
    {
        if (!typeId.equals(getTypeId())) {
            return null;// 表示未能读取任何类型
        }

        // 读取是否存在物品的标志
        boolean hasItem = buf.readBoolean();
        if (!hasItem) {
            return new FluidStackType(getEmptyStack());
        }

        // 读取数量
        int amount = buf.readVarInt();
        // 读取NBT
        NBTTagCompound nbt;
        try
        {
            nbt = buf.readCompoundTag();
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
        // 构建FluidStack（需要处理无效流体情况）
        Fluid fluid = FluidRegistry.getFluid(nbt.getString("FluidName"));
        if (fluid == null) return new FluidStackType(null);
        FluidStack stack = new FluidStack(fluid, amount, nbt.getCompoundTag("Tag"));
        return new FluidStackType(stack);

    }

    @Override
    public NBTTagCompound serializeNBT()
    {
        NBTTagCompound tag = new NBTTagCompound();

        // 写入数量（1.12.2 FluidStack的amount是int类型）
        tag.setInteger("Amount", stack.amount); // 或使用 tag.setLong("Amount", ...) 如果自定义容量支持long

        // 写入流体堆栈（需处理空值）
        if (stack != null && stack.getFluid() != null) {
            NBTTagCompound fluidTag = new NBTTagCompound();
            stack.writeToNBT(fluidTag); // 1.12.2的writeToNBT直接修改传入的tag
            tag.setTag("Stack", fluidTag);
        } else {
            tag.setTag("Stack", new NBTTagCompound()); // 防止空指针
        }

        return tag;
    }

    @Override
    public IStackType<FluidStack> deserializeNBT(NBTTagCompound nbt)
    {
        // 读取流体堆栈（使用1.12.2专用方法）
        NBTTagCompound stackTag = nbt.getCompoundTag("Stack");
        FluidStack fluidStack = FluidStack.loadFluidStackFromNBT(stackTag);

        // 读取数量（根据实际存储类型选择）
        long amount = nbt.getLong("Amount"); // 如果支持long
        // int amount = nbt.getInteger("Amount"); // 如果使用int

        // 重建对象
        FluidStackType result = new FluidStackType(fluidStack);
        if (fluidStack != null) {
            // 确保不超限（1.12.2 FluidStack的amount是int）
            result.setStackAmount(Math.min(amount, Integer.MAX_VALUE));
        }
        return result;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void render(int x, int y)
    {
        FluidStack fluidStack = this.stack;
        if (fluidStack == null || isEmpty()) return;

        // 渲染图标
        Fluid fluid = fluidStack.getFluid();
        ResourceLocation fluidStill = fluid.getStill(fluidStack);
        TextureAtlasSprite sprite = Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite(fluidStill.toString());
        int fluidColor = fluid.getColor(fluidStack);
        IngredientRenderer.drawTiledSprite(
                16,
                16,
                fluidColor,
                16,
                sprite,
                x,
                y
        );


        // 渲染数量文本
        if (fluidStack.amount > 0) {
            String countText = getCountText(fluidStack.amount);
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
        return stack.getLocalizedName();
    }

    @Override
    public List<String> getTooltipLines(@Nullable EntityPlayer player, ITooltipFlag tooltipFlag)
    {
        // 直接使用全局stack
        if (stack == null) return new ArrayList<>();
        Fluid fluid = stack.getFluid();
        if (fluid == null) return new ArrayList<>();

        List<String> tooltip = new ArrayList<>();
        // 流体显示名称
        tooltip.add(fluid.getLocalizedName(stack));
        // 高级模式显示注册名
        if (Minecraft.getMinecraft().gameSettings.advancedItemTooltips) {
            String id = FluidRegistry.getFluidName(fluid);
            if (id != null) {
                tooltip.add(TextFormatting.DARK_GRAY + id);
            }
        }
        // 获取Mod信息
        String modId = FluidRegistry.getModId(stack);
        if (modId == null) {
            modId = fluid.getStill().getNamespace(); // 备用获取方式
        }
        String modName = "";
        if (modId != null) {
            // 直接内联查找逻辑
            for (ModContainer mod : Loader.instance().getModList()) {
                if (mod.getModId().equalsIgnoreCase(modId) ||
                        mod.getModId().replace("-", "_").equalsIgnoreCase(modId)) {
                    modName = mod.getName();
                    break;
                }
            }
            if (modName.isEmpty()) {
                modName = WordUtils.capitalizeFully(
                        modId.replace('_', ' ').replace('-', ' ')
                );
            }
        }
        tooltip.add(TextFormatting.BLUE.toString() + TextFormatting.ITALIC + modName);
        tooltip.add("已存储: " + stack.amount + "mB");

        return tooltip;
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
        if(other instanceof FluidStackType otherStack)
        {
            return this.isSameTypeSameComponents(otherStack);
        }
        return false;
    }

    @Override
    public int hashCode() {
        // 基于物品类型和组件生成哈希码
        return stack.hashCode();
    }
}
