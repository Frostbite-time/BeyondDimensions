package com.wintercogs.beyonddimensions.common.machine;

/**
 * 机器一个面的输入输出：相邻方块能否从这一面放入、取出，主动弹出只送往能取出的面
 */
public enum SideMode
{
    INPUT(true, false),
    OUTPUT(false, true),
    BOTH(true, true);

    private final boolean input;
    private final boolean output;

    SideMode(boolean input, boolean output)
    {
        this.input = input;
        this.output = output;
    }

    /**
     * 相邻方块能否从这一面放入
     */
    public boolean allowsInput()
    {
        return input;
    }

    /**
     * 相邻方块能否从这一面取出，主动弹出能否送往这一面
     */
    public boolean allowsOutput()
    {
        return output;
    }
}
