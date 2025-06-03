package org.zlab.ocov.tracker.graph;

import org.junit.jupiter.api.Test;
import org.zlab.ocov.tracker.ObjectGraphCoverage;

import java.io.Serializable;

public class TestSerializationWindow {
    static class Base {
        A a;

        Base() {
            this.a = new A();
        }
    }

    static class A {
        int a = 0;
    }

    static class Base1 implements Serializable {
        A1 a;

        Base1() {
            this.a = new A1();
        }

    }

    static class A1 implements Serializable {
        int a = 0;
    }

    @Test
    public void testSerializationWindowAddWithoutSerializable() {
        SerializationWindow window = new SerializationWindow(
                ObjectGraphCoverage.serializationWindowTsLimitMillis,
                ObjectGraphCoverage.serializationWindowCapacity,
                ObjectGraphCoverage.serializationWindowPeriodMillis,
                ObjectGraphCoverage.periodicUpdate);

        createAndAddObject(window);

        // Check object
        assert window.queue.peek() != null;
        Object obj = window.queue.peek().object;
        if (obj instanceof Base) {
            Base base = (Base) obj;
            assert base.a != null : "Base A should not be null";
        } else {
            throw new AssertionError("Expected object of type Base");
        }
    }

    static void createAndAddObject(SerializationWindow window) {
        Base base = new Base();
        window.add(base);

        SerializationWindow.RecordedObject o = window.queue.peek();
        assert o != null : "Queue should not be empty after adding object";
        assert o.object instanceof Base : "Expected object of type Base in the queue";
        assert o.object == base : "Expected the same object reference to be in the queue";
    }

    @Test
    public void testSerializationWindowAddSerializable() {
        SerializationWindow window = new SerializationWindow(
                ObjectGraphCoverage.serializationWindowTsLimitMillis,
                ObjectGraphCoverage.serializationWindowCapacity,
                ObjectGraphCoverage.serializationWindowPeriodMillis,
                ObjectGraphCoverage.periodicUpdate);

        testSerializationWindowSerializable(window);

        // Check object
        assert window.queue.peek() != null;
        Object obj = window.queue.peek().object;
        if (obj instanceof Base1) {
            Base1 base = (Base1) obj;
            assert base.a != null : "Base A should not be null";
        } else {
            throw new AssertionError("Expected object of type Base");
        }
    }

    static public void testSerializationWindowSerializable(SerializationWindow window) {
        Base1 base = new Base1();
        window.add(base);
        SerializationWindow.RecordedObject o = window.queue.peek();

        assert o != null : "Queue should not be empty after adding object";
        assert o.object instanceof Base1 : "Expected object of type Base1 in the queue";
        assert o.object != base : "Expected the same object reference to be in the queue";
    }

    @Test
    public void testSerializationWindowUpdateRemoval() {
        SerializationWindow window = new SerializationWindow(
                ObjectGraphCoverage.serializationWindowTsLimitMillis,
                ObjectGraphCoverage.serializationWindowCapacity,
                ObjectGraphCoverage.serializationWindowPeriodMillis,
                ObjectGraphCoverage.periodicUpdate);

        Base1 base = new Base1();
        window.add(base);
        SerializationWindow.RecordedObject o = window.queue.peek();

        assert o != null : "Queue should not be empty after adding object";
        assert o.object instanceof Base1 : "Expected object of type Base1 in the queue";
        assert o.object != base : "Expected the same object reference to be in the queue";

        // sleep for 1.5 * ObjectGraphCoverage.serializationWindowPeriodMillis
        try {
            Thread.sleep((long) (1.1 * ObjectGraphCoverage.serializationWindowTsLimitMillis));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Simulate an update
        window.update();

        // Check if the object is still in the queue
        assert window.queue.isEmpty()
                : "Queue should be empty after update and removal of old objects";
    }
}
