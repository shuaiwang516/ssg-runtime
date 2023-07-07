package org.zlab.dinv.diffconfig;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

public class Utils {

    public static Map<String, Map<String, Map<String, Set<Integer>>>> loadFields2Locations(Path filePath) {
        // Read the map from the JSON file
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            Map<String, Map<String, Map<String, Set<Integer>>>> mapFromFile = objectMapper.readValue(filePath.toFile(),
                    new TypeReference<Map<String, Map<String, Map<String, Set<Integer>>>>>() {});
            return mapFromFile;
        } catch (IOException e) {
            System.err.println("Exception happen when loading output from " + filePath);
            throw new RuntimeException(e);
        }
    }

}
