import info.kgeorgiy.java.advanced.streams.*;

import javax.swing.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Gatherer;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

public class Streams implements HardStreams {


    // ═══════════════════════════════════════════
    //  сплитераторы для простых деревьев
    // ═══════════════════════════════════════════
    @Override
    public <T> Spliterator<T> binaryTreeSpliterator(Trees.Binary<T> tree) {
        return new BinarySpliterator<>(tree);
    }

    private static final class BinarySpliterator<T>
            extends TreeSpliterator<Trees.Binary<T>, T> {


        BinarySpliterator(Trees.Binary<T> trees) {
            super(trees, trees instanceof Trees.Leaf<T> ? 1 : Long.MAX_VALUE);
        }


        @Override
        protected boolean tryLeaf(Trees.Binary<T> node, Consumer<? super T> action) {
            if (node instanceof Trees.Leaf<T>(T value)) {
                action.accept(value);
                return true;
            }
            return false;
        }

        @Override
        protected void pushChildren(Trees.Binary<T> node) {
            if (node instanceof Trees.Binary.Branch<T>(Trees.Binary<T> left, Trees.Binary<T> right)) {
                stack.push(right);
                stack.push(left);
            }
        }

        @Override
        protected Spliterator<T> splitTop() {
            final Trees.Binary<T> node = stack.peek();
            if (node instanceof Trees.Leaf<T>) {
                return null;
            }
            if (node instanceof Trees.Binary.Branch<T>( Trees.Binary<T> left, Trees.Binary<T> right)) {
                stack.pop();
                stack.push(right);
                return new BinarySpliterator<>(left);
            }

            return null;
        }
    }


    @Override
    public <T> Spliterator<T> sizedBinaryTreeSpliterator(Trees.SizedBinary<T> tree) { return new sizeBinarySpliterator<>(tree); }

    private static final class sizeBinarySpliterator<T>
            extends TreeSpliterator<Trees.SizedBinary<T>, T> {

        sizeBinarySpliterator(Trees.SizedBinary<T> tree) {
            super(tree, tree == null ? 0 : tree.size());
        }

        @Override
        protected boolean tryLeaf(Trees.SizedBinary<T> node, Consumer<? super T> action) {
            if (node instanceof Trees.Leaf<T>(T value)) {
                action.accept(value);
                return true;
            }
            return false;
        }

        @Override
        protected void pushChildren(Trees.SizedBinary<T> node) {
            if (node instanceof Trees.SizedBinary.Branch<T>(
                    Trees.SizedBinary<T> left, Trees.SizedBinary<T> right, int size1
            )) {
                stack.push(right);
                stack.push(left);
            }
        }

        @Override
        protected Spliterator<T> splitTop() {
            final Trees.SizedBinary<T> node = stack.peek();
            if (node instanceof Trees.Leaf<T>) {
                return null;
            }
            if (node instanceof Trees.SizedBinary.Branch<T> branch) {
                stack.pop();
                stack.push(branch.right());
                size -= branch.left().size();
                return new sizeBinarySpliterator<>(branch.left());
            }

            return null;
        }


    }


    @Override
    public <T> Spliterator<T> naryTreeSpliterator(Trees.Nary<T> tree) {
        return new narySpliterator<>(tree);
    }

    private static final class narySpliterator<T> extends TreeSpliterator<Trees.Nary<T>, T> {

        narySpliterator(Trees.Nary<T> tree) {
            super(tree, tree instanceof Trees.Leaf<T> ? 1 : Long.MAX_VALUE);
        }


        @Override
        protected boolean tryLeaf(Trees.Nary<T> node, Consumer<? super T> action) {
            if (node instanceof Trees.Leaf<T>(T value)) {
                action.accept(value);
                return true;
            }
            return false;
        }

        @Override
        protected void pushChildren(Trees.Nary<T> node) {
            if (node instanceof Trees.Nary.Node<T>(List<Trees.Nary<T>> children)) {
                IntStream.iterate(children.size() - 1, i -> i >= 0, i -> i - 1).mapToObj(children::get).forEach(stack::push);
            }
        }

