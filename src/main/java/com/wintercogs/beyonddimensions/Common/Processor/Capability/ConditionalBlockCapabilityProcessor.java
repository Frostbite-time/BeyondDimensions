package com.wintercogs.beyonddimensions.Common.Processor.Capability;

import com.wintercogs.beyonddimensions.Common.InterfaceHelper.BlockCapabilityFunction;
import com.wintercogs.beyonddimensions.Common.InterfaceHelper.InsertAction;
import com.wintercogs.beyonddimensions.Common.InterfaceHelper.SlotsCount;
import com.wintercogs.beyonddimensions.DataBase.Stack.IStackType;

import java.util.function.BooleanSupplier;
import java.util.function.Predicate;

public class ConditionalBlockCapabilityProcessor<T> extends SimpleBlockCapabilityProcessor<T>
{

    private final BooleanSupplier availabilityCheck;

    public ConditionalBlockCapabilityProcessor(
            BooleanSupplier availabilityCheck,
            T capabilityHandler,
            Predicate<IStackType> stackChecker,
            BlockCapabilityFunction<T> capabilityGetter,
            InsertAction<T> insertAction,
            SlotsCount<T> slotsCounter
    )
    {
        super(
                capabilityHandler,
                stackChecker,
                capabilityGetter,
                insertAction,
                slotsCounter
        );
        this.availabilityCheck = availabilityCheck;
    }

    @Override
    public boolean isAvailable()
    {
        return availabilityCheck.getAsBoolean();
    }
}
