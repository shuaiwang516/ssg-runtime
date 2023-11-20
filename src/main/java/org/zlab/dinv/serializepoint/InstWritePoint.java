package org.zlab.dinv.serializepoint;

import com.github.javaparser.Range;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.Statement;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class InstWritePoint extends IterateAST<WritePoint> {
    public Map<String, Map<Integer, Set<WritePoint>>> writePointsMap;

    public boolean injected = false;

    public InstWritePoint(Map<String, Map<Integer, Set<WritePoint>>> writePointsMap) {
        this.writePointsMap = writePointsMap;
    }

    public boolean process(CompilationUnit cu) {
        injected = false;
        cu.findAll(ClassOrInterfaceDeclaration.class).forEach(classDecl -> {
            if (!classDecl.getFullyQualifiedName().isPresent()) {
                return;
            }
            String clazzFullName = classDecl.getFullyQualifiedName().get();

            if (!writePointsMap.containsKey(clazzFullName))
                return;

            // Iterate the entire AST, check whether the line matches
            Map<Integer, Set<WritePoint>> line2SerializePoints = writePointsMap.get(clazzFullName);

            classDecl.getMethods().forEach(methodDecl -> {
                methodDecl.getBody().ifPresent(body -> processBlockStmt(body, line2SerializePoints,
                        methodDecl.isStatic()));
            });
        });

        // TODO: abstract this part, avoid redundant code
        if (injected)
            Utils.injectLogger(cu);
        return injected;
    }

    @Override
    public void recurProcess(Statement stmt, NodeList<Statement> newStatements,
            Map<Integer, Set<WritePoint>> line2SerializePoints, boolean isMethodStatic) {
        /**
         * Check whether the current statement belongs to the line set, if so, inject
         * the serialization point
         */
        if (stmt.getRange().isPresent()) {
            Range range = stmt.getRange().get();
            int begin = range.begin.line;
            if (line2SerializePoints.containsKey(begin)) {
                if (!injected)
                    injected = true;
                // inject a log statement before or after it
                // Check the type
                // if it's a ForEachStmt, inject after it need to be injected into the blocks
                Set<WritePoint> writePoints = line2SerializePoints.get(begin);
                for (WritePoint serializePoint : writePoints) {
                    // TODO: inject logs according to types and the current statement
                    // get the varName from the invoke expr
                    // find method call expr, get the first argument
                    if (stmt instanceof ExpressionStmt) {
                        Expression expr = ((ExpressionStmt) stmt).getExpression();

                        Set<String> varNameList = new LinkedHashSet<>();
                        expr.findAll(Expression.class).forEach(innerExpr -> {
                            if (innerExpr.isMethodCallExpr()) {
                                MethodCallExpr methodCallExpr = innerExpr.asMethodCallExpr();
                                if (methodCallExpr.getName().toString()
                                        .equals(serializePoint.writeMethodName)
                                        && innerExpr.asMethodCallExpr().getArguments().size() > 0) {
                                    // Found one, inject log before it!
                                    varNameList.add(
                                            innerExpr.asMethodCallExpr().getArgument(0).toString());
                                }
                            }
                        });
                        for (String varName : varNameList) {
                            Statement isSerializeStmt = StaticJavaParser.parseStatement(
                                    Utils.logWritePointStmt(varName, serializePoint.printableType));

                            // log before it
                            newStatements.add(newStatements.indexOf(stmt), isSerializeStmt);
                        }
                    }
                }
            }
        }
        iterateStmt(stmt, line2SerializePoints, isMethodStatic);
    }

    @Override
    public BlockStmt processNonBlockStmt(Statement stmt,
            Map<Integer, Set<WritePoint>> line2SerializePoints, boolean isMethodStatic) {
        BlockStmt blockStmt = null;

        if (stmt.getRange().isPresent()) {
            Range range = stmt.getRange().get();
            int begin = range.begin.line;
            if (line2SerializePoints.containsKey(begin)) {
                if (!injected)
                    injected = true;
                blockStmt = new BlockStmt();
                // inject our log statement
                NodeList<Statement> statements = new NodeList<>();
                statements.add(stmt);
                NodeList<Statement> newStatements = new NodeList<>(statements);
                recurProcess(stmt, newStatements, line2SerializePoints, isMethodStatic);
                blockStmt.setStatements(newStatements);
            }
        }
        return blockStmt;
    }
}
