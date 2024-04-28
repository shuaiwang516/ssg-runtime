package org.zlab.ocov.tracker;

import org.apache.commons.lang3.SerializationUtils;
import org.zlab.ocov.tracker.graph.GraphPattern;
import org.zlab.ocov.tracker.inv.unary.LogInfo;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Context implements Serializable {
    private static final long serialVersionUID = 20231215L;

    private boolean flag;

    public Context(boolean flag) {
        this.flag = flag;
    }

    @Override
    public String toString() {
        return "Context{" + "flag=" + flag + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        Context context = (Context) o;

        return flag == context.flag;
    }

    @Override
    public int hashCode() {
        return (flag ? 1 : 0);
    }

    public static boolean compute(int dumpId, Map<String, GraphPattern> baseClassInfo,
            EqualitySet equalitySet, IsSerialize isSerialized, Object... contextArgs) {
        if (contextArgs == null || contextArgs.length == 0)
            return false;
        boolean containPreservedString = false;
        for (Object contextObj : contextArgs) {
            if (contextObj == null)
                continue;
            String className = contextObj.getClass().getName();
            if (!baseClassInfo.containsKey(className))
                continue;
            Set<String> brokenInvs = new HashSet<>();
            GraphPattern graphPattern = SerializationUtils.clone(baseClassInfo.get(className));
            graphPattern.update(contextObj, baseClassInfo, new LogInfo(dumpId), equalitySet,
                    isSerialized, brokenInvs, System.identityHashCode(contextObj));
            // FIXME: examine whether the brokenInvs contains a <String: preserved String>
            for (String brokenInvsStr : brokenInvs) {
                if (brokenInvsStr.contains("<PreservedStringOnce>")) {
                    containPreservedString = true;
                    break;
                }
            }
        }
        return containPreservedString;
    }
}
