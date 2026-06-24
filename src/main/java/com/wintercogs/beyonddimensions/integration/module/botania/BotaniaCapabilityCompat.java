package com.wintercogs.beyonddimensions.integration.module.botania;

import net.minecraft.core.Direction;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.ItemCapability;
import vazkii.botania.api.BotaniaForgeCapabilities;
import vazkii.botania.api.block.WandHUD;
import vazkii.botania.api.block.Wandable;
import vazkii.botania.api.mana.ManaItem;
import vazkii.botania.api.mana.ManaReceiver;
import vazkii.botania.api.mana.spark.SparkAttachable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * 兼容新旧两套 Botania 能力（capability）暴露方式。
 *
 * <p>Botania 454 快照重构了能力 API：原先直接挂在 {@code BotaniaForgeCapabilities} 上的
 * {@code MANA_RECEIVER}/{@code MANA_ITEM}/{@code SPARK_ATTACHABLE}/{@code WANDABLE}
 * 以及 {@code BotaniaForgeClientCapabilities.BLOCK_WAND_HUD} 等字段被移除，改为各接口上的
 * {@code LOOKUP}（如 {@code ManaReceiver.LOOKUP}），再经
 * {@code BotaniaForgeCapabilities.getBlockApiLookupById(...)} / {@code getItemApiLookupById(...)}
 * 转换为 NeoForge 的能力对象。
 *
 * <p>为同时兼容新旧 Botania（任一版本上都不抛 {@link NoSuchFieldError}），此处全部用反射在运行时
 * 探测：优先尝试新 API，失败再回退到旧字段。涉及的接口类型（{@link ManaReceiver} 等）本身在新旧版本
 * 都存在，因此可直接引用；只有“能力字段所在位置”随版本变化，故仅对位置做反射。
 */
public final class BotaniaCapabilityCompat
{
    private static final String API = "vazkii.botania.api.capability.";
    private static final String OLD = "vazkii.botania.api.BotaniaForgeCapabilities";
    private static final String OLD_CLIENT = "vazkii.botania.api.BotaniaForgeClientCapabilities";

    private BotaniaCapabilityCompat() {}

    @SuppressWarnings("unchecked")
    public static BlockCapability<ManaReceiver, Direction> manaReceiver()
    {
        return (BlockCapability<ManaReceiver, Direction>) resolveBlock(
                "vazkii.botania.api.mana.ManaReceiver", "LOOKUP", OLD, "MANA_RECEIVER");
    }

    @SuppressWarnings("unchecked")
    public static ItemCapability<ManaItem, Void> manaItem()
    {
        return (ItemCapability<ManaItem, Void>) resolveItem(
                "vazkii.botania.api.mana.ManaItem", "LOOKUP", OLD, "MANA_ITEM");
    }

    @SuppressWarnings("unchecked")
    public static BlockCapability<SparkAttachable, Void> sparkAttachable()
    {
        return (BlockCapability<SparkAttachable, Void>) resolveBlock(
                "vazkii.botania.api.mana.spark.SparkAttachable", "LOOKUP", OLD, "SPARK_ATTACHABLE");
    }

    @SuppressWarnings("unchecked")
    public static BlockCapability<Wandable, Direction> wandable()
    {
        return (BlockCapability<Wandable, Direction>) resolveBlock(
                "vazkii.botania.api.block.Wandable", "LOOKUP", OLD, "WANDABLE");
    }

    @SuppressWarnings("unchecked")
    public static BlockCapability<WandHUD, Void> blockWandHud()
    {
        return (BlockCapability<WandHUD, Void>) resolveBlock(
                "vazkii.botania.api.block.WandHUD", "BLOCK_LOOKUP", OLD_CLIENT, "BLOCK_WAND_HUD");
    }

    private static BlockCapability<?, ?> resolveBlock(String newHolder, String newField, String oldHolder, String oldField)
    {
        // 新 API：接口上的 LOOKUP -> getBlockApiLookupById(...)
        try
        {
            Object lookup = field(newHolder, newField);
            Class<?> withCtx = Class.forName(API + "BlockApiWithContext");
            Class<?> param = withCtx.isInstance(lookup) ? withCtx : Class.forName(API + "BlockApiNoContext");
            Method getById = BotaniaForgeCapabilities.class.getMethod("getBlockApiLookupById", param);
            return (BlockCapability<?, ?>) getById.invoke(null, lookup);
        }
        catch (ReflectiveOperationException ignored)
        {
            // 旧 Botania 上没有 LOOKUP / 转换方法，落到旧字段
        }

        // 旧 API：直接挂在 capabilities 类上的字段
        try
        {
            return (BlockCapability<?, ?>) field(oldHolder, oldField);
        }
        catch (ReflectiveOperationException e)
        {
            throw new IllegalStateException(
                    "Beyond Dimensions: 无法在当前 Botania 版本上解析方块能力 " + newField, e);
        }
    }

    private static ItemCapability<?, ?> resolveItem(String newHolder, String newField, String oldHolder, String oldField)
    {
        // 新 API：接口上的 LOOKUP -> getItemApiLookupById(...)
        try
        {
            Object lookup = field(newHolder, newField);
            Class<?> withCtx = Class.forName(API + "ItemApiWithContext");
            Class<?> param = withCtx.isInstance(lookup) ? withCtx : Class.forName(API + "ItemApiNoContext");
            Method getById = BotaniaForgeCapabilities.class.getMethod("getItemApiLookupById", param);
            return (ItemCapability<?, ?>) getById.invoke(null, lookup);
        }
        catch (ReflectiveOperationException ignored)
        {
            // 旧 Botania 上没有 LOOKUP / 转换方法，落到旧字段
        }

        // 旧 API：直接挂在 capabilities 类上的字段
        try
        {
            return (ItemCapability<?, ?>) field(oldHolder, oldField);
        }
        catch (ReflectiveOperationException e)
        {
            throw new IllegalStateException(
                    "Beyond Dimensions: 无法在当前 Botania 版本上解析物品能力 " + newField, e);
        }
    }

    private static Object field(String className, String fieldName) throws ReflectiveOperationException
    {
        Field f = Class.forName(className).getField(fieldName);
        return f.get(null);
    }
}
