package org.zlab.dinv.visibility;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

public class Utils {
    public static Map<String, Map<String, Set<Integer>>> readIfInfo(Path path) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Map<String, Set<Integer>>> data = objectMapper.readValue(path.toFile(), Map.class);
        return data;
    }
}
