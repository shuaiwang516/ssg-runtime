package org.zlab.ocov.dumper;

import org.junit.jupiter.api.Test;

public class TestObjectGraphDumper {

    public static class TargetClassA {
        // Define some fields here for testing
        public int a = 1;
        public int b = 0;
        public int c = 0; // Not target field
        public TargetClassB bObj = new TargetClassB();
    }

    public static class TargetClassB {
        // Define some fields here for testing
        public int i = 0;
        String value = "Hello World!";
    }

    public static class TargetClassC {
        // Define some fields here for testing
        int c = 100;
    }

    public static class TargetClassD {
        public int a = 1;
        public TargetClassB bObj = new TargetClassB();

        public TargetClassD() {
            bObj.i = 100;
        }
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
