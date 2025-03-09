package com.wintercogs.beyonddimensions.DataBase.Stack;

import com.mojang.blaze3d.systems.RenderSystem;
import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.Unit.StringFormat;
import mekanism.api.MekanismAPI;
import mekanism.api.chemical.Chemical;
import mekanism.api.chemical.ChemicalStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.ClientTooltipFlag;
import org.apache.commons.lang3.text.WordUtils;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// 用于处理通用机械的化学品类
public class ChemicalStackType implements IStackType<ChemicalStack>
{

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(BeyondDimensions.MODID, "stack_type/chemical");
    private static final long CUSTOM_MAX_STACK_SIZE = Long.MAX_VALUE; // 自定义堆叠大小

    private ChemicalStack stack;

    // 创建空stack
    public ChemicalStackType()
    {
        stack = ChemicalStack.EMPTY;
    }

    // 创建给定stack
    public ChemicalStackType(ChemicalStack stack)
    {
        this.stack = stack;
    }

    @Override
    public ResourceLocation getTypeId()
    {
        return this.ID;
    }

    @Override
    public IStackType<ChemicalStack> getEmpty()
    {
        return new ChemicalStackType();
    }

    @Override
    public ChemicalStack getStack()
    {
        return stack.copyWithAmount(getStackAmount());
    }

    @Override
    public void setStack(ChemicalStack stack)
    {
        this.stack = stack.copy();
    }

    @Override
    public Class<ChemicalStack> getStackClass()
    {
        return ChemicalStack.class;
    }

    @Override
    public Class<?> getSourceClass()
    {
        return Chemical.class;
    }

    @Override
    public Object getSource()
    {
        return ChemicalStack.EMPTY.getChemical();
    }

    @Override
    public boolean isEmpty()
    {
        return stack.isEmpty();
    }

    @Override
    public ChemicalStack getEmptyStack()
    {
        return ChemicalStack.EMPTY;
    }

    @Override
    public ChemicalStack copyStack()
    {
        return stack.copy();
    }

    @Override
    public ChemicalStack copyStackWithCount(long count)
    {
        return stack.copyWithAmount(count);
    }

    @Override
    public IStackType<ChemicalStack> copy()
    {
        return new ChemicalStackType(stack.copy());
    }

    @Override
    public IStackType<ChemicalStack> copyWithCount(long count)
    {
        return new ChemicalStackType(stack.copyWithAmount(count));
    }

    @Override
    public long getStackAmount()
    {
        return stack.getAmount();
    }

