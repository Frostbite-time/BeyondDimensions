<div align="center">

<img src="docs/images/blocks.png" alt="草地上的超越维度方块" width="100%">

# 超越维度 Beyond Dimensions

一个 Minecraft 存储模组。存储一切，随处访问。

[![CurseForge 下载量](https://img.shields.io/curseforge/dt/1222890?logo=curseforge&label=CurseForge&color=F16436)](https://www.curseforge.com/projects/1222890)
[![QQ群：Frostbite的被窝OWO](https://img.shields.io/badge/QQ%E7%BE%A4-%E5%8A%A0%E5%85%A5-12B7F5?logo=qq&logoColor=white)](https://qun.qq.com/universal-share/share?ac=1&authKey=Ibpg3bzCDcd7hop8wWByfXSrLX%2FbWxey4nSK6IVaaJwTAEmmna69U%2B9RDKNpFYYA&busi_data=eyJncm91cENvZGUiOiIxMDQxNzk0OTExIiwidG9rZW4iOiJoVlIrUUdWN2prbnBTWnl5ZVhyOHkvYVZnc1hrbFY3WVN1TlAxaHJBd3NrWDNZcElZY1RkeTcxa3ZRU3F2QnNTIiwidWluIjoiMjIzOTgyODUyOCJ9&data=aCptLUim5SAxuzyBa-qtoNNUKjMcnN-4YEoHbXzfTmH7UbT_ZUcVZQzGLSqb1YmI9qz1LoqZ36yiNgcnhthcVg&svctype=4&tempid=h5_group_info)
[![MC百科](https://img.shields.io/badge/MC%E7%99%BE%E7%A7%91-%E8%B6%85%E8%B6%8A%E7%BB%B4%E5%BA%A6-3E8E41)](https://www.mcmod.cn/class/19045.html)
[![许可证：MIT](https://img.shields.io/badge/license-MIT-blue)](LICENSE)

[English](README.md) | **简体中文**

</div>

## 简介

超越维度带来了**维度网络**：一个存储系统，能用快捷键在任何地方打开，无需区块加载
还可以与朋友共享，并接入管道、机器和其他物流模组。

## 功能

<img src="docs/images/zh-CN/terminal.png" align="right" width="320" alt="合成终端里存着能量、流体、化学品、魔力、魔源和物品">

### 一个网络存下一切

- 支持物品、流体和 FE 能量。装了通用机械、植物魔法、新生魔艺或工业先锋：灵魂涌动，还能存化学品、魔力、魔源和监守者之魂。
- 最多存二十多亿种资源，每种最多 2⁶³−1 个。
- UI支持容器交互，拿着桶、储罐等容器点存储格，即可装入或倒出。

### 为海量存储设计的终端

- 随时按 <kbd>O</kbd> 打开网络。
- 搜索支持名称和拼音，也支持 `@` 搜模组、`#` 搜标签、`$` 搜提示文字、`*` 搜物品 ID、`%<50` 搜耐久、`&` 搜额外数据；`-` 表示排除，`|` 表示“或”。
- 可以自建分类页，设置图标和搜索规则，以及标记特定资源。
- 能按名称、数量、模组、创造标签页、最大堆叠、存入时间或变动时间排序。
- 自带合成格，可以从 JEI 和 EMI 转移配方。
- 生存模式下，空手对着方块按中键，从网络取一组这种方块；潜行时按中键，把手里的物品存进网络。

<br clear="right">

### 机器

<p align="center">
  <img src="docs/images/zh-CN/machines.png" alt="冶炼中的网络熔炉，以及能量网络界面" width="100%">
</p>

| 方块 | 作用 |
| --- | --- |
| 维度网络通道 | 管道、漏斗和其他模组的存储能通过它访问整个网络 |
| 维度网络能量通道 | FE 能量的通道，带实时流量图，也能往外输出能量 |
| 网络接口 | 从网络内抽出资源，或者从外部接受资源 |
| 网络泵 | 从相邻容器里抽资源进网络 |
| 网络漏斗 | 收集掉落物、经验球和流体 |
| 网络熔炉、网络高炉、网络烟熏炉 | 方便的冶炼工具，能直接从网络内取原料和燃料 |
| 存量发信器 | 网络里某种资源高于或低于阈值时，发出红石信号 |
| 网络终端 | 打开带合成格的网络界面 |
| 网络控制器 | 管理网络成员和权限 |

机器都有过滤、分面输入输出和红石控制。并提供**机器配置卡**用于快速复制粘贴。

### 工具

| 物品 | 作用 |
| --- | --- |
| 便携网络终端 | 放在背包或饰品栏里的终端 |
| 网络磁铁 | 收集掉落物、经验球和流体 |
| 网络喂食器 | 自动喂食 |
| 网络补货器 | 自动补货 |
| 经验棒 | 存取经验，或维持经验等级 |

### 共享网络

网络里有所有者、管理员和成员三种身份。把**成员连接符**或**管理员连接符**绑到你的网络上，给别的玩家用一下，对方就加入了。加入了多个网络的玩家，可以在主网络切换器里选主网络。

### 配色

<p align="center">
  <img src="docs/images/zh-CN/schemes.png" alt="同一个终端的超维机壳、玻璃、原版和黄铜配色" width="100%">
</p>

界面有超维机壳、玻璃、原版、黄铜四种配色。点窗口上的调色板按钮可以换配色，游戏里能随意修改配色。资源包能添加自定义配色。

## 快速上手

1. 用下界之星、四颗钻石和四个 TNT 合成**不稳定时空碎片**。
2. 把它带在背包里，大约一小时后变成**稳态时空碎片**。
3. 合成**维度网络发生器**，用它建立你的网络。
4. 按 <kbd>O</kbd> 打开网络。
5. 网络每十分钟产出一个**破碎的时空结晶**。把结晶烧成**时空锭**，再做成**稳定的时空框架**，超越维度的方块和工具用它合成。

## 模组联动

| 模组 | 联动内容 |
| --- | --- |
| 应用能源 2 | 用维度ME磁盘把网络接进 ME 系统，性能远超存储总线 |
| 精致存储 | 用 RS维度通道把网络接进 RS 系统 |
| 通用机械 | 存化学品 |
| 机械动力 | 蓝图接口让蓝图加农炮直接从网络取材料 |
| 植物魔法 | 存魔力 |
| 新生魔艺 | 存魔源 |
| 工业先锋：灵魂涌动 | 存监守者之魂 |
| 通用拼音搜索（JECh） | 安装时使用它的拼音搜索 |

## 安装

在 [CurseForge](https://www.curseforge.com/projects/1222890) 下载对应 Minecraft 版本的文件，所需前置以文件页面为准。使用 CompixelUI 界面的版本还要装：

- [CompixelUI](https://www.curseforge.com/projects/1715758)：超越维度的界面库；
- Kotlin：装 [Kotlin for Forge](https://www.curseforge.com/minecraft/mc-mods/kotlin-for-forge)，或者用文件名以 `-with-kotlin` 结尾的 CompixelUI，它自带 Kotlin。

### 版本支持

| Minecraft | 加载器 | 分支 | 状态 |
| --- | --- | --- | --- |
| 26.3 | NeoForge | [`26.3`](https://github.com/Frostbite-time/BeyondDimensions/tree/26.3) | 更新中 |
| 26.2 | NeoForge | [`26.2`](https://github.com/Frostbite-time/BeyondDimensions/tree/26.2) | 更新中 |
| 26.1 | NeoForge | [`26.1`](https://github.com/Frostbite-time/BeyondDimensions/tree/26.1) | 更新中 |
| 1.21.11 | NeoForge | [`1.21.11`](https://github.com/Frostbite-time/BeyondDimensions/tree/1.21.11) | 已停更 |
| 1.21.1 | NeoForge | [`1.21.1`](https://github.com/Frostbite-time/BeyondDimensions/tree/1.21.1) | 更新中 |
| 1.20.1 | Forge | [`1.20.1`](https://github.com/Frostbite-time/BeyondDimensions/tree/1.20.1) | 更新中 |
| 1.12.2 | Forge | [`1.12.2`](https://github.com/Frostbite-time/BeyondDimensions/tree/1.12.2) | 已停更 |

## 指南

整合包和服务器：

- [KubeJS](docs/zh-CN/kubejs.md)：用脚本限制网络、放初始物资、做升级流程
- [存储规则](docs/zh-CN/storage-rules.md)：资源进出网络时拒收、转换等
- [配色](docs/zh-CN/color-schemes.md)：自定义UI配色并导出成资源包
- [命令与配置](docs/zh-CN/commands.md)

附属开发：

- [网络与存储](docs/zh-CN/networks.md)：查找网络、存取资源、监听事件，在网络上存自定义的数据
- [资源类型](docs/zh-CN/resource-types.md)：让网络存一种新资源
- [机器设置](docs/zh-CN/machines.md)：自动保存、同步，并能用机器配置卡复制的设置
- [界面](docs/zh-CN/screens.md)：给超越维度的界面加页面、向服务器发动作、改槽位点击

## 开发者

[![Maven](https://img.shields.io/maven-metadata/v?metadataUrl=https%3A%2F%2Fmaven.wintercogs.com%2Freleases%2Fcom%2Fwintercogs%2Fbeyonddimensions%2Fbeyonddimensions-1.21.1-neoforge%2Fmaven-metadata.xml&label=maven)](https://maven.wintercogs.com/releases/com/wintercogs/beyonddimensions/beyonddimensions-1.21.1-neoforge/maven-metadata.xml)

超越维度发布在 wintercogs Maven 仓库。使用下述方式引入：

```groovy
repositories {
    maven { url = 'https://maven.wintercogs.com/releases' }
}

dependencies {
    implementation "com.wintercogs.beyonddimensions:beyonddimensions-1.21.1-neoforge:${beyond_dimensions_version}"

    // 开发环境运行时还需要 Kotlin 提供者和超越维度的拼音库
    runtimeOnly "maven.modrinth:kotlin-for-forge:${kff_version}"
    additionalRuntimeClasspath "com.github.promeg:tinypinyin:2.0.3"
}
```

### 从源码构建

用 JDK 21 运行 `./gradlew build`，模组 jar 在 `build/libs` 里。

## 参与贡献

反馈问题请到 [Issues](https://github.com/Frostbite-time/BeyondDimensions/issues) 。提交代码，请先看[贡献指南](docs/CONTRIBUTING.md)。

## 许可证

超越维度以 [MIT 许可证](LICENSE) 发布，第三方声明见 [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)。
