package com.wintercogs.beyonddimensions.api.storage.key.impl;

import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import com.wintercogs.beyonddimensions.api.storage.key.IStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.IStackRender;
import com.wintercogs.beyonddimensions.api.storage.key.KeyAmount;
import com.wintercogs.beyonddimensions.api.storage.key.render.ItemStackKeyRender;
import com.wintercogs.beyonddimensions.util.BDMath;
import com.wintercogs.beyonddimensions.util.DataComponentPatchHelper;
import com.wintercogs.beyonddimensions.util.RegistryAccessResolver;
import com.wintercogs.beyonddimensions.util.RegistryUtil;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.PatchedDataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
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
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(BDConstants.MODID, "stack_type/item");
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

    // 最终产出的 MapCodec（编码始终走新格式；解码按代次兼容）
    public static final MapCodec<ItemStackKey> TYPE_CODEC = new MapCodec<>()
    {

        // —— 统一键名，便于维护 —— //
        private static final String K_ITEM = "item";
        private static final String K_COMPS = "components";
        // 1代旧键名
        private static final String K_ITEM_OLD = "Item";
        private static final String K_COMPS_OLD = "Components";
        // 2代旧键名
        private static final String K_STACK = "Stack";
        // 3代旧键名
        private static final String K_LEGACY = "internal_stack";
        // 注意：旧数据中可能还有 amount/Amount；Key 层面不需要，读取时**忽略**即可
        private static final String K_AMOUNT = "amount";
        private static final String K_AMOUNT_OLD = "Amount";

        @Override
        public <T> DataResult<ItemStackKey> decode(DynamicOps<T> ops, MapLike<T> input)
        {
            final T kItem = ops.createString(K_ITEM);
            final T kComps = ops.createString(K_COMPS);
            final T kItemOld = ops.createString(K_ITEM_OLD);
            final T kCompsOld = ops.createString(K_COMPS_OLD);
            final T kStack = ops.createString(K_STACK);
            final T kLegacy = ops.createString(K_LEGACY);
            final T kAmt = ops.createString(K_AMOUNT);
            final T kAmtOld = ops.createString(K_AMOUNT_OLD);

            // 新格式 item + components
            if (input.get(kItem) != null)
            {
                DataResult<ItemStackKey> r = NEW_FMT.decode(ops, input);
                if (r.result().isPresent()) return r;

                // 失败时使用Items.AIR的宽松回退
                DataComponentPatch patch = DataComponentPatch.EMPTY;
                T compsNode = input.get(kComps);
                if (compsNode != null)
                {
                    patch = DataComponentPatch.CODEC.parse(ops, compsNode)
                            .result().orElse(DataComponentPatch.EMPTY);
                }
                return DataResult.success(new ItemStackKey(Items.AIR, patch));
            }

            // 旧格式 Item + Components 转为新格式然后转交给新版本解码
            if (input.get(kItemOld) != null || input.get(kCompsOld) != null)
            {
                java.util.Map<T, T> map = new java.util.LinkedHashMap<>();
                input.entries().forEach(p -> {
                    T key = p.getFirst();
                    if (key.equals(kItemOld)) key = kItem;   // Item -> item
                    else if (key.equals(kCompsOld)) key = kComps; // Components -> components
                    // Key 层忽略 amount/Amount
                    if (!key.equals(kAmt) && !key.equals(kAmtOld))
                    {
                        map.put(key, p.getSecond());
                    }
                });
                T remapped = ops.createMap(map);
                return NEW_FMT.codec().decode(ops, remapped).map(com.mojang.datafixers.util.Pair::getFirst);
            }

            // internal_stack 或 Stack 直接内嵌ItemStack的型态，解析ItemStack后转交给ItemStackKey构造
            T legacyNode = input.get(kLegacy);
            if (legacyNode == null) legacyNode = input.get(kStack);
            if (legacyNode != null) return ItemStack.OPTIONAL_CODEC.parse(ops, legacyNode).map(ItemStackKey::new);

            // 如果上述都不符合，最终仍给新格式
            return NEW_FMT.decode(ops, input);
        }

        @Override
        public <T> RecordBuilder<T> encode(ItemStackKey value, DynamicOps<T> ops, RecordBuilder<T> prefix)
        {
            // 仅写新格式
            return NEW_FMT.encode(value, ops, prefix);
        }

        @Override
        public <T> java.util.stream.Stream<T> keys(DynamicOps<T> ops)
        {
            // 对外暴露新格式键集合
            return java.util.stream.Stream.of(K_ITEM, K_COMPS).map(ops::createString);
        }
    };

    public static final Codec<ItemStackKey> CODEC = TYPE_CODEC.codec();

    // 实际存储-持久化保存和网络传输均以此为准
    private final Item item;
    private final DataComponentPatch patch; // 只拿额外组件，理论上完全足够了

    // 识别字段-用于hashcode和equals，用于解决组件可能未正确实现hashcode和equals的问题，同时也能处理数字值为NaN时的比较
    private byte[] patchByte = new byte[0]; // patch -> NBT -> 递归排序 -> 写为非压缩字节
    private transient WeakReference<HolderLookup.Provider> equalsByteProviderRef = null; // 序列化时所用的注册表提供者，如果提供者变化，应当重新计算字节

    // 与原patch尝试断开关系的patchCache
    // 主要为了防止只读形式的ItemStack在外部受到不可预知的修改，使得patch变化
    private transient DataComponentPatch detachedPatchCache = null;
    private transient WeakReference<HolderLookup.Provider> detachedPatchProviderRef = null;

    // 缓存字段
    private ItemStack serverCache; // 用于非渲染用途的缓存，始终保持对外数量为1（懒加载）
    private ItemStack clientCache; // 用于客户端的缓存，主要给getTooltip之类的方法使用（懒加载）
    private int vanillaMaxSize = -1; // 缓存原版堆叠的最大大小，从serverCache去获取
    private int hashCodeCache = 0;

    private ItemStackKey()
    {
        this(Items.AIR, DataComponentPatch.EMPTY);
    }

    public ItemStackKey(ItemStack stack)
    {
        this(stack.getItem(), stack.getComponentsPatch());
    }

    // 仅供内部使用，不直接对外暴露
    private ItemStackKey(Item item, DataComponentPatch patch)
    {
        this.item = item;
        this.patch = patch == null ? DataComponentPatch.EMPTY : patch;
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
        if (stack instanceof ItemStack itemStack)
            return new KeyAmount(new ItemStackKey(itemStack), itemStack.getCount());
        return null;
    }

    @Override
    public @Nullable ItemStackKey fromSourceObject(Object key, DataComponentPatch dataComponentPatch)
    {
        if (key instanceof Item it)
        {
            // 未知路径的清洗一遍后再送入
            DataComponentPatch p = dataComponentPatch != null && !dataComponentPatch.isEmpty() ? PatchedDataComponentMap.fromPatch(it.components(), dataComponentPatch).asPatch() : DataComponentPatch.EMPTY;
            return new ItemStackKey(it, p);
        }
        return null;
    }

    @Override
    public ItemStack getReadOnlyStack()
    {
        if (this.serverCache == null)
        {
            this.serverCache = this.item == Items.AIR ? ItemStack.EMPTY : new ItemStack(RegistryUtil.holderOf(this.item), 1, this.getDetachedPatch());
        }
        // item为空时，必须返回 EMPTY，且不要对 EMPTY 调用 setCount(方便复制到1.20.1的流体实现去)
        if (this.item == Items.AIR)
        {
            if (!this.serverCache.isEmpty())
            {
                this.serverCache = ItemStack.EMPTY; // 折叠为 EMPTY，防止外界留存非空引用
            }
            return ItemStack.EMPTY;
        }

        // 非AIR：若为空或物品被外界改了，则重建（数量直接置 1）
        ItemStack cache = this.serverCache;
        if (cache.isEmpty() || cache.getItem() != this.item)
        {
            this.serverCache = new ItemStack(RegistryUtil.holderOf(this.item), 1, this.getDetachedPatch());
            return this.serverCache;
        }

        // 缓存非空且物品匹配，返回前设置数量为1
        cache.setCount(1);
        return cache;
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
                : new ItemStack(RegistryUtil.holderOf(this.item), BDMath.clampLongToInt(count), this.getDetachedPatch());
    }

    @Override
    public long getVanillaMaxStackSize()
    {
        // 此处已经判过AIR，如果不是，不可能继续往后走，故后续只要是isEmpty则证明需要重设
        if (this.item == Items.AIR) return 1; // 返回1，与原版行为一致，且不会使外部认为无法输入
        if (vanillaMaxSize <= 0)
        {
            vanillaMaxSize = new ItemStack(RegistryUtil.holderOf(this.item), 1, this.getDetachedPatch()).getMaxStackSize();
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
        @SuppressWarnings("unchecked")
        TagKey<Item> itemTag = (TagKey<Item>) tagKey;
        return RegistryUtil.holderOf(this.item).is(itemTag);
    }

    @Override
    public Stream<? extends TagKey<?>> getTags()
    {
        return RegistryUtil.holderOf(this.item).tags();
    }

    @Override
    public boolean isSame(IStackKey<?> other)
    {
        if (this == other) return true; // 额外做一次引用对比
        if (other instanceof ItemStackKey otherItemStackKey) // 顺手处理空
        {
            return this.item == otherItemStackKey.item; // 直接比对
        }
        return false;
    }

    @Override
    public boolean isSameTypeSameComponents(IStackKey<?> other)
    {
        if (this == other) return true; // 额外做一次引用对比
        if (other instanceof ItemStackKey otherKey) // 会顺带处理null
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
    public @NotNull ItemStackKey deserialize(RegistryFriendlyByteBuf buf)
    {
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
    public @NotNull CompoundTag serializeNBT(HolderLookup.Provider levelRegistryAccess)
    {
        try
        {
            var ops = levelRegistryAccess.createSerializationContext(NbtOps.INSTANCE);
            return CODEC.encodeStart(ops, this)
                    .resultOrPartial(err -> BeyondDimensions.LOGGER.warn(
                            "ItemStackKey 在序列化(Codec)时出错: {}", err))
                    .map(nbt -> {
                        if (nbt instanceof CompoundTag ct)
                        {
                            // 直接返回编码结果；避免无意义的再包装
                            return ct;
                        }
                        else
                        {
                            // 理论上不应发生：MapCodec 在 NbtOps 下应产生 CompoundTag
                            BeyondDimensions.LOGGER.error(
                                    "ItemStackKey 序列化得到的 NBT 非 CompoundTag，已丢弃该结果: {}",
                                    nbt.getClass().getName());
                            return new CompoundTag();
                        }
                    })
                    .orElseGet(CompoundTag::new); // 编码失败：给空 CompoundTag
        }
        catch (Throwable t)
        {
            BeyondDimensions.LOGGER.error("ItemStackKey 在序列化时异常: {}", t.getMessage(), t);
            return new CompoundTag();
        }
    }

    @Override
    public @NotNull ItemStackKey deserializeNBT(CompoundTag nbt, HolderLookup.Provider levelRegistryAccess)
    {
        try
        {
            var ops = levelRegistryAccess.createSerializationContext(NbtOps.INSTANCE);
            return CODEC.parse(ops, nbt)
                    .resultOrPartial(err -> BeyondDimensions.LOGGER.warn(
                            "ItemStackKey 在反序列化(Codec)时出错: {} | Keys={}", err, nbt.getAllKeys()))
                    .orElse(ItemStackKey.EMPTY);
        }
        catch (Throwable t)
        {
            BeyondDimensions.LOGGER.error("ItemStackKey 反序列化异常。Keys={} Error={}",
                    nbt.getAllKeys(), t.getMessage(), t);
            return ItemStackKey.EMPTY;
        }
    }


    @Override
    public @NotNull IStackRender getRender()
    {
        return ItemStackKeyRender.INSTANCE;
    }

    @Override
    public @NotNull ItemStack getRenderStack()
    {
        if (this.clientCache == null)
        {
            this.clientCache = this.item == Items.AIR ? ItemStack.EMPTY : new ItemStack(RegistryUtil.holderOf(this.item), 1, this.getDetachedPatch());
        }
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
        if (cache.isEmpty() || cache.getItem() != this.item)
        {
            this.clientCache = new ItemStack(RegistryUtil.holderOf(this.item), 1, this.getDetachedPatch());
            return this.clientCache;
        }

        // 缓存非空且物品匹配，返回前设置数量为1
        cache.setCount(1);
        return cache;
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        if (other instanceof ItemStackKey otherKey) // 此处会顺带处理null
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
        try
        {
            current = RegistryAccessResolver.resolve();
        }
        catch (Throwable ignored)
        {
            // 保持null，后面会兜底
        }

        // 如果已有有效缓存，且提供者身份不变，则保持不继续计算，直接使用缓存
        HolderLookup.Provider cached = (equalsByteProviderRef != null) ? equalsByteProviderRef.get() : null;
        if (this.patchByte != null && this.patchByte.length > 0 // 字节已经计算完毕
                && cached != null && cached == current)
        { // 注册表提供者身份不变
            return;
        }

        try
        { // 客户端断线重连后可能会为了缓存tooltip的IStackType有一次cached导致的计算，这是正常的

            // 1) 用当前 Provider 先算一次（空补丁会得到稳定的非空 EMPTY_BYTES，length不会为0，仅有失败时为0）
            // 若当前provider不可用，则用内建表
            HolderLookup.Provider use = (current != null) ? current : RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
            byte[] out = DataComponentPatchHelper.toCanonicalBytes(this.patch, use);

            // 2) 客户端兜底：若失败（返回长度==0），再强制用 Connection 的 Provider 重试一次
            if (out.length == 0 && net.neoforged.fml.loading.FMLEnvironment.dist == net.neoforged.api.distmarker.Dist.CLIENT)
            {
                var mc = net.minecraft.client.Minecraft.getInstance();
                var conn = mc.getConnection();
                if (conn != null)
                {
                    HolderLookup.Provider connProv = conn.registryAccess();
                    if (connProv != use)
                    { // 提供者和已经失败的提供者不应为同一人
                        byte[] retry = DataComponentPatchHelper.toCanonicalBytes(this.patch, connProv);
                        if (retry.length > 0)
                        {
                            out = retry;
                            use = connProv;
                        }
                    }
                }
            }

            // 3) 写入缓存；失败则清空 Provider 绑定
            this.patchByte = out;
            if (out.length > 0)
            {
                this.equalsByteProviderRef = new java.lang.ref.WeakReference<>(use);
            }
            else
            {
                this.equalsByteProviderRef = null;
            }
        }
        catch (Throwable t)
        {
            // 不让逻辑中断，留给 equals/hashCode 回退到 patch.equals/hash
            BeyondDimensions.LOGGER.warn("ItemStackKey字节序列化失败: {}", t.toString());
            this.patchByte = new byte[0];
            this.equalsByteProviderRef = null;
        }
    }

    private DataComponentPatch getDetachedPatch()
    {
        if (this.patch == null || this.patch.isEmpty())
        {
            this.detachedPatchCache = DataComponentPatch.EMPTY;
            this.detachedPatchProviderRef = null;
            return DataComponentPatch.EMPTY;
        }

        HolderLookup.Provider current = null;
        try
        {
            current = RegistryAccessResolver.resolve();
        }
        catch (Throwable ignored)
        {
        }

        HolderLookup.Provider cached = this.detachedPatchProviderRef != null ? this.detachedPatchProviderRef.get() : null;
        if (this.detachedPatchCache != null && cached != null && cached == current)
        {
            return this.detachedPatchCache;
        }

        try
        {
            HolderLookup.Provider use = current != null ? current : RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
            DataComponentPatch detached = DataComponentPatchHelper.detachWithBuiltinFallback(this.patch, use);
            this.detachedPatchCache = detached;
            this.detachedPatchProviderRef = new WeakReference<>(use);
            return detached;
        }
        catch (Throwable t)
        {
            BeyondDimensions.LOGGER.warn("ItemStackKey patch 断开失败: {}", t.toString());
            this.detachedPatchCache = this.patch;
            this.detachedPatchProviderRef = null;
            return this.patch;
        }
    }
}
