package org.zlab.ocov.tracker.graph.label;

import org.zlab.ocov.tracker.FormatCoverageStatus;
import org.zlab.ocov.tracker.Runtime;
import org.zlab.ocov.tracker.graph.ObjectGraph;
import org.zlab.ocov.tracker.inv.unary.LogInfo;
import org.zlab.ocov.tracker.inv.unary.UnaryInvariant;

import java.util.List;
import java.util.Set;

public class ValueConstraint extends LabelConstraint {

    public ValueConstraint(List<UnaryInvariant> unaryFormatInvariants,
            List<UnaryInvariant> unaryBoundaryInvariants) {
        super(unaryFormatInvariants, unaryBoundaryInvariants);
    }

    @Override
    public boolean checkPure(ObjectGraph.Vertex vertex, LogInfo logInfo, String itinerary,
            Set<String> brokenInvs) {
        return checkPure(vertex.value, logInfo, itinerary, brokenInvs);
    }

    @Override
    public boolean update(ObjectGraph.Vertex vertex, LogInfo logInfo) {
        return update(vertex.value, logInfo);
    }

    @Override
    public FormatCoverageStatus merge(LabelConstraint otherConstraint, String itinerary,
            LogInfo logInfo) {
        FormatCoverageStatus formatCoverageStatus = new FormatCoverageStatus();

        if (otherConstraint instanceof ValueConstraint) {
            ValueConstraint other = (ValueConstraint) otherConstraint;
            assert unaryFormatInvariants.size() == other.unaryFormatInvariants.size();
            for (int i = 0; i < unaryFormatInvariants.size(); i++) {
                if (unaryFormatInvariants.get(i).merge(other.unaryFormatInvariants.get(i))) {
                    Runtime.log("Broken Value Format Constraint: " + "context hash = "
                            + logInfo.contextHashCode + ", "
                            + unaryFormatInvariants.get(i).toString() + ", " + "itinerary = "
                            + itinerary);
                    formatCoverageStatus.newFormat = true;
                }
            }
            assert unaryBoundaryInvariants.size() == other.unaryBoundaryInvariants.size();
            for (int i = 0; i < unaryBoundaryInvariants.size(); i++) {
                if (unaryBoundaryInvariants.get(i).merge(other.unaryBoundaryInvariants.get(i))) {
                    Runtime.log("Broken Value Boundary Constraint: " + "context hash = "
                            + logInfo.contextHashCode + ", "
                            + unaryBoundaryInvariants.get(i).toString() + ", " + "itinerary = "
                            + itinerary);
                    formatCoverageStatus.boundaryChange = true;
                }
            }
        }
        return formatCoverageStatus;
    }
}
