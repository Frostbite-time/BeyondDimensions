## English

- The Stonecutter refills its input from the storage, like the Crafting Table: taking a product while one input item is left takes another from the storage, then from your inventory, so the selected recipe stays.
- The Crafting Table's Space and Shift clicks work on the Stonecutter too: Space + left-click the product to keep cutting until your inventory is full or the input runs out, Shift + left-click it to cut a stack, and Space + left-click the input to clear it.

### For developers

- `BatchCraftWidget` marks a workbench that can craft repeatedly; the storage screen gives its product and input slots the Crafting Table's Space and Shift clicks.

### Breaking changes

For developers:

- `CraftingWidget.clearGrid()` and `isGridSlot()` were removed; use `clearItems(toStorageFirst)` and `inputSlots()`.

## 中文

- 切石机像合成台一样从存储补充原料：原料只剩最后一个时取出产物，会先从存储、再从背包补上一个，选中的配方不会丢失。
- 合成台的空格与 Shift 点击也可用于切石机：空格 + 左键点产物连续切割，直到背包放满或原料用完；Shift + 左键点产物切一组；空格 + 左键点原料槽清空原料。

### 开发者

- `BatchCraftWidget` 表示能连续制作的工作台，存储界面为它的产物槽与原料槽提供合成台的空格与 Shift 点击。

### 破坏性改动

开发者：

- 移除了 `CraftingWidget.clearGrid()` 与 `isGridSlot()`，请改用 `clearItems(toStorageFirst)` 与 `inputSlots()`。
