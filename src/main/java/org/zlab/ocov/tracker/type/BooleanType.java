package org.zlab.ocov.tracker.type;

import org.zlab.ocov.tracker.ClassInfo;
import org.zlab.ocov.tracker.EqualitySet;
import org.zlab.ocov.tracker.IsSerialize;
import org.zlab.ocov.tracker.inv.unary.FalseOnce;
import org.zlab.ocov.tracker.inv.unary.LogInfo;
import org.zlab.ocov.tracker.inv.unary.NullOnce;
import org.zlab.ocov.tracker.inv.unary.TrueOnce;

import java.util.Map;

public class BooleanType extends TypeInfo {
    private static final long serialVersionUID = 20231215L;

    NullOnce nullOnce = new NullOnce();
    TrueOnce trueOnce = new TrueOnce();
    FalseOnce falseOnce = new FalseOnce();

    // describe some characteristics
    public BooleanType(String itinerary) {
        super("Boolean", itinerary);
    }

    @Override
    public void updateItinerary(String itineraryPrefix) {
        itinerary = itineraryPrefix + itinerary;
    }

    @Override
    public boolean update(Object value, Map<String, ClassInfo> baseClassInfo, int dumpId,
            EqualitySet equalitySet, IsSerialize isSerialized) {
        if (value == null)
            return nullOnce.add(value, new LogInfo(dumpId));

        if (value instanceof Boolean) {
            boolean changed = false;
            LogInfo logInfo = new LogInfo(dumpId);
            if (trueOnce.add(value, logInfo) || falseOnce.add(value, logInfo)) {
                changed = true;
            }
            return changed;
        }
        throw new RuntimeException(String.format("Not an %s but claimed to be", typeName));
    }

    @Override
    public boolean merge(TypeInfo otherTypeInfo) {
        if (otherTypeInfo instanceof BooleanType) {
            BooleanType otherBooleanType = (BooleanType) otherTypeInfo;
            boolean changed = false;

            if (nullOnce.merge(otherBooleanType.nullOnce)) {
                log("itineraryNullOnce", itinerary, nullOnce.dumpId);
                changed = true;
            }
            if (trueOnce.merge(otherBooleanType.trueOnce)) {
                log("itineraryTrueOnce", itinerary, trueOnce.dumpId);
                changed = true;
            }
            if (falseOnce.merge(otherBooleanType.falseOnce)) {
                log("itineraryFalseOnce", itinerary, falseOnce.dumpId);
                changed = true;
            }
            return changed;
        }
        throw new RuntimeException(String.format("Not an %s but claimed to be", typeName));
    }

}
