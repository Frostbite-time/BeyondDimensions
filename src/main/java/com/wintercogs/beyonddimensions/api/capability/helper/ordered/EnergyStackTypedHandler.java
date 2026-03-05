package com.wintercogs.beyonddimensions.api.capability.helper.ordered;

import com.wintercogs.beyonddimensions.api.storage.handler.impl.StackHandler;
import com.wintercogs.beyonddimensions.api.storage.key.KeyAmount;
import com.wintercogs.beyonddimensions.api.storage.key.impl.EmptyStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.impl.EnergyStackKey;
import com.wintercogs.beyonddimensions.util.BDMath;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class EnergyStackTypedHandler extends SnapshotJournal<List<KeyAmount>> implements EnergyHandler
{
    private final StackHandler handlerStorage;

    public EnergyStackTypedHandler(StackHandler handlerStorage)
    {
        this.handlerStorage = handlerStorage;
    }

    @Override
    public long getAmountAsLong()
    {
        return handlerStorage.getBucket(EnergyStackKey.ID)
                .map(bucket -> {
                    long sum = 0L;
                    int n = bucket.size();
                    for (int i = 0; i < n; i++)
                    {
                        int slot = bucket.get(i);
                        long amt = handlerStorage.getStackBySlot(slot).amount();
                        if (amt <= 0L) continue;

                        long remain = Long.MAX_VALUE - sum;
                        if (amt >= remain)
                        {
                            return Long.MAX_VALUE;
                        }
                        sum += amt;
                    }
                    return sum;
                })
                .orElse(0L);
    }

    @Override
    public long getCapacityAsLong()
    {
        long sum = 0L;
        long perSlotByType = EnergyStackKey.INSTANCE.getVanillaMaxStackSize();

        var energyBucketOpt = handlerStorage.getBucket(EnergyStackKey.ID);
        if (energyBucketOpt.isPresent())
        {
            var bucket = energyBucketOpt.get();
            for (int i = 0; i < bucket.size(); i++)
            {
                int slot = bucket.get(i);
                long cap = Math.max(0L, Math.min(perSlotByType, handlerStorage.getSlotCapacity(slot)));
                long remain = Long.MAX_VALUE - sum;
                if (cap >= remain) return Long.MAX_VALUE;
                sum += cap;
            }
        }

        var emptyBucketOpt = handlerStorage.getBucket(EmptyStackKey.INSTANCE);
        if (emptyBucketOpt.isPresent())
        {
            var bucket = emptyBucketOpt.get();
            for (int i = 0; i < bucket.size(); i++)
            {
                int slot = bucket.get(i);
                long cap = Math.max(0L, Math.min(perSlotByType, handlerStorage.getSlotCapacity(slot)));
                long remain = Long.MAX_VALUE - sum;
                if (cap >= remain) return Long.MAX_VALUE;
                sum += cap;
            }
        }

        return sum;
    }

    @Override
    public int insert(int amount, @NotNull TransactionContext transaction)
    {
        TransferPreconditions.checkNonNegative(amount);
        if (amount == 0) return 0;

        KeyAmount simulatedLeft = handlerStorage.insert(EnergyStackKey.INSTANCE, amount, true);
        long simulatedInserted = amount - simulatedLeft.amount();
        if (simulatedInserted <= 0L) return 0;

        updateSnapshots(transaction);

        KeyAmount left = handlerStorage.insert(EnergyStackKey.INSTANCE, amount, false);
        long inserted = amount - left.amount();
        return BDMath.clampLongToInt(Math.max(0L, inserted));
    }

    @Override
    public int extract(int amount, @NotNull TransactionContext transaction)
    {
        TransferPreconditions.checkNonNegative(amount);
        if (amount == 0) return 0;

        KeyAmount simulated = handlerStorage.extract(EnergyStackKey.INSTANCE, amount, true, false);
        if (simulated.isEmpty() || simulated.amount() <= 0L) return 0;

        updateSnapshots(transaction);

        KeyAmount taken = handlerStorage.extract(EnergyStackKey.INSTANCE, amount, false, false);
        return BDMath.clampLongToInt(Math.max(0L, taken.amount()));
    }

    @Override
    protected List<KeyAmount> createSnapshot()
    {
        int total = handlerStorage.getSlots();
        ArrayList<KeyAmount> snapshot = new ArrayList<>(total);
        for (int i = 0; i < total; i++)
        {
            KeyAmount ka = handlerStorage.getStackBySlot(i);
            snapshot.add(new KeyAmount(ka.key(), ka.amount()));
        }
        return snapshot;
    }

    @Override
    protected void revertToSnapshot(List<KeyAmount> snapshot)
    {
        if (snapshot == null) return;

        int total = handlerStorage.getSlots();
        int restore = Math.min(total, snapshot.size());

        for (int i = 0; i < restore; i++)
        {
            KeyAmount ka = snapshot.get(i);
            handlerStorage.setStackDirectly(i, ka.key(), ka.amount());
        }
        for (int i = restore; i < total; i++)
        {
            handlerStorage.setStackDirectly(i, EmptyStackKey.INSTANCE, 0L);
        }
    }
}