    @Override
    public void setStackAmount(long amount)
    {
        stack.setAmount(amount);
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
    public ChemicalStack splitStack(long amount)
    {
        if (amount <= 0) return ChemicalStack.EMPTY;

        // 计算可分割的数量
        long splitAmount = Math.min(amount, stack.getAmount());
        ChemicalStack split = stack.copy();
        split.setAmount(splitAmount);
        stack.shrink(splitAmount);
        return split;
    }

    @Override
    public IStackType<ChemicalStack> split(long amount)
    {
        if (amount <= 0) return new ChemicalStackType();

        // 计算可分割的数量
        long splitAmount = Math.min(amount, stack.getAmount());
        ChemicalStack split = stack.copy();
        split.setAmount(splitAmount);
        stack.shrink(splitAmount);
        return new ChemicalStackType(split);
    }

    @Override
    public boolean isSame(IStackType<ChemicalStack> other)
    {
        if(!other.getTypeId().equals(this.getTypeId()))
            return false;
        return ChemicalStack.isSameChemical(stack, other.getStack());
    }

    @Override
    public boolean isSameTypeSameComponents(IStackType<ChemicalStack> other)
    {
        if(!other.getTypeId().equals(this.getTypeId()))
            return false;
        return ChemicalStack.isSameChemical(stack, other.getStack());
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf)
    {
        // 始终写入类型ID
        buf.writeResourceLocation(getTypeId());

        // 写入是否存在物品的标志
        boolean hasItem = !stack.isEmpty();
        buf.writeBoolean(hasItem);

        if (hasItem) {
            // 写入数量
            buf.writeVarLong(stack.getAmount());
            // 使用副本避免修改原堆栈
            ChemicalStack copy = stack.copyWithAmount(1);
            // 使用OPTIONAL_CODEC处理可能为空的情况
            ChemicalStack.OPTIONAL_STREAM_CODEC.encode(buf, copy);
        }
    }

    @Override
    public IStackType<ChemicalStack> deserialize(RegistryFriendlyByteBuf buf, ResourceLocation typeId)
    {
        if (!typeId.equals(getTypeId())) {
            return null;// 表示未能读取任何类型
        }

        // 读取是否存在物品的标志
        boolean hasItem = buf.readBoolean();
        if (!hasItem) {
            return new ChemicalStackType(ChemicalStack.EMPTY);
        }

        // 读取数量
        long count = buf.readVarLong();
        // 使用OPTIONAL_CODEC解码
        ChemicalStack stack = ChemicalStack.OPTIONAL_STREAM_CODEC.decode(buf)
                .copyWithAmount(count);
        return new ChemicalStackType(stack);
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider levelRegistryAccess)
    {
        CompoundTag tag = new CompoundTag();
        tag.putLong("Amount", getStackAmount());
        tag.put("Stack",stack.copyWithAmount(1).save(levelRegistryAccess));
        return tag;
    }

    @Override
    public IStackType<ChemicalStack> deserializeNBT(CompoundTag nbt, HolderLookup.Provider levelRegistryAccess)
    {
        ChemicalStackType stack =  new ChemicalStackType(ChemicalStack.parseOptional(levelRegistryAccess,nbt.getCompound("Stack")));
        stack.setStackAmount(nbt.getLong("Amount"));
        return stack;
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void render(net.minecraft.client.gui.GuiGraphics gui, int x, int y)
    {
        // 渲染图标
        var poseStack = gui.pose(); // 获取渲染的变换矩阵
        poseStack.pushPose(); // 保存矩阵状态

        Chemical chemical = stack.getChemical();
        if(!chemical.isEmptyType())
        {
            ResourceLocation fluidStill = chemical.getIcon();
            Optional<net.minecraft.client.renderer.texture.TextureAtlasSprite> fluidStillSprite = Optional.ofNullable(fluidStill)
                    .map(f -> Minecraft.getInstance()
                            .getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                            .apply(f)
                    )
                    .filter(s -> s.atlasLocation() != net.minecraft.client.renderer.texture.MissingTextureAtlasSprite.getLocation());
            if(fluidStillSprite.isPresent())
            {
                int fluidColor = chemical.getTint();
                com.wintercogs.beyonddimensions.Render.IngredientRenderer.drawTiledSprite(gui,16,16,fluidColor,16,fluidStillSprite.get(),x,y);
            }
        }


        poseStack.popPose(); // 恢复矩阵状态，结束渲染

        // 渲染数量文本
        String countText = getCountText(stack.getAmount());
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
        gui.drawString(Minecraft.getInstance().font,
                countText,
                X,
                Y,
                0xFFFFFF);
        poseStackText.popPose();
    }

    @Override
    public String getCountText(long count)
    {
        if (count <= 0) return "";
        return StringFormat.formatCount(count);
    }

    @Override
    public Component getDisplayName()
    {
        return stack.getTextComponent();
    }

    @Override
    public List<Component> getTooltipLines(Item.TooltipContext tooltipContext, @Nullable Player player, TooltipFlag tooltipFlag)
    {
        if(stack.isEmpty())
            return List.of(Component.empty());

        List<Component> tooltips = new ArrayList<>();
        Chemical chemical = stack.getChemical();

        Component displayName = getDisplayName();
        tooltips.add(displayName);

        ResourceLocation resourceLocation = MekanismAPI.CHEMICAL_REGISTRY.getKey(chemical);
        if (resourceLocation != null) {
            if (tooltipFlag.isAdvanced()) {
                MutableComponent advancedId = Component.literal(resourceLocation.toString())
                        .withStyle(ChatFormatting.DARK_GRAY);
                tooltips.add(advancedId);
            }
            Optional<? extends ModContainer> container = ModList.get().getModContainerById(resourceLocation.getNamespace());
            Component modName;
            if(container.isPresent())
            {
                modName = Component.literal(container.get().getModInfo().getDisplayName()).withStyle(ChatFormatting.BLUE).withStyle(ChatFormatting.ITALIC);
            }
            else
            {
                container = ModList.get().getModContainerById(resourceLocation.getNamespace().replace('_', '-'));
                if (container.isPresent()) {
                    modName = Component.literal(container.get().getModInfo().getDisplayName()).withStyle(ChatFormatting.BLUE).withStyle(ChatFormatting.ITALIC);
                }
                else
                {
                    modName = Component.literal(WordUtils.capitalizeFully(resourceLocation.getNamespace().replace('_', ' '))).withStyle(ChatFormatting.BLUE).withStyle(ChatFormatting.ITALIC);
                }
            }
            tooltips.add(modName);
        }

        tooltips.add(Component.literal("已存储:"+getStackAmount()+"mB"));
        return tooltips;
    }

    @Override
    public Optional<TooltipComponent> getTooltipImage()
    {
        return Optional.empty();
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void renderTooltip(net.minecraft.client.gui.GuiGraphics gui, net.minecraft.client.gui.Font font, int mouseX, int mouseY)
    {
        var minecraft = Minecraft.getInstance();
        gui.renderTooltip(minecraft.font, this.getTooltipLines(Item.TooltipContext.of(minecraft.level),minecraft.player, ClientTooltipFlag.of(minecraft.options.advancedItemTooltips ? TooltipFlag.Default.ADVANCED : TooltipFlag.Default.NORMAL))
                , getTooltipImage(), ItemStack.EMPTY, mouseX, mouseY);
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
        code = 31 * code + stack.getChemical().hashCode();
        return code;
    }
}
