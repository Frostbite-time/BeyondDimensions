package com.wintercogs.beyonddimensions.Api.DataBase.Stack;

import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.wintercogs.beyonddimensions.BeyondDimensions;
import mekanism.api.MekanismAPI;
import mekanism.api.chemical.Chemical;
import mekanism.api.chemical.ChemicalStack;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.stream.Stream;

public class ChemicalStackKey implements IStackKey<ChemicalStack> {

    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(BeyondDimensions.MODID, "stack_type/chemical");

    // 新格式：仅化学品注册名
    private static final MapCodec<ChemicalStackKey> NEW_FMT = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    ResourceLocation.CODEC.fieldOf("chemical")
                            .forGetter(t -> MekanismAPI.CHEMICAL_REGISTRY.getKey(t.chemical))
            ).apply(instance, (rl) -> {
                Chemical c = MekanismAPI.CHEMICAL_REGISTRY.get(rl);
                if (c == null) c = MekanismAPI.EMPTY_CHEMICAL; // 兜底
                return new ChemicalStackKey(c);
            })
    );

    // 旧格式：internal_stack（忽略 amount）
    private static final Codec<ChemicalStackKey> LEGACY_CODEC = RecordCodecBuilder.create(inst -> inst.group(
            ChemicalStack.OPTIONAL_CODEC.fieldOf("internal_stack")
                    .forGetter(ChemicalStackKey::legacyGetter)
    ).apply(inst, (stack) -> new ChemicalStackKey(stack)));

    public static final MapCodec<ChemicalStackKey> TYPE_CODEC = new MapCodec<>() {
        @Override
        public <T> DataResult<ChemicalStackKey> decode(DynamicOps<T> ops, MapLike<T> input) {
            final T kNew = ops.createString("chemical");
            final T kOld = ops.createString("internal_stack");

            if (input.get(kNew) != null) {
                return NEW_FMT.decode(ops, input);
            }
            if (input.get(kOld) != null) {
                // 将整个 map 交给旧 codec
                java.util.Map<T,T> map = new java.util.LinkedHashMap<>();
                input.entries().forEach(p -> map.put(p.getFirst(), p.getSecond()));
                T node = ops.createMap(map);
                return LEGACY_CODEC.decode(ops, node).map(com.mojang.datafixers.util.Pair::getFirst);
            }
            // 触发 NEW_FMT 的缺键报错
            return NEW_FMT.decode(ops, input);
        }

        @Override
        public <T> RecordBuilder<T> encode(ChemicalStackKey value, DynamicOps<T> ops, RecordBuilder<T> prefix) {
            return NEW_FMT.encode(value, ops, prefix);
        }

        @Override
        public <T> Stream<T> keys(DynamicOps<T> ops) {
            return Stream.of("chemical").map(ops::createString);
        }
    };

    public static final Codec<ChemicalStackKey> CODEC = TYPE_CODEC.codec();

    // —— 实际不可变要素 —— //
    private final Chemical chemical;

    // 客户端渲染/复制缓存（amount≥1，仅用于显示，不参与 Key 语义）
    private ChemicalStack clientCache;

    public ChemicalStackKey() {
        this.chemical = MekanismAPI.EMPTY_CHEMICAL;
        this.clientCache = ChemicalStack.EMPTY;
    }

    public ChemicalStackKey(ChemicalStack stack) {
        this(stack == null ? MekanismAPI.EMPTY_CHEMICAL : stack.getChemical());
    }

    private ChemicalStackKey(Chemical chemical) {
        this.chemical = (chemical == null) ? MekanismAPI.EMPTY_CHEMICAL : chemical;
        this.clientCache = this.chemical.isEmptyType() ? ChemicalStack.EMPTY : new ChemicalStack(this.chemical, 1);
    }

    // 仅用于旧 codec 的虚拟 getter（不会被 encode 调用）
    private ChemicalStack legacyGetter() {
        return getRenderStack();
    }

    // ===== IStackKey =====

    @Override
    public ResourceLocation getTypeId() {
        return ID;
    }

    @Override
    public MapCodec<ChemicalStackKey> codec() {
        return TYPE_CODEC;
    }

    @Override
    public @Nullable ChemicalStackKey fromObject(Object key, DataComponentPatch ignored) {
        if (key instanceof Chemical c) {
            return new ChemicalStackKey(c);
        }
        return null;
    }

    @Override
    public Class<ChemicalStack> getStackClass() {
        return ChemicalStack.class;
    }

    @Override
    public @NotNull Object getSource() {
        return chemical;
    }

    @Override
    public Class<?> getSourceClass() {
        return Chemical.class;
    }

    @Override
    public String getModId() {
        ResourceLocation rl = MekanismAPI.CHEMICAL_REGISTRY.getKey(chemical);
        return rl == null ? "mekanism" : rl.getNamespace();
    }

    @Override
    public boolean isEmpty() {
        return chemical.isEmptyType();
    }

    @Override
    public ChemicalStackKey getEmpty() {
        return new ChemicalStackKey();
    }

    @Override
    public ChemicalStack getEmptyStack() {
        return ChemicalStack.EMPTY;
    }

    @Override
    public ChemicalStack copyStack() {
        return copyStackWithCount(1);
    }

    @Override
    public ChemicalStack copyStackWithCount(long count) {
        if (chemical.isEmptyType()) return ChemicalStack.EMPTY;
        long amt = Math.max(1, Math.min(Integer.MAX_VALUE, count));
        return new ChemicalStack(chemical, amt);
    }

    @Override
    public long getVanillaMaxStackSize() {
        // 仅用于 UI/逻辑辅助（不参与 Key）
        return Math.min(64_000L, getCustomMaxStackSize());
    }

    @Override
    public long getCustomMaxStackSize() {
        return Long.MAX_VALUE;
    }

    @Override
    public boolean hasTag(TagKey<?> tagKey) {
        if (tagKey == null || chemical.isEmptyType()) return false;
        if (!tagKey.isFor(MekanismAPI.CHEMICAL_REGISTRY_NAME)) return false;
        @SuppressWarnings("unchecked")
        TagKey<Chemical> t = (TagKey<Chemical>) tagKey;
        ResourceLocation key = MekanismAPI.CHEMICAL_REGISTRY.getKey(chemical);
        if (key == null) return false;
        return MekanismAPI.CHEMICAL_REGISTRY.getHolder(key).map(h -> h.is(t)).orElse(false);
    }

    @Override
    public boolean isSame(IStackKey<?> other) {
        if (this == other) return true;
        if (other instanceof ChemicalStackKey o) {
            return this.chemical == o.chemical;
        }
        return false;
    }

    @Override
    public boolean isSameTypeSameComponents(IStackKey<?> other) {
        // Chemical 无额外组件，组件相等与 isSame 一致
        return isSame(other);
    }

    // —— 网络序列化（新形态）——

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        buf.writeResourceLocation(getTypeId());
        boolean has = !chemical.isEmptyType();
        buf.writeBoolean(has);
        if (!has) return;

        ResourceLocation rl = MekanismAPI.CHEMICAL_REGISTRY.getKey(chemical);
        if (rl == null) rl = ResourceLocation.tryBuild("mekanism", "empty");
        buf.writeResourceLocation(rl);
    }

    @Override
    public ChemicalStackKey deserialize(RegistryFriendlyByteBuf buf, ResourceLocation typeId) {
        if (!typeId.equals(getTypeId())) return null;

        boolean has = buf.readBoolean();
        if (!has) return new ChemicalStackKey();

        ResourceLocation rl = buf.readResourceLocation();
        Chemical c = MekanismAPI.CHEMICAL_REGISTRY.get(rl);
        if (c == null) c = MekanismAPI.EMPTY_CHEMICAL;
        return new ChemicalStackKey(c);
    }

    // —— NBT：写新；读新优先 + 旧兼容 —— //

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider levelRegistryAccess) {
        CompoundTag out = new CompoundTag();
        try {
            out.putString("Type", ID.toString());
            var ops = levelRegistryAccess.createSerializationContext(NbtOps.INSTANCE);
            CODEC.encodeStart(ops, this)
                    .resultOrPartial(err -> BeyondDimensions.LOGGER.warn("ChemicalStackKey 序列化(Codec)出错: {}", err))
                    .ifPresent(nbt -> {
                        if (nbt instanceof CompoundTag ct) out.merge(ct);
                        else out.put("value", nbt);
                    });
        } catch (Throwable t) {
            BeyondDimensions.LOGGER.error("ChemicalStackKey 序列化时出错: {}", t.getMessage(), t);
        }
        return out;
    }

    @Override
    public ChemicalStackKey deserializeNBT(CompoundTag nbt, HolderLookup.Provider levelRegistryAccess) {
        try {
            var ops = levelRegistryAccess.createSerializationContext(NbtOps.INSTANCE);
            var decoded = CODEC.parse(ops, nbt).result();
            if (decoded.isPresent()) return decoded.get();

            if (nbt.contains("value", Tag.TAG_COMPOUND)) {
                var val = CODEC.parse(ops, nbt.getCompound("value")).result();
                if (val.isPresent()) return val.get();
            }

            // 旧格式：Stack(+Amount)
            if (nbt.contains("Stack", Tag.TAG_COMPOUND)) {
                ChemicalStack s = ChemicalStack.parseOptional(levelRegistryAccess, nbt.getCompound("Stack"));
                return new ChemicalStackKey(s);
            }

            BeyondDimensions.LOGGER.warn("ChemicalStackKey 反序列化：新旧均不匹配，返回 EMPTY。Keys={}", nbt.getAllKeys());
        } catch (Throwable t) {
            BeyondDimensions.LOGGER.error("ChemicalStackKey 反序列化错误。Keys={} Error={}", nbt.getAllKeys(), t.getMessage(), t);
        }
        return new ChemicalStackKey();
    }

    // —— 渲染支持 —— //

    @Override
    public IStackRender<?> getRender() {
        return ChemicalStackKeyRender.INSTANCE;
    }

    @Override
    public ChemicalStack getRenderStack() {
        if (chemical.isEmptyType()) {
            if (!clientCache.isEmpty()) clientCache = ChemicalStack.EMPTY;
            return ChemicalStack.EMPTY;
        }
        ChemicalStack cache = clientCache;
        if (cache.isEmpty() || cache.getChemical() != this.chemical) {
            clientCache = new ChemicalStack(this.chemical, 1);
            return clientCache;
        }
        if (cache.getAmount() <= 0) cache.setAmount(1);
        return cache;
    }

    // —— equals/hash —— //

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o instanceof ChemicalStackKey k) {
            return this.chemical == k.chemical;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return 31 + Objects.hashCode(this.chemical);
    }
}