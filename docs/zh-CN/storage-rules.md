# 存储规则

[English](../en/storage-rules.md) · [全部指南](../../README.zh-CN.md#指南)

存储规则为资源进出网络添加额外处理：拒收、转换、收费，或保留一定数量不允许取出。界面、管道等所有存取操作都会经过规则。

## 应用规则

存入和取出各有一张规则表。资源每次进出，都按表中顺序逐条判断：第一条给出计划的规则决定处理方式；返回 `null` 表示不处理，继续判断下一条。没有规则给出计划时，按常规存取。

规则接收的请求提供以下方法：

| 方法 | 返回值 |
| --- | --- |
| `key()` | 本次处理的资源 |
| `amount()` | 请求的数量 |
| `net()` | 所在的网络 |
| `stored(key)` | 网络中 `key` 的数量，包含本次操作尚未提交的变化 |

## 存入计划

| 计划 | 效果 |
| --- | --- |
| `InsertPlan.reject()` | 拒收 |
| `InsertPlan.store()` | 按原样存入，不再询问后续规则 |
| `InsertPlan.discard()` | 接收，但不存入任何资源 |
| `InsertPlan.convert(unit)` | 以 `unit` 个为一份接收，改为存入计划中 `add` 的资源 |

以下方法按每一份追加变化：

| 方法 | 效果 |
| --- | --- |
| `add(key, amount)` | 额外存入 `amount` 个 `key` |
| `take(key, amount)` | 从网络扣除 `amount` 个 `key`；无法扣足的份数不予接收 |
| `limit(max)` | 请求的资源最多接收 `max` 个 |
| `onCommit((request, amount) -> ...)` | 操作提交后执行，`amount` 为实际接收的数量；模拟时不执行 |

只接收完整的份数，其余部分退回给存入方。

## 取出计划

| 计划或方法 | 效果 |
| --- | --- |
| `ExtractPlan.reject()` | 不交出 |
| `ExtractPlan.allow()` | 按原样交出 |
| `take(key, amount)` | 每交出一个，额外扣除 `amount` 个 `key`；不足时减少交出的数量 |
| `limit(max)` | 最多交出 `max` 个 |
| `onCommit((request, amount) -> ...)` | 操作提交后执行，`amount` 为实际交出的数量 |

交出的始终是请求的那种资源。

## 注册规则

| 方法 | 作用范围 |
| --- | --- |
| `UnifiedStorageRules.insert(id, key, rule)` | 单一资源的存入 |
| `UnifiedStorageRules.insertMatching(id, matcher, rule)` | `matcher` 匹配的所有资源的存入 |
| `UnifiedStorageRules.extract(id, key, rule)` | 单一资源的取出 |
| `UnifiedStorageRules.extractMatching(id, matcher, rule)` | `matcher` 匹配的所有资源的取出 |
| `removeInsert(id)`、`removeExtract(id)` | 移除规则 |
| `insertIds()`、`extractIds()` | 按表中顺序列出规则 ID |

新规则默认追加到表尾，可通过第四个参数指定位置：`UnifiedStorageRules.first()`、`last()`、`before(id)` 或 `after(id)`。同一张表中每个 ID 只能注册一次。

在模组初始化阶段注册规则，例如在 `FMLCommonSetupEvent` 中：

```java
// 存入水桶时拆分为空桶和 1000 mB 水
UnifiedStorageRules.insert(new ResourceLocation("examplemod", "unpack_water_bucket"),
        new ItemStackKey(new ItemStack(Items.WATER_BUCKET)),
        request -> InsertPlan.convert(1)
                .add(new ItemStackKey(new ItemStack(Items.BUCKET)), 1)
                .add(new FluidStackKey(new FluidStack(Fluids.WATER, 1)), 1000));

// 网络中至少保留 1000 个圆石，不允许取出
UnifiedStorageRules.extract(new ResourceLocation("examplemod", "keep_cobblestone"),
        new ItemStackKey(new ItemStack(Items.COBBLESTONE)),
        request -> ExtractPlan.allow().limit(request.stored(request.key()) - 1000));

// 拒收机械动力的所有资源
UnifiedStorageRules.insertMatching(new ResourceLocation("examplemod", "no_create"),
        key -> key.getModId().equals("create"),
        request -> InsertPlan.reject());
```

## 编写规则的注意事项

- 规则只能读取状态，不能修改任何存储。模拟和实际执行都会调用规则，相同状态下必须返回相同的计划。对存储的修改写在计划中，其他副作用放在 `onCommit` 中。
- 计划中存入或扣除的资源同样会经过规则；同一条转换链上每条规则最多生效一次，因此不会形成循环。
- `matcher` 在每次存取时都会调用，应保持开销较低。针对单一资源的规则按资源直接查找，无需逐条判断。
- 规则抛出异常时，本次存取不执行，错误写入日志，每条规则只记录一次。
- 超越维度自身注册了一条存入规则 `beyonddimensions:uncompress_matter_ball`，用于拆开压缩物质球。如需替代它，将自己的规则注册在它之前。
