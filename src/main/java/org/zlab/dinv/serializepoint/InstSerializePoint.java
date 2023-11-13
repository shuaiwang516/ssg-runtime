package org.zlab.dinv.serializepoint;

import com.github.javaparser.Range;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.Statement;

import java.util.Map;
import java.util.Set;

public class InstSerializePoint extends IterateAST {
    public Map<String, Map<Integer, Set<SerializePoint>>> serializePointsMap;

    public InstSerializePoint(Map<String, Map<Integer, Set<SerializePoint>>> serializePointsMap) {
        this.serializePointsMap = serializePointsMap;
    }

    public void process(CompilationUnit cu) {
        cu.findAll(ClassOrInterfaceDeclaration.class).forEach(classDecl -> {
            if (!classDecl.getFullyQualifiedName().isPresent()) {
                return;
            }
            String clazzFullName = classDecl.getFullyQualifiedName().get();

            if (!serializePointsMap.containsKey(clazzFullName))
                return;

            // Iterate the entire AST, check whether the line matches
            Map<Integer, Set<SerializePoint>> line2SerializePoints = serializePointsMap
                    .get(clazzFullName);

            classDecl.getMethods().forEach(methodDecl -> {
                methodDecl.getBody()
                        .ifPresent(body -> processBlockStmt(body, line2SerializePoints));
            });
        });
    }

    @Override
    public void recurProcess(Statement stmt, NodeList<Statement> newStatements,
            Map<Integer, Set<SerializePoint>> line2SerializePoints) {
        // TODO: mimic how we inject isSerialize field
        /**
         * Check whether the current statement belongs to the line set, if so, inject
         * the serialization point
         */
        if (stmt.getRange().isPresent()) {
            Range range = stmt.getRange().get();
            int begin = range.begin.line;
            if (line2SerializePoints.containsKey(begin)) {
                // inject a log statement before or after it
                // Check the type

                Statement isSerializeStmt = StaticJavaParser.parseStatement(Utils.logStatement());
                // if it's a ForEachStmt, inject after it need to be injected into the blocks
                Set<SerializePoint> serializePoints = line2SerializePoints.get(begin);
                for (SerializePoint serializePoint : serializePoints) {
                    // inject logs according to types and the current statement
                    if (serializePoint.type == SerializePoint.Type.fieldRef) {
                        // log before it
                        newStatements.add(newStatements.indexOf(stmt), isSerializeStmt);
                    } else {
                        // log after it (if it's a loop, it might inject to a wrong place): FIXME
                        if (stmt instanceof ForEachStmt) {
                            // if there's a loop, we need to inject the log into the block
                            ForEachStmt forEachStmt = (ForEachStmt) stmt;
                            Statement forEachStmtBody = forEachStmt.getBody();
                            if (forEachStmtBody instanceof BlockStmt) {
                                BlockStmt forEachStmtBodyBlock = (BlockStmt) forEachStmtBody;
                                forEachStmtBodyBlock.addStatement(0, isSerializeStmt);
                            } else {
                                // if it's not a block, we need to create a block
                                BlockStmt forEachStmtBodyBlock = new BlockStmt();
                                forEachStmtBodyBlock.addStatement(isSerializeStmt);
                                forEachStmtBodyBlock.addStatement(forEachStmtBody);
                                forEachStmt.setBody(forEachStmtBodyBlock);
                            }
                        } else {
                            newStatements.add(newStatements.indexOf(stmt) + 1, isSerializeStmt);
                        }
                    }
                }
                // String isSerializeNewField = String.format("isSerialize_%d", fieldId++);
                // String isSerializeAssignExpr = String.format("%s = true;",
                // isSerializeNewField);
                // Statement isSerializeStmt =
                // StaticJavaParser.parseStatement(isSerializeAssignExpr);
                // newStatements.add(newStatements.indexOf(stmt), isSerializeStmt);
            }
        }
        iterateStmt(stmt, line2SerializePoints);
    }
}
