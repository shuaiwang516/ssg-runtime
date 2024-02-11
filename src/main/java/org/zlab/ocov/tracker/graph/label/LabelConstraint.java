package org.zlab.ocov.tracker.graph.label;

import org.zlab.ocov.tracker.graph.ObjectGraph;
import org.zlab.ocov.tracker.inv.unary.LogInfo;
import org.zlab.ocov.tracker.inv.unary.UnaryInvariant;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

public abstract class LabelConstraint implements Serializable {

    final List<UnaryInvariant> unaryInvariants;

    public LabelConstraint(List<UnaryInvariant> unaryInvariants) {
        this.unaryInvariants = unaryInvariants;
    }

    public abstract boolean checkPure(ObjectGraph.Vertex vertex, LogInfo logInfo, String itinerary,
            Set<String> brokenInvs);

    public boolean checkPure(Object object, LogInfo logInfo, String itinerary,
            Set<String> brokenInvs) {
        boolean changed = false;
        for (UnaryInvariant invariant : unaryInvariants) {
            if (invariant.checkPure(object, logInfo)) {
                brokenInvs.add("<" + invariant.typeName + ">, iti = " + itinerary);
                changed = true;
            }
        }
        return changed;
    }

    public abstract boolean update(ObjectGraph.Vertex vertex, LogInfo logInfo);

    public boolean update(Object object, LogInfo logInfo) {
        boolean changed = false;
        for (UnaryInvariant invariant : unaryInvariants) {
            if (invariant.add(object, logInfo))
                changed = true;
        }
        return changed;
    }

    public abstract boolean merge(LabelConstraint otherConstraint, String itinerary);

}
