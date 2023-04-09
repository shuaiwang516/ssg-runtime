package org.zlab.dinv.runtimechecker;

import java.util.LinkedList;
import java.util.List;

public class Utils {

    public static String getMethodName(String methodSig) {
        // input: DataStructures.StackArTester.createItem(int)
        // output: createItem

        int pos1 = methodSig.indexOf("(");
        if (pos1 == -1) {
            throw new RuntimeException("method Sig " + methodSig + " does not contain (");
        }
        String methodSigNoParam = methodSig.substring(0, pos1);
        int pos2 = methodSigNoParam.lastIndexOf(".");
        if (pos2 == -1) {
            throw new RuntimeException("method Sig " + methodSigNoParam + " does not contain dot");
        }
        return methodSigNoParam.substring(pos2 + 1);

    }

    public static String constructIfCondition(String stmt) {
        int invId = InstrumentInvariant.curInvId++;
        //        return "if (!(" + stmt + "))" + "{System.out.println(\"broken inv!\"); }";
        return "if (!(" + stmt + "))" + String.format("{org.zlab.dinv.runtimechecker.Runtime.addViolation(%s); }", invId);

    }

    public static List<String> constructInvStmt(List<String> invs) {
        List<String> ret = new LinkedList<>();
        for (String inv: invs) {
            // TODO: Handle comparison between pre-state and post-state
            if (inv.contains("orig")) {
                continue;
            }
            // All the generated invs will be added
            ret.add(constructIfCondition(inv));
        }
        return ret;
    }

    public static void test() {


    }


}
