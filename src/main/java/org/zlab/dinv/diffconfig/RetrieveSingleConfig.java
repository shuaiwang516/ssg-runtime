package org.zlab.dinv.diffconfig;

import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.zlab.dinv.modifiedfields.Utils.createDirIfNotExist;

public class RetrieveSingleConfig extends ConfigRetriever implements Runnable {
    /**
     * Iterate the target config class, return configuration + default value
     */
    @CommandLine.Option(names = {
            "-infoPath"}, required = true, description = "path to files generated from vasco")
    private Path infoPath;

    @CommandLine.Option(names = {
            "-targetSystemPath"}, required = true, description = "path to old system")
    private Path targetSystemPath;

    @CommandLine.Option(names = {
            "-tc"}, split = ",", required = true, description = "target config classes")
    private List<String> targetClasses;

    @Override
    public void run() {
        try {
            ConfigInfo configInfo = extractConfigs(targetSystemPath, targetClasses);
            createDirIfNotExist(infoPath);

            saveSingleConfigInfo(configInfo, infoPath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void saveSingleConfigInfo(ConfigInfo configInfo, Path outputPath) {
        saveConfigInfo(removeClassInfo(configInfo.classToFieldsWithType),
                outputPath.resolve("config2Type.json"));
        saveConfigInfo(removeClassInfo(configInfo.classToFieldsWithInit),
                outputPath.resolve("config2Init.json"));
        saveEnumInfo(configInfo.enumClass2Constants, outputPath.resolve("enum2Constant.json"));
    }
}
