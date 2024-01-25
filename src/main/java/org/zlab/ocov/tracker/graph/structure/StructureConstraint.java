package org.zlab.ocov.tracker.graph.structure;

import org.zlab.ocov.tracker.graph.ObjectGraph;
import org.zlab.ocov.tracker.inv.unary.LogInfo;
import org.zlab.ocov.tracker.inv.unary.UnaryInvariant;

import java.io.Serializable;
import java.util.List;

public abstract class StructureConstraint implements Serializable {
    protected List<UnaryInvariant> unaryInvariants;

    public StructureConstraint(List<UnaryInvariant> unaryInvariants) {
        this.unaryInvariants = unaryInvariants;
    }

    public abstract boolean update(ObjectGraph.Vertex vertex, ObjectGraph graph, LogInfo logInfo);
    public abstract boolean merge(StructureConstraint otherConstraint);

}
