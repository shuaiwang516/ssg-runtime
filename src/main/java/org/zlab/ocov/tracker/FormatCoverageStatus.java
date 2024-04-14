package org.zlab.ocov.tracker;

import java.io.Serializable;

/**
 * Use this as a return value for update() Separate new format from boundary
 * changes
 */
public class FormatCoverageStatus implements Serializable {
    private static final long serialVersionUID = 20231215L;

    public boolean newFormat;
    public boolean boundaryChange;

    public FormatCoverageStatus() {
        this(false, false);
    }

    public FormatCoverageStatus(boolean newFormat, boolean boundaryChange) {
        this.newFormat = newFormat;
        this.boundaryChange = boundaryChange;
    }

    public void incorporate(FormatCoverageStatus other) {
        if (other == null) {
            return;
        }
        this.newFormat = this.newFormat || other.newFormat;
        this.boundaryChange = this.boundaryChange || other.boundaryChange;
    }

    public boolean isChanged() {
        return newFormat || boundaryChange;
    }
}
