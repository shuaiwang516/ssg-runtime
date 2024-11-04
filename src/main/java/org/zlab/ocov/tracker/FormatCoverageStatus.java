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

    // Modification Guided Testing
    private boolean newFormatAtModifiedMergePoint = false;
    private boolean newFormatRelatedToModifiedType = false;

    // Matchable new format
    private boolean matchableNewFormat = false;

    public void setNewFormat(String log) {
        Runtime.log("[New format] " + log);
        this.newFormat = true;
    }

    public void setBoundaryChange(String log) {
        Runtime.log("[New boundary change] " + log);
        this.boundaryChange = true;
    }

    public void setNewFormatAtModifiedMergePoint(String log) {
        // Runtime.log("[New format at modified merge point] " + log);
        this.newFormatAtModifiedMergePoint = true;
    }

    public void setNewFormatRelatedToModifiedType(String log) {
        // Runtime.log("[New format related to modified type] " + log);
        this.newFormatRelatedToModifiedType = true;
    }

    public void incorporate(FormatCoverageStatus other) {
        if (other == null) {
            return;
        }
        this.newFormat = this.newFormat || other.newFormat;
        this.boundaryChange = this.boundaryChange || other.boundaryChange;
        this.newFormatAtModifiedMergePoint = this.newFormatAtModifiedMergePoint
                || other.newFormatAtModifiedMergePoint;
        this.newFormatRelatedToModifiedType = this.newFormatRelatedToModifiedType
                || other.newFormatRelatedToModifiedType;
        this.matchableNewFormat = this.matchableNewFormat || other.matchableNewFormat;

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

    public boolean isNewFormatAtModifiedMergePoint() {
        return newFormatAtModifiedMergePoint;
    }

    public boolean isNewFormatRelatedToModifiedType() {
        return newFormatRelatedToModifiedType;
    }

    public boolean isMatchableNewFormat() {
        return matchableNewFormat;
    }

    public void setMatchableNewFormat(String log) {
        // Runtime.log("[Matchable new format] " + log);
        this.matchableNewFormat = true;
    }

    @Override
    public String toString() {
        return "FormatCoverageStatus{" + "newFormat=" + newFormat + ", boundaryChange="
                + boundaryChange + ", newFormatAtModifiedMergePoint="
                + newFormatAtModifiedMergePoint + ", newFormatRelatedToModifiedType="
                + newFormatRelatedToModifiedType + '}';
    }

}
