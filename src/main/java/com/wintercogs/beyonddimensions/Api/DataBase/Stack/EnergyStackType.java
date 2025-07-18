package com.wintercogs.beyonddimensions.Api.DataBase.Stack;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.wintercogs.beyonddimensions.Api.DataBase.LongType.EnergyType;
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

public final class EnergyStackType extends LongStackType<EnergyType>
{
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(BeyondDimensions.MODID, "stack_type/energy");
    public static final EnergyStackType EMPTY = new EnergyStackType(); // 空定义

    public static final MapCodec<EnergyStackType> TYPE_CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    EnergyType.CODEC.fieldOf("internal_stack").forGetter(EnergyStackType::getStack)
            ).apply(instance, EnergyStackType::new));

    public static final Codec<EnergyStackType> CODEC = TYPE_CODEC.codec();

    public EnergyStackType()
    {
        stack = new EnergyType(0);
    }

    public EnergyStackType(EnergyType stack)
    {
        this.stack = stack;
    }

    public EnergyStackType(long stackSize)
    {
        this.stack = new EnergyType(stackSize);
    }

    @Override
    public MapCodec<EnergyStackType> codec()
    {
        return TYPE_CODEC;
    }

    @Override
    public ResourceLocation getTypeID()
    {
        return ID;
    }

    @Override
    public IStackType<EnergyType> fromObject(Object key, long amount, DataComponentPatch dataComponentPatch)
    {
        if(key instanceof EnergyType)
        {
            return new EnergyStackType(amount);
        }
        return null;
    }

    @Override
    public IStackType<EnergyType> getEmpty()
    {
        return new EnergyStackType();
    }

    @Override
    public Object getSource()
    {
        return new EnergyType(0);
    }

    @Override
    public EnergyType getEmptyStack()
    {
        return new EnergyType(0);
    }

    @Override
    public IStackType<EnergyType> copy()
    {
        // copy时将哈希码状态一起带上，最大程度降低hash计算负担
        EnergyStackType copy = new EnergyStackType(stack.getStackCount());
        copy.NeedRecalHash = this.NeedRecalHash;
        copy.hashCodeCache = this.hashCodeCache;
        return copy;
    }

    @Override
    public IStackType<EnergyType> copyWithCount(long count)
    {
        EnergyStackType copy = new EnergyStackType(count);
        copy.NeedRecalHash = this.NeedRecalHash;
        copy.hashCodeCache = this.hashCodeCache;
        return copy;
    }

    @Override
    public IStackType<EnergyType> split(long amount)
    {
        if (amount <= 0) return new EnergyStackType();

        long splitAmount = Math.min(amount, stack.getStackCount());
        stack.shrink(splitAmount);
        return new EnergyStackType(splitAmount);
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
    public IStackType<EnergyType> deserialize(RegistryFriendlyByteBuf buf, ResourceLocation typeId)
    {
        if (!typeId.equals(getTypeId())) {
            return null;// 表示未能读取任何类型
        }
        // 读取数量
        long count = buf.readVarLong();
        return new EnergyStackType(count);
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
    public IStackType<EnergyType> deserializeNBT(CompoundTag nbt, HolderLookup.Provider levelRegistryAccess)
    {
        return new EnergyStackType(nbt.getLong("Amount"));
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
                int fluidColor =  0x50F18E; // 绿色
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
}
