package com.wintercogs.beyonddimensions.util;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 将 DataComponentPatch 规范化为“稳定字节”的工具。
 * <p>
 * 重要约定：
 * - EMPTY_BYTES：代表“补丁确实为空”的稳定字节（非空长度）。
 * - UNAVAILABLE_BYTES：代表“当前无法得到稳定字节”（Provider 未就绪/编码失败）的哨兵（长度==0）。
 * <p>
 * 注意：调用方（如 ItemStackType）只在 equals/hash 中当 equalsByte.length>0 时才走“字节相等”，
 * 否则回退到 patch.equals/hashCode，从而避免“失败被误当作空补丁”的错误合并。
 */
public class DataComponentPatchHelper
{

    /**
     * “空 CompoundTag”的稳定字节（用于补丁为空时）
     */
    private static final byte[] EMPTY_BYTES = buildEmptyBytes();

    /**
     * “当前不可用/失败”的哨兵字节（长度==0）
     */
    private static final byte[] UNAVAILABLE_BYTES = new byte[0];

    private static byte[] buildEmptyBytes()
    {
        ByteBuf buf = Unpooled.buffer(16);
        try
        {
            FriendlyByteBuf.writeNbt(buf, new CompoundTag());
            byte[] out = new byte[buf.readableBytes()];
            buf.readBytes(out);
            return out;
        }
        finally
        {
            buf.release();
        }
    }

    /**
     * Patch -> NBT（带注册表上下文）-> 递归规范化 -> 非压缩字节
     * 产出“稳定、可用于 equals/hash 的字节”。
     *
     * @return - 若 patch 为空：返回 {@link #EMPTY_BYTES}（非空长度）
     * - 若 Provider 未就绪/编码失败：返回 {@link #UNAVAILABLE_BYTES}（长度==0）
     * - 否则：返回规范化后的稳定字节（非空长度）
     */
    public static byte[] toCanonicalBytes(DataComponentPatch patch, HolderLookup.Provider registries)
    {
        if (patch == null || patch.isEmpty())
        {
            return EMPTY_BYTES;
        }

        // 1) patch -> NBT（RegistryOps<Tag>）
        Tag root = DataComponentPatch.CODEC.encodeStart(
                registries.createSerializationContext(NbtOps.INSTANCE),
                patch
        ).result().orElse(null);

        // Provider 未就绪或编码失败：返回“不可用哨兵”
        if (root == null)
        {
            return UNAVAILABLE_BYTES;
        }

        // 2) 递归规范化
        Tag canon = canonicalize(root);

        // 3) 写成“网络 NBT”字节，并显式释放 ByteBuf
        ByteBuf raw = null;
        try
        {
            raw = Unpooled.buffer(Math.max(64, 8 * sizeHint(canon)));
            FriendlyByteBuf.writeNbt(raw, canon instanceof CompoundTag ct ? ct : wrap(canon));
            byte[] out = new byte[raw.readableBytes()];
            raw.readBytes(out);
            return out;
        }
        catch (Throwable t)
        {
            // 任意异常：以“不可用哨兵”回退，让上层走 patch.equals/hashCode
            return UNAVAILABLE_BYTES;
        }
        finally
        {
            if (raw != null)
            {
                raw.release();
            }
        }
    }

    /**
     * 递归规范化：CompoundTag 按 key 字典序、ListTag 保序，子元素递归
     */
    private static Tag canonicalize(Tag in)
    {
        if (in instanceof CompoundTag ct)
        {
            List<String> keys = new ArrayList<>(ct.keySet());
            Collections.sort(keys); // 字典序，消除写入顺序差异
            CompoundTag out = new CompoundTag();
            for (String k : keys)
            {
                out.put(k, canonicalize(ct.get(k)));
            }
            return out;
        }
        if (in instanceof ListTag lt)
        {
            ListTag out = new ListTag();
            for (Tag elem : lt)
            {
                out.add(canonicalize(elem)); // 保留顺序，仅对子元素递归
            }
            return out;
        }
        return in; // 其他原样返回
    }

    /**
     * 若顶层不是 CompoundTag，则包一层，便于 writeNbt
     */
    private static CompoundTag wrap(Tag t)
    {
        CompoundTag wrap = new CompoundTag();
        wrap.put("value", t);
        return wrap;
    }

    /**
     * 粗略容量估计，减少 ByteBuf 扩容次数（无需精确）
     */
    private static int sizeHint(Tag t)
    {
        if (t instanceof CompoundTag ct) return Math.max(1, ct.size() * 4);
        if (t instanceof ListTag lt) return Math.max(1, lt.size() * 4);
        return 4;
    }
}

