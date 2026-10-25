package com.wintercogs.beyonddimensions.integration;

import net.minecraftforge.fml.ModList;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;

public final class ModPresence
{
    private ModPresence()
    {
    }

    public static boolean isLoaded(String modId)
    {
        return ModList.get().isLoaded(modId);
    }

    /**
     * 模组已加载且版本在 versionRange（Maven 写法）内；范围为空时只看是否加载
     */
    public static boolean isLoaded(String modId, String versionRange)
    {
        if (versionRange.isEmpty())
            return isLoaded(modId);
        VersionRange range;
        try
        {
            range = VersionRange.createFromVersionSpec(versionRange);
        }
        catch (InvalidVersionSpecificationException e)
        {
            throw new IllegalArgumentException("Invalid version range for " + modId + ": " + versionRange, e);
        }
        return ModList.get().getModContainerById(modId)
                .map(mod -> range.containsVersion(mod.getModInfo().getVersion()))
                .orElse(false);
    }
}
