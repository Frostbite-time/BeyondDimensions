package com.wintercogs.beyonddimensions.Api.DataBase.Stack;

import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.Unit.BDMath;
import com.wintercogs.beyonddimensions.Unit.DataComponentPatchHelper;
import com.wintercogs.beyonddimensions.Unit.RegistryAccessResolver;
import com.wintercogs.beyonddimensions.Unit.RegistryUtil;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.PatchedDataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Stream;

public final class ItemStackKey implements IStackKey<ItemStack>
{
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(BeyondDimensions.MODID, "stack_type/item");
    public static final ItemStackKey EMPTY = new ItemStackKey();

    private static final long CUSTOM_MAX_STACK_SIZE = Long.MAX_VALUE; // 自定义堆叠大小

    // 新的CODEC，写入时使用
    private static final MapCodec<ItemStackKey> NEW_FMT = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    BuiltInRegistries.ITEM.holderByNameCodec().fieldOf("item")
                            .forGetter(t -> RegistryUtil.holderOf(t.item)),
                    DataComponentPatch.CODEC.optionalFieldOf("components", DataComponentPatch.EMPTY)
                            .forGetter(t -> t.patch)
            ).apply(instance, (holder, patch) -> new ItemStackKey(holder.value(), patch))
    );

    // 旧格式CODEC
    private static final Codec<ItemStackKey> LEGACY_CODEC = RecordCodecBuilder.create(inst -> inst.group(
            ItemStack.OPTIONAL_CODEC.fieldOf("internal_stack").forGetter(ItemStackKey::copyStack)
    ).apply(inst, (stack) -> new ItemStackKey(stack)));

    // 读时新优先、无新字段则读旧；写时永远用新
    public static final MapCodec<ItemStackKey> TYPE_CODEC = new MapCodec<>() {
        @Override
        public <T> DataResult<ItemStackKey> decode(DynamicOps<T> ops, MapLike<T> input) {
            final T kItem = ops.createString("item");
            final T kLegacy = ops.createString("internal_stack");
            final T kComps  = ops.createString("components");

            T hasItem = input.get(kItem);

            if (hasItem != null) {
                // 先按新格式正常解码
                DataResult<ItemStackKey> r = NEW_FMT.decode(ops, input);
                if (r.result().isPresent()) return r;
                DataComponentPatch patch = DataComponentPatch.EMPTY;
                T compsNode = input.get(kComps);
                if (compsNode != null) {
                    patch = DataComponentPatch.CODEC.parse(ops, compsNode).result().orElse(DataComponentPatch.EMPTY);
                }
                return DataResult.success(new ItemStackKey(Items.AIR, patch));
            }

            // 尝试旧格式
            T hasLegacy = input.get(kLegacy);
            if (hasLegacy != null)
            {
                java.util.Map<T, T> map = new java.util.LinkedHashMap<>();
                input.entries().forEach(p -> map.put(p.getFirst(), p.getSecond()));
                T node = ops.createMap(map);
                return LEGACY_CODEC.decode(ops, node).map(com.mojang.datafixers.util.Pair::getFirst);
            }

            // 都不满足：让 NEW_FMT 报出明确缺字段信息
            return NEW_FMT.decode(ops, input);
        }

        @Override
        public <T> RecordBuilder<T> encode(ItemStackKey value, DynamicOps<T> ops, RecordBuilder<T> prefix) {
            // 写出：始终用新格式
            return NEW_FMT.encode(value, ops, prefix);
        }

        @Override
        public <T> Stream<T> keys(DynamicOps<T> ops) {
            // 向外暴露新格式的键集合
            return Stream.of("item", "components").map(ops::createString);
        }

    };

    public static final Codec<ItemStackKey> CODEC = TYPE_CODEC.codec();

    // 实际存储-持久化保存和网络传输均以此为准
    private final Item item;
    private final DataComponentPatch patch; // 只拿额外组件，理论上完全足够了

    // 识别字段-用于hashcode和equals，用于解决组件可能未正确实现hashcode和equals的问题，同时也能处理数字值为NaN时的比较
    private byte[] patchByte = new byte[0]; // patch -> NBT -> 递归排序 -> 写为非压缩字节
    private transient WeakReference<HolderLookup.Provider> equalsByteProviderRef = null; // 序列化时所用的注册表提供者，如果提供者变化，应当重新计算字节

    // 缓存字段
    private ItemStack clientCache; // 用于客户端的缓存，主要给getTooltip之类的方法使用
    private int vanillaMaxSize = -1; // 缓存原版堆叠的最大大小，从serverCache去获取
    private int hashCodeCache = 0;

    private ItemStackKey()
    {
        this.item = Items.AIR;
        this.patch = DataComponentPatch.EMPTY;
        this.clientCache = ItemStack.EMPTY;
    }

    public ItemStackKey(ItemStack stack)
    {
        this.item = stack.getItem();
        this.patch = stack.getComponentsPatch(); // 这里的返回值在后续实际上无法被修改，因此无需担心
        this.clientCache = new ItemStack(RegistryUtil.holderOf(this.item), 1, this.patch);
    }

    // 仅供内部使用，不直接对外暴露
    private ItemStackKey(Item item, DataComponentPatch patch)
    {
        this.item = item;
        this.patch = patch == null ? DataComponentPatch.EMPTY : patch;
        this.clientCache = this.item == Items.AIR ? ItemStack.EMPTY : new ItemStack(RegistryUtil.holderOf(this.item), 1, this.patch);
    }

    @Override
    public ResourceLocation getTypeId()
    {
        return ID;
    }

    @Override
    public MapCodec<ItemStackKey> codec()
    {
        return TYPE_CODEC;
    }

    @Override
    public @Nullable KeyAmount fromStackObject(Object stack)
    {
        if(stack instanceof ItemStack itemStack)
            return new KeyAmount(new ItemStackKey(itemStack), itemStack.getCount());
        return null;
    }

    @Override
    public @Nullable ItemStackKey fromSourceObject(Object key, DataComponentPatch dataComponentPatch)
    {
        if (key instanceof Item it) {
            // 未知路径的清洗一遍后再送入
            DataComponentPatch p = dataComponentPatch != null && !dataComponentPatch.isEmpty() ? PatchedDataComponentMap.fromPatch(it.components(),dataComponentPatch).asPatch() : DataComponentPatch.EMPTY;
            return new ItemStackKey(it, p);
        }
        return null;
    }

    @Override
    public Class<ItemStack> getStackClass()
    {
        return ItemStack.class;
    }

    @Override
    public @NotNull Item getSource()
    {
        return item;
    }

    @Override
    public Class<?> getSourceClass()
    {
        return Item.class;
    }

    @Override
    public String getModId()
    {
        return BuiltInRegistries.ITEM.getKey(item).getNamespace();
    }

    @Override
    public boolean isEmpty()
    {
        return this == ItemStackKey.EMPTY || this.item == Items.AIR;
    }

    @Override
    public ItemStackKey getEmpty()
    {
        return ItemStackKey.EMPTY;
    }

    @Override
    public ItemStack getEmptyStack()
    {
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack copyStack()
    {
        return copyStackWithCount(1);
    }

    @Override
    public ItemStack copyStackWithCount(long count)
    {
        return this.item == Items.AIR ? ItemStack.EMPTY
                : new ItemStack(RegistryUtil.holderOf(this.item), BDMath.clampLongToInt(count), this.patch);
    }

    @Override
    public long getVanillaMaxStackSize()
    {
        // 此处已经判过AIR，如果不是，不可能继续往后走，故后续只要是isEmpty则证明需要重设
        if (this.item == Items.AIR) return 1; // 返回1，与原版行为一致，且不会使外部认为无法输入
        if(vanillaMaxSize <=0)
        {
            vanillaMaxSize = new ItemStack(RegistryUtil.holderOf(this.item), 1, this.patch).getMaxStackSize();
        }
        return Math.min(vanillaMaxSize, getCustomMaxStackSize());
    }

    @Override
    public long getCustomMaxStackSize()
    {
        // 可配置化的最大堆叠尺寸
        return CUSTOM_MAX_STACK_SIZE;
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
    public boolean isSame(IStackKey<?> other)
    {
        if(this == other) return true; // 额外做一次引用对比
        if(other instanceof ItemStackKey otherItemStackKey) // 顺手处理空
        {
            return this.item == otherItemStackKey.item; // 直接比对
        }
        return false;
    }

    @Override
    public boolean isSameTypeSameComponents(IStackKey<?> other)
    {
        if(this == other) return true; // 额外做一次引用对比
        if(other instanceof ItemStackKey otherKey) // 会顺带处理null
        {
            if (this.item != otherKey.item) return false;

            // 确保两侧都有规范化后的字节快照
            this.ensureByte();
            otherKey.ensureByte();

            if (this.patchByte != null && this.patchByte.length > 0
                    && otherKey.patchByte != null && otherKey.patchByte.length > 0)
            {
                return Arrays.equals(this.patchByte, otherKey.patchByte);
            }

            // 回退：在 Provider 尚未就绪或异常时，退回到 patch 的值语义
            return Objects.equals(this.patch, otherKey.patch);
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

        // 3) 物品 ID（ResourceLocation）
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(this.item);
        buf.writeResourceLocation(key);

        // 差异组件
        DataComponentPatch.STREAM_CODEC.encode(buf, patch);
    }

    @Override
    public ItemStackKey deserialize(RegistryFriendlyByteBuf buf,ResourceLocation typeId)
    {
        if (!typeId.equals(getTypeId())) return null; //必要的，标识未能读取任何内容，用于外部处理

        // 1) 是否有物品
        boolean hasItem = buf.readBoolean();
        if (!hasItem) return new ItemStackKey(ItemStack.EMPTY);

        // 2) 物品 ID（未知或已移除 → 回退 AIR）
        ResourceLocation key = buf.readResourceLocation();
        Item it = BuiltInRegistries.ITEM.get(key); // 内部实现已经处理了null清空，未注册项目返回Items.AIR

        // 3) 组件差异
        DataComponentPatch patch = DataComponentPatch.STREAM_CODEC.decode(buf);

        return new ItemStackKey(it, patch);
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider levelRegistryAccess)
    {
        final CompoundTag out = new CompoundTag();
        try {
            // 兼容外部依赖：保留旧字段 Type
            out.putString("Type", ID.toString());

            // 写回采用与 NEW_FMT/TYPE_CODEC 完全一致的键：item / components
            var ops = levelRegistryAccess.createSerializationContext(NbtOps.INSTANCE);
            CODEC.encodeStart(ops, this)
                    .resultOrPartial(err -> BeyondDimensions.LOGGER.warn("ItemStackKey 在序列化(Codec)时出错: {}", err))
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
            BeyondDimensions.LOGGER.error("ItemStackKey 在序列化时出错: {}", t.getMessage(), t);
        }
        return out;
    }

    @Override
    public ItemStackKey deserializeNBT(CompoundTag nbt, HolderLookup.Provider levelRegistryAccess)
    {
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
                    BeyondDimensions.LOGGER.warn("ItemStackKey 反序列化旧格式时未找到物品: '{}', 回退 AIR", nbt.getString("Item"));
                    it = Items.AIR;
                }

                DataComponentPatch p = DataComponentPatch.EMPTY;
                if (nbt.contains("Components")) {
                    p = DataComponentPatch.CODEC.parse(ops, nbt.get("Components"))
                            .resultOrPartial(err -> BeyondDimensions.LOGGER.warn("ItemStackKey 反序列化旧组件时出错: {}", err))
                            .orElse(DataComponentPatch.EMPTY);
                }
                return new ItemStackKey(it, p);
            }

            // 4) 旧格式回退 B：Stack + Amount
            if (nbt.contains("Stack", Tag.TAG_COMPOUND)) {
                ItemStack s = ItemStack.parseOptional(levelRegistryAccess, nbt.getCompound("Stack"));
                return new ItemStackKey(s);
            }

            BeyondDimensions.LOGGER.warn("ItemStackKey 反序列化时：新旧格式均不匹配，返回空实现。Keys={}", nbt.getAllKeys());
        } catch (Throwable t) {
            BeyondDimensions.LOGGER.error("ItemStackKey 反序列化出现错误。Keys={} Error={}", nbt.getAllKeys(), t.getMessage(), t);
        }
        return ItemStackKey.EMPTY; // 空实现
    }

    @Override
    public IStackRender getRender()
    {
        return ItemStackKeyRender.INSTANCE;
    }

    @Override
    public ItemStack getRenderStack()
    {
        // item为空时，必须返回 EMPTY，且不要对 EMPTY 调用 setCount(方便复制到1.20.1的流体实现去)
        if (this.item == Items.AIR)
        {
            if (!this.clientCache.isEmpty())
            {
                this.clientCache = ItemStack.EMPTY; // 折叠为 EMPTY，防止外界留存非空引用
            }
            return ItemStack.EMPTY;
        }

        // 非AIR：若为空或物品被外界改了，则重建（数量直接置 1）
        ItemStack cache = this.clientCache;
        if (cache.isEmpty() || cache.getItem() != this.item) {
            this.clientCache = new ItemStack(RegistryUtil.holderOf(this.item), 1, this.patch);
            return this.clientCache;
        }

        // 缓存非空且物品匹配，返回前设置数量为1
        cache.setCount(1);
        return cache;
    }

    @Override
    public boolean equals(Object other)
    {
        if(this == other) return true;
        if(other instanceof ItemStackKey otherKey) // 此处会顺带处理null
        {
            return this.isSameTypeSameComponents(otherKey);
        }
        return false;
    }

    @Override
    public int hashCode()
    {
        if (hashCodeCache == 0 || this.patchByte == null || this.patchByte.length == 0)
        {
            ensureByte(); // 尝试生成/刷新字节；失败时会保留空字节以便回退
            int base = 31 + item.hashCode();
            int patchPart = (this.patchByte != null && this.patchByte.length > 0)
                    ? Arrays.hashCode(this.patchByte)
                    : patch.hashCode(); // 回退，Provider未就绪或者发生别的异常时启用
            hashCodeCache = 31 * base + patchPart;
        }
        return hashCodeCache;
    }

    private void ensureByte()
    {
        // 优先使用最优提供者（Server→Connection→Level→Builtin）
        HolderLookup.Provider current = null;
        try {
            current = RegistryAccessResolver.resolve();
        } catch (Throwable ignored) {
            // 保持null，后面会兜底
        }

        // 如果已有有效缓存，且提供者身份不变，则保持不继续计算，直接使用缓存
        HolderLookup.Provider cached = (equalsByteProviderRef != null) ? equalsByteProviderRef.get() : null;
        if (this.patchByte != null && this.patchByte.length > 0 // 字节已经计算完毕
                && cached != null && cached == current) { // 注册表提供者身份不变
            return;
        }

        try { // 客户端断线重连后可能会为了缓存tooltip的IStackType有一次cached导致的计算，这是正常的

            // 1) 用当前 Provider 先算一次（空补丁会得到稳定的非空 EMPTY_BYTES，length不会为0，仅有失败时为0）
            // 若当前provider不可用，则用内建表
            HolderLookup.Provider use = (current != null) ? current : RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
            byte[] out = DataComponentPatchHelper.toCanonicalBytes(this.patch, use);

            // 2) 客户端兜底：若失败（返回长度==0），再强制用 Connection 的 Provider 重试一次
            if (out.length == 0 && net.neoforged.fml.loading.FMLEnvironment.dist == net.neoforged.api.distmarker.Dist.CLIENT) {
                var mc = net.minecraft.client.Minecraft.getInstance();
                var conn = mc.getConnection();
                if (conn != null) {
                    HolderLookup.Provider connProv = conn.registryAccess();
                    if (connProv != use) { // 提供者和已经失败的提供者不应为同一人
                        byte[] retry = DataComponentPatchHelper.toCanonicalBytes(this.patch, connProv);
                        if (retry.length > 0) {
                            out = retry;
                            use = connProv;
                        }
                    }
                }
            }

            // 3) 写入缓存；失败则清空 Provider 绑定
            this.patchByte = out;
            if (out.length > 0) {
                this.equalsByteProviderRef = new java.lang.ref.WeakReference<>(use);
            } else {
                this.equalsByteProviderRef = null;
            }
        } catch (Throwable t) {
            // 不让逻辑中断，留给 equals/hashCode 回退到 patch.equals/hash
            BeyondDimensions.LOGGER.warn("ItemStackKey字节序列化失败: {}", t.toString());
            this.patchByte = new byte[0];
            this.equalsByteProviderRef = null;
        }
    }
}
