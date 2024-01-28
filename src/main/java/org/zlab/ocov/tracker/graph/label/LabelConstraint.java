package org.zlab.ocov.tracker.graph.label;

import org.zlab.ocov.tracker.graph.ObjectGraph;
import org.zlab.ocov.tracker.inv.unary.LogInfo;
import org.zlab.ocov.tracker.inv.unary.UnaryInvariant;

import java.io.Serializable;
import java.util.List;

public abstract class LabelConstraint implements Serializable {

    final List<UnaryInvariant> unaryInvariants;
    String itinerary;

    public LabelConstraint(List<UnaryInvariant> unaryInvariants) {
        this.unaryInvariants = unaryInvariants;
    }

    public abstract boolean update(ObjectGraph.Vertex vertex, LogInfo logInfo);

    public abstract boolean merge(LabelConstraint otherConstraint, String itinerary);

}
