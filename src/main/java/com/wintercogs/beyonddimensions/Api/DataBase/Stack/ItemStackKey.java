package com.wintercogs.beyonddimensions.Api.DataBase.Stack;

import com.wintercogs.beyonddimensions.Api.Util.NbtEq;
import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.Unit.BDMath;
import com.wintercogs.beyonddimensions.Unit.NbtCanonicalBytesHelper;
import com.wintercogs.beyonddimensions.Unit.RegistryUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.stream.Stream;

/**
 * 1.20.1: Key = item + tag + caps
 * 结构/用法对齐 1.21.1 ItemStackKey：不可变、Key层不带数量、提供read-only/render缓存、序列化仅写Key负载。
 * 大部分类似字段的具体信息请参考1.21.1版本实现
 */
public final class ItemStackKey implements IStackKey<ItemStack>
{

    public static final ResourceLocation ID =
            ResourceLocation.tryBuild(BeyondDimensions.MODID, "stack_type/item");

    public static final ItemStackKey EMPTY = new ItemStackKey(Items.AIR, null, null);

    private static final long CUSTOM_MAX_STACK_SIZE = Long.MAX_VALUE;

    @Nullable
    public static CompoundTag serializeStackCaps(ItemStack stack)
    {
        try
        {
            var caps = stack.serializeCaps();
            return (caps == null || caps.isEmpty()) ? null : caps;
        }
        catch (Throwable ex)
        {
            throw new RuntimeException("Failed to call serializeCaps", ex);
        }
    }

    public static void deserializeStackCaps(ItemStack stack, @Nullable CompoundTag caps)
    {
        try
        {
            if (caps != null && !caps.isEmpty())
            {
                stack.deserializeCaps(caps);
            }
        }
        catch (Throwable ex)
        {
            throw new RuntimeException("Failed to call deserializeCaps", ex);
        }
    }

    // 实际存储-持久化保存和网络传输均以此为准
    private final Item item;
    private final @Nullable CompoundTag tag;
    private final @Nullable CompoundTag caps;

    // 缓存字段
    private transient ItemStack serverCache;
    private transient ItemStack clientCache;
    private transient int vanillaMaxSize = -1;

    // 识别字段-用于hashcode和equals，处理数字值为NaN时的比较并用于规避其他异常
    private transient byte[] signatureBytes; //包含tag+caps
    private transient int hashCache;
    private transient boolean hashReady;

    private ItemStackKey(Item item, @Nullable CompoundTag tag, @Nullable CompoundTag caps)
    {
        this.item = (item == null) ? Items.AIR : item;
        this.tag = (tag == null) ? null : tag.copy();
        this.caps = (caps == null) ? null : caps.copy();
    }

    public ItemStackKey(ItemStack stack)
    {
        this(stack.getItem(),
                stack.tag == null ? null : stack.tag.copy(),
                copyTagOrNull(serializeStackCaps(stack)));
    }


    // ---------------- IStackKey ----------------

    @Override
    public ResourceLocation getTypeId()
    {
        return ID;
    }

    @Override
    public @Nullable KeyAmount fromStackObject(Object stack)
    {
        if (stack instanceof ItemStack s)
        {
            return new KeyAmount(new ItemStackKey(s), s.getCount());
        }
        return null;
    }

    @Override
    public @Nullable IStackKey<ItemStack> fromSourceObject(Object key, CompoundTag dataComponentPatch)
    {
        if (key instanceof Item it)
        {
            // 先将item变为ItemStack，使其Caps自然显现，随后手动设置tag
            ItemStack itemStack = new ItemStack(it, 1);
            if (dataComponentPatch != null)
                itemStack.tag = dataComponentPatch.copy();
            return new ItemStackKey(itemStack);
        }
        return null;
    }

