package com.wintercogs.beyonddimensions.Api.DataBase.Storage;

import com.wintercogs.beyonddimensions.Api.DataBase.Stack.EnergyStackKey;
import com.wintercogs.beyonddimensions.Api.DataBase.Stack.KeyAmount;
import com.wintercogs.beyonddimensions.Util.BDMath;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class EnergyUnifiedStorageHandler extends SnapshotJournal<List<KeyAmount>> implements EnergyHandler
{
    private final UnifiedStorage storage;

    public EnergyUnifiedStorageHandler(UnifiedStorage storage)
    {
        this.storage = storage;
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

        updateSnapshots(transaction);

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

        updateSnapshots(transaction);

        KeyAmount taken = storage.extract(EnergyStackKey.INSTANCE, amount, false, false);
        return BDMath.clampLongToInt(Math.max(0L, taken.amount()));
    }

    @Override
    protected List<KeyAmount> createSnapshot()
    {
        List<KeyAmount> view = storage.getStorage();
        ArrayList<KeyAmount> snapshot = new ArrayList<>(view.size());
        for (int i = 0; i < view.size(); i++)
        {
            KeyAmount ka = view.get(i);
            snapshot.add(new KeyAmount(ka.key(), ka.amount()));
        }
        return snapshot;
    }

    @Override
    protected void revertToSnapshot(List<KeyAmount> snapshot)
    {
        if (snapshot == null) return;

        storage.clearStorage();
        for (int i = 0; i < snapshot.size(); i++)
        {
            KeyAmount ka = snapshot.get(i);
            if (!ka.isEmpty())
            {
                storage.setAmountByKey(ka.key(), ka.amount());
            }
        }
    }
}
