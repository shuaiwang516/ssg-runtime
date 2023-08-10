package org.zlab.dinv.modifiedfields;

import com.github.javaparser.ast.body.EnumConstantDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class EnumVisitor extends VoidVisitorAdapter<Map<String, List<String>>> {
    @Override
    public void visit(EnumDeclaration ed, Map<String, List<String>> enumInfo) {
        super.visit(ed, enumInfo);
        if (!ed.getFullyQualifiedName().isPresent())
            return;
        String enumName = ed.getFullyQualifiedName().get();
        List<String> constants = new LinkedList<>();
        List<EnumConstantDeclaration> entries = ed.getEntries();
        for (EnumConstantDeclaration entry : entries) {
            constants.add(entry.getNameAsString());
        }
        enumInfo.put(enumName, constants);
    }
}




