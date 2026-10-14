<div align="center">

<img src="docs/images/blocks.png" alt="Beyond Dimensions blocks standing in a meadow" width="100%">

# Beyond Dimensions

A storage mod for Minecraft. Store everything and access it from anywhere.

[![CurseForge downloads](https://img.shields.io/curseforge/dt/1222890?logo=curseforge&label=CurseForge&color=F16436)](https://www.curseforge.com/projects/1222890)
[![Modrinth downloads](https://img.shields.io/modrinth/dt/beyonddimensions?logo=modrinth&label=Modrinth&color=1BD96A)](https://modrinth.com/mod/beyonddimensions)
[![Discord](https://img.shields.io/badge/Discord-join-5865F2?logo=discord&logoColor=white)](https://discord.gg/AgnbJXeEXK)
[![License: MIT](https://img.shields.io/badge/license-MIT-blue)](LICENSE)

**English** | [简体中文](README.zh-CN.md)

</div>

## About

Beyond Dimensions adds the **Dimensional Network**, a storage system that belongs to you rather than to a block. Create
one in the late game, then press a key to open it wherever you are, share it with friends, and connect it to pipes,
machines and other storage mods.

## Features

<img src="docs/images/en/terminal.png" align="right" width="320" alt="The crafting terminal holding energy, fluids, chemicals, mana, source and items">

### One network for everything

- Items, fluids and Forge Energy, plus chemicals, mana, source and warden souls when Mekanism, Botania, Ars Nouveau or
  Industrial Foregoing Souls is installed.
- Room for more than two billion kinds of resources and up to 2⁶³−1 of each.
- Click a storage slot with a bucket, tank or other container to fill or empty it.

### A terminal for large collections

- Press <kbd>O</kbd> anywhere to open your network, or use a Network Terminal or the Portable Network Terminal.
- Search by name or pinyin, or by mod `@`, tag `#`, tooltip `$`, item ID `*`, durability `%<50` and extra data `&`.
  Leave things out with `-` and allow alternatives with `|`.
- Make your own category tabs with icons, search rules and hand-picked resources.
- Sort by name, amount, mod, creative tab, stack size, first stored or last changed, then by a second key.
- Craft in the built-in grid, with recipe transfer from JEI and EMI.
- In survival, middle-click a block with an empty hand to take a stack of it from the network; sneak and middle-click
  to put the stack in your hand into the network.

<br clear="right">

### Machines

<p align="center">
  <img src="docs/images/en/machines.png" alt="A Network Furnace smelting in nine lanes and the Energy Network screen" width="100%">
</p>

| Block | What it does |
| --- | --- |
| Dimensional Network Pathway | Opens the whole network to pipes, hoppers and other mods' storage |
| Dimensional Network Energy Pathway | Does the same for Forge Energy, with a live flow graph, and can push energy out |
| Network Interface | Keeps marked resources in stock and sends everything else to the network; works on Create contraptions |
| Network Pump | Pulls resources from neighbouring containers into the network |
| Network Hopper | Collects dropped items, XP orbs and fluids, from a 2-block radius up to a whole chunk |
| Network Furnace, Blast Furnace and Smoker | Smelt in nine lanes, taking inputs and fuel from the network; FE and lava burn as fuel |
| Stock Emitter | Emits a redstone signal while a resource in the network is above or below a threshold |
| Network Terminal | Opens the network with a crafting grid |
| Network Controller | Manages a network's members and their roles |

Machines have filters, per-side input and output settings and redstone control. A **Machine Configuration Card** copies
the settings of one machine to others, including every connected machine of the same kind.

### Tools

| Item | What it does |
| --- | --- |
| Portable Network Terminal | Your terminal, in the inventory or a Curios slot |
| Network Magnet | Collects nearby items and fluids into the network or your inventory |
| Network Feeder | Feeds you from the network when you get hungry, in the mode you choose |
| Network Restocker | Keeps your inventory stocked from the network and sends extra items back |
| XP Rod | Stores and returns experience, and can keep you at a target level |

### Shared networks

Each network has an owner, managers and members. Bind a **Member Connector** or **Manager Connector** to your network
and give it to another player; using it brings them in. A player in several networks picks a primary one in the network
switcher.

### Color schemes

<p align="center">
  <img src="docs/images/en/schemes.png" alt="The same terminal in the Beyond Dimensions Casing, Glass, Vanilla and Brass color schemes" width="100%">
</p>

Choose between four color schemes, Beyond Dimensions Casing, Glass, Vanilla and Brass, or change any color in the
in-game editor, opened with the palette button in each window.

## Getting started

1. Craft an **Unstable Space-Time Fragment** from a Nether Star, four Diamonds and four TNT.
2. Keep it in your inventory. After about an hour it becomes a **Stable Space-Time Fragment**.
3. Craft a **Dimensional Network Generator** and use it to create your network.
4. Press <kbd>O</kbd> to open the network.
5. Every ten minutes the network produces a **Shattered Space-Time Crystal**. Smelt crystals into **Space-Time Ingots**
   and craft them into **Stable Space-Time Frames**, which BD's blocks and tools are made from.

JEI or EMI show every recipe.

## Compatibility

| Mod | What Beyond Dimensions adds |
| --- | --- |
| JEI, EMI | Recipe transfer, dragging resources into filter slots and a shared search box |
| Applied Energistics 2 | The Dimensional ME Storage Cell puts a network into an ME system. With Applied Flux, Applied Mekanistics, Applied Botanics, Ars Énergistique or Soulplied Energistics it also carries energy, chemicals, mana, source or souls |
| Refined Storage | The RS Dimensional Pathway connects a network to an RS system, with chemicals through Refined Storage Mekanism Integration and energy and source through Refined Types |
| Mekanism | Chemical storage |
| Create | The Schematic Interface supplies Schematicannons from the network, and Network Interfaces work on contraptions |
| Botania | Mana storage and the Dimensional Mana Pool |
| Ars Nouveau | Source storage and the Dimensional Source Pathway |
| Industrial Foregoing Souls | Warden soul storage and the Warden Soul Tagger for filter slots |
| Curios | The Portable Network Terminal, Network Magnet, Network Feeder and Network Restocker work in curio slots |
| Jade | Shows a block's network and contents |
| Polymorph | A choice between conflicting recipes in the crafting grid |
| Just Enough Characters | Its pinyin matching in the search box |
| KubeJS | Scripts can change how networks are created, their limits and their storage rules |

## Installation

Beyond Dimensions runs on Minecraft 1.21.1 with NeoForge. It also needs:

- [CompixelUI](https://www.curseforge.com/projects/1715758), the library its screens are built with;
- Kotlin: install [Kotlin for Forge](https://www.curseforge.com/minecraft/mc-mods/kotlin-for-forge), or use the
  CompixelUI file whose name ends in `-with-kotlin`, which includes it.

### Other Minecraft versions

This page describes the 1.21.1 version. Each Minecraft version is developed on its own branch, and all of them can be
downloaded from CurseForge and Modrinth.

| Minecraft | Loader | Branch |
| --- | --- | --- |
| 26.3 | NeoForge | [`26.3`](https://github.com/Frostbite-time/BeyondDimensions/tree/26.3) |
| 26.2 | NeoForge | [`26.2`](https://github.com/Frostbite-time/BeyondDimensions/tree/26.2) |
| 26.1 | NeoForge | [`26.1`](https://github.com/Frostbite-time/BeyondDimensions/tree/26.1) |
| 1.21.11 | NeoForge | [`1.21.11`](https://github.com/Frostbite-time/BeyondDimensions/tree/1.21.11) |
| 1.21.1 | NeoForge | [`1.21.1`](https://github.com/Frostbite-time/BeyondDimensions/tree/1.21.1) |
| 1.20.1 | Forge | [`1.20.1`](https://github.com/Frostbite-time/BeyondDimensions/tree/1.20.1) |
| 1.12.2 | Forge | [`1.12.2`](https://github.com/Frostbite-time/BeyondDimensions/tree/1.12.2) |

## For developers

[![Maven](https://img.shields.io/maven-metadata/v?metadataUrl=https%3A%2F%2Fmaven.wintercogs.com%2Freleases%2Fcom%2Fwintercogs%2Fbeyonddimensions%2Fbeyonddimensions-1.21.1-neoforge%2Fmaven-metadata.xml&label=maven)](https://maven.wintercogs.com/releases/com/wintercogs/beyonddimensions/beyonddimensions-1.21.1-neoforge/maven-metadata.xml)

Beyond Dimensions is published to the wintercogs Maven repository together with its sources, so your IDE shows its code
as written. CompixelUI comes with the dependency.

```groovy
repositories {
    maven { url = 'https://maven.wintercogs.com/releases' }
}

dependencies {
    implementation "com.wintercogs.beyonddimensions:beyonddimensions-1.21.1-neoforge:${beyond_dimensions_version}"

    // Development runs also need a Kotlin provider and BD's pinyin library
    runtimeOnly "maven.modrinth:kotlin-for-forge:${kff_version}"
    additionalRuntimeClasspath "com.github.promeg:tinypinyin:2.0.3"
}
```

Kotlin for Forge comes from Modrinth's Maven repository, `https://api.modrinth.com/maven`.

### Building from source

Run `./gradlew build` with JDK 21. The mod jar is written to `build/libs`.

## Contributing

Bug reports and pull requests are welcome in the [issue tracker](https://github.com/Frostbite-time/BeyondDimensions/issues).
Please read [CONTRIBUTING.md](docs/CONTRIBUTING.md) (in Chinese) before you open a pull request. You can also find us on
[Discord](https://discord.gg/AgnbJXeEXK), and there is a Chinese wiki on [MC百科](https://www.mcmod.cn/class/19045.html).

## License

Beyond Dimensions is released under the [MIT License](LICENSE). Third-party notices are listed in
[THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).

Not an official Minecraft product. Not approved by or associated with Mojang or Microsoft.
