package org.zlab.ocov.tracker.graph.structure;

import org.zlab.ocov.tracker.graph.ObjectGraph;
import org.zlab.ocov.tracker.inv.unary.LogInfo;
import org.zlab.ocov.tracker.inv.unary.UnaryInvariant;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class InDegreeConstraint extends StructureConstraint {

    public InDegreeConstraint(List<UnaryInvariant> unaryFormatInvariants,
            List<UnaryInvariant> unaryBoundaryInvariants, String edgeLabel) {
        super(unaryFormatInvariants, unaryBoundaryInvariants, edgeLabel);
    }

    @Override
    public boolean checkPure(ObjectGraph.Vertex vertex, ObjectGraph graph, LogInfo logInfo,
            String itinerary, Set<String> brokenInvs) {
        int count = 0;
        for (ObjectGraph.Edge edge : graph.graph.incomingEdgesOf(vertex)) {
            if (edge.getName().equals(edgeLabel)) {
                count++;
            }
        }
        boolean result = false;
        for (UnaryInvariant invariant : unaryFormatInvariants) {
            if (invariant.checkPure(count, logInfo)) {
                brokenInvs.add("<" + invariant.typeName + ">, iti = " + itinerary);
                result = true;
            }
        }
        for (UnaryInvariant invariant : unaryBoundaryInvariants) {
            if (invariant.checkPure(count, logInfo)) {
                brokenInvs.add("<" + invariant.typeName + ">, iti = " + itinerary);
                result = true;
            }
        }
        return result;
    }

    @Override
    public boolean checkPure(Object object, LogInfo logInfo, String itinerary,
            Set<String> brokenInvs) {
        throw new RuntimeException("InDegreeConstraint does not support checking pure on object");
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
        for (UnaryInvariant invariant : unaryFormatInvariants) {
            if (invariant.add(count, logInfo))
                result = true;
        }
        for (UnaryInvariant invariant : unaryBoundaryInvariants) {
            if (invariant.add(count, logInfo))
                result = true;
        }
        return result;
    }

    @Override
    public boolean update(Object object, LogInfo logInfo) {
        throw new RuntimeException("InDegreeConstraint does not support checking pure on object");
    }

}
