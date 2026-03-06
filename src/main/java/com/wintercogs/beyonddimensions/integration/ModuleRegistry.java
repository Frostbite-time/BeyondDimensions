package com.wintercogs.beyonddimensions.integration;

import com.wintercogs.beyonddimensions.BeyondDimensions;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.forgespi.language.ModFileScanData;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Type;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ModuleRegistry
{
    private static volatile List<ModuleSpec> commonModules;
    private static volatile List<ModuleSpec> clientModules;

    private ModuleRegistry()
    {
    }

    public static List<ModuleSpec> commonModules()
    {
        ensureScanned();
        return commonModules;
    }

    public static List<ModuleSpec> clientModules()
    {
        ensureScanned();
        return clientModules;
    }

    private static void ensureScanned()
    {
        if (commonModules != null && clientModules != null)
        {
            return;
        }

        synchronized (ModuleRegistry.class)
        {
            if (commonModules != null && clientModules != null)
            {
                return;
            }

            Type commonAnnotationType = Type.getType(BDIntegrationModule.class);
            Type clientAnnotationType = Type.getType(BDIntegrationClientModule.class);

            List<ModuleSpec> discoveredCommonModules = new ArrayList<>();
            List<ModuleSpec> discoveredClientModules = new ArrayList<>();

            for (ModFileScanData scanData : ModList.get().getAllScanData())
            {
                for (ModFileScanData.AnnotationData annotationData : scanData.getAnnotations())
                {
                    if (commonAnnotationType.equals(annotationData.annotationType()))
                    {
                        ModuleSpec spec = parseModuleSpec(annotationData);
                        if (spec != null)
                        {
                            discoveredCommonModules.add(spec);
                        }
                        continue;
                    }

                    if (clientAnnotationType.equals(annotationData.annotationType()))
                    {
                        ModuleSpec spec = parseModuleSpec(annotationData);
                        if (spec != null)
                        {
                            discoveredClientModules.add(spec);
                        }
                    }
                }
            }

            commonModules = deduplicateByClassName(discoveredCommonModules);
            clientModules = deduplicateByClassName(discoveredClientModules);

            BeyondDimensions.LOGGER.info("Discovered {} common integration modules and {} client integration modules",
                    commonModules.size(), clientModules.size());
        }
    }

    private static @Nullable ModuleSpec parseModuleSpec(ModFileScanData.AnnotationData annotationData)
    {
        Object modIdData = annotationData.annotationData().get("modId");
        if (!(modIdData instanceof String modId) || modId.isBlank())
        {
            BeyondDimensions.LOGGER.warn("Ignoring integration module {} because modId is missing", annotationData.clazz().getClassName());
            return null;
        }

        return new ModuleSpec(modId, annotationData.clazz().getClassName());
    }

    private static List<ModuleSpec> deduplicateByClassName(List<ModuleSpec> modules)
    {
        Map<String, ModuleSpec> dedup = new LinkedHashMap<>();
        for (ModuleSpec module : modules)
        {
            dedup.putIfAbsent(module.implClassName(), module);
        }
        return List.copyOf(dedup.values());
    }
}
