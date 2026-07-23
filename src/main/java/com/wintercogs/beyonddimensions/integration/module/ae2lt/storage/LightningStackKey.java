package com.wintercogs.beyonddimensions.integration.module.ae2lt.storage;

import com.moakiee.ae2lt.api.lightning.LightningTier;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import com.wintercogs.beyonddimensions.api.storage.key.IStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.IStackRender;
import com.wintercogs.beyonddimensions.api.storage.key.KeyAmount;
import com.wintercogs.beyonddimensions.api.storage.key.impl.LongStackKey;
import com.wintercogs.beyonddimensions.integration.OtherModIds;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.stream.Stream;

public final class LightningStackKey extends LongStackKey<LightningType>
{
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(
            BDConstants.MODID, "stack_type/lightning");
    public static final LightningStackKey HIGH_VOLTAGE = new LightningStackKey(LightningTier.HIGH_VOLTAGE);
    public static final LightningStackKey EXTREME_HIGH_VOLTAGE = new LightningStackKey(LightningTier.EXTREME_HIGH_VOLTAGE);
    public static final MapCodec<LightningStackKey> TYPE_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            LightningTier.CODEC.fieldOf("tier").forGetter(LightningStackKey::tier)
    ).apply(instance, LightningStackKey::of));

    private final LightningTier tier;

    private LightningStackKey(LightningTier tier)
    {
        this.tier = Objects.requireNonNull(tier);
        this.stack = new LightningType(tier, 0);
    }

    public static LightningStackKey of(LightningTier tier)
    {
        return tier == LightningTier.EXTREME_HIGH_VOLTAGE ? EXTREME_HIGH_VOLTAGE : HIGH_VOLTAGE;
    }

    public LightningTier tier()
    {
        return tier;
    }

    @Override
    public ResourceLocation getTypeID()
    {
        return ID;
    }

    @Override
    public MapCodec<LightningStackKey> codec()
    {
        return TYPE_CODEC;
    }

    @Override
    public @Nullable KeyAmount fromStackObject(Object stack)
    {
        return stack instanceof LightningType lightning
                ? new KeyAmount(of(lightning.tier()), lightning.getStackCount())
                : null;
    }

    @Override
    public @Nullable LightningStackKey fromSourceObject(Object key, DataComponentPatch ignored)
    {
        if (key instanceof LightningType lightning) return of(lightning.tier());
        if (key instanceof LightningTier lightningTier) return of(lightningTier);
        return null;
    }

    @Override
    public String getModId()
    {
        return OtherModIds.AE2_LIGHTNING_TECH;
    }

    @Override
    public LightningStackKey getEmpty()
    {
        return HIGH_VOLTAGE;
    }

    @Override
    public @NotNull LightningType getSource()
    {
        return stack;
    }

    @Override
    public LightningType getEmptyStack()
    {
        return new LightningType(tier, 0);
    }

    @Override
    public boolean hasTag(TagKey<?> tagKey)
    {
        return false;
    }

    @Override
    public Stream<? extends TagKey<?>> getTags()
    {
        return Stream.empty();
    }

    @Override
    public boolean isSame(IStackKey<?> other)
    {
        return other instanceof LightningStackKey lightning && lightning.tier == tier;
    }

    @Override
    public boolean isSameTypeSameComponents(IStackKey<?> other)
    {
        return isSame(other);
    }

    @Override
    public boolean equals(Object other)
    {
        return other instanceof LightningStackKey lightning && lightning.tier == tier;
    }

    @Override
    public int hashCode()
    {
        return 31 * ID.hashCode() + tier.hashCode();
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf)
    {
        LightningTier.STREAM_CODEC.encode(buf, tier);
    }

    @Override
    public @NotNull LightningStackKey deserialize(RegistryFriendlyByteBuf buf)
    {
        return of(LightningTier.STREAM_CODEC.decode(buf));
    }

    @Override
    public @NotNull CompoundTag serializeNBT(HolderLookup.Provider access)
    {
        CompoundTag tag = new CompoundTag();
        tag.putString("tier", tier.getSerializedName());
        return tag;
    }

    @Override
    public @NotNull LightningStackKey deserializeNBT(CompoundTag nbt, HolderLookup.Provider access)
    {
        return of(LightningTier.fromSerializedName(nbt.getString("tier")));
    }

    @Override
    public @NotNull IStackRender getRender()
    {
        return LightningStackKeyRender.INSTANCE;
    }
}
