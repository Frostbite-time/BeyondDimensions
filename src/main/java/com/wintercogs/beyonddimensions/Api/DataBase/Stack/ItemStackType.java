package com.wintercogs.beyonddimensions.Api.DataBase.Stack;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.Unit.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.PatchedDataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
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
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

public final class ItemStackType implements IStackType<ItemStack> {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(BeyondDimensions.MODID, "stack_type/item");
    private static final long CUSTOM_MAX_STACK_SIZE = Long.MAX_VALUE; // 自定义堆叠大小

    // 很尴尬，我一开始没写CODEC，但是后续又被数据组件所需要，现在要同时维护CODEC的还有序列化方法里面的两份兼容

    // 新的CODEC，写入时使用
    private static final MapCodec<ItemStackType> NEW_FMT = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    BuiltInRegistries.ITEM.holderByNameCodec().fieldOf("item")
                            .forGetter(t -> RegistryUtil.holderOf(t.item)),
                    DataComponentPatch.CODEC.optionalFieldOf("components", DataComponentPatch.EMPTY)
                            .forGetter(t -> t.patch),
                    Codec.LONG.fieldOf("amount").forGetter(ItemStackType::getStackAmount)
            ).apply(instance, (holder, patch, amount) -> new ItemStackType(holder.value(), patch, amount))
    );

    // 旧格式CODEC
    private static final Codec<ItemStackType> LEGACY_CODEC = RecordCodecBuilder.create(inst -> inst.group(
            ItemStack.OPTIONAL_CODEC.fieldOf("internal_stack").forGetter(ItemStackType::getStack),
            Codec.LONG.fieldOf("amount").forGetter(ItemStackType::getStackAmount)
    ).apply(inst, (stack, amt) -> new ItemStackType(stack, amt)));

    // 这里的 TYPE_CODEC 自己实现“读时新优先、无新字段则读旧；写时永远用新”
    public static final MapCodec<ItemStackType> TYPE_CODEC = new MapCodec<>() {
        @Override
        public <T> DataResult<ItemStackType> decode(DynamicOps<T> ops, MapLike<T> input) {
            final T kItem = ops.createString("item");
            final T kAmount = ops.createString("amount");
            final T kLegacy = ops.createString("internal_stack");
            final T kComps  = ops.createString("components");

            T hasItem = input.get(kItem);
            T hasAmount = input.get(kAmount);

            if (hasItem != null && hasAmount != null) {
                // 先按新格式正常解码
                DataResult<ItemStackType> r = NEW_FMT.decode(ops, input);
                if (r.result().isPresent()) return r;

                // ——宽松回退：新格式存在但解码失败（多为未知 item）→ 退化为 AIR —— //
                long amt = Codec.LONG.parse(ops, hasAmount).result().orElse(0L);
                DataComponentPatch patch = DataComponentPatch.EMPTY;
                T compsNode = input.get(kComps);
                if (compsNode != null) {
                    patch = DataComponentPatch.CODEC.parse(ops, compsNode).result().orElse(DataComponentPatch.EMPTY);
                }
                return DataResult.success(new ItemStackType(Items.AIR, patch, amt));
            }

            // 尝试旧格式
            T hasLegacy = input.get(kLegacy);
            if (hasLegacy != null && hasAmount != null) {
                java.util.Map<T, T> map = new java.util.LinkedHashMap<>();
                input.entries().forEach(p -> map.put(p.getFirst(), p.getSecond()));
                T node = ops.createMap(map);
                return LEGACY_CODEC.decode(ops, node).map(com.mojang.datafixers.util.Pair::getFirst);
            }

            // 都不满足：让 NEW_FMT 报出明确缺字段信息
            return NEW_FMT.decode(ops, input);
        }

        @Override
        public <T> RecordBuilder<T> encode(ItemStackType value, DynamicOps<T> ops, RecordBuilder<T> prefix) {
            // 写出：始终用新格式
            return NEW_FMT.encode(value, ops, prefix);
        }

        @Override
        public <T> Stream<T> keys(DynamicOps<T> ops) {
            // 向外暴露新格式的键集合
            return Stream.of("item", "components", "amount").map(ops::createString);
        }

    };

    public static final Codec<ItemStackType> CODEC = TYPE_CODEC.codec();

    // 实际存储
    private Item item;
    private DataComponentPatch patch; // 只拿额外组件，理论上完全足够了
    private long stackSize;

    // 统一缓存，避免频繁 new ItemStack
    private ItemStack serverCache;
    private ItemStack clientCache;
    private int vanillaStackSize = -1; // 缓存原版堆叠大小

    private int hashCodeCache = 0;
    private boolean NeedRecalHash = true;

    // Patch -> NBT -> 递归顺序规范化 -> 写为非压缩字节
    /** 规范化后的 NBT 字节缓存（用于 equals/hash），使用前必须以ensureByte刷新，空patch会返回空标记（有长度的非空数组），provider未就绪则返回空数组*/
    private byte[] equalsByte = new byte[0];
    /** 生成 equalsByte 时使用的注册表 epoch */
    private long RegistryEpochCache = -1L;

    public ItemStackType() {
        this.item = Items.AIR;
        this.patch = DataComponentPatch.EMPTY;
        this.stackSize = 0;
        this.serverCache = ItemStack.EMPTY;
        this.clientCache = ItemStack.EMPTY;
    }

    public ItemStackType(ItemStack stack) {
        this.item = stack.getItem();
        this.patch = stack.getComponentsPatch(); // 这里的返回值在后续实际上无法被修改，因此无需担心
        this.stackSize = stack.getCount();
        this.serverCache = new ItemStack(RegistryUtil.holderOf(this.item), 1, this.patch); // this.patch直接传递引用即可，PatchedDataComponentMap会在第一次修改前替换内部引用
        this.clientCache = new ItemStack(RegistryUtil.holderOf(this.item), 1, this.patch);
    }

    public ItemStackType(ItemStack stack, long stackSize) {
        this.item = stack.getItem();
        this.patch = stack.getComponentsPatch();
        this.stackSize = stackSize;
        this.serverCache = new ItemStack(RegistryUtil.holderOf(this.item), 1, this.patch);
        this.clientCache = new ItemStack(RegistryUtil.holderOf(this.item), 1, this.patch);
    }

    // 仅供内部使用，不直接对外暴露
    private ItemStackType(Item item, DataComponentPatch patch, long stackSize) {
        this.item = item;
        this.patch = patch == null ? DataComponentPatch.EMPTY : patch;
        this.stackSize = stackSize;
        this.serverCache = this.item == Items.AIR ? ItemStack.EMPTY : new ItemStack(RegistryUtil.holderOf(this.item), 1, this.patch);
        this.clientCache = this.item == Items.AIR ? ItemStack.EMPTY : new ItemStack(RegistryUtil.holderOf(this.item), 1, this.patch);
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
            // 未知路径的清洗一遍后再送入
            DataComponentPatch p = dataComponentPatch != null && !dataComponentPatch.isEmpty() ? PatchedDataComponentMap.fromPatch(it.components(),dataComponentPatch).asPatch() : DataComponentPatch.EMPTY;
            return new ItemStackType(it, p, amount);
        }
        return null;
    }

    // 不要在任何路径调用上修改它的任何部分！
    @Override
    public ItemStack getStack()
    {
        // 返回缓存对象，并把数量同步为当前 long（clamp 到 int）
        if (this.serverCache.isEmpty() || this.serverCache.getItem() != this.item) {
            this.serverCache = this.item == Items.AIR ? ItemStack.EMPTY
                    : new ItemStack(RegistryUtil.holderOf(this.item), 1, this.patch);
            NeedRecalHash = true; // 极少发生：item 变化
        }
        this.serverCache.setCount(BDMath.clampLongToInt(this.stackSize));
        return this.serverCache;
    }

    @Override
    public void setStack(ItemStack stack)
    {
        this.item = stack.getItem();
        this.patch = stack.getComponentsPatch();
        this.stackSize = stack.getCount();
        this.serverCache = new ItemStack(RegistryUtil.holderOf(this.item), 1, this.patch);
        this.clientCache = new ItemStack(RegistryUtil.holderOf(this.item), 1, this.patch);
        NeedRecalHash = true;
        vanillaStackSize = -1; // 强制要求重算
        this.equalsByte = new byte[0]; // 失效字节缓存
        this.RegistryEpochCache = -1L;
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
        return Items.AIR; // 总是返回AIR，这是目前接口的定义
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
                : new ItemStack(RegistryUtil.holderOf(this.item), 1, this.patch);
        return base.copyWithCount(BDMath.clampLongToInt(this.stackSize));
    }

    @Override
    public ItemStack copyStackWithCount(long count) {
        ItemStack base = this.item == Items.AIR ? ItemStack.EMPTY
                : new ItemStack(RegistryUtil.holderOf(this.item), 1, this.patch);
        return base.copyWithCount(BDMath.clampLongToInt(count));
    }

    @Override
    public IStackType<ItemStack> copy() {
        ItemStackType cp = new ItemStackType(this.item, this.patch, this.stackSize);
        cp.NeedRecalHash = this.NeedRecalHash;
        cp.hashCodeCache = this.hashCodeCache;
        // 拷贝字节缓存与 epoch
        cp.equalsByte = (this.equalsByte == null ? null : Arrays.copyOf(this.equalsByte, this.equalsByte.length));
        cp.RegistryEpochCache = this.RegistryEpochCache;
        return cp;
    }

    @Override
    public IStackType<ItemStack> copyWithCount(long count) {
        ItemStackType cp = new ItemStackType(this.item, this.patch, count);
        cp.NeedRecalHash = this.NeedRecalHash;
        cp.hashCodeCache = this.hashCodeCache;
        // 拷贝字节缓存与 epoch
        cp.equalsByte = (this.equalsByte == null ? null : Arrays.copyOf(this.equalsByte, this.equalsByte.length));
        cp.RegistryEpochCache = this.RegistryEpochCache;
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
    public long getVanillaMaxStackSize()
    {
        // 此处已经判过AIR，如果不是，不可能继续往后走，故后续只要是isEmpty则证明需要重设
        if (this.item == Items.AIR) return 1; // 返回1，与原版行为一致，且不会使外部认为无法输入
        if(vanillaStackSize<=0)
        {
            if (this.serverCache.isEmpty() || this.serverCache.getItem() != this.item) {
                this.serverCache = new ItemStack(RegistryUtil.holderOf(this.item), 1, this.patch);
            }
            vanillaStackSize = serverCache.getMaxStackSize();
        }
        return Math.min(vanillaStackSize, getCustomMaxStackSize());
    }

    @Override
    public long getCustomMaxStackSize() {
        // 可配置化的最大堆叠尺寸
        return CUSTOM_MAX_STACK_SIZE;
    }

    @Override
    public ItemStack splitStack(long amount)
    {
        if (amount <= 0 || this.item == Items.AIR) return ItemStack.EMPTY;
        int splitAmount = BDMath.clampLongToInt(Math.min(amount, this.stackSize));
        shrink(splitAmount);
        return new ItemStack(RegistryUtil.holderOf(this.item), splitAmount, this.patch);
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
        return RegistryUtil.holderOf(this.item).is(itemTag);
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
            if (this.item != otherItemStackType.item) return false;

            // 确保两侧都有规范化后的字节快照
            this.ensureByte();
            otherItemStackType.ensureByte();

            if (this.equalsByte != null && this.equalsByte.length > 0 &&
                    otherItemStackType.equalsByte != null && otherItemStackType.equalsByte.length > 0) {
                return Arrays.equals(this.equalsByte, otherItemStackType.equalsByte);
            }
            // 回退：在 Provider 尚未就绪或异常时，退回到 patch 的值语义
            return Objects.equals(this.patch, otherItemStackType.patch);
        }
        return false;
    }

    // 网络序列化
    @Override
    public void serialize(RegistryFriendlyByteBuf buf)
    {
        // 1) 类型ID
        buf.writeResourceLocation(getTypeId());

        // 2) 是否有物品（AIR 视为无）
        boolean hasItem = this.item != Items.AIR;
        buf.writeBoolean(hasItem);
        if (!hasItem) return;

        // 3) 数量
        buf.writeVarLong(this.stackSize);

        // 4) 物品 ID（ResourceLocation）
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(this.item);
        buf.writeResourceLocation(key);

        // 差异组件
        DataComponentPatch.STREAM_CODEC.encode(buf, patch);
    }

    @Override
    public ItemStackType deserialize(RegistryFriendlyByteBuf buf,ResourceLocation typeId) {
        if (!typeId.equals(getTypeId())) return null; //必要的，标识未能读取任何内容，用于外部处理

        // 1) 是否有物品
        boolean hasItem = buf.readBoolean();
        if (!hasItem) return new ItemStackType(ItemStack.EMPTY);

        // 2) 数量
        long amount = buf.readVarLong();

        // 3) 物品 ID（未知或已移除 → 回退 AIR）
        ResourceLocation key = buf.readResourceLocation();
        Item it = BuiltInRegistries.ITEM.get(key); // 内部实现已经处理了null清空，未注册项目返回Items.AIR

        // 4) 组件差异
        DataComponentPatch patch = DataComponentPatch.STREAM_CODEC.decode(buf);

        return new ItemStackType(it, patch, amount);
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider levelRegistryAccess) {
        final CompoundTag out = new CompoundTag();
        try {
            // 兼容外部依赖：保留旧字段 Type
            out.putString("Type", ID.toString());

            // 写回采用与 NEW_FMT/TYPE_CODEC 完全一致的键：item / components / amount
            var ops = levelRegistryAccess.createSerializationContext(NbtOps.INSTANCE);
            CODEC.encodeStart(ops, this)
                    .resultOrPartial(err -> BeyondDimensions.LOGGER.warn("ItemStackType 在序列化(Codec)时出错: {}", err))
                    .ifPresent(nbt -> {
                        if (nbt instanceof CompoundTag ct) {
                            // 合并到根层；额外保留的 Type 不会被覆盖
                            out.merge(ct);
                        } else {
                            // 理论上不会发生：MapCodec 应该产出 CompoundTag
                            out.put("value", nbt);
                        }
                    });

        } catch (Throwable t) {
            // 写入异常不抛出，避免 IO 过程中崩溃
            BeyondDimensions.LOGGER.error("ItemStackType 在序列化时出错: {}", t.getMessage(), t);
        }
        return out;
    }

    @Override
    public ItemStackType deserializeNBT(CompoundTag nbt, HolderLookup.Provider levelRegistryAccess) {
        try {
            // 1) 新格式：直接用与写入一致的 CODEC（TYPE_CODEC）
            var ops = levelRegistryAccess.createSerializationContext(NbtOps.INSTANCE);
            var decoded = CODEC.parse(ops, nbt).result();
            if (decoded.isPresent()) {
                return decoded.get();
            }

            // 2) 嵌套回退：如果被偶然 "value" 包裹
            if (nbt.contains("value", Tag.TAG_COMPOUND)) {
                var valDecoded = CODEC.parse(ops, nbt.getCompound("value")).result();
                if (valDecoded.isPresent()) {
                    return valDecoded.get();
                }
            }

            // 3) 旧格式回退 A：Item + Components + Amount（大写键）
            if (nbt.contains("Item", Tag.TAG_STRING)) {
                ResourceLocation key = ResourceLocation.tryParse(nbt.getString("Item"));
                Item it = (key != null) ? BuiltInRegistries.ITEM.get(key) : null;
                if (it == null) {
                    BeyondDimensions.LOGGER.warn("ItemStackType 反序列化旧格式时未找到物品: '{}', 回退 AIR", nbt.getString("Item"));
                    it = Items.AIR;
                }
                long amount = nbt.getLong("Amount");

                DataComponentPatch p = DataComponentPatch.EMPTY;
                if (nbt.contains("Components")) {
                    p = DataComponentPatch.CODEC.parse(ops, nbt.get("Components"))
                            .resultOrPartial(err -> BeyondDimensions.LOGGER.warn("ItemStackType 反序列化旧组件时出错: {}", err))
                            .orElse(DataComponentPatch.EMPTY);
                }
                return new ItemStackType(it, p, amount);
            }

            // 4) 旧格式回退 B：Stack + Amount
            if (nbt.contains("Stack", Tag.TAG_COMPOUND)) {
                ItemStack s = ItemStack.parseOptional(levelRegistryAccess, nbt.getCompound("Stack"));
                long amount = nbt.getLong("Amount");
                return new ItemStackType(s, amount);
            }

            BeyondDimensions.LOGGER.warn("ItemStackType 反序列化时：新旧格式均不匹配，返回空实现。Keys={}", nbt.getAllKeys());
        } catch (Throwable t) {
            BeyondDimensions.LOGGER.error("ItemStackType 反序列化出现错误。Keys={} Error={}", nbt.getAllKeys(), t.getMessage(), t);
        }
        return new ItemStackType(); // 空实现
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void render(net.minecraft.client.gui.GuiGraphics gui,int x, int y) {
        // 渲染物品图标
        var poseStack = gui.pose(); // 获取渲染的变换矩阵
        poseStack.pushPose(); // 保存矩阵状态
        if (this.clientCache.isEmpty() || this.clientCache.getItem() != this.item) { // 这是个极其轻量的检查，即使是每帧渲染也不可能卡顿
            this.clientCache = this.item == Items.AIR ? ItemStack.EMPTY
                    : new ItemStack(RegistryUtil.holderOf(this.item), 1, this.patch);
        }
        this.clientCache.setCount(1); // 防止数量被设为0后getItem返回EMPTY 实际数量单独绘制 此处仅为客户端使用，数量错误也无问题
        gui.renderFakeItem(clientCache, x, y);
        gui.renderItemDecorations(Minecraft.getInstance().font, clientCache, x, y, "");
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
        if(!clientCache.isEmpty())
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
        if (this.clientCache.isEmpty() || this.clientCache.getItem() != this.item) {
            this.clientCache = this.item == Items.AIR ? ItemStack.EMPTY
                    : new ItemStack(RegistryUtil.holderOf(this.item), 1, this.patch);
        }
        return clientCache.getDisplayName();
    }

    @Override
    public List<Component> getTooltipLines(Item.TooltipContext tooltipContext, @Nullable Player player, TooltipFlag tooltipFlag)
    {
        if (this.clientCache.isEmpty() || this.clientCache.getItem() != this.item) {
            this.clientCache = this.item == Items.AIR ? ItemStack.EMPTY
                    : new ItemStack(RegistryUtil.holderOf(this.item), 1, this.patch);
        }
        List<Component> tooltips = clientCache.getTooltipLines(tooltipContext,player,tooltipFlag);
        tooltips.add(Component.translatable("istack.beyonddimensions.storage_num.item", getStackAmount()));
        return tooltips;
    }

    @Override
    public Optional<TooltipComponent> getTooltipImage()
    {
        if (this.clientCache.isEmpty() || this.clientCache.getItem() != this.item) {
            this.clientCache = this.item == Items.AIR ? ItemStack.EMPTY
                    : new ItemStack(RegistryUtil.holderOf(this.item), 1, this.patch);
        }
        return clientCache.getTooltipImage();
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void renderTooltip(net.minecraft.client.gui.GuiGraphics gui,net.minecraft.client.gui.Font font, int mouseX, int mouseY)
    {
        var minecraft = Minecraft.getInstance();
        var ctx = minecraft.level != null ? Item.TooltipContext.of(minecraft.level)
                : Item.TooltipContext.EMPTY; // 或提供你自己的空上下文
        gui.renderTooltip(minecraft.font, this.getTooltipLines(ctx,minecraft.player, ClientTooltipFlag.of(minecraft.options.advancedItemTooltips ? TooltipFlag.Default.ADVANCED : TooltipFlag.Default.NORMAL))
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
        // 当 item/patch 变化或 epoch 变化，需要重算
        long curEpoch = RegistryAccessResolver.epoch();
        if (NeedRecalHash || this.RegistryEpochCache != curEpoch || this.equalsByte == null || this.equalsByte.length == 0) {
            ensureByte(); // 尝试生成/刷新字节；失败时会保留空字节以便回退
            int base = 31 + item.hashCode();
            int patchPart = (this.equalsByte != null && this.equalsByte.length > 0)
                    ? Arrays.hashCode(this.equalsByte)
                    : patch.hashCode(); // 回退，Provider未就绪或者发生别的异常时启用
            hashCodeCache = 31 * base + patchPart;
            NeedRecalHash = false;
        }
        return hashCodeCache;
    }

    private void ensureByte()
    {
        long cur = RegistryAccessResolver.epoch();

        // 缓存仍有效：epoch 未变、字节已算出且没有 item/patch 改动触发 NeedRecalHash
        if (this.equalsByte != null && this.equalsByte.length > 0
                && this.RegistryEpochCache == cur && !NeedRecalHash) {
            return;
        }

        try {
            // 空补丁快速路径：工具类也会返回常量 EMPTY_BYTES，这里直接走工具类即可
            HolderLookup.Provider provider = RegistryAccessResolver.current();
            this.equalsByte = DataComponentPatchHelper.toCanonicalBytes(this.patch, provider);
            this.RegistryEpochCache = cur;
        } catch (Throwable t) {
            // 不让逻辑中断，留给 equals/hashCode 回退到 patch.equals/hash
            BeyondDimensions.LOGGER.warn("ItemStackType.ensureByte failed: {}", t.toString());
        }
    }
}

