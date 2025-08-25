package com.wintercogs.beyonddimensions.Api.Util;

import net.minecraft.nbt.*;

import javax.annotation.Nullable;

// 防止某些傻子把NaN塞进NBT中，导致哈希相等、NBT打印完全一致，但是equals时不相等
public final class NbtEq
{
    public static boolean equalsRelaxed(@Nullable Tag a, @Nullable Tag b)
    {
        if (a == b) return true;
        if (a == null || b == null) return false;
        if (a.getId() != b.getId()) return false;

        switch (a.getId()) {
            case Tag.TAG_COMPOUND -> {
                CompoundTag ca = (CompoundTag) a, cb = (CompoundTag) b;
                var ak = ca.getAllKeys();
                var bk = cb.getAllKeys();
                if (!ak.equals(bk)) return false; // 长度比较会在这里顺便做掉
                for (String k : ak) {
                    if (!equalsRelaxed(ca.get(k), cb.get(k))) return false;
                }
                return true;
            }
            case Tag.TAG_LIST -> {
                ListTag la = (ListTag) a, lb = (ListTag) b;
                if (la.size() != lb.size() || la.getElementType() != lb.getElementType()) return false;
                for (int i = 0; i < la.size(); i++) {
                    if (!equalsRelaxed(la.get(i), lb.get(i))) return false;
                }
                return true;
            }
            case Tag.TAG_DOUBLE -> {
                double x = ((DoubleTag) a).getAsDouble();
                double y = ((DoubleTag) b).getAsDouble();
                // 让 NaN == NaN；== 也让 -0.0 与 +0.0 视作相等
                return (x == y) || (Double.isNaN(x) && Double.isNaN(y));
            }
            case Tag.TAG_FLOAT -> {
                float x = ((FloatTag) a).getAsFloat();
                float y = ((FloatTag) b).getAsFloat();
                return (x == y) || (Float.isNaN(x) && Float.isNaN(y));
            }
            default -> {
                return a.equals(b); // 其它类型用原生实现（整数/数组/字符串…）
            }
        }
    }

    // 一个简单、顺序无关的合并：按 Map.hashCode 语义“求和每个 entry 的哈希”
    // 再做一次轻度 avalanche，避免低位聚集。
    public static int hashRelaxed(@Nullable Tag t) {
        if (t == null) return 0;
        return switch (t.getId()) {
            case Tag.TAG_COMPOUND -> {
                CompoundTag c = (CompoundTag) t;
                int sum = 0;
                for (String k : c.getAllKeys()) {
                    int kh = k.hashCode();
                    int vh = hashRelaxed(c.get(k));
                    // entry 哈希：与 Map.Entry.hashCode 类似
                    int eh = kh ^ vh;
                    sum += eh; // 顺序无关
                }
                yield avalanche32(sum);
            }
            case Tag.TAG_LIST -> {
                ListTag l = (ListTag) t;
                int h = 1;
                h = 31 * h + l.getElementType();
                for (Tag e : l) h = 31 * h + hashRelaxed(e); // 保持“有序列表”哈希
                yield h;
            }
            case Tag.TAG_DOUBLE -> {
                double v = ((DoubleTag) t).getAsDouble();
                if (Double.isNaN(v)) v = Double.NaN; // 统一 NaN
                if (v == 0.0d) v = 0.0d;            // 折叠 -0.0d
                long bits = Double.doubleToRawLongBits(v);
                yield (int) (bits ^ (bits >>> 32));
            }
            case Tag.TAG_FLOAT -> {
                float v = ((FloatTag) t).getAsFloat();
                if (Float.isNaN(v)) v = Float.NaN;
                if (v == 0.0f) v = 0.0f;            // 折叠 -0.0f
                yield Float.floatToRawIntBits(v);
            }
            default -> t.hashCode(); // 其它类型使用原生哈希
        };
    }

    private static int avalanche32(int x) {
        x ^= (x >>> 16);
        x *= 0x7feb352d;
        x ^= (x >>> 15);
        x *= 0x846ca68b;
        x ^= (x >>> 16);
        return x;
    }
}
