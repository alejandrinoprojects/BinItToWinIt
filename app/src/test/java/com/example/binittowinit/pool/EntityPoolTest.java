package com.example.binittowinit.pool;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

public class EntityPoolTest {

    private static class TestEntity {
        int id;
        boolean isClean = true;

        TestEntity(int id) {
            this.id = id;
        }

        void reset() {
            isClean = true;
        }
    }

    private AtomicInteger createdCount;
    private AtomicInteger resetCount;
    private EntityPool<TestEntity> pool;

    @Before
    public void setUp() {
        createdCount = new AtomicInteger(0);
        resetCount = new AtomicInteger(0);

        pool = new EntityPool<>(
                () -> new TestEntity(createdCount.incrementAndGet()),
                entity -> {
                    entity.reset();
                    resetCount.incrementAndGet();
                },
                5
        );
    }

    @Test
    public void testInitialPreAllocation() {
        assertEquals(5, pool.getFreeCount());
        assertEquals(5, createdCount.get());
    }

    @Test
    public void testObtainConsumesFromPool() {
        TestEntity item1 = pool.obtain();
        assertNotNull(item1);
        assertEquals(4, pool.getFreeCount());
        assertEquals(5, createdCount.get()); // No extra allocations
    }

    @Test
    public void testRecycleReturnsToPoolAndCallsResetter() {
        TestEntity item = pool.obtain();
        item.isClean = false;
        assertEquals(4, pool.getFreeCount());

        pool.recycle(item);
        assertEquals(5, pool.getFreeCount());
        assertEquals(1, resetCount.get());
        assertEquals(true, item.isClean);

        // Next obtain cycles through existing items before reaching the recycled item at the tail of the FIFO queue
        for (int i = 0; i < 4; i++) {
            pool.obtain();
        }
        TestEntity reobtained = pool.obtain();
        assertSame(item, reobtained);
    }

    @Test
    public void testPoolGrowthWhenExhausted() {
        for (int i = 0; i < 5; i++) {
            pool.obtain();
        }
        assertEquals(0, pool.getFreeCount());
        assertEquals(5, createdCount.get());

        // 6th item triggers factory creation
        TestEntity overflowItem = pool.obtain();
        assertNotNull(overflowItem);
        assertEquals(6, createdCount.get());
        assertEquals(0, pool.getFreeCount());

        // Recycle it back
        pool.recycle(overflowItem);
        assertEquals(1, pool.getFreeCount());
    }
}
