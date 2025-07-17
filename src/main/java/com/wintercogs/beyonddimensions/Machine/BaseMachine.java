package com.wintercogs.beyonddimensions.Machine;

public interface BaseMachine extends IMachine
{
    // working应当每tick调用一次
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

    // 子类继承必须与父类用 与 运算，除非你完全确定不需要父类效果
    @Override
    default boolean shouldWork()
    {
        // 检测前增加步进时间，确保检测值为当前tick
        setStepTick(getStepTick()+1); // 步进时间+1

        // 同时确保getTicksPerWork为0时可以每tick触发
        if(getStepTick() >= getTicksPerWork())
        {
            setStepTick(0); // 重置步进时间
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
        return false;
    }

    RedStoneControlMode getControlMode();

    boolean hasRedStoneSignal();

    int getTicksPerWork();

    int getStepTick();

    void setStepTick(int newTick);

    default void workStart(){}
    default void workContent(){}
    default void workEnd(){}
}