        @Override
        protected Spliterator<T> splitTop() {
            final Trees.Nary<T> node = stack.peek();
            if (node instanceof Trees.Nary.Node<T>(List<Trees.Nary<T>> children) && children.size() > 1) {
                stack.pop();
                final long mid = children.size()/2;
                for (int i = children.size() - 1; i >= mid; i--) {
                    stack.push(children.get(i));
                }

                return new narySpliterator<>(
                        new Trees.Nary.Node<>(children.subList(0, (int) mid))
                );
            }
            return null;
        }
    }



    // ═══════════════════════════════════════════
    //  Колекторы для Easy
    // ═══════════════════════════════════════════    @Override
    public <T> Collector<T, ?, Optional<T>> first() {
        return Collectors.reducing((a, b) -> a);
    }

    @Override
    public <T> Collector<T, ?, Optional<T>> last() {
        return Collectors.reducing((a, b) -> b);
    }

    @Override
    public <T> Collector<T, ?, Optional<T>> middle() {
        return Collector.<T, ArrayList<T>, Optional<T>>of(
                ArrayList::new,
                List::add,
                (a, b) -> { a.addAll(b); return a; },
                list -> list.isEmpty()
                    ? Optional.empty()
                    : Optional.of(list.get(list.size() / 2))
        );
    }

    @Override
    public Collector<CharSequence, ?, String> commonPrefix() {
        return Collector.<CharSequence, AtomicReference<String>, String>of(
                () -> new AtomicReference<>(null),
                (ref, seq) -> {
                    final String cur = ref.get();
                    ref.set(cur == null ? seq.toString() : prefixOfTwo(cur, seq.toString()));
                },
                (a, b) -> {
                    final String x = a.get();
                    final String y = b.get();
                    if (x == null) { a.set(y); return a; }
                    if (y == null) return a;
                    a.set(prefixOfTwo(x, y));
                    return a;
                },
                ref -> ref.get() == null ? "" : ref.get()
        );

    }

    @Override
    public Collector<CharSequence, ?, String> commonSuffix() {
        return Collector.<CharSequence, AtomicReference<String>, String>of(
                () -> new AtomicReference<>(null),
                (ref, seq) -> {
                    final String cur = ref.get();
                    ref.set(cur == null ? seq.toString() : suffixOfTwo(cur, seq.toString()));
                },
                (a, b) -> {
                    if (a.get() == null) { a.set(b.get()); return a; }
                    if (b.get() == null) { return a; }
                    a.set(suffixOfTwo(a.get(), b.get()));
                    return a;
                },
                ref -> ref.get() == null ? "" : ref.get()
        );
    }



    // ═══════════════════════════════════════════
    //  методы для колекторов Easy
    // ═══════════════════════════════════════════

    private static String suffixOfTwo(final String a, final String b) {
        int i = a.length() - 1;
        int i_b = b.length() - 1;
        while (i >= 0 && i_b >= 0 && a.charAt(i) == b.charAt(i_b)) {
            i--;
            i_b--;
        }
        return a.substring(i + 1);
    }

    private static String prefixOfTwo(final String a, final String b) {
        final int max = Math.min(a.length(), b.length());
        int i = 0;
        while (i < max && a.charAt(i) == b.charAt(i)) {
            i++;
        }
        return a.substring(0, i);
    }



    // ═══════════════════════════════════════════
    // Gatherer для Easy
    // ═══════════════════════════════════════════

    @Override
    public Gatherer<CharSequence, ?, CharSequence> stringPrefixes() {
        return Gatherer.ofSequential(
                (state, element, downstream) -> {
                    final String s = element.toString();
                    for (int i = 1; i <= s.length(); i += Character.charCount((s.codePointAt(i == s.length() ? i-1 : i)))) {
                        if (!downstream.push(s.substring(0, i))) {
                            return false;
                        }
                    }
                    return true;
                }
        );
    }

