package org.zlab.dinv.visibility;

import com.github.javaparser.Range;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.stmt.*;

import java.util.*;

public class InstField {

    private final Map<String, Map<String, Set<Integer>>> targetIfBranches;

    private Set<String> newStaticFields;
    private Set<String> newNonStaticFields;
    private boolean isStatic;

    InstField(Map<String, Map<String, Set<Integer>>> targetIfBranches) {
        this.targetIfBranches = targetIfBranches;
    }

    public void process(CompilationUnit cu) {
        cu.findAll(ClassOrInterfaceDeclaration.class).forEach(classDecl -> {
            if (!classDecl.getFullyQualifiedName().isPresent()) {
                return;
            }
            String clazzFullName = classDecl.getFullyQualifiedName().get();
            if (!targetIfBranches.containsKey(clazzFullName))
                return;
            Map<String, Set<Integer>> method2lineNumber = targetIfBranches.get(clazzFullName);

            newStaticFields = new HashSet<>();
            newNonStaticFields = new HashSet<>();

            classDecl.getMethods().forEach(methodDecl -> {
                isStatic = methodDecl.isStatic();
                Set<Integer> lineSet = new HashSet<>();
                for (Map.Entry<String, Set<Integer>> entry: method2lineNumber.entrySet()) {
                    lineSet.addAll(entry.getValue());
                }
                // iterate all the if branches
                methodDecl.getBody().ifPresent(body -> processBlockStmt(body, lineSet));
            });

            // add new fields
            // Create a new field
            // Create the new field declaration
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
        vars.add(new VariableDeclarator(com.github.javaparser.ast.type.PrimitiveType.intType(), fieldName));
        field.setVariables(vars); // Set the type to int and the variable name to 'a'

        // Add the new field to the class declaration
        classDecl.getMembers().add(field);

    }

    public void recurProcess(Statement stmt, NodeList<Statement> newStatements, Set<Integer> lineSet) {
        if (stmt instanceof IfStmt) {
            // decide whether this is the target if branch
            // line id
            if (stmt.getRange().isPresent()) {
                Range range = stmt.getRange().get();
                int begin = range.begin.line;
                if (lineSet.contains(begin)) {
                    // add fields
                    // left value:

                    Expression expr = ((IfStmt) stmt).getCondition();
                    if (expr instanceof BinaryExpr) {
                        Expression leftExpr = ((BinaryExpr) expr).getLeft();
                        Expression rightExpr = ((BinaryExpr) expr).getRight();

                        String leftNewField = String.format("left_%d", begin);
                        String rightNewField = String.format("right_%d", begin);
                        if (isStatic) {
                            newStaticFields.add(leftNewField);
                            newStaticFields.add(rightNewField);
                        } else {
                            newNonStaticFields.add(leftNewField);
                            newNonStaticFields.add(rightNewField);
                            leftNewField = "this." + leftNewField;
                            rightNewField = "this." + rightNewField;
                        }

                        String leftAssignExpr = String.format("%s = (%s);", leftNewField, leftExpr.toString());
                        String rightAssignExpr = String.format("%s = (%s);", rightNewField, rightExpr.toString());

                        Statement newLeftStmt = StaticJavaParser.parseStatement(leftAssignExpr);
                        Statement newRightStmt = StaticJavaParser.parseStatement(rightAssignExpr);

                        newStatements.add(newStatements.indexOf(stmt), newLeftStmt);
                        newStatements.add(newStatements.indexOf(stmt), newRightStmt);

                    }

                    // Statement newStmt = StaticJavaParser.parseStatement("System.out.println(\"This statement was added before an if branch\");");
                }
            }
        }

        if (stmt instanceof IfStmt) {
            Statement thenStmt = ((IfStmt) stmt).getThenStmt();

            if (thenStmt instanceof BlockStmt) {
                processBlockStmt((BlockStmt) thenStmt, lineSet);
            }
            ((IfStmt) stmt).getElseStmt().ifPresent(elseStmt -> processBlockStmt((BlockStmt) elseStmt, lineSet));
        } else if (stmt instanceof BlockStmt) {
            processBlockStmt((BlockStmt) stmt, lineSet);
        }  else if (stmt instanceof WhileStmt) {
            Statement body = ((WhileStmt) stmt).getBody();
            if (body instanceof BlockStmt) {
                processBlockStmt((BlockStmt) body, lineSet);
            }
        }
        // TODOs: add more types
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
