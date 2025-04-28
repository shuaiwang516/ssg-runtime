package net.tracker;

import org.junit.jupiter.api.Test;
import org.zlab.net.tracker.ObjectGraphTraverser;
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
}
