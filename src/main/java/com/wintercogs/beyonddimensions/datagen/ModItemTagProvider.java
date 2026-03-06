package com.wintercogs.beyonddimensions.datagen;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import com.wintercogs.beyonddimensions.api.ids.BDConstants;
import com.wintercogs.beyonddimensions.integration.IIntegrationModule;
import com.wintercogs.beyonddimensions.integration.IntegrationManager;
import com.wintercogs.beyonddimensions.integration.module.rs.tags.RSTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.concurrent.CompletableFuture;

public class ModItemTagProvider extends ItemTagsProvider
{
    public ModItemTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, CompletableFuture<TagLookup<Block>> blockTags, @Nullable ExistingFileHelper existingFileHelper)
    {
        super(output, lookupProvider, blockTags, BDConstants.MODID, existingFileHelper);
    }

    @Override
    public String getName()
    {
        return "BeyondDimensions ItemTag Provider";
    }

    @Override
    protected void addTags(HolderLookup.Provider provider)
    {
        // 添加RS富铁石英和机器框架的合成表支持
        tag(RSTags.RS_QUARTZ_ENRICHED_IRON)
                .addOptional(ResourceLocation.tryBuild(BeyondDimensions.RSModId, RSTags.QUARTZ_ENRICHED_IRON_NAME));
        tag(RSTags.RS_MACHINE_CASING)
                .addOptional(ResourceLocation.tryBuild(BeyondDimensions.RSModId, RSTags.MACHINE_CASING_NAME));

        IntegrationManager.onItemTagDatagen(provider, new IIntegrationModule.ItemTagAppender()
        {
            @Override
            public void add(TagKey<Item> tag, Item... items)
            {
                ModItemTagProvider.this.tag(tag).add(items);
            }

            @Override
            public void addTag(TagKey<Item> tag, TagKey<Item> nestedTag)
            {
                ModItemTagProvider.this.tag(tag).addTag(nestedTag);
            }

            @Override
            public void addOptional(TagKey<Item> tag, ResourceLocation itemId)
            {
                ModItemTagProvider.this.tag(tag).addOptional(itemId);
            }

            @Override
            public void addOptionalTag(TagKey<Item> tag, ResourceLocation nestedTagId)
            {
                ModItemTagProvider.this.tag(tag).addOptionalTag(nestedTagId);
            }
        });
    }
}
