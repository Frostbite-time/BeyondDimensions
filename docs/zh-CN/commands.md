# 命令与配置

[English](../en/commands.md) · [全部指南](../../README.zh-CN.md#指南)

## 命令

以下命令需要 2 级权限（管理员）。网络 ID 即网络名称后显示的编号，如 `#0003`。

| 命令 | 作用 |
| --- | --- |
| `/bdtools network create <player> [<slotCapacity> [<slotMaxSize>]]` | 为尚无网络的玩家创建网络，可存 `slotMaxSize` 种资源，每种最多 `slotCapacity` 个；省略时不限制 |
| `/bdtools network setOwner <netId> <player>` | 将玩家设为所有者，原所有者成为管理员 |
| `/bdtools network addManager <netId> <player>` | 添加管理员 |
| `/bdtools network removeManager <netId> <player>` | 将管理员降为成员 |
| `/bdtools network addPlayer <netId> <player>` | 添加成员 |
| `/bdtools network removePlayer <netId> <player>` | 移除成员 |
| `/bdtools network deleteNet <netId>` | 删除网络及其中的全部资源 |
| `/bdtools network deleteNetByPlayer <player>` | 删除玩家的主网络 |

客户端命令 `/bdtools searchCache clear` 用于清空搜索所用的提示文字缓存。

## 服务端配置

每个世界的配置保存在 `serverconfig/beyonddimensions-server.toml`。整合包可以通过 `defaultconfigs/beyonddimensions-server.toml` 设置新世界的默认值。

| 选项 | 默认值 | 作用 |
| --- | --- | --- |
| `interface.receive` | `true` | 允许网络接口将资源送入网络 |
| `interface.output` | `true` | 允许网络接口从网络取出标记的资源 |
| `interface.push_out` | `true` | 允许网络接口将内容推送到相邻容器 |
| `interface.blocked_input_types` | `[]` | 禁止网络接口送入网络的资源类型，如 `"beyonddimensions:stack_type/fluid"` |
| `space_time.fragment_transfer_time` | `3600` | 不稳定时空碎片转化为稳态时空碎片所需的秒数 |
| `space_time.crystal_generate_time` | `600` | 产出破碎的时空结晶的间隔秒数；`0` 表示不产出 |

存储界面的设置（如排序方式、界面大小）按玩家分别保存在 `config/beyonddimensions-client.toml`。
