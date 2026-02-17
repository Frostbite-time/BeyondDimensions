package com.wintercogs.beyonddimensions.Api.DataBase.Stack;

import com.wintercogs.beyonddimensions.Api.DataBase.LongType.ManaType;
import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.Render.IngredientRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

public class ManaStackType extends LongStackType<ManaType>
{

    public static final ResourceLocation ID = ResourceLocation.tryBuild(BeyondDimensions.MODID, "stack_type/mana");
    public static final ManaStackType EMPTY = new ManaStackType(); // 空定义

    public ManaStackType()
    {
        stack = new ManaType(0);
    }

    public ManaStackType(ManaType stack)
    {
        this.stack = stack;
    }

    public ManaStackType(long stackSize)
    {
        this.stack = new ManaType(stackSize);
    }


    @Override
    public ResourceLocation getTypeID()
    {
        return ID;
    }

    @Override
    public String getModId()
    {
        return BeyondDimensions.Botania_ModId;
    }

    @Override
    public IStackKey<ManaType> fromObject(Object key, long amount, CompoundTag dataComponentPatch)
    {
        if (key instanceof ManaType)
        {
            return new ManaStackType(amount);
        }
        return null;
    }

    @Override
    public IStackKey<ManaType> getEmpty()
    {
        return new ManaStackType(0);
    }

    @Override
    public @NotNull ManaType getSource()
    {
        return this.stack;
    }

    @Override
    public ManaType getEmptyStack()
    {
        return new ManaType(0);
    }

    @Override
    public IStackKey<ManaType> copy()
    {
        // copy时将哈希码状态一起带上，最大程度降低hash计算负担
        ManaStackType copy = new ManaStackType(stack.getStackCount());
        copy.NeedRecalHash = this.NeedRecalHash;
        copy.hashCodeCache = this.hashCodeCache;
        return copy;
    }

    @Override
    public IStackKey<ManaType> copyWithCount(long count)
    {
        ManaStackType copy = new ManaStackType(count);
        copy.NeedRecalHash = this.NeedRecalHash;
        copy.hashCodeCache = this.hashCodeCache;
        return copy;
    }

    @Override
    public IStackKey<ManaType> split(long amount)
    {
        if (amount <= 0) return new ManaStackType();

        long splitAmount = Math.min(amount, stack.getStackCount());
        stack.shrink(splitAmount);
        return new ManaStackType(splitAmount);
    }

    @Override
    public boolean hasTag(TagKey<?> tagKey)
    {
        return false;
    }

    @Override
    public long getVanillaMaxStackSize()
    {
        return 1000000L; // 一格一个池子，很合理~
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
    public IStackKey<ManaType> deserialize(FriendlyByteBuf buf, ResourceLocation typeId)
    {
        if (!typeId.equals(getTypeId()))
        {
            return null;// 表示未能读取任何类型
        }
        // 读取数量
        long count = buf.readVarLong();
        return new ManaStackType(count);
    }

    @Override
    public @NotNull CompoundTag serializeNBT()
    {
        CompoundTag tag = new CompoundTag();
        tag.putString("Type", ID.toString());
        tag.putLong("Amount", getStackAmount());
        return tag;
    }

    @Override
    public @NotNull IStackKey<ManaType> deserializeNBT(CompoundTag nbt)
    {
        return new ManaStackType(nbt.getLong("Amount"));
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void render(net.minecraft.client.gui.GuiGraphics gui, int x, int y)
    {
        if (stack.isEmpty())
            return;

        // 渲染图标
        var poseStack = gui.pose(); // 获取渲染的变换矩阵
        poseStack.pushPose(); // 保存矩阵状态

        int tintColor = 0xFFFFFFFF;
        net.minecraft.client.renderer.texture.TextureAtlasSprite sprite = IngredientRenderer.BOTANIA_MANA.sprite();
        com.wintercogs.beyonddimensions.Render.IngredientRenderer.drawTiledSprite(gui, 16, 16, tintColor, 16, sprite, x, y);


        poseStack.popPose(); // 恢复矩阵状态，结束渲染

        // 渲染数量文本
        String countText = getCountText(getStackAmount());
        float scale = 0.666f; // 文本缩放因数
        var poseStackText = gui.pose();
        poseStackText.pushPose();
        poseStackText.translate(0, 0, 200); // 确保文本在顶层
        poseStackText.scale(scale, scale, scale); // 文本整体缩放，便于查看
        com.mojang.blaze3d.systems.RenderSystem.disableBlend(); // 禁用混合渲染模式
        final int X = (int) (
                (x + -1 + 16.0f + 2.0f - Minecraft.getInstance().font.width(countText) * 0.666f)
                        * 1.0f / 0.666f
        );
        final int Y = (int) (
                (y + -1 + 16.0f - 5.0f * 0.666f)
                        * 1.0f / 0.666f
        );
        if (!stack.isEmpty())
            gui.drawString(Minecraft.getInstance().font, countText, X, Y, 0xFFFFFF);
        poseStackText.popPose();
    }

}
