package com.wintercogs.beyonddimensions.util;

import net.minecraft.nbt.*;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 将 (tag,caps) 规范化为“稳定字节”的工具（1.20.1 / Forge ItemStackKey 用）。
 * <p>
 * 重要约定：
 * - EMPTY_BYTES：代表“tag 与 caps 都为空”的稳定字节（非空长度）。
 * - UNAVAILABLE_BYTES：代表“当前无法得到稳定字节”的哨兵（长度==0）。
 * <p>
 * 规范化规则：
 * - CompoundTag：按 key 字典序排序（消除写入顺序差异），子元素递归
 * - ListTag：保序（List 语义有序），子元素递归
 */
public final class NbtCanonicalBytesHelper
{

    /**
     * 空 (tag,caps) 的稳定字节（非空长度，避免与 UNAVAILABLE 混淆）
     */
    private static final byte[] EMPTY_BYTES = buildEmptyBytes();

    /**
     * 编码失败/不可用的哨兵（长度==0）
     */
    private static final byte[] UNAVAILABLE_BYTES = new byte[0];

    private NbtCanonicalBytesHelper()
    {
    }

    private static byte[] buildEmptyBytes()
    {
        // 用我们自己的编码写一个空 CompoundTag，保证稳定且非空
        try
        {
            CompoundTag empty = new CompoundTag();
            CompoundTag canon = canonicalizeCompound(empty);
            return encodeDeterministic(canon);
        }
        catch (Throwable t)
        {
            return new byte[]{0x0A, 0x00, 0x00, 0x00, 0x00};
        }
    }

    /**
     * 将 (tag,caps) 转为稳定字节。
     *
     * @return - 若两者都为空：返回 EMPTY_BYTES（非空长度）
     * - 若编码失败：返回 UNAVAILABLE_BYTES（长度==0）
     * - 否则：返回稳定字节（非空长度）
     */
    public static byte[] toCanonicalBytes(@Nullable CompoundTag tag, @Nullable CompoundTag caps)
    {
        boolean tagEmpty = (tag == null || tag.isEmpty());
        boolean capsEmpty = (caps == null || caps.isEmpty());
        if (tagEmpty && capsEmpty)
        {
            return EMPTY_BYTES;
        }

        try
        {
            CompoundTag root = new CompoundTag();
            if (!tagEmpty) root.put("tag", tag);
            if (!capsEmpty) root.put("caps", caps);

            Tag canon = canonicalize(root);
            return encodeDeterministic(canon);
        }
        catch (Throwable t)
        {
            return UNAVAILABLE_BYTES;
        }
    }

    /**
     * 递归规范化：CompoundTag 按 key 字典序、ListTag 保序，子元素递归
     */
    private static Tag canonicalize(Tag in)
    {
        if (in == null)
        {
            return EndTag.INSTANCE;
        }

        if (in instanceof CompoundTag ct)
        {
            return canonicalizeCompound(ct);
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

        // 其他 Tag 原样返回即可
        return in;
    }

    private static CompoundTag canonicalizeCompound(CompoundTag ct)
    {
        List<String> keys = new ArrayList<>(ct.getAllKeys());
        Collections.sort(keys);
        CompoundTag out = new CompoundTag();
        for (String k : keys)
        {
            Tag v = ct.get(k);
            out.put(k, canonicalize(v));
        }
        return out;
    }

    /**
     * 自定义编码：Tag -> bytes
     * 目标是稳定用于 equals/hash，不追求与原版 NBT 二进制完全一致。
     */
    private static byte[] encodeDeterministic(Tag root) throws Exception
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(256);
        DataOutputStream out = new DataOutputStream(baos);

        writeTag(out, root);

        out.flush();
        return baos.toByteArray();
    }

    /**
     * 写入一个 Tag： [typeId:byte] + payload
     */
    private static void writeTag(DataOutputStream out, Tag tag) throws Exception
    {
        if (tag == null)
        {
            out.writeByte(Tag.TAG_END);
            return;
        }

        byte type = tag.getId();
        out.writeByte(type);

        switch (type)
        {
            case Tag.TAG_END ->
            {
            }
            case Tag.TAG_BYTE -> out.writeByte(((ByteTag) tag).getAsByte());
            case Tag.TAG_SHORT -> out.writeShort(((ShortTag) tag).getAsShort());
            case Tag.TAG_INT -> out.writeInt(((IntTag) tag).getAsInt());
            case Tag.TAG_LONG -> out.writeLong(((LongTag) tag).getAsLong());
            case Tag.TAG_FLOAT -> out.writeInt(canonicalFloatBits(((FloatTag) tag).getAsFloat()));
            case Tag.TAG_DOUBLE -> out.writeLong(canonicalDoubleBits(((DoubleTag) tag).getAsDouble()));

            case Tag.TAG_BYTE_ARRAY ->
            {
                byte[] arr = ((ByteArrayTag) tag).getAsByteArray();
                out.writeInt(arr.length);
                out.write(arr);
            }
            case Tag.TAG_STRING -> writeUtf8(out, ((StringTag) tag).getAsString());
            case Tag.TAG_LIST -> writeList(out, (ListTag) tag);
            case Tag.TAG_COMPOUND -> writeCompound(out, (CompoundTag) tag);
            case Tag.TAG_INT_ARRAY ->
            {
                int[] arr = ((IntArrayTag) tag).getAsIntArray();
                out.writeInt(arr.length);
                for (int v : arr) out.writeInt(v);
            }
            case Tag.TAG_LONG_ARRAY ->
            {
                long[] arr = ((LongArrayTag) tag).getAsLongArray();
                out.writeInt(arr.length);
                for (long v : arr) out.writeLong(v);
            }
            default -> throw new IllegalStateException("Unknown NBT tag id: " + type);
        }
    }

    private static final int CANONICAL_FLOAT_NAN_BITS = 0x7fc00000;
    private static final long CANONICAL_DOUBLE_NAN_BITS = 0x7ff8000000000000L;

    private static int canonicalFloatBits(float v)
    {
        if (v == 0.0f) return 0; // +0.0
        if (Float.isNaN(v)) return CANONICAL_FLOAT_NAN_BITS;
        return Float.floatToRawIntBits(v);
    }

    private static long canonicalDoubleBits(double v)
    {
        if (v == 0.0d) return 0L; // +0.0
        if (Double.isNaN(v)) return CANONICAL_DOUBLE_NAN_BITS;
        return Double.doubleToRawLongBits(v);
    }

    private static void writeCompound(DataOutputStream out, CompoundTag ct) throws Exception
    {
        // 这里仍然用 getAllKeys 再排序一遍，保证即使调用方没canonicalize也稳定。
        List<String> keys = new ArrayList<>(ct.getAllKeys());
        Collections.sort(keys);

        out.writeInt(keys.size());
        for (String k : keys)
        {
            writeUtf8(out, k);
            writeTag(out, ct.get(k));
        }
    }

    private static void writeList(DataOutputStream out, ListTag lt) throws Exception
    {
        out.writeInt(lt.size());
        for (Tag tag : lt)
        {
            writeTag(out, tag);
        }
    }

    private static void writeUtf8(DataOutputStream out, String s) throws Exception
    {
        if (s == null) s = "";
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        out.writeInt(bytes.length);
        out.write(bytes);
    }

    // getter

    public static byte[] emptyBytes()
    {
        return EMPTY_BYTES;
    }

    public static byte[] unavailableBytes()
    {
        return UNAVAILABLE_BYTES;
    }
}
