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

public class EnergyStackTypedHandler implements EnergyHandler
{
    private final StackHandler handlerStorage;
    private final ArrayList<StackJournal> snapshotJournals = new ArrayList<>();

    public EnergyStackTypedHandler(StackHandler handlerStorage)
    {
        this.handlerStorage = handlerStorage;
    }

    /**
     * 查找或创建与 EnergyStackKey.INSTANCE 关联的 StackJournal。
     */
    private StackJournal getOrCreateJournal()
    {
        for (StackJournal journal : snapshotJournals)
        {
            if (journal.getKey().equals(EnergyStackKey.INSTANCE))
            {
                return journal;
            }
        }
        StackJournal journal = new StackJournal(EnergyStackKey.INSTANCE);
        snapshotJournals.add(journal);
        return journal;
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

        getOrCreateJournal().updateSnapshots(transaction);

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

        getOrCreateJournal().updateSnapshots(transaction);

        KeyAmount taken = handlerStorage.extract(EnergyStackKey.INSTANCE, amount, false, false);
        return BDMath.clampLongToInt(Math.max(0L, taken.amount()));
    }

    // ---- Per-key journal for transaction support ----

    private class StackJournal extends SnapshotJournal<KeyAmount>
    {
        private final EnergyStackKey key;

        StackJournal(EnergyStackKey key)
        {
            this.key = key;
        }

        public EnergyStackKey getKey()
        {
            return key;
        }

        @Override
        protected KeyAmount createSnapshot()
        {
            KeyAmount ka = handlerStorage.getStackByKey(key);
            return ka.isEmpty() ? new KeyAmount(key, 0L) : ka;
        }

        @Override
        protected void revertToSnapshot(KeyAmount snapshot)
        {
            if (snapshot == null) return;

            // 先提取所有当前能量，再恢复快照中的量
            KeyAmount current = handlerStorage.getStackByKey(key);
            if (!current.isEmpty())
            {
                handlerStorage.extract(key, current.amount(), false, false);
            }
            long restoreAmount = snapshot.isEmpty() ? 0L : snapshot.amount();
            if (restoreAmount > 0L)
            {
                handlerStorage.insert(key, restoreAmount, false);
            }
        }
    }
}