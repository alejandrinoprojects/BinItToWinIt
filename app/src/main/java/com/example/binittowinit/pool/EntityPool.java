package com.example.binittowinit.pool;

import java.util.ArrayDeque;
import java.util.Queue;

/**
 * High-performance, zero-allocation object pool for game loop entities.
 * <p>
 * Eliminates Dalvik/ART garbage collection pauses during active gameplay by
 * recycling short-lived transient objects (such as {@code WasteItem},
 * {@code FloatingText}, and {@code WindStreak}) instead of allocating new instances
 * on the heap.
 * </p>
 *
 * @param <T> The entity type being pooled.
 */
public class EntityPool<T> {

    /**
     * Factory responsible for instantiating new entity objects when the pool is starved.
     *
     * @param <T> Entity type.
     */
    public interface Factory<T> {
        T create();
    }

    /**
     * Strategy interface for cleaning and resetting an entity's fields upon return to the pool.
     *
     * @param <T> Entity type.
     */
    public interface Resetter<T> {
        void reset(T object);
    }

    private final Queue<T> freeList = new ArrayDeque<>();
    private final Factory<T> factory;
    private final Resetter<T> resetter;
    private int totalCreated = 0;

    /**
     * Constructs an entity pool and pre-allocates an initial supply of instances.
     *
     * @param factory         Factory creating new instances.
     * @param resetter        Cleaner applied when recycling an instance.
     * @param initialCapacity Number of objects to pre-allocate.
     */
    public EntityPool(Factory<T> factory, Resetter<T> resetter, int initialCapacity) {
        if (factory == null) throw new IllegalArgumentException("Factory cannot be null");
        this.factory = factory;
        this.resetter = resetter;

        for (int i = 0; i < initialCapacity; i++) {
            freeList.add(factory.create());
            totalCreated++;
        }
    }

    /**
     * Obtains an available entity from the pool, or allocates a new one if exhausted.
     *
     * @return An entity instance ready for use.
     */
    public synchronized T obtain() {
        if (!freeList.isEmpty()) {
            return freeList.poll();
        }
        totalCreated++;
        return factory.create();
    }

    /**
     * Returns an entity to the pool after running its reset action.
     *
     * @param object The entity to recycle.
     */
    public synchronized void recycle(T object) {
        if (object == null) return;
        if (resetter != null) {
            resetter.reset(object);
        }
        freeList.offer(object);
    }

    /**
     * Returns the count of currently available entities in the free list.
     */
    public synchronized int getFreeCount() {
        return freeList.size();
    }

    /**
     * Returns total instances created by this pool over its lifetime.
     */
    public synchronized int getTotalCreated() {
        return totalCreated;
    }
}
