package com.wintercogs.beyonddimensions.DataComponents.Custom;

import com.mojang.serialization.Codec;
import com.wintercogs.beyonddimensions.Unit.CodecHelper;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

public record ItemStackContents(NonNullList<ItemStack> contents)
{
    public static final Codec<ItemStackContents> CODEC = CodecHelper.nonNullListMutableCodecOf(ItemStack.OPTIONAL_CODEC, ItemStack.EMPTY)
            .xmap(ItemStackContents::new, ItemStackContents::contents);

    public static final StreamCodec<RegistryFriendlyByteBuf, ItemStackContents> STREAM_CODEC =
            ByteBufCodecs.collection(
                    NonNullList::createWithCapacity,
                    ItemStack.OPTIONAL_STREAM_CODEC
            ).map(ItemStackContents::new, ItemStackContents::contents);

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return ItemStack.listMatches(this.contents, ((ItemStackContents) o).contents);
    }

    @Override
    public int hashCode()
    {
        return ItemStack.hashStackList(contents);
    }
}
