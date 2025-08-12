package com.wintercogs.beyonddimensions.Api.DataBase.Stack;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.wintercogs.beyonddimensions.Api.DataBase.LongType.WardenSoulType;
import com.wintercogs.beyonddimensions.BeyondDimensions;
import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;

import java.util.ArrayList;
import java.util.List;

// 工业先锋-灵魂涌动 监守者之魂
public class WardenSoulStackType extends LongStackType<WardenSoulType>
{
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(BeyondDimensions.MODID, "stack_type/warden_soul");
    public static final WardenSoulStackType EMPTY = new WardenSoulStackType(); // 空定义

    public static final MapCodec<WardenSoulStackType> TYPE_CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    WardenSoulType.CODEC.fieldOf("internal_stack").forGetter(WardenSoulStackType::getStack)
            ).apply(instance, WardenSoulStackType::new));

    public static final Codec<WardenSoulStackType> CODEC = TYPE_CODEC.codec();

    private static long lastCheckedForParticle = 0L; // 用于渲染
    private static List<GuiParticle> particleList = new ArrayList(); // 用于渲染

    public WardenSoulStackType()
    {
        stack = new WardenSoulType(0);
    }

    public WardenSoulStackType(WardenSoulType stack)
    {
        this.stack = stack;
    }

    public WardenSoulStackType(long stackSize)
    {
        this.stack = new WardenSoulType(stackSize);
    }

    @Override
    public ResourceLocation getTypeID()
    {
        return ID;
    }

    @Override
    public String getModId()
    {
        return BeyondDimensions.IFS_ModId;
    }

    @Override
    public MapCodec<? extends IStackType<WardenSoulType>> codec()
    {
        return TYPE_CODEC;
    }

    @Override
    public IStackType<WardenSoulType> fromObject(Object key, long amount, DataComponentPatch dataComponentPatch)
    {
        if(key instanceof WardenSoulType)
        {
            return new WardenSoulStackType(amount);
        }
        return null;
    }

    @Override
    public IStackType<WardenSoulType> getEmpty()
    {
        return new WardenSoulStackType(0);
    }

    @Override
    public Object getSource()
    {
        return new WardenSoulType(0);
    }

    @Override
    public WardenSoulType getEmptyStack()
    {
        return new WardenSoulType(0);
    }

    @Override
    public IStackType<WardenSoulType> copy()
    {
        // copy时将哈希码状态一起带上，最大程度降低hash计算负担
        WardenSoulStackType copy = new WardenSoulStackType(stack.getStackCount());
        copy.NeedRecalHash = this.NeedRecalHash;
        copy.hashCodeCache = this.hashCodeCache;
        return copy;
    }

    @Override
    public IStackType<WardenSoulType> copyWithCount(long count)
    {
        WardenSoulStackType copy = new WardenSoulStackType(count);
        copy.NeedRecalHash = this.NeedRecalHash;
        copy.hashCodeCache = this.hashCodeCache;
        return copy;
    }

    @Override
    public IStackType<WardenSoulType> split(long amount)
    {
        if (amount <= 0) return new WardenSoulStackType();

        long splitAmount = Math.min(amount, stack.getStackCount());
        stack.shrink(splitAmount);
        return new WardenSoulStackType(splitAmount);
    }

    @Override
    public boolean hasTag(TagKey<?> tagKey)
    {
        return false;
    }

    @Override
    public long getVanillaMaxStackSize()
    {
        return 64000L; // 对灵魂涌动来说足够多了
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
    public IStackType<WardenSoulType> deserialize(RegistryFriendlyByteBuf buf, ResourceLocation typeId)
    {
        if (!typeId.equals(getTypeId())) {
            return null;// 表示未能读取任何类型
        }
        // 读取数量
        long count = buf.readVarLong();
        return new WardenSoulStackType(count);
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
    public IStackType<WardenSoulType> deserializeNBT(CompoundTag nbt, HolderLookup.Provider levelRegistryAccess)
    {
        return new WardenSoulStackType(nbt.getLong("Amount"));
    }

    @Override
    public void render(net.minecraft.client.gui.GuiGraphics gui, int x, int y)
    {
        if(stack.isEmpty())
            return;

        // 渲染图标
        gui.pose().pushPose();
        ResourceLocation warden_rl = ResourceLocation.withDefaultNamespace("textures/entity/warden/warden.png");
        ResourceLocation warden_hear = ResourceLocation.withDefaultNamespace("textures/entity/warden/warden_heart.png");
        gui.blit(warden_rl, x, y, 12.0F, 14.0F, 16, 16, 128, 128);
        gui.pose().pushPose();
        float heart_timing = 30.0F;
        heart_timing = 1.0F - (float)Minecraft.getInstance().level.getGameTime() % heart_timing / heart_timing;
        RenderSystem.setShaderColor(heart_timing, heart_timing, heart_timing, heart_timing);
        gui.blit(warden_hear, x - 1, y - 1, 11.0F, 13.0F, 18, 18, 128, 128);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        gui.pose().popPose();
        long rotation = Minecraft.getInstance().level.getGameTime() % 160L - 80L;
        gui.pose().pushPose();
        gui.pose().translate((float)x, (float)(y - 1), 100.0F);
        gui.pose().mulPose(Axis.YP.rotationDegrees((float)rotation));
        gui.blit(warden_rl, 0, 0, 91.0F, 13.0F, 17, 18, 128, 128);
        gui.pose().popPose();
        gui.pose().pushPose();
        gui.pose().translate((float)(x + 16), (float)(y + 17), 100.0F);
        gui.pose().mulPose(Axis.ZP.rotationDegrees(180.0F));
        gui.pose().mulPose(Axis.YP.rotationDegrees((float)rotation));
        gui.blit(warden_rl, 0, 0, 91.0F, 13.0F, 17, 18, 128, 128);
        gui.pose().popPose();
        gui.pose().scale(0.75F, 0.75F, 0.75F);
        double fullAmount = 0.05;
        int xSize = 8;
        int ySize = 6;
        long currentTime = Minecraft.getInstance().level.getGameTime();
        if (this.lastCheckedForParticle != currentTime) {
            if (Minecraft.getInstance().level.random.nextDouble() <= fullAmount) {
                this.particleList.add(new GuiParticle(Minecraft.getInstance().level.random.nextInt(xSize), ySize - Minecraft.getInstance().level.random.nextInt(3), currentTime));
            }

            this.lastCheckedForParticle = currentTime;
        }

        int ageTick = 3;
        if (currentTime % (long)ageTick == 0L) {
            this.particleList.removeIf((guiParticlex) -> (currentTime - guiParticlex.age) / (long)ageTick > 10L);
        }

        gui.pose().translate(0.0F, 0.0F, 200.0F);

        for(GuiParticle guiParticle : this.particleList.reversed()) {
            double particleAge = (double)(currentTime - guiParticle.age) / (double)ageTick;
            double extraY = (double)(ySize - 32) / (double)20.0F * particleAge;
            gui.blit(ResourceLocation.withDefaultNamespace("textures/particle/sculk_soul_" + Math.max(0, Math.min(10, (int)particleAge)) + ".png"), (int)((float)(x + guiParticle.x) * 1.3333334F), (int)((float)((int)((double)(y + guiParticle.y) + extraY)) * 1.3333334F), 0.0F, 0.0F, 16, 16, 16, 16);
        }

        gui.pose().popPose();

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
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc(); // 恢复状态
    }

    // 用于渲染
    private class GuiParticle {
        private int x;
        private int y;
        private long age;

        public GuiParticle(int x, int y, long age) {
            this.x = x;
            this.y = y;
            this.age = age;
        }
    }
}
