package org.zlab.ocov.tracker.type;

import org.zlab.ocov.tracker.ClassInfo;

import java.io.Serializable;
import java.util.Map;

public abstract class TypeInfo implements Serializable {
    private static final long serialVersionUID = 20231215L;

    public String typeName;

    public TypeInfo() {
    }

    public TypeInfo(String typeName) {
        this.typeName = typeName;
    }

    // Update constraint information
    // NULL Value should be handled inside each type!
    public abstract boolean update(Object value, Map<String, ClassInfo> baseClassInfo);

    // Merge
    public abstract boolean merge(TypeInfo otherTypeInfo);

    // TODO: Support all types
    public static TypeInfo createTypeInfo(String typeName) {
        if (typeName.equals("int") || typeName.equals("java.lang.Integer")) {
            return new IntegerType();
        } else if (typeName.equals("long") || typeName.equals("java.lang.Long")) {
            return new LongType();
        } else if (typeName.equals("float") || typeName.equals("java.lang.Float")) {
            return new FloatType();
        } else if (typeName.equals("double") || typeName.equals("java.lang.Double")) {
            return new DoubleType();
        } else if (typeName.equals("boolean") || typeName.equals("java.lang.Boolean")) {
            return new BooleanType();
        } else if (typeName.equals("char") || typeName.equals("java.lang.Character")) {
            // Skip char?
            return null;
        } else if (typeName.equals("byte") || typeName.equals("java.lang.Byte")) {
            return null;
        } else if (typeName.equals("short") || typeName.equals("java.lang.Short")) {
            return new ShortType();
        } else if (typeName.equals("java.lang.String")) {
            return new StringType();
        } else if (typeName.equals("java.util.List") || typeName.equals("java.util.ArrayList")
                || typeName.equals("java.util.LinkedList") || typeName.equals("java.util.Vector")
                || typeName.equals("java.util.Stack") || typeName.equals("java.util.Queue")
                || typeName.equals("java.util.PriorityQueue")) {
            return new CollectionType();
        } else if (typeName.equals("java.util.Map") || typeName.equals("java.util.HashMap")
                || typeName.equals("java.util.TreeMap") || typeName.equals("java.util.Hashtable")
                || typeName.equals("java.util.LinkedHashMap")
                || typeName.equals("java.util.WeakHashMap")
                || typeName.equals("java.util.IdentityHashMap")
                || typeName.equals("java.util.EnumMap")
                || typeName.equals("java.util.ConcurrentHashMap")
                || typeName.equals("java.util.ConcurrentSkipListMap")) {
            // Map
            return null;
        } else if (typeName.equals("java.util.Set") || typeName.equals("java.util.HashSet")
                || typeName.equals("java.util.TreeSet")
                || typeName.equals("java.util.LinkedHashSet")
                || typeName.equals("java.util.EnumSet")
                || typeName.equals("java.util.concurrent.CopyOnWriteArraySet")) {
            // Set
            return new CollectionType();
        } else if (typeName.equals("java.util.SortedSet")
                || typeName.equals("java.util.NavigableSet")
                || typeName.equals("java.util.concurrent.ConcurrentSkipListSet")) {
            // SortedSet
            return new CollectionType();
        } else if (typeName.contains("[]")) {
            // Array
            return new ArrayType();
        } else {
            return new ObjectType();
        }
    }

}