    @Override
    public <T> Gatherer<T, ?, T> nth(int n) {
        return Gatherer.ofSequential(
                () -> new Object() {
                    int count = 0;
                },
                (state, element, downstream) -> {
                    if(++state.count % n == 0) {
                        downstream.push(element);
                    }
                    return true;
                }
        );
    }

    @Override
    public <T> Gatherer<T, ?, T> distinctPrefix() {
        return Gatherer.ofSequential(
                HashSet::new,
                (state, element, downstream) -> {
                    if (!state.add(element)) {
                        return false;
                    }
                    downstream.push(element);
                    return true;
                }
                );
    }


    // ═══════════════════════════════════════════
    // Абстрактные классы
    // ═══════════════════════════════════════════

    /**
     * Абстрактный класс для всех возможных сплитератор для Easy
     * @param <N> тип дерева
     * @param <T> тип значения листа этого дерева
     */
    private abstract static class TreeSpliterator<N, T> implements Spliterator<T> {
        protected final Deque<N> stack = new ArrayDeque<>();
        protected long size;

        protected TreeSpliterator(final N root, final long size) {
            if ( root != null) stack.push(root);
            this.size = size;
        }


        /**
         * Провека на лист или узел
         * @param node узел
         * @param action - консумер функция
         * @return true - лист - сделать дейсвтие
         *         false - узел
         */
        protected abstract boolean tryLeaf(N node, Consumer<? super T> action);

        /**
         * Делим узел на детей и кидаем в стэк
         * @param node - узел с детьми
         */
        protected abstract void pushChildren(N node);

        /**
         * разделить сплитераторы и выдать новый
         * @return Spliterator<T> - отделенный сплитератор
         */
        protected abstract Spliterator<T> splitTop();


        @Override
        public final boolean tryAdvance(final Consumer<? super T> action) {
            while (!stack.isEmpty()) {
                final N node = stack.pop();
                if(tryLeaf(node, action)) {
                    if (size != Long.MAX_VALUE) size--;
                    return true;
                }
                pushChildren(node);
            }
            return false;
        }

        @Override
        public final Spliterator<T> trySplit() {
            if (stack.isEmpty()) return null;
            return splitTop();
        }

        @Override
        public long estimateSize() {
            return size;
        }

        @Override
        public int characteristics() {
            int c = IMMUTABLE | ORDERED;
            if (size != Long.MAX_VALUE) c |= SIZED | SUBSIZED;
            return c;
        }
    }

    /**
     * Абстрактный класс для всех возможных сплитератор для Hard
     * @param <N> тип дерева
     * @param <T> тип значения листа этого дерева
     */
    private abstract static class NestedTreeSpliterator<N, T> implements Spliterator<T> {
        protected final Deque<N> stack = new ArrayDeque<>();
        protected List<T> currentList = null;
        protected int currentIndex = 0;
        protected long size;

        protected NestedTreeSpliterator(final N root, final long size) {
            if (root != null) stack.push(root);
            this.size = size;
        }

        /**
         * Если лист вернуть массив его детей
         * @param node ветка
         * @return List<T> </T> лист детей
         * null - не лист
         */
        protected abstract List<T> leafList(N node);

        // пушить детей в стэк
        protected abstract void pushChildren(N node);

        // дать новый сплитератор
        protected abstract Spliterator<T> splitTop();


        @Override
        public final boolean tryAdvance(final Consumer<? super T> action) {
            boolean flag = true;
            while (true) {
                if (currentList != null && currentIndex < currentList.size()) {
                    action.accept(currentList.get(currentIndex++));
                    if (size != Long.MAX_VALUE) size--;
                    return true;
                }
                currentList = null;
                if (stack.isEmpty()) return false;

                final N node = stack.pop();
                final List<T> leaf = leafList(node);
                if (leaf != null) {
                    currentList = leaf;
                    currentIndex = 0;
                } else {
                    pushChildren(node);
                }
            }
        }

        @Override
        public final Spliterator<T> trySplit() {
            if (stack.isEmpty() || currentList != null) return  null;
            return splitTop();
        }


