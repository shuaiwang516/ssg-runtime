package org.zlab.ocov.dumper;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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

        public List<Integer> ids = new LinkedList<>();

        public TargetClassB() {
            ids.add(1);
            ids.add(2);
        }
    }

    public static class TargetClassC {
        // Define some fields here for testing
        int c = 100;
    }

    public static class TargetClassD {
        public int a = 1;
        public TargetClassB bObj = new TargetClassB();
        public TargetClassD dObj = null;

        public TargetClassD() {
            bObj.i = 100;
        }
    }

    public static class TargetClassE {
        // Define some fields here for testing
        public List<TargetClassF> fList = new LinkedList<>();
    }

    public static abstract class TargetClassF {
        // Define some fields here for testing
        public abstract void foo();
    }

    public static class TargetClassF1 extends TargetClassF {
        // Define some fields here for testing
        public int f1 = 1;

        @Override
        public void foo() {
            System.out.println("TargetClassF1");
        }
    }

    public static class TargetClassF2 extends TargetClassF {
        // Define some fields here for testing
        public int f2 = 2;

        @Override
        public void foo() {
            System.out.println("TargetClassF2");
        }
    }

    public static class TargetClassWithMap {
        // Define some fields here for testing
        public Map<Integer, TargetClassF> map = new HashMap<>();
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

    @Test
    public void testRecursiveObject() {
        TargetClassD obj = new TargetClassD();
    }

}
