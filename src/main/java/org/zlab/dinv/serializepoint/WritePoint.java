package org.zlab.dinv.serializepoint;

import java.util.Objects;

public class WritePoint {
    // Location
    public String className;
    public String methodName;
    public int lineNumber;

    public String writeMethodName;
    public boolean isPrintableType; // field
    public SerializePoint.PrintableType printableType;

    public WritePoint() {
    }

    public WritePoint(String className, String methodName, int lineNumber, String writeMethodName,
            boolean isPrintableType, SerializePoint.PrintableType printableType) {
        this.className = className;
        this.methodName = methodName;
        this.lineNumber = lineNumber;
        this.isPrintableType = isPrintableType;
        this.printableType = printableType;
        this.writeMethodName = writeMethodName;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        WritePoint writePoint = (WritePoint) obj;

        boolean isPosEqual = (className.equals(writePoint.className))
                && (methodName.equals(writePoint.methodName))
                && (lineNumber == writePoint.lineNumber);

        if (!isPosEqual || !writeMethodName.equals(writePoint.writeMethodName)
                || isPrintableType != writePoint.isPrintableType
                || printableType != writePoint.printableType)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hash(className, methodName, lineNumber, writeMethodName, isPrintableType,
                printableType);
    }

    @Override
    public String toString() {
        return "WritePoint{" + "className='" + className + '\'' + ", methodName='" + methodName
                + '\'' + ", lineNumber=" + lineNumber + ", writeMethodName='" + writeMethodName
                + '\'' + ", isPrintableType=" + isPrintableType + ", printableType=" + printableType
                + '}';
    }

}