        @Override
        public final long estimateSize() { return size; }

        @Override
        public final int characteristics() {
            int c = ORDERED;
            if (size != Long.MAX_VALUE) c |= SIZED | SUBSIZED;
            return c;
        }
    }



    // ═══════════════════════════════════════════
    // Сплитераторы для Hard
    // ═══════════════════════════════════════════

    @Override
    public <T> Spliterator<T> nestedBinaryTreeSpliterator(Trees.Binary<List<T>> var1) {
        return new nestBinarySpliterator<>(var1);
    }

    private static final class nestBinarySpliterator<T>
            extends NestedTreeSpliterator<Trees.Binary<List<T>>, T> {

        nestBinarySpliterator(final Trees.Binary<List<T>> root) {
            super(root, Long.MAX_VALUE);
        }


        @Override
        protected List<T> leafList(Trees.Binary<List<T>> node) {
            return node instanceof Trees.Leaf<List<T>>(List<T> value)  ? value : null;
        }

        @Override
        protected void pushChildren(Trees.Binary<List<T>> node) {
            if (node instanceof Trees.Binary.Branch<List<T>>(Trees.Binary<List<T>> left, Trees.Binary<List<T>> right)) {
                stack.push(right);
                stack.push(left);
            }
        }

        @Override
        protected Spliterator<T> splitTop() {
            if (stack.peek() instanceof Trees.Binary.Branch<List<T>>(Trees.Binary<List<T>> left, Trees.Binary<List<T>> right)) {
                stack.pop();
                stack.push(right);
                return new nestBinarySpliterator<>(left);
            }
            return null;
        }
    }

    @Override
    public <T> Spliterator<T> nestedSizedBinaryTreeSpliterator(Trees.SizedBinary<List<T>> var1) {
        return new nestSizeBinarySpliterator<>(var1);
    }

    private static final class nestSizeBinarySpliterator<T>
            extends NestedTreeSpliterator<Trees.SizedBinary<List<T>>, T> {


        nestSizeBinarySpliterator(Trees.SizedBinary<List<T>> root) {
            super(root, countElements(root));
        }


        private static <T> long countElements(final Trees.SizedBinary<List<T>> node) {
            return switch (node) {
                case Trees.Leaf<List<T>>(List<T> value) -> value.size();
                case Trees.SizedBinary.Branch<List<T>> branch ->
                        countElements(branch.left()) + countElements(branch.right());
                case null -> 0;
            };
        }

        @Override
        protected List<T> leafList(Trees.SizedBinary<List<T>> node) {
            return node instanceof Trees.Leaf<List<T>>(List<T> value) ? value : null;
        }

        @Override
        protected void pushChildren(Trees.SizedBinary<List<T>> node) {
            if (node instanceof Trees.SizedBinary.Branch<List <T>>(Trees.SizedBinary<List<T>> left, Trees.SizedBinary<List<T>> right, int size1)) {
                stack.push(right);
                stack.push(left);
            }
        }

        @Override
        protected Spliterator<T> splitTop() {
            if (stack.peek() instanceof Trees.SizedBinary.Branch<List<T>>(Trees.SizedBinary<List<T>> left, Trees.SizedBinary<List<T>> right, int size1)) {
                stack.pop();
                stack.push(right);
                size -= countElements(left);
                return new nestSizeBinarySpliterator<>(left);
            }
            return null;
        }
    }

    @Override
    public <T> Spliterator<T> nestedNaryTreeSpliterator(Trees.Nary<List <T>> var1) {
        return new NestedNarySpliterator<>(var1);
    }

