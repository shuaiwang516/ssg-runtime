package org.zlab.dinv.extractvars;

import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.util.Map;

public class FieldVisitor extends VoidVisitorAdapter<Map<String, String>> {

    @Override
    public void visit(FieldDeclaration field, Map<String, String> collector) {
        super.visit(field, collector);
        for (VariableDeclarator variableDeclarator: field.getVariables()) {
            collector.put(variableDeclarator.getNameAsString(), variableDeclarator.getTypeAsString());
        }
    }

}