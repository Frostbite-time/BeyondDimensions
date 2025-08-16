package com.wintercogs.beyonddimensions.Api.DataBase.Stack;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.Unit.BDMath;
import com.wintercogs.beyonddimensions.Unit.StringFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.ClientTooltipFlag;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class ItemStackType implements IStackType<ItemStack> {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(BeyondDimensions.MODID, "stack_type/item");
    private static final long CUSTOM_MAX_STACK_SIZE = Long.MAX_VALUE; // 自定义堆叠大小

    // === 新格式：item + components(patch) + amount（写入始终用它） ===
    public static final MapCodec<ItemStackType> TYPE_CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    BuiltInRegistries.ITEM.holderByNameCodec().fieldOf("item")
                            .forGetter(t -> t.item.builtInRegistryHolder()),
                    DataComponentPatch.CODEC.optionalFieldOf("components", DataComponentPatch.EMPTY)
                            .forGetter(t -> t.patch),
                    Codec.LONG.fieldOf("amount").forGetter(ItemStackType::getStackAmount)
            ).apply(instance, (holder, patch, amount) ->
                    new ItemStackType(
                            // 构造时就建立 cachedStack，后续复用
                            holder.value(),
                            patch,
                            amount
                    )
            )
    );

    // === 旧格式（只用于读取）：internal_stack + amount ===
    private static final Codec<ItemStackType> LEGACY_CODEC = RecordCodecBuilder.create(inst -> inst.group(
            ItemStack.OPTIONAL_CODEC.fieldOf("internal_stack").forGetter(ItemStackType::getStack),
            Codec.LONG.fieldOf("amount").forGetter(ItemStackType::getStackAmount)
    ).apply(inst, (stack, amt) -> new ItemStackType(stack, amt)));

    // 读时先尝试新格式，失败则回退旧格式；写时只用新格式
    public static final Codec<ItemStackType> CODEC =
            net.minecraft.util.ExtraCodecs.orAlternative(TYPE_CODEC.codec(), LEGACY_CODEC);

    // === 实际存储：item + patch + amount ===
    private Item item;
    private DataComponentPatch patch;
    private long stackSize;

    // 统一缓存（服务端/客户端通用），避免频繁 new ItemStack
    private ItemStack cachedStack;

    private int hashCodeCache = 0; // 哈希码缓存（基于 item+patch）
    private boolean NeedRecalHash = true; // 指示何时需要重算哈希

    public ItemStackType() {
        this.item = Items.AIR;
        this.patch = DataComponentPatch.EMPTY;
        this.stackSize = 0;
        this.cachedStack = ItemStack.EMPTY;
    }

    public ItemStackType(ItemStack stack) {
        this.item = stack.getItem();
        this.patch = stack.getComponentsPatch(); // 只存增量
        this.stackSize = stack.getCount();
        this.cachedStack = new ItemStack(this.item.builtInRegistryHolder(), 1, this.patch);
    }

    public ItemStackType(ItemStack stack, long stackSize) {
        this.item = stack.getItem();
        this.patch = stack.getComponentsPatch();
        this.stackSize = stackSize;
        this.cachedStack = new ItemStack(this.item.builtInRegistryHolder(), 1, this.patch);
    }

    // 供 TYPE_CODEC 使用的构造
    public ItemStackType(Item item, DataComponentPatch patch, long stackSize) {
        this.item = item;
        this.patch = patch != null ? patch : DataComponentPatch.EMPTY;
        this.stackSize = stackSize;
        this.cachedStack = this.item == Items.AIR ? ItemStack.EMPTY
                : new ItemStack(this.item.builtInRegistryHolder(), 1, this.patch);
    }

    @Override
    public MapCodec<ItemStackType> codec()
    {
        return TYPE_CODEC;
    }

    @Override
    public IStackType<ItemStack> fromObject(Object key, long amount, DataComponentPatch dataComponentPatch)
    {
        if (key instanceof Item it) {
            Item item0 = it;
            DataComponentPatch p = dataComponentPatch != null ? dataComponentPatch : DataComponentPatch.EMPTY;
            return new ItemStackType(item0, p, amount);
        }
        return null;
    }

    @Override
    public ItemStack getStack()
    {
        // 返回缓存对象，并把数量同步为当前 long（clamp 到 int）
        if (this.cachedStack.isEmpty() || this.cachedStack.getItem() != this.item) {
            this.cachedStack = this.item == Items.AIR ? ItemStack.EMPTY
                    : new ItemStack(this.item.builtInRegistryHolder(), 1, this.patch);
            NeedRecalHash = true; // 极少发生：item 变化
        }
        this.cachedStack.setCount(BDMath.clampLongToInt(this.stackSize));
        return this.cachedStack;
    }

    @Override
    public void setStack(ItemStack stack)
    {
        this.item = stack.getItem();
        this.patch = stack.getComponentsPatch();
        this.stackSize = stack.getCount();
        this.cachedStack = new ItemStack(this.item.builtInRegistryHolder(), 1, this.patch);
        NeedRecalHash = true;
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
    public String getModId()
    {
        return BuiltInRegistries.ITEM.getKey(item).getNamespace();
    }

    @Override
    public boolean isEmpty() {
        return this.item == Items.AIR || this.stackSize <= 0;
    }

    @Override
    public boolean isEmptyStack() {
        // 不依赖 ItemStack#isEmpty，直接判 AIR
        return this.item == Items.AIR;
    }

    @Override
    public ItemStack getEmptyStack()
    {
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack copyStack() {
        // 基于缓存生成副本，数量为当前 size
        ItemStack base = this.item == Items.AIR ? ItemStack.EMPTY
                : new ItemStack(this.item.builtInRegistryHolder(), 1, this.patch);
        return base.copyWithCount(BDMath.clampLongToInt(this.stackSize));
    }

    @Override
    public ItemStack copyStackWithCount(long count) {
        ItemStack base = this.item == Items.AIR ? ItemStack.EMPTY
                : new ItemStack(this.item.builtInRegistryHolder(), 1, this.patch);
        return base.copyWithCount(BDMath.clampLongToInt(count));
    }

    @Override
    public IStackType<ItemStack> copy() {
        ItemStackType cp = new ItemStackType(this.item, this.patch, this.stackSize);
        cp.NeedRecalHash = this.NeedRecalHash;
        cp.hashCodeCache = this.hashCodeCache;
        return cp;
    }

    @Override
    public IStackType<ItemStack> copyWithCount(long count) {
        ItemStackType cp = new ItemStackType(this.item, this.patch, count);
        cp.NeedRecalHash = this.NeedRecalHash;
        cp.hashCodeCache = this.hashCodeCache;
        return cp;
    }

    @Override
    public long getStackAmount() {
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
    public long getVanillaMaxStackSize() {
        // 保留与原版一致的行为：交给ItemStack去获取getMaxStackSize，保证能得到正确值
        if (this.item == Items.AIR) return 0;
        if (this.cachedStack.isEmpty() || this.cachedStack.getItem() != this.item) {
            this.cachedStack = new ItemStack(this.item.builtInRegistryHolder(), 1, this.patch);
        }
        return Math.min(this.cachedStack.getMaxStackSize(), getCustomMaxStackSize());
    }

    @Override
    public long getCustomMaxStackSize() {
        // 可配置化的最大堆叠尺寸
        return CUSTOM_MAX_STACK_SIZE;
    }

    @Override
    public ItemStack splitStack(long amount) {

        if (amount <= 0 || this.item == Items.AIR) return ItemStack.EMPTY;
        int splitAmount = BDMath.clampLongToInt(Math.min(amount, this.stackSize));
        shrink(splitAmount);
        return new ItemStack(this.item.builtInRegistryHolder(), splitAmount, this.patch);
    }

    @Override
    public IStackType<ItemStack> split(long amount)
    {
        if (amount <= 0 || this.item == Items.AIR) return new ItemStackType();
        long splitAmount = Math.min(amount, this.stackSize);
        shrink(splitAmount);
        return new ItemStackType(this.item, this.patch, splitAmount);
    }

    @Override
    public boolean hasTag(TagKey<?> tagKey)
    {
        if (tagKey == null || this.item == Items.AIR) return false;
        if (!tagKey.isFor(Registries.ITEM)) return false;

        TagKey<Item> itemTag = (TagKey<Item>) tagKey;
        return this.item.builtInRegistryHolder().is(itemTag);
    }

    @Override
    public boolean isSame(IStackType<?> other)
    {
        if(other instanceof ItemStackType otherItemStackType) // 顺手处理空
        {
            return this.item == otherItemStackType.item; // 直接比对
        }
        return false;
    }

    @Override
    public boolean isSameTypeSameComponents(IStackType<?> other)
    {
        if(other instanceof ItemStackType otherItemStackType) // 顺手处理空
        {
            return this.item == otherItemStackType.item && Objects.equals(this.patch,otherItemStackType.patch); // 直接比对
        }
        return false;
    }

    // 网络序列化
    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        // 始终写入类型ID
        buf.writeResourceLocation(getTypeId());

        // 写入是否存在物品的标志
        boolean hasItem = !stack.isEmpty();
        buf.writeBoolean(hasItem);

        if (hasItem) {
            // 写入数量
            buf.writeVarLong(stackSize);
            // 使用副本避免修改原堆栈
            ItemStack copy = stack.copyWithCount(1);
            // 使用OPTIONAL_CODEC处理可能为空的情况
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, copy);
        }
    }

    @Override
    public ItemStackType deserialize(RegistryFriendlyByteBuf buf,ResourceLocation typeId) {
        if (!typeId.equals(getTypeId())) {
            return null;// 表示未能读取任何类型
        }

        // 读取是否存在物品的标志
        boolean hasItem = buf.readBoolean();
        if (!hasItem) {
            return new ItemStackType(ItemStack.EMPTY);
        }

        // 读取数量
        long count = buf.readVarLong();
        // 使用OPTIONAL_CODEC解码
        ItemStack stack = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
        return new ItemStackType(stack,count);
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider levelRegistryAccess) {
        CompoundTag tag = new CompoundTag();
        tag.putString("Type", ID.toString());
        tag.putLong("Amount", getStackAmount());
        tag.put("Stack",stack.copyWithCount(1).saveOptional(levelRegistryAccess));
        return tag;
    }

    @Override
    public ItemStackType deserializeNBT(CompoundTag nbt, HolderLookup.Provider levelRegistryAccess) {
        ItemStackType stack =  new ItemStackType(ItemStack.parseOptional(levelRegistryAccess,nbt.getCompound("Stack")));
        stack.setStackAmount(nbt.getLong("Amount"));
        return stack;
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void render(net.minecraft.client.gui.GuiGraphics gui,int x, int y) {
        // 渲染物品图标
        var poseStack = gui.pose(); // 获取渲染的变换矩阵
        poseStack.pushPose(); // 保存矩阵状态
        if (this.cachedStack.isEmpty() || this.cachedStack.getItem() != this.item) { // 这是个极其轻量的检查，即使是每帧渲染也不可能卡顿
            this.cachedStack = this.item == Items.AIR ? ItemStack.EMPTY
                    : new ItemStack(this.item.builtInRegistryHolder(), 1, this.patch);
        }
        this.cachedStack.setCount(1); // 图标显示 1；实际数量单独绘制
        gui.renderFakeItem(cachedStack, x, y);
        gui.renderItemDecorations(Minecraft.getInstance().font, cachedStack, x, y, "");
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
        if(!cachedStack.isEmpty())
            gui.drawString(Minecraft.getInstance().font, countText, X, Y, 0xFFFFFF);
        poseStackText.popPose();
    }

    @Override
    public String getCountText(long count) {
        if (count < 0) return "";
        return StringFormat.formatCount(count);
    }

    @Override
    public Component getDisplayName()
    {
        if (this.cachedStack.isEmpty() || this.cachedStack.getItem() != this.item) {
            this.cachedStack = this.item == Items.AIR ? ItemStack.EMPTY
                    : new ItemStack(this.item.builtInRegistryHolder(), 1, this.patch);
        }
        return cachedStack.getDisplayName();
    }

    @Override
    public List<Component> getTooltipLines(Item.TooltipContext tooltipContext, @Nullable Player player, TooltipFlag tooltipFlag)
    {
        if (this.cachedStack.isEmpty() || this.cachedStack.getItem() != this.item) {
            this.cachedStack = this.item == Items.AIR ? ItemStack.EMPTY
                    : new ItemStack(this.item.builtInRegistryHolder(), 1, this.patch);
        }
        List<Component> tooltips = cachedStack.getTooltipLines(tooltipContext,player,tooltipFlag);
        tooltips.add(Component.translatable("istack.beyonddimensions.storage_num.item", getStackAmount()));
        return tooltips;
    }

    @Override
    public Optional<TooltipComponent> getTooltipImage()
    {
        if (this.cachedStack.isEmpty() || this.cachedStack.getItem() != this.item) {
            this.cachedStack = this.item == Items.AIR ? ItemStack.EMPTY
                    : new ItemStack(this.item.builtInRegistryHolder(), 1, this.patch);
        }
        return cachedStack.getTooltipImage();
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void renderTooltip(net.minecraft.client.gui.GuiGraphics gui,net.minecraft.client.gui.Font font, int mouseX, int mouseY)
    {
        var minecraft = Minecraft.getInstance();
        gui.renderTooltip(minecraft.font, this.getTooltipLines(Item.TooltipContext.of(minecraft.level),minecraft.player, ClientTooltipFlag.of(minecraft.options.advancedItemTooltips ? TooltipFlag.Default.ADVANCED : TooltipFlag.Default.NORMAL))
                , getTooltipImage(), ItemStack.EMPTY, mouseX, mouseY);
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
        if(NeedRecalHash)
        {
            int i = 31 + item.hashCode();
            hashCodeCache = 31 * i + patch.hashCode();
            NeedRecalHash = false;
        }
        return hashCodeCache;
    }
}

