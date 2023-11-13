package org.zlab.dinv.serializepoint;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;

import static org.zlab.dinv.modifiedfields.Utils.createOutputDirIfNotExist;

public class Utils {

    public static void saveSerializePoints(Set<SerializePoint> serializePoints, Path filePath) {
        createOutputDirIfNotExist();
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            objectMapper.writeValue(filePath.toFile(), serializePoints);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Set<SerializePoint> loadSerializePoints(Path filePath) {
        // Read the map from the JSON file
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            Set<SerializePoint> mapFromFile = objectMapper.readValue(filePath.toFile(),
                    new TypeReference<Set<SerializePoint>>() {
                    });
            return mapFromFile;
        } catch (IOException e) {
            System.err.println("Exception happen when loading output from " + filePath);
            throw new RuntimeException(e);
        }
    }

    public static String logStatement() {
        return "System.out.println(\"[Dinv] \" + Thread.currentThread().getName() + \" \" + System.currentTimeMillis());";
    }

}
