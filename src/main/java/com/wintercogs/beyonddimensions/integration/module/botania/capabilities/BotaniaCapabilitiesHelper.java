package com.wintercogs.beyonddimensions.integration.module.botania.capabilities;

import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.EntityCapability;
import net.neoforged.neoforge.capabilities.ItemCapability;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;
import vazkii.botania.api.BotaniaForgeCapabilities;
import vazkii.botania.api.capability.*;

import java.util.IdentityHashMap;
import java.util.Map;

/***
 * 模仿植物模仿的能力构造器（我搞不懂他为什么要把自己的能力构造弄得乱七八糟且注册实际还贼tm晚，稍有不慎就能拿到null）
 */
public final class BotaniaCapabilitiesHelper
{
    private static final Map<ApiIdBlock<?>, BlockCapability<?, ?>> FOR_BLOCKS = new IdentityHashMap<>();
    private static final Map<ApiIdEntity<?>, EntityCapability<?, ?>> FOR_ENTITIES = new IdentityHashMap<>();
    private static final Map<ApiIdItem<?>, ItemCapability<?, ?>> FOR_ITEMS = new IdentityHashMap<>();

    public static <A> BlockCapability<A, @UnknownNullability Void> getBlockApiLookupById(BlockApiNoContext<A> id)
    {
        return getOrCreateBlock(id, Void.class, BotaniaForgeCapabilities.getBlockApiLookupById(id));
    }

    public static <A, C> BlockCapability<A, @UnknownNullability C> getBlockApiLookupById(BlockApiWithContext<A, C> id)
    {
        return getOrCreateBlock(id, id.getContextClass(), BotaniaForgeCapabilities.getBlockApiLookupById(id));
    }

    public static <A> EntityCapability<A, @UnknownNullability Void> getEntityApiLookupById(EntityApiNoContext<A> id)
    {
        return getOrCreateEntity(id, Void.class, BotaniaForgeCapabilities.getEntityApiLookupById(id));
    }

    public static <A, C> EntityCapability<A, @UnknownNullability C> getEntityApiLookupById(EntityApiWithContext<A, C> id)
    {
        return getOrCreateEntity(id, id.getContextClass(), BotaniaForgeCapabilities.getEntityApiLookupById(id));
    }

    public static <A> ItemCapability<A, @UnknownNullability Void> getItemApiLookupById(ItemApiNoContext<A> id)
    {
        return getOrCreateItem(id, Void.class, BotaniaForgeCapabilities.getItemApiLookupById(id));
    }

    public static <A, C> ItemCapability<A, @UnknownNullability C> getItemApiLookupById(ItemApiWithContext<A, C> id)
    {
        return getOrCreateItem(id, id.getContextClass(), BotaniaForgeCapabilities.getItemApiLookupById(id));
    }

    @SuppressWarnings("unchecked")
    private static synchronized <A, C> BlockCapability<A, @UnknownNullability C> getOrCreateBlock(ApiIdBlock<A> id, Class<C> contextClass,
                                                                                                  @Nullable BlockCapability<A, C> botaniaLookup)
    {
        BlockCapability<A, C> lookup = (BlockCapability<A, C>) FOR_BLOCKS.get(id);
        if (lookup == null)
        {
            lookup = botaniaLookup != null ? botaniaLookup : BlockCapability.create(id.getId(), id.getApiClass(), contextClass);
            FOR_BLOCKS.put(id, lookup);
        }
        return lookup;
    }

    @SuppressWarnings("unchecked")
    private static synchronized <A, C> EntityCapability<A, @UnknownNullability C> getOrCreateEntity(ApiIdEntity<A> id, Class<C> contextClass,
                                                                                                    @Nullable EntityCapability<A, C> botaniaLookup)
    {
        EntityCapability<A, C> lookup = (EntityCapability<A, C>) FOR_ENTITIES.get(id);
        if (lookup == null)
        {
            lookup = botaniaLookup != null ? botaniaLookup : EntityCapability.create(id.getId(), id.getApiClass(), contextClass);
            FOR_ENTITIES.put(id, lookup);
        }
        return lookup;
    }

    @SuppressWarnings("unchecked")
    private static synchronized <A, C> ItemCapability<A, @UnknownNullability C> getOrCreateItem(ApiIdItem<A> id, Class<C> contextClass,
                                                                                                @Nullable ItemCapability<A, C> botaniaLookup)
    {
        ItemCapability<A, C> lookup = (ItemCapability<A, C>) FOR_ITEMS.get(id);
        if (lookup == null)
        {
            lookup = botaniaLookup != null ? botaniaLookup : ItemCapability.create(id.getId(), id.getApiClass(), contextClass);
            FOR_ITEMS.put(id, lookup);
        }
        return lookup;
    }

    private BotaniaCapabilitiesHelper()
    {
    }
}
