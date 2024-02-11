package org.zlab.ocov.tracker.graph.structure;

import org.zlab.ocov.tracker.graph.ObjectGraph;
import org.zlab.ocov.tracker.inv.unary.LogInfo;
import org.zlab.ocov.tracker.inv.unary.UnaryInvariant;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class OutDegreeConstraint extends StructureConstraint {

    public OutDegreeConstraint(List<UnaryInvariant> unaryInvariants, String edgeLabel) {
        super(unaryInvariants, edgeLabel);
    }

    @Override
    public boolean checkPure(ObjectGraph.Vertex vertex, ObjectGraph graph, LogInfo logInfo,
            String itinerary, Set<String> brokenInvs) {
        int count = 0;
        for (ObjectGraph.Edge edge : graph.graph.outgoingEdgesOf(vertex)) {
            if (edge.getName().equals(edgeLabel)) {
                count++;
            }
        }
        boolean changed = false;
        for (UnaryInvariant invariant : unaryInvariants) {
            if (invariant.checkPure(count, logInfo)) {
                brokenInvs.add("<" + invariant.typeName + ">, iti = " + itinerary);
                changed = true;
            }
        }
        return changed;
    }

    @Override
    public boolean checkPure(Object object, LogInfo logInfo, String itinerary,
            Set<String> brokenInvs) {
        if (object == null)
            return false;
        int count;
        if (edgeLabel.equals("collection_item")) {
            count = ((Collection) object).size();
        } else if (edgeLabel.equals("map_keyItem")) {
            count = ((Map) object).keySet().size();
        } else if (edgeLabel.equals("map_valueItem")) {
            count = ((Map) object).values().size();
        } else if (edgeLabel.equals("array_item")) {
            count = Array.getLength(object);
        } else {
            throw new RuntimeException("Unknown edge label: " + edgeLabel);
        }
        boolean result = false;
        for (UnaryInvariant invariant : unaryInvariants) {
            if (invariant.checkPure(count, logInfo)) {
                brokenInvs.add("<" + invariant.typeName + ">, iti = " + itinerary);
                result = true;
            }
        }
        return result;
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
