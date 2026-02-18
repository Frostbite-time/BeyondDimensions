package com.wintercogs.beyonddimensions.Api.DataBase.Stack;

import com.wintercogs.beyonddimensions.Api.Util.NbtEq;
import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.Util.BDMath;
import com.wintercogs.beyonddimensions.Util.NbtCanonicalBytesHelper;
import com.wintercogs.beyonddimensions.Util.RegistryUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * 1.20.1: Key = fluid + tag
 * 结构/用法对齐 1.21.1 风格：不可变、Key层不带数量、read-only/render缓存、序列化仅写Key负载。
 * 大部分类似字段的具体信息请参考1.21.1版本实现
 */
public final class FluidStackKey implements IStackKey<FluidStack>
{
    public static final ResourceLocation ID =
            ResourceLocation.tryBuild(BeyondDimensions.MODID, "stack_type/fluid");

    public static final FluidStackKey EMPTY = new FluidStackKey(Fluids.EMPTY, null);

    private static final long CUSTOM_MAX_STACK_SIZE = Long.MAX_VALUE;

    // ===== 不可变要素（Key 语义）=====
    private final Fluid fluid;
    private final @Nullable CompoundTag tag;

    // ===== 缓存字段（不参与 key 语义）=====
    private transient FluidStack serverCache; // amount 恒为 1
    private transient FluidStack clientCache; // amount 恒为 1
    private transient byte[] signatureBytes;  // 规范化字节快照（只包含 tag）
    private transient int hashCache;
    private transient boolean hashReady;

    private FluidStackKey(Fluid fluid, @Nullable CompoundTag tag)
    {
        this.fluid = (fluid == null) ? Fluids.EMPTY : fluid;
        this.tag = (tag == null) ? null : tag.copy();
    }

    public FluidStackKey(FluidStack stack)
    {
        this(stack.getFluid(), copyTagOrNull(stack.getTag()));
    }

    // ===== IStackKey =====

    @Override
    public ResourceLocation getTypeId()
    {
        return ID;
    }

    @Override
    public @Nullable KeyAmount fromStackObject(Object stack)
    {
        if (stack instanceof FluidStack s)
        {
            return new KeyAmount(new FluidStackKey(s), s.getAmount());
        }
        return null;
    }

    @Override
    public @Nullable IStackKey<FluidStack> fromSourceObject(Object key, CompoundTag dataComponentPatch)
    {
        if (key instanceof Fluid f)
        {
            CompoundTag t = (dataComponentPatch == null) ? null : dataComponentPatch.copy();
            return new FluidStackKey(f, t);
        }
        return null;
    }

    @Override
    public FluidStack getReadOnlyStack()
    {
        if (this.serverCache == null)
        {
            this.serverCache = this.fluid == Fluids.EMPTY ? FluidStack.EMPTY : buildFluidStack(this.fluid, this.tag, 1);
        }

        if (this.fluid == Fluids.EMPTY)
        {
            if (!this.serverCache.isEmpty())
            {
                this.serverCache = FluidStack.EMPTY;
            }
            return FluidStack.EMPTY;
        }

        FluidStack cache = this.serverCache;
        if (cache.isEmpty() || cache.getFluid() != this.fluid)
        {
            this.serverCache = buildFluidStack(this.fluid, this.tag, 1);
            return this.serverCache;
        }

        // 非 EMPTY：返回前保证 amount = 1
        cache.setAmount(1);
        return cache;
    }

    @Override
    public Class<FluidStack> getStackClass()
    {
        return FluidStack.class;
    }

    @Override
    public @NotNull Fluid getSource()
    {
        return fluid;
    }

    @Override
    public Class<?> getSourceClass()
    {
        return Fluid.class;
    }

    @Override
    public String getModId()
    {
        if (this.fluid == Fluids.EMPTY) return "minecraft";
        ResourceLocation key = ForgeRegistries.FLUIDS.getKey(this.fluid);
        if (key == null) key = BuiltInRegistries.FLUID.getKey(this.fluid);
        return key.getNamespace();
    }

