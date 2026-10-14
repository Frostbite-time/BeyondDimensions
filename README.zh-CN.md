<div align="center">

<img src="docs/images/blocks.png" alt="草地上的超越维度方块" width="100%">

# 超越维度 Beyond Dimensions

一个 Minecraft 存储模组。存储一切，随处访问。

[![CurseForge 下载量](https://img.shields.io/curseforge/dt/1222890?logo=curseforge&label=CurseForge&color=F16436)](https://www.curseforge.com/projects/1222890)
[![Modrinth 下载量](https://img.shields.io/modrinth/dt/beyonddimensions?logo=modrinth&label=Modrinth&color=1BD96A)](https://modrinth.com/mod/beyonddimensions)
[![Discord](https://img.shields.io/badge/Discord-join-5865F2?logo=discord&logoColor=white)](https://discord.gg/AgnbJXeEXK)
[![MC百科](https://img.shields.io/badge/MC%E7%99%BE%E7%A7%91-%E8%B6%85%E8%B6%8A%E7%BB%B4%E5%BA%A6-3E8E41)](https://www.mcmod.cn/class/19045.html)
[![许可证：MIT](https://img.shields.io/badge/license-MIT-blue)](LICENSE)

[English](README.md) | **简体中文**

</div>

## 简介

超越维度带来**维度网络**：一个属于你、而不是属于某个方块的存储系统。在游戏后期创建它之后，按一个键就能在任何地方打开，
还可以与朋友共享，并接入管道、机器和其他存储模组。

## 功能

<img src="docs/images/zh-CN/terminal.png" align="right" width="320" alt="合成终端里存着能量、流体、化学品、魔力、魔源和物品">

### 一个网络存下一切

- 物品、流体和 FE 能量；装有通用机械、植物魔法、新生魔艺或工业先锋：灵魂涌动时，还能存化学品、植物魔力、魔源和监守者之魂。
- 可存二十多亿种资源，每种最多 2⁶³−1 个。
- 拿着桶、储罐或其他容器点击存储槽位，即可装入或倒出。

### 为海量存储设计的终端

- 在任何地方按 <kbd>O</kbd> 打开网络，也可以用网络终端或便携网络终端。
- 按名称或拼音搜索，也可以按模组 `@`、标签 `#`、提示文字 `$`、物品 ID `*`、耐久 `%<50` 和额外数据 `&` 搜索；
  用 `-` 排除，用 `|` 表示“或”。
- 自建分类标签，设置图标、搜索规则和单独挑选的资源。
- 按名称、数量、模组、创造模式标签页、堆叠上限、存入时间或变动时间排序，还能再按第二项排序。
- 自带合成格，支持从 JEI 和 EMI 转移配方。
- 生存模式下，空手对着方块按鼠标中键，可从网络取出一组该方块；潜行时按中键，把手中的物品存入网络。

<br clear="right">

### 机器

<p align="center">
  <img src="docs/images/zh-CN/machines.png" alt="九条通道同时冶炼的网络熔炉，以及能量网络界面" width="100%">
</p>

| 方块 | 作用 |
| --- | --- |
| 维度网络通道 | 把整个网络开放给管道、漏斗和其他模组的存储 |
| 维度网络能量通道 | 对 FE 能量做同样的事，显示实时流量图，也能向外输出能量 |
| 网络接口 | 让标记的资源保持有货，其他内容送进网络；在机械动力的装置上也能工作 |
| 网络泵 | 从相邻容器中抽取资源存入网络 |
| 网络漏斗 | 收集掉落物、经验球和流体，范围从半径 2 格到整个区块 |
| 网络熔炉、网络高炉、网络烟熏炉 | 九条通道同时冶炼，原料和燃料取自网络；FE 能量和熔岩也能当燃料 |
| 存量发信器 | 网络中某种资源高于或低于阈值时发出红石信号 |
| 网络终端 | 打开带合成格的网络界面 |
| 网络控制器 | 管理网络的成员和身份 |

机器支持过滤、按面设置输入输出和红石控制。**机器配置卡**可以把一台机器的设置复制到其他机器上，也能一次粘贴到所有相连的同类机器。

### 工具

| 物品 | 作用 |
| --- | --- |
| 便携网络终端 | 放在背包或饰品栏里的终端 |
| 网络磁铁 | 把附近的物品和流体收进网络或背包 |
| 网络喂食器 | 饿了时按你选的方式用网络里的食物喂你 |
| 网络补货器 | 从网络补足背包里的物品，多余的送回网络 |
| 经验棒 | 存取经验，也能让你保持在目标等级 |

### 共享网络

每个网络有所有者、管理员和成员。把**成员连接符**或**管理员连接符**绑定到你的网络，交给其他玩家使用，对方就会加入。
同时加入多个网络的玩家可以在主网络切换器里选择主网络。

### 配色

<p align="center">
  <img src="docs/images/zh-CN/schemes.png" alt="同一个终端的超维机壳、玻璃、原版和黄铜配色" width="100%">
</p>

可以在超维机壳、玻璃、原版、黄铜四种配色之间切换，也可以点每个窗口上的调色板按钮，在游戏里修改任意颜色。

## 快速上手

1. 用下界之星、四颗钻石和四个 TNT 合成**不稳定时空碎片**。
2. 把它放在背包里，大约一小时后会变成**稳态时空碎片**。
3. 合成**维度网络发生器**，使用它创建你的网络。
4. 按 <kbd>O</kbd> 打开网络。
5. 网络每十分钟产出一个**破碎的时空结晶**。把结晶烧成**时空锭**，再做成**稳定的时空框架**，超越维度的方块和工具都要用到它。

所有配方都可以在 JEI 或 EMI 中查看。

## 模组联动

| 模组 | 超越维度提供的内容 |
| --- | --- |
| JEI、EMI | 配方转移、把资源拖进过滤槽、同步搜索框 |
| 应用能源 2 | 维度ME磁盘让网络成为 ME 系统的一部分；装有 Applied Flux、Applied Mekanistics、Applied Botanics、Ars Énergistique 或 Soulplied Energistics 时，还能接入能量、化学品、植物魔力、魔源或灵魂 |
| 精致存储 | RS维度通道把网络接入 RS 系统；装有 Refined Storage Mekanism Integration 和 Refined Types 时，还能接入化学品、能量和魔源 |
| 通用机械 | 化学品存储 |
| 机械动力 | 蓝图接口让蓝图炮直接从网络取材料；网络接口在装置上也能工作 |
| 植物魔法 | 植物魔力存储和维度魔力池 |
| 新生魔艺 | 魔源存储和维度魔源通道 |
| 工业先锋：灵魂涌动 | 监守者之魂存储，以及用于过滤槽的灵魂标记器 |
| 饰品栏（Curios） | 便携网络终端、网络磁铁、网络喂食器和网络补货器可以放进饰品栏 |
| Jade | 显示方块所属的网络和其中的内容 |
| 多态合成（Polymorph） | 在合成格里选择冲突的配方 |
| 通用拼音搜索（JECh） | 搜索框使用它的拼音匹配 |
| KubeJS | 用脚本修改网络的创建方式、容量上限和存储规则 |

## 安装

超越维度运行在 Minecraft 1.21.1 的 NeoForge 上，还需要：

- [CompixelUI](https://www.curseforge.com/projects/1715758)：超越维度的界面所用的库；
- Kotlin：安装 [Kotlin for Forge](https://www.curseforge.com/minecraft/mc-mods/kotlin-for-forge)，或者使用文件名以
  `-with-kotlin` 结尾、自带 Kotlin 的 CompixelUI。

### 其他 Minecraft 版本

本页介绍的是 1.21.1 版本。每个 Minecraft 版本在各自的分支上开发，都可以在 CurseForge 和 Modrinth 下载。

| Minecraft | 加载器 | 分支 |
| --- | --- | --- |
| 26.3 | NeoForge | [`26.3`](https://github.com/Frostbite-time/BeyondDimensions/tree/26.3) |
| 26.2 | NeoForge | [`26.2`](https://github.com/Frostbite-time/BeyondDimensions/tree/26.2) |
| 26.1 | NeoForge | [`26.1`](https://github.com/Frostbite-time/BeyondDimensions/tree/26.1) |
| 1.21.11 | NeoForge | [`1.21.11`](https://github.com/Frostbite-time/BeyondDimensions/tree/1.21.11) |
| 1.21.1 | NeoForge | [`1.21.1`](https://github.com/Frostbite-time/BeyondDimensions/tree/1.21.1) |
| 1.20.1 | Forge | [`1.20.1`](https://github.com/Frostbite-time/BeyondDimensions/tree/1.20.1) |
| 1.12.2 | Forge | [`1.12.2`](https://github.com/Frostbite-time/BeyondDimensions/tree/1.12.2) |

## 开发者

[![Maven](https://img.shields.io/maven-metadata/v?metadataUrl=https%3A%2F%2Fmaven.wintercogs.com%2Freleases%2Fcom%2Fwintercogs%2Fbeyonddimensions%2Fbeyonddimensions-1.21.1-neoforge%2Fmaven-metadata.xml&label=maven)](https://maven.wintercogs.com/releases/com/wintercogs/beyonddimensions/beyonddimensions-1.21.1-neoforge/maven-metadata.xml)

超越维度连同源码发布在 wintercogs Maven 仓库，IDE 中看到的就是原始代码。CompixelUI 会随依赖一起引入。

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

Kotlin for Forge 来自 Modrinth 的 Maven 仓库 `https://api.modrinth.com/maven`。

### 从源码构建

使用 JDK 21 运行 `./gradlew build`，模组 jar 会生成在 `build/libs`。

## 参与贡献

欢迎在 [Issues](https://github.com/Frostbite-time/BeyondDimensions/issues) 反馈问题、提交拉取请求。提交拉取请求前请先阅读
[贡献指南](docs/CONTRIBUTING.md)。也欢迎加入 [Discord](https://discord.gg/AgnbJXeEXK)，或在
[MC百科](https://www.mcmod.cn/class/19045.html) 查看资料。

## 许可证

超越维度以 [MIT 许可证](LICENSE) 发布，第三方声明见 [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)。

本模组不是 Minecraft 官方产品，未经 Mojang 或 Microsoft 批准，也与其无关。
