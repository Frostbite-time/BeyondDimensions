package com.wintercogs.beyonddimensions.client.recipe;

import com.wintercogs.beyonddimensions.api.storage.key.IStackKey;
import com.wintercogs.beyonddimensions.api.storage.key.KeyAmount;
import com.wintercogs.beyonddimensions.api.storage.key.impl.ItemStackKey;
import com.wintercogs.beyonddimensions.common.menu.DimensionsCraftMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.*;
import java.util.function.Predicate;

/** Shared JEI/EMI accounting. Exact component variants form separate stacks; viewer matching stays in the bridges. */
public record RecipeFillPlan(List<IStackKey<?>> keys, List<Long> amounts, List<Integer> missing) {
    public RecipeFillPlan {
        keys = List.copyOf(keys);
        amounts = List.copyOf(amounts);
        missing = List.copyOf(missing);
    }

    public boolean complete() { return missing.isEmpty(); }
    public boolean hasMaterials() { return amounts.stream().anyMatch(amount -> amount > 0); }

    public record Input(List<ItemStackKey> choices, long count, boolean catalyst) {
        public Input { choices = List.copyOf(choices); if (count < 0) throw new IllegalArgumentException("Negative ingredient amount"); }
        public static Input blank() { return new Input(List.of(), 0, false); }
    }

    public static final class Pool {
        private final Map<ItemStackKey, Long> totals = new LinkedHashMap<>();
        private final Map<Item, List<ItemStackKey>> byItem = new HashMap<>();

        public void add(ItemStackKey key, long count) {
            if (key.isEmpty() || count <= 0) return;
            if (!totals.containsKey(key)) byItem.computeIfAbsent(key.getSource(), ignored -> new ArrayList<>()).add(key);
            totals.merge(key, count, RecipeFillPlan::saturatedAdd);
        }

        public List<ItemStackKey> matching(Collection<Item> items, Predicate<ItemStackKey> matches) {
            var found = new ArrayList<ItemStackKey>();
            for (Item item : new LinkedHashSet<>(items))
                for (ItemStackKey key : byItem.getOrDefault(item, List.of()))
                    if (matches.test(key)) found.add(key);
            return List.copyOf(found);
        }

        /** Count each physical source once, then merge matching component variants across sources. */
        public static Pool capture(DimensionsCraftMenu menu) {
            var pool = new Pool();
            for (int slot = menu.craftSlotStartIndex; slot < menu.craftSlotEndIndex; slot++) pool.add(menu.slots.get(slot).getItem());
            for (ItemStack stack : menu.player.getInventory().items) pool.add(stack);
            for (KeyAmount value : menu.storage.getStorage())
                if (value.key() instanceof ItemStackKey key) pool.add(key, value.amount());
            return pool;
        }

        private void add(ItemStack stack) { if (!stack.isEmpty()) add(new ItemStackKey(stack), stack.getCount()); }

        public RecipeFillPlan plan(List<Input> inputs, int requestedBatches) {
            if (inputs.size() > 4096 || requestedBatches < 1) throw new IllegalArgumentException("Invalid recipe fill size");
            if (inputs.stream().allMatch(input -> input.count() <= 1)) return unitPlan(inputs, requestedBatches);
            var remaining = new HashMap<>(totals);
            var keys = new ArrayList<IStackKey<?>>(Collections.nCopies(inputs.size(), ItemStackKey.EMPTY));
            var amounts = new ArrayList<Long>(Collections.nCopies(inputs.size(), 0L));
            var missing = new ArrayList<Integer>();
            var order = new ArrayList<Integer>();
            for (int i = 0; i < inputs.size(); i++) if (inputs.get(i).count() > 0) order.add(i);
            // Reserve narrowly constrained ingredients first, then prefer the largest exact stack pool.
            order.sort(Comparator.<Integer>comparingInt(i -> inputs.get(i).choices().size())
                    .thenComparing(Comparator.<Integer>comparingLong(i -> inputs.get(i).count()).reversed()));
            for (int index : order) {
                Input input = inputs.get(index);
                ItemStackKey selected = null;
                long available = 0;
                for (ItemStackKey candidate : input.choices()) {
                    long amount = remaining.getOrDefault(candidate, 0L);
                    if (input.count() <= candidate.getVanillaMaxStackSize() && amount > available) {
                        selected = candidate; available = amount;
                    }
                }
                long take = Math.min(available, input.count());
                if (take < input.count()) missing.add(index);
                if (take > 0) {
                    keys.set(index, selected); amounts.set(index, take); remaining.put(selected, available - take);
                }
            }

            return scale(inputs, keys, amounts, missing, requestedBatches);
        }

