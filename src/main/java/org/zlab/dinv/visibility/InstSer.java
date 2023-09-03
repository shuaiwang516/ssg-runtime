package org.zlab.dinv.visibility;

import com.github.javaparser.Range;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.BooleanLiteralExpr;
import com.github.javaparser.ast.stmt.*;

import java.util.*;

public class InstSer extends RewriteAST {

    public InstSer(Map<String, Set<Integer>> programLocations) {
        super(programLocations);
    }

    public void addField(ClassOrInterfaceDeclaration classDecl, boolean isStatic,
            String fieldName) {
        FieldDeclaration field = new FieldDeclaration();
        field.addModifier(Modifier.Keyword.PRIVATE); // Add the 'private' modifier
        field.setStatic(isStatic);

        NodeList<VariableDeclarator> vars = new NodeList<>();
        VariableDeclarator variableDeclarator = new VariableDeclarator(
                com.github.javaparser.ast.type.PrimitiveType.booleanType(), fieldName);
        variableDeclarator.setInitializer(new BooleanLiteralExpr(false));
        vars.add(variableDeclarator);
        field.setVariables(vars); // Set the type to int and the variable name to 'a'
        // Add the new field to the class declaration
        classDecl.getMembers().add(field);
    }

    public void recurProcess(Statement stmt, NodeList<Statement> newStatements,
            Set<Integer> lineSet) {
        if (stmt.getRange().isPresent()) {
            Range range = stmt.getRange().get();
            int begin = range.begin.line;

            if (lineSet.contains(begin)) {
                // inject a field assignment before it
                // isSerialize_fieldId = true;
                String isSerializeNewField = String.format("isSerialize_%d", fieldId++);
                if (isStatic) {
                    newStaticFields.add(isSerializeNewField);
                    Utils.recordStaticPptVar(pptVars, currentClassFullName, currentMethodName,
                            isSerializeNewField);

                } else {
                    newNonStaticFields.add(isSerializeNewField);
                    isSerializeNewField = "this." + isSerializeNewField;
                    Utils.recordNonStaticPptVar(pptVars, currentClassFullName, currentMethodName,
                            isSerializeNewField);
                }
                String isSerializeAssignExpr = String.format("%s = true;", isSerializeNewField);
                Statement isSerializeStmt = StaticJavaParser.parseStatement(isSerializeAssignExpr);
                newStatements.add(newStatements.indexOf(stmt), isSerializeStmt);
            }
        }
        iterateStmt(stmt, lineSet);
    }
}
