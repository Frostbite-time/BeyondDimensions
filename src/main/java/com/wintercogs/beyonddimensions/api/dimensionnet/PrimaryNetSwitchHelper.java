package com.wintercogs.beyonddimensions.api.dimensionnet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public final class PrimaryNetSwitchHelper
{
    private PrimaryNetSwitchHelper()
    {
    }

    public static List<Integer> sortNetIds(Collection<Integer> netIds)
    {
        List<Integer> sortedNetIds = new ArrayList<>(netIds);
        Collections.sort(sortedNetIds);
        return sortedNetIds;
    }

    public static int findNextPrimaryNetId(Collection<Integer> memberships, int currentPrimaryNetId)
    {
        List<Integer> sortedNetIds = sortNetIds(memberships);
        if (sortedNetIds.isEmpty())
        {
            return PlayerNetIndex.NO_PRIMARY_NET;
        }

        if (currentPrimaryNetId == PlayerNetIndex.NO_PRIMARY_NET)
        {
            return sortedNetIds.get(0);
        }

        int currentIndex = sortedNetIds.indexOf(currentPrimaryNetId);
        if (currentIndex < 0)
        {
            return sortedNetIds.get(0);
        }

        return sortedNetIds.get((currentIndex + 1) % sortedNetIds.size());
    }
}
