package net.tracker;

import org.junit.jupiter.api.Test;
import org.zlab.net.tracker.ObjectGraphTraverser;
import org.zlab.net.tracker.RecvMeta;
import org.zlab.net.tracker.Runtime;
import org.zlab.net.tracker.SendMeta;
import org.zlab.net.tracker.Trace;
import org.zlab.net.tracker.TraceEntry;

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
        Runtime.init(false);
        Runtime.clear();
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

    @Test
    public void testRecordSendWithMeta() {
        Runtime.init(false);
        Runtime.clear();

        Message<Mutation> msg = new Message<>(new Mutation(1));
        Runtime.hit(10);
        Runtime.recordSend("sendRR", 7, msg,
                SendMeta.builder().nodeId("node-a").peerId("node-b").fanoutType("UNICAST")
                        .logicalMessageId("msg-1").deliveryId("msg-1-node-b")
                        .messageType("Mutation").messageVersion("v2").build(),
                msg);

        Trace trace = Runtime.getTrace();
        assert trace.size() == 1;
        TraceEntry entry = trace.getTraceEntries().get(0);
        assert entry.eventType == TraceEntry.EventType.SEND;
        assert "node-a".equals(entry.nodeId);
        assert "node-b".equals(entry.peerId);
        assert "msg-1".equals(entry.logicalMessageId);
        assert entry.beforeExecPath != null && entry.beforeExecPath.length > 0;
    }

    @Test
    public void testReceiveBeforeAfterCapture() {
        Runtime.init(false);
        Runtime.clear();

        Message<Mutation> msg = new Message<>(new Mutation(1));
        Runtime.hit(100);

        long token = Runtime.beginReceive("recvRR", 9, msg,
                RecvMeta.builder().nodeId("node-b").peerId("node-a").logicalMessageId("msg-2")
                        .deliveryId("msg-2-node-b").messageType("Mutation").messageVersion("v2")
                        .build(),
                msg);
        Runtime.hit(101);
        Runtime.hit(102);
        Runtime.endReceive(token);

        Trace trace = Runtime.getTrace();
        assert trace.size() == 2;
        TraceEntry begin = trace.getTraceEntries().get(0);
        TraceEntry end = trace.getTraceEntries().get(1);
        assert begin.eventType == TraceEntry.EventType.RECV_BEGIN;
        assert end.eventType == TraceEntry.EventType.RECV_END;
        assert end.beforeExecPath != null && end.beforeExecPath.length > 0;
        assert end.afterExecPath != null && end.afterExecPath.length >= 2;
        assert end.afterExecPath[end.afterExecPath.length - 2] == 101;
        assert end.afterExecPath[end.afterExecPath.length - 1] == 102;
    }
}