    @Override
    public boolean isEmpty()
    {
        return this == EMPTY || this.fluid == Fluids.EMPTY;
    }

    @Override
    public IStackKey<FluidStack> getEmpty()
    {
        return EMPTY;
    }

    @Override
    public FluidStack getEmptyStack()
    {
        return FluidStack.EMPTY;
    }

    @Override
    public FluidStack copyStack()
    {
        return copyStackWithCount(1);
    }

    @Override
    public FluidStack copyStackWithCount(long count)
    {
        if (this.fluid == Fluids.EMPTY) return FluidStack.EMPTY;
        return buildFluidStack(this.fluid, this.tag, BDMath.clampLongToInt(count));
    }

    @Override
    public long getVanillaMaxStackSize()
    {
        return Math.min(64_000L, getCustomMaxStackSize());
    }

    @Override
    public long getCustomMaxStackSize()
    {
        return CUSTOM_MAX_STACK_SIZE;
    }

    @Override
    public boolean hasTag(TagKey<?> tagKey)
    {
        if (tagKey == null || this.fluid == Fluids.EMPTY) return false;
        if (!tagKey.isFor(Registries.FLUID)) return false;

        @SuppressWarnings("unchecked")
        TagKey<Fluid> fluidTag = (TagKey<Fluid>) tagKey;
        return RegistryUtil.holderOf(this.fluid).is(fluidTag);
    }

    @Override
    public Stream<? extends TagKey<?>> getTags()
    {
        return RegistryUtil.holderOf(this.fluid).tags();
    }

    @Override
    public boolean isSame(IStackKey<?> other)
    {
        if (this == other) return true;
        if (other instanceof FluidStackKey o)
        {
            return this.fluid == o.fluid;
        }
        return false;
    }

    @Override
    public boolean isSameTypeSameComponents(IStackKey<?> other)
    {
        if (this == other) return true;
        if (!(other instanceof FluidStackKey o)) return false;
        if (this.fluid != o.fluid) return false;

        // 优先字节签名比较
        this.ensureSignatureBytes();
        o.ensureSignatureBytes();
        if (this.signatureBytes.length > 0 && o.signatureBytes.length > 0)
        {
            return Arrays.equals(this.signatureBytes, o.signatureBytes);
        }

        // 回退：relaxed NBT equals
        return NbtEq.equalsRelaxed(this.tag, o.tag);
    }

    /**
     * 网络序列化：只写 payload（typeId 由 IStackKey.serializeCommon 写）
     */
    @Override
    public void serialize(FriendlyByteBuf buf)
    {
        boolean hasFluid = this.fluid != Fluids.EMPTY;
        buf.writeBoolean(hasFluid);
        if (!hasFluid) return;

        buf.writeId(BuiltInRegistries.FLUID, this.fluid);
        buf.writeNbt(this.tag);
    }

    @Override
    public @NotNull IStackKey<FluidStack> deserialize(FriendlyByteBuf buf)
    {
        boolean hasFluid = buf.readBoolean();
        if (!hasFluid) return EMPTY;

        Fluid f = buf.readById(BuiltInRegistries.FLUID);
        CompoundTag tag = buf.readNbt();
        return new FluidStackKey(f, tag);
    }

    /**
     * NBT 序列化：只写 payload（外层由 serializeNBTCommon 写 type）
     */
    @Override
    public @NotNull CompoundTag serializeNBT()
    {
        CompoundTag out = new CompoundTag();

        ResourceLocation id = ForgeRegistries.FLUIDS.getKey(this.fluid);
        if (id == null) id = BuiltInRegistries.FLUID.getKey(this.fluid);
        out.putString("fluid", id.toString());

        if (this.tag != null) out.put("tag", this.tag.copy());

        return out;
    }

