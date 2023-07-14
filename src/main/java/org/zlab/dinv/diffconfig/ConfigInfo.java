package org.zlab.dinv.diffconfig;

import java.util.HashMap;
import java.util.Map;

public class ConfigInfo {
    public Map<String, Map<String, String>> classToFieldsWithType = new HashMap<>();
    public Map<String, Map<String, String>> classToFieldsWithInit = new HashMap<>();
}
