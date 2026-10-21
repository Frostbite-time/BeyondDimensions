# Color schemes

[简体中文](../zh-CN/color-schemes.md) · [All guides](../../README.md#guides)

Beyond Dimensions' screens take their colors from [CompixelUI color schemes](https://github.com/Frostbite-time/CompixelUI/blob/main/docs/en/themes.md)
under the namespace `beyonddimensions`. Players pick a scheme and change any color with the palette button on each
window. Resource packs and modpacks add or change schemes with JSON files; CompixelUI's guide explains the format, and
this page lists what is particular to Beyond Dimensions.

## Schemes

| File | Scheme |
| --- | --- |
| `casing` | Beyond Dimensions Casing, the default |
| `glass` | Glass, translucent |
| `vanilla` | Vanilla |
| `brass` | Brass |

They live in `assets/beyonddimensions/compixel/schemes/`, and `assets/beyonddimensions/compixel/schemes.json` sets the
default and the order. A pack changes a scheme with a file of the same name and adds one with a new name:

```json
{
  "format": 1,
  "name": "Deep Sea",
  "extends": "casing",
  "colors": {
    "window": "#16324A",
    "accent": "#4FD1C5"
  }
}
```

## Color names

The scheme files bundled with your version, the JSON files under `assets/beyonddimensions/compixel/schemes/` in the
mod's jar, show the color names and how the built-in schemes set them; copy one as a starting point. The in-game
editor lists every color, so you can also adjust the colors there and export them as a resource pack. For a
translucent scheme, start from `glass.json`.
