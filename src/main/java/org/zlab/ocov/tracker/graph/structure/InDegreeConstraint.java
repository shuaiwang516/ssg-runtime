package org.zlab.ocov.tracker.graph.structure;

import org.zlab.ocov.tracker.graph.ObjectGraph;
import org.zlab.ocov.tracker.inv.unary.LogInfo;
import org.zlab.ocov.tracker.inv.unary.UnaryInvariant;

import java.util.List;

public class InDegreeConstraint extends StructureConstraint {

    public InDegreeConstraint(List<UnaryInvariant> unaryInvariants, String edgeLabel) {
        super(unaryInvariants, edgeLabel);
    }

    @Override
    public boolean update(ObjectGraph.Vertex vertex, ObjectGraph graph, LogInfo logInfo) {
        int count = 0;
        for (ObjectGraph.Edge edge : graph.graph.incomingEdgesOf(vertex)) {
            if (edge.getName().equals(edgeLabel)) {
                count++;
            }
        }
        boolean result = false;
        for (UnaryInvariant invariant : unaryInvariants) {
            if (invariant.add(count, logInfo))
                result = true;
        }
        return result;
    }

    @Override
    public boolean update(Object object, LogInfo logInfo) {
        return false;
    }

}
