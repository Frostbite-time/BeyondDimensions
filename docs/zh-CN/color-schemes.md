# 配色

[English](../en/color-schemes.md) · [全部指南](../../README.zh-CN.md#指南)

超越维度的界面颜色来自 [CompixelUI 配色](https://github.com/Frostbite-time/CompixelUI/blob/main/docs/zh-CN/themes.md)，命名空间为 `beyonddimensions`。玩家可以通过窗口上的调色板按钮切换配色或修改任意颜色。资源包和整合包可以用 JSON 文件添加或修改配色。文件格式见 CompixelUI 的说明，本页仅说明超越维度特有的内容。

## 配色

| 文件 | 配色 |
| --- | --- |
| `casing` | 超维机壳（默认） |
| `glass` | 玻璃（半透明） |
| `vanilla` | 原版 |
| `brass` | 黄铜 |

配色文件位于 `assets/beyonddimensions/compixel/schemes/`，`assets/beyonddimensions/compixel/schemes.json` 指定默认配色和排列顺序。资源包中放置同名文件即可修改对应配色，使用新文件名则添加新配色：

```json
{
  "format": 1,
  "name": "深海",
  "extends": "casing",
  "colors": {
    "window": "#16324A",
    "accent": "#4FD1C5"
  }
}
```

## 颜色名

当前版本附带的配色文件（模组 jar 中 `assets/beyonddimensions/compixel/schemes/` 下的 JSON）展示了各颜色名及内置配色的取值，可以复制其中一份作为起点。游戏内的编辑器列出全部颜色，也可以直接在编辑器中调整配色，再导出为资源包。制作半透明配色时，建议以 `glass.json` 为起点。
