package org.zlab.ocov.dumper;

import org.junit.jupiter.api.Test;

public class TestObjectGraphDumper {

    public static class TargetClassA {
        // Define some fields here for testing
        public int a = 1;
        public int b = 0;
        TargetClassB bObj = new TargetClassB();
    }

    public static class TargetClassB {
        // Define some fields here for testing
        String value = "Hello World!";
    }

    public static class TargetClassC {
        // Define some fields here for testing
        int c = 100;
    }

    @Test
    public void testDumper() {
        TargetClassA obj = new TargetClassA();
        try {
            new ObjectGraphDumper().dump(obj);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

}
