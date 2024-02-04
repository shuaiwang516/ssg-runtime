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

    public abstract boolean update(ObjectGraph.Vertex vertex, LogInfo logInfo);

    public abstract boolean update(Object object, LogInfo logInfo);

    public abstract boolean merge(LabelConstraint otherConstraint, String itinerary);

}
