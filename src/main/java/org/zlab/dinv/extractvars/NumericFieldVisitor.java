package org.zlab.dinv.extractvars;

import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.util.List;

public class NumericFieldVisitor extends VoidVisitorAdapter<List<String>> {

    @Override
    public void visit(FieldDeclaration field, List<String> collector) {
        super.visit(field, collector);

        // Check if the field type is numeric
        if (isNumericType(field.getCommonType().toString())) {
            field.getVariables().forEach(var -> collector.add(var.getNameAsString()));
        }
    }

    private boolean isNumericType(String type) {
        switch (type) {
            case "byte":
            case "short":
            case "int":
            case "long":
            case "float":
            case "double":
                return true;
            default:
                return false;
        }
    }
}