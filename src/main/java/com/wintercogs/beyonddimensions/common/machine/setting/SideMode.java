package com.wintercogs.beyonddimensions.common.machine.setting;

/**
 * 机器一个面的输入输出：相邻方块能否从这一面放入、取出。主动弹出只送往能取出的面，网络泵只从能放入的面抽取。
 * 禁用的面既不能放入也不能取出，不向相邻方块提供能力
 */
public enum SideMode
{
    INPUT(true, false),
    OUTPUT(false, true),
    BOTH(true, true),
    NONE(false, false);

    private final boolean input;
    private final boolean output;

    SideMode(boolean input, boolean output)
    {
        this.input = input;
        this.output = output;
    }

    /**
     * 相邻方块能否从这一面放入，网络泵能否从这一面抽取
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
