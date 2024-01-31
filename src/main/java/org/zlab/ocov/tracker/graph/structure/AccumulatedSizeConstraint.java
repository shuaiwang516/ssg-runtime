package org.zlab.ocov.tracker.graph.structure;

import org.zlab.ocov.tracker.graph.ObjectGraph;
import org.zlab.ocov.tracker.inv.unary.LogInfo;
import org.zlab.ocov.tracker.inv.unary.UnaryInvariant;

import java.util.List;

public class AccumulatedSizeConstraint extends StructureConstraint {

    public AccumulatedSizeConstraint(List<UnaryInvariant> unaryInvariants, String edgeLabel) {
        super(unaryInvariants, edgeLabel);
    }

    @Override
    public boolean update(ObjectGraph.Vertex vertex, ObjectGraph graph, LogInfo logInfo) {
        boolean atLeastOne = false;
        int accumulatedSize = 0;
        for (ObjectGraph.Edge edge : graph.graph.outgoingEdgesOf(vertex)) {
            if (edge.getName().equals(edgeLabel)) {
                // get target of the edge
                ObjectGraph.Vertex target = graph.graph.getEdgeTarget(edge);
                if (target.size != null) {
                    accumulatedSize += target.size;
                    if (!atLeastOne)
                        atLeastOne = true;
                }
            }
        }
        if (!atLeastOne)
            return false;
        boolean changed = false;
        for (UnaryInvariant invariant : unaryInvariants) {
            if (invariant.add(accumulatedSize, logInfo))
                changed = true;
        }
        return changed;
    }

    @Override
    public boolean update(Object object, LogInfo logInfo) {
        throw new RuntimeException("Not implemented");
    }

}
