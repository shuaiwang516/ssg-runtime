package org.zlab.dinv.runtimechecker;

import com.github.javaparser.Range;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.Statement;
import org.zlab.dinv.visibility.IterateAST;
import java.util.Set;

public class ExitPointVisitor extends IterateAST {

    public void process(MethodDeclaration methodDeclaration, Set<Integer> lineSet) {
        methodDeclaration.getBody().ifPresent(body -> processBlockStmt(body, lineSet));
    }

    @Override
    public void recurProcess(Statement stmt, NodeList<Statement> newStatements, Set<Integer> lineSet) {
        if (stmt.getRange().isPresent()) {
            Range range = stmt.getRange().get();
            int begin = range.begin.line;

            if (lineSet.contains(begin)) {
                // inject a field assignment before it
                // isSerialize_fieldId = true;
                String exitField = String.format("EXIT%d", begin);
                String exitVariableAssignExpr = String.format("%s = true;", exitField);
                Statement exitVariableAssignStmt = StaticJavaParser.parseStatement(exitVariableAssignExpr);
                newStatements.add(newStatements.indexOf(stmt), exitVariableAssignStmt);
            }
        }
        iterateStmt(stmt, lineSet);
    }

    @Override
    public BlockStmt processNonBlockStmt(Statement stmt, Set<Integer> lineSet) {
        // if processed, return a block stmt
        BlockStmt blockStmt = null;
        if (stmt.getRange().isPresent()) {
            Range range = stmt.getRange().get();
            int begin = range.begin.line;
            if (lineSet.contains(begin)) {
                blockStmt = new BlockStmt();
                // inject something
                String exitField = String.format("EXIT%d", begin);
                String exitVariableAssignExpr = String.format("%s = true;", exitField);
                Statement exitVariableAssignStmt = StaticJavaParser.parseStatement(exitVariableAssignExpr);
                blockStmt.addStatement(exitVariableAssignStmt);
                blockStmt.addStatement(stmt);
                return blockStmt;
            }
        }
        return blockStmt;
    }
}
