package com.wintercogs.beyonddimensions.api.storage.key.impl;

import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import com.wintercogs.beyonddimensions.api.longtype.EnergyType;
import com.wintercogs.beyonddimensions.api.storage.key.IStackRender;
import com.wintercogs.beyonddimensions.api.storage.key.KeyAmount;
import com.wintercogs.beyonddimensions.api.storage.key.render.EnergyStackKeyRender;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Stream;

public class EnergyStackKey extends LongStackKey<EnergyType>
{

    public static final ResourceLocation ID =
            ResourceLocation.tryBuild(BDConstants.MODID, "stack_type/energy");

    /**
     * 唯一实例（不区分空/非空）
     */
    public static final EnergyStackKey INSTANCE = new EnergyStackKey();

    private EnergyStackKey()
    {
        this.stack = new EnergyType(0);
    }

    // ---------------- IStackKey ----------------
    @Override
    public @Nullable KeyAmount fromStackObject(Object stack)
    {
        if (stack instanceof EnergyType energyType)
            return new KeyAmount(EnergyStackKey.INSTANCE, energyType.getStackCount());
        return null;
    }

    @Override
    public ResourceLocation getTypeID()
    {
        return ID;
    }

    @Override
    public long getVanillaMaxStackSize()
    {
        return 1000000;
    }

    /**
     * 允许从 EnergyType/数字（数量无意义）转换为同一个 Key 实例
     */
    @Override
    public @Nullable EnergyStackKey fromSourceObject(Object key, CompoundTag ignored)
    {
        if (key instanceof EnergyType || key instanceof Number)
        {
            return INSTANCE;
        }
        return null;
    }

    @Override
    public @NotNull EnergyType getSource()
    {
        return this.stack;
    }

    @Override
    public String getModId()
    {
        return "NeoForge";
    }

    @Override
    public EnergyStackKey getEmpty()
    {
        return EnergyStackKey.INSTANCE;
    }

    @Override
    public EnergyType getEmptyStack()
    {
        return new EnergyType(0);
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
    public void serialize(FriendlyByteBuf buf)
    {
    }

    @Override
    public @NotNull EnergyStackKey deserialize(FriendlyByteBuf buf)
    {
        return INSTANCE;
    }

    @Override
    public @NotNull CompoundTag serializeNBT()
    {
        return new CompoundTag();
    }

    @Override
    public @NotNull EnergyStackKey deserializeNBT(CompoundTag nbt)
    {
        return INSTANCE;
    }

    @Override
    public @NotNull IStackRender getRender()
    {
        return EnergyStackKeyRender.INSTANCE;
    }
}