# Storage rules

[简体中文](../zh-CN/storage-rules.md) · [All guides](../../README.md#guides)

Storage rules decide what happens when a resource enters or leaves a network: refuse it, convert it, charge for it or
keep a reserve. They apply to every insert and extract, from screens, pipes and other mods alike.

## Applying rules

Inserts and extracts each have their own table. For each resource, the rules are asked in table order; the first one
that returns a plan decides, and a rule that returns `null` passes to the next. If no rule returns a plan, the resource is
stored or handed out as usual.

A rule receives a request:

| Method | Returns |
| --- | --- |
| `key()` | The resource |
| `amount()` | The amount requested |
| `net()` | The network |
| `stored(key)` | How much of `key` the network holds, counting changes this operation hasn't committed yet |

## Insert plans

| Plan | Effect |
| --- | --- |
| `InsertPlan.reject()` | Refuse the resource |
| `InsertPlan.store()` | Store it, without asking later rules |
| `InsertPlan.discard()` | Accept it and store nothing |
| `InsertPlan.convert(unit)` | Accept it in groups of `unit` and store the plan's `add`s instead |

Then, per unit:

| Method | Effect |
| --- | --- |
| `add(key, amount)` | Also store `amount` of `key` |
| `take(key, amount)` | Remove `amount` of `key` from the network; without enough, the unit is refused |
| `limit(max)` | Accept at most `max` of the requested resource |
| `onCommit((request, amount) -> ...)` | Run once the operation is committed, with the amount accepted; not during simulation |

Only whole units are accepted; the rest goes back to whoever was inserting.

## Extract plans

| Plan or method | Effect |
| --- | --- |
| `ExtractPlan.reject()` | Hand out nothing |
| `ExtractPlan.allow()` | Hand out the resource |
| `take(key, amount)` | For each one handed out, also remove `amount` of `key`; without enough, hand out fewer |
| `limit(max)` | Hand out at most `max` |
| `onCommit((request, amount) -> ...)` | Run once committed, with the amount handed out |

The network only ever hands out the resource that was asked for.

## Register a rule

| Method | Rule for |
| --- | --- |
| `UnifiedStorageRules.insert(id, key, rule)` | Inserts of one resource |
| `UnifiedStorageRules.insertMatching(id, matcher, rule)` | Inserts of every resource `matcher` accepts |
| `UnifiedStorageRules.extract(id, key, rule)` | Extracts of one resource |
| `UnifiedStorageRules.extractMatching(id, matcher, rule)` | Extracts of every resource `matcher` accepts |
| `removeInsert(id)`, `removeExtract(id)` | Remove a rule |
| `insertIds()`, `extractIds()` | The rules in table order |

New rules go to the end of the table. To place one elsewhere, pass a fourth argument: `UnifiedStorageRules.first()`,
`last()`, `before(id)` or `after(id)`. Each ID can be registered once per table.

Register rules during mod setup, for example in `FMLCommonSetupEvent`:

```java
// A water bucket becomes an empty bucket and 1,000 mB of water
UnifiedStorageRules.insert(ResourceLocation.fromNamespaceAndPath("examplemod", "unpack_water_bucket"),
        new ItemStackKey(new ItemStack(Items.WATER_BUCKET)),
        request -> InsertPlan.convert(1)
                .add(new ItemStackKey(new ItemStack(Items.BUCKET)), 1)
                .add(new FluidStackKey(new FluidStack(Fluids.WATER, 1)), 1000));

// The last 1,000 cobblestone stay in the network
UnifiedStorageRules.extract(ResourceLocation.fromNamespaceAndPath("examplemod", "keep_cobblestone"),
        new ItemStackKey(new ItemStack(Items.COBBLESTONE)),
        request -> ExtractPlan.allow().limit(request.stored(request.key()) - 1000));

// Nothing from Create goes into networks
UnifiedStorageRules.insertMatching(ResourceLocation.fromNamespaceAndPath("examplemod", "no_create"),
        key -> key.getModId().equals("create"),
        request -> InsertPlan.reject());
```

## Writing rules

- A rule only reads and must not change any storage. It is called both when simulating and when carrying out an
  operation, and the same state must give the same plan. Changes to storage go in the plan; other side effects go in
  `onCommit`.
- Resources a plan adds or takes pass through the rules too, but each rule applies at most once along the way, so a
  rule can't loop.
- Matchers run for every insert or extract, so keep them cheap. Rules for one exact resource are looked up directly.
- If a rule throws, the operation is refused and the error is logged, once per rule.
- Beyond Dimensions registers one insert rule itself: `beyonddimensions:uncompress_matter_ball` unpacks Compressed
  Matter Orbs. Register a rule before it to take over.