        /** Unit ingredients use capacitated matching, so overlapping tags cannot starve a later ingredient. */
        private RecipeFillPlan unitPlan(List<Input> inputs, int requestedBatches) {
            var keys = matchUnits(inputs, 1);
            var missing = new ArrayList<Integer>();
            for (int i = 0; i < inputs.size(); i++) if (inputs.get(i).count() > 0 && keys.get(i).isEmpty()) missing.add(i);
            int batches = 1;
            if (missing.isEmpty() && inputs.stream().noneMatch(Input::catalyst)) {
                int upper = Math.min(requestedBatches, inputs.stream().flatMap(input -> input.choices().stream())
                        .mapToInt(key -> (int)key.getVanillaMaxStackSize()).max().orElse(1));
                while (batches < upper) {
                    int trial = batches + (upper - batches + 1) / 2;
                    var candidate = matchUnits(inputs, trial);
                    boolean complete = true;
                    for (int i = 0; i < inputs.size(); i++) if (inputs.get(i).count() > 0 && candidate.get(i).isEmpty()) { complete = false; break; }
                    if (complete) { batches = trial; keys = candidate; } else upper = trial - 1;
                }
            }
            var amounts = new ArrayList<Long>();
            for (var key : keys) amounts.add(key.isEmpty() ? 0L : (long)batches);
            return inputs.stream().anyMatch(Input::catalyst) ? scale(inputs, keys, amounts, missing, requestedBatches)
                    : new RecipeFillPlan(keys, amounts, missing);
        }

        private ArrayList<IStackKey<?>> matchUnits(List<Input> inputs, int batches) {
            var keys = new ArrayList<IStackKey<?>>(Collections.nCopies(inputs.size(), ItemStackKey.EMPTY));
            var owners = new HashMap<ItemStackKey, List<Integer>>();
            for (int index = 0; index < inputs.size(); index++) if (inputs.get(index).count() > 0)
                assignUnit(index, inputs, batches, keys, owners);
            return keys;
        }

        private boolean assignUnit(int root, List<Input> inputs, int batches, List<IStackKey<?>> keys,
                                   Map<ItemStackKey, List<Integer>> owners) {
            var queue = new ArrayDeque<Integer>(); queue.add(root);
            var visitedSlots = new HashSet<Integer>(); visitedSlots.add(root);
            var parents = new HashMap<ItemStackKey, Integer>();
            while (!queue.isEmpty()) {
                int index = queue.removeFirst();
                for (var key : inputs.get(index).choices()) {
                    long capacity = Math.min(inputs.size(), totals.getOrDefault(key, 0L) / batches);
                    if (capacity == 0 || batches > key.getVanillaMaxStackSize() || parents.putIfAbsent(key, index) != null) continue;
                    var assigned = owners.computeIfAbsent(key, ignored -> new ArrayList<>());
                    if (assigned.size() < capacity) {
                        ItemStackKey destination = key;
                        while (true) {
                            int slot = parents.get(destination);
                            var previous = keys.get(slot);
                            if (!previous.isEmpty()) owners.get(previous).remove(Integer.valueOf(slot));
                            owners.get(destination).add(slot); keys.set(slot, destination);
                            if (slot == root) return true;
                            destination = (ItemStackKey)previous;
                        }
                    }
                    for (int previous : assigned) if (visitedSlots.add(previous)) queue.addLast(previous);
                }
            }
            return false;
        }

        private RecipeFillPlan scale(List<Input> inputs, List<IStackKey<?>> keys, List<Long> amounts, List<Integer> missing, int requestedBatches) {
            if (missing.isEmpty() && requestedBatches > 1) {
                var consumed = new HashMap<ItemStackKey, Long>();
                var catalysts = new HashMap<ItemStackKey, Long>();
                long batches = requestedBatches;
                for (int i = 0; i < inputs.size(); i++) {
                    Input input = inputs.get(i); var key = (ItemStackKey)keys.get(i);
                    if (input.count() == 0) continue;
                    (input.catalyst() ? catalysts : consumed).merge(key, input.count(), RecipeFillPlan::saturatedAdd);
                    if (!input.catalyst()) batches = Math.min(batches, key.getVanillaMaxStackSize() / input.count());
                }
                for (var entry : consumed.entrySet()) {
                    long available = Math.max(0, totals.get(entry.getKey()) - catalysts.getOrDefault(entry.getKey(), 0L));
                    batches = Math.min(batches, available / entry.getValue());
                }
                for (int i = 0; i < inputs.size(); i++) if (!inputs.get(i).catalyst()) amounts.set(i, inputs.get(i).count() * batches);
            }
            Collections.sort(missing);
            return new RecipeFillPlan(keys, amounts, missing);
        }
    }

    private static long saturatedAdd(long first, long second) { return second > Long.MAX_VALUE - first ? Long.MAX_VALUE : first + second; }
}
