package me.bamberghh.nospypackets.util;

import org.jetbrains.annotations.NotNull;

import java.util.*;

public class IndexHashSet<E> implements Set<E>, List<E> {
    private final ArrayList<E> list;
    private final HashMap<E, Integer> keyToIndex;

    public IndexHashSet(int initialCapacity, float loadFactor) {
        list = new ArrayList<>(initialCapacity);
        keyToIndex = new HashMap<>(initialCapacity, loadFactor);
    }

    public IndexHashSet(int initialCapacity) {
        list = new ArrayList<>(initialCapacity);
        keyToIndex = new HashMap<>(initialCapacity);
    }

    public IndexHashSet() {
        keyToIndex = new HashMap<>();
        list = new ArrayList<>();
    }

    public IndexHashSet(Collection<? extends E> collection) {
        keyToIndex = new HashMap<>(collection.size());
        list = new ArrayList<>(collection.size());
        addAll(collection);
    }

    @Override
    public int size() {
        return list.size();
    }

    @Override
    public boolean isEmpty() {
        return list.isEmpty();
    }

    @Override
    public E get(int i) {
        return list.get(i);
    }

    @Override
    public int indexOf(Object o) {
        //noinspection SuspiciousMethodCalls
        Integer i = keyToIndex.get(o);
        if (i == null) {
            return -1;
        }
        return i;
    }

    @Override
    public int lastIndexOf(Object o) {
        return indexOf(o);
    }

    @Override
    public boolean contains(Object o) {
        //noinspection SuspiciousMethodCalls
        return keyToIndex.containsKey(o);
    }

    @Override
    public boolean containsAll(@NotNull Collection<?> collection) {
        return keyToIndex.keySet().containsAll(collection);
    }

    private String outOfBoundsMsg(int index) {
        return "Index: " + index + ", Size: " + size();
    }

    private void rangeCheckForAdd(int index) {
        if (index < 0 || index > size()) {
            throw new IndexOutOfBoundsException(outOfBoundsMsg(index));
        }
    }

    @Override
    public boolean add(E e) {
        if (keyToIndex.putIfAbsent(e, list.size()) != null) {
            return false;
        }
        list.add(e);
        return true;
    }

    @Override
    public void add(int i, E e) {
        rangeCheckForAdd(i);
        for (int shiftI = i; shiftI < list.size(); shiftI++) {
            E shiftE = list.get(shiftI);
            Objects.requireNonNull(keyToIndex.put(shiftE, shiftI+1));
        }
        list.add(i, e);
    }

    @Override
    public boolean addAll(@NotNull Collection<? extends E> collection) {
        boolean modified = false;

        for(E e : collection) {
            if (add(e)) {
                modified = true;
            }
        }

        return modified;
    }

    @Override
    public boolean addAll(int i, @NotNull Collection<? extends E> collection) {
        rangeCheckForAdd(i);

        if (!list.addAll(i, collection)) {
            return false;
        }

        for (int shiftI = i + collection.size(); shiftI < list.size(); shiftI++) {
            keyToIndex.put(list.get(shiftI), shiftI);
        }

        return true;
    }

    @Override
    public boolean remove(Object o) {
        Integer index = keyToIndex.remove(o);
        if (index == null) {
            return false;
        }
        list.remove(index.intValue());
        return true;
    }

    @Override
    public boolean removeAll(@NotNull Collection<?> collection) {
        Objects.requireNonNull(collection);
        boolean modified = false;
        if (size() > collection.size()) {
            for(Object e : collection) {
                modified |= remove(e);
            }
        } else {
            Iterator<?> i = iterator();

            while(i.hasNext()) {
                if (collection.contains(i.next())) {
                    i.remove();
                    modified = true;
                }
            }
        }

        return modified;
    }

    public E removeAt(int i) {
        Objects.checkIndex(i, size());
        for (int shiftI = i+1; shiftI < list.size(); shiftI++) {
            E shiftE = list.get(shiftI);
            Objects.requireNonNull(keyToIndex.put(shiftE, shiftI-1));
        }
        return list.remove(i);
    }

    @Override
    public E remove(int i) {
        return removeAt(i);
    }

    @Override
    public E set(int i, E e) {
        Objects.checkIndex(i, size());
        E old = list.set(i, e);
        keyToIndex.remove(old);
        Integer existingI = keyToIndex.get(e);
        if (existingI != null) {
            removeAt(existingI);
        }
        keyToIndex.put(e, i);
        return old;
    }

    @Override
    public boolean retainAll(@NotNull Collection<?> collection) {
        Objects.requireNonNull(collection);
        boolean modified = false;
        Iterator<?> it = iterator();

        while(it.hasNext()) {
            if (collection.contains(it.next())) {
                it.remove();
                modified = true;
            }
        }

        return modified;
    }

    @Override
    public void clear() {
        keyToIndex.clear();
        list.clear();
    }

    @Override
    public @NotNull Spliterator<E> spliterator() {
        return List.super.spliterator();
    }

    @Override
    public @NotNull Iterator<E> iterator() {
        return list.iterator();
    }

    @Override
    public @NotNull ListIterator<E> listIterator() {
        return list.listIterator();
    }

    @Override
    public @NotNull ListIterator<E> listIterator(int i) {
        return list.listIterator(i);
    }

    @Override
    public Object @NotNull [] toArray() {
        return list.toArray();
    }

    @Override
    public <T1> T1 @NotNull [] toArray(T1 @NotNull [] t1s) {
        return list.toArray(t1s);
    }

    @Override
    public @NotNull List<E> subList(int fromI, int toI) {
        return list.subList(fromI, toI);
    }

    @Override
    public int hashCode() {
        return list.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (other instanceof List) {
            return list.equals(other);
        }
        if (other instanceof Set) {
            return keyToIndex.keySet().equals(other);
        }
        return false;
    }
}
