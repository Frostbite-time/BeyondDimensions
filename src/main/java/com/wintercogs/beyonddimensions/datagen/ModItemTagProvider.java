package com.wintercogs.beyonddimensions.datagen;

import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import com.wintercogs.beyonddimensions.integration.IIntegrationModule;
import com.wintercogs.beyonddimensions.integration.IntegrationManager;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.common.data.ItemTagsProvider;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public class ModItemTagProvider extends ItemTagsProvider
{
    public ModItemTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider)
    {
        super(output, lookupProvider, BDConstants.MODID);
    }

    @Override
    public @NotNull String getName()
    {
        return "BeyondDimensions ItemTag Provider";
    }

    @Override
    protected void addTags(HolderLookup.@NotNull Provider provider)
    {
        IntegrationManager.onItemTagDatagen(provider, new IIntegrationModule.ItemTagAppender()
        {
            @Override
            public void add(TagKey<Item> tag, ResourceKey<Item>... items)
            {
                ModItemTagProvider.this.tag(tag).add(items);
            }

            @Override
            public void addTag(TagKey<Item> tag, TagKey<Item> nestedTag)
            {
                ModItemTagProvider.this.tag(tag).addTag(nestedTag);
            }

            @Override
            public void addOptional(TagKey<Item> tag, ResourceKey<Item> item)
            {
                ModItemTagProvider.this.tag(tag).addOptional(item);
            }

            @Override
            public void addOptionalTag(TagKey<Item> tag, TagKey<Item> nestedTag)
            {
                ModItemTagProvider.this.tag(tag).addOptionalTag(nestedTag);
            }
        });
    }
}
