package org.zlab.ocov.tracker;

import org.zlab.ocov.tracker.type.TypeInfo;

import java.util.HashMap;
import java.util.Map;

public class ClassInfo {
    // Iterate all instances of this class, and update the constraint information
    Map<String, TypeInfo> fields = new HashMap<>();
}
