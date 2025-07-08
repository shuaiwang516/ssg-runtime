package org.zlab.ocov.tracker.graph;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ScheduledExecutorService;

import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

// Disabled for now...
public class SerializationWindow {

    private final long tsLimit;
    private final int capacity;

    /* ------------ new fields ------------ */
    private final ScheduledExecutorService scheduler = Executors
            .newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "serialization-window-cleaner");
                t.setDaemon(true); // won’t block JVM exit
                return t;
            });
    private final ReentrantLock lock = new ReentrantLock(); // thread-safety

    // Create a queue of object so that we can infer likely invariants between them
    Queue<RecordedObject> queue = new LinkedList<>();

    public SerializationWindow(long tsLimit, int capacity, long periodMillis,
            boolean periodicUpdate) {
        this.tsLimit = tsLimit;
        this.capacity = capacity;
        // run update() every periodMillis; first run starts after same delay

        if (periodicUpdate)
            scheduler.scheduleAtFixedRate(this::update, periodMillis, periodMillis,
                    TimeUnit.MILLISECONDS);
    }

    public void update() {
        long now = System.currentTimeMillis();
        lock.lock();
        try {
            while (!queue.isEmpty() && now - queue.peek().timestamp > tsLimit) {
                queue.poll();
            }
        } finally {
            lock.unlock();
        }
    }

    public void add(Object obj) {
        lock.lock();
        try {
            if (queue.size() >= capacity)
                queue.poll();
            RecordedObject rec = new RecordedObject();
            rec.timestamp = System.currentTimeMillis();
            rec.object = cloneBestEffort(obj);
            queue.add(rec);
        } finally {
            lock.unlock();
        }
    }

    private Object cloneBestEffort(Object obj) {
        if (obj instanceof Serializable)
            return org.apache.commons.lang3.SerializationUtils.clone((Serializable) obj);
        try {
            Method m = obj.getClass().getMethod("clone");
            m.setAccessible(true);
            return m.invoke(obj);
        } catch (Exception e) {
            return obj; // fallback: shallow copy (reference)
        }
    }

    public Queue<RecordedObject> getQueue() {
        return queue;
    }

    public void clear() {
        lock.lock();
        try {
            queue.clear();
        } finally {
            lock.unlock();
        }
    }

    public static class RecordedObject {
        public long timestamp;
        public Object object;
    }
}
