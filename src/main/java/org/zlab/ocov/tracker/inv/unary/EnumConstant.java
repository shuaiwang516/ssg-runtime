package org.zlab.ocov.tracker.inv.unary;

import org.zlab.ocov.tracker.inv.Invariant;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class EnumConstant extends UnaryInvariant {
    private static final long serialVersionUID = 20231215L;

    public Map<String, Set<String>> enumConstants = new HashMap<>();

    @Override
    public boolean merge(Invariant other) {
        if (other instanceof EnumConstant) {
            boolean changed = false;
            EnumConstant otherEnumConstant = (EnumConstant) other;
            for (String className : otherEnumConstant.enumConstants.keySet()) {
                if (!enumConstants.containsKey(className)) {
                    enumConstants.put(className, new HashSet<>());
                }
                for (String enumConstant : otherEnumConstant.enumConstants.get(className)) {
                    // check whether it's already included
                    if (!enumConstants.get(className).contains(enumConstant)) {
                        changed = true;
                        enumConstants.get(className).add(enumConstant);
                    }
                }

            }
            return changed;
        }
        return false;
    }

    @Override
    public void reset() {
        enumConstants.clear();
    }

    @Override
    public boolean checkPure(Object val, LogInfo logInfo) {
        if (val == null)
            return false;
        boolean changed = false;
        if (val.getClass().isEnum()) {
            // check if the enum class has been visited
            if (!enumConstants.containsKey(val.getClass().getName())) {
                changed = true;
            } else {
                // check if the enum constant has been visited
                if (!enumConstants.get(val.getClass().getName()).contains(val.toString())) {
                    changed = true;
                }
            }
        }
        return changed;
    }

    @Override
    public boolean add(Object val, LogInfo logInfo) {
        if (val == null)
            return false;
        boolean changed = false;
        if (val.getClass().isEnum()) {
            // check if the enum class has been visited
            if (!enumConstants.containsKey(val.getClass().getName())) {
                enumConstants.put(val.getClass().getName(), new HashSet<>());
            }
            // check if the enum constant has been visited
            if (!enumConstants.get(val.getClass().getName()).contains(val.toString())) {
                enumConstants.get(val.getClass().getName()).add(val.toString());
                dumpId = logInfo.dumpId;
                changed = true;
            }
        }
        return changed;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("<%s> dumpId = %s", typeName, dumpId));
        sb.append(" (");
        for (String className : enumConstants.keySet()) {
            sb.append(className);
            sb.append(": ");
            sb.append(enumConstants.get(className));
            sb.append(", ");
        }
        sb.append(")");
        return sb.toString();
    }
}
