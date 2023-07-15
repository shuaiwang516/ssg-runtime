package org.zlab.dinv.diffconfig;

import com.github.javaparser.ast.body.EnumConstantDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class EnumVisitor extends VoidVisitorAdapter<Map<String, List<String>>> {
    @Override
    public void visit(EnumDeclaration ed, Map<String, List<String>> collector) {
        if (!ed.getFullyQualifiedName().isPresent())
            return;
        super.visit(ed, collector);
        List<String> constants = new LinkedList<>();
        for (EnumConstantDeclaration ecd : ed.getEntries()) {
            constants.add(ecd.getNameAsString());
        }
        collector.put(ed.getFullyQualifiedName().get(), constants);
    }
}
