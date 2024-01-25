package org.zlab.ocov.tracker.graph.structure;

import org.zlab.ocov.tracker.graph.ObjectGraph;
import org.zlab.ocov.tracker.inv.unary.LogInfo;
import org.zlab.ocov.tracker.inv.unary.UnaryInvariant;

import java.util.List;

public class OutDegreeConstraint extends StructureConstraint {

    private final String edgeLabel;

    public OutDegreeConstraint(List<UnaryInvariant> unaryInvariants, String edgeLabel) {
        super(unaryInvariants);
        this.edgeLabel = edgeLabel;
    }

    @Override
    public boolean update(ObjectGraph.Vertex vertex, ObjectGraph graph, LogInfo logInfo) {
        int count = 0;
        for (ObjectGraph.Edge edge : graph.graph.outgoingEdgesOf(vertex)) {
            if (edge.getName().equals(edgeLabel)) {
                count++;
            }
        }
        boolean result = false;
        for (UnaryInvariant invariant : unaryInvariants) {
            if (invariant.add(count, logInfo))
                result = true;
        }
        return false;
    }

    @Override
    public boolean merge(StructureConstraint otherConstraint) {
        return false;
    }
}
