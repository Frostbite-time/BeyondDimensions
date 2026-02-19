* **Backported `IStackKey` from 1.21.1**

    * The former `IStackType` design has been refactored: it is now split into `IStackKey`, and `IStackKey` no longer stores an amount.
    * Use `KeyAmount` as a partial replacement where you previously relied on “type + amount” together.
    * **Important:** if you were using `IStackType` as a map/set key, migrate the key to `IStackKey` (not `KeyAmount`).

* **Dimensions network: fuzzy extraction API**

    * Added an interface to support fuzzy-based extraction.

* **UI improvements**

    * Added a “sort by time” option.
