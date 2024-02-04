package org.zlab.ocov.tracker.graph.structure;

import org.zlab.ocov.tracker.Runtime;
import org.zlab.ocov.tracker.graph.ObjectGraph;
import org.zlab.ocov.tracker.inv.unary.LogInfo;
import org.zlab.ocov.tracker.inv.unary.UnaryInvariant;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

public abstract class StructureConstraint implements Serializable {
    protected List<UnaryInvariant> unaryInvariants;

    String edgeLabel;

    public StructureConstraint(List<UnaryInvariant> unaryInvariants, String edgeLabel) {
        this.unaryInvariants = unaryInvariants;
        this.edgeLabel = edgeLabel;
    }

    public abstract boolean checkPure(ObjectGraph.Vertex vertex, ObjectGraph graph, LogInfo logInfo,
            String itinerary, Set<String> brokenInvs);

    public abstract boolean update(ObjectGraph.Vertex vertex, ObjectGraph graph, LogInfo logInfo);

    public abstract boolean update(Object object, LogInfo logInfo);

    public boolean merge(StructureConstraint otherConstraint, String itinerary) {
        if (otherConstraint != null) {
            boolean changed = false;
            assert unaryInvariants.size() == otherConstraint.unaryInvariants.size();
            for (int i = 0; i < unaryInvariants.size(); i++) {
                if (unaryInvariants.get(i).merge(otherConstraint.unaryInvariants.get(i))) {
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
