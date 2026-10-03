# Binary Tree Spliterator

A high-performance Java library for parallel traversal of recursive tree structures
via the `Spliterator` API and the standard `Stream` pipeline.

[![Java](https://img.shields.io/badge/Java-22%2B-orange)](https://openjdk.org/)
[![License](https://img.shields.io/badge/license-MIT-blue)](LICENSE)

---

## Overview

`binary-tree-spliterator` provides **O(1) memory** and **O(log n) split** implementations
of `java.util.Spliterator` for three families of tree structures, together with a set of
composable `Collector` and `Gatherer` implementations that integrate directly into
`java.util.stream`.

The library targets workloads where:

- trees are **immutable** and safely traversable concurrently,
- traversal order is **pre-order** and deterministic,
- memory consumption must remain **constant** regardless of tree size.

## Features

### Spliterators

| Spliterator | Characteristics | Split strategy |
|---|---|---|
| `Binary` | `ORDERED \| IMMUTABLE` | Detach right subtree in O(1) |
| `SizedBinary` | `ORDERED \| IMMUTABLE \| SIZED \| SUBSIZED` | Right subtree + O(1) size update |
| `Nary` | `ORDERED \| IMMUTABLE` | Half-split of children list |

### Collectors

| Collector | Description | Memory |
|---|---|---|
| `first()` | First element of a stream | O(1) |
| `last()` | Last element of a stream | O(1) |
| `middle()` | Element at median index | O(n/2) |
| `commonPrefix()` | Longest common prefix of `CharSequence`s | O(k) |
| `commonSuffix()` | Longest common suffix of `CharSequence`s | O(k) |

### Gatherers

| Gatherer | Description | Memory |
|---|---|---|
| `stringPrefixes()` | All non-empty prefixes, shortest → longest | O(k) |
| `nth(n)` | Every n-th element | O(1) |
| `distinctPrefix()` | Prefix up to first duplicate | O(k) |

## Architecture

A `Spliterator` wraps an explicit **LIFO work-stack** of pending subtrees. On `tryAdvance`,
the top frame is popped and either emitted (leaf) or expanded (branch). On `trySplit`,
the rightmost subtree is detached in **constant time** and handed off to a new spliterator —
no buffering, no copying, no precomputation.
