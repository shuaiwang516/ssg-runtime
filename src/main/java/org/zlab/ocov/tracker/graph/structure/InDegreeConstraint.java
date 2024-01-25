package org.zlab.ocov.tracker.graph.structure;

import org.zlab.ocov.tracker.graph.ObjectGraph;
import org.zlab.ocov.tracker.inv.unary.LogInfo;
import org.zlab.ocov.tracker.inv.unary.UnaryInvariant;

import java.util.List;

public class InDegreeConstraint extends StructureConstraint {

    private final String edgeLabel;

    public InDegreeConstraint(List<UnaryInvariant> unaryInvariants, String edgeLabel) {
        super(unaryInvariants);
        this.edgeLabel = edgeLabel;
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
    public boolean merge(StructureConstraint otherConstraint) {

        if (otherConstraint instanceof InDegreeConstraint) {
            boolean result = false;
            InDegreeConstraint other = (InDegreeConstraint) otherConstraint;
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