    @Override
    public @NotNull IStackKey<FluidStack> deserializeNBT(CompoundTag nbt)
    {
        if (nbt == null) return EMPTY;

        // 新
        if (nbt.contains("fluid", net.minecraft.nbt.Tag.TAG_STRING))
        {
            return readNewFmt(nbt);
        }
        //旧
        if (nbt.contains("Stack", net.minecraft.nbt.Tag.TAG_COMPOUND))
        {
            return fromLegacyTypedStack(nbt.getCompound("Stack"));
        }
        return readNewFmt(nbt);
    }

    private @NotNull IStackKey<FluidStack> readNewFmt(@NotNull CompoundTag nbt)
    {
        ResourceLocation id = ResourceLocation.tryParse(nbt.getString("fluid"));
        Fluid f = Fluids.EMPTY;

        if (id != null)
        {
            Fluid fromForge = ForgeRegistries.FLUIDS.getValue(id);
            f = Objects.requireNonNullElseGet(fromForge, () -> BuiltInRegistries.FLUID.get(id));
        }

        CompoundTag tag = nbt.contains("tag", Tag.TAG_COMPOUND) ? nbt.getCompound("tag") : null;
        return new FluidStackKey(f, tag);
    }

    private @NotNull IStackKey<FluidStack> fromLegacyTypedStack(@NotNull CompoundTag stackNbt)
    {
        try
        {
            FluidStack fs = FluidStack.loadFluidStackFromNBT(stackNbt);
            if (fs == null || fs.isEmpty())
            {
                return EMPTY;
            }

            Fluid f = fs.getFluid();
            CompoundTag tag = fs.hasTag() ? fs.getTag() : null;
            return new FluidStackKey(f, tag);
        }
        catch (Throwable t)
        {
            return EMPTY;
        }
    }

    @Override
    public @NotNull IStackRender getRender()
    {
        return FluidStackKeyRender.INSTANCE;
    }

    @Override
    public @NotNull FluidStack getRenderStack()
    {
        if (this.clientCache == null)
        {
            this.clientCache = this.fluid == Fluids.EMPTY ? FluidStack.EMPTY : buildFluidStack(this.fluid, this.tag, 1);
        }

        if (this.fluid == Fluids.EMPTY)
        {
            if (!this.clientCache.isEmpty())
            {
                this.clientCache = FluidStack.EMPTY;
            }
            return FluidStack.EMPTY;
        }

        FluidStack cache = this.clientCache;
        if (cache.isEmpty() || cache.getFluid() != this.fluid)
        {
            this.clientCache = buildFluidStack(this.fluid, this.tag, 1);
            return this.clientCache;
        }

        cache.setAmount(1);
        return cache;
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        if (other instanceof FluidStackKey o) return isSameTypeSameComponents(o);
        return false;
    }

    @Override
    public int hashCode()
    {
        if (!hashReady)
        {
            ensureSignatureBytes();
            int base = 31 + fluid.hashCode();
            int nbtPart = (signatureBytes.length > 0)
                    ? Arrays.hashCode(signatureBytes)
                    : NbtEq.hashRelaxed(tag);
            hashCache = 31 * base + nbtPart;
            hashReady = true;
        }
        return hashCache;
    }

    // ===== internals =====

    private static @Nullable CompoundTag copyTagOrNull(@Nullable CompoundTag in)
    {
        return in == null ? null : in.copy();
    }

    private static @NotNull FluidStack buildFluidStack(Fluid fluid, @Nullable CompoundTag tag, int amount)
    {
        return new FluidStack(fluid, amount, copyTagOrNull(tag));
    }

    private void ensureSignatureBytes()
    {
        if (this.signatureBytes != null && this.signatureBytes.length > 0) return;

        byte[] out = NbtCanonicalBytesHelper.toCanonicalBytes(this.tag, null);
        this.signatureBytes = (out != null) ? out : new byte[0];
    }
}