    @Override
    public ItemStack getReadOnlyStack()
    {
        if (this.serverCache == null)
        {
            this.serverCache = this.item == Items.AIR ? ItemStack.EMPTY : buildItemStack(this.item, this.tag, this.caps);
        }
        // item为空时，必须返回 EMPTY，且不要对 EMPTY 调用 setCount
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
            this.serverCache = buildItemStack(this.item, this.tag, this.caps);
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
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(item);
        return key != null ? key.getNamespace() : "unknown";
    }

    @Override
    public boolean isEmpty()
    {
        return this == EMPTY || this.item == Items.AIR;
    }

    @Override
    public IStackKey<ItemStack> getEmpty()
    {
        return EMPTY;
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
        if (this.item == Items.AIR) return ItemStack.EMPTY;
        return buildItemStack(this.item, this.tag, this.caps, BDMath.clampLongToInt(count));
    }

    @Override
    public long getVanillaMaxStackSize()
    {
        if (this.item == Items.AIR) return 1;
        if (vanillaMaxSize <= 0)
        {
            // 用一个临时 stack 计算原版最大堆叠
            ItemStack tmp = copyStackWithCount(1);
            vanillaMaxSize = tmp.getMaxStackSize();
        }
        return Math.min(vanillaMaxSize, getCustomMaxStackSize());
    }

    @Override
    public long getCustomMaxStackSize()
    {
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
        if (this == other) return true;
        if (other instanceof ItemStackKey o)
        {
            return this.item == o.item;
        }
        return false;
    }

    @Override
    public boolean isSameTypeSameComponents(IStackKey<?> other)
    {
        if (this == other) return true;
        if (!(other instanceof ItemStackKey o)) return false;
        if (this.item != o.item) return false;

        // 优先字节签名比较
        this.ensureSignatureBytes();
        o.ensureSignatureBytes();
        if (this.signatureBytes.length > 0 && o.signatureBytes.length > 0)
        {
            return Arrays.equals(this.signatureBytes, o.signatureBytes);
        }

        // 回退：relaxed NBT equals
        return NbtEq.equalsRelaxed(this.tag, o.tag) && NbtEq.equalsRelaxed(this.caps, o.caps);
    }

    @Override
    public void serialize(FriendlyByteBuf buf)
    {
        boolean hasItem = this.item != Items.AIR;
        buf.writeBoolean(hasItem);
        if (!hasItem) return;

        buf.writeId(BuiltInRegistries.ITEM, this.item);
        buf.writeNbt(this.tag);
        buf.writeNbt(this.caps);
    }

    @Override
    public @NotNull IStackKey<ItemStack> deserialize(FriendlyByteBuf buf)
    {
        boolean hasItem = buf.readBoolean();
        if (!hasItem) return EMPTY;

        Item it = buf.readById(BuiltInRegistries.ITEM);
        CompoundTag tag = buf.readNbt();
        CompoundTag cap = buf.readNbt();
        return new ItemStackKey(it, tag, cap);
    }

    @Override
    public @NotNull CompoundTag serializeNBT()
    {
        CompoundTag out = new CompoundTag();

        ResourceLocation id = ForgeRegistries.ITEMS.getKey(this.item);
        out.putString("item", id == null ? "minecraft:air" : id.toString());

        if (this.tag != null) out.put("tag", this.tag.copy());
        if (this.caps != null && !this.caps.isEmpty()) out.put("caps", this.caps.copy());

        return out;
    }

    @Override
    public @NotNull IStackKey<ItemStack> deserializeNBT(CompoundTag nbt)
    {
        if (nbt == null) return EMPTY;

        // 新格式：{ item, tag?, caps? }
        if (nbt.contains("item", Tag.TAG_STRING))
        {
            return readNewFmt(nbt);
        }

        // 旧 1.20.1 TypedStack 形状：{ Type, Amount, Stack:{id, tag, ForgeCaps} }
        if (nbt.contains("Stack", Tag.TAG_COMPOUND))
        {
            return fromLegacyTypedStack(nbt);
        }

        // 2) 其他未知形状：兜底为空
        return EMPTY;
    }

