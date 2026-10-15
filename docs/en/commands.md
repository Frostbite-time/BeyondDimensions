# Commands and configuration

[简体中文](../zh-CN/commands.md) · [All guides](../../README.md#guides)

## Commands

These need permission level 2 (operators). Network IDs are the numbers shown after a network's name, such as `#0003`.

| Command | Does |
| --- | --- |
| `/bdtools network create <player> [<slotCapacity> [<slotMaxSize>]]` | Creates a network for a player who has none; it holds `slotMaxSize` kinds of resources and up to `slotCapacity` of each, without limits by default |
| `/bdtools network setOwner <netId> <player>` | Makes the player the owner; the old owner stays as a manager |
| `/bdtools network addManager <netId> <player>` | Adds a manager |
| `/bdtools network removeManager <netId> <player>` | Makes a manager a member again |
| `/bdtools network addPlayer <netId> <player>` | Adds a member |
| `/bdtools network removePlayer <netId> <player>` | Removes a member |
| `/bdtools network deleteNet <netId>` | Deletes a network and everything in it |
| `/bdtools network deleteNetByPlayer <player>` | Deletes the player's primary network |

On the client, `/bdtools searchCache clear` clears the tooltip text that search keeps.

## Server configuration

Each world keeps its settings in `serverconfig/beyonddimensions-server.toml`; a modpack sets the defaults for new worlds
with `defaultconfigs/beyonddimensions-server.toml`. NeoForge's config screen in the mod list edits it too.

| Option | Default | Does |
| --- | --- | --- |
| `interface.receive` | `true` | Network Interfaces send resources into the network |
| `interface.output` | `true` | Network Interfaces take marked resources from the network |
| `interface.push_out` | `true` | Network Interfaces push their contents into neighboring containers |
| `interface.blocked_input_types` | `[]` | Resource types Network Interfaces may not send into the network, such as `"beyonddimensions:stack_type/fluid"` |
| `space_time.fragment_transfer_time` | `3600` | Seconds an Unstable Space-Time Fragment needs to become stable |
| `space_time.crystal_generate_time` | `600` | Seconds between Shattered Space-Time Crystals; `0` turns them off |

Settings for the storage screen, such as sorting and its size, are per player, in `config/beyonddimensions-client.toml`.
