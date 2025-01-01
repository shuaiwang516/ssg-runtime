package org.zlab.ocov.tracker;

import java.io.Serializable;

/**
 * Use this as a return value for update() Separate new format from boundary
 * changes
 */
public class FormatCoverageStatus implements Serializable {
    private static final long serialVersionUID = 20231215L;

    private boolean newFormat = false;
    private int newFormatCount = 0;
    private boolean boundaryChange = false;
    private boolean multiInvBroken = false;

    // Matchable new format
    private boolean matchableNewFormat = false;
    private boolean nonMatchableNewFormat = false;
    private int nonMatchableNewFormatCount = 0;

    private boolean nonMatchableMultiInv = false;
    private int nonMatchableMultiInvCount = 0;

    // isSerialize likely invariant
    private boolean isSerialized = false;

    public void setNewFormat(String log) {
        Runtime.log("[New format] " + log);
        this.newFormat = true;
        this.newFormatCount++;
    }

    public void setBoundaryChange(String log) {
        Runtime.log("[New boundary change] " + log);
        this.boundaryChange = true;
    }

    public void setMultiInvBroken(String log) {
        // Runtime.log("[Multi-inv broken] " + log);
        this.multiInvBroken = true;
    }

    public void setNonMatchableMultiInv(String log) {
        // Runtime.log("[Multi-inv broken] " + log);
        this.nonMatchableMultiInv = true;
        this.nonMatchableMultiInvCount++;
    }

    public void incorporate(FormatCoverageStatus other) {
        if (other == null) {
            return;
        }
        this.newFormat = this.newFormat || other.newFormat;
        this.newFormatCount += other.newFormatCount;
        this.boundaryChange = this.boundaryChange || other.boundaryChange;
        this.multiInvBroken = this.multiInvBroken || other.multiInvBroken;
        this.matchableNewFormat = this.matchableNewFormat || other.matchableNewFormat;
        this.nonMatchableNewFormat = this.nonMatchableNewFormat || other.nonMatchableNewFormat;
        this.nonMatchableNewFormatCount += other.nonMatchableNewFormatCount;
        this.nonMatchableMultiInv = this.nonMatchableMultiInv || other.nonMatchableMultiInv;
        this.nonMatchableMultiInvCount += other.nonMatchableMultiInvCount;
        this.isSerialized = this.isSerialized || other.isSerialized;
    }

    public boolean isChanged() {
        return newFormat || boundaryChange;
    }

    public boolean isNewFormat() {
        return newFormat;
    }

    public int getNewFormatCount() {
        return newFormatCount;
    }

    public boolean isBoundaryChange() {
        return boundaryChange;
    }

    public boolean isMultiInvBroken() {
        return multiInvBroken;
    }

    public boolean isNonMatchableMultiInv() {
        return nonMatchableMultiInv;
    }

    public boolean isMatchableNewFormat() {
        return matchableNewFormat;
    }

    public boolean isNonMatchableNewFormat() {
        return nonMatchableNewFormat;
    }

    public int getNonMatchableNewFormatCount() {
        return nonMatchableNewFormatCount;
    }

    public int getNonMatchableMultiInvCount() {
        return nonMatchableMultiInvCount;
    }

    public boolean isNewIsSerialize() {
        return isSerialized;
    }

    public void setMatchableNewFormat(String log) {
        // Runtime.log("[Matchable new format] " + log);
        this.matchableNewFormat = true;
    }

    public void setNonMatchableNewFormat(String log) {
        // Runtime.log("[Matchable new format] " + log);
        this.nonMatchableNewFormat = true;
        this.nonMatchableNewFormatCount++;
    }

    public void setIsSerialize(String log) {
        // Runtime.log("[Matchable new format] " + log);
        this.isSerialized = true;
    }

    @Override
    public String toString() {
        return "FormatCoverageStatus{" + "newFormat=" + newFormat + ", newFormatCount="
                + newFormatCount + ", boundaryChange=" + boundaryChange + ", multiInvBroken="
                + multiInvBroken + ", matchableNewFormat=" + matchableNewFormat
                + ", nonMatchableNewFormat=" + nonMatchableNewFormat
                + ", nonMatchableNewFormatCount=" + nonMatchableNewFormatCount
                + ", nonMatchableMultiInv=" + nonMatchableMultiInv + ", nonMatchableMultiInvCount="
                + nonMatchableMultiInvCount + ", isSerialized=" + isSerialized + '}';
    }
}
