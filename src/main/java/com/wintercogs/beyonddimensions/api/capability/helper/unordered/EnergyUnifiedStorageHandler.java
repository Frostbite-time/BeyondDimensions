package com.wintercogs.beyonddimensions.api.capability.helper.unordered;

import com.wintercogs.beyonddimensions.api.dimensionnet.UnifiedStorage;
import com.wintercogs.beyonddimensions.api.storage.key.KeyAmount;
import com.wintercogs.beyonddimensions.api.storage.key.impl.EnergyStackKey;
import com.wintercogs.beyonddimensions.util.BDMath;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class EnergyUnifiedStorageHandler implements EnergyHandler
{
    private final UnifiedStorage storage;
    private final ArrayList<StackJournal> snapshotJournals = new ArrayList<>();

    public EnergyUnifiedStorageHandler(UnifiedStorage storage)
    {
        this.storage = storage;
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
        return Math.max(0L, storage.getStackByKey(EnergyStackKey.INSTANCE).amount());
    }

    @Override
    public long getCapacityAsLong()
    {
        return Math.max(0L, storage.getSlotCapacity(0));
    }

    @Override
    public int insert(int amount, @NotNull TransactionContext transaction)
    {
        TransferPreconditions.checkNonNegative(amount);
        if (amount == 0) return 0;

        KeyAmount simulatedLeft = storage.insert(EnergyStackKey.INSTANCE, amount, true);
        long simulatedInserted = amount - simulatedLeft.amount();
        if (simulatedInserted <= 0L) return 0;

        getOrCreateJournal().updateSnapshots(transaction);

        KeyAmount left = storage.insert(EnergyStackKey.INSTANCE, amount, false);
        long inserted = amount - left.amount();
        return BDMath.clampLongToInt(Math.max(0L, inserted));
    }

    @Override
    public int extract(int amount, @NotNull TransactionContext transaction)
    {
        TransferPreconditions.checkNonNegative(amount);
        if (amount == 0) return 0;

        KeyAmount simulated = storage.extract(EnergyStackKey.INSTANCE, amount, true, false);
        if (simulated.isEmpty() || simulated.amount() <= 0L) return 0;

        getOrCreateJournal().updateSnapshots(transaction);

        KeyAmount taken = storage.extract(EnergyStackKey.INSTANCE, amount, false, false);
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
            return storage.getStackByKey(key);
        }

        @Override
        protected void revertToSnapshot(KeyAmount snapshot)
        {
            if (snapshot == null) return;
            if (snapshot.isEmpty() || snapshot.key().isEmpty())
            {
                storage.setAmountByKey(key, 0L);
            }
            else
            {
                storage.setAmountByKey(snapshot.key(), snapshot.amount());
            }
        }
    }
}