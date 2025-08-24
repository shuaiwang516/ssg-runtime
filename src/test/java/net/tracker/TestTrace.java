package net.tracker;

import org.junit.jupiter.api.Test;
import org.zlab.net.tracker.ObjectGraphTraverser;
import org.zlab.net.tracker.Runtime;
import org.zlab.net.tracker.Trace;

import static java.lang.Thread.sleep;

public class TestTrace {
    @Test
    public void TestMergeBasedOnTimestamp() throws InterruptedException {
        Trace trace0 = new Trace();
        Trace trace1 = new Trace();

        trace0.record("sendRR_0", 1);
        sleep(10);
        trace1.record("sendRR_1", 1);
        sleep(10);
        trace0.record("sendRR_0", 2);
        sleep(10);
        trace0.record("sendAll_0", 3);
        sleep(10);
        trace1.record("sendAll_1", 2);
        sleep(10);
        trace1.record("sendRR_1", 3);
        sleep(10);

        Trace mergedTrace0 = Trace.mergeBasedOnTimestamp(trace0, trace1);

        assert mergedTrace0.size() == 6;
        assert mergedTrace0.getTraceEntries().get(0).methodName.equals("sendRR_0");
        assert mergedTrace0.getTraceEntries().get(1).methodName.equals("sendRR_1");
    }

    public static class Message<T> {
        public final T payload;

        public Message(T payload) {
            this.payload = payload;
        }
    }

    public static class MessageIn extends Message<Mutation> {
        public MessageIn(Mutation payload) {
            super(payload);
        }
    }

    public static class Mutation {
        int id;
        public Mutation(int id) {
            this.id = id;
        }
    }

    @Test
    public void testTraversePayload1() {
        Message<Mutation> msg = new Message<>(new Mutation(1));
        ObjectGraphTraverser traverser = new ObjectGraphTraverser();
        traverser.traverse(msg);
        assert traverser.payloadType.equals("net.tracker.TestTrace$Mutation");

    }

    @Test
    public void testTraversePayload2() {
        MessageIn msg = new MessageIn(new Mutation(1));
        ObjectGraphTraverser traverser = new ObjectGraphTraverser();
        traverser.traverse(msg);
        assert traverser.payloadType.equals("net.tracker.TestTrace$Mutation");
    }

    @Test
    public void testExecPath() throws InterruptedException {
        /**
         *
         */

        // Start up a new thread to invoke foo() method
        Thread thread = new Thread(() -> {
            TestTrace testTrace = new TestTrace();
            testTrace.foo();
        });
        thread.start();
        // wait for it to finish
        thread.join();

        // Print out Runtime.trace
        Trace trace = Runtime.getTrace();
        // trace size
        System.out.println("Trace size: " + trace.size());
        assert trace.size() == 1;
        System.out.println("First trace entry: " + trace.getTraceEntries().get(0));
    }

    public void foo() {
        Runtime.init();
        int i = 100;
        f1(i);
        Runtime.record("sendRR", 1);
    }

    public void f1(int i) {
        Runtime.hit(0);
        if (i == 0) {
            Runtime.hit(1);
        } else {
            Runtime.hit(2);
            f2();
        }
    }

    public void f2() {
        Runtime.hit(3);
    }
}
