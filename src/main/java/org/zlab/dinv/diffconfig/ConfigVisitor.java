package org.zlab.dinv.diffconfig;

import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

public class ConfigVisitor extends VoidVisitorAdapter<SingleClassConfigInfo> {

    @Override
    public void visit(FieldDeclaration field, SingleClassConfigInfo singleClassConfigInfo) {
        super.visit(field, singleClassConfigInfo);
        for (VariableDeclarator variableDeclarator: field.getVariables()) {
            singleClassConfigInfo.typeCollector.put(variableDeclarator.getNameAsString(), variableDeclarator.getTypeAsString());
            if (variableDeclarator.getInitializer().isPresent()) {
                singleClassConfigInfo.initCollector.put(variableDeclarator.getNameAsString(), variableDeclarator.getInitializer().get().toString());
            }
        }
    }

}