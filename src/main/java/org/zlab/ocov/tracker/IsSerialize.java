package org.zlab.ocov.tracker;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class IsSerialize implements Serializable {
    private static final long serialVersionUID = 20231215L;

    private final Map<String, Set<String>> modifiedFields;
    private final Set<String> modifiedEnums;

    private final Set<String> serializedClasses = new HashSet<>();
    private final Map<String, Set<String>> serializedEnums = new HashMap<>();

    public IsSerialize(Map<String, Set<String>> modifiedFields, Set<String> modifiedEnums) {
        this.modifiedFields = modifiedFields;
        this.modifiedEnums = modifiedEnums;
    }

    public void updateVisitedClasses(String className) {
        if (modifiedFields.containsKey(className)) {
            serializedClasses.add(className);
        }
    }

    public void updateVisitedEnums(String enumName, String enumValue) {
        if (modifiedEnums.contains(enumName)) {
            Set<String> enumValues = serializedEnums.computeIfAbsent(enumName,
                    k -> new HashSet<>());
            enumValues.add(enumValue);
        }
    }

    public boolean merge(IsSerialize other) {
        if (other == null) {
            return false;
        }
        boolean changed = false;
        for (String className : other.serializedClasses) {
            if (!serializedClasses.contains(className)) {
                serializedClasses.add(className);
                // log
                Runtime.log(String.format("<isSerialized Class: %s>", className));
                changed = true;
            }
        }
        for (Map.Entry<String, Set<String>> entry : other.serializedEnums.entrySet()) {
            String enumName = entry.getKey();
            Set<String> otherEnumValues = entry.getValue();
            if (!serializedEnums.containsKey(enumName)) {
                serializedEnums.put(enumName, new HashSet<>());
            }
            for (String enumValue : otherEnumValues) {
                if (!serializedEnums.get(enumName).contains(enumValue)) {
                    serializedEnums.get(enumName).add(enumValue);
                    // log
                    Runtime.log(String.format("<isSerialized Enum: %s.%s>", enumName, enumValue));
                    changed = true;
                }
            }
        }
        return changed;
    }

}
