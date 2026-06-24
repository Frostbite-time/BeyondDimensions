package com.wintercogs.beyonddimensions.integration.module.botania;

import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.ItemCapability;
import vazkii.botania.api.block.WandHUD;
import vazkii.botania.api.block.Wandable;
import vazkii.botania.api.mana.ManaItem;
import vazkii.botania.api.mana.ManaReceiver;
import vazkii.botania.api.mana.spark.SparkAttachable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Keeps Botania snapshot API drift out of the integration code.
 */
public final class BotaniaCompat
{
    private static final String API = "vazkii.botania.api.capability.";
    private static final String FORGE_CAPS = "vazkii.botania.api.BotaniaForgeCapabilities";
    private static final String FORGE_CLIENT_CAPS = "vazkii.botania.api.BotaniaForgeClientCapabilities";
    private static final String XPLAT = "vazkii.botania.xplat.XplatAbstractions";
    private static final String BOTANIA_ITEMS = "vazkii.botania.common.item.BotaniaItems";
    private static final String BOTANIA_BLOCKS = "vazkii.botania.common.block.BotaniaBlocks";
    private static final String ITEM_SOURCE = "vazkii.botania.api.internal.ItemSource";
    private static final String ITEM_SOURCES = "vazkii.botania.common.internal_caps.ItemSources";
    private static final String MANA_INFUSION_SPAWNED_TAG = "beyonddimensions:mana_infusion_spawned";

    private static final ResourceLocation MANA_MIRROR = ResourceLocation.fromNamespaceAndPath("botania", "mana_mirror");
    private static final ResourceLocation MANA_TABLET = ResourceLocation.fromNamespaceAndPath("botania", "mana_tablet");
    private static final ResourceLocation MANA_VOID = ResourceLocation.fromNamespaceAndPath("botania", "mana_void");
    private static final ResourceLocation LIVINGROCK = ResourceLocation.fromNamespaceAndPath("botania", "livingrock");

    private BotaniaCompat() {}

    @SuppressWarnings("unchecked")
    public static BlockCapability<ManaReceiver, Direction> manaReceiver()
    {
        return (BlockCapability<ManaReceiver, Direction>) resolveBlockCapability(
                "vazkii.botania.api.mana.ManaReceiver", "LOOKUP", FORGE_CAPS, "MANA_RECEIVER");
    }

    @SuppressWarnings("unchecked")
    public static ItemCapability<ManaItem, Void> manaItem()
    {
        return (ItemCapability<ManaItem, Void>) resolveItemCapability(
                "vazkii.botania.api.mana.ManaItem", "LOOKUP", FORGE_CAPS, "MANA_ITEM");
    }

    @SuppressWarnings("unchecked")
    public static BlockCapability<SparkAttachable, Void> sparkAttachable()
    {
        return (BlockCapability<SparkAttachable, Void>) resolveBlockCapability(
                "vazkii.botania.api.mana.spark.SparkAttachable", "LOOKUP", FORGE_CAPS, "SPARK_ATTACHABLE");
    }

    @SuppressWarnings("unchecked")
    public static BlockCapability<Wandable, Direction> wandable()
    {
        return (BlockCapability<Wandable, Direction>) resolveBlockCapability(
                "vazkii.botania.api.block.Wandable", "LOOKUP", FORGE_CAPS, "WANDABLE");
    }

    @SuppressWarnings("unchecked")
    public static BlockCapability<WandHUD, Void> blockWandHud()
    {
        return (BlockCapability<WandHUD, Void>) resolveBlockCapability(
                "vazkii.botania.api.block.WandHUD", "BLOCK_LOOKUP", FORGE_CLIENT_CAPS, "BLOCK_WAND_HUD");
    }

    public static Item manaMirror()
    {
        return item(MANA_MIRROR, "manaMirror", "MANA_MIRROR");
    }

    public static Item manaTablet()
    {
        return item(MANA_TABLET, "manaTablet", "MANA_TABLET");
    }

    public static Block manaVoid()
    {
        return block(MANA_VOID, "manaVoid", "MANA_VOID");
    }

    public static Block livingrock()
    {
        return block(LIVINGROCK, "livingrock", "LIVINGROCK");
    }

    public static ManaItem findManaItem(ItemStack stack)
    {
        Object xplat = xplat();
        try
        {
            Method findManaItem = Class.forName(XPLAT).getMethod("findManaItem", ItemStack.class);
            return (ManaItem) findManaItem.invoke(xplat, stack);
        }
        catch (ReflectiveOperationException | LinkageError ignored)
        {
            // Botania 454 removed findManaItem in favor of findItemApi(ManaItem.LOOKUP, stack).
        }

        try
        {
            Object lookup = field("vazkii.botania.api.mana.ManaItem", "LOOKUP");
            Method findItemApi = Class.forName(XPLAT).getMethod("findItemApi", Class.forName(API + "ItemApiNoContext"), ItemStack.class);
            return (ManaItem) findItemApi.invoke(xplat, lookup, stack);
        }
        catch (ReflectiveOperationException | LinkageError e)
        {
            throw new IllegalStateException("Beyond Dimensions: unable to resolve Botania mana item API", e);
        }
    }

    public static boolean isManaInfusionSpawned(ItemEntity item)
    {
        if (item.getPersistentData().getBoolean(MANA_INFUSION_SPAWNED_TAG))
        {
            return true;
        }

        try
        {
            Object source = itemSource(item);
            return source == field(ITEM_SOURCES, "MANA_INFUSION");
        }
        catch (ReflectiveOperationException | LinkageError ignored)
        {
            // Botania 454+ uses ItemSource attachments; older versions used item flags.
        }

        try
        {
            Object flags = itemFlagsComponent(item);
            return flags.getClass().getField("manaInfusionSpawned").getBoolean(flags);
        }
        catch (ReflectiveOperationException | LinkageError ignored)
        {
            return false;
        }
    }

