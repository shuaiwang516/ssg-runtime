package org.zlab.dinv.visibility;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public abstract class RewriteAST extends IterateAST {
    public final Map<String, Set<Integer>> programLocations;
    public int fieldId = 0;
    public String currentClassFullName;
    public String currentMethodName;

    public Set<String> newStaticFields;
    public Set<String> newNonStaticFields;
    public boolean isStatic;

    public Map<String, Map<String, Set<String>>> pptVars = new HashMap<>();

    public RewriteAST(Map<String, Set<Integer>> programLocations) {
        this.programLocations = programLocations;
    }

    public abstract void addField(ClassOrInterfaceDeclaration classDecl, boolean isStatic, String fieldName);

    public void process(CompilationUnit cu) {
        cu.findAll(ClassOrInterfaceDeclaration.class).forEach(classDecl -> {
            if (!classDecl.getFullyQualifiedName().isPresent()) {
                return;
            }
            String clazzFullName = classDecl.getFullyQualifiedName().get();
            if (!programLocations.containsKey(clazzFullName))
                return;
            Set<Integer> lineSet = programLocations.get(clazzFullName);
            currentClassFullName = clazzFullName;

            newStaticFields = new HashSet<>();
            newNonStaticFields = new HashSet<>();

            classDecl.getMethods().forEach(methodDecl -> {
                isStatic = methodDecl.isStatic();
                currentMethodName = methodDecl.getNameAsString();
                methodDecl.getBody().ifPresent(body -> processBlockStmt(body, lineSet));
            });

            for (String newStaticField: newStaticFields) {
                addField(classDecl, true, newStaticField);
            }

            for (String newNonStaticField: newNonStaticFields) {
                addField(classDecl, false, newNonStaticField);
            }

        });
    }
}
