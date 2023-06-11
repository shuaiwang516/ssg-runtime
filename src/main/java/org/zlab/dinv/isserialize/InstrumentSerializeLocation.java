package org.zlab.dinv.isserialize;

import com.github.javaparser.Range;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.BooleanLiteralExpr;
import com.github.javaparser.ast.stmt.*;

import java.util.*;

public class InstrumentSerializeLocation {

    private final Map<String, Set<Integer>> serializeLocations;
    private int fieldId = 0;
    private String currentClassFullName;
    private String currentMethodName;

    public Map<String, Map<String, Set<String>>> pptVars = new HashMap<>();

    private Set<String> newStaticFields;
    private Set<String> newNonStaticFields;
    private boolean isStatic;

    InstrumentSerializeLocation(Map<String, Set<Integer>> serializeLocations) {
        this.serializeLocations = serializeLocations;
    }

    public void process(CompilationUnit cu) {
        cu.findAll(ClassOrInterfaceDeclaration.class).forEach(classDecl -> {

            if (!classDecl.getFullyQualifiedName().isPresent()) {
                return;
            }
            String clazzFullName = classDecl.getFullyQualifiedName().get();
            if (!serializeLocations.containsKey(clazzFullName))
                return;
            Set<Integer> lineSet = serializeLocations.get(clazzFullName);
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

    public static void addField(ClassOrInterfaceDeclaration classDecl, boolean isStatic, String fieldName) {
        FieldDeclaration field = new FieldDeclaration();
        field.addModifier(Modifier.Keyword.PRIVATE); // Add the 'private' modifier
        field.setStatic(isStatic);

        NodeList<VariableDeclarator> vars = new NodeList<>();
        VariableDeclarator variableDeclarator = new VariableDeclarator(com.github.javaparser.ast.type.PrimitiveType.booleanType(), fieldName);
        variableDeclarator.setInitializer(new BooleanLiteralExpr(false));
        vars.add(variableDeclarator);
        field.setVariables(vars); // Set the type to int and the variable name to 'a'
        // Add the new field to the class declaration
        classDecl.getMembers().add(field);
    }

    public void recurProcess(Statement stmt, NodeList<Statement> newStatements, Set<Integer> lineSet) {
        if (stmt.getRange().isPresent()) {
            Range range = stmt.getRange().get();
            int begin = range.begin.line;

            if (lineSet.contains(begin)) {
                // inject a field assignment before it
                // isSerialize_fieldId = true;
                String isSerializeNewField = String.format("isSerialize_%d", fieldId++);
                if (isStatic) {
                    newStaticFields.add(isSerializeNewField);
                    Utils.recordStaticPptVar(pptVars, currentClassFullName, currentMethodName, isSerializeNewField);

                } else {
                    newNonStaticFields.add(isSerializeNewField);
                    isSerializeNewField = "this." + isSerializeNewField;
                    Utils.recordNonStaticPptVar(pptVars, currentClassFullName, currentMethodName, isSerializeNewField);
                }
                String isSerializeAssignExpr = String.format("%s = true;", isSerializeNewField);
                Statement isSerializeStmt = StaticJavaParser.parseStatement(isSerializeAssignExpr);
                newStatements.add(newStatements.indexOf(stmt), isSerializeStmt);
            }
        }

        if (stmt instanceof IfStmt) {
            Statement iterateStmt = stmt;
            while (true) {
                // iterate all if-elseif-elseif-elseblock
                Statement thenStmt = ((IfStmt) iterateStmt).getThenStmt();
                if (thenStmt instanceof BlockStmt) {
                    processBlockStmt((BlockStmt) thenStmt, lineSet);
                }
                if (((IfStmt) iterateStmt).getElseStmt().isPresent()) {
                    Statement elseStmt =  ((IfStmt) iterateStmt).getElseStmt().get();
                    if (elseStmt instanceof IfStmt) {
                        iterateStmt = elseStmt;
                    } else {
                        // this is a block o null
                        if (elseStmt instanceof BlockStmt)
                            processBlockStmt((BlockStmt) elseStmt, lineSet);
                        break;
                    }
                } else {
                    break;
                }
            }
        } else if (stmt instanceof BlockStmt) {
            processBlockStmt((BlockStmt) stmt, lineSet);
        }  else if (stmt instanceof WhileStmt) {
            Statement body = ((WhileStmt) stmt).getBody();
            if (body instanceof BlockStmt) {
                processBlockStmt((BlockStmt) body, lineSet);
            }
        }
        // TODO: add more types
    }

    public void processBlockStmt(BlockStmt blockStmt, Set<Integer> lineSet) {
        NodeList<Statement> statements = blockStmt.getStatements();
        NodeList<Statement> newStatements = new NodeList<>(statements);
        for (Statement innerStmt : statements) {
            recurProcess(innerStmt, newStatements, lineSet);
        }
        blockStmt.setStatements(newStatements);
    }
}