    public static void setManaInfusionSpawned(ItemEntity item)
    {
        item.getPersistentData().putBoolean(MANA_INFUSION_SPAWNED_TAG, true);

        try
        {
            setItemSource(item, field(ITEM_SOURCES, "MANA_INFUSION"));
        }
        catch (ReflectiveOperationException | LinkageError ignored)
        {
            // Older Botania versions do not expose the ItemSource attachment.
        }

        try
        {
            Object flags = itemFlagsComponent(item);
            flags.getClass().getField("manaInfusionSpawned").setBoolean(flags, true);
        }
        catch (ReflectiveOperationException | LinkageError ignored)
        {
            // Newer Botania versions do not expose this legacy component.
        }
    }

    private static BlockCapability<?, ?> resolveBlockCapability(String newHolder, String newField, String oldHolder, String oldField)
    {
        try
        {
            Object lookup = field(newHolder, newField);
            Class<?> caps = Class.forName(FORGE_CAPS);
            Class<?> withCtx = Class.forName(API + "BlockApiWithContext");
            Class<?> param = withCtx.isInstance(lookup) ? withCtx : Class.forName(API + "BlockApiNoContext");
            Method getById = caps.getMethod("getBlockApiLookupById", param);
            BlockCapability<?, ?> capability = (BlockCapability<?, ?>) getById.invoke(null, lookup);
            if (capability != null)
            {
                return capability;
            }
        }
        catch (ReflectiveOperationException | LinkageError ignored)
        {
            // Fall back to the pre-454 fields.
        }

        try
        {
            return (BlockCapability<?, ?>) field(oldHolder, oldField);
        }
        catch (ReflectiveOperationException | LinkageError e)
        {
            throw new IllegalStateException("Beyond Dimensions: unable to resolve Botania block capability " + newHolder + "#" + newField + " / " + oldHolder + "#" + oldField, e);
        }
    }

    private static ItemCapability<?, ?> resolveItemCapability(String newHolder, String newField, String oldHolder, String oldField)
    {
        try
        {
            Object lookup = field(newHolder, newField);
            Class<?> caps = Class.forName(FORGE_CAPS);
            Class<?> withCtx = Class.forName(API + "ItemApiWithContext");
            Class<?> param = withCtx.isInstance(lookup) ? withCtx : Class.forName(API + "ItemApiNoContext");
            Method getById = caps.getMethod("getItemApiLookupById", param);
            ItemCapability<?, ?> capability = (ItemCapability<?, ?>) getById.invoke(null, lookup);
            if (capability != null)
            {
                return capability;
            }
        }
        catch (ReflectiveOperationException | LinkageError ignored)
        {
            // Fall back to the pre-454 fields.
        }

        try
        {
            return (ItemCapability<?, ?>) field(oldHolder, oldField);
        }
        catch (ReflectiveOperationException | LinkageError e)
        {
            throw new IllegalStateException("Beyond Dimensions: unable to resolve Botania item capability " + newHolder + "#" + newField + " / " + oldHolder + "#" + oldField, e);
        }
    }

    private static Item item(ResourceLocation id, String... fallbackFields)
    {
        return BuiltInRegistries.ITEM.getOptional(id)
                .orElseGet(() -> (Item) fallback(BOTANIA_ITEMS, id, fallbackFields));
    }

    private static Block block(ResourceLocation id, String... fallbackFields)
    {
        return BuiltInRegistries.BLOCK.getOptional(id)
                .orElseGet(() -> (Block) fallback(BOTANIA_BLOCKS, id, fallbackFields));
    }

    private static Object fallback(String className, ResourceLocation id, String... fieldNames)
    {
        for (String fieldName : fieldNames)
        {
            try
            {
                return field(className, fieldName);
            }
            catch (ReflectiveOperationException | LinkageError ignored)
            {
                // Try the next spelling.
            }
        }
        throw new IllegalStateException("Beyond Dimensions: unable to resolve Botania registry entry " + id);
    }

    private static Object xplat()
    {
        try
        {
            return field(XPLAT, "INSTANCE");
        }
        catch (ReflectiveOperationException | LinkageError ignored)
        {
            try
            {
                return Class.forName(XPLAT).getMethod("instance").invoke(null);
            }
            catch (ReflectiveOperationException | LinkageError e)
            {
                throw new IllegalStateException("Beyond Dimensions: unable to access Botania xplat bridge", e);
            }
        }
    }

    private static Object itemFlagsComponent(ItemEntity item) throws ReflectiveOperationException
    {
        return Class.forName(XPLAT).getMethod("itemFlagsComponent", ItemEntity.class).invoke(xplat(), item);
    }

    private static Object itemSource(ItemEntity item) throws ReflectiveOperationException
    {
        Object holder = field(ITEM_SOURCE, "HOLDER");
        return holder.getClass().getMethod("getOrDefault", Entity.class, Object.class).invoke(holder, item, null);
    }

    private static void setItemSource(ItemEntity item, Object source) throws ReflectiveOperationException
    {
        Object holder = field(ITEM_SOURCE, "HOLDER");
        holder.getClass().getMethod("setFor", Entity.class, Object.class).invoke(holder, item, source);
    }

    private static Object field(String className, String fieldName) throws ReflectiveOperationException
    {
        Field f = Class.forName(className).getField(fieldName);
        return f.get(null);
    }
}
