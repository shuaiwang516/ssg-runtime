package org.zlab.ocov.tracker;

import java.io.Serializable;

/**
 * Use this as a return value for update() Separate new format from boundary
 * changes
 */
public class FormatCoverageStatus implements Serializable {
    private static final long serialVersionUID = 20231215L;

    private boolean newFormat = false;
    private boolean boundaryChange = false;

    // Matchable new format
    private boolean matchableNewFormat = false;
    private boolean nonMatchableNewFormat = false;

    public void setNewFormat(String log) {
        Runtime.log("[New format] " + log);
        this.newFormat = true;
    }

    public void setBoundaryChange(String log) {
        Runtime.log("[New boundary change] " + log);
        this.boundaryChange = true;
    }

    public void incorporate(FormatCoverageStatus other) {
        if (other == null) {
            return;
        }
        this.newFormat = this.newFormat || other.newFormat;
        this.boundaryChange = this.boundaryChange || other.boundaryChange;
        this.matchableNewFormat = this.matchableNewFormat || other.matchableNewFormat;
        this.nonMatchableNewFormat = this.nonMatchableNewFormat || other.nonMatchableNewFormat;
    }

    public boolean isChanged() {
        return newFormat || boundaryChange;
    }

    public boolean isNewFormat() {
        return newFormat;
    }

    public boolean isBoundaryChange() {
        return boundaryChange;
    }

    public boolean isMatchableNewFormat() {
        return matchableNewFormat;
    }

    public boolean isNonMatchableNewFormat() {
        return nonMatchableNewFormat;
    }

    public void setMatchableNewFormat(String log) {
        // Runtime.log("[Matchable new format] " + log);
        this.matchableNewFormat = true;
    }

    public void setNonMatchableNewFormat(String log) {
        // Runtime.log("[Matchable new format] " + log);
        this.nonMatchableNewFormat = true;
    }

    @Override
    public String toString() {
        return "FormatCoverageStatus{" + "newFormat=" + newFormat + ", boundaryChange="
                + boundaryChange + ", matchableNewFormat=" + matchableNewFormat
                + ", nonMatchableNewFormat=" + nonMatchableNewFormat + '}';
    }

}