    private static final class NestedNarySpliterator<T>
            extends NestedTreeSpliterator<Trees.Nary<List<T>>, T> {

        NestedNarySpliterator(final Trees.Nary<List<T>> root) {
            super(root, root instanceof Trees.Leaf<List<T>>(List<T> value) ? value.size() : Long.MAX_VALUE);
        }

        @Override
        protected List<T> leafList(final Trees.Nary<List<T>> node) {
            return node instanceof Trees.Leaf<List<T>>(List<T> value) ? value : null;
        }

        @Override
        protected void pushChildren(final Trees.Nary<List<T>> node) {
            if (node instanceof Trees.Nary.Node<List<T>>(List<Trees.Nary<List<T>>> children)) {
                for (int i = children.size() - 1; i >= 0; i--) {
                    stack.push(children.get(i));
                }
            }
        }

        @Override
        protected Spliterator<T> splitTop() {
            final Trees.Nary<List<T>> node = stack.peek();
            if (node instanceof Trees.Nary.Node<List<T>>(List<Trees.Nary<List<T>>> children) && children.size() > 1) {
                final int mid = children.size() / 2;

                stack.pop();
                for (int i = children.size() - 1; i >= mid; i--) {
                    stack.push(children.get(i));
                }
                return new NestedNarySpliterator<>(
                        new Trees.Nary.Node<>(children.subList(0, mid))
                );
            }
            return null;
        }
    }


    @Override
    public <T> Collector<T, ?, List<T>> head(int var1) {
        return Collector.of(
                ArrayList::new,
                (list, item) -> {
                    if (list.size() < var1) {
                        list.add(item);
                    }
                },
                (a, b) -> {
                    for (T item : b) {
                        if (a.size() < var1) {
                            a.add(item);
                        } else break;
                    }
                    return a;
                },
                Collector.Characteristics.IDENTITY_FINISH
        );
    }


    @Override
    public <T> Collector<T, ?, List<T>> tail(int k) {
        return Collector.<T, ArrayDeque<T>, List<T>>of(
                ArrayDeque::new,
                (deque, item) -> {
                    if (k <= 0) return;
                    if (deque.size() == k) {
                        deque.pollFirst();
                    }
                    deque.addLast(item);
                },
                (a, b) -> {
                    for(T item : b) {
                        if (a.size() == k) {
                            a.pollFirst();
                        }
                        a.addLast(item);
                    }
                    return a;
                },
                ArrayList::new
        );
    }


    private static final class kthBox<T> {
        int count = 0;
        T value;
        boolean found;
    }

    @Override
    public <T> Collector<T, ?, Optional<T>> kth(int k) {
        return Collector.<T, kthBox<T>, Optional<T>>of(
                kthBox::new,
                (box, item) -> {
                    if (box.count == k) {
                        box.value = item;
                        box.found = true;
                    }
                    box.count++;
                },
                (a, b) -> {
                    if (a.found) return a;
                    if (b.found) return b;
                    return a;
                },
                box -> box.found ? Optional.of(box.value) : Optional.empty()
        );
    }


    @Override
    public Gatherer<CharSequence, ?, CharSequence> stringSuffixes() {
        return Gatherer.ofSequential(
                (state, element, downstream) -> {
                    final String s = element.toString();
                    for (int i = s.length() - 1; i >= 0; i -= Character.charCount(s.codePointAt(i == s.length() ? i-1 : i))) {
                        if (!downstream.push(s.substring(i)))
                            return false;
                    }
                    return true;
                }
        );
    }

    @Override
    public <T> Gatherer<T, ?, T> ithOfN(final int i, final int n) {
        if (i < 0 || i >= n) {
            throw new IllegalArgumentException("0 <= i < n required");
        }
        return Gatherer.ofSequential(
                () -> new int[]{0},
                (state, element, downstream) -> {
                    if (state[0] == i) {
                        downstream.push(element);
                    }
                    state[0] = (state[0] + 1) % n;
                    return true;
                }
        );
    }


    @Override
    public <T, K> Gatherer<T, ?, T> distinctPrefixBy(Function<? super T, K> function) {
        return Gatherer.ofSequential(
                HashSet::new,
                (state, element, downstream) -> {
                    final K key = function.apply(element);
                    if (!state.add(key)) {
                        return false;
                    }
                    downstream.push(element);
                    return true;
                }
        );
    }
}
