package org.zlab.ocov.tracker.graph.structure;

import org.zlab.ocov.tracker.Runtime;
import org.zlab.ocov.tracker.graph.ObjectGraph;
import org.zlab.ocov.tracker.inv.unary.LogInfo;
import org.zlab.ocov.tracker.inv.unary.UnaryInvariant;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class OutDegreeConstraint extends StructureConstraint {

    public OutDegreeConstraint(List<UnaryInvariant> unaryInvariants, String edgeLabel) {
        super(unaryInvariants, edgeLabel);
    }

    @Override
    public boolean update(ObjectGraph.Vertex vertex, ObjectGraph graph, LogInfo logInfo) {
        int count = 0;
        for (ObjectGraph.Edge edge : graph.graph.outgoingEdgesOf(vertex)) {
            if (edge.getName().equals(edgeLabel)) {
                count++;
            }
        }
        boolean changed = false;
        for (UnaryInvariant invariant : unaryInvariants) {
            if (invariant.add(count, logInfo))
                changed = true;
        }
        return changed;
    }

    @Override
    public boolean update(Object object, LogInfo logInfo) {
        if (object == null)
            return false;
        Integer size = null;
        if (object instanceof Collection) {
            assert edgeLabel.equals("collection_item");
            size = ((Collection) object).size();
        } else if (object instanceof Map) {
            if (edgeLabel.equals("map_keyItem")) {
                size = ((Map) object).keySet().size();
            } else if (edgeLabel.equals("map_valueItem")) {
                size = ((Map) object).values().size();
            }
        } else if (object.getClass().isArray()) {
            assert edgeLabel.equals("array_item");
            size = Array.getLength(object);
        }
        if (size != null) {
            boolean changed = false;
            for (UnaryInvariant invariant : unaryInvariants) {
                if (invariant.add(size, logInfo))
                    changed = true;
            }
            return changed;
        }
        return false;
    }

}
