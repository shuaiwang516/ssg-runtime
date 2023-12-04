package org.zlab.ocov.tracker.type;

public class CollectionType extends TypeInfo {
    int maxSize = Integer.MIN_VALUE;
    int minSize = Integer.MAX_VALUE;

    public CollectionType() {
        super("collection");
    }

    @Override
    public boolean update(Object value) {
        if (value instanceof java.util.Collection) {
            int size = ((java.util.Collection) value).size();
            boolean changed = false;
            if (size > maxSize) {
                maxSize = size;
                changed = true;
            }
            if (size < minSize) {
                minSize = size;
                changed = true;
            }
            return changed;
        }
        // Why would it not be a collection type?
        throw new RuntimeException("Not an collectiontype but claimed to be");
    }

}
