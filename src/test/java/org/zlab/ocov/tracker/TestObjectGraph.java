package org.zlab.ocov.tracker;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class TestObjectGraph {

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

    public enum TargetEnum {
        A, B, C
    }

    public static class TargetClassForEnum {
        // Define some fields here for testing
        public TargetEnum e;
        public TargetClassF1 f1 = new TargetClassF1();
    }

    public static class TargetClassEquality {
        public TargetClassEqualityA targetClassEqualityA = new TargetClassEqualityA();
        public TargetClassEqualityC targetClassEqualityC = new TargetClassEqualityC();
    }

    public static class TargetClassEqualityA {
        public TargetClassEqualityAA targetClassEqualityAA = new TargetClassEqualityAA();
    }

    public static class TargetClassEqualityAA {
        public CompClass compClass = new CompClass(2);
    }

    public static class TargetClassEqualityC {
        public CompClass compClass = new CompClass(3);
    }

    public static class CompClass {
        // comparable class
        public int a;

        public CompClass(int a) {
            this.a = a;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (obj instanceof CompClass) {
                CompClass other = (CompClass) obj;
                return a == other.a;
            }
            return false;
        }

        @Override
        public int hashCode() {
            return a;
        }
    }

    public static class TargetClassWithSizeBase {
        public TargetClassWithSizeA a = new TargetClassWithSizeA();
    }

    public static class TargetClassWithSizeA {
        public int size = 100;
        int size() {
            return size;
        }
    }

    @Test
    public void testRecursiveObject() {
        TargetClassD obj = new TargetClassD();
    }

}
