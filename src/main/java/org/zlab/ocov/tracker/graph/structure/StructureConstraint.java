package org.zlab.ocov.tracker.graph.structure;

import org.zlab.ocov.tracker.FormatCoverageStatus;
import org.zlab.ocov.tracker.Runtime;
import org.zlab.ocov.tracker.graph.ObjectGraph;
import org.zlab.ocov.tracker.inv.unary.LogInfo;
import org.zlab.ocov.tracker.inv.unary.UnaryInvariant;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

public abstract class StructureConstraint implements Serializable {
    protected List<UnaryInvariant> unaryFormatInvariants;
    protected List<UnaryInvariant> unaryBoundaryInvariants;

    String edgeLabel;

    public StructureConstraint(List<UnaryInvariant> unaryFormatInvariants,
            List<UnaryInvariant> unaryBoundaryInvariants, String edgeLabel) {
        this.unaryFormatInvariants = unaryFormatInvariants;
        this.unaryBoundaryInvariants = unaryBoundaryInvariants;

        this.edgeLabel = edgeLabel;
    }

    /**
     * Do not merge, only record all broken invariants
     */
    public abstract boolean checkPure(ObjectGraph.Vertex vertex, ObjectGraph graph, LogInfo logInfo,
            String itinerary, Set<String> brokenInvs);

    public abstract boolean checkPure(Object object, LogInfo logInfo, String itinerary,
            Set<String> brokenInvs);

    public abstract boolean update(ObjectGraph.Vertex vertex, ObjectGraph graph, LogInfo logInfo);

    public abstract boolean update(Object object, LogInfo logInfo);

    public FormatCoverageStatus merge(StructureConstraint otherConstraint, String itinerary) {
        // If this constraint is related to max min, handle it specially
        boolean newFormat = false;
        boolean boundaryChanged = false;
        if (otherConstraint != null) {
            assert unaryFormatInvariants.size() == otherConstraint.unaryFormatInvariants.size();
            for (int i = 0; i < unaryFormatInvariants.size(); i++) {
                if (unaryFormatInvariants.get(i)
                        .merge(otherConstraint.unaryFormatInvariants.get(i))) {
                    Runtime.log("Broken Structure Constraint: "
                            + unaryFormatInvariants.get(i).toString() + ", itinerary = "
                            + itinerary);
                    newFormat = true;
                }
            }

            assert unaryBoundaryInvariants.size() == otherConstraint.unaryBoundaryInvariants.size();
            for (int i = 0; i < unaryBoundaryInvariants.size(); i++) {
                if (unaryBoundaryInvariants.get(i)
                        .merge(otherConstraint.unaryBoundaryInvariants.get(i))) {
                    Runtime.log("Broken Structure Constraint: "
                            + unaryBoundaryInvariants.get(i).toString() + ", itinerary = "
                            + itinerary);
                    boundaryChanged = true;
                }
            }
        }
        return new FormatCoverageStatus(newFormat, boundaryChanged);
    }

    public void reset() {
        for (UnaryInvariant invariant : unaryFormatInvariants) {
            invariant.reset();
        }
        for (UnaryInvariant invariant : unaryBoundaryInvariants) {
            invariant.reset();
        }
    }
}
