package org.zlab.dinv.visibility;

import com.github.javaparser.Range;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.stmt.*;

import java.util.*;

public class InstField extends RewriteAST {

    InstField(Map<String, Set<Integer>> programLocations) {
        super(programLocations);
    }

    public void addField(ClassOrInterfaceDeclaration classDecl, boolean isStatic, String fieldName) {
        FieldDeclaration field = new FieldDeclaration();
        field.addModifier(Modifier.Keyword.PRIVATE); // Add the 'private' modifier
        field.setStatic(isStatic);

        NodeList<VariableDeclarator> vars = new NodeList<>();
        vars.add(new VariableDeclarator(com.github.javaparser.ast.type.PrimitiveType.longType(), fieldName));
        field.setVariables(vars); // Set the type to int and the variable name to 'a'

        // Add the new field to the class declaration
        classDecl.getMembers().add(field);
    }

    public void recurProcessBinaryExpr(Statement stmt, BinaryExpr binaryExpr, NodeList<Statement> newStatements) {
        BinaryExpr.Operator operator = binaryExpr.getOperator();
        if (operator == BinaryExpr.Operator.GREATER ||
                operator == BinaryExpr.Operator.GREATER_EQUALS ||
                operator == BinaryExpr.Operator.LESS ||
                operator == BinaryExpr.Operator.LESS_EQUALS) {

            Expression leftExpr = binaryExpr.getLeft();
            Expression rightExpr = binaryExpr.getRight();

            String leftNewField = String.format("left_%d", fieldId);
            String rightNewField = String.format("right_%d", fieldId++);
            if (isStatic) {
                newStaticFields.add(leftNewField);
                newStaticFields.add(rightNewField);
                Utils.recordStaticPptVar(pptVars, currentClassFullName, currentMethodName, leftNewField);
                Utils.recordStaticPptVar(pptVars, currentClassFullName, currentMethodName, rightNewField);
            } else {
                newNonStaticFields.add(leftNewField);
                newNonStaticFields.add(rightNewField);
                leftNewField = "this." + leftNewField;
                rightNewField = "this." + rightNewField;
                Utils.recordNonStaticPptVar(pptVars, currentClassFullName, currentMethodName, leftNewField);
                Utils.recordNonStaticPptVar(pptVars, currentClassFullName, currentMethodName, rightNewField);
            }

            String leftAssignExpr = String.format("%s = (%s);", leftNewField, leftExpr.toString());
            String rightAssignExpr = String.format("%s = (%s);", rightNewField, rightExpr.toString());

            Statement newLeftStmt = StaticJavaParser.parseStatement(leftAssignExpr);
            Statement newRightStmt = StaticJavaParser.parseStatement(rightAssignExpr);

            newStatements.add(newStatements.indexOf(stmt), newLeftStmt);
            newStatements.add(newStatements.indexOf(stmt), newRightStmt);
            return;
        }

        if (operator == BinaryExpr.Operator.AND || operator == BinaryExpr.Operator.OR) {
            Expression leftExpr = binaryExpr.getLeft();
            Expression rightExpr = binaryExpr.getRight();
            if (leftExpr instanceof BinaryExpr) {
                recurProcessBinaryExpr(stmt, (BinaryExpr) leftExpr, newStatements);
            }
            if (rightExpr instanceof BinaryExpr) {
                recurProcessBinaryExpr(stmt, (BinaryExpr) rightExpr, newStatements);
            }
        }
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
                        recurProcessBinaryExpr(stmt, (BinaryExpr) expr, newStatements);
                    }
                    // Statement newStmt = StaticJavaParser.parseStatement("System.out.println(\"This statement was added before an if branch\");");
                }
            }
        }
        iterateStmt(stmt, lineSet);
    }
}
