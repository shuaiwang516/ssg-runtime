package org.zlab.ocov.tracker.type;

import org.junit.jupiter.api.Test;

public class TestEnumType {

    public enum EnumType {
        A, B, C
    }

    @Test
    public void testEnumType() {
        EnumType a = EnumType.A;

        System.out.println("class = " + a.getClass());

        if (a instanceof Object) {
            System.out.println("a is an instance of Object");
            Class z = a.getClass();

            String name = a.getClass().getName();
            System.out.println("name = " + name);

            if (z.isEnum()) {
                System.out.println("a is an instance of Enum");
                // print all its constants
                for (Object o : z.getEnumConstants()) {
                    System.out.println(o);
                }
            } else {
                System.out.println("a is not an instance of Enum");
            }
        } else {
            System.out.println("a is not an instance of Object");
        }
    }
}
