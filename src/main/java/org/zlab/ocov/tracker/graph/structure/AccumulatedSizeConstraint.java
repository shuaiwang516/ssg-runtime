package org.zlab.ocov.tracker.graph.structure;

import org.zlab.ocov.tracker.graph.ObjectGraph;
import org.zlab.ocov.tracker.graph.ObjectGraphDumper;
import org.zlab.ocov.tracker.inv.unary.LogInfo;
import org.zlab.ocov.tracker.inv.unary.UnaryInvariant;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AccumulatedSizeConstraint extends StructureConstraint {

    public AccumulatedSizeConstraint(List<UnaryInvariant> unaryFormatInvariants,
            List<UnaryInvariant> unaryBoundaryInvariants, String edgeLabel) {
        super(unaryFormatInvariants, unaryBoundaryInvariants, edgeLabel);
    }

    @Override
    public boolean checkPure(ObjectGraph.Vertex vertex, ObjectGraph graph, LogInfo logInfo,
            String itinerary, Set<String> brokenInvs) {
        Integer accumulatedSize = computeAccumulatedSize(vertex, graph, edgeLabel);
        if (accumulatedSize == null)
            return false;
        boolean changed = false;
        for (UnaryInvariant invariant : unaryFormatInvariants) {
            if (invariant.checkPure(accumulatedSize, logInfo)) {
                brokenInvs.add("<" + invariant.typeName + ">, iti = " + itinerary);
                changed = true;
            }
        }
        for (UnaryInvariant invariant : unaryBoundaryInvariants) {
            if (invariant.checkPure(accumulatedSize, logInfo)) {
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
        Integer accumulatedSize = computeAccumulatedSize(object, edgeLabel);
        if (accumulatedSize == null)
            return false;
        boolean result = false;
        for (UnaryInvariant invariant : unaryFormatInvariants) {
            if (invariant.checkPure(accumulatedSize, logInfo)) {
                brokenInvs.add("<" + invariant.typeName + ">, iti = " + itinerary);
                result = true;
            }
        }
        for (UnaryInvariant invariant : unaryBoundaryInvariants) {
            if (invariant.checkPure(accumulatedSize, logInfo)) {
                brokenInvs.add("<" + invariant.typeName + ">, iti = " + itinerary);
                result = true;
            }
        }
        return result;
    }

    @Override
    public boolean update(ObjectGraph.Vertex vertex, ObjectGraph graph, LogInfo logInfo) {
        Integer accumulatedSize = computeAccumulatedSize(vertex, graph, edgeLabel);
        if (accumulatedSize == null)
            return false;
        boolean changed = false;
        for (UnaryInvariant invariant : unaryFormatInvariants) {
            if (invariant.add(accumulatedSize, logInfo))
                changed = true;
        }
        for (UnaryInvariant invariant : unaryBoundaryInvariants) {
            if (invariant.add(accumulatedSize, logInfo))
                changed = true;
        }
        return changed;
    }

    @Override
    public boolean update(Object object, LogInfo logInfo) {
        if (object == null)
            return false;
        Integer accumulatedSize = computeAccumulatedSize(object, edgeLabel);
        if (accumulatedSize == null)
            return false;
        boolean changed = false;
        for (UnaryInvariant invariant : unaryFormatInvariants) {
            if (invariant.add(accumulatedSize, logInfo))
                changed = true;
        }
        for (UnaryInvariant invariant : unaryBoundaryInvariants) {
            if (invariant.add(accumulatedSize, logInfo))
                changed = true;
        }
        return changed;
    }

    public static Integer computeAccumulatedSize(ObjectGraph.Vertex vertex, ObjectGraph graph,
            String edgeLabel) {
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
            return null;
        return accumulatedSize;
    }

    public static Integer computeAccumulatedSize(Object object, String edgeLabel) {
        int accumulatedSize = 0;
        boolean atLeastOne = false;
        if (edgeLabel.equals("collection_item")) {
            for (Object item : (Collection) object) {
                Integer size = ObjectGraphDumper.invokeSizeMethodIfExists(item);
                if (size != null) {
                    accumulatedSize += size;
                    if (!atLeastOne)
                        atLeastOne = true;
                }
            }
        } else if (edgeLabel.equals("map_keyItem")) {
            for (Object item : ((Map) object).keySet()) {
                Integer size = ObjectGraphDumper.invokeSizeMethodIfExists(item);
                if (size != null) {
                    accumulatedSize += size;
                }
            }
        } else if (edgeLabel.equals("map_valueItem")) {
            for (Object item : ((Map) object).values()) {
                Integer size = ObjectGraphDumper.invokeSizeMethodIfExists(item);
                if (size != null) {
                    accumulatedSize += size;
                }
            }
        } else if (edgeLabel.equals("array_item")) {
            for (int i = 0; i < Array.getLength(object); i++) {
                Object item = Array.get(object, i);
                Integer size = ObjectGraphDumper.invokeSizeMethodIfExists(item);
                if (size != null) {
                    accumulatedSize += size;
                    if (!atLeastOne)
                        atLeastOne = true;
                }
            }
        } else {
            throw new RuntimeException("Unknown edge label: " + edgeLabel);
        }
        if (!atLeastOne)
            return null;
        return accumulatedSize;
    }

}
