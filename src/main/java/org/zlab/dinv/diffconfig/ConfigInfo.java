package org.zlab.dinv.diffconfig;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigInfo {
    public Map<String, Map<String, String>> classToFieldsWithType = new HashMap<>();
    public Map<String, Map<String, String>> classToFieldsWithInit = new HashMap<>();
    public Map<String, List<String>> enumClass2Constants = new HashMap<>();
}
