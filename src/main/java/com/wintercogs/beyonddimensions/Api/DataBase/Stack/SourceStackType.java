package com.wintercogs.beyonddimensions.Api.DataBase.Stack;

import com.mojang.blaze3d.systems.RenderSystem;
import com.wintercogs.beyonddimensions.Api.DataBase.LongType.SourceType;
import com.wintercogs.beyonddimensions.BeyondDimensions;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class SourceStackType extends LongStackType<SourceType>
{
    public static final ResourceLocation ID = ResourceLocation.tryBuild(BeyondDimensions.MODID, "stack_type/source");
    public static final SourceStackType EMPTY = new SourceStackType(); // 空定义

    public SourceStackType()
    {
        stack = new SourceType(0);
    }

    public SourceStackType(SourceType stack)
    {
        this.stack = stack;
    }

    public SourceStackType(long stackSize)
    {
        this.stack = new SourceType(stackSize);
    }

    @Override
    public ResourceLocation getTypeID()
    {
        return ID;
    }

    @Override
    public String getModId()
    {
        return BeyondDimensions.ARS_ModId;
    }

    @Override
    public IStackType<SourceType> fromObject(Object key, long amount, CompoundTag dataComponentPatch)
    {
        if(key instanceof SourceType)
        {
            return new SourceStackType(amount);
        }
        return null;
    }

    @Override
    public IStackType<SourceType> getEmpty()
    {
        return new SourceStackType();
    }

    @Override
    public SourceType getSource()
    {
        return this.stack;
    }

    @Override
    public SourceType getEmptyStack()
    {
        return new SourceType(0);
    }

    @Override
    public IStackType<SourceType> copy()
    {
        // copy时将哈希码状态一起带上，最大程度降低hash计算负担
        SourceStackType copy = new SourceStackType(stack.getStackCount());
        copy.NeedRecalHash = this.NeedRecalHash;
        copy.hashCodeCache = this.hashCodeCache;
        return copy;
    }

    @Override
    public IStackType<SourceType> copyWithCount(long count)
    {
        SourceStackType copy = new SourceStackType(count);
        copy.NeedRecalHash = this.NeedRecalHash;
        copy.hashCodeCache = this.hashCodeCache;
        return copy;
    }

    @Override
    public IStackType<SourceType> split(long amount)
    {
        if (amount <= 0) return new SourceStackType();

        long splitAmount = Math.min(amount, stack.getStackCount());
        stack.shrink(splitAmount);
        return new SourceStackType(splitAmount);
    }

    @Override
    public boolean hasTag(TagKey<?> tagKey)
    {
        return false;
    }

    @Override
    public long getVanillaMaxStackSize()
    {
        return 1000000; //单槽最大100w，即100个罐子或一次创造魔源罐交互
    }

    @Override
    public void serialize(FriendlyByteBuf buf)
    {
        // 始终写入类型ID
        buf.writeResourceLocation(getTypeId()); // 会被deserializeCommon读取，因此deserialize中不要读取它
        // 写入数量
        buf.writeVarLong(stack.getStackCount());
    }

    @Override
    public IStackType<SourceType> deserialize(FriendlyByteBuf buf, ResourceLocation typeId)
    {
        if (!typeId.equals(getTypeId())) {
            return null;// 表示未能读取任何类型
        }
        // 读取数量
        long count = buf.readVarLong();
        return new SourceStackType(count);
    }

    @Override
    public CompoundTag serializeNBT()
    {
        CompoundTag tag = new CompoundTag();
        tag.putString("Type", ID.toString());
        tag.putLong("Amount", getStackAmount());
        return tag;
    }

    @Override
    public IStackType<SourceType> deserializeNBT(CompoundTag nbt)
    {
        return new SourceStackType(nbt.getLong("Amount"));
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void render(net.minecraft.client.gui.GuiGraphics gui, int x, int y)
    {
        if(stack.isEmpty())
            return;

        // 渲染图标
        var poseStack = gui.pose(); // 获取渲染的变换矩阵
        poseStack.pushPose(); // 保存矩阵状态

        int tintColor = 0xFFFFFFFF;
        net.minecraft.client.renderer.texture.TextureAtlasSprite sprite = com.wintercogs.beyonddimensions.Render.IngredientRenderer.ARS_SOURCE.sprite();
        com.wintercogs.beyonddimensions.Render.IngredientRenderer.drawTiledSprite(gui,16,16,tintColor,16,sprite,x,y);


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
}
