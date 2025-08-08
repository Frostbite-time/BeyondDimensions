package com.wintercogs.beyonddimensions.Api.DataBase.Stack;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.wintercogs.beyonddimensions.Api.DataBase.LongType.XpType;
import com.wintercogs.beyonddimensions.BeyondDimensions;
import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.Optional;

public class XpStackType extends LongStackType<XpType>
{
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(BeyondDimensions.MODID, "stack_type/xp");
    public static final XpStackType EMPTY = new XpStackType();

    public static final MapCodec<XpStackType> TYPE_CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    XpType.CODEC.fieldOf("internal_stack").forGetter(XpStackType::getStack)
            ).apply(instance, XpStackType::new));

    public static final Codec<XpStackType> CODEC = TYPE_CODEC.codec();

    public XpStackType()
    {
        stack = new XpType(0);
    }

    public XpStackType(XpType stack)
    {
        this.stack = stack;
    }

    public XpStackType(long stackSize)
    {
        this.stack = new XpType(stackSize);
    }


    @Override
    public ResourceLocation getTypeID()
    {
        return ID;
    }

    @Override
    public MapCodec<? extends IStackType<XpType>> codec()
    {
        return TYPE_CODEC;
    }

    @Override
    public IStackType<XpType> fromObject(Object key, long amount, DataComponentPatch dataComponentPatch)
    {
        if(key instanceof XpType)
        {
            return new XpStackType(amount);
        }
        return null;
    }

    @Override
    public IStackType<XpType> getEmpty()
    {
        return new XpStackType();
    }

    @Override
    public Object getSource()
    {
        return new XpType(0);
    }

    @Override
    public XpType getEmptyStack()
    {
        return new XpType(0);
    }

    @Override
    public IStackType<XpType> copy()
    {
        // copy时将哈希码状态一起带上，最大程度降低hash计算负担
        XpStackType copy = new XpStackType(stack.getStackCount());
        copy.NeedRecalHash = this.NeedRecalHash;
        copy.hashCodeCache = this.hashCodeCache;
        return copy;
    }

    @Override
    public IStackType<XpType> copyWithCount(long count)
    {
        XpStackType copy = new XpStackType(count);
        copy.NeedRecalHash = this.NeedRecalHash;
        copy.hashCodeCache = this.hashCodeCache;
        return copy;
    }

    @Override
    public IStackType<XpType> split(long amount)
    {
        if (amount <= 0) return new XpStackType();

        long splitAmount = Math.min(amount, stack.getStackCount());
        stack.shrink(splitAmount);
        return new XpStackType(splitAmount);
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf)
    {
        // 始终写入类型ID
        buf.writeResourceLocation(getTypeId()); // 会被deserializeCommon读取，因此deserialize中不要读取它
        // 写入数量
        buf.writeVarLong(stack.getStackCount());
    }

    @Override
    public IStackType<XpType> deserialize(RegistryFriendlyByteBuf buf, ResourceLocation typeId)
    {
        if (!typeId.equals(getTypeId())) {
            return null;// 表示未能读取任何类型
        }
        // 读取数量
        long count = buf.readVarLong();
        return new XpStackType(count);
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider levelRegistryAccess)
    {
        CompoundTag tag = new CompoundTag();
        tag.putString("Type", ID.toString());
        tag.putLong("Amount", getStackAmount());
        return tag;
    }

    @Override
    public IStackType<XpType> deserializeNBT(CompoundTag nbt, HolderLookup.Provider levelRegistryAccess)
    {
        return new XpStackType(nbt.getLong("Amount"));
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

        Fluid fluid = Fluids.WATER;
        if(!fluid.isSame(Fluids.EMPTY))
        {
            net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions renderProperties = net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions.of(fluid);
            ResourceLocation fluidStill = renderProperties.getStillTexture();
            Optional<net.minecraft.client.renderer.texture.TextureAtlasSprite> fluidStillSprite = Optional.ofNullable(fluidStill)
                    .map(f -> Minecraft.getInstance()
                            .getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                            .apply(f)
                    )
                    .filter(s -> s.atlasLocation() != net.minecraft.client.renderer.texture.MissingTextureAtlasSprite.getLocation());
            if(fluidStillSprite.isPresent())
            {
                int fluidColor = xpTintColor();
                com.wintercogs.beyonddimensions.Render.IngredientRenderer.drawTiledSprite(gui,16,16,fluidColor,16,fluidStillSprite.get(),x,y);
            }
        }


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

    // 为xp提供一个渐变效果
    private static int xpTintColor() {
        // === 时间相关 ===
        final float PERIOD_SECS = 3f;      // 整个循环总时长：常绿 -> 渐起变黄 -> 短停 -> 缓回绿 -> 纯绿静止
        final float ATTACK_FRAC = 0.3f;    // 渐起阶段占比：从绿逐渐过渡到峰值黄的时段
        final float HOLD_FRAC   = 0.05f;   // 峰值维持占比：在最亮/最黄附近短暂停留
        final float DECAY_FRAC  = 0.7f - ATTACK_FRAC - HOLD_FRAC; // 回落占比：从黄回到绿的时段（剩余30%留给纯绿静止）

        // === 颜色与强度（视觉风格） ===
        final int rG = 0x00, gG = 0xFE, bG = 0x00; // 预设绿色
        final int rY = 0xFE, gY = 0xFE, bY = 0x00; // 预设黄色
        final float YELLOW_PEAK_WEIGHT = 0.88f;    // 峰值时“偏黄”的强度（越大越黄）
        final float MIX_GAMMA          = 0.85f;    // 颜色混合的响应曲线（<1 回落更柔和）
        final float GLOW_GAIN          = 0.16f;    // 爆发期整体提亮幅度（发光感强弱）
        final float GLOW_GAMMA         = 0.70f;    // 提亮随包络的响应曲线（越小越“拖尾”）
        final float FLASH_WHITEN       = 0.04f;    // 峰值附近叠加少量白光（“亮点”感）
        final float FLASH_WIDTH_FRAC   = 0.16f;    // 白光峰的宽度（越宽越平滑）
        final boolean OUTPUT_ABGR      = false;    // true 则输出 ABGR；false 输出 ARGB（看你的渲染管线需求）

        // smootherstep：首尾导数为0的平滑曲线，用于渐起/回落
        java.util.function.DoubleUnaryOperator smoother = u -> {
            double x = u <= 0 ? 0 : (u >= 1 ? 1 : u);
            return x * x * x * (x * (x * 6 - 15) + 10);
        };

        // 相位（0..1）
        long now = net.minecraft.Util.getMillis();
        float invPeriodMs = 1f / (PERIOD_SECS * 1000f);
        float t = (now % (long)(PERIOD_SECS * 1000f)) * invPeriodMs;

        // 包络：控制从绿到黄再回绿的强度变化
        float env;
        if (t < ATTACK_FRAC) {
            env = (float) smoother.applyAsDouble(t / ATTACK_FRAC); // 渐起
        } else if (t < ATTACK_FRAC + HOLD_FRAC) {
            env = 1f;                                              // 短停
        } else if (t < ATTACK_FRAC + HOLD_FRAC + DECAY_FRAC) {
            env = (float) (1.0 - smoother.applyAsDouble((t - ATTACK_FRAC - HOLD_FRAC) / DECAY_FRAC)); // 回落
        } else {
            env = 0f;                                              // 纯绿静止段
        }

        // 颜色混合：在绿与黄之间按包络权重过渡
        float mix = YELLOW_PEAK_WEIGHT * (float) Math.pow(env, MIX_GAMMA);
        int r = Math.round(rG * (1 - mix) + rY * mix);
        int g = Math.round(gG * (1 - mix) + gY * mix);
        int b = Math.round(bG * (1 - mix) + bY * mix);

        // 发光提亮：随包络调整亮度
        float gain = 1f + GLOW_GAIN * (float) Math.pow(env, GLOW_GAMMA);
        r = Math.min(255, Math.round(r * gain));
        g = Math.min(255, Math.round(g * gain));
        b = Math.min(255, Math.round(b * gain));

        // 峰值附近的白色高光：制造“亮点”效果
        float peakCenter = ATTACK_FRAC + HOLD_FRAC * 0.5f;         // 高光居中在峰值时段
        float sigma      = FLASH_WIDTH_FRAC;                       // 高光宽度（相对周期）
        float d          = (t - peakCenter) / (sigma <= 1e-6f ? 1e-6f : sigma);
        float flash      = (float) Math.exp(-0.5f * d * d);        // 高斯形
        if (flash > 1e-3f) {
            float w = FLASH_WHITEN * flash;
            r = Math.min(255, Math.round(r + (255 - r) * w));
            g = Math.min(255, Math.round(g + (255 - g) * w));
            b = Math.min(255, Math.round(b + (255 - b) * w));
        }

        int argb = (0xFF << 24) | (r << 16) | (g << 8) | b;
        if (!OUTPUT_ABGR) return argb;
        return (argb & 0xFF000000)
                | ((argb & 0x00FF0000) >> 16)
                |  (argb & 0x0000FF00)
                | ((argb & 0x000000FF) << 16);
    }

}
