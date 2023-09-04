package org.zlab.dinv.diffconfig;

import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

import static org.zlab.dinv.modifiedfields.Utils.createDirIfNotExist;

public class RetrieveDiffConfig extends ConfigRetriever implements Runnable {
    /**
     * Iterate the target config class, return configuration + default value
     */
    @CommandLine.Option(names = {
            "-infoPath"}, required = true, description = "path to files generated from vasco")
    private Path infoPath;

    @CommandLine.Option(names = {
            "-targetOldSystemPath"}, required = true, description = "path to old system")
    private Path targetOldSystemPath;

    @CommandLine.Option(names = {
            "-targetNewSystemPath"}, required = true, description = "path to new system")
    private Path targetNewSystemPath;

    @CommandLine.Option(names = {
            "-tc"}, split = ",", required = true, description = "target config classes")
    private List<String> targetClasses;

    @Override
    public void run() {
        try {
            ConfigInfo oldConfigInfo = extractConfigs(targetOldSystemPath, targetClasses);
            ConfigInfo newConfigInfo = extractConfigs(targetNewSystemPath, targetClasses);

            // post process for HDFS: merge configs
            if (targetOldSystemPath.toString().contains("hadoop-hdfs-project")) {
                oldConfigInfo = hdfs_post_process(oldConfigInfo);
                newConfigInfo = hdfs_post_process(newConfigInfo);
            } else if (targetOldSystemPath.toString().contains("hbase")) {
                oldConfigInfo = hbase_post_process(oldConfigInfo);
                newConfigInfo = hbase_post_process(newConfigInfo);
            }

            // compute ModifiedConfigInfo
            ModifiedConfigInfo modifiedConfigInfo = computeModifiedConfigInfo(oldConfigInfo,
                    newConfigInfo);
            // save modifiedConfigInfo
            createDirIfNotExist(infoPath);
            saveDiffConfigInfo(oldConfigInfo, newConfigInfo, modifiedConfigInfo, infoPath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static ModifiedConfigInfo computeModifiedConfigInfo(ConfigInfo oldConfigInfo,
            ConfigInfo newConfigInfo) {
        Set<String> addedConfig = new HashSet<>();
        Set<String> deletedConfig = new HashSet<>();
        Set<String> changedTypeConfig = new HashSet<>();
        Set<String> changedDefaultConfig = new HashSet<>();
        Set<String> boundaryRelatedConfig = new HashSet<>();

        // added config
        diffConfig(newConfigInfo.classToFieldsWithType, oldConfigInfo.classToFieldsWithType,
                addedConfig);
        // deleted config
        diffConfig(oldConfigInfo.classToFieldsWithType, newConfigInfo.classToFieldsWithType,
                deletedConfig);

        computeDefaultValueChangedConfig(oldConfigInfo, newConfigInfo, changedTypeConfig,
                changedDefaultConfig);
        computeBoundaryRelatedConfig(oldConfigInfo, newConfigInfo, boundaryRelatedConfig);
        return new ModifiedConfigInfo(addedConfig, deletedConfig, changedTypeConfig,
                changedDefaultConfig, boundaryRelatedConfig);
    }

    // only exists in classToFieldsWithType1
    public static void diffConfig(Map<String, Map<String, String>> classToFieldsWithType1,
            Map<String, Map<String, String>> classToFieldsWithType2, Set<String> output) {
        for (String className : classToFieldsWithType1.keySet()) {
            if (!classToFieldsWithType2.containsKey(className)) {
                // add all
                output.addAll(classToFieldsWithType1.get(className).keySet());
            } else {
                for (String oldConfigName : classToFieldsWithType1.get(className).keySet()) {
                    if (!classToFieldsWithType2.get(className).containsKey(oldConfigName)) {
                        output.add(oldConfigName);
                    }
                }
            }
        }
    }

    public static void computeDefaultValueChangedConfig(ConfigInfo oldConfigInfo,
            ConfigInfo newConfigInfo, Set<String> changedTypeConfig,
            Set<String> changedDefaultConfig) {
        for (String className : oldConfigInfo.classToFieldsWithType.keySet()) {
            if (newConfigInfo.classToFieldsWithType.containsKey(className)) {
                // check whether type is changed
                for (String configName : oldConfigInfo.classToFieldsWithType.get(className)
                        .keySet()) {
                    if (newConfigInfo.classToFieldsWithType.containsKey(configName)) {
                        // check type changed
                        if (oldConfigInfo.classToFieldsWithType.get(className).get(configName)
                                .equals(newConfigInfo.classToFieldsWithType.get(className)
                                        .get(configName))) {
                            // type changed
                            changedTypeConfig.add(configName);
                            continue;
                        }
                        // default value changed?
                        String oldInit = null;
                        String newInit = null;
                        if (oldConfigInfo.classToFieldsWithInit.get(className)
                                .containsKey(configName)) {
                            oldInit = oldConfigInfo.classToFieldsWithInit.get(className)
                                    .get(configName);
                        }
                        if (newConfigInfo.classToFieldsWithInit.get(className)
                                .containsKey(configName)) {
                            newInit = newConfigInfo.classToFieldsWithInit.get(className)
                                    .get(configName);
                        }
                        if ((oldInit == null && newInit != null)
                                || (oldInit != null && !oldInit.equals(newInit))) {
                            changedDefaultConfig.add(configName);
                        }
                    }
                }
            }
        }
    }

    public static void computeBoundaryRelatedConfig(ConfigInfo oldConfigInfo,
            ConfigInfo newConfigInfo, Set<String> boundaryRelatedConfig) {
        for (String className : oldConfigInfo.classToFieldsWithType.keySet()) {
            if (newConfigInfo.classToFieldsWithType.containsKey(className)) {
                for (String configName : oldConfigInfo.classToFieldsWithType.get(className)
                        .keySet()) {
                    if (configName.toLowerCase().contains("size")
                            && newConfigInfo.classToFieldsWithType.get(className)
                                    .containsKey(configName)) {
                        boundaryRelatedConfig.add(configName);
                    }
                }
            }

        }
    }

    public static void saveDiffConfigInfo(ConfigInfo oldConfigInfo, ConfigInfo newConfigInfo,
            ModifiedConfigInfo modifiedConfigInfo, Path outputPath) {

        saveConfigInfo(removeClassInfo(oldConfigInfo.classToFieldsWithType),
                outputPath.resolve("oriConfig2Type.json"));
        saveConfigInfo(removeClassInfo(oldConfigInfo.classToFieldsWithInit),
                outputPath.resolve("oriConfig2Init.json"));
        saveEnumInfo(oldConfigInfo.enumClass2Constants,
                outputPath.resolve("oriEnum2Constant.json"));
        saveConfigInfo(removeClassInfo(newConfigInfo.classToFieldsWithType),
                outputPath.resolve("upConfig2Type.json"));
        saveConfigInfo(removeClassInfo(newConfigInfo.classToFieldsWithInit),
                outputPath.resolve("upConfig2Init.json"));
        saveEnumInfo(newConfigInfo.enumClass2Constants, outputPath.resolve("upEnum2Constant.json"));

        saveConfigs(modifiedConfigInfo.addedConfig, outputPath.resolve("addedClassConfig.json"));
        saveConfigs(modifiedConfigInfo.deletedConfig,
                outputPath.resolve("deletedClassConfig.json"));
        saveConfigs(modifiedConfigInfo.changedTypeConfig,
                outputPath.resolve("changedTypeConfig.json"));
        saveConfigs(modifiedConfigInfo.changedDefaultConfig,
                outputPath.resolve("changedDefaultConfig.json"));
        saveConfigs(modifiedConfigInfo.boundaryRelatedConfig,
                outputPath.resolve("boundaryRelatedConfig.json"));
        // Save the last three as common configs
        Set<String> commonConfigs = new HashSet<>();
        commonConfigs.addAll(modifiedConfigInfo.changedTypeConfig);
        commonConfigs.addAll(modifiedConfigInfo.changedDefaultConfig);
        commonConfigs.addAll(modifiedConfigInfo.boundaryRelatedConfig);
        saveConfigs(commonConfigs, outputPath.resolve("commonConfig.json"));
    }

    public static ConfigInfo hdfs_post_process(ConfigInfo configInfo) {
        ConfigInfo mergedConfigInfo = new ConfigInfo();
        for (String clazz : configInfo.classToFieldsWithType.keySet()) {
            for (String config1 : configInfo.classToFieldsWithType.get(clazz).keySet()) {

                // skip default
                if (config1.endsWith("_DEFAULT"))
                    continue;;

                String type1 = configInfo.classToFieldsWithType.get(clazz).get(config1);
                String init1 = null;
                if (configInfo.classToFieldsWithInit.get(clazz).containsKey(config1)) {
                    init1 = configInfo.classToFieldsWithInit.get(clazz).get(config1);
                }

                String configName = config1;
                String configType = type1;
                String configInit = init1;

                if (init1 != null) {
                    String configRealName = init1.replace("\"", "");

                    String config2;
                    if (config1.endsWith("_KEY")) {
                        config2 = config1.substring(0, config1.length() - 4) + "_DEFAULT";
                    } else {
                        config2 = config1 + "_DEFAULT";
                    }
                    // look for default value (this is init)
                    if (configInfo.classToFieldsWithInit.get(clazz).containsKey(config2)) {
                        // Merge
                        String configRealInit = configInfo.classToFieldsWithInit.get(clazz)
                                .get(config2);
                        String configRealType = configInfo.classToFieldsWithType.get(clazz)
                                .get(config2);

                        configName = configRealName;
                        configType = configRealType;
                        configInit = configRealInit;
                    }
                }

                // include this config
                if (!mergedConfigInfo.classToFieldsWithType.containsKey(clazz)) {
                    mergedConfigInfo.classToFieldsWithType.put(clazz, new HashMap<>());
                }
                mergedConfigInfo.classToFieldsWithType.get(clazz).put(configName, configType);
                if (configInit != null) {
                    if (!mergedConfigInfo.classToFieldsWithInit.containsKey(clazz)) {
                        mergedConfigInfo.classToFieldsWithInit.put(clazz, new HashMap<>());
                    }
                    mergedConfigInfo.classToFieldsWithInit.get(clazz).put(configName, configInit);
                }
            }
        }

        return mergedConfigInfo;
    }

    public static ConfigInfo hbase_post_process(ConfigInfo configInfo) {
        ConfigInfo mergedConfigInfo = new ConfigInfo();
        for (String clazz : configInfo.classToFieldsWithType.keySet()) {
            for (String config1 : configInfo.classToFieldsWithType.get(clazz).keySet()) {

                // skip default
                if (config1.startsWith("DEFAULT_"))
                    continue;

                String type1 = configInfo.classToFieldsWithType.get(clazz).get(config1);
                String init1 = null;
                if (configInfo.classToFieldsWithInit.get(clazz).containsKey(config1)) {
                    init1 = configInfo.classToFieldsWithInit.get(clazz).get(config1);
                }

                String configName = config1;
                String configType = type1;
                String configInit = init1;

                if (init1 != null) {
                    String configRealName = init1.replace("\"", "");

                    String config2;
                    if (config1.endsWith("_KEY")) {
                        config2 = "DEFAULT_" + config1.substring(0, config1.length() - 4);
                    } else {
                        config2 = "DEFAULT_" + config1;
                    }
                    // look for default value (this is init)
                    if (configInfo.classToFieldsWithInit.get(clazz).containsKey(config2)) {
                        // Merge
                        String configRealInit = configInfo.classToFieldsWithInit.get(clazz)
                                .get(config2);
                        String configRealType = configInfo.classToFieldsWithType.get(clazz)
                                .get(config2);

                        configName = configRealName;
                        configType = configRealType;
                        configInit = configRealInit;
                    }
                }

                // include this config
                if (!mergedConfigInfo.classToFieldsWithType.containsKey(clazz)) {
                    mergedConfigInfo.classToFieldsWithType.put(clazz, new HashMap<>());
                }
                mergedConfigInfo.classToFieldsWithType.get(clazz).put(configName, configType);
                if (configInit != null) {
                    if (!mergedConfigInfo.classToFieldsWithInit.containsKey(clazz)) {
                        mergedConfigInfo.classToFieldsWithInit.put(clazz, new HashMap<>());
                    }
                    mergedConfigInfo.classToFieldsWithInit.get(clazz).put(configName, configInit);
                }
            }
        }

        return mergedConfigInfo;
    }

}
