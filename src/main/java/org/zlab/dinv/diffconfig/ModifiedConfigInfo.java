package org.zlab.dinv.diffconfig;

import java.util.Map;
import java.util.Set;

public class ModifiedConfigInfo {
    public Set<String> addedConfig;
    public Set<String> deletedConfig;
    public Set<String> changedTypeConfig;
    public Set<String> changedDefaultConfig;
    public Set<String> boundaryRelatedConfig;

    public ModifiedConfigInfo(Set<String> addedConfig,
                              Set<String> deletedConfig,
                              Set<String> changedTypeConfig,
                              Set<String> changedDefaultConfig,
                              Set<String> boundaryRelatedConfig) {

        this.addedConfig = addedConfig;
        this.deletedConfig = deletedConfig;
        this.changedTypeConfig = changedTypeConfig;
        this.changedDefaultConfig = changedDefaultConfig;
        this.boundaryRelatedConfig = boundaryRelatedConfig;
    }
}
