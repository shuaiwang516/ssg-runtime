package org.zlab.ocov.tracker.graph.label;

import org.zlab.ocov.tracker.Runtime;
import org.zlab.ocov.tracker.graph.ObjectGraph;
import org.zlab.ocov.tracker.inv.unary.LogInfo;
import org.zlab.ocov.tracker.inv.unary.UnaryInvariant;

import java.util.List;

public class ValueConstraint extends LabelConstraint {

    public ValueConstraint(List<UnaryInvariant> unaryInvariants) {
        super(unaryInvariants);
    }

    @Override
    public boolean update(ObjectGraph.Vertex vertex, LogInfo logInfo) {
        boolean changed = false;
        for (UnaryInvariant invariant : unaryInvariants) {
            if (invariant.add(vertex.value, logInfo))
                changed = true;
        }
        return changed;
    }

    @Override
    public boolean merge(LabelConstraint otherConstraint, String itinerary) {
        boolean changed = false;
        if (otherConstraint instanceof ValueConstraint) {
            ValueConstraint other = (ValueConstraint) otherConstraint;
            assert unaryInvariants.size() == other.unaryInvariants.size();
            for (int i = 0; i < unaryInvariants.size(); i++) {
                if (unaryInvariants.get(i).merge(other.unaryInvariants.get(i))) {
                    Runtime.log("Broken Value Constraint: " + unaryInvariants.get(i).toString()
                            + ", itinerary = " + itinerary);
                    changed = true;
                }
            }
            return changed;
        }
        return false;
    }

}
