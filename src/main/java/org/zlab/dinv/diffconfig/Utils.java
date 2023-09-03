package org.zlab.dinv.diffconfig;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javaparser.ast.expr.Expression;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class Utils {

    public static Map<String, Map<String, Map<String, Set<Integer>>>> loadFields2Locations(
            Path filePath) {
        // Read the map from the JSON file
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            Map<String, Map<String, Map<String, Set<Integer>>>> mapFromFile = objectMapper
                    .readValue(filePath.toFile(),
                            new TypeReference<Map<String, Map<String, Map<String, Set<Integer>>>>>() {
                            });
            return mapFromFile;
        } catch (IOException e) {
            System.err.println("Exception happen when loading output from " + filePath);
            throw new RuntimeException(e);
        }
    }

    public static Set<String> mergeConfigHadoop(Set<String> configs,
            Map<String, Optional<Expression>> config2Init, Map<String, String> config2Type,
            Map<String, Optional<Expression>> merged_config2Init,
            Map<String, String> merged_config2type) {
        Set<String> mergedConfig = new HashSet<>();
        for (String config : configs) {
            Optional<Expression> configInit = config2Init.get(config);
            if (configInit.isPresent()) {
                String configRealKey = configInit.get().toString().replace("\"", "");
                String configInitKey;
                if (config.endsWith("_KEY")) {
                    configInitKey = config.substring(0, config.length() - 4) + "_DEFAULT";
                } else {
                    configInitKey = config + "_DEFAULT";
                }
                if (config2Init.containsKey(configInitKey)) {
                    // Merge
                    // String DFS_DOMAIN_SOCKET_DISABLE_INTERVAL_SECOND_KEY =
                    // "dfs.domain.socket.disable.interval.seconds";
                    // long DFS_DOMAIN_SOCKET_DISABLE_INTERVAL_SECOND_DEFAULT = 600;
                    Optional<Expression> configRealInit = config2Init.get(configInitKey);
                    String configReadType = config2Type.get(configInitKey);
                    mergedConfig.add(configRealKey);
                    merged_config2Init.put(configRealKey, configRealInit);
                    merged_config2type.put(configRealKey, configReadType);
                } else {
                    // Default is not given
                    // keep the original
                    // logger.error(
                    // String.format("config key %s is given but default value is not given",
                    // config));
                }
            }
        }
        return mergedConfig;
    }

}
