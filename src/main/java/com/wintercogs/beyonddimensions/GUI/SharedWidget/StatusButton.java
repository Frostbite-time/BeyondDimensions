package com.wintercogs.beyonddimensions.GUI.SharedWidget;

import com.wintercogs.beyonddimensions.DataBase.ButtonState;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Map;

public abstract class StatusButton extends IconButton
{
    protected ArrayList<ButtonState> states;
    protected Map<ButtonState,ResourceLocation> iconMap;
    public ButtonState currentState;


    protected StatusButton(int x, int y, int width, int height, Component name, OnPress onPress)
    {
        // 给予一个默认图片用于构造父类
        super(x, y, width, height, ResourceLocation.tryBuild("minecraft", "textures/misc/unknown_pack.png"), name, onPress);
        initButton();
        setIcon(iconMap.get(currentState));
    }

    // 用于子类初始化状态、状态图片映射表、当前状态
    protected abstract void initButton();

    // 用于快速切换状态的便捷方法
    public void toggleState()
    {
        // 确保状态列表不为空
        if (states == null || states.isEmpty()) {
            throw new IllegalStateException("Button states are not initialized.");
        }

        // 找到当前状态的索引
        int currentIndex = states.indexOf(currentState);

        // 如果当前状态不在列表中，抛出异常
        if (currentIndex == -1) {
            throw new IllegalStateException("Current state is not in the states list.");
        }

        // 计算下一个状态的索引（循环切换）
        int nextIndex = (currentIndex + 1) % states.size();

        // 切换到下一个状态
        currentState = states.get(nextIndex);

        // 更新按钮图标为当前状态对应的图标
        setIcon(iconMap.get(currentState));
    }

    // 用于手动设置当前状态
    public void setState(ButtonState state)
    {
        int currentIndex = states.indexOf(currentState);
        // 如果当前状态不在列表中，抛出异常
        if (currentIndex == -1) {
            return;
        }
        currentState = state;
        setIcon(iconMap.get(currentState));
    }
}
