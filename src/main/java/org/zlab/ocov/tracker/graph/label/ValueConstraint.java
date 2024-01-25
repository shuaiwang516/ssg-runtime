package org.zlab.ocov.tracker.graph.label;

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
        boolean result = false;
        for (UnaryInvariant invariant : unaryInvariants) {
            if (invariant.add(vertex.value, logInfo))
                result = true;
        }
        return result;
    }

    @Override
    public boolean merge(LabelConstraint otherConstraint) {
        if (otherConstraint instanceof ValueConstraint) {
            boolean result = false;
            ValueConstraint other = (ValueConstraint) otherConstraint;
            // the two likely invariants should be in the same order
            assert unaryInvariants.size() == other.unaryInvariants.size();
            for (int i = 0; i < unaryInvariants.size(); i++) {
                if (unaryInvariants.get(i).merge(other.unaryInvariants.get(i)))
                    result = true;
            }
            return result;
        }
        return false;
    }

}
