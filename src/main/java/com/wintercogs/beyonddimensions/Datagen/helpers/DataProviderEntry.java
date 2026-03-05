package com.wintercogs.beyonddimensions.Datagen.helpers;

import net.minecraft.data.DataProvider;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.function.Function;
import java.util.function.Predicate;

public record DataProviderEntry(Predicate<GatherDataEvent> condition, Function<GatherDataEvent, DataProvider> factory)
{
}
