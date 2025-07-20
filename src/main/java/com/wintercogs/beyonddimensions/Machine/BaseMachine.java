package com.wintercogs.beyonddimensions.Machine;

public interface BaseMachine extends IMachine
{


    @Override
    default void working()
    {
        if(shouldWork())
        {
            workStart();
            workContent();
            workEnd();
        }
    }

    @Override
    default boolean shouldWork()
    {
        RedStoneControlMode controlMode = getControlMode();
        if(controlMode == null)
            return true;
        switch(controlMode)
        {
            case IGNORE ->
            {
                return true;
            }
            case NOT_WORKING ->
            {
                return false;
            }
            case POWERED ->
            {
                return hasRedStoneSignal();
            }
            case UNPOWERED ->
            {
                return !hasRedStoneSignal();
            }
        }
        return false;
    }

    RedStoneControlMode getControlMode();

    boolean hasRedStoneSignal();

    default void workStart(){}
    default void workContent(){}
    default void workEnd(){}
}
