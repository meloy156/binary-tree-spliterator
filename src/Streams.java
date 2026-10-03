import info.kgeorgiy.java.advanced.streams.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Gatherer;
import java.util.List;
import java.util.Optional;

public class Streams implements EasyStreams {

    @Override
    public <T> Spliterator<T> binaryTreeSpliterator(Trees.Binary<T> tree) {
        return new BinarySpliterator<>(tree);
    }

    private static final class BinarySpliterator<T> implements Spliterator<T> {
        private final Deque<Trees.Binary<T>> stack = new ArrayDeque<>();

        BinarySpliterator(final Trees.Binary<T> root) {
            if (root != null) {
                stack.push(root);
            }
        }

        @Override
        public boolean tryAdvance(final Consumer<? super T> action) {
            while (!stack.isEmpty()) {
                final Trees.Binary<T> node = stack.pop();
                if (node instanceof Trees.Leaf<T>(T value)) {
                    action.accept(value);
                    return true;
                } else if (node instanceof Trees.Binary.Branch<T>(Trees.Binary<T> left, Trees.Binary<T> right)) {
                    stack.push(right);
                    stack.push(left);
                }
            }
            return false;
        }

        @Override
        public Spliterator<T> trySplit() {
            if(stack.isEmpty()) {
                return null;
            }

            final Trees.Binary<T> node = stack.peek();
            if (node instanceof Trees.Binary.Branch<T>(Trees.Binary<T> left, Trees.Binary<T> right)) {
                stack.pop();
                stack.push(right);
                return  new BinarySpliterator<>(left);
            }

            return null;
        }

        @Override
        public long estimateSize() {
            return Long.MAX_VALUE;
        }

        @Override
        public int characteristics() {
            return IMMUTABLE | ORDERED;
        }
    }






    @Override
    public <T> Spliterator<T> sizedBinaryTreeSpliterator(Trees.SizedBinary<T> tree) { return new sizeBinarySpliterator<>(tree); }

    private static final class sizeBinarySpliterator<T> implements Spliterator<T> {

        private final Deque<Trees.SizedBinary<T>> stack = new ArrayDeque<>();
        private long size;

        sizeBinarySpliterator(Trees.SizedBinary<T> tree) {
            if (tree != null) {
                stack.push(tree);
                size = tree.size();
            }
        }


        @Override
        public boolean tryAdvance(Consumer<? super T> action) {
            while (!stack.isEmpty()) {
                final Trees.SizedBinary<T> node = stack.pop();
                if (node instanceof Trees.Leaf<T>(T value)) {
                    action.accept(value);
                    size--;
                    return true;
                } else if (node instanceof Trees.SizedBinary.Branch<T> branch) {
                    stack.push(branch.right());
                    stack.push(branch.left());
                }
            }
            return false;
        }

        @Override
        public Spliterator<T> trySplit() {
            if (stack.isEmpty()) {
                return null;
            }

            final Trees.SizedBinary<T> node = stack.peek();
            if (node instanceof Trees.SizedBinary.Branch<T> branch) {
                stack.pop();
                stack.push(branch.right());
                size -= branch.right().size();
                return new sizeBinarySpliterator<>(branch.left());
            }
            return null;
        }

        @Override
        public long estimateSize() {
            return size;
        }

        @Override
        public int characteristics() {
            return ORDERED | IMMUTABLE | SIZED | SUBSIZED;
        }
    }



    @Override
    public <T> Spliterator<T> naryTreeSpliterator(Trees.Nary<T> tree) {
        return new narySpliterator<>(tree);
    }

    private static final class narySpliterator<T> implements Spliterator<T> {
        private final Deque<Trees.Nary<T>> stack = new ArrayDeque<>();
        private long knownSize = -1;

        narySpliterator(Trees.Nary<T> tree) {
            if (tree != null) {
                stack.push(tree);
                if (tree instanceof Trees.Leaf) knownSize = 1;
            }
        }


        @Override
        public boolean tryAdvance(Consumer<? super T> action) {
            while(!stack.isEmpty()) {
                final Trees.Nary<T> node = stack.pop();
                if (node instanceof Trees.Leaf<T>(T value)) {
                    action.accept(value);
                    return true;
                } else if (node instanceof Trees.Nary.Node<T>(List<Trees.Nary<T>> children)) {
                    for (int i = children.size() - 1; i >= 0; i--) {
                        stack.push(children.get(i));
                    }
                }
            }
            return false;
        }


        @Override
        public Spliterator<T> trySplit() {
            if (stack.isEmpty()) {
                return null;
            }

            final Trees.Nary<T> node = stack.peek();
            if (node instanceof Trees.Nary.Node<T>(List<Trees.Nary<T>> children) && children.size() > 1) {
                stack.pop();
                for (int i = children.size() - 1; i >= children.size()/2; --i) {
                    stack.push(children.get(i));
                }

                return new narySpliterator<>(new Trees.Nary.Node<>(children.subList(0, children.size()/2)));
            }

            return null;
        }


        @Override
        public long estimateSize() {

            return knownSize == -1 ? Long.MAX_VALUE : knownSize;
        }

        @Override
        public int characteristics() {
            int c = ORDERED | IMMUTABLE;
            if (knownSize >= 0) c |= SIZED |SUBSIZED;
            return c;
        }
    }




























    @Override
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

    private static String prefixOfTwo(final String a, final String b) {
        final int max = Math.min(a.length(), b.length());
        int i = 0;
        while (i < max && a.charAt(i) == b.charAt(i)) {
            i++;
        }
        return a.substring(0, i);
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



    private static String suffixOfTwo(final String a, final String b) {
        int i = a.length() - 1;
        int i_b = b.length() - 1;
        while (i >= 0 && i_b >= 0 && a.charAt(i) == b.charAt(i_b)) {
            i--;
            i_b--;
        }
        return a.substring(i + 1);
    }



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
}
