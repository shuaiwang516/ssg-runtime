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
    private boolean isNewFormatAtModifiedMergePoint = false;
    private boolean isNewFormatRelatedToModifiedType = false;

    public void setNewFormat() {
        this.newFormat = true;
    }

    public void setNewFormat(String log) {
        Runtime.log("[New format] " + log);
        this.newFormat = true;
    }

    public void setBoundaryChange(String log) {
        Runtime.log("[New boundary change] " + log);
        this.boundaryChange = true;
    }

    public void setNewFormatAtModifiedMergePoint(String log) {
        Runtime.log("[New format at modified merge point] " + log);
        this.isNewFormatAtModifiedMergePoint = true;
    }

    public void setNewFormatRelatedToModifiedType(String log) {
        Runtime.log("[New format related to modified type] " + log);
        this.isNewFormatRelatedToModifiedType = true;
    }

    public void incorporate(FormatCoverageStatus other) {
        if (other == null) {
            return;
        }
        this.newFormat = this.newFormat || other.newFormat;
        this.boundaryChange = this.boundaryChange || other.boundaryChange;
        this.isNewFormatAtModifiedMergePoint = this.isNewFormatAtModifiedMergePoint
                || other.isNewFormatAtModifiedMergePoint;
        this.isNewFormatRelatedToModifiedType = this.isNewFormatRelatedToModifiedType
                || other.isNewFormatRelatedToModifiedType;
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
        return isNewFormatAtModifiedMergePoint;
    }

    public boolean isNewFormatRelatedToModifiedType() {
        return isNewFormatRelatedToModifiedType;
    }

    @Override
    public String toString() {
        return "FormatCoverageStatus{" + "newFormat=" + newFormat + ", boundaryChange="
                + boundaryChange + ", isNewFormatAtModifiedMergePoint="
                + isNewFormatAtModifiedMergePoint + ", isNewFormatRelatedToModifiedType="
                + isNewFormatRelatedToModifiedType + '}';
    }
}