    private @NotNull IStackKey<ItemStack> fromLegacyTypedStack(@NotNull CompoundTag typed)
    {
        try
        {
            CompoundTag stackTag = typed.getCompound("Stack");

            // 旧字段：ForgeCaps
            CompoundTag caps = stackTag.contains("ForgeCaps", net.minecraft.nbt.Tag.TAG_COMPOUND)
                    ? stackTag.getCompound("ForgeCaps")
                    : null;

            // 标准字段：tag
            CompoundTag tag = stackTag.contains("tag", net.minecraft.nbt.Tag.TAG_COMPOUND)
                    ? stackTag.getCompound("tag")
                    : null;

            // 物品 id
            var id = net.minecraft.resources.ResourceLocation.tryParse(stackTag.getString("id"));
            Item raw = (id == null) ? Items.AIR : BuiltInRegistries.ITEM.get(id);

            Item it;
            try
            {
                it = ForgeRegistries.ITEMS.getDelegateOrThrow(raw).get();
            }
            catch (Throwable ignored)
            {
                it = raw;
            }

            return new ItemStackKey(it, tag, caps);
        }
        catch (Throwable t)
        {
            BeyondDimensions.LOGGER.warn("ItemStackKey: failed to decode legacy typed stack. Keys={}", typed.getAllKeys());
            return EMPTY;
        }
    }

    private @NotNull IStackKey<ItemStack> readNewFmt(@NotNull CompoundTag nbt)
    {
        var id = net.minecraft.resources.ResourceLocation.tryParse(nbt.getString("item"));
        Item raw = id == null ? Items.AIR : BuiltInRegistries.ITEM.get(id);

        Item it;
        try
        {
            it = ForgeRegistries.ITEMS.getDelegateOrThrow(raw).get();
        }
        catch (Throwable ignored)
        {
            it = raw;
        }

        CompoundTag tag = nbt.contains("tag", net.minecraft.nbt.Tag.TAG_COMPOUND) ? nbt.getCompound("tag") : null;
        CompoundTag cap = nbt.contains("caps", net.minecraft.nbt.Tag.TAG_COMPOUND) ? nbt.getCompound("caps") : null;

        return new ItemStackKey(it, tag, cap);
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
            this.clientCache = this.item == Items.AIR ? ItemStack.EMPTY : buildItemStack(this.item, this.tag, this.caps);
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
            this.clientCache = buildItemStack(this.item, this.tag, this.caps);
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
        if (other instanceof ItemStackKey o) return isSameTypeSameComponents(o);
        return false;
    }

    @Override
    public int hashCode()
    {
        if (!hashReady)
        {
            ensureSignatureBytes();
            int base = 31 + item.hashCode();
            int nbtPart = (signatureBytes.length > 0)
                    ? Arrays.hashCode(signatureBytes)
                    : (31 * NbtEq.hashRelaxed(tag) + NbtEq.hashRelaxed(caps));
            hashCache = 31 * base + nbtPart;
            hashReady = true;
        }
        return hashCache;
    }

    // ---------------- internals ----------------

    private static @Nullable CompoundTag copyTagOrNull(@Nullable CompoundTag in)
    {
        return in == null ? null : in.copy();
    }

    private static @NotNull ItemStack buildItemStack(Item item, @Nullable CompoundTag tag, @Nullable CompoundTag caps)
    {
        return buildItemStack(item, tag, caps, 1);
    }

    private static @NotNull ItemStack buildItemStack(Item item, @Nullable CompoundTag tag, @Nullable CompoundTag caps, int count)
    {
        ItemStack itemStack = new ItemStack(item, count, copyTagOrNull(caps));
        if (tag != null) itemStack.tag = tag.copy();
        return itemStack;
    }

    private void ensureSignatureBytes()
    {
        if (this.signatureBytes != null && this.signatureBytes.length > 0) return;
        byte[] out = NbtCanonicalBytesHelper.toCanonicalBytes(this.tag, this.caps);
        this.signatureBytes = (out != null) ? out : new byte[0];
    }
}