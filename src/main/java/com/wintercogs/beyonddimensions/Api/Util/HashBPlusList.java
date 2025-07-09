package com.wintercogs.beyonddimensions.Api.Util;

import java.io.Serializable;
import java.util.AbstractList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.RandomAccess;

//以B+树为存储结构，以HashMap作为辅助，实现高速
public class HashBPlusList<E> extends AbstractList<E>
        implements RandomAccess, Serializable {

    private final int LEAF_CAPACITY;    // 每片叶子最大元素数量
    private final int BRANCH_FACTOR;    // 每个分支最大子节点数量
    private final int MIN_LEAF_OCCUPANCY;
    private final int MIN_BRANCH_CHILDREN;

    private interface Node<E> {
        boolean isLeaf();
        int size();                       // 子树的元素总量（对于叶子-元素数量；对于分支是最后一个子节点的前缀和(即整棵子树大小)）
        BranchNode<E> parent();
        void setParent(BranchNode<E> parent);
    }

    // 叶节点
    private static final class LeafNode<E> implements Node<E> {
        E[] elements;
        int count;
        LeafNode<E> next, prev;
        BranchNode<E> parent;
        @SuppressWarnings("unchecked")
        LeafNode(int leafCapacity)
        {
            elements = (E[]) new Object[leafCapacity];
        }
        public boolean isLeaf() { return true; }
        public int size()       { return count; }
        public BranchNode<E> parent()       { return parent; }
        public void setParent(BranchNode<E> p) { parent = p; }
    }

    // 分支节点
    private static final class BranchNode<E> implements Node<E> {
        Node<E>[] children;
        int[] subSizes;
        int childCount;
        BranchNode<E> parent;
        @SuppressWarnings("unchecked")
        BranchNode(int branchFactor)
        {
            // 为方便分裂操作，额外分配一个槽位
            children = (Node<E>[]) new Node[branchFactor + 1];
            subSizes = new int[branchFactor + 1];
        }
        public boolean isLeaf() { return false; }
        public int size() {
            // 分支节点的大小等于最后一个子节点的前缀和
            return childCount == 0 ? 0 : subSizes[childCount - 1];
        }
        public BranchNode<E> parent()       { return parent; }
        public void setParent(BranchNode<E> p) { parent = p; }
    }

    /** 存储元素在树中的位置(叶节点+偏移量) */
    private static final class Pos<E> {
        LeafNode<E> leaf;
        int offset;
        Pos(LeafNode<E> leaf, int offset) { this.leaf = leaf; this.offset = offset; }
    }

    // 树的根节点
    private Node<E> root;
    // 指向最左侧的叶子，便于迭代
    private LeafNode<E> firstLeaf;
    // 元素到所在位置的映射，实现按元素的O(1)查找
    private final HashMap<E, Pos<E>> index;
    // 全树元素总数
    private int size;

    public HashBPlusList(int leafCapacity, int branchFactor) {
        this.LEAF_CAPACITY = leafCapacity;
        this.BRANCH_FACTOR = branchFactor;
        this.MIN_LEAF_OCCUPANCY = LEAF_CAPACITY/2;
        this.MIN_BRANCH_CHILDREN  = Math.max(2, (BRANCH_FACTOR + 1) / 2);
        this.root = new LeafNode<>(LEAF_CAPACITY);
        this.firstLeaf = (LeafNode<E>) root;
        this.index = new HashMap<>();
        this.size = 0;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public E get(int idx) {
        rangeCheck(idx);
        // 从全局索引查找叶子和偏移量
        IntRef offsetRef = new IntRef();
        LeafNode<E> leaf = findLeaf(idx, offsetRef);
        return leaf.elements[offsetRef.value];
    }

    @Override
    public int indexOf(Object o) {
        Pos<E> pos = index.get(o);
        if (pos == null) return -1;
        // 计算全局索引
        LeafNode<E> leaf = pos.leaf;
        int offset = pos.offset;
        return prefixUpToLeaf(leaf) + offset;
    }

    @Override
    public boolean contains(Object o) {
        return index.containsKey(o);
    }

    /** 按元素寻找内部元素 */
    public E get(E key) {
        Pos<E> pos = index.get(key);
        if (pos == null) return null;
        return pos.leaf.elements[pos.offset];
    }

    @Override
    public E set(int idx, E element) {
        rangeCheck(idx);

        E old = get(idx);
        // 只有当引用相等的时候完全不做任何操作，否则将抛出错误
        if (old == element) {
            return old;
        }
        // 禁止插入相同元素
        if (index.containsKey(element)) {
            throw new IllegalArgumentException("尝试向列表中插入相同的元素: " + element);
        }
        // 运行替换
        IntRef offsetRef = new IntRef();
        LeafNode<E> leaf = findLeaf(idx, offsetRef);
        int offset = offsetRef.value;
        // 更新hashMap
        index.remove(old);
        leaf.elements[offset] = element;
        index.put(element, new Pos<>(leaf, offset));
        return old;
    }

    @Override
    public boolean add(E e) {
        add(size, e);
        return true;
    }

    @Override
    public void add(int idx, E element) {
        if (index.containsKey(element)) {
            throw new IllegalArgumentException("尝试向列表中插入相同的元素: " + element);
        }
        if (idx < 0 || idx > size) {
            throw new IndexOutOfBoundsException("Index: " + idx);
        }
        // 找到插入点
        IntRef offsetRef = new IntRef();
        LeafNode<E> leaf = findLeaf(idx, offsetRef);

        // 如果目标叶子满，需要先拆分
        if (leaf.count == LEAF_CAPACITY) {
            splitLeaf(leaf);          // 拆分叶子
            // 重新定位
            leaf = findLeaf(idx, offsetRef);
        }

        // 插入元素到叶子
        insertIntoLeaf(leaf, offsetRef.value, element);
        size++;
        modCount++;
    }

    @Override
    public E remove(int idx) {
        rangeCheck(idx);
        IntRef offsetRef = new IntRef();
        LeafNode<E> leaf = findLeaf(idx, offsetRef);
        int offset = offsetRef.value;
        E removedElement = leaf.elements[offset];
        removeFromLeaf(leaf, offset);
        size--;
        modCount++;
        return removedElement;
    }

    @Override
    public boolean remove(Object o) {
        Pos<E> pos = index.get(o);
        if (pos == null) return false;
        LeafNode<E> leaf = pos.leaf;
        int offset = pos.offset;
        // 因为我们拥有实际位置，所以我们可以直接移除元素，而不需要搜索
        removeFromLeaf(leaf, offset);
        size--;
        modCount++;
        return true;
    }

    // ------------ 内部辅助方法 ------------

    /** 简单的int持有者，用于保存偏移量的引用 */
    private static class IntRef { int value; }

    /** 定位包含给定全局索引的叶子节点，并返回该叶子节点及其在节点内的本地偏移量 */
    private LeafNode<E> findLeaf(int globalIndex, IntRef offsetOut) {
        Node<E> node = root;
        int pos = globalIndex;
        // 向下遍历至叶子节点
        while (!node.isLeaf()) {
            BranchNode<E> branch = (BranchNode<E>) node;
            int i;
            if (pos == branch.size()) {
                // 如果 pos 正好等于该子树的总元素数，则转到最右侧的子节点
                i = branch.childCount - 1;
            } else {
                // 二分查找以定位包含位置 pos 的子节点
                int key = pos + 1;  // +1 因为 subSizes 是累积计数（基于1的索引）
                int bsResult = Arrays.binarySearch(branch.subSizes, 0, branch.childCount, key);
                i = (bsResult >= 0 ? bsResult : -bsResult - 1);
                if (i >= branch.childCount) {
                    // 如果插入位置在末尾，则使用最后一个子节点
                    i = branch.childCount - 1;
                }
            }
            // 减去所有先前子节点的计数，使 pos 相对于子节点 i 本地化
            if (i > 0) {
                pos -= branch.subSizes[i - 1];
            }
            node = branch.children[i];
        }
        // 现在新的节点是目标叶子
        LeafNode<E> leaf = (LeafNode<E>) node;
        offsetOut.value = pos;
        return leaf;
    }

    /** 计算给定叶子节点之前整个列表中的元素数量 */
    // 保证subSizes数组的正确性，这是按索引操作的重要前提
    private int prefixUpToLeaf(LeafNode<E> leaf) {
        int sum = 0;
        Node<E> node = leaf;
        while (node.parent() != null) {
            BranchNode<E> parent = node.parent();
            int childIndex = findChildIndex(parent, node);
            if (childIndex > 0) {
                sum += parent.subSizes[childIndex - 1];
            }
            node = parent;
        }
        return sum;
    }

    /** 查找分支节点 parent 中子节点 child 的索引 */
    private int findChildIndex(BranchNode<E> parent, Node<E> child) {
        // We only search up to parent.childCount
        for (int i = 0; i < parent.childCount; i++) {
            if (parent.children[i] == child) {
                return i;
            }
        }
        throw new IllegalStateException("子节点在父节点中未找到");
    }

    /** 在给定的偏移位置向叶子节点插入一个新元素。处理元素移位和索引映射的更新 */
    private void insertIntoLeaf(LeafNode<E> leaf, int offset, E element) {
        int toMove = leaf.count - offset;
        if (toMove > 0) {
            // ① 先整体右移
            System.arraycopy(leaf.elements, offset, leaf.elements, offset + 1, toMove);

            // ② 先扩容，再“倒着”更新偏移，保证不会被覆盖
            leaf.count++;
            for (int j = leaf.count - 1; j >= offset + 1; j--) {
                E shifted = leaf.elements[j];
                if (shifted != null) index.get(shifted).offset = j;
            }
        } else {
            leaf.count++;   // 追加到尾部的简单情形
        }

        // ③ 放入新元素并写入索引
        leaf.elements[offset] = element;
        index.put(element, new Pos<>(leaf, offset));

        // ④ 向上递增 size
        updateSizesUpward(leaf, +1);
    }

    /** 从叶节点中移除给定偏移量的元素。处理移位和索引映射更新 */
    private void removeFromLeaf(LeafNode<E> leaf, int offset) {
        E removedElem = leaf.elements[offset];
        // 从映射中移除
        index.remove(removedElem);
        // 左翼元素以填充空间
        if (offset < leaf.count - 1) {
            System.arraycopy(leaf.elements, offset + 1, leaf.elements, offset, leaf.count - offset - 1);
            // 更新被移动元素的偏移量
            for (int j = offset; j < leaf.count - 1; j++) {
                E shiftedElem = leaf.elements[j];
                index.get(shiftedElem).offset = j;
            }
        }
        // 清除叶节点中现在未使用的最后一个槽位
        leaf.elements[leaf.count - 1] = null;
        leaf.count--;
        // 减少祖先分支节点的计数
        updateSizesUpward(leaf, -1); // ← 已把“少 1”反映到所有祖先

        // 检查是否欠满并尝试再平衡
        rebalanceAfterDeleteLeaf(leaf);
    }

    /** 将给定节点的大小变化（增量 delta = +1 或 -1）向上传播至根节点，同时更新前缀和。 */
    private void updateSizesUpward(Node<E> node, int delta) {
        BranchNode<E> parent = node.parent();
        Node<E> child = node;
        while (parent != null) {
            int childIndex = findChildIndex(parent, child);
            // 将该子节点及其右侧所有子节点的 subSize 增加 delta
            for (int i = childIndex; i < parent.childCount; i++) {
                parent.subSizes[i] += delta;
            }
            // 向上一级移动
            child = parent;
            parent = parent.parent();
        }
    }

    /** 将一个满的叶节点拆分成两个叶节点，并将新叶节点插入到父分支中。 */
    private void splitLeaf(LeafNode<E> leaf) {
        int total = leaf.count;
        int mid = total / 2;  // 大致对半拆分（向下取整）
        LeafNode<E> rightLeaf = new LeafNode<>(LEAF_CAPACITY);
        // 将后半部分元素移动到右侧叶节点
        int rightCount = total - mid;
        System.arraycopy(leaf.elements, mid, rightLeaf.elements, 0, rightCount);
        leaf.count = mid;
        rightLeaf.count = rightCount;
        // 从左叶节点的数组中清除已移动的元素（可选，用于垃圾回收）
        Arrays.fill(leaf.elements, mid, mid + rightCount, null);
        // 更新链表指针
        rightLeaf.next = leaf.next;
        if (rightLeaf.next != null) {
            rightLeaf.next.prev = rightLeaf;
        }
        leaf.next = rightLeaf;
        rightLeaf.prev = leaf;
        // 更新移动到新叶节点的元素在HashMap中的位置
        for (int j = 0; j < rightCount; j++) {
            E elem = rightLeaf.elements[j];
            index.get(elem).leaf = rightLeaf;
            index.get(elem).offset = j;
        }
        // 将新叶子节点插入父分支
        BranchNode<E> parent = leaf.parent();
        if (parent == null) {
            // 如果分裂根叶子节点则创建新的根分支
            parent = new BranchNode<>(BRANCH_FACTOR);
            root = parent;
            parent.children[0] = leaf;
            parent.childCount = 1;
            leaf.setParent(parent);
            firstLeaf = leaf;  // 第一个叶子节点保持不变（位于最左侧）
        }
        // 将SubSizes计算工作留到insertChild中整个重建
        // 仅一次遍历且只有加法计算，n不超过256，开销极低
        // 再者，如果在这里计算一次SubSizes，insertChild中再计算一次，说不准哪一个的更低
        insertChild(parent, rightLeaf, leaf);
    }

    /** 将新子节点（叶子或分支）插入到父分支节点中，紧接在leftSibling子节点之后。 */
    private void insertChild(BranchNode<E> parent, Node<E> newChild, Node<E> leftSibling) {
        int insertIndex = findChildIndex(parent, leftSibling) + 1;
        // 从插入位置开始，将现有子节点及其子树大小向右移动
        if (insertIndex < parent.childCount) {
            System.arraycopy(parent.children, insertIndex, parent.children, insertIndex + 1, parent.childCount - insertIndex);
        }
        // Insert new child
        parent.children[insertIndex] = newChild;
        newChild.setParent(parent);
        parent.childCount++;
        rebuildSubSizes(parent);

        // 如果父节点过大，则分割分支
        if (parent.childCount > BRANCH_FACTOR) {
            splitBranch(parent);
        }
    }

    private void removeChild(BranchNode<E> parent, int idx) {
        // 左移数组，覆盖 idx 位置
        int move = parent.childCount - idx - 1;
        if (move > 0) {
            System.arraycopy(parent.children, idx + 1, parent.children, idx, move);
        }
        // 清尾
        parent.children[parent.childCount - 1] = null;
        parent.childCount--;

        rebuildSubSizes(parent);               // 重新计算前缀

        rebalanceAfterDeleteBranch(parent);
    }


    /** 将一个过满的分支节点拆分成两个，并将新分支插入其父节点中 */
    private void splitBranch(BranchNode<E> branch) {
        int totalChildren = branch.childCount;
        int mid = totalChildren / 2;
        BranchNode<E> rightBranch = new BranchNode<>(BRANCH_FACTOR);
        int rightChildCount = totalChildren - mid;
        // 将一半的子节点移动到右分支
        System.arraycopy(branch.children, mid, rightBranch.children, 0, rightChildCount);
        // 修复移动子节点的父指针
        for (int j = 0; j < rightChildCount; j++) {
            Node<E> child = rightBranch.children[j];
            child.setParent(rightBranch);
        }
        // 调整子节点计数
        branch.childCount = mid;
        rightBranch.childCount = rightChildCount;
        // 重新计算两个分支的前缀和
        rebuildSubSizes(branch);
        rebuildSubSizes(rightBranch);
        // 将新分支插入到branch的父节点中
        BranchNode<E> parent = branch.parent();
        if (parent == null) {
            // 如果分裂根分支，则创建新的根节点
            parent = new BranchNode<>(BRANCH_FACTOR);
            root = parent;
            parent.children[0] = branch;
            parent.childCount = 1;
            branch.setParent(parent);
        }
        insertChild(parent, rightBranch, branch);
    }

    /** 重新计算分支节点的前缀和数组（subSizes） */
    private void rebuildSubSizes(BranchNode<E> branch) {
        int cumulative = 0;
        for (int i = 0; i < branch.childCount; i++) {
            cumulative += branch.children[i].size();
            branch.subSizes[i] = cumulative;
        }
    }

    private void rangeCheck(int idx) {
        if (idx < 0 || idx >= size) throw new IndexOutOfBoundsException("Index: " + idx);
    }

    // ------------------- 再平衡：叶节点 -------------------
    private void rebalanceAfterDeleteLeaf(LeafNode<E> leaf) {
        if (leaf.count >= MIN_LEAF_OCCUPANCY || leaf.parent() == null) return; // 根叶或够半满
        BranchNode<E> par = leaf.parent(); int idx = findChildIndex(par, leaf);
        LeafNode<E> left  = idx > 0 ? (LeafNode<E>) par.children[idx - 1] : null;
        LeafNode<E> right = idx < par.childCount - 1 ? (LeafNode<E>) par.children[idx + 1] : null;

        // 1) 尝试向左借
        if (left != null && left.count > MIN_LEAF_OCCUPANCY) {
            borrowFromLeftLeaf(left, leaf);
            return;
        }
        // 2) 向右借
        if (right != null && right.count > MIN_LEAF_OCCUPANCY) {
            borrowFromRightLeaf(leaf, right);
            return;
        }
        // 3) 合并（优先与左合并）
        if (left != null) {
            mergeLeaves(left, leaf); // leaf 被清空，由左侧留存
        } else if (right != null) {
            mergeLeaves(leaf, right); // right 被清空
        }
    }

    private void borrowFromLeftLeaf(LeafNode<E> left, LeafNode<E> underflow) {
        // 将左兄最后一个元素移到 underflow 的最前
        E moved = left.elements[--left.count]; left.elements[left.count] = null;
        insertIntoLeaf(underflow, 0, moved);          // 插入会维护 index 和 sizes
        updateSizesUpward(left, -1);                  // 先前 insertIntoLeaf 已 +1 给 underflow
    }

    private void borrowFromRightLeaf(LeafNode<E> underflow, LeafNode<E> right) {
        E moved = right.elements[0]; removeFromLeaf(right, 0);   // remove 会 -1
        insertIntoLeaf(underflow, underflow.count, moved);       // 在尾部追加；内部 +1
    }

    /**
     * 合并两个叶片，调用函数时，需要保证两个叶片属于同一个父分支
     */
    private void mergeLeaves(LeafNode<E> left, LeafNode<E> right) {
        // 把 right 整个拼接到 left 后
        System.arraycopy(right.elements, 0, left.elements, left.count, right.count);
        for (int j = 0; j < right.count; j++) {
            E e = right.elements[j]; Pos<E> p = index.get(e); p.leaf = left; p.offset = left.count + j;
        }
        left.count += right.count;
        left.next = right.next; if (right.next != null) right.next.prev = left;

        BranchNode<E> par = right.parent();
        int idx = findChildIndex(par, right);
        // 不需要updateSizesUpward向上传递变化，因为实际上，父分支元素总数不变
        // removeChild本身也会调用函数重算父节点Subsize，递归也会继续再平衡分支
        removeChild(par, idx);
    }

    // ------------------- 再平衡：分支节点 -------------------
    private void rebalanceAfterDeleteBranch(BranchNode<E> branch) {
        // 根节点特殊处理：若只有 1 个孩子，直接下拉；若空则树为空
        if (branch == root) {
            if (branch.childCount == 1) {
                root = branch.children[0]; root.setParent(null);
            }
            return;
        }
        if (branch.childCount >= MIN_BRANCH_CHILDREN) return; // 半满
        BranchNode<E> par = branch.parent(); int idx = findChildIndex(par, branch);
        BranchNode<E> left  = idx > 0 ? (BranchNode<E>) par.children[idx - 1] : null;
        BranchNode<E> right = idx < par.childCount - 1 ? (BranchNode<E>) par.children[idx + 1] : null;

        // 1) 向左借
        if (left != null && left.childCount > MIN_BRANCH_CHILDREN) {
            borrowFromLeftBranch(left, branch);
            return;
        }
        // 2) 向右借
        if (right != null && right.childCount > MIN_BRANCH_CHILDREN) {
            borrowFromRightBranch(branch, right);
            return;
        }
        // 3) 合并
        if (left != null) {
            mergeBranches(left, branch);
        } else if (right != null) {
            mergeBranches(branch, right);
        }
    }

    private void borrowFromLeftBranch(BranchNode<E> left, BranchNode<E> underflow) {
        // 将左兄最后一个子节点移动到 underflow 首位
        Node<E> child = left.children[--left.childCount]; left.children[left.childCount] = null;
        rebuildSubSizes(left);
        // 插入到 underflow 最前
        if (underflow.childCount > 0)
            System.arraycopy(underflow.children, 0, underflow.children, 1, underflow.childCount);
        underflow.children[0] = child; child.setParent(underflow); underflow.childCount++;
        rebuildSubSizes(underflow);
        updateSizesUpward(left, -child.size());
        updateSizesUpward(underflow, +child.size());
    }

    private void borrowFromRightBranch(BranchNode<E> underflow, BranchNode<E> right) {
        Node<E> child = right.children[0];
        // 删除 right 第 0 个
        System.arraycopy(right.children, 1, right.children, 0, right.childCount - 1);
        right.children[--right.childCount] = null; rebuildSubSizes(right);
        // 追加到 underflow 末尾
        underflow.children[underflow.childCount++] = child; child.setParent(underflow);
        rebuildSubSizes(underflow);
        updateSizesUpward(right, -child.size());
        updateSizesUpward(underflow, +child.size());
    }

    private void mergeBranches(BranchNode<E> left, BranchNode<E> right) {
        System.arraycopy(right.children, 0, left.children, left.childCount, right.childCount);
        for (int j = 0; j < right.childCount; j++) right.children[j].setParent(left);
        left.childCount += right.childCount;
        rebuildSubSizes(left);
        BranchNode<E> par = right.parent(); int idx = findChildIndex(par, right);
        removeChild(par, idx);           // 进一步向上再平衡
    }
}
