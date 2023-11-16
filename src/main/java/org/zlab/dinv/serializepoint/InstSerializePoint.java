package org.zlab.dinv.serializepoint;

import com.github.javaparser.Range;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.ArrayAccessExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.Statement;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.zlab.dinv.serializepoint.Utils.checkIfParentNameExists;

public class InstSerializePoint extends IterateAST {
    public static final boolean USE_PRINT = false;
    public Map<String, Map<Integer, Set<SerializePoint>>> serializePointsMap;

    public boolean injected = false;

    public InstSerializePoint(Map<String, Map<Integer, Set<SerializePoint>>> serializePointsMap) {
        this.serializePointsMap = serializePointsMap;
    }

    public boolean process(CompilationUnit cu) {
        injected = false;
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
        if (injected) {
            cu.findAll(ClassOrInterfaceDeclaration.class).forEach(classDecl -> {
                if (!classDecl.getFullyQualifiedName().isPresent()) {
                    return;
                }
                // check whether this class is an inner class since we only need to inject
                // logger at the top level
                if (classDecl.isNestedType()) {
                    return;
                }
                // add logger declaration
                // String loggerDecl = "private static final org.slf4j.Logger serialize_logger =
                // org.slf4j.LoggerFactory.getLogger(\"serialize.logger\");";
                String type = "org.slf4j.Logger";
                String loggerName = "serialize_logger";
                String loggerInitExpr = "org.slf4j.LoggerFactory.getLogger(\"serialize.logger\");";
                // Transform loggerAssignExpr to Expression
                ExpressionStmt stmt = (ExpressionStmt) StaticJavaParser
                        .parseStatement(loggerInitExpr);

                if (classDecl.isInterface())
                    classDecl.addFieldWithInitializer(type, loggerName, stmt.getExpression(),
                            Modifier.Keyword.STATIC, Modifier.Keyword.FINAL);
                else
                    classDecl.addFieldWithInitializer(type, loggerName, stmt.getExpression(),
                            Modifier.Keyword.PRIVATE, Modifier.Keyword.STATIC,
                            Modifier.Keyword.FINAL);
            });
        }
        return injected;
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
                if (!injected)
                    injected = true;
                // inject a log statement before or after it
                // Check the type
                // if it's a ForEachStmt, inject after it need to be injected into the blocks
                Set<SerializePoint> serializePoints = line2SerializePoints.get(begin);
                for (SerializePoint serializePoint : serializePoints) {
                    // inject logs according to types and the current statement
                    if (serializePoint.type == SerializePoint.Type.fieldRef) {
                        // for field ref, the parent name and child name are provided
                        // the local var name might not match

                        // check whether the parent name exists in current statement
                        // (parentName.xxx)

                        // iterate all child nodes until a FieldAccessExpr is found, check whether
                        // the object name is parent name
                        // check
                        if (!checkIfParentNameExists(stmt, serializePoint.parentName,
                                serializePoint.isStatic))
                            continue;
                        Statement isSerializeStmt = StaticJavaParser.parseStatement(
                                Utils.logSerializePointStmt(serializePoint.parentName,
                                        serializePoint.parentName + "." + serializePoint.fieldName,
                                        serializePoint.printableType, serializePoint.isStatic));

                        // log before it
                        newStatements.add(newStatements.indexOf(stmt), isSerializeStmt);
                    } else {
                        // Array Ref, Collection get, iterator...
                        if (stmt instanceof ForEachStmt) {
                            // usually there's only one, so we pick the first one
                            // Caution! this could cause side effects
                            String iterableName = ((ForEachStmt) stmt).getIterable().toString();
                            List<VariableDeclarator> vars = ((ForEachStmt) stmt).getVariable()
                                    .getVariables();
                            String varName = vars.get(0).getNameAsString();

                            // get PrintableType
                            SerializePoint.PrintableType printableType = Utils
                                    .type2Printable(vars.get(0).getType().toString());

                            // can we get the type here? Specially for String
                            Statement isSerializeStmt = StaticJavaParser
                                    .parseStatement(Utils.logSerializePointStmt(iterableName,
                                            varName, printableType, serializePoint.isStatic));

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
                            Statement isSerializeStmt = null;
                            // TODO: construct log if it's not inside a loop
                            if (serializePoint.type == SerializePoint.Type.arrayRef) {
                                // get the reference from the AST
                                ArrayAccessExpr arrayAccessExpr = null;
                                if (stmt instanceof ExpressionStmt) {
                                    ExpressionStmt expressionStmt = (ExpressionStmt) stmt;
                                    Expression expression = expressionStmt.getExpression();
                                    // find array access expr
                                    if (expression instanceof ArrayAccessExpr) {
                                        arrayAccessExpr = (ArrayAccessExpr) expression;
                                    } else {
                                        // find from the child
                                        for (Node child : expression.getChildNodes()) {
                                            if (child instanceof ArrayAccessExpr) {
                                                arrayAccessExpr = (ArrayAccessExpr) child;
                                                break;
                                            }
                                        }
                                    }
                                    if (arrayAccessExpr != null) {
                                        String arrayName = arrayAccessExpr.getName().toString();
                                        String indexName = arrayAccessExpr.getIndex().toString();
                                        isSerializeStmt = StaticJavaParser.parseStatement(
                                                Utils.logSerializePointStmt(arrayName,
                                                        String.format("%s[%s]", arrayName,
                                                                indexName),
                                                        serializePoint.printableType,
                                                        serializePoint.isStatic));
                                    }
                                }
                            } else if (serializePoint.type == SerializePoint.Type.collectionGet) {

                                MethodCallExpr methodCallExpr = null;
                                if (stmt instanceof ExpressionStmt) {
                                    Expression expression = ((ExpressionStmt) stmt).getExpression();
                                    if (expression instanceof MethodCallExpr
                                            && ((MethodCallExpr) expression).getNameAsString()
                                                    .equals("get")) {
                                        methodCallExpr = (MethodCallExpr) expression;
                                    } else {
                                        // find from the child
                                        for (Node child : expression.getChildNodes()) {
                                            if (child instanceof MethodCallExpr
                                                    && ((MethodCallExpr) child).getNameAsString()
                                                            .equals("get")) {
                                                // check whether it's a collection get
                                                if (((MethodCallExpr) child).getNameAsString()
                                                        .equals("get")) {
                                                    methodCallExpr = (MethodCallExpr) child;
                                                    break;
                                                }
                                            }
                                        }
                                    }
                                }
                                if (methodCallExpr != null) {
                                    String collectionName = methodCallExpr.getScope().get()
                                            .toString();
                                    String indexName = methodCallExpr.getArgument(0).toString();
                                    isSerializeStmt = StaticJavaParser.parseStatement(
                                            Utils.logSerializePointStmt(collectionName,
                                                    String.format("%s.get(%s)", collectionName,
                                                            indexName),
                                                    serializePoint.printableType,
                                                    serializePoint.isStatic));
                                }
                            } else if (serializePoint.type == SerializePoint.Type.iterator) {

                            }
                            if (isSerializeStmt != null)
                                newStatements.add(newStatements.indexOf(stmt) + 1, isSerializeStmt);
                            else {
                                System.out.println(
                                        "Cannot find the reference for: " + serializePoint);
                            }
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

    @Override
    public BlockStmt processNonBlockStmt(Statement stmt,
            Map<Integer, Set<SerializePoint>> line2SerializePoints) {
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
                recurProcess(stmt, newStatements, line2SerializePoints);
                blockStmt.setStatements(newStatements);
            }
        }
        return blockStmt;
    }
}
