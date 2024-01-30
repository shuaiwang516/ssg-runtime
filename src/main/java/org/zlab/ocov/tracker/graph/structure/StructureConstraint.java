package org.zlab.ocov.tracker.graph.structure;

import org.zlab.ocov.tracker.Runtime;
import org.zlab.ocov.tracker.graph.ObjectGraph;
import org.zlab.ocov.tracker.inv.unary.LogInfo;
import org.zlab.ocov.tracker.inv.unary.UnaryInvariant;

import java.io.Serializable;
import java.util.List;

public abstract class StructureConstraint implements Serializable {
    protected List<UnaryInvariant> unaryInvariants;

    String edgeLabel;

    public StructureConstraint(List<UnaryInvariant> unaryInvariants, String edgeLabel) {
        this.unaryInvariants = unaryInvariants;
        this.edgeLabel = edgeLabel;
    }

    public abstract boolean update(ObjectGraph.Vertex vertex, ObjectGraph graph, LogInfo logInfo);

    public abstract boolean update(Object object, LogInfo logInfo);

    public boolean merge(StructureConstraint otherConstraint, String itinerary) {
        if (otherConstraint instanceof OutDegreeConstraint) {
            boolean changed = false;
            OutDegreeConstraint other = (OutDegreeConstraint) otherConstraint;
            assert unaryInvariants.size() == other.unaryInvariants.size();
            for (int i = 0; i < unaryInvariants.size(); i++) {
                if (unaryInvariants.get(i).merge(other.unaryInvariants.get(i))) {
                    Runtime.log("Broken Structure Constraint: " + unaryInvariants.get(i).toString()
                            + ", itinerary = " + itinerary);
                    changed = true;
                }
            }
            return changed;
        }
        return false;
    }

}
