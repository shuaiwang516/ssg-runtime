package org.zlab.ocov.tracker;

import org.junit.jupiter.api.Test;
import org.zlab.ocov.Utils;

import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;

public class TestObjectGraphTraverser {
    @Test
    public void test() {
        Integer a = 1;
        System.out.println(a.getClass().isPrimitive());
    }

    @Test
    public void testBasic() {
        Map<String, Map<String, String>> classInfoOri = Utils
                .loadMapFromFile(Paths.get("input/baseClassInfo.json"));
        ObjectGraphTraverser traverser = new ObjectGraphTraverser(classInfoOri);
        TargetClass.TargetClassA a = new TargetClass.TargetClassA();
        traverser.traverse(a);
        Set<Integer> visited = traverser.getVisited();
        assert visited.size() == 3;
    }
}
